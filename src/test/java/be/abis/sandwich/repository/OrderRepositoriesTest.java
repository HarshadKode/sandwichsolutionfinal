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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Course, person, order and order detail repositories together: they depend on each other's rows. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoriesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @Autowired private CourseRepository courseRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SandwichRepository sandwichRepository;
    @Autowired private SandwichOrderRepository orderRepository;
    @Autowired private SandwichOrderDetailRepository detailRepository;

    private Course course;
    private Person person;
    private Sandwich sandwich;

    @BeforeEach
    void createRows() {
        course = courseRepository.save(new Course("Spring Boot"));
        person = personRepository.save(new Person("Ada", "Lovelace"));
        sandwich = sandwichRepository.findByNameIgnoreCase("Pinky").orElseThrow();
    }

    private SandwichOrderDetail detail(SandwichOrder order, Person who, int amount, BreadType bread, boolean vegetables, String comment) {
        SandwichOrderDetail detail = new SandwichOrderDetail(sandwich, amount, bread, vegetables, comment, who);
        detail.setSandwichOrder(order);
        return detailRepository.save(detail);
    }

    @Test
    void courseCanBeFoundByTitleIgnoringCase() {
        assertThat(course.getId()).isPositive();
        assertThat(courseRepository.findByTitleIgnoreCase("spring BOOT")).get().extracting(Course::getId).isEqualTo(course.getId());
        assertThat(courseRepository.findByTitleIgnoreCase("Unknown")).isEmpty();
    }

    @Test
    void coursesComeSortedByTitle() {
        courseRepository.save(new Course("Zeta course"));
        courseRepository.save(new Course("Alpha course"));

        assertThat(courseRepository.findAllByOrderByTitleAsc())
                .extracting(Course::getTitle).containsExactly("Alpha course", "Spring Boot", "Zeta course");
    }

    @Test
    void personCanBeFoundByNameIgnoringCase() {
        assertThat(person.getId()).isPositive();
        assertThat(personRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("ADA", "lovelace"))
                .get().extracting(Person::getId).isEqualTo(person.getId());
        assertThat(personRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Ada", "Unknown")).isEmpty();
    }

    @Test
    void thereIsOnlyOneOrderPerCoursePerDay() {
        orderRepository.saveAndFlush(new SandwichOrder(DAY, course));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(new SandwichOrder(DAY, course)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void orderCanBeFoundByDateAndCourse() {
        SandwichOrder saved = orderRepository.save(new SandwichOrder(DAY, course));

        SandwichOrder found = orderRepository.findByOrderDateAndCourse(DAY, course).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getOrderDate()).isEqualTo(DAY);
        assertThat(found.getCourse().getTitle()).isEqualTo("Spring Boot");
        assertThat(found.getTotalPrice()).isZero();
        assertThat(orderRepository.findByOrderDateAndCourse(DAY.plusDays(1), course)).isEmpty();
    }

    @Test
    void totalPriceIsSavedInTheDatabase() {
        SandwichOrder order = orderRepository.save(new SandwichOrder(DAY, course));
        order.setTotalPrice(12.6);

        orderRepository.saveAndFlush(order);

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getTotalPrice()).isEqualTo(12.6);
    }

    @Test
    void detailsAreStoredAndFoundWithTheirSandwichAndPerson() {
        SandwichOrder order = orderRepository.save(new SandwichOrder(DAY, course));

        SandwichOrderDetail saved = detail(order, person, 2, BreadType.BROWN, false, "extra sauce");
        List<SandwichOrderDetail> found = detailRepository.findDetailsByOrder(order);

        assertThat(saved.getId()).isPositive();
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
    void detailsComeSortedByPersonName() {
        SandwichOrder order = orderRepository.save(new SandwichOrder(DAY, course));
        Person bob = personRepository.save(new Person("Bob", "Builder"));
        detail(order, person, 1, BreadType.WHITE, true, null);
        detail(order, bob, 1, BreadType.WHITE, true, null);

        assertThat(detailRepository.findDetailsByOrder(order))
                .extracting(d -> d.getPerson().getLastName()).containsExactly("Builder", "Lovelace");
    }

    @Test
    void countSandwichesOfPersonAddsUpTheAmountsOfThatPersonInThatOrder() {
        SandwichOrder order = orderRepository.save(new SandwichOrder(DAY, course));
        SandwichOrder otherOrder = orderRepository.save(new SandwichOrder(DAY.plusDays(1), course));
        Person bob = personRepository.save(new Person("Bob", "Builder"));
        assertThat(detailRepository.countSandwichesOfPerson(order, person)).isZero();

        detail(order, person, 1, BreadType.WHITE, true, null);
        detail(order, person, 1, BreadType.BROWN, true, null);
        detail(order, bob, 2, BreadType.WHITE, true, null);
        detail(otherOrder, person, 2, BreadType.WHITE, true, null);

        assertThat(detailRepository.countSandwichesOfPerson(order, person)).isEqualTo(2);
        assertThat(detailRepository.countSandwichesOfPerson(order, bob)).isEqualTo(2);
    }
}
