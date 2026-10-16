package be.abis.sandwich.service;

import be.abis.sandwich.model.Person;
import be.abis.sandwich.repository.PersonRepository;
import org.springframework.stereotype.Service;

@Service
public class AbisPersonService implements PersonService {

    private final PersonRepository personRepository;

    public AbisPersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public Person findOrCreatePerson(String firstName, String lastName) {
        String first = firstName.trim();
        String last = lastName.trim();
        return personRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(first, last)
                .orElseGet(() -> personRepository.save(new Person(first, last)));
    }
}
