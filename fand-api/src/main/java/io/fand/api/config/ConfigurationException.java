package io.fand.api.config;

/**
 * Thrown when a configuration cannot be parsed, loaded, or saved.
 *
 * <p>Type-mismatch on a single key never throws — typed getters return their
 * default instead. This exception is reserved for IO failures or syntactically
 * invalid documents.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
