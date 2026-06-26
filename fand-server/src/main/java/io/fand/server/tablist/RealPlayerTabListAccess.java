package io.fand.server.tablist;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.UUID;

public interface RealPlayerTabListAccess {

    Collection<UUID> showOnlyCandidateIds(Player viewer, Collection<? extends Player> visibleTargets);

    boolean visibleInRealPlayerList(UUID viewerId, UUID targetId);

    void setRealEntryVisible(UUID viewerId, UUID targetId, boolean visible);
}
