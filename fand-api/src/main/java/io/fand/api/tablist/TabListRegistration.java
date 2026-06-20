package io.fand.api.tablist;

import java.util.UUID;

/**
 * Viewer-scoped virtual or remote tab-list row registration.
 */
public interface TabListRegistration extends AutoCloseable {

    UUID viewerId();

    UUID entryId();

    boolean active();

    void update(TabListEntry entry);

    void remove();

    @Override
    default void close() {
        remove();
    }
}
