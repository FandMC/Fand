package io.fand.api.gui;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import net.kyori.adventure.text.Component;

/** Lightweight custom inventory GUI. */
public final class Gui {

    private final InventoryType type;
    private final int size;
    private final Component title;
    private final ItemStack[] contents;
    private final GuiSlotHandler[] handlers;
    private final boolean[] protectedSlots;
    private final Map<Integer, Integer> properties;
    private final GuiCloseHandler closeHandler;

    private Gui(Builder builder) {
        this.type = builder.type;
        this.size = builder.size;
        this.title = builder.title;
        this.contents = builder.contents.clone();
        this.handlers = builder.handlers.clone();
        this.protectedSlots = builder.protectedSlots.clone();
        this.properties = Map.copyOf(builder.properties);
        this.closeHandler = builder.closeHandler;
    }

    public static Builder chest(int rows, Component title) {
        return builder(InventoryType.CHEST, rows * 9, title);
    }

    public static Builder anvil(Component title) {
        return builder(InventoryType.ANVIL, 3, title);
    }

    public static Builder furnace(Component title) {
        return builder(InventoryType.FURNACE, 3, title);
    }

    public static Builder blastFurnace(Component title) {
        return builder(InventoryType.BLAST_FURNACE, 3, title);
    }

    public static Builder smoker(Component title) {
        return builder(InventoryType.SMOKER, 3, title);
    }

    public static Builder enchanting(Component title) {
        return builder(InventoryType.ENCHANTING, 2, title);
    }

    public static Builder brewing(Component title) {
        return builder(InventoryType.BREWING, 5, title);
    }

    public static Builder builder(InventoryType type, int size, Component title) {
        return new Builder(type, size, title);
    }

    public GuiView open(Player player) {
        return Fand.server().guis().open(player, this);
    }

    public Inventory createInventory() {
        var inventory = io.fand.api.inventory.Inventories.create(type, size, title);
        for (int slot = 0; slot < contents.length; slot++) {
            inventory.set(slot, contents[slot]);
        }
        return inventory;
    }

    public InventoryType type() {
        return type;
    }

    public int size() {
        return size;
    }

    public Component title() {
        return title;
    }

    public ItemStack item(int slot) {
        checkSlot(slot);
        return contents[slot];
    }

    public boolean protectedSlot(int slot) {
        checkSlot(slot);
        return protectedSlots[slot];
    }

    public boolean handles(int slot) {
        return slot >= 0 && slot < handlers.length && handlers[slot] != null;
    }

    public Map<Integer, Integer> properties() {
        return properties;
    }

    public void handle(GuiClick click) {
        Objects.requireNonNull(click, "click");
        if (click.slot() < 0 || click.slot() >= handlers.length) {
            return;
        }
        var handler = handlers[click.slot()];
        if (handler != null) {
            handler.click(click);
        }
    }

    public void close(GuiClose close) {
        if (closeHandler != null) {
            closeHandler.close(close);
        }
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IndexOutOfBoundsException("slot " + slot + " outside 0.." + (size - 1));
        }
    }

    public static final class Builder {

        private final InventoryType type;
        private final int size;
        private final Component title;
        private final ItemStack[] contents;
        private final GuiSlotHandler[] handlers;
        private final boolean[] protectedSlots;
        private final Map<Integer, Integer> properties = new LinkedHashMap<>();
        private GuiCloseHandler closeHandler;

        private Builder(InventoryType type, int size, Component title) {
            this.type = Objects.requireNonNull(type, "type");
            this.size = size;
            this.title = Objects.requireNonNull(title, "title");
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
            this.contents = new ItemStack[size];
            this.handlers = new GuiSlotHandler[size];
            this.protectedSlots = new boolean[size];
            java.util.Arrays.fill(contents, ItemStack.EMPTY);
        }

        public Builder item(int slot, ItemStack item) {
            checkSlot(slot);
            contents[slot] = Objects.requireNonNull(item, "item");
            return this;
        }

        public Builder handler(int slot, GuiSlotHandler handler) {
            checkSlot(slot);
            handlers[slot] = Objects.requireNonNull(handler, "handler");
            return this;
        }

        public Builder protectedSlot(int slot) {
            checkSlot(slot);
            protectedSlots[slot] = true;
            return this;
        }

        public Builder property(int id, int value) {
            if (id < 0) {
                throw new IllegalArgumentException("property id must be >= 0");
            }
            properties.put(id, value);
            return this;
        }

        public Builder button(int slot, ItemStack item, GuiSlotHandler handler) {
            return item(slot, item).handler(slot, handler).protectedSlot(slot);
        }

        public Builder page(int startSlot, int pageSize, int page, Collection<ItemStack> items) {
            Objects.requireNonNull(items, "items");
            var list = items.stream().toList();
            return page(startSlot, pageSize, page, list, list::get);
        }

        public <T> Builder page(int startSlot, int pageSize, int page, List<T> items, IntFunction<ItemStack> renderer) {
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(renderer, "renderer");
            if (page < 0) {
                throw new IllegalArgumentException("page must be >= 0");
            }
            for (int index = 0; index < pageSize; index++) {
                int slot = startSlot + index;
                checkSlot(slot);
                int itemIndex = page * pageSize + index;
                contents[slot] = itemIndex < items.size() ? renderer.apply(itemIndex) : ItemStack.EMPTY;
                protectedSlots[slot] = true;
            }
            return this;
        }

        public Builder onClose(GuiCloseHandler handler) {
            this.closeHandler = Objects.requireNonNull(handler, "handler");
            return this;
        }

        public Gui build() {
            return new Gui(this);
        }

        private void checkSlot(int slot) {
            if (slot < 0 || slot >= size) {
                throw new IndexOutOfBoundsException("slot " + slot + " outside 0.." + (size - 1));
            }
        }
    }
}
