package be.abis.sandwich.exception;

/** Something that was asked for does not exist (HTTP 404). */
public abstract class ResourceNotFoundException extends RuntimeException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
