package io.fand.api.scoreboard;

/**
 * Vanilla team visibility rules for name tags and death messages.
 */
public enum TeamVisibility {
    ALWAYS("always"),
    NEVER("never"),
    HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
    HIDE_FOR_OWN_TEAM("hideForOwnTeam");

    private final String id;

    TeamVisibility(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
