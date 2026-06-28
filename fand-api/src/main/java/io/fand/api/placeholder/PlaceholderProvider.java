package io.fand.api.placeholder;

import io.fand.api.entity.Player;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface PlaceholderProvider {

    @Nullable String resolve(@Nullable Player viewer, String identifier);

    default @Nullable String resolve(PlaceholderContext context, String identifier) {
        Objects.requireNonNull(context, "context");
        return resolve(context.viewer(), identifier);
    }

    static PlaceholderProvider contextual(Contextual resolver) {
        Objects.requireNonNull(resolver, "resolver");
        return new PlaceholderProvider() {
            @Override
            public @Nullable String resolve(@Nullable Player viewer, String identifier) {
                return resolver.resolve(PlaceholderContext.viewer(viewer), identifier);
            }

            @Override
            public @Nullable String resolve(PlaceholderContext context, String identifier) {
                return resolver.resolve(context, identifier);
            }
        };
    }

    @FunctionalInterface
    interface Contextual {

        @Nullable String resolve(PlaceholderContext context, String identifier);
    }
}
