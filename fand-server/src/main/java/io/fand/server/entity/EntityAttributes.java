package io.fand.server.entity;

import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

final class EntityAttributes {

    private EntityAttributes() {
    }

    static Optional<Holder.Reference<Attribute>> holder(Key key) {
        var id = Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        return BuiltInRegistries.ATTRIBUTE.get(id);
    }
}
