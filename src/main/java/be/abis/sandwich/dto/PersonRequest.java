package be.abis.sandwich.dto;

import be.abis.sandwich.model.Person;
import jakarta.validation.constraints.NotBlank;

/** A person in a request. */
public record PersonRequest(
        @NotBlank(message = "first name is required") String firstName,
        @NotBlank(message = "last name is required") String lastName) {

    public Person toEntity() {
        return new Person(firstName.trim(), lastName.trim());
    }
}
