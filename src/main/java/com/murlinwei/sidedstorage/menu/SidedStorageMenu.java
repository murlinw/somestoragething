package com.murlinwei.sidedstorage.menu;

import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.block.SidedStorageBlock;
import com.murlinwei.sidedstorage.block.SidedStorageBlockEntity;
import com.murlinwei.sidedstorage.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for one face of a Sided Storage block.
 *
 * <p>The menu holds only the slots of the face currently being viewed, plus the 36 player slots. That keeps
 * shift-click and automation scoped to a single face: you cannot accidentally reach another face's items.
 *
 * <p>Slot positions are fixed for a slot's lifetime ({@code Slot.x}/{@code Slot.y} are {@code final}), so switching
 * faces reopens the menu rather than moving slots. The client copy is rebuilt from the extra data the server writes
 * when opening the menu, so both sides agree on which face is showing.
 */
public class SidedStorageMenu extends AbstractContainerMenu {

    /** Layout of the storage grid, in screen pixels. */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 47;
    public static final int COLUMNS = 9;

    private final IItemHandlerModifiable faceHandler;
    private final int activeSide;
    private final int slotsPerSide;
    private final BlockPos blockPos;

    /** Server side. */
    public SidedStorageMenu(int containerId, Inventory playerInventory, SidedStorageBlockEntity storage, int activeSide) {
        super(ModMenus.SIDED_STORAGE.get(), containerId);
        this.activeSide = Mth.clamp(activeSide, 0, StorageTier.SIDES - 1);
        this.slotsPerSide = storage.getTier().getSlotsPerSide();
        this.blockPos = storage.getBlockPos();
        this.faceHandler = modifiable(storage.getHandlerForSide(Direction.values()[this.activeSide]),
                this.slotsPerSide);
        this.addSlots(playerInventory);
    }

    /** Client side; the server wrote the face and position when the menu was opened. */
    public SidedStorageMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readAccess(playerInventory, buffer));
    }

    private SidedStorageMenu(int containerId, Inventory playerInventory, Access access) {
        super(ModMenus.SIDED_STORAGE.get(), containerId);
        this.activeSide = access.activeSide();
        this.slotsPerSide = access.slotsPerSide();
        this.blockPos = access.pos();
        this.faceHandler = access.handler();
        this.addSlots(playerInventory);
    }

    private void addSlots(Inventory playerInventory) {
        for (int i = 0; i < this.slotsPerSide; i++) {
            this.addSlot(new SlotItemHandler(this.faceHandler, i,
                    GRID_X + i % COLUMNS * 18,
                    GRID_Y + i / COLUMNS * 18));
        }

        int playerInvY = getPlayerInvY(this.slotsPerSide);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, playerInvY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, playerInvY + 58));
        }
    }

    /** Everything the client needs to rebuild a menu that matches the server's. */
    private record Access(IItemHandlerModifiable handler, int activeSide, int slotsPerSide, BlockPos pos) {}

    private static Access readAccess(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        int side = Mth.clamp(buffer.readVarInt(), 0, StorageTier.SIDES - 1);
        BlockPos pos = buffer.readBlockPos();

        if (playerInventory.player.level().getBlockEntity(pos) instanceof SidedStorageBlockEntity storage) {
            int perSide = storage.getTier().getSlotsPerSide();
            return new Access(modifiable(storage.getHandlerForSide(Direction.values()[side]), perSide),
                    side, perSide, pos);
        }

        // Block not loaded client-side: build an empty face of the right size and let the server resync.
        StorageTier tier = StorageTier.BASIC;
        return new Access(new ItemStackHandler(tier.getSlotsPerSide()), side, tier.getSlotsPerSide(), pos);
    }

    private static IItemHandlerModifiable modifiable(IItemHandler handler, int slotsPerSide) {
        return handler instanceof IItemHandlerModifiable modifiable ? modifiable : new ItemStackHandler(slotsPerSide);
    }

    private static int getPlayerInvY(int slotsPerSide) {
        return GRID_Y + (slotsPerSide / COLUMNS) * 18 + 12;
    }

    public int getActiveSide() {
        return this.activeSide;
    }

    public int getSlotsPerSide() {
        return this.slotsPerSide;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public int getPlayerInvY() {
        return getPlayerInvY(this.slotsPerSide);
    }

    /** Total panel height, derived from how many rows the selected face needs. */
    public int getImageHeight() {
        return getPlayerInvY() + 82;
    }

    /** Mirrors the vanilla check: the block must still be ours, and the player must still be in reach. */
    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.blockPos).getBlock() instanceof SidedStorageBlock
                && player.canInteractWithBlock(this.blockPos, 4.0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        boolean moved = index < this.slotsPerSide
                ? this.moveItemStackTo(original, this.slotsPerSide, this.slots.size(), true)
                : this.moveItemStackTo(original, 0, this.slotsPerSide, false);

        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}