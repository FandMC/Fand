package io.fand.server.entity;

import io.fand.api.entity.Villager;
import io.fand.api.entity.VillagerTrade;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.WorldRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class FandVillager extends FandAgeable implements Villager {

    public FandVillager(net.minecraft.world.entity.npc.villager.Villager handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.npc.villager.Villager handle() {
        return (net.minecraft.world.entity.npc.villager.Villager) handle;
    }

    @Override
    public Key villagerType() {
        return apiKey(handle().getVillagerData().type().unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setVillagerType(Key type) {
        Objects.requireNonNull(type, "type");
        runOnServerThread(() -> handle().setVillagerData(handle().getVillagerData().withType(
                BuiltInRegistries.VILLAGER_TYPE.getOrThrow(villagerTypeKey(type)))));
    }

    @Override
    public Key profession() {
        return apiKey(handle().getVillagerData().profession().unwrapKey().orElseThrow().identifier());
    }

    @Override
    public void setProfession(Key profession) {
        Objects.requireNonNull(profession, "profession");
        runOnServerThread(() -> handle().setVillagerData(handle().getVillagerData().withProfession(
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(villagerProfessionKey(profession)))));
    }

    @Override
    public int villagerLevel() {
        return handle().getVillagerData().level();
    }

    @Override
    public void setVillagerLevel(int level) {
        int clamped = Math.max(VillagerData.MIN_VILLAGER_LEVEL, Math.min(VillagerData.MAX_VILLAGER_LEVEL, level));
        runOnServerThread(() -> handle().setVillagerData(handle().getVillagerData().withLevel(clamped)));
    }

    @Override
    public int villagerExperience() {
        return handle().getVillagerXp();
    }

    @Override
    public void setVillagerExperience(int experience) {
        runOnServerThread(() -> handle().setVillagerXp(Math.max(0, experience)));
    }

    @Override
    public List<VillagerTrade> trades() {
        return handle().getOffers().stream()
                .map(FandVillager::toApi)
                .toList();
    }

    @Override
    public void setTrades(List<VillagerTrade> trades) {
        Objects.requireNonNull(trades, "trades");
        var copy = List.copyOf(trades);
        runOnServerThread(() -> {
            var offers = new MerchantOffers();
            copy.stream()
                    .filter(Objects::nonNull)
                    .map(FandVillager::toVanilla)
                    .forEach(offers::add);
            handle().setOffers(offers);
        });
    }

    @Override
    public void restockTrades() {
        runOnServerThread(handle()::restock);
    }

    private static VillagerTrade toApi(MerchantOffer offer) {
        return new VillagerTrade(
                FandItemStacks.fromVanilla(offer.getBaseCostA()),
                FandItemStacks.fromVanilla(offer.getCostB()),
                FandItemStacks.fromVanilla(offer.getResult()),
                offer.getUses(),
                offer.getMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand(),
                offer.getSpecialPriceDiff(),
                offer.shouldRewardExp());
    }

    private static MerchantOffer toVanilla(VillagerTrade trade) {
        var firstCost = FandItemStacks.toVanilla(trade.firstCost());
        var secondCost = FandItemStacks.toVanilla(trade.secondCost());
        var offer = new MerchantOffer(
                new ItemCost(
                        firstCost.getItem().builtInRegistryHolder(),
                        Math.max(1, firstCost.getCount()),
                        DataComponentExactPredicate.EMPTY),
                secondCost.isEmpty()
                        ? Optional.empty()
                        : Optional.of(new ItemCost(
                                secondCost.getItem().builtInRegistryHolder(),
                                Math.max(1, secondCost.getCount()),
                                DataComponentExactPredicate.EMPTY)),
                FandItemStacks.toVanilla(trade.result()),
                trade.uses(),
                trade.maxUses(),
                trade.experience(),
                trade.priceMultiplier(),
                trade.demand());
        offer.setSpecialPriceDiff(trade.specialPrice());
        return offer;
    }

    private static ResourceKey<VillagerType> villagerTypeKey(Key key) {
        return ResourceKey.create(Registries.VILLAGER_TYPE, id(key));
    }

    private static ResourceKey<VillagerProfession> villagerProfessionKey(Key key) {
        return ResourceKey.create(Registries.VILLAGER_PROFESSION, id(key));
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key apiKey(Identifier id) {
        return Key.key(id.getNamespace(), id.getPath());
    }
}
