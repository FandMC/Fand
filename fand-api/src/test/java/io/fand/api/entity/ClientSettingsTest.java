package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ClientSettingsTest {

    @Test
    void exposesLocaleAndViewDistance() {
        var settings = new ClientSettings("en_us", 12);

        assertThat(settings.locale()).isEqualTo("en_us");
        assertThat(settings.viewDistance()).isEqualTo(12);
    }

    @Test
    void rejectsNullLocale() {
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(null, 8));
    }
}
