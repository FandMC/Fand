package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.world.Vector3;
import org.junit.jupiter.api.Test;

final class DisplayTransformationTest {

    @Test
    void identityMatchesVanillaDisplayDefaults() {
        assertThat(DisplayTransformation.IDENTITY.translation()).isEqualTo(Vector3.ZERO);
        assertThat(DisplayTransformation.IDENTITY.leftRotation()).isEqualTo(Quaternion.IDENTITY);
        assertThat(DisplayTransformation.IDENTITY.scale()).isEqualTo(new Vector3(1.0, 1.0, 1.0));
        assertThat(DisplayTransformation.IDENTITY.rightRotation()).isEqualTo(Quaternion.IDENTITY);
    }

    @Test
    void withersReturnUpdatedCopies() {
        var moved = DisplayTransformation.IDENTITY.withTranslation(new Vector3(1.0, 2.0, 3.0));
        var scaled = moved.withScale(new Vector3(2.0, 2.0, 2.0));
        var rotated = scaled.withLeftRotation(new Quaternion(0.0F, 0.707F, 0.0F, 0.707F));

        assertThat(moved.translation()).isEqualTo(new Vector3(1.0, 2.0, 3.0));
        assertThat(scaled.scale()).isEqualTo(new Vector3(2.0, 2.0, 2.0));
        assertThat(rotated.leftRotation()).isEqualTo(new Quaternion(0.0F, 0.707F, 0.0F, 0.707F));
        assertThat(DisplayTransformation.IDENTITY.translation()).isEqualTo(Vector3.ZERO);
    }

    @Test
    void rejectsNonFiniteValues() {
        assertThatThrownBy(() -> new Quaternion(Float.NaN, 0.0F, 0.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> DisplayTransformation.IDENTITY.withScale(new Vector3(Double.POSITIVE_INFINITY, 1.0, 1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }
}
