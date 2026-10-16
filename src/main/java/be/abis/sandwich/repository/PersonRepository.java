package be.abis.sandwich.repository;

import be.abis.sandwich.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}
