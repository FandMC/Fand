package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundMerchantOffersPacket}. */
public interface ClientboundMerchantOffersView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default Object offers() {
        return require("offers", Object.class);
    }
    default int villagerLevel() {
        return require("villagerLevel", int.class);
    }
    default int villagerXp() {
        return require("villagerXp", int.class);
    }
    default boolean showProgress() {
        return require("showProgress", boolean.class);
    }
    default boolean canRestock() {
        return require("canRestock", boolean.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundMerchantOffersView withContainerId(int containerId) {
        return (ClientboundMerchantOffersView) with("containerId", containerId);
    }
    /** Returns a copy with {@code offers} replaced. */
    default ClientboundMerchantOffersView withOffers(Object offers) {
        return (ClientboundMerchantOffersView) with("offers", offers);
    }
    /** Returns a copy with {@code villagerLevel} replaced. */
    default ClientboundMerchantOffersView withVillagerLevel(int villagerLevel) {
        return (ClientboundMerchantOffersView) with("villagerLevel", villagerLevel);
    }
    /** Returns a copy with {@code villagerXp} replaced. */
    default ClientboundMerchantOffersView withVillagerXp(int villagerXp) {
        return (ClientboundMerchantOffersView) with("villagerXp", villagerXp);
    }
    /** Returns a copy with {@code showProgress} replaced. */
    default ClientboundMerchantOffersView withShowProgress(boolean showProgress) {
        return (ClientboundMerchantOffersView) with("showProgress", showProgress);
    }
    /** Returns a copy with {@code canRestock} replaced. */
    default ClientboundMerchantOffersView withCanRestock(boolean canRestock) {
        return (ClientboundMerchantOffersView) with("canRestock", canRestock);
    }
}
