package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.gui.Gui;
import io.fand.api.gui.GuiService;
import io.fand.api.gui.GuiView;
import io.fand.server.gui.FandGuiService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PluginGuiService implements GuiService {

    private final GuiService delegate;
    private final PluginResourceTracker tracker;

    public PluginGuiService(GuiService delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public GuiView open(Player player, Gui gui) {
        var view = tracker.track(delegate.open(player, gui));
        if (delegate instanceof FandGuiService fandGuiService) {
            fandGuiService.addCloseListener(view, view::releaseFromExternalClose);
        }
        return view;
    }

    @Override
    public Optional<GuiView> openView(Player player) {
        return delegate.openView(player);
    }

    @Override
    public Optional<GuiView> view(UUID id) {
        return delegate.view(id);
    }
}
