package io.fand.api.scoreboard;

import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jspecify.annotations.Nullable;

/**
 * Optional vanilla number-format override for a score or objective.
 */
public final class ScoreNumberFormat {

    public static final ScoreNumberFormat DEFAULT = new ScoreNumberFormat(Kind.DEFAULT, null, null);
    public static final ScoreNumberFormat BLANK = new ScoreNumberFormat(Kind.BLANK, null, null);

    private final Kind kind;
    private final @Nullable Component fixedValue;
    private final @Nullable Style style;

    public ScoreNumberFormat(Kind kind, @Nullable Component fixedValue, @Nullable Style style) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (kind != Kind.FIXED && fixedValue != null) {
            throw new IllegalArgumentException("fixedValue is only valid for FIXED number formats");
        }
        if (kind == Kind.FIXED && fixedValue == null) {
            throw new IllegalArgumentException("fixedValue is required for FIXED number formats");
        }
        if (kind != Kind.STYLED && style != null) {
            throw new IllegalArgumentException("style is only valid for STYLED number formats");
        }
        if (kind == Kind.STYLED && style == null) {
            throw new IllegalArgumentException("style is required for STYLED number formats");
        }
        this.fixedValue = fixedValue;
        this.style = style;
    }

    public Kind kind() {
        return kind;
    }

    public Optional<Component> fixedValue() {
        return Optional.ofNullable(fixedValue);
    }

    public Optional<Style> style() {
        return Optional.ofNullable(style);
    }

    public static ScoreNumberFormat fixed(Component value) {
        return new ScoreNumberFormat(Kind.FIXED, Objects.requireNonNull(value, "value"), null);
    }

    public static ScoreNumberFormat styled(Style style) {
        return new ScoreNumberFormat(Kind.STYLED, null, Objects.requireNonNull(style, "style"));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ScoreNumberFormat that
                && kind == that.kind
                && Objects.equals(fixedValue, that.fixedValue)
                && Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, fixedValue, style);
    }

    @Override
    public String toString() {
        return "ScoreNumberFormat[kind=" + kind + ", fixedValue=" + fixedValue + ", style=" + style + "]";
    }

    public enum Kind {
        DEFAULT,
        BLANK,
        FIXED,
        STYLED
    }
}
