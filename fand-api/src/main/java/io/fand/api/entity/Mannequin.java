package io.fand.api.entity;

import io.fand.api.player.PlayerProfile;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/** Player-shaped Mannequin profile and presentation controls. */
public interface Mannequin extends LivingEntity {

    enum Pose { STANDING, CROUCHING, SWIMMING, FALL_FLYING, SLEEPING }

    Optional<PlayerProfile> profile();
    void setProfile(@Nullable PlayerProfile profile);
    boolean immovable();
    void setImmovable(boolean immovable);
    Optional<Component> description();
    void setDescription(@Nullable Component description);
    Set<ClientSkinPart> visibleModelParts();
    void setVisibleModelParts(Set<ClientSkinPart> parts);
    ClientMainHand mainHand();
    void setMainHand(ClientMainHand hand);
    Pose mannequinPose();
    void setMannequinPose(Pose pose);
}
