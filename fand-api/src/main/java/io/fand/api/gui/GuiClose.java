package io.fand.api.gui;

import io.fand.api.entity.Player;
import java.util.Objects;

/** Close context passed to GUI close handlers. */
public record GuiClose(GuiView view, Player player) {

    public GuiClose {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(player, "player");
    }
}
