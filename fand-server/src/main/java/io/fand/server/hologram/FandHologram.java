package io.fand.server.hologram;

import io.fand.api.entity.Display;
import io.fand.api.entity.Entity;
import io.fand.api.entity.EntityTypes;
import io.fand.api.entity.TextDisplay;
import io.fand.api.hologram.Hologram;
import io.fand.api.hologram.HologramOptions;
import io.fand.api.world.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

final class FandHologram implements Hologram {

    private static final Key TEXT_DISPLAY = Key.key("minecraft", "text_display");

    private final UUID id = UUID.randomUUID();
    private final FandHologramService owner;
    private final HologramOptions options;
    private final Object lock = new Object();
    private Location location;
    private List<Component> lines;
    private List<TextDisplay> displays = List.of();
    private long revision;
    private boolean active = true;

    FandHologram(
            FandHologramService owner,
            Location location,
            List<? extends Component> lines,
            HologramOptions options
    ) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.location = Objects.requireNonNull(location, "location");
        this.lines = copyLines(lines);
        this.options = Objects.requireNonNull(options, "options");
    }

    CompletableFuture<FandHologram> spawn() {
        return rebuild(location, lines).thenApply(ignored -> this);
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Location location() {
        synchronized (lock) {
            return location;
        }
    }

    @Override
    public HologramOptions options() {
        return options;
    }

    @Override
    public List<Component> lines() {
        synchronized (lock) {
            return lines;
        }
    }

    @Override
    public List<? extends TextDisplay> displays() {
        synchronized (lock) {
            return displays;
        }
    }

    @Override
    public CompletableFuture<Void> teleport(Location location) {
        Objects.requireNonNull(location, "location");
        List<TextDisplay> current;
        synchronized (lock) {
            ensureActive();
            revision++;
            this.location = location;
            current = displays;
        }
        var futures = new ArrayList<CompletableFuture<Boolean>>(current.size());
        for (int index = 0; index < current.size(); index++) {
            futures.add(current.get(index).teleport(lineLocation(location, index)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public CompletableFuture<Void> setLines(List<? extends Component> lines) {
        var copy = copyLines(lines);
        Location currentLocation;
        long nextRevision;
        synchronized (lock) {
            ensureActive();
            nextRevision = ++revision;
            currentLocation = location;
        }
        return rebuild(currentLocation, copy, nextRevision);
    }

    @Override
    public boolean active() {
        synchronized (lock) {
            return active;
        }
    }

    @Override
    public void close() {
        List<TextDisplay> removed;
        synchronized (lock) {
            if (!active) {
                return;
            }
            active = false;
            removed = displays;
            displays = List.of();
        }
        for (var display : removed) {
            display.remove();
        }
        owner.removeFromService(id, this);
    }

    private CompletableFuture<Void> rebuild(Location location, List<Component> lines) {
        long targetRevision;
        synchronized (lock) {
            ensureActive();
            targetRevision = ++revision;
        }
        return rebuild(location, lines, targetRevision);
    }

    private CompletableFuture<Void> rebuild(Location location, List<Component> lines, long targetRevision) {
        if (lines.isEmpty()) {
            List<TextDisplay> oldDisplays;
            synchronized (lock) {
                ensureActive();
                if (targetRevision != revision) {
                    return CompletableFuture.completedFuture(null);
                }
                this.lines = lines;
                oldDisplays = displays;
                displays = List.of();
            }
            oldDisplays.forEach(Entity::remove);
            return CompletableFuture.completedFuture(null);
        }

        var futures = new ArrayList<CompletableFuture<TextDisplay>>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            futures.add(spawnLine(location, lines.get(index), index));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).handle((ignored, failure) -> {
            var spawned = new ArrayList<TextDisplay>(futures.size());
            for (var future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
                    spawned.add(future.join());
                }
            }
            if (failure != null) {
                spawned.forEach(Entity::remove);
                if (failure instanceof java.util.concurrent.CompletionException completion) {
                    throw completion;
                }
                throw new java.util.concurrent.CompletionException(failure);
            }
            List<TextDisplay> oldDisplays;
            synchronized (lock) {
                if (!active) {
                    spawned.forEach(Entity::remove);
                    return null;
                }
                if (targetRevision != revision) {
                    spawned.forEach(Entity::remove);
                    return null;
                }
                this.lines = lines;
                oldDisplays = displays;
                displays = List.copyOf(spawned);
            }
            oldDisplays.forEach(Entity::remove);
            return null;
        });
    }

    private CompletableFuture<TextDisplay> spawnLine(Location base, Component line, int index) {
        return base.world()
                .spawnEntity(lineLocation(base, index), EntityTypes.of(TEXT_DISPLAY))
                .thenApply(entity -> {
                    var display = entity
                            .filter(TextDisplay.class::isInstance)
                            .map(TextDisplay.class::cast)
                            .orElseThrow(() -> new IllegalStateException("Failed to spawn text display for hologram " + id));
                    configure(display, line);
                    return display;
                });
    }

    private void configure(TextDisplay display, Component line) {
        display.setText(line);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setLineWidth(options.lineWidth());
        display.setTextOpacity(options.textOpacity());
        display.setBackgroundColor(options.backgroundColor());
        display.setShadowed(options.shadowed());
        display.setSeeThrough(options.seeThrough());
        display.setDefaultBackground(options.defaultBackground());
        display.setAlignment(options.alignment());
        display.setBillboard(options.billboard());
        display.setViewRange(options.viewRange());
        display.setDisplayWidth(0.0F);
        display.setDisplayHeight(0.0F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
    }

    private Location lineLocation(Location base, int index) {
        return base.offset(0.0D, -index * options.lineSpacing(), 0.0D);
    }

    private static List<Component> copyLines(List<? extends Component> lines) {
        Objects.requireNonNull(lines, "lines");
        var copy = new ArrayList<Component>(lines.size());
        for (var line : lines) {
            copy.add(Objects.requireNonNull(line, "lines cannot contain null"));
        }
        return List.copyOf(copy);
    }

    private void ensureActive() {
        if (!active) {
            throw new IllegalStateException("Hologram is closed");
        }
    }
}
