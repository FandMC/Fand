package io.fand.api.gui;

import io.fand.api.entity.Player;
import java.util.Optional;
import java.util.UUID;

/** Runtime service that routes inventory events to open GUI views. */
public interface GuiService {

    GuiView open(Player player, Gui gui);

    Optional<GuiView> openView(Player player);

    Optional<GuiView> view(UUID id);
}
