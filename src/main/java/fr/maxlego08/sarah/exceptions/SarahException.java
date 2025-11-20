package fr.maxlego08.sarah.exceptions;

/**
 * Base exception for all Sarah library exceptions.
 */
public class SarahException extends RuntimeException {

    public SarahException(String message) {
        super(message);
    }

    public SarahException(String message, Throwable cause) {
        super(message, cause);
    }
}