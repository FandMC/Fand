package io.fand.api.block.custom;

import io.fand.api.block.BlockType;
import java.util.Objects;

/** Mining properties for a logical custom block. */
public record CustomBlockMining(
        float hardness,
        float blastResistance,
        BlockType toolRuleBase,
        boolean requiresCorrectTool
) {

    public CustomBlockMining {
        if (hardness < -1.0F) {
            throw new IllegalArgumentException("hardness must be >= -1");
        }
        if (blastResistance < 0.0F) {
            throw new IllegalArgumentException("blastResistance must be >= 0");
        }
        Objects.requireNonNull(toolRuleBase, "toolRuleBase");
        if (toolRuleBase instanceof CustomBlockType) {
            throw new IllegalArgumentException("toolRuleBase must be a vanilla block type");
        }
    }

    public static CustomBlockMining inherit(BlockType baseType) {
        Objects.requireNonNull(baseType, "baseType");
        var physics = baseType.physics();
        return new CustomBlockMining(
                physics.hardness(),
                physics.blastResistance(),
                baseType,
                physics.requiresTool());
    }
}
