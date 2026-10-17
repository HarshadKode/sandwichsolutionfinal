package be.abis.sandwich.controller;

import be.abis.sandwich.model.Course;
import be.abis.sandwich.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CourseService courseService;

    @Test
    void getReturnsTheCoursesAndOnlyTheirTitle() throws Exception {
        when(courseService.findAllCourses()).thenReturn(List.of(new Course(1, "Java"), new Course(2, "Spring Boot")));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Java"))
                .andExpect(jsonPath("$[1].title").value("Spring Boot"))
                .andExpect(jsonPath("$[0]", aMapWithSize(1)));   // no id: that is a detail of the database
    }

    @Test
    void getReturnsAnEmptyListWhenThereAreNoCourses() throws Exception {
        when(courseService.findAllCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
