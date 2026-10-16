package be.abis.sandwich.exception;

public class SandwichNotFoundException extends ResourceNotFoundException {

    public SandwichNotFoundException(int id) {
        super("Sandwich with id " + id + " does not exist");
    }
}
