package io.fand.server.block;

import io.fand.api.block.CommandBlockEntity;
import io.fand.server.command.AdventureBridge;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

public final class FandCommandBlockEntity extends FandBlockEntity implements CommandBlockEntity {

    public FandCommandBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.CommandBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.CommandBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.CommandBlockEntity) handle;
    }

    @Override
    public String command() {
        return handle().getCommandBlock().getCommand();
    }

    @Override
    public void setCommand(String command) {
        handle().getCommandBlock().setCommand(command == null ? "" : command);
    }

    @Override
    public int successCount() {
        return handle().getCommandBlock().getSuccessCount();
    }

    @Override
    public void setSuccessCount(int count) {
        handle().getCommandBlock().setSuccessCount(Math.max(0, count));
    }

    @Override
    public boolean trackOutput() {
        return handle().getCommandBlock().isTrackOutput();
    }

    @Override
    public void setTrackOutput(boolean trackOutput) {
        handle().getCommandBlock().setTrackOutput(trackOutput);
    }

    @Override
    public Component lastOutput() {
        return AdventureBridge.fromVanilla(handle().getCommandBlock().getLastOutput(), block.worldHandle().registryAccess());
    }

    @Override
    public void setLastOutput(@Nullable Component output) {
        handle().getCommandBlock().setLastOutput(output == null
                ? null
                : AdventureBridge.toVanilla(output, block.worldHandle().registryAccess()));
    }

    @Override
    public Optional<Component> customName() {
        return Optional.ofNullable(handle().getCommandBlock().getCustomName())
                .map(name -> AdventureBridge.fromVanilla(name, block.worldHandle().registryAccess()));
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        handle().getCommandBlock().setCustomName(name == null
                ? null
                : AdventureBridge.toVanilla(name, block.worldHandle().registryAccess()));
    }

    @Override
    public boolean powered() {
        return handle().isPowered();
    }

    @Override
    public void setPowered(boolean powered) {
        handle().setPowered(powered);
    }

    @Override
    public boolean automatic() {
        return handle().isAutomatic();
    }

    @Override
    public void setAutomatic(boolean automatic) {
        handle().setAutomatic(automatic);
    }

    @Override
    public boolean conditionMet() {
        return handle().wasConditionMet();
    }

    @Override
    public Mode mode() {
        return switch (handle().getMode()) {
            case SEQUENCE -> Mode.SEQUENCE;
            case AUTO -> Mode.AUTO;
            case REDSTONE -> Mode.REDSTONE;
        };
    }

    @Override
    public boolean conditional() {
        return handle().isConditional();
    }
}
