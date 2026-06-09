package io.fand.server.block;

import io.fand.api.block.BannerBlockEntity;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemDyeColor;
import io.fand.server.command.AdventureBridge;
import io.fand.server.item.FandItemStacks;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public final class FandBannerBlockEntity extends FandBlockEntity implements BannerBlockEntity {

    public FandBannerBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.BannerBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.BannerBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.BannerBlockEntity) handle;
    }

    @Override
    public ItemDyeColor baseColor() {
        return ItemDyeColor.fromSerializedName(handle().getBaseColor().getSerializedName());
    }

    @Override
    public ItemStack asItem() {
        return FandItemStacks.fromVanilla(handle().getItem());
    }

    @Override
    public Optional<Component> customName() {
        return Optional.ofNullable(handle().getCustomName())
                .map(name -> AdventureBridge.fromVanilla(name, block.worldHandle().registryAccess()));
    }
}
