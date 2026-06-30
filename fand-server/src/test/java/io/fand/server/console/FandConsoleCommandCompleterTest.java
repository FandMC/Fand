package io.fand.server.console;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import io.fand.server.command.CommandBridge;
import java.util.List;
import org.junit.jupiter.api.Test;

final class FandConsoleCommandCompleterTest {

    @Test
    void preservesSlashPrefixForMinecraftSuggestions() {
        var normalized = FandConsoleCommandCompleter.normalize("/pl");
        var suggestion = new Suggestion(StringRange.between(0, 2), "plugins");

        assertThat(FandConsoleCommandCompleter.toMinecraftCandidate("/pl", normalized, suggestion))
                .isEqualTo("/plugins");
    }

    @Test
    void omitsSlashPrefixWhenUserDidNotTypeSlash() {
        var normalized = FandConsoleCommandCompleter.normalize("pl");
        var suggestion = new Suggestion(StringRange.between(0, 2), "plugins");

        assertThat(FandConsoleCommandCompleter.toMinecraftCandidate("pl", normalized, suggestion))
                .isEqualTo("plugins");
    }

    @Test
    void replacesOnlySuggestedRange() {
        var normalized = FandConsoleCommandCompleter.normalize("plugin r tail");
        var suggestion = new Suggestion(StringRange.between(7, 8), "reload");

        assertThat(FandConsoleCommandCompleter.toMinecraftCandidate("plugin r tail", normalized, suggestion))
                .isEqualTo("plugin reload tail");
    }

    @Test
    void appliesFandSuggestionRange() {
        var result = new CommandBridge.SuggestionResult(List.of("reload"), 7, 8);

        assertThat(FandConsoleCommandCompleter.toFandCandidate("plugin r tail", result, "reload"))
                .isEqualTo("plugin reload tail");
    }

    @Test
    void hidesNamespacedRootSuggestionsWhenLocalRootsMatch() {
        assertThat(FandConsoleCommandCompleter.rootLocalFirst("fand", List.of(
                "fand-test-plugin:fanddemo",
                "fanddemo",
                "fand:fand",
                "fandtitle"
        ))).containsExactly("fanddemo", "fandtitle");
    }

    @Test
    void keepsNamespacedRootSuggestionsAfterNamespaceSeparator() {
        assertThat(FandConsoleCommandCompleter.rootLocalFirst("fand:", List.of(
                "fand:fand",
                "fand:mspt"
        ))).containsExactly("fand:fand", "fand:mspt");
    }

    @Test
    void keepsNamespacedRootSuggestionsWhenNoLocalRootMatches() {
        assertThat(FandConsoleCommandCompleter.rootLocalFirst("fand", List.of(
                "fand:fand",
                "fand:mspt"
        ))).containsExactly("fand:fand", "fand:mspt");
    }

    @Test
    void keepsNamespacedArgumentSuggestionsOutsideRootToken() {
        assertThat(FandConsoleCommandCompleter.rootLocalFirst("give ", List.of(
                "give minecraft:stone",
                "give minecraft:dirt"
        ))).containsExactly("give minecraft:stone", "give minecraft:dirt");
    }
}
