package be.abis.sandwich.exception;

public class MaxSandwichesExceededException extends ConflictException {

    public MaxSandwichesExceededException(String personName, int max) {
        super(personName + " can order at most " + max + " sandwiches per order");
    }
}
