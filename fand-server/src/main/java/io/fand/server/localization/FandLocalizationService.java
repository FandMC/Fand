package io.fand.server.localization;

import io.fand.api.config.ConfigurationSection;
import io.fand.api.entity.Player;
import io.fand.api.localization.LocalizationService;
import io.fand.api.localization.MessageBundle;
import io.fand.api.placeholder.PlaceholderContext;
import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.text.MiniMessageService;
import io.fand.server.config.YamlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public final class FandLocalizationService implements LocalizationService {

    private static final String MESSAGES_DIRECTORY = "messages";

    private final Path directory;
    private final Locale fallbackLocale;
    private final PlaceholderService placeholders;
    private final MiniMessageService miniMessages;
    private final @Nullable ClassLoader defaults;
    private final AtomicReference<LoadedBundle> bundle = new AtomicReference<>();

    public FandLocalizationService(
            Path dataDirectory,
            Locale fallbackLocale,
            PlaceholderService placeholders,
            MiniMessageService miniMessages,
            @Nullable ClassLoader defaults
    ) {
        this.directory = Objects.requireNonNull(dataDirectory, "dataDirectory").resolve(MESSAGES_DIRECTORY).toAbsolutePath().normalize();
        this.fallbackLocale = Objects.requireNonNull(fallbackLocale, "fallbackLocale");
        this.placeholders = Objects.requireNonNull(placeholders, "placeholders");
        this.miniMessages = Objects.requireNonNull(miniMessages, "miniMessages");
        this.defaults = defaults;
    }

    @Override
    public MessageBundle bundle() {
        var existing = bundle.get();
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = bundle.get();
            if (existing == null) {
                existing = load();
                bundle.set(existing);
            }
            return existing;
        }
    }

    @Override
    public void reload() {
        synchronized (this) {
            bundle.set(load());
        }
    }

    @Override
    public Optional<String> raw(Locale locale, String key) {
        return bundle().raw(locale, key);
    }

    @Override
    public String message(Locale locale, String key, Map<String, ?> variables) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(variables, "variables");
        var template = raw(locale, key).orElse(key);
        return applyVariables(template, variables);
    }

    @Override
    public Component component(@Nullable Player viewer, String key, Map<String, ?> variables) {
        var locale = viewer == null ? fallbackLocale : LocalizationService.locale(viewer.clientSettings().locale());
        return component(locale, PlaceholderContext.viewer(viewer), key, variables);
    }

    @Override
    public Component component(Locale locale, PlaceholderContext context, String key, Map<String, ?> variables) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(context, "context");
        var text = placeholders.replace(message(locale, key, variables), context);
        return miniMessages.parser().deserialize(text);
    }

    private LoadedBundle load() {
        materialiseDefault(fallbackLocale);
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create messages directory " + directory, failure);
        }
        var messages = new LinkedHashMap<Locale, Map<String, String>>();
        try (var stream = Files.list(directory)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .forEach(path -> messages.put(localeFromFile(path), flatten(YamlConfiguration.load(path))));
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to list messages in " + directory, failure);
        }
        return new LoadedBundle(fallbackLocale, messages);
    }

    private void materialiseDefault(Locale locale) {
        if (defaults == null) {
            return;
        }
        var name = LocalizationService.localeId(locale);
        if (name.isBlank()) {
            return;
        }
        var target = directory.resolve(name + ".yml").normalize();
        if (Files.exists(target)) {
            return;
        }
        var resource = MESSAGES_DIRECTORY + "/" + name + ".yml";
        try (InputStream input = defaults.getResourceAsStream(resource)) {
            if (input == null) {
                return;
            }
            Files.createDirectories(directory);
            Files.copy(input, target);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to copy default messages " + resource, failure);
        }
    }

    private static Locale localeFromFile(Path path) {
        var name = path.getFileName().toString();
        return LocalizationService.locale(name.substring(0, name.length() - ".yml".length()));
    }

    private static Map<String, String> flatten(ConfigurationSection section) {
        var output = new LinkedHashMap<String, String>();
        flatten(section, "", output);
        return Map.copyOf(output);
    }

    private static void flatten(ConfigurationSection section, String prefix, Map<String, String> output) {
        for (var key : section.keys()) {
            var path = prefix.isEmpty() ? key : prefix + "." + key;
            var value = section.value(key);
            if (value instanceof ConfigurationSection child) {
                flatten(child, path, output);
            } else if (value != null) {
                output.put(path, value.toString());
            }
        }
    }

    private static String applyVariables(String input, Map<String, ?> variables) {
        var output = input;
        for (var entry : variables.entrySet()) {
            var key = Objects.requireNonNull(entry.getKey(), "variable key");
            var value = entry.getValue() == null ? "" : entry.getValue().toString();
            output = output.replace("{" + key + "}", value);
        }
        return output;
    }

    private record LoadedBundle(Locale fallbackLocale, Map<Locale, Map<String, String>> messages) implements MessageBundle {

        private LoadedBundle {
            fallbackLocale = Objects.requireNonNull(fallbackLocale, "fallbackLocale");
            messages = Map.copyOf(messages);
        }

        @Override
        public java.util.Collection<Locale> locales() {
            return messages.keySet();
        }

        @Override
        public Optional<String> raw(Locale locale, String key) {
            Objects.requireNonNull(locale, "locale");
            Objects.requireNonNull(key, "key");
            return find(locale, key)
                    .or(() -> find(languageOnly(locale), key))
                    .or(() -> find(fallbackLocale, key))
                    .or(() -> find(languageOnly(fallbackLocale), key));
        }

        private Optional<String> find(Locale locale, String key) {
            var values = messages.get(locale);
            return values == null ? Optional.empty() : Optional.ofNullable(values.get(key));
        }

        private static Locale languageOnly(Locale locale) {
            return locale.getLanguage().isBlank() ? locale : Locale.forLanguageTag(locale.getLanguage());
        }
    }
}
