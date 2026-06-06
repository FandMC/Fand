package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.Location;
import io.fand.api.world.Particles;
import io.fand.api.world.Sounds;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandeffect", permission = "fand.testplugin.effect")
final class EffectCommand implements CommandExecutor {

    private final PluginContext context;

    EffectCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /fandeffect <type>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Types: levelup, teleport, heal, explosion, magic", NamedTextColor.GRAY));
            return;
        }
        var effectType = args.get(0).toLowerCase();
        switch (effectType) {
            case "levelup" -> playLevelUpEffect(player);
            case "teleport" -> playTeleportEffect(player);
            case "heal" -> playHealEffect(player);
            case "explosion" -> playExplosionEffect(player);
            case "magic" -> playMagicEffect(player);
            default -> {
                sender.sendMessage(Component.text("Unknown effect: " + effectType, NamedTextColor.RED));
                return;
            }
        }
        sender.sendMessage(Component.text("Playing " + effectType + " effect", NamedTextColor.GREEN));
    }

    private void playLevelUpEffect(Player player) {
        Location loc = player.location();
        player.playSound(Sounds.ENTITY_PLAYER_LEVELUP.at(loc));
        player.world().spawnParticle(Particles.TOTEM_OF_UNDYING.at(loc).count(30).offset(0.5, 1.0, 0.5).speed(0.2));
        player.world().spawnParticle(Particles.HAPPY_VILLAGER.at(loc).count(20).offset(0.3, 0.5, 0.3).speed(0.1));
    }

    private void playTeleportEffect(Player player) {
        Location loc = player.location();
        player.world().spawnParticle(Particles.PORTAL.at(loc).count(50).offset(0.5, 1.0, 0.5).speed(1.0));
        player.playSound(Sounds.ENTITY_ENDERMAN_TELEPORT.at(loc));
        context.scheduler().runMainAfter(() -> {
            Location newLoc = player.location();
            player.world().spawnParticle(Particles.PORTAL.at(newLoc).count(50).offset(0.5, 1.0, 0.5).speed(1.0));
        }, Duration.ofMillis(500));
    }

    private void playHealEffect(Player player) {
        Location loc = player.location();
        player.world().spawnParticle(Particles.HEART.at(loc).count(10).offset(0.5, 0.5, 0.5).speed(0.1));
        player.world().spawnParticle(Particles.GLOW.at(loc).count(15).offset(0.3, 0.8, 0.3).speed(0.05));
        player.playSound(Sounds.BLOCK_NOTE_BLOCK_PLING.at(loc).pitch(1.5f));
    }

    private void playExplosionEffect(Player player) {
        Location loc = player.location();
        player.world().spawnParticle(Particles.EXPLOSION.at(loc).count(1));
        player.world().spawnParticle(Particles.SMOKE.at(loc).count(30).offset(1.0, 1.0, 1.0).speed(0.1));
        player.world().spawnParticle(Particles.FLAME.at(loc).count(20).offset(0.8, 0.8, 0.8).speed(0.2));
        player.playSound(Sounds.ENTITY_GENERIC_EXPLODE.at(loc).volume(0.8f).pitch(1.2f));
    }

    private void playMagicEffect(Player player) {
        Location loc = player.location();
        for (int i = 0; i < 5; i++) {
            int delay = i * 100;
            context.scheduler().runMainAfter(() -> {
                Location spiralLoc = player.location();
                player.world().spawnParticle(Particles.END_ROD.at(spiralLoc).count(3).offset(0.2, 0.5, 0.2).speed(0.05));
                player.world().spawnParticle(Particles.ENCHANTED_HIT.at(spiralLoc).count(5).offset(0.3, 0.3, 0.3).speed(0.1));
            }, Duration.ofMillis(delay));
        }
        player.playSound(Sounds.BLOCK_NOTE_BLOCK_PLING.at(loc).volume(0.8f).pitch(0.8f));
        context.scheduler().runMainAfter(() -> {
            Location finalLoc = player.location();
            player.playSound(Sounds.BLOCK_NOTE_BLOCK_PLING.at(finalLoc).pitch(1.2f));
        }, Duration.ofMillis(400));
    }
}
