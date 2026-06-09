package io.fand.api.block;

import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemDyeColor;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/** Banner block entity. */
public interface BannerBlockEntity extends BlockEntity {

    ItemDyeColor baseColor();

    ItemStack asItem();

    Optional<Component> customName();
}
