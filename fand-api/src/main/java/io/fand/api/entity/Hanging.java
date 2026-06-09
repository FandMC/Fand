package io.fand.api.entity;

import io.fand.api.block.Block;

/**
 * Entity attached to a block, such as paintings and item frames.
 */
public interface Hanging extends Entity {

    Block attachedBlock();

    boolean survives();
}
