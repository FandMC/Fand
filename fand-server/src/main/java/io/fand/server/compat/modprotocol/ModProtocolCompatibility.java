package io.fand.server.compat.modprotocol;

import io.fand.server.event.EventDispatcher;
import io.fand.server.config.FandConfig;
import io.fand.server.compat.modprotocol.recipeviewer.FabricRecipeSyncProtocol;
import io.fand.server.compat.modprotocol.recipeviewer.JeiRecipeViewerProtocol;
import io.fand.server.compat.modprotocol.recipeviewer.ReiRecipeViewerProtocol;
import io.fand.server.compat.modprotocol.servux.ServuxProtocol;
import io.fand.server.messaging.FandPluginMessaging;
import io.fand.server.structure.FandStructureService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ModProtocolCompatibility implements AutoCloseable {

    private final List<AutoCloseable> registrations = new ArrayList<>();
    private FabricRecipeSyncProtocol fabricRecipeSync;
    private ServuxProtocol servux;

    public ModProtocolCompatibility(
            FandPluginMessaging messaging,
            EventDispatcher events,
            FandStructureService structures,
            FandConfig.ModProtocols config
    ) {
        Objects.requireNonNull(messaging, "messaging");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(config, "config");
        installRecipeViewers(messaging, events, config.recipeViewers);
        installServux(messaging, events, structures, config.servux);
    }

    @Override
    public synchronized void close() {
        closeRegistrations();
    }

    public void syncDataPackContents(ServerPlayer player, boolean joined) {
        var sync = fabricRecipeSync;
        if (sync != null) {
            sync.syncDataPackContents(player, joined);
        }
    }

    public void tick(MinecraftServer server) {
        var protocol = servux;
        if (protocol != null) {
            protocol.tick(server);
        }
    }

    private void installRecipeViewers(FandPluginMessaging messaging, EventDispatcher events, FandConfig.RecipeViewers config) {
        if (config.jei || config.rei) {
            fabricRecipeSync = new FabricRecipeSyncProtocol(messaging, events);
            registrations.add(fabricRecipeSync);
        }
        if (config.jei) {
            registrations.add(new JeiRecipeViewerProtocol(messaging));
        }
        if (config.rei) {
            registrations.add(new ReiRecipeViewerProtocol(messaging));
        }
    }

    private void installServux(
            FandPluginMessaging messaging,
            EventDispatcher events,
            FandStructureService structures,
            FandConfig.Servux config
    ) {
        if (config.enabled) {
            servux = new ServuxProtocol(messaging, events, structures, config);
            registrations.add(servux);
        }
    }

    private void closeRegistrations() {
        for (var registration : registrations) {
            try {
                registration.close();
            } catch (Exception ignored) {
            }
        }
        registrations.clear();
        fabricRecipeSync = null;
        servux = null;
    }
}
