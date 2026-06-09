package io.fand.api.scoreboard;

/**
 * Vanilla team collision rules.
 */
public enum TeamCollisionRule {
    ALWAYS("always"),
    NEVER("never"),
    PUSH_OTHER_TEAMS("pushOtherTeams"),
    PUSH_OWN_TEAM("pushOwnTeam");

    private final String id;

    TeamCollisionRule(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
