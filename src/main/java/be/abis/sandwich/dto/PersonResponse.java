package be.abis.sandwich.dto;

import be.abis.sandwich.model.Person;

/** A person as the API shows it. */
public record PersonResponse(String firstName, String lastName) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getFirstName(), person.getLastName());
    }
}
