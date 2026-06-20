package io.fand.api.placeholder;

import io.fand.api.entity.Player;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Registry and resolver for percent-style placeholders such as
 * {@code %player_name%} or {@code %plugin_stat%}.
 */
public interface PlaceholderService {

    PlaceholderRegistration register(String namespace, PlaceholderProvider provider);

    Optional<String> resolve(@Nullable Player viewer, String identifier);

    default String replace(@Nullable Player viewer, String input) {
        Objects.requireNonNull(input, "input");
        var output = new StringBuilder(input.length());
        int cursor = 0;
        while (cursor < input.length()) {
            int start = input.indexOf('%', cursor);
            if (start < 0) {
                output.append(input, cursor, input.length());
                break;
            }
            int end = input.indexOf('%', start + 1);
            if (end < 0) {
                output.append(input, cursor, input.length());
                break;
            }
            output.append(input, cursor, start);
            var identifier = input.substring(start + 1, end);
            var replacement = identifier.isBlank() ? Optional.<String>empty() : resolve(viewer, identifier);
            output.append(replacement.orElseGet(() -> input.substring(start, end + 1)));
            cursor = end + 1;
        }
        return output.toString();
    }

    static PlaceholderService empty() {
        return new PlaceholderService() {
            @Override
            public PlaceholderRegistration register(String namespace, PlaceholderProvider provider) {
                throw new UnsupportedOperationException("Placeholders are not supported");
            }

            @Override
            public Optional<String> resolve(@Nullable Player viewer, String identifier) {
                Objects.requireNonNull(identifier, "identifier");
                return Optional.empty();
            }
        };
    }
}
