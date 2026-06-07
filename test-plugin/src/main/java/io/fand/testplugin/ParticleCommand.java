package io.fand.testplugin;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.world.Location;
import io.fand.api.world.Particle;
import io.fand.api.world.Particles;
import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandparticle", permission = "fand.testplugin.particle")
final class ParticleCommand implements CommandExecutor, CommandCompleter {

    private static final List<String> PARTICLE_SUGGESTIONS = List.of(
            "flame",
            "heart",
            "explosion",
            "portal",
            "smoke",
            "crit",
            "cloud",
            "note",
            "happy",
            "angry",
            "totem",
            "end_rod",
            "dragon",
            "soul",
            "glow",
            "dust",
            "block",
            "item",
            "shriek",
            "sculk",
            "minecraft:flame"
    );
    private static final List<String> FUNCTION_SUGGESTIONS = List.of("circle", "helix", "sine", "rose", "lissajous");
    private static final List<String> COUNT_SUGGESTIONS = List.of("10", "50", "100");

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /fandparticle <type|key|function> [args...]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Types: flame, heart, dust, block, item, shriek, minecraft:dust{...}", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Functions: circle <type>, helix <type>, sine <type>, rose <type>, lissajous <type>", NamedTextColor.GRAY));
            return;
        }
        var particleType = args.get(0).toLowerCase();
        Location loc = player.location();
        switch (particleType) {
            case "circle" -> {
                Particle particle = parseParticle(sender, args, 1, Particles.FLAME);
                if (particle == null) return;
                double radius = parseDouble(sender, args, 2, 2.0, 0.1, 32.0, "Radius");
                if (Double.isNaN(radius)) return;
                int points = parseInt(sender, args, 3, 80, 1, 2000, "Points");
                if (points < 0) return;
                plotParticle(player, particle, loc, ParticleFunctions.circle(radius, 1.0), 0.0, Math.PI * 2.0, points);
                player.sendMessage(Component.text("Plotted circle function", NamedTextColor.GREEN));
                return;
            }
            case "helix" -> {
                Particle particle = parseParticle(sender, args, 1, Particles.PORTAL);
                if (particle == null) return;
                double radius = parseDouble(sender, args, 2, 1.0, 0.1, 32.0, "Radius");
                if (Double.isNaN(radius)) return;
                plotParticle(player, particle, loc, ParticleFunctions.helix(radius, 1.0), 0.0, Math.PI * 6.0, 160);
                player.sendMessage(Component.text("Plotted helix function", NamedTextColor.GREEN));
                return;
            }
            case "sine" -> {
                Particle particle = parseParticle(sender, args, 1, Particles.CRIT);
                if (particle == null) return;
                plotParticle(player, particle, loc, ParticleFunctions.sine(1.5, 0.5), -Math.PI * 4.0, Math.PI * 4.0, 140);
                player.sendMessage(Component.text("Plotted sine function", NamedTextColor.GREEN));
                return;
            }
            case "rose" -> {
                Particle particle = parseParticle(sender, args, 1, Particles.HAPPY_VILLAGER);
                if (particle == null) return;
                plotParticle(player, particle, loc, ParticleFunctions.rose(3.0, 5), 0.0, Math.PI * 2.0, 220);
                player.sendMessage(Component.text("Plotted rose function", NamedTextColor.GREEN));
                return;
            }
            case "lissajous" -> {
                Particle particle = parseParticle(sender, args, 1, Particles.END_ROD);
                if (particle == null) return;
                plotParticle(player, particle, loc, ParticleFunctions.lissajous(2.0, 2.0, 2.0, 3.0, 4.0, 5.0), 0.0, Math.PI * 2.0, 240);
                player.sendMessage(Component.text("Plotted Lissajous function", NamedTextColor.GREEN));
                return;
            }
            default -> {
            }
        }
        int count = 10;
        if (args.size() > 1) {
            count = parseInt(sender, args, 1, 10, 1, 1000, "Count");
            if (count < 0) return;
        }
        var particle = resolveParticle(particleType);
        if (particle == null) {
            sender.sendMessage(Component.text("Unknown or invalid particle: " + particleType, NamedTextColor.RED));
            sender.sendMessage(Component.text("Try minecraft:flame or minecraft:dust{color:16711680,scale:1.0}", NamedTextColor.GRAY));
            return;
        }
        player.world().spawnParticle(particle.at(loc.x(), loc.y() + 1.5, loc.z())
                .count(count)
                .offset(0.5, 0.5, 0.5)
                .speed(0.1));
        player.sendMessage(Component.text("Spawned " + count + " " + particle.argument() + " particles", NamedTextColor.GREEN));
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return switch (args.size()) {
            case 0, 1 -> matching(allRootSuggestions(), args.isEmpty() ? "" : args.get(0));
            case 2 -> FUNCTION_SUGGESTIONS.contains(args.get(0).toLowerCase())
                    ? matching(PARTICLE_SUGGESTIONS, args.get(1))
                    : matching(COUNT_SUGGESTIONS, args.get(1));
            default -> List.of();
        };
    }

    private Particle parseParticle(CommandSender sender, List<String> args, int index, Particle fallback) {
        if (args.size() <= index) {
            return fallback;
        }
        Particle particle = resolveParticle(args.get(index).toLowerCase());
        if (particle == null) {
            sender.sendMessage(Component.text("Unknown or invalid particle: " + args.get(index), NamedTextColor.RED));
        }
        return particle;
    }

    private void plotParticle(Player player, Particle particle, Location origin, ParticleFunctions.Function function,
                              double from, double to, int samples) {
        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0.0 : (double) i / (samples - 1);
            double t = from + (to - from) * progress;
            var point = function.apply(t);
            player.world().spawnParticle(particle.at(
                    origin.x() + point.x(),
                    origin.y() + point.y(),
                    origin.z() + point.z()));
        }
    }

    private Particle resolveParticle(String particleType) {
        try {
            return switch (particleType) {
                case "flame" -> Particles.FLAME;
                case "heart" -> Particles.HEART;
                case "explosion" -> Particles.EXPLOSION;
                case "portal" -> Particles.PORTAL;
                case "smoke" -> Particles.SMOKE;
                case "crit" -> Particles.CRIT;
                case "cloud" -> Particles.CLOUD;
                case "note" -> Particles.NOTE;
                case "happy" -> Particles.HAPPY_VILLAGER;
                case "angry" -> Particles.ANGRY_VILLAGER;
                case "totem" -> Particles.TOTEM_OF_UNDYING;
                case "end_rod" -> Particles.END_ROD;
                case "dragon" -> Particles.DRAGON_BREATH;
                case "soul" -> Particles.SOUL_FIRE_FLAME;
                case "glow" -> Particles.GLOW;
                case "dust" -> Particles.dust(0xFF5555, 1.0F);
                case "block" -> Particles.block("minecraft:stone");
                case "item" -> Particles.item("minecraft:diamond");
                case "shriek" -> Particles.shriek(0);
                case "sculk" -> Particles.sculkCharge(0.0F);
                default -> particleType.contains(":") ? Particles.raw(particleType) : null;
            };
        } catch (InvalidKeyException ex) {
            return null;
        }
    }

    private int parseInt(CommandSender sender, List<String> args, int index, int fallback, int min, int max, String label) {
        if (args.size() <= index) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(args.get(index));
            if (value < min || value > max) {
                sender.sendMessage(Component.text(label + " must be " + min + "-" + max, NamedTextColor.RED));
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(label + " must be a number", NamedTextColor.RED));
            return -1;
        }
    }

    private double parseDouble(CommandSender sender, List<String> args, int index, double fallback,
                               double min, double max, String label) {
        if (args.size() <= index) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(args.get(index));
            if (!Double.isFinite(value) || value < min || value > max) {
                sender.sendMessage(Component.text(label + " must be " + min + "-" + max, NamedTextColor.RED));
                return Double.NaN;
            }
            return value;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(label + " must be a number", NamedTextColor.RED));
            return Double.NaN;
        }
    }

    private List<String> allRootSuggestions() {
        return java.util.stream.Stream.concat(PARTICLE_SUGGESTIONS.stream(), FUNCTION_SUGGESTIONS.stream()).toList();
    }

    private List<String> matching(List<String> options, String prefix) {
        var lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.startsWith(lowerPrefix))
                .toList();
    }
}
