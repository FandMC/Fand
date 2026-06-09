package io.fand.api.gui;

import io.fand.api.entity.Player;
import io.fand.api.inventory.Inventory;
import java.util.Optional;
import java.util.UUID;

/** Live handle for an opened GUI. */
public interface GuiView extends AutoCloseable {

    UUID id();

    Player player();

    Gui gui();

    Inventory inventory();

    boolean open();

    void reopen();

    @Override
    void close();

    Optional<Object> state(String key);

    void state(String key, Object value);

    void removeState(String key);
}
