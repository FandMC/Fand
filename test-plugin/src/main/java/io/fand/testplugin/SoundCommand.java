package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.world.Sound;
import io.fand.api.world.Sounds;
import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandsound", permission = "fand.testplugin.sound")
final class SoundCommand implements CommandExecutor {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /fandsound <type|key> [volume] [pitch] [minVolume] [seed]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples: levelup 1 1, explode 4 0.8, minecraft:block.note_block.pling", NamedTextColor.GRAY));
            return;
        }
        var soundType = args.get(0).toLowerCase();
        float volume = parseFloat(sender, args, 1, 1.0f, 0.0f, 64.0f, "Volume");
        if (Float.isNaN(volume)) {
            return;
        }
        float pitch = parseFloat(sender, args, 2, 1.0f, 0.0f, 2.0f, "Pitch");
        if (Float.isNaN(pitch)) {
            return;
        }
        float minVolume = parseFloat(sender, args, 3, 0.0f, 0.0f, 1.0f, "Min volume");
        if (Float.isNaN(minVolume)) {
            return;
        }
        long seed = parseLong(sender, args, 4, 0L);
        if (seed == Long.MIN_VALUE) {
            return;
        }
        var sound = resolveSound(soundType);
        if (sound == null) {
            sender.sendMessage(Component.text("Unknown sound: " + soundType, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use a resource key like minecraft:block.note_block.pling for custom lookup.", NamedTextColor.GRAY));
            return;
        }
        var loc = player.location();
        var playback = sound.at(loc)
                .category(Sound.Category.MASTER)
                .volume(volume)
                .pitch(pitch)
                .minVolume(minVolume)
                .seed(seed);
        player.world().playSound(playback);
        player.sendMessage(Component.text(
                "Played " + sound.key().asString()
                        + " at " + String.format("%.1f %.1f %.1f", loc.x(), loc.y(), loc.z())
                        + " (volume=" + volume + ", pitch=" + pitch + ", minVolume=" + minVolume + ", seed=" + seed + ")",
                NamedTextColor.GREEN));
    }

    private Sound resolveSound(String soundType) {
        try {
            return switch (soundType) {
                case "levelup" -> Sounds.ENTITY_PLAYER_LEVELUP;
                case "xp", "orb" -> Sounds.ENTITY_EXPERIENCE_ORB_PICKUP;
                case "pling" -> Sounds.BLOCK_NOTE_BLOCK_PLING;
                case "anvil" -> Sounds.BLOCK_ANVIL_USE;
                case "villager_yes" -> Sounds.ENTITY_VILLAGER_YES;
                case "villager_no" -> Sounds.ENTITY_VILLAGER_NO;
                case "teleport" -> Sounds.ENTITY_ENDERMAN_TELEPORT;
                case "explode" -> Sounds.ENTITY_GENERIC_EXPLODE;
                case "chest_open" -> Sounds.BLOCK_CHEST_OPEN;
                case "chest_close" -> Sounds.BLOCK_CHEST_CLOSE;
                case "totem" -> Sounds.ITEM_TOTEM_USE;
                case "click" -> Sounds.UI_BUTTON_CLICK;
                case "hurt" -> Sounds.ENTITY_PLAYER_HURT;
                case "death" -> Sounds.ENTITY_PLAYER_DEATH;
                default -> soundType.contains(":") ? Sounds.sound(soundType) : null;
            };
        } catch (InvalidKeyException ex) {
            return null;
        }
    }

    private float parseFloat(CommandSender sender, List<String> args, int index, float fallback,
                             float min, float max, String label) {
        if (args.size() <= index) {
            return fallback;
        }
        try {
            float value = Float.parseFloat(args.get(index));
            if (value < min || value > max) {
                sender.sendMessage(Component.text(label + " must be " + min + "-" + max, NamedTextColor.RED));
                return Float.NaN;
            }
            return value;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(label + " must be a number", NamedTextColor.RED));
            return Float.NaN;
        }
    }

    private long parseLong(CommandSender sender, List<String> args, int index, long fallback) {
        if (args.size() <= index) {
            return fallback;
        }
        try {
            return Long.parseLong(args.get(index));
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Seed must be an integer", NamedTextColor.RED));
            return Long.MIN_VALUE;
        }
    }
}
