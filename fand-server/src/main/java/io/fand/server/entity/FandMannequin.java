package io.fand.server.entity;

import io.fand.api.entity.ClientMainHand;
import io.fand.api.entity.ClientSkinPart;
import io.fand.api.entity.Mannequin;
import io.fand.api.player.PlayerProfile;
import io.fand.server.command.AdventureBridge;
import io.fand.server.player.PlayerProfiles;
import io.fand.server.world.WorldRegistry;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

public final class FandMannequin extends FandLivingEntity implements Mannequin {
    public FandMannequin(net.minecraft.world.entity.decoration.Mannequin handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override public net.minecraft.world.entity.decoration.Mannequin handle() { return (net.minecraft.world.entity.decoration.Mannequin) handle; }

    @Override
    public Optional<PlayerProfile> profile() {
        var profile = handle().getProfile().partialProfile();
        return profile.name().isBlank() ? Optional.empty() : Optional.of(PlayerProfiles.fromVanilla(profile));
    }

    @Override
    public void setProfile(@Nullable PlayerProfile profile) {
        runOnServerThread(() -> handle().fand$setProfile(profile == null
                ? net.minecraft.world.entity.decoration.Mannequin.DEFAULT_PROFILE
                : ResolvableProfile.createResolved(PlayerProfiles.toGameProfile(profile))));
    }

    @Override public boolean immovable() { return handle().fand$isImmovable(); }
    @Override public void setImmovable(boolean immovable) { runOnServerThread(() -> handle().fand$setImmovable(immovable)); }

    @Override
    public Optional<Component> description() {
        var description = handle().fand$getDescription();
        return description == null ? Optional.empty() : Optional.of(AdventureBridge.fromVanilla(description, handle().registryAccess()));
    }

    @Override
    public void setDescription(@Nullable Component description) {
        runOnServerThread(() -> handle().fand$setDescription(description == null
                ? null
                : AdventureBridge.toVanilla(description, handle().registryAccess())));
    }

    @Override
    public Set<ClientSkinPart> visibleModelParts() {
        int mask = Byte.toUnsignedInt(handle().fand$getModelParts());
        var parts = EnumSet.noneOf(ClientSkinPart.class);
        for (var part : ClientSkinPart.values()) {
            if ((mask & part.mask()) != 0) parts.add(part);
        }
        return Set.copyOf(parts);
    }

    @Override
    public void setVisibleModelParts(Set<ClientSkinPart> parts) {
        int mask = Set.copyOf(parts).stream().mapToInt(ClientSkinPart::mask).reduce(0, (left, right) -> left | right);
        runOnServerThread(() -> handle().fand$setModelParts((byte) mask));
    }

    @Override public ClientMainHand mainHand() { return handle().getMainArm() == HumanoidArm.LEFT ? ClientMainHand.LEFT : ClientMainHand.RIGHT; }
    @Override public void setMainHand(ClientMainHand hand) { runOnServerThread(() -> handle().setMainArm(hand == ClientMainHand.LEFT ? HumanoidArm.LEFT : HumanoidArm.RIGHT)); }
    @Override public Pose mannequinPose() { return Pose.valueOf(handle().getPose().name()); }
    @Override public void setMannequinPose(Pose pose) { runOnServerThread(() -> handle().setPose(net.minecraft.world.entity.Pose.valueOf(pose.name()))); }
}
