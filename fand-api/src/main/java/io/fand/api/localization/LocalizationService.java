package io.fand.api.localization;

import io.fand.api.entity.Player;
import io.fand.api.placeholder.PlaceholderContext;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Plugin message catalog loader and renderer.
 *
 * <p>Language files use dot-separated message keys in YAML files named like
 * {@code zh_cn.yml} or {@code en_us.yml}. Implementations choose the nearest
 * available locale and then fall back to the configured fallback locale.
 */
public interface LocalizationService {

    Locale DEFAULT_LOCALE = Locale.forLanguageTag("en-US");

    MessageBundle bundle();

    void reload();

    Optional<String> raw(Locale locale, String key);

    default Optional<String> raw(String locale, String key) {
        return raw(locale(locale), key);
    }

    String message(Locale locale, String key, Map<String, ?> variables);

    default String message(Locale locale, String key) {
        return message(locale, key, Map.of());
    }

    default String message(String locale, String key, Map<String, ?> variables) {
        return message(locale(locale), key, variables);
    }

    default String message(String locale, String key) {
        return message(locale(locale), key);
    }

    Component component(@Nullable Player viewer, String key, Map<String, ?> variables);

    default Component component(@Nullable Player viewer, String key) {
        return component(viewer, key, Map.of());
    }

    Component component(Locale locale, PlaceholderContext context, String key, Map<String, ?> variables);

    default Component component(Locale locale, PlaceholderContext context, String key) {
        return component(locale, context, key, Map.of());
    }

    static Locale locale(String locale) {
        java.util.Objects.requireNonNull(locale, "locale");
        var normalized = locale.trim().replace('_', '-');
        if (normalized.isEmpty()) {
            return DEFAULT_LOCALE;
        }
        return Locale.forLanguageTag(normalized);
    }

    static String localeId(Locale locale) {
        java.util.Objects.requireNonNull(locale, "locale");
        var tag = locale.toLanguageTag();
        return "und".equals(tag) ? "" : tag.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    static LocalizationService empty() {
        return new LocalizationService() {
            private final MessageBundle bundle = new MessageBundle() {
                @Override
                public Locale fallbackLocale() {
                    return DEFAULT_LOCALE;
                }

                @Override
                public java.util.Collection<Locale> locales() {
                    return java.util.List.of();
                }

                @Override
                public Optional<String> raw(Locale locale, String key) {
                    return Optional.empty();
                }
            };

            @Override
            public MessageBundle bundle() {
                return bundle;
            }

            @Override
            public void reload() {
            }

            @Override
            public Optional<String> raw(Locale locale, String key) {
                return Optional.empty();
            }

            @Override
            public String message(Locale locale, String key, Map<String, ?> variables) {
                return key;
            }

            @Override
            public Component component(@Nullable Player viewer, String key, Map<String, ?> variables) {
                return Component.text(key);
            }

            @Override
            public Component component(Locale locale, PlaceholderContext context, String key, Map<String, ?> variables) {
                return Component.text(key);
            }
        };
    }
}
