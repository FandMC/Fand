package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired before a player edits or signs a writable book.
 */
public final class PlayerEditBookEvent implements Event, Cancellable {

    private final Player player;
    private final int slot;
    private final ItemStack previousBook;
    private ItemStack newBook;
    private final @Nullable String title;
    private final boolean signing;
    private boolean cancelled;

    public PlayerEditBookEvent(Player player, int slot, ItemStack previousBook, ItemStack newBook, @Nullable String title, boolean signing) {
        this.player = Objects.requireNonNull(player, "player");
        this.slot = slot;
        this.previousBook = Objects.requireNonNull(previousBook, "previousBook");
        this.newBook = Objects.requireNonNull(newBook, "newBook");
        this.title = title;
        this.signing = signing;
    }

    public Player player() {
        return player;
    }

    public int slot() {
        return slot;
    }

    public ItemStack previousBook() {
        return previousBook;
    }

    public ItemStack newBook() {
        return newBook;
    }

    public void setNewBook(ItemStack newBook) {
        this.newBook = Objects.requireNonNull(newBook, "newBook");
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public boolean signing() {
        return signing;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
