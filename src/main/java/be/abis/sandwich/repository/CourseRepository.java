package be.abis.sandwich.repository;

import be.abis.sandwich.model.Course;

import java.util.Optional;

public interface CourseRepository {

    Optional<Course> findCourseByTitle(String title);
    /** Inserts the course and returns it with its generated id. */
    Course addCourse(Course course);

}
