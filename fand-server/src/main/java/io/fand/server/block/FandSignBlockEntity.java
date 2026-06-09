package io.fand.server.block;

import io.fand.api.block.SignBlockEntity;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public final class FandSignBlockEntity extends FandBlockEntity implements SignBlockEntity {

    public FandSignBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.SignBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.SignBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.SignBlockEntity) handle;
    }

    @Override
    public Component line(int index, boolean front) {
        checkLine(index);
        var message = handle().getText(front).getMessage(index, false);
        return AdventureBridge.fromVanilla(message, block.worldHandle().registryAccess());
    }

    @Override
    public void setLine(int index, boolean front, Component line) {
        checkLine(index);
        Objects.requireNonNull(line, "line");
        block.runOnServerThread(() -> {
            var vanilla = AdventureBridge.toVanilla(line, block.worldHandle().registryAccess());
            handle().updateText(text -> text.setMessage(index, vanilla), front);
        });
    }

    @Override
    public boolean waxed() {
        return handle().isWaxed();
    }

    @Override
    public void setWaxed(boolean waxed) {
        block.runOnServerThread(() -> handle().setWaxed(waxed));
    }

    @Override
    public boolean glowingText(boolean front) {
        return handle().getText(front).hasGlowingText();
    }

    @Override
    public void setGlowingText(boolean front, boolean glowing) {
        block.runOnServerThread(() -> handle().updateText(text -> text.setHasGlowingText(glowing), front));
    }

    private static void checkLine(int index) {
        if (index < 0 || index >= 4) {
            throw new IndexOutOfBoundsException("Sign line index must be between 0 and 3");
        }
    }
}
