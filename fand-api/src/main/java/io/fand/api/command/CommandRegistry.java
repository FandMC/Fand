package io.fand.api.command;

import java.util.List;
import java.util.Optional;

/**
 * Registry for server commands. Plugin commands should be registered during
 * {@link io.fand.api.plugin.Plugin#onEnable} and unregistered automatically on
 * disable.
 */
public interface CommandRegistry {

    /**
     * Registers {@code handler} under {@code label} plus any aliases. The label
     * is the canonical name; aliases route to the same handler.
     *
     * @throws IllegalStateException if any of the names are already registered
     */
    void register(String label, List<String> aliases, CommandHandler handler);

    Optional<CommandHandler> lookup(String label);

    void unregister(String label);
}
