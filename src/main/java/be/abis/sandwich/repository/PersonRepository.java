package be.abis.sandwich.repository;

import be.abis.sandwich.model.Person;

import java.util.Optional;

public interface PersonRepository {

    Optional<Person> findPersonByName(String firstName, String lastName);
    /** Inserts the person and returns it with its generated id. */
    Person addPerson(Person person);

}
