package io.fand.api.text;

import io.fand.api.entity.Player;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.Nullable;

/**
 * MiniMessage parser for RGB, gradients, hover/click events, fonts, and other
 * Adventure-supported text tags.
 */
public interface MiniMessageService {

    MiniMessage parser();

    default Component parse(String input) {
        return parser().deserialize(Objects.requireNonNull(input, "input"));
    }

    default Component parse(String input, TagResolver... resolvers) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(resolvers, "resolvers");
        return parser().deserialize(input, resolvers);
    }

    default Component parse(@Nullable Player viewer, String input) {
        return parse(input);
    }

    default Component parse(@Nullable Player viewer, String input, TagResolver... resolvers) {
        return parse(input, resolvers);
    }

    default String serialize(Component component) {
        return parser().serialize(Objects.requireNonNull(component, "component"));
    }

    default String escapeTags(String input) {
        return parser().escapeTags(Objects.requireNonNull(input, "input"));
    }

    default String stripTags(String input) {
        return parser().stripTags(Objects.requireNonNull(input, "input"));
    }

    static MiniMessageService empty() {
        return () -> MiniMessage.miniMessage();
    }
}
