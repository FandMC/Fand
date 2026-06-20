package io.fand.api.placeholder;

import io.fand.api.entity.Player;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface PlaceholderProvider {

    @Nullable String resolve(@Nullable Player viewer, String identifier);
}
