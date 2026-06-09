package io.fand.server.entity;

import io.fand.api.entity.TextDisplay;
import io.fand.server.command.AdventureBridge;
import io.fand.server.world.WorldRegistry;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public final class FandTextDisplay extends FandDisplay implements TextDisplay {

    private static final byte FLAG_SHADOW = net.minecraft.world.entity.Display.TextDisplay.FLAG_SHADOW;
    private static final byte FLAG_SEE_THROUGH = net.minecraft.world.entity.Display.TextDisplay.FLAG_SEE_THROUGH;
    private static final byte FLAG_USE_DEFAULT_BACKGROUND = net.minecraft.world.entity.Display.TextDisplay.FLAG_USE_DEFAULT_BACKGROUND;
    private static final byte FLAG_ALIGN_LEFT = net.minecraft.world.entity.Display.TextDisplay.FLAG_ALIGN_LEFT;
    private static final byte FLAG_ALIGN_RIGHT = net.minecraft.world.entity.Display.TextDisplay.FLAG_ALIGN_RIGHT;

    public FandTextDisplay(net.minecraft.world.entity.Display.TextDisplay handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Display.TextDisplay handle() {
        return (net.minecraft.world.entity.Display.TextDisplay) handle;
    }

    @Override
    public Component text() {
        return AdventureBridge.fromVanilla(handle().fand$text(), handle().registryAccess());
    }

    @Override
    public void setText(Component text) {
        Objects.requireNonNull(text, "text");
        runOnServerThread(() -> handle().fand$setText(AdventureBridge.toVanilla(text, handle().registryAccess())));
    }

    @Override
    public int lineWidth() {
        return handle().fand$lineWidth();
    }

    @Override
    public void setLineWidth(int width) {
        runOnServerThread(() -> handle().fand$setLineWidth(Math.max(1, width)));
    }

    @Override
    public int textOpacity() {
        return handle().fand$textOpacity();
    }

    @Override
    public void setTextOpacity(int opacity) {
        runOnServerThread(() -> handle().fand$setTextOpacity((byte) Math.clamp(opacity, -1, 255)));
    }

    @Override
    public int backgroundColor() {
        return handle().fand$backgroundColor();
    }

    @Override
    public void setBackgroundColor(int argb) {
        runOnServerThread(() -> handle().fand$setBackgroundColor(argb));
    }

    @Override
    public boolean shadowed() {
        return hasFlag(FLAG_SHADOW);
    }

    @Override
    public void setShadowed(boolean shadowed) {
        setFlag(FLAG_SHADOW, shadowed);
    }

    @Override
    public boolean seeThrough() {
        return hasFlag(FLAG_SEE_THROUGH);
    }

    @Override
    public void setSeeThrough(boolean seeThrough) {
        setFlag(FLAG_SEE_THROUGH, seeThrough);
    }

    @Override
    public boolean defaultBackground() {
        return hasFlag(FLAG_USE_DEFAULT_BACKGROUND);
    }

    @Override
    public void setDefaultBackground(boolean defaultBackground) {
        setFlag(FLAG_USE_DEFAULT_BACKGROUND, defaultBackground);
    }

    @Override
    public Alignment alignment() {
        return switch (net.minecraft.world.entity.Display.TextDisplay.getAlign(handle().fand$styleFlags())) {
            case CENTER -> Alignment.CENTER;
            case LEFT -> Alignment.LEFT;
            case RIGHT -> Alignment.RIGHT;
        };
    }

    @Override
    public void setAlignment(Alignment alignment) {
        Objects.requireNonNull(alignment, "alignment");
        runOnServerThread(() -> {
            byte flags = (byte) (handle().fand$styleFlags() & ~(FLAG_ALIGN_LEFT | FLAG_ALIGN_RIGHT));
            flags = switch (alignment) {
                case CENTER -> flags;
                case LEFT -> (byte) (flags | FLAG_ALIGN_LEFT);
                case RIGHT -> (byte) (flags | FLAG_ALIGN_RIGHT);
            };
            handle().fand$setStyleFlags(flags);
        });
    }

    private boolean hasFlag(byte flag) {
        return (handle().fand$styleFlags() & flag) != 0;
    }

    private void setFlag(byte flag, boolean enabled) {
        runOnServerThread(() -> {
            byte flags = handle().fand$styleFlags();
            handle().fand$setStyleFlags(enabled ? (byte) (flags | flag) : (byte) (flags & ~flag));
        });
    }
}
