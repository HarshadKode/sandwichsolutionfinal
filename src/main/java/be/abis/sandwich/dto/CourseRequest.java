package be.abis.sandwich.dto;

import be.abis.sandwich.model.Course;
import jakarta.validation.constraints.NotBlank;

/** A course in a request: only its title. */
public record CourseRequest(@NotBlank(message = "course title is required") String title) {

    public Course toEntity() {
        return new Course(title.trim());
    }
}
