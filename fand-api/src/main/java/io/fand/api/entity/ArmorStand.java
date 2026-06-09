package io.fand.api.entity;

/**
 * Armor stand entity.
 */
public interface ArmorStand extends LivingEntity {

    boolean small();

    void setSmall(boolean small);

    boolean armsVisible();

    void setArmsVisible(boolean visible);

    boolean basePlateVisible();

    void setBasePlateVisible(boolean visible);

    boolean marker();

    void setMarker(boolean marker);

    boolean invisible();

    void setInvisible(boolean invisible);
}
