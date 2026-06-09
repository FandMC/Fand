package io.fand.api.entity;

import io.fand.api.item.ItemStack;
import java.util.Objects;

/** A single villager merchant offer. */
public record VillagerTrade(
        ItemStack firstCost,
        ItemStack secondCost,
        ItemStack result,
        int uses,
        int maxUses,
        int experience,
        float priceMultiplier,
        int demand,
        int specialPrice,
        boolean rewardExperience) {

    public VillagerTrade {
        Objects.requireNonNull(firstCost, "firstCost");
        Objects.requireNonNull(secondCost, "secondCost");
        Objects.requireNonNull(result, "result");
        uses = Math.max(0, uses);
        maxUses = Math.max(0, maxUses);
        experience = Math.max(0, experience);
    }

    public VillagerTrade(ItemStack firstCost, ItemStack secondCost, ItemStack result, int maxUses, int experience) {
        this(firstCost, secondCost, result, 0, maxUses, experience, 0.05F, 0, 0, true);
    }
}
