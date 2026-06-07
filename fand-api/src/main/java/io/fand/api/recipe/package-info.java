/**
 * Recipe registration and lookup API.
 *
 * <p>Recipes are live server state. Registration and removal marshal to the
 * server thread; lookup methods return snapshots. Recipe outputs use
 * {@link io.fand.api.item.ItemStack}, so modern data components are preserved.
 */
@NullMarked
package io.fand.api.recipe;

import org.jspecify.annotations.NullMarked;
