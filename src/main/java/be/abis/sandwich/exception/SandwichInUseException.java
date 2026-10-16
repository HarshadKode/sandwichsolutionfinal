package be.abis.sandwich.exception;

public class SandwichInUseException extends ConflictException {

    public SandwichInUseException(String name) {
        super("Sandwich '" + name + "' is part of existing orders and cannot be removed");
    }
}
