package be.abis.sandwich.dto;

import be.abis.sandwich.model.Person;
import jakarta.validation.constraints.NotBlank;

public record PersonDto(
        @NotBlank(message = "first name is required") String firstName,
        @NotBlank(message = "last name is required") String lastName) {

    public static PersonDto from(Person person) {
        return new PersonDto(person.getFirstName(), person.getLastName());
    }

    public Person toEntity() {
        return new Person(firstName.trim(), lastName.trim());
    }
}
