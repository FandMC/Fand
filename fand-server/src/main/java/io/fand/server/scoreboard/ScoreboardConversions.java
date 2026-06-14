package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreDisplaySlot;
import io.fand.api.scoreboard.ScoreNumberFormat;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.TeamCollisionRule;
import io.fand.api.scoreboard.TeamVisibility;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

final class ScoreboardConversions {

    private ScoreboardConversions() {
    }

    static DisplaySlot toVanilla(ScoreDisplaySlot slot) {
        return switch (Objects.requireNonNull(slot, "slot")) {
            case LIST -> DisplaySlot.LIST;
            case SIDEBAR -> DisplaySlot.SIDEBAR;
            case BELOW_NAME -> DisplaySlot.BELOW_NAME;
            case TEAM_BLACK -> DisplaySlot.TEAM_BLACK;
            case TEAM_DARK_BLUE -> DisplaySlot.TEAM_DARK_BLUE;
            case TEAM_DARK_GREEN -> DisplaySlot.TEAM_DARK_GREEN;
            case TEAM_DARK_AQUA -> DisplaySlot.TEAM_DARK_AQUA;
            case TEAM_DARK_RED -> DisplaySlot.TEAM_DARK_RED;
            case TEAM_DARK_PURPLE -> DisplaySlot.TEAM_DARK_PURPLE;
            case TEAM_GOLD -> DisplaySlot.TEAM_GOLD;
            case TEAM_GRAY -> DisplaySlot.TEAM_GRAY;
            case TEAM_DARK_GRAY -> DisplaySlot.TEAM_DARK_GRAY;
            case TEAM_BLUE -> DisplaySlot.TEAM_BLUE;
            case TEAM_GREEN -> DisplaySlot.TEAM_GREEN;
            case TEAM_AQUA -> DisplaySlot.TEAM_AQUA;
            case TEAM_RED -> DisplaySlot.TEAM_RED;
            case TEAM_LIGHT_PURPLE -> DisplaySlot.TEAM_LIGHT_PURPLE;
            case TEAM_YELLOW -> DisplaySlot.TEAM_YELLOW;
            case TEAM_WHITE -> DisplaySlot.TEAM_WHITE;
        };
    }

    static ScoreDisplaySlot fromVanilla(DisplaySlot slot) {
        return switch (slot) {
            case LIST -> ScoreDisplaySlot.LIST;
            case SIDEBAR -> ScoreDisplaySlot.SIDEBAR;
            case BELOW_NAME -> ScoreDisplaySlot.BELOW_NAME;
            case TEAM_BLACK -> ScoreDisplaySlot.TEAM_BLACK;
            case TEAM_DARK_BLUE -> ScoreDisplaySlot.TEAM_DARK_BLUE;
            case TEAM_DARK_GREEN -> ScoreDisplaySlot.TEAM_DARK_GREEN;
            case TEAM_DARK_AQUA -> ScoreDisplaySlot.TEAM_DARK_AQUA;
            case TEAM_DARK_RED -> ScoreDisplaySlot.TEAM_DARK_RED;
            case TEAM_DARK_PURPLE -> ScoreDisplaySlot.TEAM_DARK_PURPLE;
            case TEAM_GOLD -> ScoreDisplaySlot.TEAM_GOLD;
            case TEAM_GRAY -> ScoreDisplaySlot.TEAM_GRAY;
            case TEAM_DARK_GRAY -> ScoreDisplaySlot.TEAM_DARK_GRAY;
            case TEAM_BLUE -> ScoreDisplaySlot.TEAM_BLUE;
            case TEAM_GREEN -> ScoreDisplaySlot.TEAM_GREEN;
            case TEAM_AQUA -> ScoreDisplaySlot.TEAM_AQUA;
            case TEAM_RED -> ScoreDisplaySlot.TEAM_RED;
            case TEAM_LIGHT_PURPLE -> ScoreDisplaySlot.TEAM_LIGHT_PURPLE;
            case TEAM_YELLOW -> ScoreDisplaySlot.TEAM_YELLOW;
            case TEAM_WHITE -> ScoreDisplaySlot.TEAM_WHITE;
        };
    }

    static ObjectiveCriteria.RenderType toVanilla(ScoreRenderType renderType) {
        return switch (Objects.requireNonNull(renderType, "renderType")) {
            case INTEGER -> ObjectiveCriteria.RenderType.INTEGER;
            case HEARTS -> ObjectiveCriteria.RenderType.HEARTS;
        };
    }

    static ScoreRenderType fromVanilla(ObjectiveCriteria.RenderType renderType) {
        return switch (renderType) {
            case INTEGER -> ScoreRenderType.INTEGER;
            case HEARTS -> ScoreRenderType.HEARTS;
        };
    }

    static Team.Visibility toVanilla(TeamVisibility visibility) {
        return switch (Objects.requireNonNull(visibility, "visibility")) {
            case ALWAYS -> Team.Visibility.ALWAYS;
            case NEVER -> Team.Visibility.NEVER;
            case HIDE_FOR_OTHER_TEAMS -> Team.Visibility.HIDE_FOR_OTHER_TEAMS;
            case HIDE_FOR_OWN_TEAM -> Team.Visibility.HIDE_FOR_OWN_TEAM;
        };
    }

    static TeamVisibility fromVanilla(Team.Visibility visibility) {
        return switch (visibility) {
            case ALWAYS -> TeamVisibility.ALWAYS;
            case NEVER -> TeamVisibility.NEVER;
            case HIDE_FOR_OTHER_TEAMS -> TeamVisibility.HIDE_FOR_OTHER_TEAMS;
            case HIDE_FOR_OWN_TEAM -> TeamVisibility.HIDE_FOR_OWN_TEAM;
        };
    }

    static Team.CollisionRule toVanilla(TeamCollisionRule rule) {
        return switch (Objects.requireNonNull(rule, "rule")) {
            case ALWAYS -> Team.CollisionRule.ALWAYS;
            case NEVER -> Team.CollisionRule.NEVER;
            case PUSH_OTHER_TEAMS -> Team.CollisionRule.PUSH_OTHER_TEAMS;
            case PUSH_OWN_TEAM -> Team.CollisionRule.PUSH_OWN_TEAM;
        };
    }

    static TeamCollisionRule fromVanilla(Team.CollisionRule rule) {
        return switch (rule) {
            case ALWAYS -> TeamCollisionRule.ALWAYS;
            case NEVER -> TeamCollisionRule.NEVER;
            case PUSH_OTHER_TEAMS -> TeamCollisionRule.PUSH_OTHER_TEAMS;
            case PUSH_OWN_TEAM -> TeamCollisionRule.PUSH_OWN_TEAM;
        };
    }

    static ChatFormatting toVanilla(@Nullable NamedTextColor color) {
        if (color == null) {
            return ChatFormatting.RESET;
        }
        if (color == NamedTextColor.BLACK) return ChatFormatting.BLACK;
        if (color == NamedTextColor.DARK_BLUE) return ChatFormatting.DARK_BLUE;
        if (color == NamedTextColor.DARK_GREEN) return ChatFormatting.DARK_GREEN;
        if (color == NamedTextColor.DARK_AQUA) return ChatFormatting.DARK_AQUA;
        if (color == NamedTextColor.DARK_RED) return ChatFormatting.DARK_RED;
        if (color == NamedTextColor.DARK_PURPLE) return ChatFormatting.DARK_PURPLE;
        if (color == NamedTextColor.GOLD) return ChatFormatting.GOLD;
        if (color == NamedTextColor.GRAY) return ChatFormatting.GRAY;
        if (color == NamedTextColor.DARK_GRAY) return ChatFormatting.DARK_GRAY;
        if (color == NamedTextColor.BLUE) return ChatFormatting.BLUE;
        if (color == NamedTextColor.GREEN) return ChatFormatting.GREEN;
        if (color == NamedTextColor.AQUA) return ChatFormatting.AQUA;
        if (color == NamedTextColor.RED) return ChatFormatting.RED;
        if (color == NamedTextColor.LIGHT_PURPLE) return ChatFormatting.LIGHT_PURPLE;
        if (color == NamedTextColor.YELLOW) return ChatFormatting.YELLOW;
        if (color == NamedTextColor.WHITE) return ChatFormatting.WHITE;
        return ChatFormatting.RESET;
    }

    static @Nullable NamedTextColor fromVanilla(ChatFormatting color) {
        return switch (color) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    static Optional<TeamColor> toVanillaTeamColor(@Nullable NamedTextColor color) {
        if (color == null) {
            return Optional.empty();
        }
        if (color == NamedTextColor.BLACK) return Optional.of(TeamColor.BLACK);
        if (color == NamedTextColor.DARK_BLUE) return Optional.of(TeamColor.DARK_BLUE);
        if (color == NamedTextColor.DARK_GREEN) return Optional.of(TeamColor.DARK_GREEN);
        if (color == NamedTextColor.DARK_AQUA) return Optional.of(TeamColor.DARK_AQUA);
        if (color == NamedTextColor.DARK_RED) return Optional.of(TeamColor.DARK_RED);
        if (color == NamedTextColor.DARK_PURPLE) return Optional.of(TeamColor.DARK_PURPLE);
        if (color == NamedTextColor.GOLD) return Optional.of(TeamColor.GOLD);
        if (color == NamedTextColor.GRAY) return Optional.of(TeamColor.GRAY);
        if (color == NamedTextColor.DARK_GRAY) return Optional.of(TeamColor.DARK_GRAY);
        if (color == NamedTextColor.BLUE) return Optional.of(TeamColor.BLUE);
        if (color == NamedTextColor.GREEN) return Optional.of(TeamColor.GREEN);
        if (color == NamedTextColor.AQUA) return Optional.of(TeamColor.AQUA);
        if (color == NamedTextColor.RED) return Optional.of(TeamColor.RED);
        if (color == NamedTextColor.LIGHT_PURPLE) return Optional.of(TeamColor.LIGHT_PURPLE);
        if (color == NamedTextColor.YELLOW) return Optional.of(TeamColor.YELLOW);
        if (color == NamedTextColor.WHITE) return Optional.of(TeamColor.WHITE);
        return Optional.empty();
    }

    static @Nullable NamedTextColor fromVanillaTeamColor(Optional<TeamColor> color) {
        return color.map(value -> switch (value) {
            case BLACK -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE -> NamedTextColor.WHITE;
        }).orElse(null);
    }

    static @Nullable NumberFormat toVanilla(ScoreNumberFormat format, RegistryAccess registries) {
        return switch (Objects.requireNonNull(format, "format").kind()) {
            case DEFAULT -> null;
            case BLANK -> BlankFormat.INSTANCE;
            case FIXED -> new FixedFormat(AdventureBridge.toVanilla(format.fixedValue().orElseThrow(), registries));
            case STYLED -> new StyledFormat(StyleBridge.toVanilla(format.style().orElseThrow()));
        };
    }

    static ScoreNumberFormat fromVanilla(@Nullable NumberFormat format, RegistryAccess registries) {
        if (format == null) {
            return ScoreNumberFormat.DEFAULT;
        }
        if (format instanceof BlankFormat) {
            return ScoreNumberFormat.BLANK;
        }
        if (format instanceof FixedFormat fixed) {
            return ScoreNumberFormat.fixed(AdventureBridge.fromVanilla(fixed.value(), registries));
        }
        if (format instanceof StyledFormat styled) {
            return ScoreNumberFormat.styled(StyleBridge.fromVanilla(styled.style()));
        }
        return ScoreNumberFormat.DEFAULT;
    }
}
