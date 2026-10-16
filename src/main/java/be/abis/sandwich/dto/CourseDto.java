package be.abis.sandwich.dto;

import be.abis.sandwich.model.Course;
import jakarta.validation.constraints.NotBlank;

public record CourseDto(@NotBlank(message = "course title is required") String title) {

    public static CourseDto from(Course course) {
        return new CourseDto(course.getTitle());
    }

    public Course toEntity() {
        return new Course(title.trim());
    }
}
