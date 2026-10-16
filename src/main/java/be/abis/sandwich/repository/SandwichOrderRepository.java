package be.abis.sandwich.repository;

import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.SandwichOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SandwichOrderRepository extends JpaRepository<SandwichOrder, Integer> {

    /** There is one order per course per day. */
    Optional<SandwichOrder> findByOrderDateAndCourse(LocalDate orderDate, Course course);
}
