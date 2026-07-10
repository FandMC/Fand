package io.fand.api.localization;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * Loaded message catalogs for one plugin or service namespace.
 */
public interface MessageBundle {

    Locale fallbackLocale();

    Collection<Locale> locales();

    Optional<String> raw(Locale locale, String key);

    default Optional<String> raw(String locale, String key) {
        return raw(LocalizationService.locale(locale), key);
    }
}
