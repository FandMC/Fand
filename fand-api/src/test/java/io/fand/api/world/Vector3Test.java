package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class Vector3Test {

    @Test
    void supportsBasicVectorOperations() {
        var vector = new Vector3(1.0, 2.0, 2.0);

        assertThat(vector.add(new Vector3(2.0, 0.5, -1.0))).isEqualTo(new Vector3(3.0, 2.5, 1.0));
        assertThat(vector.multiply(2.0)).isEqualTo(new Vector3(2.0, 4.0, 4.0));
        assertThat(vector.lengthSquared()).isEqualTo(9.0);
        assertThat(vector.length()).isEqualTo(3.0);
    }
}
