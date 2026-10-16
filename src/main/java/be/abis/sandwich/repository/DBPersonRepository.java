package be.abis.sandwich.repository;

import be.abis.sandwich.model.Person;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DBPersonRepository implements PersonRepository {

    private final JdbcClient jdbc;

    public DBPersonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Person> findPersonByName(String firstName, String lastName) {
        return jdbc.sql("SELECT id, first_name, last_name FROM abis_persons " +
                        "WHERE LOWER(first_name) = LOWER(:firstName) AND LOWER(last_name) = LOWER(:lastName)")
                .param("firstName", firstName)
                .param("lastName", lastName)
                .query((rs, rowNum) -> new Person(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name")))
                .optional();
    }

    @Override
    public Person addPerson(Person person) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO abis_persons (first_name, last_name) VALUES (:firstName, :lastName)")
                .param("firstName", person.getFirstName())
                .param("lastName", person.getLastName())
                .update(keys, "ID");
        person.setId(keys.getKey().intValue());
        return person;
    }
}
