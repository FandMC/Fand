package io.fand.server.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class PluginTabListVisibilityRegistry {

    private final Map<VisibilityKey, Set<Object>> hiddenBy = new HashMap<>();

    synchronized void hide(Object owner, UUID viewerId, UUID targetId, Runnable hide) {
        Objects.requireNonNull(owner, "owner");
        var owners = hiddenBy.computeIfAbsent(new VisibilityKey(viewerId, targetId), ignored -> new HashSet<>());
        if (owners.add(owner) && owners.size() == 1) {
            hide.run();
        }
    }

    synchronized void show(Object owner, UUID viewerId, UUID targetId, Runnable show) {
        var key = new VisibilityKey(viewerId, targetId);
        var owners = hiddenBy.get(key);
        if (owners == null || !owners.remove(owner)) {
            return;
        }
        if (owners.isEmpty()) {
            hiddenBy.remove(key);
            show.run();
        }
    }

    private record VisibilityKey(UUID viewerId, UUID targetId) {

        private VisibilityKey {
            Objects.requireNonNull(viewerId, "viewerId");
            Objects.requireNonNull(targetId, "targetId");
        }
    }
}
