/**
 * Item types and immutable item stacks. {@link io.fand.api.item.ItemStack}
 * carries a type, amount, and modern item data components. Item data is a value
 * model: methods never mutate the current stack, and absent components are
 * exposed as {@link java.util.Optional} or empty collections.
 */
@NullMarked
package io.fand.api.item;

import org.jspecify.annotations.NullMarked;
