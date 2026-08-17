package io.fand.api.event.entity;

import io.fand.api.entity.Entity;
import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemEquipmentSlot;
import io.fand.api.world.Vector3;
import java.util.Objects;

/** Fired before one kinetic-weapon contact applies damage, knockback, or dismounting. */
public final class EntityKineticWeaponHitEvent implements Event, Cancellable {

    private final LivingEntity attacker;
    private final Entity target;
    private final ItemStack weapon;
    private final ItemEquipmentSlot weaponSlot;
    private final int usedTicks;
    private final Vector3 attackerVelocity;
    private final Vector3 targetVelocity;
    private final double attackerSpeedProjection;
    private final double targetSpeedProjection;
    private final double relativeSpeed;
    private float damage;
    private boolean dealsDamage;
    private boolean dealsKnockback;
    private boolean dismounts;
    private boolean cancelled;

    public EntityKineticWeaponHitEvent(
            LivingEntity attacker,
            Entity target,
            ItemStack weapon,
            ItemEquipmentSlot weaponSlot,
            int usedTicks,
            Vector3 attackerVelocity,
            Vector3 targetVelocity,
            double attackerSpeedProjection,
            double targetSpeedProjection,
            double relativeSpeed,
            float damage,
            boolean dealsDamage,
            boolean dealsKnockback,
            boolean dismounts
    ) {
        this.attacker = Objects.requireNonNull(attacker, "attacker");
        this.target = Objects.requireNonNull(target, "target");
        this.weapon = Objects.requireNonNull(weapon, "weapon");
        this.weaponSlot = Objects.requireNonNull(weaponSlot, "weaponSlot");
        this.usedTicks = Math.max(0, usedTicks);
        this.attackerVelocity = Objects.requireNonNull(attackerVelocity, "attackerVelocity");
        this.targetVelocity = Objects.requireNonNull(targetVelocity, "targetVelocity");
        this.attackerSpeedProjection = attackerSpeedProjection;
        this.targetSpeedProjection = targetSpeedProjection;
        this.relativeSpeed = Math.max(0.0, relativeSpeed);
        setDamage(damage);
        this.dealsDamage = dealsDamage;
        this.dealsKnockback = dealsKnockback;
        this.dismounts = dismounts;
    }

    public LivingEntity attacker() { return attacker; }
    public Entity target() { return target; }
    public ItemStack weapon() { return weapon; }
    public ItemEquipmentSlot weaponSlot() { return weaponSlot; }
    public int usedTicks() { return usedTicks; }
    /** Motion in blocks per second, matching the kinetic weapon calculation. */
    public Vector3 attackerVelocity() { return attackerVelocity; }
    /** Motion in blocks per second, matching the kinetic weapon calculation. */
    public Vector3 targetVelocity() { return targetVelocity; }
    public double attackerSpeedProjection() { return attackerSpeedProjection; }
    public double targetSpeedProjection() { return targetSpeedProjection; }
    public double relativeSpeed() { return relativeSpeed; }
    public float damage() { return damage; }

    public void setDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0.0F) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        this.damage = damage;
    }

    public boolean dealsDamage() { return dealsDamage; }
    public void setDealsDamage(boolean dealsDamage) { this.dealsDamage = dealsDamage; }
    public boolean dealsKnockback() { return dealsKnockback; }
    public void setDealsKnockback(boolean dealsKnockback) { this.dealsKnockback = dealsKnockback; }
    public boolean dismounts() { return dismounts; }
    public void setDismounts(boolean dismounts) { this.dismounts = dismounts; }

    @Override
    public boolean cancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
