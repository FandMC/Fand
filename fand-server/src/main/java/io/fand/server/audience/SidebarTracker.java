package io.fand.server.audience;

import io.fand.api.scoreboard.Sidebar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Sends a transient per-player sidebar without mutating the server scoreboard.
 *
 * <p>Not thread-safe. FandPlayer marshals calls onto the main server thread.
 */
public final class SidebarTracker {

    private static final String OBJECTIVE_NAME = "fand_sidebar";
    private static final String OWNER_PREFIX = "fand:sidebar:";

    private ServerPlayer player;
    private Objective objective;
    private Sidebar sidebar;
    private List<String> owners = List.of();
    private boolean resendDisplay;

    public SidebarTracker(ServerPlayer player) {
        this.player = player;
    }

    public void rebind(ServerPlayer freshHandle) {
        this.player = freshHandle;
        var current = sidebar;
        if (current == null) {
            return;
        }
        this.owners = List.of();
        this.resendDisplay = true;
        show(current);
    }

    public void show(Sidebar next) {
        Objects.requireNonNull(next, "sidebar");
        sidebar = next;
        var objective = ensureObjective(next);
        var nextOwners = ownersFor(next.lines().size());
        resetRemovedOwners(nextOwners);
        sendScores(next, nextOwners);
        owners = nextOwners;
    }

    public void clear() {
        var shownObjective = objective;
        if (shownObjective == null) {
            sidebar = null;
            owners = List.of();
            return;
        }
        for (var owner : owners) {
            PacketAudience.send(player, new ClientboundResetScorePacket(owner, OBJECTIVE_NAME));
        }
        PacketAudience.send(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
        PacketAudience.send(player, new ClientboundSetObjectivePacket(
                shownObjective,
                ClientboundSetObjectivePacket.METHOD_REMOVE));
        objective = null;
        sidebar = null;
        owners = List.of();
        resendDisplay = false;
    }

    private Objective ensureObjective(Sidebar next) {
        var title = PacketAudience.toVanilla(player, next.title());
        if (objective == null) {
            objective = new Objective(
                    new Scoreboard(),
                    OBJECTIVE_NAME,
                    ObjectiveCriteria.DUMMY,
                    title,
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    BlankFormat.INSTANCE);
            PacketAudience.send(player, new ClientboundSetObjectivePacket(
                    objective,
                    ClientboundSetObjectivePacket.METHOD_ADD));
            PacketAudience.send(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
            resendDisplay = false;
            return objective;
        }
        objective.setDisplayName(title);
        PacketAudience.send(player, new ClientboundSetObjectivePacket(
                objective,
                ClientboundSetObjectivePacket.METHOD_CHANGE));
        if (resendDisplay) {
            PacketAudience.send(player, new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
            resendDisplay = false;
        }
        return objective;
    }

    private void resetRemovedOwners(List<String> nextOwners) {
        if (owners.isEmpty()) {
            return;
        }
        var retained = new HashSet<>(nextOwners);
        for (var owner : owners) {
            if (!retained.contains(owner)) {
                PacketAudience.send(player, new ClientboundResetScorePacket(owner, OBJECTIVE_NAME));
            }
        }
    }

    private void sendScores(Sidebar next, List<String> nextOwners) {
        for (int i = 0; i < next.lines().size(); i++) {
            var line = next.lines().get(i);
            PacketAudience.send(player, new ClientboundSetScorePacket(
                    nextOwners.get(i),
                    OBJECTIVE_NAME,
                    scoreForIndex(i),
                    Optional.of(PacketAudience.toVanilla(player, line.text())),
                    Optional.of(BlankFormat.INSTANCE)));
        }
    }

    static List<String> ownersFor(int lineCount) {
        if (lineCount < 0 || lineCount > Sidebar.MAX_LINES) {
            throw new IllegalArgumentException("lineCount must be in [0, " + Sidebar.MAX_LINES + "]");
        }
        return java.util.stream.IntStream.range(0, lineCount)
                .mapToObj(SidebarTracker::ownerForIndex)
                .toList();
    }

    static String ownerForIndex(int index) {
        if (index < 0 || index >= Sidebar.MAX_LINES) {
            throw new IllegalArgumentException("index must be in [0, " + Sidebar.MAX_LINES + ")");
        }
        return OWNER_PREFIX + index;
    }

    static int scoreForIndex(int index) {
        if (index < 0 || index >= Sidebar.MAX_LINES) {
            throw new IllegalArgumentException("index must be in [0, " + Sidebar.MAX_LINES + ")");
        }
        return Sidebar.MAX_LINES - index;
    }
}
