package io.fand.server.player;

import io.fand.api.inventory.PlayerInventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.player.OfflinePlayer;
import io.fand.api.player.PlayerProfile;
import io.fand.server.item.FandItemStacks;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.LoggerFactory;

final class FandOfflinePlayer implements OfflinePlayer {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FandOfflinePlayer.class);
    private static final int PLAYER_INVENTORY_SIZE = 43;

    private final MinecraftServer server;
    private final NameAndId vanillaProfile;
    private final PlayerProfile profile;
    private final CompoundTag data;
    private final ServerStatsCounter stats;
    private final OfflineInventory inventory;

    FandOfflinePlayer(NameAndId vanillaProfile, PlayerProfile profile, CompoundTag data, ServerStatsCounter stats, MinecraftServer server) {
        this.vanillaProfile = Objects.requireNonNull(vanillaProfile, "vanillaProfile");
        this.server = Objects.requireNonNull(server, "server");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.data = Objects.requireNonNull(data, "data");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.inventory = readInventory(data, server);
    }

    @Override
    public PlayerProfile profile() {
        return profile;
    }

    @Override
    public Optional<Instant> firstPlayed() {
        return data.getLong("firstPlayed").map(Instant::ofEpochMilli);
    }

    @Override
    public Optional<Instant> lastPlayed() {
        return data.getLong("lastPlayed").map(Instant::ofEpochMilli);
    }

    @Override
    public boolean playedBefore() {
        return true;
    }

    @Override
    public int statistic(Key key) {
        Objects.requireNonNull(key, "key");
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        if (!net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT.containsKey(id)) {
            return 0;
        }
        return stats.getValue(Stats.CUSTOM, id);
    }

    @Override
    public Optional<PlayerInventory> inventory() {
        return Optional.of(inventory);
    }

    @Override
    public CompletableFuture<Boolean> save() {
        Runnable updateData = () -> {
            writeInventory(data, inventory, server);
            stats.save();
        };
        if (server.isSameThread()) {
            updateData.run();
            return CompletableFuture.completedFuture(server.getPlayerList().savePlayerData(
                    vanillaProfile,
                    output -> data.forEach(output::putRaw)));
        }
        return server.submit(() -> {
            updateData.run();
            return server.getPlayerList().savePlayerData(vanillaProfile, output -> data.forEach(output::putRaw));
        });
    }

    private static OfflineInventory readInventory(CompoundTag data, MinecraftServer server) {
        var container = new SimpleContainer(PLAYER_INVENTORY_SIZE);
        try (var reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(
                () -> "offline player inventory",
                LOGGER)) {
            var input = TagValueInput.create(reporter, server.registryAccess(), data);
            for (var item : input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC)) {
                if (item.isValidInContainer(container.getContainerSize())) {
                    container.setItem(item.slot(), item.stack());
                }
            }
        }
        return new OfflineInventory(container, Math.clamp(data.getIntOr("SelectedItemSlot", 0), 0, 8));
    }

    private static void writeInventory(CompoundTag data, OfflineInventory inventory, MinecraftServer server) {
        try (var reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(
                () -> "offline player inventory",
                LOGGER)) {
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, server.registryAccess());
            var list = output.list("Inventory", ItemStackWithSlot.CODEC);
            for (int slot = 0; slot < inventory.size(); slot++) {
                var stack = inventory.handle.getItem(slot);
                if (!stack.isEmpty()) {
                    list.add(new ItemStackWithSlot(slot, stack));
                }
            }
            var encoded = output.buildResult();
            var inventoryTag = encoded.get("Inventory");
            data.put("Inventory", inventoryTag == null ? new net.minecraft.nbt.ListTag() : inventoryTag);
            data.putInt("SelectedItemSlot", inventory.selectedSlot());
        }
    }

    private static final class OfflineInventory implements PlayerInventory {
        private final SimpleContainer handle;
        private int selectedSlot;

        private OfflineInventory(SimpleContainer handle, int selectedSlot) {
            this.handle = Objects.requireNonNull(handle, "handle");
            this.selectedSlot = selectedSlot;
        }

        @Override
        public InventoryType type() {
            return InventoryType.PLAYER;
        }

        @Override
        public int size() {
            return handle.getContainerSize();
        }

        @Override
        public ItemStack get(int slot) {
            return FandItemStacks.fromVanilla(handle.getItem(slot));
        }

        @Override
        public void set(int slot, ItemStack stack) {
            handle.setItem(slot, FandItemStacks.toVanilla(stack));
        }

        @Override
        public ItemStack add(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            var leftover = handle.addItem(FandItemStacks.toVanilla(stack));
            return FandItemStacks.fromVanilla(leftover);
        }

        @Override
        public void clear() {
            handle.clearContent();
        }

        @Override
        public int selectedSlot() {
            return selectedSlot;
        }

        @Override
        public void setSelectedSlot(int slot) {
            if (slot < 0 || slot > 8) {
                throw new IllegalArgumentException("Selected hotbar slot must be in [0, 8], got " + slot);
            }
            selectedSlot = slot;
        }

        @Override
        public ItemStack heldItem() {
            return get(selectedSlot());
        }

        @Override
        public void setHeldItem(ItemStack stack) {
            set(selectedSlot(), stack);
        }

        @Override
        public ItemStack offhandItem() {
            return get(40);
        }

        @Override
        public void setOffhandItem(ItemStack stack) {
            set(40, stack);
        }

        @Override
        public ItemStack helmet() {
            return get(39);
        }

        @Override
        public void setHelmet(ItemStack stack) {
            set(39, stack);
        }

        @Override
        public ItemStack chestplate() {
            return get(38);
        }

        @Override
        public void setChestplate(ItemStack stack) {
            set(38, stack);
        }

        @Override
        public ItemStack leggings() {
            return get(37);
        }

        @Override
        public void setLeggings(ItemStack stack) {
            set(37, stack);
        }

        @Override
        public ItemStack boots() {
            return get(36);
        }

        @Override
        public void setBoots(ItemStack stack) {
            set(36, stack);
        }
    }
}
