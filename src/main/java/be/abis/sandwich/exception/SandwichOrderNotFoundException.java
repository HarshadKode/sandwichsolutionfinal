package be.abis.sandwich.exception;

import java.time.LocalDate;

public class SandwichOrderNotFoundException extends ResourceNotFoundException {

    public SandwichOrderNotFoundException(LocalDate orderDate, String courseTitle) {
        super("There is no sandwich order on " + orderDate + " for course '" + courseTitle + "'");
    }
}
