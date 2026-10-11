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
 * <p>The face remembered here seeds newly created menus. Each open menu retains its own fixed face so players
 * viewing the same storage do not change each other's inventory view.
 */
public class SidedStorageBlockEntity extends BlockEntity implements MenuProvider, IMenuProviderExtension {

    private final StorageTier tier;
    private final ItemStackHandler itemHandler;
    private final IItemHandler[] sideHandlers;
    private int activeSide = Direction.UP.ordinal();

    /**
     * Creates the tier-sized backing inventory and six ranged views, one for each world direction. Each
     * view covers a separate contiguous slice, which enforces Sided Storage's partition boundaries.
     */
    public SidedStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIDED_STORAGE.get(), pos, state);
        this.tier = state.getBlock() instanceof SidedStorageBlock block ? block.getTier() : StorageTier.BASIC;
        this.itemHandler = new ItemStackHandler(this.tier.getTotalSlots()) {
            /**
             * Marks inventory edits for saving and refreshes comparator output on the server. Both GUI edits and
             * automation transfers use this callback.
             */
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                }
            }
        };
        this.sideHandlers = new IItemHandler[StorageTier.SIDES];
        int per = this.tier.getSlotsPerSide();
        for (int side = 0; side < StorageTier.SIDES; side++) {
            this.sideHandlers[side] = new RangedWrapper(this.itemHandler, side * per, (side + 1) * per);
        }
    }

    /**
     * Returns the inventory owner's tier so menu creation and upgrade logic can determine its face
     * capacity.
     */
    public StorageTier getTier() {
        return this.tier;
    }

    /**
     * Provides internal mod code with the full backing inventory for saving, spilling contents, upgrades,
     * and comparator calculations. Automation must use the restricted directional capability instead.
     */
    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    /**
     * Returns only the partition assigned to the requested direction. Returns null for an unsided query so
     * automation cannot bypass Sided Storage's face separation.
     */
    public @Nullable IItemHandler getHandlerForSide(@Nullable Direction side) {
        return side == null ? null : this.sideHandlers[side.ordinal()];
    }

    /**
     * Builds the translated title for the current storage tier. Menus display this as the name of the
     * storage block.
     */
    public Component getTitle() {
        return Component.translatable("block.sidedstorage." + this.tier.getBlockPath());
    }

    // ---- selected face ----

    /**
     * Returns the face remembered for the next menu opening. Already-open menus keep their own face
     * selection so players do not switch each other's tabs.
     */
    public int getActiveSide() {
        return this.activeSide;
    }

    /**
     * Remembers a valid face for menu creation and saves that selection with the block. Ignores values
     * outside Minecraft's six directions.
     */
    public void setActiveSide(int side) {
        if (side >= 0 && side < StorageTier.SIDES) {
            this.activeSide = side;
            setChanged();
        }
    }

    // ---- MenuProvider ----

    /**
     * Supplies Minecraft's menu-provider interface with the storage tier's translated title.
     */
    @Override
    public Component getDisplayName() {
        return getTitle();
    }

    /**
     * Creates a server menu backed by the remembered face's inventory. The resulting menu owns its
     * selection independently of later changes to this block's remembered face.
     */
    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SidedStorageMenu(containerId, inventory, this, this.activeSide);
    }

    /**
     * Adds no extra provider data because the block interaction and tab-switch handler already write the
     * complete opening payload. Keeping this hook empty avoids duplicating fields the client reads.
     */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        // Nothing to add here: the block (and the tab-switch handler) already append the position/face to the
        // extra data, and the client menu reads exactly that layout.
    }

    /**
     * Suppresses the separate client-close packet when opening a replacement storage menu. Face switches
     * can replace the screen without flashing the player inventory or recentering the mouse; normal
     * server-side menu cleanup still occurs.
     */
    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen() {
        return false;
    }

    // ---- persistence ----

    /**
     * Writes all six inventories and the remembered face into the block entity's save data. This preserves
     * stored items and the selection across world saves and reloads.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.putInt("ActiveSide", this.activeSide);
    }

    /**
     * Restores inventory contents and the remembered face from saved block data. Clamps the saved
     * selection to a valid direction before it can be used by a menu.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("ActiveSide", Tag.TAG_INT)) {
            this.activeSide = Mth.clamp(tag.getInt("ActiveSide"), 0, StorageTier.SIDES - 1);
        }
    }
}
