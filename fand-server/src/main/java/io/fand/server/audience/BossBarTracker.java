package io.fand.server.audience;

import io.fand.server.command.AdventureBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Bridges Adventure {@link BossBar}s to vanilla {@link ServerBossEvent}s for a
 * single player. One tracker instance is held per FandPlayer and wires bossbar
 * mutations through an Adventure listener.
 *
 * <p>Not thread-safe. Always invoked from the main server thread; show/hide
 * calls posted from other threads must already be marshalled by the caller
 * (FandPlayer does this).
 */
public final class BossBarTracker {

    private final ServerPlayer player;
    private final Map<BossBar, Entry> entries = new HashMap<>();

    public BossBarTracker(ServerPlayer player) {
        this.player = player;
    }

    public void rebind(ServerPlayer freshHandle) {
        for (var entry : entries.values()) {
            sendAdd(freshHandle, entry.event);
        }
    }

    public void show(BossBar bar) {
        if (entries.containsKey(bar)) {
            return;
        }
        var event = new ServerBossEvent(
                UUID.randomUUID(),
                AdventureBridge.toVanilla(bar.name(), player.registryAccess()),
                colorOf(bar.color()),
                overlayOf(bar.overlay())
        );
        applyFlags(event, bar);
        event.setProgress(bar.progress());
        var listener = new Listener(bar, event);
        bar.addListener(listener);
        entries.put(bar, new Entry(event, listener));
        sendAdd(player, event);
    }

    public void hide(BossBar bar) {
        var entry = entries.remove(bar);
        if (entry == null) {
            return;
        }
        bar.removeListener(entry.listener);
        sendRemove(entry.event.getId());
    }

    public void clear() {
        if (entries.isEmpty()) {
            return;
        }
        for (var slot : entries.entrySet()) {
            slot.getKey().removeListener(slot.getValue().listener);
            sendRemove(slot.getValue().event.getId());
        }
        entries.clear();
    }

    private void sendAdd(ServerPlayer target, ServerBossEvent event) {
        var connection = target.connection;
        if (connection != null) {
            connection.send(ClientboundBossEventPacket.createAddPacket(event));
        }
    }

    private void sendRemove(UUID id) {
        var connection = player.connection;
        if (connection != null) {
            connection.send(ClientboundBossEventPacket.createRemovePacket(id));
        }
    }

    private void send(ClientboundBossEventPacket packet) {
        var connection = player.connection;
        if (connection != null) {
            connection.send(packet);
        }
    }

    private final class Listener implements BossBar.Listener {
        private final BossBar bar;
        private final ServerBossEvent event;

        Listener(BossBar bar, ServerBossEvent event) {
            this.bar = bar;
            this.event = event;
        }

        @Override
        public void bossBarNameChanged(BossBar self, Component oldName, Component newName) {
            event.setName(AdventureBridge.toVanilla(newName, player.registryAccess()));
            send(ClientboundBossEventPacket.createUpdateNamePacket(event));
        }

        @Override
        public void bossBarProgressChanged(BossBar self, float oldProgress, float newProgress) {
            event.setProgress(newProgress);
            send(ClientboundBossEventPacket.createUpdateProgressPacket(event));
        }

        @Override
        public void bossBarColorChanged(BossBar self, BossBar.Color oldColor, BossBar.Color newColor) {
            event.setColor(colorOf(newColor));
            send(ClientboundBossEventPacket.createUpdateStylePacket(event));
        }

        @Override
        public void bossBarOverlayChanged(BossBar self, BossBar.Overlay oldOverlay, BossBar.Overlay newOverlay) {
            event.setOverlay(overlayOf(newOverlay));
            send(ClientboundBossEventPacket.createUpdateStylePacket(event));
        }

        @Override
        public void bossBarFlagsChanged(BossBar self, java.util.Set<BossBar.Flag> added, java.util.Set<BossBar.Flag> removed) {
            applyFlags(event, bar);
            send(ClientboundBossEventPacket.createUpdatePropertiesPacket(event));
        }
    }

    private record Entry(ServerBossEvent event, Listener listener) {
    }

    private static BossEvent.BossBarColor colorOf(BossBar.Color color) {
        return switch (color) {
            case PINK -> BossEvent.BossBarColor.PINK;
            case BLUE -> BossEvent.BossBarColor.BLUE;
            case RED -> BossEvent.BossBarColor.RED;
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            case PURPLE -> BossEvent.BossBarColor.PURPLE;
            case WHITE -> BossEvent.BossBarColor.WHITE;
        };
    }

    private static BossEvent.BossBarOverlay overlayOf(BossBar.Overlay overlay) {
        return switch (overlay) {
            case PROGRESS -> BossEvent.BossBarOverlay.PROGRESS;
            case NOTCHED_6 -> BossEvent.BossBarOverlay.NOTCHED_6;
            case NOTCHED_10 -> BossEvent.BossBarOverlay.NOTCHED_10;
            case NOTCHED_12 -> BossEvent.BossBarOverlay.NOTCHED_12;
            case NOTCHED_20 -> BossEvent.BossBarOverlay.NOTCHED_20;
        };
    }

    private static void applyFlags(ServerBossEvent event, BossBar bar) {
        var flags = bar.flags();
        event.setDarkenScreen(flags.contains(BossBar.Flag.DARKEN_SCREEN));
        event.setPlayBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
        event.setCreateWorldFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
    }
}
