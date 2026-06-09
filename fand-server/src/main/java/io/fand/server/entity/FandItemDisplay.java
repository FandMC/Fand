package io.fand.server.entity;

import io.fand.api.entity.ItemDisplay;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;

public final class FandItemDisplay extends FandDisplay implements ItemDisplay {

    public FandItemDisplay(net.minecraft.world.entity.Display.ItemDisplay handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Display.ItemDisplay handle() {
        return (net.minecraft.world.entity.Display.ItemDisplay) handle;
    }

    @Override
    public ItemStack displayedItem() {
        return FandItemStacks.fromVanilla(handle().fand$itemStack());
    }

    @Override
    public void setDisplayedItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        runOnServerThread(() -> handle().fand$setItemStack(FandItemStacks.toVanilla(item)));
    }

    @Override
    public Transform itemTransform() {
        return fromVanilla(handle().fand$itemTransform());
    }

    @Override
    public void setItemTransform(Transform transform) {
        Objects.requireNonNull(transform, "transform");
        runOnServerThread(() -> handle().fand$setItemTransform(toVanilla(transform)));
    }

    private static Transform fromVanilla(net.minecraft.world.item.ItemDisplayContext transform) {
        return switch (transform) {
            case NONE -> Transform.NONE;
            case THIRD_PERSON_LEFT_HAND -> Transform.THIRD_PERSON_LEFT_HAND;
            case THIRD_PERSON_RIGHT_HAND -> Transform.THIRD_PERSON_RIGHT_HAND;
            case FIRST_PERSON_LEFT_HAND -> Transform.FIRST_PERSON_LEFT_HAND;
            case FIRST_PERSON_RIGHT_HAND -> Transform.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> Transform.HEAD;
            case GUI -> Transform.GUI;
            case GROUND -> Transform.GROUND;
            case FIXED -> Transform.FIXED;
            case ON_SHELF -> Transform.ON_SHELF;
        };
    }

    private static net.minecraft.world.item.ItemDisplayContext toVanilla(Transform transform) {
        return switch (transform) {
            case NONE -> net.minecraft.world.item.ItemDisplayContext.NONE;
            case THIRD_PERSON_LEFT_HAND -> net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case THIRD_PERSON_RIGHT_HAND -> net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case FIRST_PERSON_LEFT_HAND -> net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            case FIRST_PERSON_RIGHT_HAND -> net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> net.minecraft.world.item.ItemDisplayContext.HEAD;
            case GUI -> net.minecraft.world.item.ItemDisplayContext.GUI;
            case GROUND -> net.minecraft.world.item.ItemDisplayContext.GROUND;
            case FIXED -> net.minecraft.world.item.ItemDisplayContext.FIXED;
            case ON_SHELF -> net.minecraft.world.item.ItemDisplayContext.ON_SHELF;
        };
    }
}
