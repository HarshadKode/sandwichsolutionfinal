package be.abis.sandwich.repository;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Course, person, order and order detail repositories together: they depend on each other's rows. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DBCourseRepository.class, DBPersonRepository.class, DBSandwichRepository.class,
        DBSandwichOrderRepository.class, DBSandwichOrderDetailRepository.class})
class DBOrderRepositoriesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @Autowired private DBCourseRepository courseRepository;
    @Autowired private DBPersonRepository personRepository;
    @Autowired private DBSandwichRepository sandwichRepository;
    @Autowired private DBSandwichOrderRepository orderRepository;
    @Autowired private DBSandwichOrderDetailRepository detailRepository;

    private Course course;
    private Person person;
    private Sandwich sandwich;

    @BeforeEach
    void createRows() {
        course = courseRepository.addCourse(new Course("Spring Boot"));
        person = personRepository.addPerson(new Person("Ada", "Lovelace"));
        sandwich = sandwichRepository.findSandwichByName("Pinky").orElseThrow();
    }

    @Test
    void courseCanBeFoundByTitleIgnoringCase() {
        assertThat(course.getId()).isPositive();
        assertThat(courseRepository.findCourseByTitle("spring BOOT")).get().extracting(Course::getId).isEqualTo(course.getId());
        assertThat(courseRepository.findCourseByTitle("Unknown")).isEmpty();
    }

    @Test
    void personCanBeFoundByNameIgnoringCase() {
        assertThat(person.getId()).isPositive();
        assertThat(personRepository.findPersonByName("ADA", "lovelace")).get().extracting(Person::getId).isEqualTo(person.getId());
        assertThat(personRepository.findPersonByName("Ada", "Unknown")).isEmpty();
    }

    @Test
    void thereIsOnlyOneOrderPerCoursePerDay() {
        orderRepository.addSandwichOrder(new SandwichOrder(DAY, course));

        assertThatThrownBy(() -> orderRepository.addSandwichOrder(new SandwichOrder(DAY, course)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void orderCanBeFoundByDateAndCourse() {
        SandwichOrder added = orderRepository.addSandwichOrder(new SandwichOrder(DAY, course));

        SandwichOrder found = orderRepository.findSandwichOrder(DAY, course.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(added.getId());
        assertThat(found.getOrderDate()).isEqualTo(DAY);
        assertThat(found.getCourse().getTitle()).isEqualTo("Spring Boot");
        assertThat(found.getTotalPrice()).isZero();
        assertThat(orderRepository.findSandwichOrder(DAY.plusDays(1), course.getId())).isEmpty();
    }

    @Test
    void totalPriceIsSavedInTheDatabase() {
        SandwichOrder order = orderRepository.addSandwichOrder(new SandwichOrder(DAY, course));

        orderRepository.updateTotalPrice(order.getId(), 12.6);

        assertThat(orderRepository.findSandwichOrder(DAY, course.getId()).orElseThrow().getTotalPrice()).isEqualTo(12.6);
    }

    @Test
    void detailsAreStoredAndFoundWithTheirSandwichAndPerson() {
        SandwichOrder order = orderRepository.addSandwichOrder(new SandwichOrder(DAY, course));
        SandwichOrderDetail detail = new SandwichOrderDetail(sandwich, 2, BreadType.BROWN, false, "extra sauce", person);
        detail.setSandwichOrder(order);

        SandwichOrderDetail added = detailRepository.addSandwichOrderDetail(detail);
        List<SandwichOrderDetail> found = detailRepository.findDetailsByOrder(order);

        assertThat(added.getId()).isPositive();
        assertThat(found).hasSize(1);
        SandwichOrderDetail stored = found.get(0);
        assertThat(stored.getSandwich().getName()).isEqualTo("Pinky");
        assertThat(stored.getPerson()).isEqualTo(person);
        assertThat(stored.getAmount()).isEqualTo(2);
        assertThat(stored.getBreadType()).isEqualTo(BreadType.BROWN);
        assertThat(stored.isVegetables()).isFalse();
        assertThat(stored.getComment()).isEqualTo("extra sauce");
    }

    @Test
    void countSandwichesOfPersonAddsUpTheAmounts() {
        SandwichOrder order = orderRepository.addSandwichOrder(new SandwichOrder(DAY, course));
        assertThat(detailRepository.countSandwichesOfPerson(order.getId(), person.getId())).isZero();

        for (int amount : new int[]{1, 1}) {
            SandwichOrderDetail detail = new SandwichOrderDetail(sandwich, amount, BreadType.WHITE, true, null, person);
            detail.setSandwichOrder(order);
            detailRepository.addSandwichOrderDetail(detail);
        }

        assertThat(detailRepository.countSandwichesOfPerson(order.getId(), person.getId())).isEqualTo(2);
    }
}
