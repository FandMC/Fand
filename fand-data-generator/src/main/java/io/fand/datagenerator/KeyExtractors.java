package io.fand.datagenerator;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KeyExtractors {

    private static final Pattern STRING_REGISTER_PATTERN = Pattern.compile(
            "\\bregister\\w*\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern REGISTER_HOLDER_PATTERN = Pattern.compile(
            "\\bregisterForHolder\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern KEY_REFERENCE_PATTERN = Pattern.compile(
            "\\b([A-Z][A-Za-z0-9_]*(?:Ids|Tags))\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern CREATE_KEY_PATTERN = Pattern.compile(
            "\\b(?:createKey|create|register)\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern BLOCK_ITEM_ID_PATTERN = Pattern.compile(
            "\\bBlockItemId\\.create\\s*\\(\\s*\"([^\"]+)\"(?:\\s*,\\s*\"([^\"]+)\")?");
    private static final Pattern BLOCK_ITEM_TAG_ID_PATTERN = Pattern.compile(
            "\\bBlockItemTagId\\.create\\s*\\(\\s*\"([^\"]+)\"(?:\\s*,\\s*\"([^\"]+)\")?");
    private static final Pattern STRING_KEY_ARGUMENT_PATTERN = Pattern.compile(
            "\\b(?:(?:register\\w*|createKey|createId|create|registryKey|key|bind)\\s*\\(\\s*"
                    + "|(?:Registry|SurfaceRules)\\.register\\s*\\(\\s*[^,]+\\s*,\\s*)\"([^\"]+)\"");
    private static final Pattern CUSTOM_STAT_PATTERN = Pattern.compile(
            "\\bmakeCustomStat\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern DEFAULT_NAMESPACE_IDENTIFIER_PATTERN = Pattern.compile(
            "\\bIdentifier\\.withDefaultNamespace\\s*\\(\\s*\"([^\"]+)\"");
    private static final Pattern TEMPERATURE_VARIANT_PATTERN = Pattern.compile(
            "\\bTemperatureVariants\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern SOUND_SET_REFERENCE_PATTERN = Pattern.compile(
            "\\bSoundSet\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern BLOCK_REFERENCE_PATTERN = Pattern.compile(
            "\\bBlocks\\.([A-Z][A-Z0-9_]*)\\b");
    private static final Pattern SOUND_SET_PATTERN = Pattern.compile(
            "([A-Z][A-Z0-9_]*)\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Map<String, String> TEMPERATURE_VARIANTS = Map.of(
            "TEMPERATE", "temperate",
            "WARM", "warm",
            "COLD", "cold");

    private KeyExtractors() {
    }

    static Optional<String> firstStringRegisterId(String initializer) {
        var matcher = STRING_REGISTER_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Optional<String> firstSoundRegisterId(String initializer) {
        var holder = REGISTER_HOLDER_PATTERN.matcher(initializer);
        if (holder.find()) {
            return Optional.of(holder.group(1));
        }
        return firstStringRegisterId(initializer);
    }

    static Optional<String> firstCreateKey(String initializer) {
        var matcher = CREATE_KEY_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Optional<String> firstBlockItemBlockId(String initializer) {
        var matcher = BLOCK_ITEM_ID_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Optional<String> firstBlockItemItemId(String initializer) {
        var matcher = BLOCK_ITEM_ID_PATTERN.matcher(initializer);
        if (!matcher.find()) {
            return Optional.empty();
        }
        var itemId = matcher.group(2);
        return Optional.of(itemId == null ? matcher.group(1) : itemId);
    }

    static Optional<String> firstBlockItemTagBlockId(String initializer) {
        var matcher = BLOCK_ITEM_TAG_ID_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Optional<String> firstBlockItemTagItemId(String initializer) {
        var matcher = BLOCK_ITEM_TAG_ID_PATTERN.matcher(initializer);
        if (!matcher.find()) {
            return Optional.empty();
        }
        var itemId = matcher.group(2);
        return Optional.of(itemId == null ? matcher.group(1) : itemId);
    }

    static Optional<String> firstRegistryKeyId(String initializer) {
        var stringKey = STRING_KEY_ARGUMENT_PATTERN.matcher(initializer);
        if (stringKey.find()) {
            return Optional.of(stringKey.group(1));
        }

        var identifier = DEFAULT_NAMESPACE_IDENTIFIER_PATTERN.matcher(initializer);
        if (identifier.find()) {
            return Optional.of(identifier.group(1));
        }

        var temperatureVariant = TEMPERATURE_VARIANT_PATTERN.matcher(initializer);
        if (temperatureVariant.find()) {
            return Optional.ofNullable(TEMPERATURE_VARIANTS.get(temperatureVariant.group(1)));
        }

        return Optional.empty();
    }

    static Optional<String> firstCustomStatId(String initializer) {
        var matcher = CUSTOM_STAT_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Optional<String> firstSoundSetKey(String initializer) {
        var matcher = SOUND_SET_REFERENCE_PATTERN.matcher(initializer);
        return matcher.find()
                ? Optional.of(KeyNames.enumNameToPath(matcher.group(1)))
                : Optional.empty();
    }

    static Optional<String> firstReferencedKey(String initializer, Map<String, String> keys) {
        var matcher = KEY_REFERENCE_PATTERN.matcher(initializer);
        while (matcher.find()) {
            var fieldName = matcher.group(2);
            var key = keys.get(matcher.group(1) + "." + fieldName);
            if (key == null) {
                key = keys.get(fieldName);
            }
            if (key != null) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    static Optional<String> referencedBlockName(String initializer) {
        var matcher = BLOCK_REFERENCE_PATTERN.matcher(initializer);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static Matcher soundSetMatcher(String source) {
        return SOUND_SET_PATTERN.matcher(source);
    }

    static Matcher stringKeyMatcher(String source) {
        return STRING_KEY_ARGUMENT_PATTERN.matcher(source);
    }
}
