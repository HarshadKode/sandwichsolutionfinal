package be.abis.sandwich.repository;

import be.abis.sandwich.model.Course;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DBCourseRepository implements CourseRepository {

    private final JdbcClient jdbc;

    public DBCourseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Course> findCourseByTitle(String title) {
        return jdbc.sql("SELECT id, title FROM abis_courses WHERE LOWER(title) = LOWER(:title)")
                .param("title", title)
                .query((rs, rowNum) -> new Course(rs.getInt("id"), rs.getString("title")))
                .optional();
    }

    @Override
    public Course addCourse(Course course) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO abis_courses (title) VALUES (:title)")
                .param("title", course.getTitle())
                .update(keys, "ID");
        course.setId(keys.getKey().intValue());
        return course;
    }
}
