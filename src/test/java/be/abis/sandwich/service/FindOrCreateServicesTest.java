package be.abis.sandwich.service;

import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.repository.CourseRepository;
import be.abis.sandwich.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindOrCreateServicesTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private PersonRepository personRepository;

    @Test
    void existingCourseIsReused() {
        Course existing = new Course(5, "Spring Boot");
        when(courseRepository.findCourseByTitle("Spring Boot")).thenReturn(Optional.of(existing));

        assertThat(new AbisCourseService(courseRepository).findOrCreateCourse("  Spring Boot ")).isSameAs(existing);
        verify(courseRepository, never()).addCourse(any());
    }

    @Test
    void unknownCourseIsCreated() {
        when(courseRepository.findCourseByTitle("Java")).thenReturn(Optional.empty());
        when(courseRepository.addCourse(any(Course.class))).thenAnswer(call -> {
            Course course = call.getArgument(0);
            course.setId(7);
            return course;
        });

        Course created = new AbisCourseService(courseRepository).findOrCreateCourse("Java");

        assertThat(created.getId()).isEqualTo(7);
        assertThat(created.getTitle()).isEqualTo("Java");
    }

    @Test
    void existingPersonIsReused() {
        Person existing = new Person(3, "Ada", "Lovelace");
        when(personRepository.findPersonByName("Ada", "Lovelace")).thenReturn(Optional.of(existing));

        assertThat(new AbisPersonService(personRepository).findOrCreatePerson(" Ada", "Lovelace ")).isSameAs(existing);
        verify(personRepository, never()).addPerson(any());
    }

    @Test
    void unknownPersonIsCreated() {
        when(personRepository.findPersonByName("Bob", "Builder")).thenReturn(Optional.empty());
        when(personRepository.addPerson(any(Person.class))).thenAnswer(call -> {
            Person person = call.getArgument(0);
            person.setId(9);
            return person;
        });

        Person created = new AbisPersonService(personRepository).findOrCreatePerson("Bob", "Builder");

        assertThat(created.getId()).isEqualTo(9);
        assertThat(created.getFirstName()).isEqualTo("Bob");
        assertThat(created.getLastName()).isEqualTo("Builder");
    }
}
