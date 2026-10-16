package be.abis.sandwich.exception;

public class DuplicateSandwichException extends ConflictException {

    public DuplicateSandwichException(String name) {
        super("A sandwich called '" + name + "' already exists");
    }
}
