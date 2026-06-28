package io.fand.api.service;

import java.util.Objects;
import net.kyori.adventure.key.Key;

public record ServiceProvider<T>(
        Key key,
        Class<T> type,
        T service,
        String owner,
        ServicePriority priority
) {
    public ServiceProvider {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        service = type.cast(Objects.requireNonNull(service, "service"));
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner cannot be blank");
        }
        priority = Objects.requireNonNull(priority, "priority");
    }
}
