package io.fand.server.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConsoleLanguage {
    public static final String DEFAULT_LOCALE = Language.DEFAULT;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLanguage.class);
    private static final Pattern LOCALE = Pattern.compile("[a-z0-9_]+");
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private static final ConsoleLanguage DEFAULT = load(DEFAULT_LOCALE);
    private static final Map<String, ConsoleLanguage> CACHE = new ConcurrentHashMap<>();
    private static volatile ConsoleLanguage current = DEFAULT;

    static {
        CACHE.put(DEFAULT_LOCALE, DEFAULT);
    }

    private final String locale;
    private final Map<String, String> translations;

    private ConsoleLanguage(String locale, Map<String, String> translations) {
        this.locale = locale;
        this.translations = translations;
    }

    public static void configure(@Nullable String requestedLocale) {
        var normalized = normalize(requestedLocale);
        current = CACHE.computeIfAbsent(normalized, ConsoleLanguage::load);
    }

    public static ConsoleLanguage current() {
        return current;
    }

    public String locale() {
        return locale;
    }

    FormattedText format(TranslatableContents contents) {
        var fallback = contents.getFallback();
        var template = translations.getOrDefault(
                contents.getKey(),
                fallback == null ? contents.getKey() : fallback
        );
        return FormattedText.composite(parts(template, contents.getArgs()));
    }

    static String normalize(@Nullable String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }

        var normalized = locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (!LOCALE.matcher(normalized).matches()) {
            LOGGER.warn("Invalid console language '{}', falling back to {}", locale, DEFAULT_LOCALE);
            return DEFAULT_LOCALE;
        }
        return normalized;
    }

    private static ConsoleLanguage load(String locale) {
        var loaded = new ConcurrentHashMap<String, String>();
        loadResource(DEFAULT_LOCALE, loaded::put);
        if (!DEFAULT_LOCALE.equals(locale) && !loadResource(locale, loaded::put)) {
            LOGGER.warn("Console language '{}' was not found, falling back to {}", locale, DEFAULT_LOCALE);
            return DEFAULT;
        }
        return new ConsoleLanguage(locale, Map.copyOf(loaded));
    }

    private static boolean loadResource(String locale, BiConsumer<String, String> output) {
        var path = "/assets/minecraft/lang/" + locale + ".json";
        try (InputStream stream = ConsoleLanguage.class.getResourceAsStream(path)) {
            if (stream == null) {
                return false;
            }
            Language.loadFromJson(stream, output);
            return true;
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("Failed to load console language resource {}", path, failure);
            return false;
        }
    }

    private static List<FormattedText> parts(String template, Object[] args) {
        var parts = new ArrayList<FormattedText>();
        var matcher = FORMAT.matcher(template);
        var nextArgument = 0;
        var current = 0;

        while (matcher.find(current)) {
            if (matcher.start() > current) {
                parts.add(FormattedText.of(template.substring(current, matcher.start())));
            }

            var type = matcher.group(2);
            var token = template.substring(matcher.start(), matcher.end());
            if ("%".equals(type) && "%%".equals(token)) {
                parts.add(FormattedText.of("%"));
            } else if ("s".equals(type)) {
                var explicit = matcher.group(1);
                var index = explicit == null ? nextArgument++ : Integer.parseInt(explicit) - 1;
                parts.add(argument(index, args));
            } else {
                parts.add(FormattedText.of(token));
            }

            current = matcher.end();
        }

        if (current < template.length()) {
            parts.add(FormattedText.of(template.substring(current)));
        }
        return parts;
    }

    private static FormattedText argument(int index, Object[] args) {
        if (index < 0 || index >= args.length) {
            return FormattedText.of("");
        }

        var arg = args[index];
        if (arg instanceof Component component) {
            return component;
        }
        if (arg instanceof FormattedText formatted) {
            return formatted;
        }
        return FormattedText.of(String.valueOf(arg));
    }

    static void useForTesting(String locale, Map<String, String> translations) {
        current = new ConsoleLanguage(locale, Map.copyOf(translations));
    }

    static void resetForTesting() {
        current = DEFAULT;
    }
}
