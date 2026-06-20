package io.fand.api.nbs;

/** Thrown when a Note Block Studio binary cannot be parsed. */
public final class NbsParseException extends RuntimeException {

    public NbsParseException(String message) {
        super(message);
    }

    public NbsParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
