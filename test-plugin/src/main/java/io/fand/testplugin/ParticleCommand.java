package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.block.BlockKey;
import io.fand.api.block.BlockTypes;
import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemTypes;
import io.fand.api.world.particle.ParticleColor;
import io.fand.api.world.particle.ParticleEmission;
import io.fand.api.world.particle.ParticleKey;
import io.fand.api.world.particle.Particles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandparticle", arguments = {"player", "mode"}, aliases = {"fparticle"}, permission = "fand.testplugin.particle")
final class ParticleCommand implements TestCommandHandler, TestCommandTabHandler {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandparticle <player> [all|simple|dust|block|item|trail|vibration]");
        if (targeted == null) {
            return;
        }
        String mode = targeted.args().isEmpty() ? "all" : targeted.args().getFirst().toLowerCase(Locale.ROOT);
        if (!PARTICLE_MODES.contains(mode)) {
            sender.sendMessage(Component.text("Usage: /fandparticle <player> [all|simple|dust|block|item|trail|vibration]", NamedTextColor.RED));
            return;
        }

        var target = targeted.player();
        var base = target.location().offset(0.0, 1.1, 0.0);
        if (mode.equals("all") || mode.equals("simple")) {
            target.spawnParticle(base, Particles.simple(ParticleKey.HAPPY_VILLAGER),
                    ParticleEmission.count(20).withOffset(0.45, 0.45, 0.45).withSpeed(0.05));
        }
        if (mode.equals("all") || mode.equals("dust")) {
            target.spawnParticle(base.offset(0.0, 0.4, 0.0),
                    Particles.dust(ParticleColor.rgb(0x33CCFF), 1.4F),
                    ParticleEmission.count(24).withOffset(0.35, 0.35, 0.35));
            target.spawnParticle(base.offset(0.0, 0.8, 0.0),
                    Particles.dustTransition(ParticleColor.rgb(0xFFAA00), ParticleColor.rgb(0x66FF99), 1.2F),
                    ParticleEmission.count(16).withOffset(0.25, 0.25, 0.25));
        }
        if (mode.equals("all") || mode.equals("block")) {
            target.world().spawnParticle(base,
                    Particles.block(BlockTypes.of(BlockKey.GLASS)),
                    ParticleEmission.count(32).withOffset(0.4, 0.35, 0.4).withSpeed(0.03));
            target.world().spawnParticle(base.offset(0.0, 0.6, 0.0),
                    Particles.fallingDust(BlockTypes.of(BlockKey.REDSTONE_BLOCK)),
                    ParticleEmission.count(20).withOffset(0.25, 0.2, 0.25));
        }
        if (mode.equals("all") || mode.equals("item")) {
            target.spawnParticle(base,
                    Particles.item(demoComponentItem(ItemTypes.of(ItemKey.DIAMOND), "particle")),
                    ParticleEmission.count(18).withOffset(0.35, 0.45, 0.35).withSpeed(0.08));
        }
        if (mode.equals("all") || mode.equals("trail")) {
            target.spawnParticle(base,
                    Particles.trail(base.offset(0.0, 1.8, 0.0), ParticleColor.rgb(0xFF66CC), 35),
                    ParticleEmission.SINGLE);
        }
        if (mode.equals("all") || mode.equals("vibration")) {
            target.world().spawnParticle(base.offset(1.5, 0.0, 0.0),
                    Particles.vibration(base.offset(0.0, 0.2, 0.0), 30),
                    ParticleEmission.SINGLE.withAlwaysShow(true));
        }

        target.sendMessage(Component.text("Spawned Fand particle demo: " + mode, NamedTextColor.AQUA));
        if (target != sender) {
            sender.sendMessage(Component.text("Spawned particle demo for " + target.name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            if (sender instanceof Player) {
                values.addAll(PARTICLE_MODES);
            }
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(PARTICLE_MODES, args.getLast());
        }
        return List.of();
    }
}
