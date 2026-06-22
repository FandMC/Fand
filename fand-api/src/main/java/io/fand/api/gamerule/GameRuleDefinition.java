package io.fand.api.gamerule;

import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Definition for a Fand custom game rule. */
public record GameRuleDefinition(
        Key key,
        GameRuleType type,
        String defaultValue,
        String description
) {

    public GameRuleDefinition(Key key, GameRuleType type, String defaultValue) {
        this(key, type, defaultValue, "");
    }

    public GameRuleDefinition {
        key = Objects.requireNonNull(key, "key");
        type = Objects.requireNonNull(type, "type");
        defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        description = description == null ? "" : description;
        validate(type, defaultValue);
    }

    public boolean validValue(String value) {
        try {
            validate(type, value);
            return true;
        } catch (IllegalArgumentException | NullPointerException ex) {
            return false;
        }
    }

    private static void validate(GameRuleType type, String value) {
        Objects.requireNonNull(value, "value");
        switch (type) {
            case BOOLEAN -> {
                if (!value.equals("true") && !value.equals("false")) {
                    throw new IllegalArgumentException("Boolean game rule values must be 'true' or 'false'");
                }
            }
            case INTEGER -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Integer game rule values must be valid integers", ex);
                }
            }
            case STRING -> {
            }
        }
    }
}
