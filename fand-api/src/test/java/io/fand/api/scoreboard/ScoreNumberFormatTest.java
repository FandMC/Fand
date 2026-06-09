package io.fand.api.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.junit.jupiter.api.Test;

final class ScoreNumberFormatTest {

    @Test
    void fixedAndStyledFormatsUseNullableStorageWithOptionalAccessors() {
        var fixed = ScoreNumberFormat.fixed(Component.text("fixed"));

        assertThat(fixed.kind()).isEqualTo(ScoreNumberFormat.Kind.FIXED);
        assertThat(fixed.fixedValue()).contains(Component.text("fixed"));
        assertThat(fixed.style()).isEmpty();

        var style = Style.style(NamedTextColor.RED);
        var styled = ScoreNumberFormat.styled(style);

        assertThat(styled.kind()).isEqualTo(ScoreNumberFormat.Kind.STYLED);
        assertThat(styled.fixedValue()).isEmpty();
        assertThat(styled.style()).contains(style);
    }

    @Test
    void defaultAndBlankFormatsDoNotCarryOptionalPayloads() {
        assertThat(ScoreNumberFormat.DEFAULT.fixedValue()).isEmpty();
        assertThat(ScoreNumberFormat.DEFAULT.style()).isEmpty();
        assertThat(ScoreNumberFormat.BLANK.fixedValue()).isEmpty();
        assertThat(ScoreNumberFormat.BLANK.style()).isEmpty();
    }

    @Test
    void rejectsPayloadsThatDoNotMatchKind() {
        assertThatThrownBy(() -> new ScoreNumberFormat(ScoreNumberFormat.Kind.FIXED, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixedValue is required");
        assertThatThrownBy(() -> new ScoreNumberFormat(ScoreNumberFormat.Kind.STYLED, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("style is required");
        assertThatThrownBy(() -> new ScoreNumberFormat(
                ScoreNumberFormat.Kind.DEFAULT,
                Component.text("unused"),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixedValue is only valid");
    }
}
