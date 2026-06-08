package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.parseDouble;
import static io.fand.testplugin.DemoSupport.parseInt;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.Player;
import io.fand.api.world.Vector3;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class EntitySpawnOptionParser {

    static final List<String> FLAGS = List.of(
            "--glow",
            "--silent",
            "--no-gravity",
            "--invulnerable",
            "--name",
            "--velocity",
            "--fire",
            "--no-ai",
            "--persistent",
            "--target-self",
            "--shoot",
            "--pickup-delay",
            "--unlimited-lifetime");

    private EntitySpawnOptionParser() {
    }

    static int firstOptionIndex(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).startsWith("--")) {
                return i;
            }
        }
        return args.size();
    }

    static EntitySpawnOptions parse(CommandSender sender, List<String> args, int start) {
        var builder = EntitySpawnOptions.builder();
        for (int i = start; i < args.size(); i++) {
            String flag = args.get(i).toLowerCase(Locale.ROOT);
            switch (flag) {
                case "--glow" -> builder.glowing(true);
                case "--silent" -> builder.silent(true);
                case "--no-gravity" -> builder.gravity(false);
                case "--invulnerable" -> builder.invulnerable(true);
                case "--no-ai" -> builder.noAi(true);
                case "--persistent" -> builder.persistent(true);
                case "--unlimited-lifetime" -> builder.unlimitedLifetime(true);
                case "--target-self" -> {
                    if (sender instanceof Player player) {
                        builder.target(player);
                    } else {
                        sender.sendMessage(Component.text("--target-self requires a player sender.", NamedTextColor.RED));
                        return null;
                    }
                }
                case "--name" -> {
                    String name = nextString(sender, args, ++i, flag);
                    if (name == null) {
                        return null;
                    }
                    builder.customName(Component.text(name.replace('_', ' '))).customNameVisible(true);
                }
                case "--velocity" -> {
                    Vector3 velocity = nextVector(sender, args, i + 1, flag);
                    if (velocity == null) {
                        return null;
                    }
                    builder.velocity(velocity);
                    i += 3;
                }
                case "--fire" -> {
                    Integer ticks = nextInt(sender, args, ++i, flag);
                    if (ticks == null) {
                        return null;
                    }
                    builder.fireTicks(ticks);
                }
                case "--shoot" -> {
                    Vector3 direction = nextVector(sender, args, i + 1, flag);
                    if (direction == null) {
                        return null;
                    }
                    i += 3;
                    double power = 1.5;
                    if (i + 1 < args.size() && !args.get(i + 1).startsWith("--")) {
                        Double parsedPower = parseDouble(sender, args.get(i + 1), flag + " power");
                        if (parsedPower == null) {
                            return null;
                        }
                        power = parsedPower;
                        i++;
                    }
                    builder.projectile(direction, power, 0.0);
                    if (sender instanceof Player player) {
                        builder.projectileShooter(player);
                    }
                }
                case "--pickup-delay" -> {
                    Integer ticks = nextInt(sender, args, ++i, flag);
                    if (ticks == null) {
                        return null;
                    }
                    builder.pickupDelay(ticks);
                }
                default -> {
                    sender.sendMessage(Component.text("Unknown spawn option: " + args.get(i), NamedTextColor.RED));
                    return null;
                }
            }
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Invalid spawn option: " + ex.getMessage(), NamedTextColor.RED));
            return null;
        }
    }

    private static String nextString(CommandSender sender, List<String> args, int index, String flag) {
        if (index >= args.size() || args.get(index).startsWith("--")) {
            sender.sendMessage(Component.text(flag + " requires a value.", NamedTextColor.RED));
            return null;
        }
        return args.get(index);
    }

    private static Integer nextInt(CommandSender sender, List<String> args, int index, String flag) {
        String value = nextString(sender, args, index, flag);
        return value == null ? null : parseInt(sender, value, flag);
    }

    private static Vector3 nextVector(CommandSender sender, List<String> args, int start, String flag) {
        if (start + 2 >= args.size()) {
            sender.sendMessage(Component.text(flag + " requires x y z.", NamedTextColor.RED));
            return null;
        }
        Double x = parseDouble(sender, args.get(start), flag + " x");
        Double y = parseDouble(sender, args.get(start + 1), flag + " y");
        Double z = parseDouble(sender, args.get(start + 2), flag + " z");
        if (x == null || y == null || z == null) {
            return null;
        }
        return new Vector3(x, y, z);
    }
}
