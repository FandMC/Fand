package io.fand.server.gui;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.Server;
import io.fand.api.entity.Player;
import io.fand.api.event.inventory.ClickType;
import io.fand.api.event.inventory.InventoryAction;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.gui.Gui;
import io.fand.api.internal.FandRuntime;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.server.event.EventDispatcher;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class FandGuiServiceTest {

    private final ArrayList<CompletableFuture<Boolean>> openResults = new ArrayList<>();
    private Server boundServer;

    @AfterEach
    void unbindServer() {
        if (boundServer != null) {
            FandRuntime.unbind(boundServer);
            boundServer = null;
        }
    }

    @Test
    void staleOpenFailureDoesNotRemoveReplacementView() {
        bindServer();
        var service = new FandGuiService(new EventDispatcher());
        var player = player(UUID.randomUUID());

        var first = service.open(player, gui("First"));
        var second = service.open(player, gui("Second"));

        openResults.get(0).complete(false);

        assertThat(first.open()).isFalse();
        assertThat(second.open()).isTrue();
        assertThat(service.openView(player)).containsSame(second);
        assertThat(service.view(second.id())).containsSame(second);
    }

    @Test
    void staleCloseEventDoesNotRemoveReplacementView() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());

        var first = service.open(player, gui("First"));
        openResults.get(0).complete(true);
        var second = service.open(player, gui("Second"));
        openResults.get(1).complete(true);

        events.fire(new InventoryCloseEvent(player, InventoryType.CHEST));

        assertThat(first.open()).isFalse();
        assertThat(second.open()).isTrue();
        assertThat(service.openView(player)).containsSame(second);
        assertThat(service.view(second.id())).containsSame(second);
    }

    @Test
    void replacingViewClosesOldViewOnce() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());
        var closed = new AtomicInteger();

        var first = service.open(player, gui("First", closed));
        openResults.get(0).complete(true);
        var second = service.open(player, gui("Second"));
        openResults.get(1).complete(true);
        events.fire(new InventoryCloseEvent(player, InventoryType.CHEST));

        assertThat(first.open()).isFalse();
        assertThat(second.open()).isTrue();
        assertThat(closed).hasValue(1);
    }

    @Test
    void reopenCloseEventDoesNotRemoveCurrentView() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());

        var view = service.open(player, gui("Menu"));
        openResults.get(0).complete(true);
        view.reopen();
        events.fire(new InventoryCloseEvent(player, InventoryType.CHEST));
        openResults.get(1).complete(true);

        assertThat(view.open()).isTrue();
        assertThat(service.openView(player)).containsSame(view);
        assertThat(service.view(view.id())).containsSame(view);
    }

    @Test
    void failedReopenClosesCurrentViewAfterStaleCloseEvent() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());
        var closed = new AtomicInteger();

        var view = service.open(player, gui("Menu", closed));
        openResults.get(0).complete(true);
        view.reopen();
        events.fire(new InventoryCloseEvent(player, InventoryType.CHEST));
        openResults.get(1).complete(false);

        assertThat(view.open()).isFalse();
        assertThat(service.openView(player)).isEmpty();
        assertThat(service.view(view.id())).isEmpty();
        assertThat(closed).hasValue(1);
    }

    @Test
    void replaceKeepsViewStateAndOpensReplacementInventory() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());
        var closed = new AtomicInteger();

        var view = service.open(player, gui("First", closed));
        openResults.get(0).complete(true);
        view.state("page", 2);
        view.replace(gui("Second"));
        openResults.get(1).complete(true);

        assertThat(view.open()).isTrue();
        assertThat(service.openView(player)).containsSame(view);
        assertThat(view.state("page")).contains(2);
        assertThat(view.gui().title()).isEqualTo(Component.text("Second"));
        assertThat(view.inventory().title()).isEqualTo(Component.text("Second"));
        assertThat(player.openInventory().orElseThrow().title()).isEqualTo(Component.text("Second"));
        assertThat(closed).hasValue(0);
    }

    @Test
    void closeEventRemovesCurrentViewWhenReplacedViewWasNeverDisplayed() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());

        var first = service.open(player, gui("First"));
        var second = service.open(player, gui("Second"));
        openResults.get(1).complete(true);

        events.fire(new InventoryCloseEvent(player, InventoryType.CHEST));

        assertThat(first.open()).isFalse();
        assertThat(second.open()).isFalse();
        assertThat(service.openView(player)).isEmpty();
        assertThat(service.view(second.id())).isEmpty();
    }

    @Test
    void protectedGuiUsesContainerSlotAndCancelsCrossInventoryActions() {
        bindServer();
        var events = new EventDispatcher();
        var service = new FandGuiService(events);
        var player = player(UUID.randomUUID());

        var view = service.open(player, gui("Menu"));
        openResults.getFirst().complete(true);

        var containerClick = click(player, view.inventory(), 36, 0, -1, ClickType.PICKUP);
        var quickMove = click(player, view.inventory(), 9, -1, 0, ClickType.QUICK_MOVE);
        var pickupAll = click(player, view.inventory(), 9, -1, 0, ClickType.PICKUP_ALL);
        events.fire(containerClick);
        events.fire(quickMove);
        events.fire(pickupAll);

        assertThat(containerClick.cancelled()).isTrue();
        assertThat(quickMove.cancelled()).isTrue();
        assertThat(pickupAll.cancelled()).isTrue();
    }

    private static InventoryClickEvent click(
            Player player,
            Inventory inventory,
            int slot,
            int containerSlot,
            int playerInventorySlot,
            ClickType clickType
    ) {
        return new InventoryClickEvent(
                player,
                inventory,
                slot,
                slot,
                containerSlot,
                playerInventorySlot,
                clickType,
                InventoryAction.UNKNOWN,
                0,
                ItemStack.EMPTY,
                ItemStack.EMPTY);
    }

    private void bindServer() {
        Object proxy = Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (instance, method, args) -> switch (method.getName()) {
                    case "createInventory" -> new TestInventory((InventoryType) args[0], (int) args[1], (Component) args[2]);
                    case "toString" -> "Server proxy";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> args != null && args.length == 1 && instance == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        boundServer = (Server) proxy;
        FandRuntime.bind(boundServer);
    }

    private Player player(UUID id) {
        var openInventory = new java.util.concurrent.atomic.AtomicReference<Inventory>();
        Object proxy = Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (instance, method, args) -> switch (method.getName()) {
                    case "uniqueId" -> id;
                    case "openInventory" -> {
                        if (args == null || args.length == 0) {
                            yield Optional.ofNullable(openInventory.get());
                        }
                        var result = new CompletableFuture<Boolean>();
                        var inventory = (Inventory) args[0];
                        result.thenAccept(opened -> {
                            if (opened) {
                                openInventory.set(inventory);
                            }
                        });
                        openResults.add(result);
                        yield result;
                    }
                    case "closeInventory" -> {
                        openInventory.set(null);
                        yield null;
                    }
                    case "cursorItem" -> ItemStack.EMPTY;
                    case "setCursorItem" -> null;
                    case "toString" -> "Player proxy";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> args != null && args.length == 1 && instance == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        return (Player) proxy;
    }

    private static Gui gui(String title) {
        return gui(title, null);
    }

    private static Gui gui(String title, AtomicInteger closed) {
        var builder = Gui.builder(InventoryType.CHEST, 9, Component.text(title))
                .button(0, new ItemStack(new TestItemType(Key.key("minecraft:diamond"), 64), 1), click -> { });
        if (closed != null) {
            builder.onClose(close -> closed.incrementAndGet());
        }
        return builder.build();
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static final class TestInventory implements Inventory {

        private final InventoryType type;
        private final int size;
        private final Component title;
        private final ItemStack[] items;

        private TestInventory(InventoryType type, int size, Component title) {
            this.type = type;
            this.size = size;
            this.title = title;
            this.items = new ItemStack[size];
            java.util.Arrays.fill(items, ItemStack.EMPTY);
        }

        @Override
        public InventoryType type() {
            return type;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public ItemStack get(int slot) {
            return items[slot];
        }

        @Override
        public void set(int slot, ItemStack stack) {
            items[slot] = stack;
        }

        @Override
        public ItemStack add(ItemStack stack) {
            return stack;
        }

        @Override
        public void clear() {
            java.util.Arrays.fill(items, ItemStack.EMPTY);
        }

        @Override
        public Component title() {
            return title;
        }
    }
}
