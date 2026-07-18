package io.fand.server.plugin;

import io.fand.api.advancement.AdvancementRegistration;
import io.fand.api.auth.LoginAuthenticationRegistration;
import io.fand.api.bossbar.BossBarHandle;
import io.fand.api.bossbar.BossBarRegistration;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandRegistration;
import io.fand.api.block.custom.CustomBlockItemBinding;
import io.fand.api.block.custom.CustomBlockRegistration;
import io.fand.api.item.custom.CustomItemRegistration;
import io.fand.api.datapack.DataPackRegistration;
import io.fand.api.enchantment.EnchantmentRegistration;
import io.fand.api.event.EventSubscription;
import io.fand.api.gamerule.GameRuleRegistration;
import io.fand.api.gui.GuiView;
import io.fand.api.hologram.Hologram;
import io.fand.api.loot.LootTableRegistration;
import io.fand.api.map.MapRenderer;
import io.fand.api.map.MapService;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.api.nms.NmsHookRegistration;
import io.fand.api.packet.PacketRegistration;
import io.fand.api.placeholder.PlaceholderRegistration;
import io.fand.api.permission.PermissionAttachment;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.region.RegionFlagRegistration;
import io.fand.api.region.RegionRegistration;
import io.fand.api.recipe.RecipeRegistration;
import io.fand.api.resourcepack.ResourcePackRegistration;
import io.fand.api.scheduler.Task;
import io.fand.api.scoreboard.ScoreboardRegistration;
import io.fand.api.service.ServiceRegistration;
import io.fand.api.structure.StructureRegistration;
import io.fand.api.tablist.TabListRegistration;
import io.fand.server.map.FandMapService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class PluginResourceTracker {

    private final Object lock = new Object();
    private final Set<TrackedSubscription> subscriptions = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCommandRegistration> commandRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedRecipeRegistration> recipeRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedLootTableRegistration> lootTableRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPermissionAttachment> permissionAttachments = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<PermissionDescriptor> permissionDescriptors = new ArrayList<>();
    private final Set<TrackedBossBarHandle> bossBarHandles = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedBossBarRegistration> bossBarRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedTabListRegistration> tabListRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedTabListVisibility> tabListVisibilities = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedSimulatedPlayer> simulatedPlayers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedScoreboardRegistration> scoreboardRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPacketRegistration> packetRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPlaceholderRegistration> placeholderRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedPluginMessageRegistration> pluginMessageRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedGameRuleRegistration> gameRuleRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedDataPackRegistration> dataPackRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedResourcePackRegistration> resourcePackRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedRegionRegistration> regionRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedRegionFlagRegistration> regionFlagRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedAdvancementRegistration> advancementRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedEnchantmentRegistration> enchantmentRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedStructureRegistration> structureRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<MapRendererBinding> mapRendererBindings = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCustomItemRegistration> customItemRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCustomBlockRegistration> customBlockRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedCustomBlockItemBinding> customBlockItemBindings = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedHologram> holograms = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedGuiView> guiViews = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedServiceRegistration<?>> serviceRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedNmsHookRegistration> nmsHookRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedLoginAuthenticationRegistration> loginAuthenticationRegistrations = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TrackedTask> tasks = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean closed;

    TrackedSubscription track(EventSubscription delegate) {
        var tracked = new TrackedSubscription(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                subscriptions.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedCommandRegistration track(CommandRegistration delegate) {
        return track(delegate, List.of());
    }

    TrackedCommandRegistration track(CommandRegistration delegate, CommandInfo descriptor) {
        return track(delegate, descriptor == null ? List.of() : List.of(descriptor));
    }

    TrackedCommandRegistration track(CommandRegistration delegate, List<CommandInfo> descriptors) {
        var tracked = new TrackedCommandRegistration(this, delegate, descriptors);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                commandRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedTask track(Task delegate) {
        var tracked = new TrackedTask(this, delegate);
        synchronized (lock) {
            if (closed) {
                tracked.cancelFromTracker();
                return tracked;
            }
            tasks.add(tracked);
        }
        return tracked;
    }

    TrackedRecipeRegistration track(RecipeRegistration delegate) {
        var tracked = new TrackedRecipeRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                recipeRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedLootTableRegistration track(LootTableRegistration delegate) {
        var tracked = new TrackedLootTableRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                lootTableRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedPacketRegistration track(PacketRegistration delegate) {
        var tracked = new TrackedPacketRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                packetRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedPermissionAttachment track(PermissionAttachment delegate) {
        var tracked = new TrackedPermissionAttachment(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                permissionAttachments.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedPlaceholderRegistration track(PlaceholderRegistration delegate) {
        var tracked = new TrackedPlaceholderRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                placeholderRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    void trackPermission(PermissionDescriptor descriptor) {
        synchronized (lock) {
            if (!closed) {
                permissionDescriptors.add(descriptor);
            }
        }
    }

    TrackedBossBarHandle track(BossBarHandle delegate) {
        var tracked = new TrackedBossBarHandle(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                bossBarHandles.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedBossBarRegistration track(BossBarRegistration delegate) {
        var tracked = new TrackedBossBarRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                bossBarRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedTabListRegistration track(TabListRegistration delegate) {
        var tracked = new TrackedTabListRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                tabListRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.removeFromTracker();
        }
        return tracked;
    }

    void trackTabListVisibility(UUID viewerId, UUID targetId, Runnable restore) {
        var tracked = new TrackedTabListVisibility(this, viewerId, targetId, restore);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else if (tabListVisibilities.stream().noneMatch(visibility -> visibility.matches(viewerId, targetId))) {
                tabListVisibilities.add(tracked);
            } else {
                return;
            }
        }
        if (dispose) {
            tracked.restoreFromTracker();
        }
    }

    void restoreTabListVisibility(UUID viewerId, UUID targetId) {
        List<TrackedTabListVisibility> matches;
        synchronized (lock) {
            matches = tabListVisibilities.stream()
                    .filter(visibility -> visibility.matches(viewerId, targetId))
                    .toList();
        }
        for (var visibility : matches) {
            visibility.restore();
        }
    }

    void trackSimulatedPlayer(UUID uniqueId, SimulatedPlayerService service) {
        var tracked = new TrackedSimulatedPlayer(this, uniqueId, service);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                simulatedPlayers.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
    }

    void releaseSimulatedPlayer(UUID uniqueId) {
        List<TrackedSimulatedPlayer> matches;
        synchronized (lock) {
            matches = simulatedPlayers.stream()
                    .filter(player -> player.uniqueId().equals(uniqueId))
                    .toList();
        }
        for (var match : matches) {
            match.release();
        }
    }

    TrackedPluginMessageRegistration track(PluginMessageRegistration delegate) {
        var tracked = new TrackedPluginMessageRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                pluginMessageRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedGameRuleRegistration track(GameRuleRegistration delegate) {
        var tracked = new TrackedGameRuleRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                gameRuleRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedDataPackRegistration track(DataPackRegistration delegate) {
        var tracked = new TrackedDataPackRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                dataPackRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedResourcePackRegistration track(ResourcePackRegistration delegate) {
        var tracked = new TrackedResourcePackRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                resourcePackRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedRegionRegistration track(RegionRegistration delegate) {
        var tracked = new TrackedRegionRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                regionRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedRegionFlagRegistration track(RegionFlagRegistration delegate) {
        var tracked = new TrackedRegionFlagRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                regionFlagRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedScoreboardRegistration track(ScoreboardRegistration delegate) {
        var tracked = new TrackedScoreboardRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                scoreboardRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedAdvancementRegistration track(AdvancementRegistration delegate) {
        var tracked = new TrackedAdvancementRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                advancementRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedEnchantmentRegistration track(EnchantmentRegistration delegate) {
        var tracked = new TrackedEnchantmentRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                enchantmentRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedStructureRegistration track(StructureRegistration delegate) {
        var tracked = new TrackedStructureRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                structureRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    void track(MapRendererBinding binding) {
        var dispose = false;
        MapRendererBinding stale = null;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                // A renderer binding tracks the renderer currently installed on a
                // map. Replacing the renderer (view.renderer(r2) after r1)
                // supersedes r1's binding: r1's renderer state is overwritten in
                // FandMapService when r2 is installed, so dropping the stale
                // binding here keeps the tracker set bounded instead of growing
                // once per swap. clearRenderer is a no-op on the stale renderer.
                for (var existing : mapRendererBindings) {
                    if (existing.mapId() == binding.mapId() && existing.service() == binding.service()) {
                        stale = existing;
                        break;
                    }
                }
                if (stale != null) {
                    mapRendererBindings.remove(stale);
                }
                mapRendererBindings.add(binding);
            }
        }
        if (dispose) {
            binding.closeFromTracker();
        }
    }

    TrackedCustomItemRegistration track(CustomItemRegistration delegate) {
        var tracked = new TrackedCustomItemRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                customItemRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedCustomBlockRegistration track(CustomBlockRegistration delegate) {
        var tracked = new TrackedCustomBlockRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                customBlockRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedCustomBlockItemBinding track(CustomBlockItemBinding delegate) {
        var tracked = new TrackedCustomBlockItemBinding(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                customBlockItemBindings.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedGuiView track(GuiView delegate) {
        var tracked = new TrackedGuiView(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                guiViews.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    TrackedHologram track(Hologram delegate) {
        var tracked = new TrackedHologram(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                holograms.add(tracked);
            }
        }
        if (dispose) {
            tracked.closeFromTracker();
        }
        return tracked;
    }

    <T> TrackedServiceRegistration<T> track(ServiceRegistration<T> delegate) {
        var tracked = new TrackedServiceRegistration<>(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                serviceRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedNmsHookRegistration track(NmsHookRegistration delegate) {
        var tracked = new TrackedNmsHookRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                nmsHookRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    TrackedLoginAuthenticationRegistration track(LoginAuthenticationRegistration delegate) {
        var tracked = new TrackedLoginAuthenticationRegistration(this, delegate);
        var dispose = false;
        synchronized (lock) {
            if (closed) {
                dispose = true;
            } else {
                loginAuthenticationRegistrations.add(tracked);
            }
        }
        if (dispose) {
            tracked.unregisterFromTracker();
        }
        return tracked;
    }

    void release(TrackedSubscription subscription) {
        synchronized (lock) {
            subscriptions.remove(subscription);
        }
    }

    void release(TrackedCommandRegistration registration) {
        synchronized (lock) {
            commandRegistrations.remove(registration);
        }
    }

    void release(TrackedTask task) {
        synchronized (lock) {
            tasks.remove(task);
        }
    }

    void release(TrackedRecipeRegistration registration) {
        synchronized (lock) {
            recipeRegistrations.remove(registration);
        }
    }

    void release(TrackedLootTableRegistration registration) {
        synchronized (lock) {
            lootTableRegistrations.remove(registration);
        }
    }

    void release(TrackedPermissionAttachment attachment) {
        synchronized (lock) {
            permissionAttachments.remove(attachment);
        }
    }

    void release(TrackedBossBarHandle handle) {
        synchronized (lock) {
            bossBarHandles.remove(handle);
        }
    }

    void release(TrackedBossBarRegistration registration) {
        synchronized (lock) {
            bossBarRegistrations.remove(registration);
        }
    }

    void release(TrackedTabListRegistration registration) {
        synchronized (lock) {
            tabListRegistrations.remove(registration);
        }
    }

    void release(TrackedTabListVisibility visibility) {
        synchronized (lock) {
            tabListVisibilities.remove(visibility);
        }
    }

    void release(TrackedSimulatedPlayer player) {
        synchronized (lock) {
            simulatedPlayers.remove(player);
        }
    }

    void release(TrackedScoreboardRegistration registration) {
        synchronized (lock) {
            scoreboardRegistrations.remove(registration);
        }
    }

    void release(TrackedPacketRegistration registration) {
        synchronized (lock) {
            packetRegistrations.remove(registration);
        }
    }

    void release(TrackedPlaceholderRegistration registration) {
        synchronized (lock) {
            placeholderRegistrations.remove(registration);
        }
    }

    void release(TrackedPluginMessageRegistration registration) {
        synchronized (lock) {
            pluginMessageRegistrations.remove(registration);
        }
    }

    void release(TrackedGameRuleRegistration registration) {
        synchronized (lock) {
            gameRuleRegistrations.remove(registration);
        }
    }

    void release(TrackedDataPackRegistration registration) {
        synchronized (lock) {
            dataPackRegistrations.remove(registration);
        }
    }

    void release(TrackedResourcePackRegistration registration) {
        synchronized (lock) {
            resourcePackRegistrations.remove(registration);
        }
    }

    void release(TrackedRegionRegistration registration) {
        synchronized (lock) {
            regionRegistrations.remove(registration);
        }
    }

    void release(TrackedRegionFlagRegistration registration) {
        synchronized (lock) {
            regionFlagRegistrations.remove(registration);
        }
    }

    void release(TrackedAdvancementRegistration registration) {
        synchronized (lock) {
            advancementRegistrations.remove(registration);
        }
    }

    void release(TrackedEnchantmentRegistration registration) {
        synchronized (lock) {
            enchantmentRegistrations.remove(registration);
        }
    }

    void release(TrackedStructureRegistration registration) {
        synchronized (lock) {
            structureRegistrations.remove(registration);
        }
    }

    void release(TrackedCustomItemRegistration registration) {
        synchronized (lock) {
            customItemRegistrations.remove(registration);
        }
    }

    void release(TrackedCustomBlockRegistration registration) {
        synchronized (lock) {
            customBlockRegistrations.remove(registration);
        }
    }

    void release(TrackedCustomBlockItemBinding binding) {
        synchronized (lock) {
            customBlockItemBindings.remove(binding);
        }
    }

    void release(TrackedGuiView view) {
        synchronized (lock) {
            guiViews.remove(view);
        }
    }

    void release(TrackedHologram hologram) {
        synchronized (lock) {
            holograms.remove(hologram);
        }
    }

    void release(TrackedServiceRegistration<?> registration) {
        synchronized (lock) {
            serviceRegistrations.remove(registration);
        }
    }

    void release(TrackedNmsHookRegistration registration) {
        synchronized (lock) {
            nmsHookRegistrations.remove(registration);
        }
    }

    void release(TrackedLoginAuthenticationRegistration registration) {
        synchronized (lock) {
            loginAuthenticationRegistrations.remove(registration);
        }
    }

    Optional<GuiView> trackedGuiView(GuiView delegate) {
        synchronized (lock) {
            return guiViews.stream()
                    .filter(view -> view.delegate == delegate || view.id().equals(delegate.id()))
                    .filter(GuiView::open)
                    .map(GuiView.class::cast)
                    .findFirst();
        }
    }

    Collection<GuiView> trackedGuiViews(io.fand.api.gui.Gui gui) {
        synchronized (lock) {
            return guiViews.stream()
                    .filter(view -> view.open() && view.gui() == gui)
                    .map(GuiView.class::cast)
                    .toList();
        }
    }

    Optional<Hologram> hologram(java.util.UUID id) {
        synchronized (lock) {
            return holograms.stream()
                    .filter(hologram -> hologram.id().equals(id))
                    .filter(Hologram::active)
                    .map(Hologram.class::cast)
                    .findFirst();
        }
    }

    Collection<Hologram> holograms() {
        synchronized (lock) {
            return holograms.stream()
                    .filter(Hologram::active)
                    .map(Hologram.class::cast)
                    .toList();
        }
    }

    List<CommandInfo> commandDescriptors() {
        synchronized (lock) {
            return commandRegistrations.stream()
                    .flatMap(registration -> registration.descriptors().stream())
                    .toList();
        }
    }

    List<PermissionDescriptor> permissionDescriptors() {
        synchronized (lock) {
            return List.copyOf(permissionDescriptors);
        }
    }

    void close() {
        List<TrackedSubscription> subscriptionsToClose;
        List<TrackedCommandRegistration> commandRegistrationsToClose;
        List<TrackedRecipeRegistration> recipeRegistrationsToClose;
        List<TrackedLootTableRegistration> lootTableRegistrationsToClose;
        List<TrackedPermissionAttachment> permissionAttachmentsToClose;
        List<TrackedBossBarHandle> bossBarHandlesToClose;
        List<TrackedBossBarRegistration> bossBarRegistrationsToClose;
        List<TrackedTabListRegistration> tabListRegistrationsToClose;
        List<TrackedTabListVisibility> tabListVisibilitiesToClose;
        List<TrackedSimulatedPlayer> simulatedPlayersToClose;
        List<TrackedScoreboardRegistration> scoreboardRegistrationsToClose;
        List<TrackedPacketRegistration> packetRegistrationsToClose;
        List<TrackedPlaceholderRegistration> placeholderRegistrationsToClose;
        List<TrackedPluginMessageRegistration> pluginMessageRegistrationsToClose;
        List<TrackedGameRuleRegistration> gameRuleRegistrationsToClose;
        List<TrackedDataPackRegistration> dataPackRegistrationsToClose;
        List<TrackedResourcePackRegistration> resourcePackRegistrationsToClose;
        List<TrackedRegionRegistration> regionRegistrationsToClose;
        List<TrackedRegionFlagRegistration> regionFlagRegistrationsToClose;
        List<TrackedAdvancementRegistration> advancementRegistrationsToClose;
        List<TrackedEnchantmentRegistration> enchantmentRegistrationsToClose;
        List<TrackedStructureRegistration> structureRegistrationsToClose;
        List<MapRendererBinding> mapRendererBindingsToClose;
        List<TrackedCustomItemRegistration> customItemRegistrationsToClose;
        List<TrackedCustomBlockRegistration> customBlockRegistrationsToClose;
        List<TrackedCustomBlockItemBinding> customBlockItemBindingsToClose;
        List<TrackedHologram> hologramsToClose;
        List<TrackedGuiView> guiViewsToClose;
        List<TrackedServiceRegistration<?>> serviceRegistrationsToClose;
        List<TrackedNmsHookRegistration> nmsHookRegistrationsToClose;
        List<TrackedLoginAuthenticationRegistration> loginAuthenticationRegistrationsToClose;
        List<TrackedTask> tasksToClose;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            subscriptionsToClose = new ArrayList<>(subscriptions);
            commandRegistrationsToClose = new ArrayList<>(commandRegistrations);
            recipeRegistrationsToClose = new ArrayList<>(recipeRegistrations);
            lootTableRegistrationsToClose = new ArrayList<>(lootTableRegistrations);
            permissionAttachmentsToClose = new ArrayList<>(permissionAttachments);
            bossBarHandlesToClose = new ArrayList<>(bossBarHandles);
            bossBarRegistrationsToClose = new ArrayList<>(bossBarRegistrations);
            tabListRegistrationsToClose = new ArrayList<>(tabListRegistrations);
            tabListVisibilitiesToClose = new ArrayList<>(tabListVisibilities);
            simulatedPlayersToClose = new ArrayList<>(simulatedPlayers);
            scoreboardRegistrationsToClose = new ArrayList<>(scoreboardRegistrations);
            packetRegistrationsToClose = new ArrayList<>(packetRegistrations);
            placeholderRegistrationsToClose = new ArrayList<>(placeholderRegistrations);
            pluginMessageRegistrationsToClose = new ArrayList<>(pluginMessageRegistrations);
            gameRuleRegistrationsToClose = new ArrayList<>(gameRuleRegistrations);
            dataPackRegistrationsToClose = new ArrayList<>(dataPackRegistrations);
            resourcePackRegistrationsToClose = new ArrayList<>(resourcePackRegistrations);
            regionRegistrationsToClose = new ArrayList<>(regionRegistrations);
            regionFlagRegistrationsToClose = new ArrayList<>(regionFlagRegistrations);
            advancementRegistrationsToClose = new ArrayList<>(advancementRegistrations);
            enchantmentRegistrationsToClose = new ArrayList<>(enchantmentRegistrations);
            structureRegistrationsToClose = new ArrayList<>(structureRegistrations);
            mapRendererBindingsToClose = new ArrayList<>(mapRendererBindings);
            customItemRegistrationsToClose = new ArrayList<>(customItemRegistrations);
            customBlockRegistrationsToClose = new ArrayList<>(customBlockRegistrations);
            customBlockItemBindingsToClose = new ArrayList<>(customBlockItemBindings);
            hologramsToClose = new ArrayList<>(holograms);
            guiViewsToClose = new ArrayList<>(guiViews);
            serviceRegistrationsToClose = new ArrayList<>(serviceRegistrations);
            nmsHookRegistrationsToClose = new ArrayList<>(nmsHookRegistrations);
            loginAuthenticationRegistrationsToClose = new ArrayList<>(loginAuthenticationRegistrations);
            tasksToClose = new ArrayList<>(tasks);
            subscriptions.clear();
            commandRegistrations.clear();
            recipeRegistrations.clear();
            lootTableRegistrations.clear();
            permissionAttachments.clear();
            permissionDescriptors.clear();
            bossBarHandles.clear();
            bossBarRegistrations.clear();
            tabListRegistrations.clear();
            tabListVisibilities.clear();
            simulatedPlayers.clear();
            scoreboardRegistrations.clear();
            packetRegistrations.clear();
            placeholderRegistrations.clear();
            pluginMessageRegistrations.clear();
            gameRuleRegistrations.clear();
            dataPackRegistrations.clear();
            resourcePackRegistrations.clear();
            regionRegistrations.clear();
            regionFlagRegistrations.clear();
            advancementRegistrations.clear();
            enchantmentRegistrations.clear();
            structureRegistrations.clear();
            mapRendererBindings.clear();
            customItemRegistrations.clear();
            customBlockRegistrations.clear();
            customBlockItemBindings.clear();
            holograms.clear();
            guiViews.clear();
            serviceRegistrations.clear();
            nmsHookRegistrations.clear();
            loginAuthenticationRegistrations.clear();
            tasks.clear();
        }
        for (var subscription : subscriptionsToClose) {
            subscription.unregisterFromTracker();
        }
        for (var registration : commandRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : recipeRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : lootTableRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var attachment : permissionAttachmentsToClose) {
            attachment.closeFromTracker();
        }
        for (var handle : bossBarHandlesToClose) {
            handle.closeFromTracker();
        }
        for (var registration : bossBarRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : tabListRegistrationsToClose) {
            registration.removeFromTracker();
        }
        for (var visibility : tabListVisibilitiesToClose) {
            visibility.restoreFromTracker();
        }
        for (var player : simulatedPlayersToClose) {
            player.closeFromTracker();
        }
        for (var registration : scoreboardRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : packetRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : placeholderRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : pluginMessageRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : gameRuleRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : dataPackRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : resourcePackRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : regionRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : regionFlagRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : advancementRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : enchantmentRegistrationsToClose) {
            registration.closeFromTracker();
        }
        for (var registration : structureRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var binding : mapRendererBindingsToClose) {
            binding.closeFromTracker();
        }
        for (var registration : customItemRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : customBlockRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var binding : customBlockItemBindingsToClose) {
            binding.unregisterFromTracker();
        }
        for (var hologram : hologramsToClose) {
            hologram.closeFromTracker();
        }
        for (var view : guiViewsToClose) {
            view.closeFromTracker();
        }
        for (var registration : serviceRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : nmsHookRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var registration : loginAuthenticationRegistrationsToClose) {
            registration.unregisterFromTracker();
        }
        for (var task : tasksToClose) {
            task.cancelFromTracker();
        }
    }

    static final class TrackedSubscription implements EventSubscription {

        private final PluginResourceTracker owner;
        private final EventSubscription delegate;
        private volatile boolean released;

        TrackedSubscription(PluginResourceTracker owner, EventSubscription delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedCommandRegistration implements CommandRegistration {

        private final PluginResourceTracker owner;
        private final CommandRegistration delegate;
        private final List<CommandInfo> descriptors;
        private volatile boolean released;

        TrackedCommandRegistration(PluginResourceTracker owner, CommandRegistration delegate, List<CommandInfo> descriptors) {
            this.owner = owner;
            this.delegate = delegate;
            this.descriptors = List.copyOf(descriptors);
        }

        List<CommandInfo> descriptors() {
            return descriptors;
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedTask implements Task {

        private final PluginResourceTracker owner;
        private final Task delegate;
        private volatile boolean released;

        TrackedTask(PluginResourceTracker owner, Task delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean cancelled() {
            return released || delegate.cancelled();
        }

        @Override
        public void cancel() {
            if (!released) {
                released = true;
                try {
                    delegate.cancel();
                } finally {
                    owner.release(this);
                }
            }
        }

        void cancelFromTracker() {
            if (!released) {
                released = true;
                delegate.cancel();
            }
        }
    }

    static final class TrackedRecipeRegistration implements RecipeRegistration {

        private final PluginResourceTracker owner;
        private final RecipeRegistration delegate;
        private volatile boolean released;

        TrackedRecipeRegistration(PluginResourceTracker owner, RecipeRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedLootTableRegistration implements LootTableRegistration {

        private final PluginResourceTracker owner;
        private final LootTableRegistration delegate;
        private volatile boolean released;

        TrackedLootTableRegistration(PluginResourceTracker owner, LootTableRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedPermissionAttachment implements PermissionAttachment {

        private final PluginResourceTracker owner;
        private final PermissionAttachment delegate;
        private volatile boolean released;

        TrackedPermissionAttachment(PluginResourceTracker owner, PermissionAttachment delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public io.fand.api.permission.PermissionSubject subject() {
            return delegate.subject();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public java.util.Map<String, Boolean> permissions() {
            return delegate.permissions();
        }

        @Override
        public java.util.Optional<Boolean> permissionValue(String node) {
            return released ? java.util.Optional.empty() : delegate.permissionValue(node);
        }

        @Override
        public void setPermission(String node, boolean value) {
            if (released) {
                throw new IllegalStateException("Permission attachment is closed");
            }
            delegate.setPermission(node, value);
        }

        @Override
        public boolean unsetPermission(String node) {
            if (released) {
                throw new IllegalStateException("Permission attachment is closed");
            }
            return delegate.unsetPermission(node);
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static class TrackedBossBarHandle implements BossBarHandle {

        protected final PluginResourceTracker owner;
        private final BossBarHandle delegate;
        private volatile boolean released;

        TrackedBossBarHandle(PluginResourceTracker owner, BossBarHandle delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public net.kyori.adventure.bossbar.BossBar bossBar() {
            return delegate.bossBar();
        }

        @Override
        public java.util.Collection<? extends io.fand.api.entity.Player> viewers() {
            return released ? java.util.List.of() : delegate.viewers();
        }

        @Override
        public void show(io.fand.api.entity.Player player) {
            ensureOpen();
            delegate.show(player);
        }

        @Override
        public void hide(io.fand.api.entity.Player player) {
            if (!released) {
                delegate.hide(player);
            }
        }

        @Override
        public void hideAll() {
            if (!released) {
                delegate.hideAll();
            }
        }

        @Override
        public void setTitle(net.kyori.adventure.text.Component title) {
            ensureOpen();
            delegate.setTitle(title);
        }

        @Override
        public void setProgress(float progress) {
            ensureOpen();
            delegate.setProgress(progress);
        }

        @Override
        public void setColor(net.kyori.adventure.bossbar.BossBar.Color color) {
            ensureOpen();
            delegate.setColor(color);
        }

        @Override
        public void setOverlay(net.kyori.adventure.bossbar.BossBar.Overlay overlay) {
            ensureOpen();
            delegate.setOverlay(overlay);
        }

        @Override
        public void setFlags(java.util.Set<net.kyori.adventure.bossbar.BossBar.Flag> flags) {
            ensureOpen();
            delegate.setFlags(flags);
        }

        @Override
        public void addFlag(net.kyori.adventure.bossbar.BossBar.Flag flag) {
            ensureOpen();
            delegate.addFlag(flag);
        }

        @Override
        public void removeFlag(net.kyori.adventure.bossbar.BossBar.Flag flag) {
            ensureOpen();
            delegate.removeFlag(flag);
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    releaseFromOwner();
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }

        private void ensureOpen() {
            if (released) {
                throw new IllegalStateException("Boss bar is closed");
            }
        }

        void releaseFromOwner() {
            owner.release(this);
        }
    }

    static final class TrackedBossBarRegistration extends TrackedBossBarHandle implements BossBarRegistration {

        private final BossBarRegistration delegate;

        TrackedBossBarRegistration(PluginResourceTracker owner, BossBarRegistration delegate) {
            super(owner, delegate);
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        void releaseFromOwner() {
            super.owner.release(this);
        }

        @Override
        void closeFromTracker() {
            super.closeFromTracker();
        }
    }

    static final class TrackedTabListRegistration implements TabListRegistration {

        private final PluginResourceTracker owner;
        private final TabListRegistration delegate;
        private volatile boolean released;

        TrackedTabListRegistration(PluginResourceTracker owner, TabListRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public java.util.UUID viewerId() {
            return delegate.viewerId();
        }

        @Override
        public java.util.UUID entryId() {
            return delegate.entryId();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void update(io.fand.api.tablist.TabListEntry entry) {
            if (released) {
                throw new IllegalStateException("Tab-list entry is closed");
            }
            delegate.update(entry);
        }

        @Override
        public void remove() {
            if (!released) {
                released = true;
                try {
                    delegate.remove();
                } finally {
                    owner.release(this);
                }
            }
        }

        void removeFromTracker() {
            if (!released) {
                released = true;
                delegate.remove();
            }
        }
    }

    static final class TrackedTabListVisibility {

        private final PluginResourceTracker owner;
        private final UUID viewerId;
        private final UUID targetId;
        private final Runnable restore;
        private volatile boolean released;

        TrackedTabListVisibility(PluginResourceTracker owner, UUID viewerId, UUID targetId, Runnable restore) {
            this.owner = owner;
            this.viewerId = java.util.Objects.requireNonNull(viewerId, "viewerId");
            this.targetId = java.util.Objects.requireNonNull(targetId, "targetId");
            this.restore = java.util.Objects.requireNonNull(restore, "restore");
        }

        boolean matches(UUID viewerId, UUID targetId) {
            return this.viewerId.equals(viewerId) && this.targetId.equals(targetId);
        }

        void restore() {
            if (!released) {
                released = true;
                try {
                    restore.run();
                } finally {
                    owner.release(this);
                }
            }
        }

        void restoreFromTracker() {
            if (!released) {
                released = true;
                restore.run();
            }
        }

        void release() {
            if (!released) {
                released = true;
                owner.release(this);
            }
        }
    }

    static final class TrackedSimulatedPlayer {

        private final PluginResourceTracker owner;
        private final UUID uniqueId;
        private final SimulatedPlayerService service;
        private volatile boolean released;

        TrackedSimulatedPlayer(PluginResourceTracker owner, UUID uniqueId, SimulatedPlayerService service) {
            this.owner = owner;
            this.uniqueId = java.util.Objects.requireNonNull(uniqueId, "uniqueId");
            this.service = java.util.Objects.requireNonNull(service, "service");
        }

        UUID uniqueId() {
            return uniqueId;
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                service.remove(uniqueId).join();
            }
        }

        void release() {
            if (!released) {
                released = true;
                owner.release(this);
            }
        }
    }

    static final class TrackedPacketRegistration implements PacketRegistration {

        private final PluginResourceTracker owner;
        private final PacketRegistration delegate;
        private volatile boolean released;

        TrackedPacketRegistration(PluginResourceTracker owner, PacketRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedPlaceholderRegistration implements PlaceholderRegistration {

        private final PluginResourceTracker owner;
        private final PlaceholderRegistration delegate;
        private volatile boolean released;

        TrackedPlaceholderRegistration(PluginResourceTracker owner, PlaceholderRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public String namespace() {
            return delegate.namespace();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedPluginMessageRegistration implements PluginMessageRegistration {

        private final PluginResourceTracker owner;
        private final PluginMessageRegistration delegate;
        private volatile boolean released;

        TrackedPluginMessageRegistration(PluginResourceTracker owner, PluginMessageRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedGameRuleRegistration implements GameRuleRegistration {

        private final PluginResourceTracker owner;
        private final GameRuleRegistration delegate;
        private volatile boolean released;

        TrackedGameRuleRegistration(PluginResourceTracker owner, GameRuleRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedDataPackRegistration implements DataPackRegistration {

        private final PluginResourceTracker owner;
        private final DataPackRegistration delegate;
        private volatile boolean released;

        TrackedDataPackRegistration(PluginResourceTracker owner, DataPackRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void enable() {
            ensureOpen();
            delegate.enable();
        }

        @Override
        public void disable() {
            if (!released) {
                delegate.disable();
            }
        }

        @Override
        public void delete() {
            if (!released) {
                released = true;
                try {
                    delegate.delete();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }

        private void ensureOpen() {
            if (released) {
                throw new IllegalStateException("Data pack registration is closed");
            }
        }
    }

    static final class TrackedResourcePackRegistration implements ResourcePackRegistration {

        private final PluginResourceTracker owner;
        private final ResourcePackRegistration delegate;
        private volatile boolean released;

        TrackedResourcePackRegistration(PluginResourceTracker owner, ResourcePackRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void delete() {
            if (!released) {
                released = true;
                try {
                    delegate.delete();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedRegionRegistration implements RegionRegistration {

        private final PluginResourceTracker owner;
        private final RegionRegistration delegate;
        private volatile boolean released;

        TrackedRegionRegistration(PluginResourceTracker owner, RegionRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedRegionFlagRegistration implements RegionFlagRegistration {

        private final PluginResourceTracker owner;
        private final RegionFlagRegistration delegate;
        private volatile boolean released;

        TrackedRegionFlagRegistration(PluginResourceTracker owner, RegionFlagRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedAdvancementRegistration implements AdvancementRegistration {

        private final PluginResourceTracker owner;
        private final AdvancementRegistration delegate;
        private volatile boolean released;

        TrackedAdvancementRegistration(PluginResourceTracker owner, AdvancementRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedEnchantmentRegistration implements EnchantmentRegistration {

        private final PluginResourceTracker owner;
        private final EnchantmentRegistration delegate;
        private volatile boolean released;

        TrackedEnchantmentRegistration(PluginResourceTracker owner, EnchantmentRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }
    }

    static final class TrackedStructureRegistration implements StructureRegistration {

        private final PluginResourceTracker owner;
        private final StructureRegistration delegate;
        private volatile boolean released;

        TrackedStructureRegistration(PluginResourceTracker owner, StructureRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    record MapRendererBinding(MapService service, int mapId, MapRenderer renderer) {
        MapRendererBinding {
            java.util.Objects.requireNonNull(service, "service");
            java.util.Objects.requireNonNull(renderer, "renderer");
        }

        void closeFromTracker() {
            if (service instanceof FandMapService maps) {
                maps.clearRenderer(mapId, renderer);
            }
        }
    }

    static final class TrackedScoreboardRegistration implements ScoreboardRegistration {

        private final PluginResourceTracker owner;
        private final ScoreboardRegistration delegate;
        private volatile boolean released;

        TrackedScoreboardRegistration(PluginResourceTracker owner, ScoreboardRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedCustomItemRegistration implements CustomItemRegistration {

        private final PluginResourceTracker owner;
        private final CustomItemRegistration delegate;
        private volatile boolean released;

        TrackedCustomItemRegistration(PluginResourceTracker owner, CustomItemRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public io.fand.api.item.custom.CustomItemType type() {
            return delegate.type();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedCustomBlockRegistration implements CustomBlockRegistration {

        private final PluginResourceTracker owner;
        private final CustomBlockRegistration delegate;
        private volatile boolean released;

        TrackedCustomBlockRegistration(PluginResourceTracker owner, CustomBlockRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public io.fand.api.block.custom.CustomBlockType type() {
            return delegate.type();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }

        @Override
        public CustomBlockItemBinding bindItem(net.kyori.adventure.key.Key itemId) {
            return owner.track(delegate.bindItem(itemId));
        }

        @Override
        public void unbindItem(net.kyori.adventure.key.Key itemId) {
            delegate.unbindItem(itemId);
        }
    }

    static final class TrackedCustomBlockItemBinding implements CustomBlockItemBinding {

        private final PluginResourceTracker owner;
        private final CustomBlockItemBinding delegate;
        private volatile boolean released;

        TrackedCustomBlockItemBinding(PluginResourceTracker owner, CustomBlockItemBinding delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key itemId() {
            return delegate.itemId();
        }

        @Override
        public net.kyori.adventure.key.Key blockId() {
            return delegate.blockId();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedGuiView implements GuiView {

        private final PluginResourceTracker owner;
        private final GuiView delegate;
        private volatile boolean released;

        TrackedGuiView(PluginResourceTracker owner, GuiView delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public java.util.UUID id() {
            return delegate.id();
        }

        @Override
        public io.fand.api.entity.Player player() {
            return delegate.player();
        }

        @Override
        public io.fand.api.gui.Gui gui() {
            return delegate.gui();
        }

        @Override
        public io.fand.api.inventory.Inventory inventory() {
            return delegate.inventory();
        }

        @Override
        public boolean open() {
            return !released && delegate.open();
        }

        @Override
        public io.fand.api.item.ItemStack cursorItem() {
            return delegate.cursorItem();
        }

        @Override
        public void setCursorItem(io.fand.api.item.ItemStack item) {
            if (!released) {
                delegate.setCursorItem(item);
            }
        }

        @Override
        public void setProperty(int id, int value) {
            if (!released) {
                delegate.setProperty(id, value);
            }
        }

        @Override
        public void reopen() {
            if (!released) {
                delegate.reopen();
            }
        }

        @Override
        public void replace(io.fand.api.gui.Gui gui) {
            if (!released) {
                delegate.replace(gui);
            }
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }

        void releaseFromExternalClose() {
            if (!released) {
                released = true;
                owner.release(this);
            }
        }

        @Override
        public java.util.Optional<Object> state(String key) {
            return delegate.state(key);
        }

        @Override
        public void state(String key, Object value) {
            delegate.state(key, value);
        }

        @Override
        public void removeState(String key) {
            delegate.removeState(key);
        }
    }

    static final class TrackedHologram implements Hologram {

        private final PluginResourceTracker owner;
        private final Hologram delegate;
        private volatile boolean released;

        TrackedHologram(PluginResourceTracker owner, Hologram delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public java.util.UUID id() {
            return delegate.id();
        }

        @Override
        public io.fand.api.world.Location location() {
            return delegate.location();
        }

        @Override
        public io.fand.api.hologram.HologramOptions options() {
            return delegate.options();
        }

        @Override
        public java.util.List<net.kyori.adventure.text.Component> lines() {
            return delegate.lines();
        }

        @Override
        public java.util.List<? extends io.fand.api.entity.TextDisplay> displays() {
            return delegate.displays();
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> teleport(io.fand.api.world.Location location) {
            ensureOpen();
            return delegate.teleport(location);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Void> setLines(
                java.util.List<? extends net.kyori.adventure.text.Component> lines
        ) {
            ensureOpen();
            return delegate.setLines(lines);
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                try {
                    delegate.close();
                } finally {
                    owner.release(this);
                }
            }
        }

        void closeFromTracker() {
            if (!released) {
                released = true;
                delegate.close();
            }
        }

        private void ensureOpen() {
            if (released) {
                throw new IllegalStateException("Hologram is closed");
            }
        }
    }

    static final class TrackedServiceRegistration<T> implements ServiceRegistration<T> {

        private final PluginResourceTracker owner;
        private final ServiceRegistration<T> delegate;
        private volatile boolean released;

        TrackedServiceRegistration(PluginResourceTracker owner, ServiceRegistration<T> delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public Class<T> type() {
            return delegate.type();
        }

        @Override
        public T service() {
            return delegate.service();
        }

        @Override
        public String owner() {
            return delegate.owner();
        }

        @Override
        public io.fand.api.service.ServicePriority priority() {
            return delegate.priority();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedNmsHookRegistration implements NmsHookRegistration {

        private final PluginResourceTracker owner;
        private final NmsHookRegistration delegate;
        private volatile boolean released;

        TrackedNmsHookRegistration(PluginResourceTracker owner, NmsHookRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key hook() {
            return delegate.hook();
        }

        @Override
        public io.fand.api.nms.NmsHook hookHandler() {
            return delegate.hookHandler();
        }

        @Override
        public String owner() {
            return delegate.owner();
        }

        @Override
        public io.fand.api.service.ServicePriority priority() {
            return delegate.priority();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }

    static final class TrackedLoginAuthenticationRegistration implements LoginAuthenticationRegistration {

        private final PluginResourceTracker owner;
        private final LoginAuthenticationRegistration delegate;
        private volatile boolean released;

        TrackedLoginAuthenticationRegistration(PluginResourceTracker owner, LoginAuthenticationRegistration delegate) {
            this.owner = owner;
            this.delegate = delegate;
        }

        @Override
        public net.kyori.adventure.key.Key key() {
            return delegate.key();
        }

        @Override
        public io.fand.api.auth.LoginAuthenticator authenticator() {
            return delegate.authenticator();
        }

        @Override
        public String owner() {
            return delegate.owner();
        }

        @Override
        public io.fand.api.service.ServicePriority priority() {
            return delegate.priority();
        }

        @Override
        public boolean active() {
            return !released && delegate.active();
        }

        @Override
        public void unregister() {
            if (!released) {
                released = true;
                try {
                    delegate.unregister();
                } finally {
                    owner.release(this);
                }
            }
        }

        void unregisterFromTracker() {
            if (!released) {
                released = true;
                delegate.unregister();
            }
        }
    }
}
