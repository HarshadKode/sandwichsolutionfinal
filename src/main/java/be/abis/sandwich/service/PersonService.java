package be.abis.sandwich.service;

import be.abis.sandwich.model.Person;

public interface PersonService {

    /** Returns the person with this name; creates them first if they don't exist yet. */
    Person findOrCreatePerson(String firstName, String lastName);
}
