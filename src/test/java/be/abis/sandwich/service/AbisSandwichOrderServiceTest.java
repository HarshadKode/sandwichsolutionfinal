package be.abis.sandwich.service;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.exception.MaxSandwichesExceededException;
import be.abis.sandwich.exception.SandwichNotFoundException;
import be.abis.sandwich.exception.SandwichOrderNotFoundException;
import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import be.abis.sandwich.repository.CourseRepository;
import be.abis.sandwich.repository.SandwichOrderDetailRepository;
import be.abis.sandwich.repository.SandwichOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbisSandwichOrderServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @Mock private SandwichService sandwichService;
    @Mock private CourseService courseService;
    @Mock private PersonService personService;
    @Mock private CourseRepository courseRepository;
    @Mock private SandwichOrderRepository orderRepository;
    @Mock private SandwichOrderDetailRepository detailRepository;
    @Mock private OrderPrinter orderPrinter;

    /** Real objects (not mocks): no real transaction, and retries without waiting (1 attempt + 3 retries). */
    @Spy private TransactionOperations transactions = TransactionOperations.withoutTransaction();
    @Spy private RetryTemplate retryTemplate = new RetryTemplate(RetryPolicy.builder()
            .includes(ConcurrencyFailureException.class, DataIntegrityViolationException.class)
            .maxRetries(3)
            .delay(Duration.ZERO)
            .build());

    @InjectMocks
    private AbisSandwichOrderService service;

    private final Course course = new Course(1, "Spring Boot");
    private final Person ada = new Person(10, "Ada", "Lovelace");
    private final Person bob = new Person(11, "Bob", "Builder");
    private final Sandwich pinky = new Sandwich(21, "Pinky", "Special", "Specials", 4.8);
    private final Sandwich hesp = new Sandwich(1, "Hesp", "Ham", "Vlees", 3.2);
    private final SandwichOrder existingOrder = orderWithId(100);

    private SandwichOrder orderWithId(int id) {
        SandwichOrder order = new SandwichOrder(DAY, course);
        order.setId(id);
        return order;
    }

    /** What the controller passes on: only the id of the sandwich, the names of the person and the course. */
    private SandwichOrderDetail request(int sandwichId, int amount, String first, String last) {
        Sandwich requested = new Sandwich();
        requested.setId(sandwichId);
        SandwichOrderDetail sod = new SandwichOrderDetail(requested, amount, BreadType.WHITE, true, "", new Person(first, last));
        sod.setSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot")));
        return sod;
    }

    private SandwichOrderDetail stored(Sandwich sandwich, int amount, Person person, boolean vegetables, String comment) {
        return new SandwichOrderDetail(sandwich, amount, BreadType.BROWN, vegetables, comment, person);
    }

    // ---- addSandwichOrderDetail ----

    @Test
    void firstSandwichCreatesTheOrderAndIsStoredWithDatabaseData() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(21)).thenReturn(pinky);
        when(personService.findOrCreatePerson("Ada", "Lovelace")).thenReturn(ada);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findByOrderDateAndCourse(DAY, course)).thenReturn(Optional.empty());
        when(orderRepository.save(any(SandwichOrder.class))).thenReturn(existingOrder);
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(0);
        when(detailRepository.save(sod)).thenReturn(sod);

        SandwichOrderDetail saved = service.addSandwichOrderDetail(sod);

        assertThat(saved).isSameAs(sod);
        verify(detailRepository).save(sod);
        assertThat(sod.getSandwich()).isSameAs(pinky);
        assertThat(sod.getPerson()).isSameAs(ada);
        assertThat(sod.getSandwichOrder()).isSameAs(existingOrder);
    }

    @Test
    void laterSandwichesAreAddedToTheExistingOrder() {
        SandwichOrderDetail sod = request(1, 1, "Bob", "Builder");
        when(sandwichService.findSandwichById(1)).thenReturn(hesp);
        when(personService.findOrCreatePerson("Bob", "Builder")).thenReturn(bob);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findByOrderDateAndCourse(DAY, course)).thenReturn(Optional.of(existingOrder));
        when(detailRepository.countSandwichesOfPerson(existingOrder, bob)).thenReturn(0);

        service.addSandwichOrderDetail(sod);

        verify(orderRepository, never()).save(argThat(order -> order != existingOrder)); // no new order
        verify(detailRepository).save(sod);
    }

    @Test
    void aPersonCanOrderTwoSandwichesInTotal() {
        SandwichOrderDetail sod = request(1, 1, "Ada", "Lovelace");
        everythingExistsForAda();
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(1);

        service.addSandwichOrderDetail(sod);

        verify(detailRepository).save(sod);
    }

    @Test
    void aThirdSandwichForTheSamePersonIsRefused() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        everythingExistsForAda();
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(2);

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod))
                .isInstanceOf(MaxSandwichesExceededException.class)
                .hasMessageContaining("Ada Lovelace")
                .hasMessageContaining("2");
        verify(detailRepository, never()).save(any());
    }

    @Test
    void anUnknownSandwichIsRefusedAndNotRetried() {
        SandwichOrderDetail sod = request(999, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(999)).thenThrow(new SandwichNotFoundException(999));

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod)).isInstanceOf(SandwichNotFoundException.class);
        verify(sandwichService, times(1)).findSandwichById(999);
        verify(detailRepository, never()).save(any());
    }

    @Test
    void theTotalPriceOfTheOrderIsUpdatedWhenASandwichIsOrdered() {
        SandwichOrderDetail sod = request(1, 2, "Bob", "Builder");
        when(sandwichService.findSandwichById(1)).thenReturn(hesp);
        when(personService.findOrCreatePerson("Bob", "Builder")).thenReturn(bob);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findByOrderDateAndCourse(DAY, course)).thenReturn(Optional.of(existingOrder));
        when(detailRepository.save(sod)).thenReturn(sod);
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(stored(pinky, 1, ada, true, ""), sod));
        sod.setSandwich(hesp);

        service.addSandwichOrderDetail(sod);

        assertThat(existingOrder.getTotalPrice()).isEqualTo(11.2);   // 4.80 + 2 * 3.20
        verify(orderRepository).save(existingOrder);
    }

    // ---- retry when orders collide ----

    private void everythingExistsForAda() {
        when(sandwichService.findSandwichById(anyInt())).thenReturn(pinky);
        when(personService.findOrCreatePerson("Ada", "Lovelace")).thenReturn(ada);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findByOrderDateAndCourse(DAY, course)).thenReturn(Optional.of(existingOrder));
    }

    @Test
    void anOrderThatCollidesWithAnotherOrderIsTriedAgain() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        everythingExistsForAda();
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(0);
        when(detailRepository.save(sod))
                .thenThrow(new CannotAcquireLockException("could not serialize access"))
                .thenThrow(new DataIntegrityViolationException("unique constraint: another order created the same person"))
                .thenReturn(sod);

        SandwichOrderDetail saved = service.addSandwichOrderDetail(sod);

        assertThat(saved).isSameAs(sod);
        verify(detailRepository, times(3)).save(sod);
    }

    @Test
    void anOrderThatKeepsCollidingGivesUpWithTheLastException() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        everythingExistsForAda();
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(0);
        when(detailRepository.save(sod)).thenThrow(new CannotAcquireLockException("could not serialize access"));

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod)).isInstanceOf(CannotAcquireLockException.class);
        verify(detailRepository, times(4)).save(sod);   // 1 attempt + 3 retries
    }

    @Test
    void theMaximumPerPersonIsNotRetried() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        everythingExistsForAda();
        when(detailRepository.countSandwichesOfPerson(existingOrder, ada)).thenReturn(2);

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod)).isInstanceOf(MaxSandwichesExceededException.class);
        verify(personService, times(1)).findOrCreatePerson("Ada", "Lovelace");
    }

    // ---- calculateSandwichOrderPrice ----

    private void orderExists() {
        when(courseRepository.findByTitleIgnoreCase("Spring Boot")).thenReturn(Optional.of(course));
        when(orderRepository.findByOrderDateAndCourse(DAY, course)).thenReturn(Optional.of(existingOrder));
    }

    @Test
    void totalPriceIsTheSumOfBasePriceTimesAmountAndIsSaved() {
        orderExists();
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(
                stored(pinky, 1, ada, true, ""),
                stored(hesp, 2, bob, false, "")));

        double total = service.calculateSandwichOrderPrice(new SandwichOrder(DAY, new Course("Spring Boot")));

        assertThat(total).isEqualTo(11.2);
        verify(orderRepository).save(existingOrder);
        assertThat(existingOrder.getTotalPrice()).isEqualTo(11.2);
    }

    @Test
    void totalPriceDoesNotSufferFromFloatingPointErrors() {
        orderExists();
        Sandwich cheap = new Sandwich(5, "Cheap", "", "Vlees", 0.1);
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(
                stored(cheap, 1, ada, true, ""), stored(cheap, 1, bob, true, ""), stored(new Sandwich(6, "Other", "", "Vlees", 0.2), 1, bob, true, "")));

        assertThat(service.calculateSandwichOrderPrice(new SandwichOrder(DAY, new Course("Spring Boot")))).isEqualTo(0.4);
    }

    @Test
    void totalPriceOfAnOrderThatDoesNotExistIsRefused() {
        when(courseRepository.findByTitleIgnoreCase("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateSandwichOrderPrice(new SandwichOrder(DAY, new Course("Nope"))))
                .isInstanceOf(SandwichOrderNotFoundException.class)
                .hasMessageContaining("Nope");
    }

    // ---- printSandwichOrder ----

    @Test
    void printHandsTheOrderAndItsDetailsToThePrinterAndReturnsTheText() {
        orderExists();
        List<SandwichOrderDetail> details = List.of(stored(pinky, 1, ada, true, "no onions"));
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(details);
        when(orderPrinter.print(existingOrder, details)).thenReturn("SANDWICH ORDER ...");

        String text = service.printSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot")));

        assertThat(text).isEqualTo("SANDWICH ORDER ...");
    }

    @Test
    void printOfAnOrderThatDoesNotExistIsRefused() {
        when(courseRepository.findByTitleIgnoreCase("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.printSandwichOrder(new SandwichOrder(DAY, new Course("Nope"))))
                .isInstanceOf(SandwichOrderNotFoundException.class);
        verify(orderPrinter, never()).print(any(), any());
    }
}
