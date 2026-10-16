package be.abis.sandwich.service;

import be.abis.sandwich.exception.MaxSandwichesExceededException;
import be.abis.sandwich.exception.PrintOrderException;
import be.abis.sandwich.exception.SandwichOrderNotFoundException;
import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import be.abis.sandwich.repository.CourseRepository;
import be.abis.sandwich.repository.SandwichOrderDetailRepository;
import be.abis.sandwich.repository.SandwichOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
    private final Object orderLock = new Object();
    private final Path printDirectory;

    public AbisSandwichOrderService(SandwichService sandwichService,
                                    CourseService courseService,
                                    PersonService personService,
                                    CourseRepository courseRepository,
                                    SandwichOrderRepository sandwichOrderRepository,
                                    SandwichOrderDetailRepository sandwichOrderDetailRepository,
                                    TransactionOperations transactions,
                                    @Value("${sandwich.print.directory:orders}") String printDirectory) {
        this.sandwichService = sandwichService;
        this.courseService = courseService;
        this.personService = personService;
        this.courseRepository = courseRepository;
        this.sandwichOrderRepository = sandwichOrderRepository;
        this.sandwichOrderDetailRepository = sandwichOrderDetailRepository;
        this.transactions = transactions;
        this.printDirectory = Path.of(printDirectory);
    }

    @Override
    public SandwichOrderDetail addSandwichOrderDetail(SandwichOrderDetail sod) {
        // One order at a time: when two students order at the same moment, both would otherwise try to create the
        // same person/course/order, or both pass the "max 2" check. Ordering is rare and fast, so this is cheap.
        // (The lock is held until the transaction has committed. It protects a single running application.)
        synchronized (orderLock) {
            return transactions.execute(status -> addInTransaction(sod));
        }
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
        return sandwichOrderDetailRepository.save(sod);
    }

    @Override
    @Transactional(readOnly = true)
    public String printSandwichOrder(SandwichOrder so) {
        SandwichOrder order = findExistingOrder(so);
        List<SandwichOrderDetail> details = sandwichOrderDetailRepository.findDetailsByOrder(order);
        String text = format(order, details);

        Path file = printDirectory.resolve("sandwich-order_" + order.getOrderDate() + "_" + slug(order.getCourse().getTitle()) + ".txt");
        try {
            Files.createDirectories(printDirectory);
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PrintOrderException("Could not write the order to " + file.toAbsolutePath(), e);
        }
        return text;
    }

    @Override
    @Transactional
    public double calculateSandwichOrderPrice(SandwichOrder so) {
        SandwichOrder order = findExistingOrder(so);
        double total = total(sandwichOrderDetailRepository.findDetailsByOrder(order));
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

    /** Base price of the sandwich times the amount, summed over all details. */
    private static double total(List<SandwichOrderDetail> details) {
        return details.stream()
                .map(AbisSandwichOrderService::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BigDecimal price(SandwichOrderDetail detail) {
        return BigDecimal.valueOf(detail.getSandwich().getBasePrice()).multiply(BigDecimal.valueOf(detail.getAmount()));
    }

    private static String format(SandwichOrder order, List<SandwichOrderDetail> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("SANDWICH ORDER\n");
        sb.append("Course: ").append(order.getCourse().getTitle()).append('\n');
        sb.append("Date:   ").append(order.getOrderDate()).append("\n\n");

        sb.append("Per person\n----------\n");
        Map<String, List<SandwichOrderDetail>> perPerson = new LinkedHashMap<>();
        for (SandwichOrderDetail d : details) {
            perPerson.computeIfAbsent(d.getPerson().getFirstName() + " " + d.getPerson().getLastName(), k -> new ArrayList<>()).add(d);
        }
        perPerson.forEach((person, list) -> {
            sb.append(person).append('\n');
            for (SandwichOrderDetail d : list) {
                sb.append(String.format(Locale.ROOT, "  %dx %-22s %-6s bread, %s vegetables  %s\n",
                        d.getAmount(), d.getSandwich().getName(), d.getBreadType().name().toLowerCase(Locale.ROOT),
                        d.isVegetables() ? "with" : "no  ", euro(price(d))));
                if (d.getComment() != null && !d.getComment().isBlank()) {
                    sb.append("     remark: ").append(d.getComment()).append('\n');
                }
            }
        });

        sb.append("\nTo order\n--------\n");
        Map<String, Integer> summary = new TreeMap<>();
        for (SandwichOrderDetail d : details) {
            String line = String.format(Locale.ROOT, "%-22s %-6s bread, %s vegetables", d.getSandwich().getName(),
                    d.getBreadType().name().toLowerCase(Locale.ROOT), d.isVegetables() ? "with" : "no  ");
            summary.merge(line, d.getAmount(), Integer::sum);
        }
        summary.forEach((line, amount) -> sb.append(String.format(Locale.ROOT, "  %dx %s\n", amount, line)));

        sb.append("\nTotal: ").append(euro(BigDecimal.valueOf(total(details)))).append('\n');
        return sb.toString();
    }

    private static String euro(BigDecimal amount) {
        return String.format(Locale.ROOT, "€ %.2f", amount);
    }

    private static String slug(String title) {
        String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "course" : slug;
    }
}
