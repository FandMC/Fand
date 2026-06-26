package io.fand.api.tablist;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * A tab-list row mirrored from another server or proxy.
 */
public record RemoteTabListEntry(Key source, TabListEntry entry) {

    public RemoteTabListEntry {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(entry, "entry");
    }
}
