package io.fand.datagenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class DamageEventMetadataWriter {

    private static final String PACKAGE_NAME = "io.fand.api.event.entity";
    private static final List<String> PROJECTILE_KEYS = List.of(
            "minecraft:arrow",
            "minecraft:trident",
            "minecraft:mob_projectile",
            "minecraft:spit",
            "minecraft:wind_charge",
            "minecraft:fireworks",
            "minecraft:fireball",
            "minecraft:unattributed_fireball",
            "minecraft:wither_skull",
            "minecraft:thrown");
    private static final List<String> EXPLOSION_KEYS = List.of(
            "minecraft:explosion",
            "minecraft:player_explosion",
            "minecraft:bad_respawn_point");
    private static final List<String> FALL_KEYS = List.of(
            "minecraft:fall",
            "minecraft:ender_pearl",
            "minecraft:fly_into_wall",
            "minecraft:stalagmite");
    private static final List<String> FIRE_KEYS = List.of(
            "minecraft:in_fire",
            "minecraft:campfire",
            "minecraft:lightning_bolt",
            "minecraft:on_fire",
            "minecraft:hot_floor",
            "minecraft:sulfur_cube_hot");
    private static final List<String> MAGIC_KEYS = List.of(
            "minecraft:magic",
            "minecraft:wither",
            "minecraft:dragon_breath",
            "minecraft:indirect_magic");

    private final Path outputSources;
    private final Path existingSources;

    DamageEventMetadataWriter(Path outputSources, Path existingSources) {
        this.outputSources = outputSources;
        this.existingSources = existingSources;
    }

    void write(List<KeyEntry> damageTypes) throws IOException {
        var availableKeys = damageTypes.stream()
                .map(KeyEntry::key)
                .collect(Collectors.toUnmodifiableSet());
        var specs = DamageEventSpec.all().stream()
                .filter(spec -> spec.causeKey() == null || availableKeys.contains(spec.causeKey()))
                .toList();

        for (var spec : specs) {
            if (!sourceExists(spec.className())) {
                writeSource(spec.className(), eventSource(spec));
            }
        }
        writeSource("DamageEventRegistry", registrySource(specs, availableKeys));
    }

    private boolean sourceExists(String typeName) {
        if (existingSources == null) {
            return false;
        }
        var packagePath = Path.of(PACKAGE_NAME.replace('.', '/'));
        return Files.isRegularFile(existingSources.resolve(packagePath).resolve(typeName + ".java"));
    }

    private void writeSource(String typeName, String source) throws IOException {
        var packagePath = Path.of(PACKAGE_NAME.replace('.', '/'));
        var outputFile = outputSources.resolve(packagePath).resolve(typeName + ".java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, source, StandardCharsets.UTF_8);
    }

    private static String eventSource(DamageEventSpec spec) {
        var source = new StringBuilder();
        source.append("package ").append(PACKAGE_NAME).append(";\n\n");
        if (spec.payload() == DamageEventSpec.Payload.BLOCK) {
            source.append("import io.fand.api.block.Block;\n");
        }
        source.append("import io.fand.api.entity.LivingEntity;\n");
        if (spec.payload() == DamageEventSpec.Payload.PLAYER) {
            source.append("import io.fand.api.entity.Player;\n");
        }
        source.append("import java.util.Map;\n");
        if (spec.payload() != DamageEventSpec.Payload.BLOCK) {
            source.append("import org.jspecify.annotations.Nullable;\n");
        }
        source.append("\n");
        source.append("/**\n");
        source.append(" * Fired before ").append(spec.description()).append(" is applied to a living entity.\n");
        source.append(" */\n");
        source.append("public ");
        if (spec.routeExact()) {
            source.append("final ");
        }
        source.append("class ").append(spec.className()).append(" extends ").append(spec.parentClass()).append(" {\n\n");
        appendConstructor(source, spec);
        if (spec.payload() == DamageEventSpec.Payload.PLAYER) {
            source.append("\n");
            source.append("    @Override\n");
            source.append("    public Player damager() {\n");
            source.append("        return (Player) super.damager();\n");
            source.append("    }\n");
        }
        source.append("}\n");
        return source.toString();
    }

    private static void appendConstructor(StringBuilder source, DamageEventSpec spec) {
        source.append("    public ").append(spec.className()).append("(\n");
        source.append("            LivingEntity entity,\n");
        if (spec.acceptsCause()) {
            source.append("            DamageCause cause,\n");
        }
        source.append("            double amount,\n");
        source.append("            Map<DamageModifier, Double> modifiers");
        switch (spec.payload()) {
            case GENERIC -> {
                source.append(",\n");
                source.append("            @Nullable LivingEntity directEntity,\n");
                source.append("            @Nullable LivingEntity attacker) {\n");
                source.append("        super(entity, ");
                appendCauseArgument(source, spec);
                source.append(", amount, modifiers, directEntity, attacker);\n");
            }
            case ENTITY -> {
                source.append(",\n");
                source.append("            LivingEntity damager,\n");
                source.append("            @Nullable LivingEntity directEntity) {\n");
                source.append("        super(entity, ");
                appendCauseArgument(source, spec);
                if (spec.parentClass().equals("EntityDamageByEntityEvent")) {
                    source.append(".asString()");
                }
                source.append(", amount, modifiers, damager, directEntity);\n");
            }
            case PLAYER -> {
                source.append(",\n");
                source.append("            Player damager,\n");
                source.append("            @Nullable LivingEntity directEntity) {\n");
                source.append("        super(entity, ");
                appendCauseArgument(source, spec);
                source.append(".asString(), amount, modifiers, damager, directEntity);\n");
            }
            case BLOCK -> {
                source.append(",\n");
                source.append("            Block damager) {\n");
                source.append("        super(entity, ");
                appendCauseArgument(source, spec);
                source.append(".asString(), amount, modifiers, damager);\n");
            }
        }
        source.append("    }\n");
    }

    private static void appendCauseArgument(StringBuilder source, DamageEventSpec spec) {
        if (spec.acceptsCause()) {
            source.append("cause");
        } else {
            source.append("DamageCause.of(\"").append(spec.causeKey()).append("\")");
        }
    }

    private static String registrySource(List<DamageEventSpec> specs, Set<String> availableKeys) {
        var source = new StringBuilder();
        source.append("package ").append(PACKAGE_NAME).append(";\n\n");
        source.append("import io.fand.api.block.Block;\n");
        source.append("import io.fand.api.entity.LivingEntity;\n");
        source.append("import io.fand.api.entity.Player;\n");
        source.append("import io.fand.api.event.Event;\n");
        source.append("import java.util.List;\n");
        source.append("import java.util.Map;\n");
        source.append("import org.jspecify.annotations.Nullable;\n\n");
        source.append("/** Generated damage event routing metadata. */\n");
        source.append("public final class DamageEventRegistry {\n\n");
        source.append("    private static final List<Class<? extends Event>> DETAILED_EVENT_TYPES = List.of(\n");
        for (int i = 0; i < specs.size(); i++) {
            source.append("            ").append(specs.get(i).className()).append(".class");
            source.append(i == specs.size() - 1 ? ");\n\n" : ",\n");
        }
        source.append("    private DamageEventRegistry() {\n");
        source.append("    }\n\n");
        source.append("    public static List<Class<? extends Event>> detailedEventTypes() {\n");
        source.append("        return DETAILED_EVENT_TYPES;\n");
        source.append("    }\n\n");
        appendCreateDetailed(source, specs);
        appendGroupHelpers(source, availableKeys);
        source.append("}\n");
        return source.toString();
    }

    private static void appendCreateDetailed(StringBuilder source, List<DamageEventSpec> specs) {
        source.append("    public static @Nullable EntityDamageEvent createDetailed(\n");
        source.append("            LivingEntity victim,\n");
        source.append("            DamageCause cause,\n");
        source.append("            double amount,\n");
        source.append("            Map<DamageModifier, Double> modifiers,\n");
        source.append("            @Nullable LivingEntity attacker,\n");
        source.append("            @Nullable LivingEntity directEntity,\n");
        source.append("            @Nullable Block blockSource) {\n");
        for (var spec : specs) {
            if (!spec.routeExact()) {
                continue;
            }
            appendExactRoute(source, spec);
        }
        appendGroupRoute(source, "minecraft:mob_attack", "minecraft:mob_attack_no_aggro", "attacker != null",
                "new EntityMobAttackDamageEvent(victim, cause, amount, modifiers, attacker, directEntity)");
        appendGroupRoute(source, "isProjectile(cause)", null, null,
                "new EntityProjectileDamageEvent(victim, cause, amount, modifiers, directEntity, attacker)");
        appendGroupRoute(source, "isExplosion(cause)", null, null,
                "new EntityExplosionDamageEvent(victim, cause, amount, modifiers, directEntity, attacker)");
        appendGroupRoute(source, "isFall(cause)", null, null,
                "new EntityFallDamageEvent(victim, cause, amount, modifiers, directEntity, attacker)");
        appendGroupRoute(source, "isFire(cause)", null, null,
                "new EntityFireDamageEvent(victim, cause, amount, modifiers, directEntity, attacker)");
        appendGroupRoute(source, "isMagic(cause)", null, null,
                "new EntityMagicDamageEvent(victim, cause, amount, modifiers, directEntity, attacker)");
        source.append("        return null;\n");
        source.append("    }\n\n");
    }

    private static void appendExactRoute(StringBuilder source, DamageEventSpec spec) {
        source.append("        if (matches(cause, \"").append(spec.causeKey()).append("\")");
        switch (spec.payload()) {
            case PLAYER -> source.append(" && attacker instanceof Player player");
            case ENTITY -> source.append(" && attacker != null");
            case BLOCK -> source.append(" && blockSource != null");
            case GENERIC -> {
            }
        }
        source.append(") {\n");
        source.append("            return ");
        switch (spec.payload()) {
            case GENERIC -> source.append("new ").append(spec.className())
                    .append("(victim, amount, modifiers, directEntity, attacker)");
            case ENTITY -> source.append("new ").append(spec.className())
                    .append("(victim, amount, modifiers, attacker, directEntity)");
            case PLAYER -> source.append("new ").append(spec.className())
                    .append("(victim, amount, modifiers, player, directEntity)");
            case BLOCK -> source.append("new ").append(spec.className())
                    .append("(victim, amount, modifiers, blockSource)");
        }
        source.append(";\n");
        source.append("        }\n");
    }

    private static void appendGroupRoute(StringBuilder source, String keyOrCondition, String secondKey, String extraCondition, String factory) {
        source.append("        if (");
        if (secondKey == null) {
            source.append(keyOrCondition);
        } else {
            source.append("(matches(cause, \"").append(keyOrCondition).append("\") || matches(cause, \"").append(secondKey).append("\"))");
        }
        if (extraCondition != null) {
            source.append(" && ").append(extraCondition);
        }
        source.append(") {\n");
        source.append("            return ").append(factory).append(";\n");
        source.append("        }\n");
    }

    private static void appendGroupHelpers(StringBuilder source, Set<String> availableKeys) {
        source.append("    private static boolean isProjectile(DamageCause cause) {\n");
        appendContains(source, PROJECTILE_KEYS, availableKeys);
        source.append("    }\n\n");
        source.append("    private static boolean isExplosion(DamageCause cause) {\n");
        appendContains(source, EXPLOSION_KEYS, availableKeys);
        source.append("    }\n\n");
        source.append("    private static boolean isFall(DamageCause cause) {\n");
        appendContains(source, FALL_KEYS, availableKeys);
        source.append("    }\n\n");
        source.append("    private static boolean isFire(DamageCause cause) {\n");
        appendContains(source, FIRE_KEYS, availableKeys);
        source.append("    }\n\n");
        source.append("    private static boolean isMagic(DamageCause cause) {\n");
        appendContains(source, MAGIC_KEYS, availableKeys);
        source.append("    }\n\n");
        source.append("    private static boolean matches(DamageCause cause, String key) {\n");
        source.append("        return cause.asString().equals(key);\n");
        source.append("    }\n");
    }

    private static void appendContains(StringBuilder source, List<String> keys, Set<String> availableKeys) {
        var filtered = keys.stream().filter(availableKeys::contains).toList();
        if (filtered.isEmpty()) {
            source.append("        return false;\n");
            return;
        }
        for (int i = 0; i < filtered.size(); i++) {
            source.append(i == 0 ? "        return " : "                || ");
            source.append("matches(cause, \"").append(filtered.get(i)).append("\")");
            source.append(i == filtered.size() - 1 ? ";\n" : "\n");
        }
    }
}
