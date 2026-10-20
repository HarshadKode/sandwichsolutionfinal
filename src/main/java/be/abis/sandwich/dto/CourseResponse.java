package be.abis.sandwich.dto;

import be.abis.sandwich.model.Course;

/** A course as the API shows it. The id is a detail of the database, so it is not shown. */
public record CourseResponse(String title) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getTitle());
    }
}
