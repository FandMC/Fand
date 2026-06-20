package io.fand.server.gui;

import io.fand.api.entity.Player;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.gui.Gui;
import io.fand.api.gui.GuiClick;
import io.fand.api.gui.GuiClose;
import io.fand.api.gui.GuiService;
import io.fand.api.gui.GuiView;
import io.fand.api.inventory.Inventory;
import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandGuiService implements GuiService, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandGuiService.class);

    private final ConcurrentHashMap<UUID, FandGuiView> viewsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, FandGuiView> viewsByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Runnable>> closeListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicInteger> ignoredCloseEvents = new ConcurrentHashMap<>();
    private final EventSubscription clickSubscription;
    private final EventSubscription closeSubscription;

    // Serializes open, handleClose, finishOpen, and close so a view being
    // installed or torn down is never observed half-applied by a concurrent
    // close event (or vice versa). Gui close callbacks may reenter open from
    // the same thread; synchronized is reentrant.
    private final Object lock = new Object();

    public FandGuiService(EventBus events) {
        this.clickSubscription = events.subscribe(InventoryClickEvent.class, EventPriority.HIGHEST, this::handleClick);
        this.closeSubscription = events.subscribe(InventoryCloseEvent.class, EventPriority.NORMAL, this::handleClose);
    }

    @Override
    public GuiView open(Player player, Gui gui) {
        java.util.Objects.requireNonNull(player, "player");
        java.util.Objects.requireNonNull(gui, "gui");
        var view = new FandGuiView(this, player, gui, gui.createInventory());
        synchronized (lock) {
            var previous = viewsByPlayer.put(player.uniqueId(), view);
            // Register the new view before closing the previous one so a reentrant
            // open from the previous gui's close callback cannot leak a detached
            // view back into viewsById.
            viewsById.put(view.id(), view);
            boolean replacedDisplayedView = false;
            if (previous != null) {
                viewsById.remove(previous.id(), previous);
                if (previous.displayed()) {
                    ignoreNextCloseEvent(player.uniqueId());
                    replacedDisplayedView = true;
                }
                closeReplaced(previous);
            }
            boolean skipCloseEventOnFailure = replacedDisplayedView;
            player.openInventory(view.inventory())
                    .whenComplete((opened, failure) -> finishOpen(view, opened, failure, skipCloseEventOnFailure));
            return view;
        }
    }

    @Override
    public Optional<GuiView> openView(Player player) {
        java.util.Objects.requireNonNull(player, "player");
        return Optional.ofNullable(viewsByPlayer.get(player.uniqueId()));
    }

    @Override
    public Collection<GuiView> openViews(Gui gui) {
        java.util.Objects.requireNonNull(gui, "gui");
        return viewsById.values().stream()
                .filter(view -> view.open() && view.gui() == gui)
                .map(GuiView.class::cast)
                .toList();
    }

    @Override
    public Optional<GuiView> view(UUID id) {
        java.util.Objects.requireNonNull(id, "id");
        return Optional.ofNullable(viewsById.get(id));
    }

    @Override
    public void close() {
        clickSubscription.unregister();
        closeSubscription.unregister();
        synchronized (lock) {
            for (var view : viewsById.values()) {
                view.detach();
                notifyCloseListeners(view);
            }
            viewsById.clear();
            viewsByPlayer.clear();
            closeListeners.clear();
            ignoredCloseEvents.clear();
        }
    }

    public AutoCloseable addCloseListener(GuiView view, Runnable listener) {
        java.util.Objects.requireNonNull(view, "view");
        java.util.Objects.requireNonNull(listener, "listener");
        var listeners = closeListeners.computeIfAbsent(view.id(), ignored -> new CopyOnWriteArrayList<>());
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    void remove(FandGuiView view) {
        viewsById.remove(view.id(), view);
        viewsByPlayer.remove(view.player().uniqueId(), view);
        view.detach();
        notifyCloseListeners(view);
    }

    private void closeReplaced(FandGuiView view) {
        view.detach();
        notifyCloseListeners(view);
        view.gui().close(new GuiClose(view, view.player()));
    }

    private boolean removeIfCurrent(FandGuiView view) {
        if (viewsByPlayer.remove(view.player().uniqueId(), view)) {
            viewsById.remove(view.id(), view);
            view.detach();
            notifyCloseListeners(view);
            return true;
        } else {
            view.detach();
            viewsById.remove(view.id(), view);
            return false;
        }
    }

    private boolean isCurrent(FandGuiView view) {
        return viewsByPlayer.get(view.player().uniqueId()) == view && viewsById.get(view.id()) == view;
    }

    private void finishOpen(
            FandGuiView view,
            Boolean opened,
            Throwable failure,
            boolean skipCloseEventOnFailure
    ) {
        synchronized (lock) {
            if (failure != null) {
                LOGGER.warn("GUI open failed", failure);
                if (skipCloseEventOnFailure) {
                    removeIgnoredCloseEvent(view.player().uniqueId());
                }
                removeIfCurrent(view);
                return;
            }
            if (!Boolean.TRUE.equals(opened)) {
                if (skipCloseEventOnFailure) {
                    removeIgnoredCloseEvent(view.player().uniqueId());
                }
                removeIfCurrent(view);
                return;
            }
            if (isCurrent(view)) {
                view.markDisplayed();
                view.applyContentsToOpenMenu();
                view.applyInitialProperties();
            }
        }
    }

    private void closeAfterFailedReopen(FandGuiView view) {
        if (removeIfCurrent(view)) {
            view.gui().close(new GuiClose(view, view.player()));
        }
    }

    private void closeAfterFailedReopen(FandGuiView view, Throwable failure) {
        LOGGER.warn("GUI reopen failed", failure);
        closeAfterFailedReopen(view);
    }

    void closeProgrammatically(FandGuiView view) {
        if (!view.open()) {
            return;
        }
        remove(view);
        view.gui().close(new GuiClose(view, view.player()));
        view.player().closeInventory();
    }

    private void handleClick(InventoryClickEvent event) {
        var view = viewsByPlayer.get(event.player().uniqueId());
        if (view == null || !view.open()) {
            return;
        }
        if (event.slot() < 0 || event.slot() >= view.gui().size()) {
            return;
        }
        if (view.gui().protectedSlot(event.slot()) || view.gui().handles(event.slot())) {
            event.setCancelled(true);
        }
        if (view.gui().handles(event.slot())) {
            view.gui().handle(new GuiClick(
                    view,
                    event.player(),
                    event.inventory(),
                    event.slot(),
                    event.clickType(),
                    event.action(),
                    event.currentItem(),
                    event.cursorItem()));
        }
    }

    private void handleClose(InventoryCloseEvent event) {
        if (removeIgnoredCloseEvent(event.player().uniqueId())) {
            return;
        }
        final FandGuiView view;
        synchronized (lock) {
            var current = viewsByPlayer.get(event.player().uniqueId());
            if (current == null || !viewsByPlayer.remove(event.player().uniqueId(), current)) {
                return;
            }
            viewsById.remove(current.id(), current);
            current.detach();
            view = current;
        }
        notifyCloseListeners(view);
        view.gui().close(new GuiClose(view, event.player()));
    }

    private void ignoreNextCloseEvent(UUID playerId) {
        ignoredCloseEvents.computeIfAbsent(playerId, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private boolean removeIgnoredCloseEvent(UUID playerId) {
        var counter = ignoredCloseEvents.get(playerId);
        if (counter == null) {
            return false;
        }
        while (true) {
            int current = counter.get();
            if (current <= 0) {
                ignoredCloseEvents.remove(playerId, counter);
                return false;
            }
            if (counter.compareAndSet(current, current - 1)) {
                if (current == 1) {
                    ignoredCloseEvents.remove(playerId, counter);
                }
                return true;
            }
        }
    }

    private void notifyCloseListeners(FandGuiView view) {
        var listeners = closeListeners.remove(view.id());
        if (listeners == null) {
            return;
        }
        for (var listener : listeners) {
            listener.run();
        }
    }

    static final class FandGuiView implements GuiView {

        private final FandGuiService owner;
        private final UUID id = UUID.randomUUID();
        private final Player player;
        private final Gui gui;
        private final Inventory inventory;
        private final ConcurrentHashMap<String, Object> state = new ConcurrentHashMap<>();
        private volatile boolean open = true;
        private volatile boolean displayed;

        FandGuiView(FandGuiService owner, Player player, Gui gui, Inventory inventory) {
            this.owner = owner;
            this.player = player;
            this.gui = gui;
            this.inventory = inventory;
        }

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public Gui gui() {
            return gui;
        }

        @Override
        public Inventory inventory() {
            return inventory;
        }

        @Override
        public boolean open() {
            return open;
        }

        boolean displayed() {
            return displayed;
        }

        void markDisplayed() {
            displayed = true;
        }

        @Override
        public ItemStack cursorItem() {
            return player.cursorItem();
        }

        @Override
        public void setCursorItem(ItemStack item) {
            player.setCursorItem(item);
        }

        @Override
        public void setProperty(int id, int value) {
            if (id < 0) {
                throw new IllegalArgumentException("property id must be >= 0");
            }
            if (!open) {
                return;
            }
            if (player instanceof io.fand.server.entity.FandPlayer fandPlayer) {
                fandPlayer.setOpenInventoryProperty(id, value);
            }
        }

        @Override
        public void reopen() {
            if (!open) {
                return;
            }
            if (displayed) {
                owner.ignoreNextCloseEvent(player.uniqueId());
            }
            player.openInventory(inventory).whenComplete((opened, failure) -> {
                if (failure != null) {
                    if (displayed) {
                        owner.removeIgnoredCloseEvent(player.uniqueId());
                    }
                    owner.closeAfterFailedReopen(this, failure);
                } else if (Boolean.TRUE.equals(opened) && owner.isCurrent(this)) {
                    markDisplayed();
                    applyContentsToOpenMenu();
                    applyInitialProperties();
                } else if (!Boolean.TRUE.equals(opened)) {
                    if (displayed) {
                        owner.removeIgnoredCloseEvent(player.uniqueId());
                    }
                    owner.closeAfterFailedReopen(this);
                }
            });
        }

        @Override
        public void close() {
            owner.closeProgrammatically(this);
        }

        @Override
        public Optional<Object> state(String key) {
            java.util.Objects.requireNonNull(key, "key");
            return Optional.ofNullable(state.get(key));
        }

        @Override
        public void state(String key, Object value) {
            state.put(java.util.Objects.requireNonNull(key, "key"), java.util.Objects.requireNonNull(value, "value"));
        }

        @Override
        public void removeState(String key) {
            state.remove(java.util.Objects.requireNonNull(key, "key"));
        }

        void detach() {
            open = false;
        }

        void applyInitialProperties() {
            for (var entry : gui.properties().entrySet()) {
                setProperty(entry.getKey(), entry.getValue());
            }
        }

        void applyContentsToOpenMenu() {
            var opened = player.openInventory();
            if (opened.isEmpty()) {
                return;
            }
            var menu = opened.get();
            int slots = Math.min(gui.size(), menu.size());
            for (int slot = 0; slot < slots; slot++) {
                var item = gui.item(slot);
                if (!item.isEmpty()) {
                    menu.set(slot, item);
                }
            }
        }
    }
}
