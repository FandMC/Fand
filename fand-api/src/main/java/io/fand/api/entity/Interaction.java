package io.fand.api.entity;

public interface Interaction extends Entity {
    float interactionWidth();

    void setInteractionWidth(float width);

    float interactionHeight();

    void setInteractionHeight(float height);

    default void setInteractionSize(float width, float height) {
        setInteractionWidth(width);
        setInteractionHeight(height);
    }

    boolean responsive();

    void setResponsive(boolean responsive);
}
