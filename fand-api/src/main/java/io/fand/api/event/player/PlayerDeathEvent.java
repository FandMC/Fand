package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread when a player dies, before the death message is
 * broadcast, before inventory drops are spawned, and before the player is sent
 * the respawn screen.
 *
 * <p>The default state mirrors vanilla for the current server rules. Plugins
 * may replace the broadcast message, replace the dropped items, change the
 * dropped XP amount, or keep inventory/XP data.
 */
public final class PlayerDeathEvent implements Event {

    private final Player player;
    private final boolean keepInventoryByDefault;
    private final int droppedExperienceByDefault;
    private @Nullable Component deathMessage;
    private List<ItemStack> drops;
    private int droppedExperience;
    private boolean keepInventory;
    private boolean keepExperience;

    public PlayerDeathEvent(Player player, @Nullable Component deathMessage) {
        this(player, deathMessage, List.of(), 0, false);
    }

    public PlayerDeathEvent(
            Player player,
            @Nullable Component deathMessage,
            List<ItemStack> drops,
            int droppedExperience,
            boolean keepInventory) {
        this.player = Objects.requireNonNull(player, "player");
        this.deathMessage = deathMessage;
        this.keepInventoryByDefault = keepInventory;
        this.droppedExperienceByDefault = Math.max(0, droppedExperience);
        this.drops = copyDrops(drops);
        this.droppedExperience = this.droppedExperienceByDefault;
        this.keepInventory = keepInventory;
        this.keepExperience = keepInventory;
    }

    public Player player() {
        return player;
    }

    /**
     * Message that will be broadcast for this death. {@code null} suppresses
     * the broadcast entirely.
     */
    public @Nullable Component deathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(@Nullable Component deathMessage) {
        this.deathMessage = deathMessage;
    }

    /**
     * Item stacks that will be spawned for this death. The returned list is a
     * copy; use {@link #setDrops(List)} to replace the actual drops.
     */
    public List<ItemStack> drops() {
        return List.copyOf(drops);
    }

    /**
     * Replaces the full death-drop list. Passing an empty list suppresses item
     * drops for this death without implying inventory retention. Empty stacks
     * in the input list are ignored.
     */
    public void setDrops(List<ItemStack> drops) {
        this.drops = copyDrops(drops);
    }

    /** Experience points that will be dropped as XP orbs. */
    public int droppedExperience() {
        return droppedExperience;
    }

    public void setDroppedExperience(int droppedExperience) {
        this.droppedExperience = Math.max(0, droppedExperience);
    }

    public int droppedExperienceByDefault() {
        return droppedExperienceByDefault;
    }

    /** Whether the player's inventory will be preserved by the death flow. */
    public boolean keepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public boolean keepInventoryByDefault() {
        return keepInventoryByDefault;
    }

    /** Whether XP level, total XP, and progress should survive the death. */
    public boolean keepExperience() {
        return keepExperience;
    }

    public void setKeepExperience(boolean keepExperience) {
        this.keepExperience = keepExperience;
    }

    private static List<ItemStack> copyDrops(List<ItemStack> drops) {
        Objects.requireNonNull(drops, "drops");
        var copy = new ArrayList<ItemStack>();
        for (var drop : drops) {
            Objects.requireNonNull(drop, "drop");
            if (!drop.empty()) {
                copy.add(drop);
            }
        }
        return copy;
    }
}
