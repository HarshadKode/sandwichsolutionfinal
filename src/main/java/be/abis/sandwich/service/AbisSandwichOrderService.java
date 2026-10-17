package be.abis.sandwich.service;

import be.abis.sandwich.exception.MaxSandwichesExceededException;
import be.abis.sandwich.exception.SandwichOrderNotFoundException;
import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import be.abis.sandwich.repository.CourseRepository;
import be.abis.sandwich.repository.SandwichOrderDetailRepository;
import be.abis.sandwich.repository.SandwichOrderRepository;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

@Service
public class AbisSandwichOrderService implements SandwichOrderService {

    public static final int MAX_SANDWICHES_PER_PERSON = 2;

    private final SandwichService sandwichService;
    private final CourseService courseService;
    private final PersonService personService;
    private final CourseRepository courseRepository;
    private final SandwichOrderRepository sandwichOrderRepository;
    private final SandwichOrderDetailRepository sandwichOrderDetailRepository;
    private final TransactionOperations transactions;
    private final RetryTemplate retryTemplate;
    private final OrderPrinter orderPrinter;

    public AbisSandwichOrderService(SandwichService sandwichService,
                                    CourseService courseService,
                                    PersonService personService,
                                    CourseRepository courseRepository,
                                    SandwichOrderRepository sandwichOrderRepository,
                                    SandwichOrderDetailRepository sandwichOrderDetailRepository,
                                    TransactionOperations transactions,
                                    RetryTemplate retryTemplate,
                                    OrderPrinter orderPrinter) {
        this.sandwichService = sandwichService;
        this.courseService = courseService;
        this.personService = personService;
        this.courseRepository = courseRepository;
        this.sandwichOrderRepository = sandwichOrderRepository;
        this.sandwichOrderDetailRepository = sandwichOrderDetailRepository;
        this.transactions = transactions;
        this.retryTemplate = retryTemplate;
        this.orderPrinter = orderPrinter;
    }

    @Override
    public SandwichOrderDetail addSandwichOrderDetail(SandwichOrderDetail sod) {
        // Students ordering at the same moment can collide: both see that the person/course/order does not exist yet
        // and both create it, or both pass the "max 2" check. In a SERIALIZABLE transaction the database refuses one
        // of them (a concurrency failure, or a unique constraint that says the person/course/order now exists), and
        // the retry template (see RetryConfig) then tries it again: the second time it sees what the other student's
        // transaction saved. Business errors (max 2, unknown sandwich) are never retried.
        return retryTemplate.invoke(() -> transactions.execute(status -> addInTransaction(sod)));
    }

    private SandwichOrderDetail addInTransaction(SandwichOrderDetail sod) {
        // never trust the sandwich data of the client: use what is in the database
        Sandwich sandwich = sandwichService.findSandwichById(sod.getSandwich().getId());
        Person person = personService.findOrCreatePerson(sod.getPerson().getFirstName(), sod.getPerson().getLastName());
        SandwichOrder order = findOrCreateOrder(sod.getSandwichOrder());

        int alreadyOrdered = sandwichOrderDetailRepository.countSandwichesOfPerson(order, person);
        if (alreadyOrdered + sod.getAmount() > MAX_SANDWICHES_PER_PERSON) {
            throw new MaxSandwichesExceededException(person.getFirstName() + " " + person.getLastName(), MAX_SANDWICHES_PER_PERSON);
        }

        sod.setSandwich(sandwich);
        sod.setPerson(person);
        sod.setSandwichOrder(order);
        sod.setId(0); // a new row: an id from the client must never overwrite an existing one
        SandwichOrderDetail saved = sandwichOrderDetailRepository.save(sod);

        // The total price of the order is kept up to date. This writes to the order row, so two orders for the same
        // order that are placed at the same moment conflict here: the SERIALIZABLE transaction of one of them fails
        // and is retried (see addSandwichOrderDetail). That is what keeps the "max 2" check correct.
        order.setTotalPrice(SandwichOrder.totalPriceOf(sandwichOrderDetailRepository.findDetailsByOrder(order)));
        sandwichOrderRepository.save(order);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public String printSandwichOrder(SandwichOrder so) {
        SandwichOrder order = findExistingOrder(so);
        return orderPrinter.print(order, sandwichOrderDetailRepository.findDetailsByOrder(order));
    }

    @Override
    @Transactional
    public double calculateSandwichOrderPrice(SandwichOrder so) {
        SandwichOrder order = findExistingOrder(so);
        double total = SandwichOrder.totalPriceOf(sandwichOrderDetailRepository.findDetailsByOrder(order));
        order.setTotalPrice(total);
        sandwichOrderRepository.save(order);
        return total;
    }

    private SandwichOrder findOrCreateOrder(SandwichOrder requested) {
        Course course = courseService.findOrCreateCourse(requested.getCourse().getTitle());
        return sandwichOrderRepository.findByOrderDateAndCourse(requested.getOrderDate(), course)
                .orElseGet(() -> sandwichOrderRepository.save(new SandwichOrder(requested.getOrderDate(), course)));
    }

    private SandwichOrder findExistingOrder(SandwichOrder requested) {
        String title = requested.getCourse().getTitle().trim();
        return courseRepository.findByTitleIgnoreCase(title)
                .flatMap(course -> sandwichOrderRepository.findByOrderDateAndCourse(requested.getOrderDate(), course))
                .orElseThrow(() -> new SandwichOrderNotFoundException(requested.getOrderDate(), title));
    }
}
