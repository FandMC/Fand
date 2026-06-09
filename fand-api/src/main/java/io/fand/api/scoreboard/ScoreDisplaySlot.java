package io.fand.api.scoreboard;

/**
 * Vanilla scoreboard display slots.
 */
public enum ScoreDisplaySlot {
    LIST("list"),
    SIDEBAR("sidebar"),
    BELOW_NAME("below_name"),
    TEAM_BLACK("sidebar.team.black"),
    TEAM_DARK_BLUE("sidebar.team.dark_blue"),
    TEAM_DARK_GREEN("sidebar.team.dark_green"),
    TEAM_DARK_AQUA("sidebar.team.dark_aqua"),
    TEAM_DARK_RED("sidebar.team.dark_red"),
    TEAM_DARK_PURPLE("sidebar.team.dark_purple"),
    TEAM_GOLD("sidebar.team.gold"),
    TEAM_GRAY("sidebar.team.gray"),
    TEAM_DARK_GRAY("sidebar.team.dark_gray"),
    TEAM_BLUE("sidebar.team.blue"),
    TEAM_GREEN("sidebar.team.green"),
    TEAM_AQUA("sidebar.team.aqua"),
    TEAM_RED("sidebar.team.red"),
    TEAM_LIGHT_PURPLE("sidebar.team.light_purple"),
    TEAM_YELLOW("sidebar.team.yellow"),
    TEAM_WHITE("sidebar.team.white");

    private final String id;

    ScoreDisplaySlot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
