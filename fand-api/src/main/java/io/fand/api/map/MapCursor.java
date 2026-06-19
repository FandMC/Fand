package io.fand.api.map;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public record MapCursor(
        MapCursorType type,
        byte x,
        byte y,
        byte rotation,
        @Nullable Component name
) {
    public MapCursor {
        rotation = (byte) (rotation & 15);
    }

    public Optional<Component> displayName() {
        return Optional.ofNullable(name);
    }
}
