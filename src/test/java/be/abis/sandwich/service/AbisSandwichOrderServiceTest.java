package be.abis.sandwich.service;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.exception.MaxSandwichesExceededException;
import be.abis.sandwich.exception.PrintOrderException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @TempDir
    private Path printDirectory;

    private AbisSandwichOrderService service;

    private final Course course = new Course(1, "Spring Boot");
    private final Person ada = new Person(10, "Ada", "Lovelace");
    private final Person bob = new Person(11, "Bob", "Builder");
    private final Sandwich pinky = new Sandwich(21, "Pinky", "Special", "Specials", 4.8);
    private final Sandwich hesp = new Sandwich(1, "Hesp", "Ham", "Vlees", 3.2);
    private final SandwichOrder existingOrder = orderWithId(100);

    @BeforeEach
    void setUp() {
        service = new AbisSandwichOrderService(sandwichService, courseService, personService, courseRepository,
                orderRepository, detailRepository, TransactionOperations.withoutTransaction(), printDirectory.toString());
    }

    private SandwichOrder orderWithId(int id) {
        SandwichOrder order = new SandwichOrder(DAY, course);
        order.setId(id);
        return order;
    }

    /** What the controller receives: only ids / names, no database ids. */
    private SandwichOrderDetail request(int sandwichId, int amount, String first, String last) {
        Sandwich requested = new Sandwich();
        requested.setId(sandwichId);
        SandwichOrderDetail sod = new SandwichOrderDetail(requested, amount, BreadType.WHITE, true, "", new Person(first, last));
        sod.setSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot")));
        return sod;
    }

    // ---- addSandwichOrderDetail ----

    @Test
    void firstSandwichCreatesTheOrderAndIsStoredWithDatabaseData() {
        SandwichOrderDetail sod = request(21, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(21)).thenReturn(pinky);
        when(personService.findOrCreatePerson("Ada", "Lovelace")).thenReturn(ada);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findSandwichOrder(DAY, 1)).thenReturn(Optional.empty());
        when(orderRepository.addSandwichOrder(any(SandwichOrder.class))).thenReturn(existingOrder);
        when(detailRepository.countSandwichesOfPerson(100, 10)).thenReturn(0);

        service.addSandwichOrderDetail(sod);

        verify(detailRepository).addSandwichOrderDetail(sod);
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
        when(orderRepository.findSandwichOrder(DAY, 1)).thenReturn(Optional.of(existingOrder));
        when(detailRepository.countSandwichesOfPerson(100, 11)).thenReturn(0);

        service.addSandwichOrderDetail(sod);

        verify(orderRepository, never()).addSandwichOrder(any());
        verify(detailRepository).addSandwichOrderDetail(sod);
    }

    @Test
    void aPersonCanOrderTwoSandwichesInTotal() {
        SandwichOrderDetail sod = request(1, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(1)).thenReturn(hesp);
        when(personService.findOrCreatePerson("Ada", "Lovelace")).thenReturn(ada);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findSandwichOrder(DAY, 1)).thenReturn(Optional.of(existingOrder));
        when(detailRepository.countSandwichesOfPerson(100, 10)).thenReturn(1);

        service.addSandwichOrderDetail(sod);

        verify(detailRepository).addSandwichOrderDetail(sod);
    }

    @Test
    void aThirdSandwichForTheSamePersonIsRefused() {
        SandwichOrderDetail sod = request(1, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(1)).thenReturn(hesp);
        when(personService.findOrCreatePerson("Ada", "Lovelace")).thenReturn(ada);
        when(courseService.findOrCreateCourse("Spring Boot")).thenReturn(course);
        when(orderRepository.findSandwichOrder(DAY, 1)).thenReturn(Optional.of(existingOrder));
        when(detailRepository.countSandwichesOfPerson(100, 10)).thenReturn(2);

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod))
                .isInstanceOf(MaxSandwichesExceededException.class)
                .hasMessageContaining("Ada Lovelace")
                .hasMessageContaining("2");
        verify(detailRepository, never()).addSandwichOrderDetail(any());
    }

    @Test
    void anUnknownSandwichIsRefused() {
        SandwichOrderDetail sod = request(999, 1, "Ada", "Lovelace");
        when(sandwichService.findSandwichById(999)).thenThrow(new SandwichNotFoundException(999));

        assertThatThrownBy(() -> service.addSandwichOrderDetail(sod)).isInstanceOf(SandwichNotFoundException.class);
        verify(detailRepository, never()).addSandwichOrderDetail(any());
    }

    // ---- calculateSandwichOrderPrice ----

    private SandwichOrderDetail stored(Sandwich sandwich, int amount, Person person, boolean vegetables, String comment) {
        return new SandwichOrderDetail(sandwich, amount, BreadType.BROWN, vegetables, comment, person);
    }

    private void orderExists() {
        when(courseRepository.findCourseByTitle("Spring Boot")).thenReturn(Optional.of(course));
        when(orderRepository.findSandwichOrder(DAY, 1)).thenReturn(Optional.of(existingOrder));
    }

    @Test
    void totalPriceIsTheSumOfBasePriceTimesAmountAndIsSaved() {
        orderExists();
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(
                stored(pinky, 1, ada, true, ""),
                stored(hesp, 2, bob, false, "")));

        double total = service.calculateSandwichOrderPrice(new SandwichOrder(DAY, new Course("Spring Boot")));

        assertThat(total).isEqualTo(11.2);
        verify(orderRepository).updateTotalPrice(100, 11.2);
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
        when(courseRepository.findCourseByTitle("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateSandwichOrderPrice(new SandwichOrder(DAY, new Course("Nope"))))
                .isInstanceOf(SandwichOrderNotFoundException.class)
                .hasMessageContaining("Nope");
    }

    // ---- printSandwichOrder ----

    @Test
    void printWritesAReadableTextFileAndReturnsTheText() throws IOException {
        orderExists();
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(
                stored(pinky, 1, ada, true, "no onions"),
                stored(hesp, 1, ada, false, ""),
                stored(hesp, 2, bob, true, "")));

        String text = service.printSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot")));

        Path file = printDirectory.resolve("sandwich-order_2026-10-16_spring-boot.txt");
        assertThat(file).exists();
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(text);
        assertThat(text)
                .contains("Course: Spring Boot", "Date:   2026-10-16")
                .contains("Ada Lovelace", "Bob Builder")
                .contains("1x Pinky", "remark: no onions")
                .contains("1x Hesp")
                .contains("Total: € 14.40")
                .doesNotContain("\r");   // the same line endings on every platform
    }

    @Test
    void printSumsTheSameSandwichInTheSummary() {
        orderExists();
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(
                stored(hesp, 1, ada, true, ""),
                stored(hesp, 2, bob, true, "")));

        String text = service.printSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot")));

        assertThat(text).contains("3x Hesp");
    }

    @Test
    void printFailsWithAClearErrorWhenTheFileCannotBeWritten() throws IOException {
        Path notADirectory = Files.createFile(printDirectory.resolve("blocked"));
        AbisSandwichOrderService broken = new AbisSandwichOrderService(sandwichService, courseService, personService,
                courseRepository, orderRepository, detailRepository, TransactionOperations.withoutTransaction(), notADirectory.toString());
        orderExists();
        when(detailRepository.findDetailsByOrder(existingOrder)).thenReturn(List.of(stored(hesp, 1, ada, true, "")));

        assertThatThrownBy(() -> broken.printSandwichOrder(new SandwichOrder(DAY, new Course("Spring Boot"))))
                .isInstanceOf(PrintOrderException.class);
    }

    @Test
    void printOfAnOrderThatDoesNotExistIsRefused() {
        when(courseRepository.findCourseByTitle("Nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.printSandwichOrder(new SandwichOrder(DAY, new Course("Nope"))))
                .isInstanceOf(SandwichOrderNotFoundException.class);
    }
}
