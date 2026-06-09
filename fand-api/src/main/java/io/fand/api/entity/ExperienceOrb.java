package io.fand.api.entity;

/**
 * Experience orb entity.
 */
public interface ExperienceOrb extends Entity {

    int experience();

    void setExperience(int experience);

    int icon();

    int age();

    void setAge(int ticks);

    int count();

    void setCount(int count);
}
