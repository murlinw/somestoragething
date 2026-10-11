package com.murlinwei.sidedstorage.block;

import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.menu.SidedStorageMenu;
import com.murlinwei.sidedstorage.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the storage. The backing inventory is one flat {@link ItemStackHandler}; each face gets a
 * {@link RangedWrapper} view over its own contiguous slice of it.
 *
 * <p>Face order matches {@link Direction#values()} (down, up, north, south, west, east), so face <em>n</em> owns
 * slots {@code [n * slotsPerSide, (n + 1) * slotsPerSide)}.
 *
 * <p>The selected face is remembered here rather than in the menu so that reopening the screen after a tab switch
 * comes back on the same face.
 */
public class SidedStorageBlockEntity extends BlockEntity implements MenuProvider, IMenuProviderExtension {

    private final StorageTier tier;
    private final ItemStackHandler itemHandler;
    private final IItemHandler[] sideHandlers;
    private int activeSide = Direction.UP.ordinal();

    public SidedStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIDED_STORAGE.get(), pos, state);
        this.tier = state.getBlock() instanceof SidedStorageBlock block ? block.getTier() : StorageTier.BASIC;
        this.itemHandler = new ItemStackHandler(this.tier.getTotalSlots()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.sideHandlers = new IItemHandler[StorageTier.SIDES];
        int per = this.tier.getSlotsPerSide();
        for (int side = 0; side < StorageTier.SIDES; side++) {
            this.sideHandlers[side] = new RangedWrapper(this.itemHandler, side * per, (side + 1) * per);
        }
    }

    public StorageTier getTier() {
        return this.tier;
    }

    /** The whole inventory, all faces together. */
    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    /**
     * The handler for one face. A {@code null} direction (a query that names no side) yields the whole inventory,
     * so side-agnostic automation keeps working.
     */
    public IItemHandler getHandlerForSide(@Nullable Direction side) {
        return side == null ? this.itemHandler : this.sideHandlers[side.ordinal()];
    }

    public Component getTitle() {
        return Component.translatable("block.sidedstorage." + this.tier.getBlockPath());
    }

    // ---- selected face ----

    public int getActiveSide() {
        return this.activeSide;
    }

    public void setActiveSide(int side) {
        if (side >= 0 && side < StorageTier.SIDES) {
            this.activeSide = side;
            setChanged();
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName() {
        return getTitle();
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SidedStorageMenu(containerId, inventory, this, this.activeSide);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        // Nothing to add here: the block (and the tab-switch handler) already append the position/face to the
        // extra data, and the client menu reads exactly that layout.
    }

    /**
     * Tab switches reopen the menu while one is already open. Returning false
     * makes that reopen silent server-side ({@code doCloseContainer} instead of
     * {@code closeContainer}), so the client never sees a close packet, the
     * screen is replaced directly, and the mouse is not recentered.
     */
    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return false;
    }

    // ---- persistence ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.putInt("ActiveSide", this.activeSide);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("ActiveSide", Tag.TAG_INT)) {
            this.activeSide = Mth.clamp(tag.getInt("ActiveSide"), 0, StorageTier.SIDES - 1);
        }
    }
}