package io.fand.api.integration;

import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Declarative handle for an external service integration such as SQL, Redis, or
 * message queues.
 */
public record ExternalIntegration(
        Key key,
        ExternalIntegrationKind kind,
        Map<String, String> properties
) {

    public ExternalIntegration {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
    }
}
