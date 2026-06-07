/**
 * Block snapshots and types. Block instances are positional handles backed by
 * the loaded world; equality is by world + position. Reads require the server
 * thread, while writes marshal to it when documented by the method.
 */
@NullMarked
package io.fand.api.block;

import org.jspecify.annotations.NullMarked;
