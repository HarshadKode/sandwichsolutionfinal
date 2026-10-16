package be.abis.sandwich.service;

import be.abis.sandwich.model.Course;
import be.abis.sandwich.repository.CourseRepository;
import org.springframework.stereotype.Service;

@Service
public class AbisCourseService implements CourseService {

    private final CourseRepository courseRepository;

    public AbisCourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public Course findOrCreateCourse(String title) {
        String trimmed = title.trim();
        return courseRepository.findCourseByTitle(trimmed)
                .orElseGet(() -> courseRepository.addCourse(new Course(trimmed)));
    }
}
