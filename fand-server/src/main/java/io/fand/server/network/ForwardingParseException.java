package io.fand.server.network;

public final class ForwardingParseException extends RuntimeException {

    public ForwardingParseException(String message) {
        super(message);
    }

    public ForwardingParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
