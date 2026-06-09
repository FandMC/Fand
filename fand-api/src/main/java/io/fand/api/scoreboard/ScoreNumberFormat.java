package io.fand.api.scoreboard;

import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

/**
 * Optional vanilla number-format override for a score or objective.
 */
public record ScoreNumberFormat(Kind kind, Optional<Component> fixedValue, Optional<Style> style) {

    public static final ScoreNumberFormat DEFAULT = new ScoreNumberFormat(Kind.DEFAULT, Optional.empty(), Optional.empty());
    public static final ScoreNumberFormat BLANK = new ScoreNumberFormat(Kind.BLANK, Optional.empty(), Optional.empty());

    public ScoreNumberFormat {
        Objects.requireNonNull(kind, "kind");
        fixedValue = Objects.requireNonNull(fixedValue, "fixedValue");
        style = Objects.requireNonNull(style, "style");
        if (kind != Kind.FIXED && fixedValue.isPresent()) {
            throw new IllegalArgumentException("fixedValue is only valid for FIXED number formats");
        }
        if (kind != Kind.STYLED && style.isPresent()) {
            throw new IllegalArgumentException("style is only valid for STYLED number formats");
        }
    }

    public static ScoreNumberFormat fixed(Component value) {
        return new ScoreNumberFormat(Kind.FIXED, Optional.of(Objects.requireNonNull(value, "value")), Optional.empty());
    }

    public static ScoreNumberFormat styled(Style style) {
        return new ScoreNumberFormat(Kind.STYLED, Optional.empty(), Optional.of(Objects.requireNonNull(style, "style")));
    }

    public enum Kind {
        DEFAULT,
        BLANK,
        FIXED,
        STYLED
    }
}
