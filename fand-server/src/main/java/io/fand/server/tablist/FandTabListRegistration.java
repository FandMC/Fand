package io.fand.server.tablist;

import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListRegistration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class FandTabListRegistration implements TabListRegistration {

    private final UUID viewerId;
    private final UUID entryId;
    private final FandTabListService owner;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private volatile TabListEntry entry;

    FandTabListRegistration(UUID viewerId, TabListEntry entry, FandTabListService owner) {
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.entry = Objects.requireNonNull(entry, "entry");
        this.entryId = entry.profile().uniqueId();
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public UUID viewerId() {
        return viewerId;
    }

    @Override
    public UUID entryId() {
        return entryId;
    }

    @Override
    public boolean active() {
        return active.get();
    }

    TabListEntry entry() {
        return entry;
    }

    @Override
    public void update(TabListEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (!entryId.equals(entry.profile().uniqueId())) {
            throw new IllegalArgumentException("Cannot change tab-list entry id from " + entryId + " to " + entry.profile().uniqueId());
        }
        if (!active()) {
            throw new IllegalStateException("Tab-list entry is closed");
        }
        this.entry = entry;
        owner.sendUpdate(viewerId, entry);
    }

    @Override
    public void remove() {
        if (active.compareAndSet(true, false)) {
            owner.removeRegistration(viewerId, entryId, this);
        }
    }

    void closeFromService() {
        active.set(false);
    }
}
