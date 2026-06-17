package io.fand.server.compat.modprotocol;

import io.fand.server.config.FandConfig;
import io.fand.server.compat.modprotocol.recipeviewer.JeiRecipeViewerProtocol;
import io.fand.server.compat.modprotocol.recipeviewer.ReiRecipeViewerProtocol;
import io.fand.server.messaging.FandPluginMessaging;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModProtocolCompatibility implements AutoCloseable {

    private final List<AutoCloseable> registrations = new ArrayList<>();

    public ModProtocolCompatibility(FandPluginMessaging messaging, FandConfig.ModProtocols config) {
        Objects.requireNonNull(messaging, "messaging");
        Objects.requireNonNull(config, "config");
        installRecipeViewers(messaging, config.recipeViewers);
    }

    @Override
    public synchronized void close() {
        closeRegistrations();
    }

    private void installRecipeViewers(FandPluginMessaging messaging, FandConfig.RecipeViewers config) {
        if (config.jei) {
            registrations.add(new JeiRecipeViewerProtocol(messaging));
        }
        if (config.rei) {
            registrations.add(new ReiRecipeViewerProtocol(messaging));
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
    }
}
