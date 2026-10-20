package be.abis.sandwich.controller;

import be.abis.sandwich.dto.CourseResponse;
import be.abis.sandwich.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Talks DTOs only: the entities of the model package never leave the service layer. */
@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "The courses that sandwiches are ordered for")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @Operation(summary = "List the courses", description = "All courses, sorted by title. A course exists as soon as the first sandwich is ordered for it.")
    public List<CourseResponse> findCourses() {
        return courseService.findAllCourses().stream().map(CourseResponse::from).toList();
    }
}
