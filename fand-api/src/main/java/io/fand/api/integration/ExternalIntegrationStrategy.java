package io.fand.api.integration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Registry-facing strategy for external integrations. Core Fand keeps the
 * contract API-only; concrete MySQL, Redis, RabbitMQ, or other clients live in
 * plugins or distribution modules.
 */
public interface ExternalIntegrationStrategy {

    Collection<ExternalIntegration> integrations();

    default Optional<ExternalIntegration> integration(Key key) {
        java.util.Objects.requireNonNull(key, "key");
        return integrations().stream()
                .filter(integration -> integration.key().equals(key))
                .findFirst();
    }

    static ExternalIntegrationStrategy empty() {
        return List::of;
    }
}
