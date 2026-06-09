package io.fand.server.block;

import io.fand.api.block.LecternBlockEntity;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Objects;

public final class FandLecternBlockEntity extends FandBlockEntity implements LecternBlockEntity {

    public FandLecternBlockEntity(FandBlock block, net.minecraft.world.level.block.entity.LecternBlockEntity handle) {
        super(block, handle);
    }

    @Override
    public net.minecraft.world.level.block.entity.LecternBlockEntity handle() {
        return (net.minecraft.world.level.block.entity.LecternBlockEntity) handle;
    }

    @Override
    public ItemStack book() {
        return FandItemStacks.fromVanilla(handle().getBook());
    }

    @Override
    public void setBook(ItemStack book) {
        Objects.requireNonNull(book, "book");
        handle().setBook(FandItemStacks.toVanilla(book));
    }

    @Override
    public boolean hasBook() {
        return handle().hasBook();
    }

    @Override
    public int page() {
        return handle().getPage();
    }

    @Override
    public void setPage(int page) {
        handle().fand$setPage(page);
    }

    @Override
    public int redstoneSignal() {
        return handle().getRedstoneSignal();
    }
}
