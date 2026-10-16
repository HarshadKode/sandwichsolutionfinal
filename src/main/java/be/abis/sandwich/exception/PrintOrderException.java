package be.abis.sandwich.exception;

/** The order could not be written to its .txt file (HTTP 500). */
public class PrintOrderException extends RuntimeException {

    public PrintOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
