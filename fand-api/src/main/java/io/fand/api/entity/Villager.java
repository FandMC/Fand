package io.fand.api.entity;

import io.fand.api.item.component.VillagerVariantKey;
import java.util.List;
import net.kyori.adventure.key.Key;

/** Villager profession, level, experience, and trade state. */
public interface Villager extends Ageable {

    Key villagerType();

    void setVillagerType(Key type);

    default void setVillagerType(VillagerVariantKey type) {
        setVillagerType(type.key());
    }

    Key profession();

    void setProfession(Key profession);

    default void setProfession(VillagerProfessionKey profession) {
        setProfession(profession.key());
    }

    int villagerLevel();

    void setVillagerLevel(int level);

    int villagerExperience();

    void setVillagerExperience(int experience);

    List<VillagerTrade> trades();

    void setTrades(List<VillagerTrade> trades);

    void restockTrades();
}
