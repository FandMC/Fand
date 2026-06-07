/**
 * Inventory abstractions. {@link io.fand.api.inventory.Inventory} is a slot-based
 * read/write view over the underlying container. Live inventory access requires
 * the server thread unless a method explicitly documents marshalling.
 */
@NullMarked
package io.fand.api.inventory;

import org.jspecify.annotations.NullMarked;
