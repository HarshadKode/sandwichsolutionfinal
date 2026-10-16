package be.abis.sandwich.repository;

import be.abis.sandwich.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByTitleIgnoreCase(String title);
}
