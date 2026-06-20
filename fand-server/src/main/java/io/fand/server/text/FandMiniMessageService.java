package io.fand.server.text;

import io.fand.api.entity.Player;
import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.text.MiniMessageService;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jspecify.annotations.Nullable;

public final class FandMiniMessageService implements MiniMessageService {

    private final PlaceholderService placeholders;
    private final MiniMessage parser;

    public FandMiniMessageService(PlaceholderService placeholders) {
        this.placeholders = Objects.requireNonNull(placeholders, "placeholders");
        this.parser = MiniMessage.miniMessage();
    }

    @Override
    public MiniMessage parser() {
        return parser;
    }

    @Override
    public Component parse(String input) {
        return parse(null, input);
    }

    @Override
    public Component parse(String input, TagResolver... resolvers) {
        return parse(null, input, resolvers);
    }

    @Override
    public Component parse(@Nullable Player viewer, String input) {
        Objects.requireNonNull(input, "input");
        return parser.deserialize(placeholders.replace(viewer, input));
    }

    @Override
    public Component parse(@Nullable Player viewer, String input, TagResolver... resolvers) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(resolvers, "resolvers");
        return parser.deserialize(placeholders.replace(viewer, input), resolvers);
    }
}
