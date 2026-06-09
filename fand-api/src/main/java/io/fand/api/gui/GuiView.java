package io.fand.api.gui;

import io.fand.api.entity.Player;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Optional;
import java.util.UUID;

/** Live handle for an opened GUI. */
public interface GuiView extends AutoCloseable {

    UUID id();

    Player player();

    Gui gui();

    Inventory inventory();

    boolean open();

    default ItemStack cursorItem() {
        return player().cursorItem();
    }

    default void setCursorItem(ItemStack item) {
        player().setCursorItem(item);
    }

    default void setProperty(int id, int value) {
        throw new UnsupportedOperationException("GUI properties are not supported");
    }

    void reopen();

    @Override
    void close();

    Optional<Object> state(String key);

    void state(String key, Object value);

    void removeState(String key);
}
