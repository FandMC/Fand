package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetExperiencePacket}. */
public interface ClientboundSetExperienceView extends PacketView {

    default float experienceProgress() {
        return require("experienceProgress", float.class);
    }
    default int totalExperience() {
        return require("totalExperience", int.class);
    }
    default int experienceLevel() {
        return require("experienceLevel", int.class);
    }

    /** Returns a copy with {@code experienceProgress} replaced. */
    default ClientboundSetExperienceView withExperienceProgress(float experienceProgress) {
        return (ClientboundSetExperienceView) with("experienceProgress", experienceProgress);
    }
    /** Returns a copy with {@code totalExperience} replaced. */
    default ClientboundSetExperienceView withTotalExperience(int totalExperience) {
        return (ClientboundSetExperienceView) with("totalExperience", totalExperience);
    }
    /** Returns a copy with {@code experienceLevel} replaced. */
    default ClientboundSetExperienceView withExperienceLevel(int experienceLevel) {
        return (ClientboundSetExperienceView) with("experienceLevel", experienceLevel);
    }
}
