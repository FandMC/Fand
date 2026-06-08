package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.world.sound.SoundCategory;
import io.fand.api.world.sound.SoundKey;
import io.fand.api.world.sound.Sounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandsound", arguments = {"player", "mode"}, aliases = {"fsound"}, permission = "fand.testplugin.sound")
final class SoundCommand implements CommandExecutor, CommandCompleter {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandsound <player> [orb|levelup|anvil|toast|world]");
        if (targeted == null) {
            return;
        }
        String mode = targeted.args().isEmpty() ? "orb" : targeted.args().getFirst().toLowerCase(Locale.ROOT);
        if (!SOUND_MODES.contains(mode)) {
            sender.sendMessage(Component.text("Usage: /fandsound <player> [orb|levelup|anvil|toast|world]", NamedTextColor.RED));
            return;
        }

        var target = targeted.player();
        var location = target.location();
        if (mode.equals("orb")) {
            target.playSound(Sounds.effect(SoundKey.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYER)
                    .withVolume(0.8F)
                    .withPitch(1.35F));
        } else if (mode.equals("levelup")) {
            target.playSound(location, Sounds.effect(SoundKey.PLAYER_LEVELUP, SoundCategory.PLAYER)
                    .withVolume(0.9F)
                    .withPitch(1.0F));
        } else if (mode.equals("anvil")) {
            target.playSound(location, Sounds.effect(SoundKey.ANVIL_USE, SoundCategory.BLOCK)
                    .withVolume(0.75F)
                    .withPitch(1.15F));
        } else if (mode.equals("toast")) {
            target.playSound(Sounds.effect(SoundKey.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER)
                    .withVolume(0.9F)
                    .withPitch(1.0F));
        } else {
            target.world().playSound(location, Sounds.effect(SoundKey.NOTE_BLOCK_PLING, SoundCategory.RECORD)
                    .withVolume(1.0F)
                    .withPitch(1.4F));
        }

        target.sendMessage(Component.text("Played Fand sound demo: " + mode, NamedTextColor.AQUA));
        if (target != sender) {
            sender.sendMessage(Component.text("Played sound demo for " + target.name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            if (sender instanceof Player) {
                values.addAll(SOUND_MODES);
            }
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(SOUND_MODES, args.getLast());
        }
        return List.of();
    }
}
