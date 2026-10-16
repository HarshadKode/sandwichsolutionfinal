package be.abis.sandwich.exception;

/** The request is valid, but conflicts with the current state or a business rule (HTTP 409). */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
