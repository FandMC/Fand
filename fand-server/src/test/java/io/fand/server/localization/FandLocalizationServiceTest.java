package io.fand.server.localization;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.placeholder.PlaceholderService;
import io.fand.api.text.MiniMessageService;
import io.fand.server.placeholder.FandPlaceholderService;
import io.fand.server.text.FandMiniMessageService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandLocalizationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesLocaleFallbackAndVariables() throws Exception {
        write("messages/en_us.yml", """
                hello: "Hello {name}"
                nested:
                  value: "Fallback"
                """);
        write("messages/zh_cn.yml", """
                hello: "你好 {name}"
                """);
        var service = service();

        assertThat(service.message(Locale.forLanguageTag("zh-CN"), "hello", Map.of("name", "20018")))
                .isEqualTo("你好 20018");
        assertThat(service.message(Locale.forLanguageTag("zh-CN"), "nested.value"))
                .isEqualTo("Fallback");
        assertThat(service.message(Locale.forLanguageTag("fr-FR"), "missing.key"))
                .isEqualTo("missing.key");
    }

    @Test
    void rendersPlaceholdersAndMiniMessage() throws Exception {
        write("messages/en_us.yml", """
                welcome: "<green>Welcome %demo_name%</green>"
                """);
        var placeholders = new FandPlaceholderService();
        placeholders.register("demo", (context, identifier) -> "demo_name".equals(identifier) ? "Steve" : null);
        MiniMessageService miniMessages = new FandMiniMessageService(placeholders);
        var service = new FandLocalizationService(tempDir, Locale.forLanguageTag("en-US"), placeholders, miniMessages, null);

        assertThat(GsonComponentSerializer.gson().serialize(
                service.component(Locale.forLanguageTag("en-US"), io.fand.api.placeholder.PlaceholderContext.viewer(null), "welcome", Map.of())))
                .contains("Welcome Steve")
                .contains("\"color\":\"green\"");
    }

    private FandLocalizationService service() {
        PlaceholderService placeholders = PlaceholderService.empty();
        MiniMessageService miniMessages = MiniMessageService.empty();
        return new FandLocalizationService(tempDir, Locale.forLanguageTag("en-US"), placeholders, miniMessages, null);
    }

    private void write(String relative, String content) throws Exception {
        var file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
