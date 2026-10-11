package com.murlinwei.sidedstorage.menu;

import com.murlinwei.sidedstorage.StorageTier;
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
    private SidedStorageBlockEntity serverStorage;

    /**
     * Creates the authoritative server menu for one storage face. Adds that face's slots and the player
     * inventory, and remembers the exact block entity so replacement invalidates the menu.
     */
    public SidedStorageMenu(int containerId, Inventory playerInventory, SidedStorageBlockEntity storage, int activeSide) {
        super(ModMenus.SIDED_STORAGE.get(), containerId);
        this.activeSide = Mth.clamp(activeSide, 0, StorageTier.SIDES - 1);
        this.slotsPerSide = storage.getTier().getSlotsPerSide();
        this.blockPos = storage.getBlockPos();
        this.serverStorage = storage;
        this.faceHandler = modifiable(storage.getHandlerForSide(Direction.values()[this.activeSide]),
                this.slotsPerSide);
        this.addSlots(playerInventory);
    }

    /**
     * Creates the client menu from the server's opening payload. Reads the face, position, and capacity
     * instead of depending on the block entity already being loaded on the client.
     */
    public SidedStorageMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readAccess(buffer));
    }

    /**
     * Builds a client menu using the decoded face description and an empty inventory for slot
     * synchronization. Its slot count and layout match the server's selected partition.
     */
    private SidedStorageMenu(int containerId, Inventory playerInventory, Access access) {
        super(ModMenus.SIDED_STORAGE.get(), containerId);
        this.activeSide = access.activeSide();
        this.slotsPerSide = access.slotsPerSide();
        this.blockPos = access.pos();
        this.faceHandler = access.handler();
        this.addSlots(playerInventory);
    }

    /**
     * Adds only the selected face's storage slots, followed by the player inventory and hotbar. This
     * ordering lets shift-click transfer items without reaching another face.
     */
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

    /**
     * Decodes and validates the server-supplied menu size, then creates a client inventory of exactly that
     * size. Explicit capacity prevents Dense or Capacity menus from falling back to Basic when client
     * block data is unavailable.
     */
    private static Access readAccess(RegistryFriendlyByteBuf buffer) {
        int side = Mth.clamp(buffer.readVarInt(), 0, StorageTier.SIDES - 1);
        BlockPos pos = buffer.readBlockPos();
        int perSide = buffer.readVarInt();
        if (perSide != 9 && perSide != 18 && perSide != 27) throw new IllegalArgumentException("Invalid face size");
        return new Access(new ItemStackHandler(perSide), side, perSide, pos);
    }

    /**
     * Obtains the mutable inventory interface needed by storage slots. A correctly sized empty fallback
     * keeps menu construction safe if an unexpected handler implementation is supplied.
     */
    private static IItemHandlerModifiable modifiable(IItemHandler handler, int slotsPerSide) {
        return handler instanceof IItemHandlerModifiable modifiable ? modifiable : new ItemStackHandler(slotsPerSide);
    }

    /**
     * Calculates where the player inventory begins below the tier's storage rows. Both menu slot placement
     * and the screen use this layout calculation.
     */
    private static int getPlayerInvY(int slotsPerSide) {
        return GRID_Y + (slotsPerSide / COLUMNS) * 18 + 12;
    }

    /**
     * Returns this menu's fixed face selection. The screen uses it for the face label and to avoid
     * requesting the currently displayed tab.
     */
    public int getActiveSide() {
        return this.activeSide;
    }

    /**
     * Returns the number of storage slots exposed by this menu. The screen layout and shift-click
     * boundaries depend on this face capacity.
     */
    public int getSlotsPerSide() {
        return this.slotsPerSide;
    }

    /**
     * Returns the storage position associated with this menu. Face-switch packets use it, and the server
     * validates it against the player's open menu.
     */
    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    /**
     * Returns the vertical start of this menu's player inventory. The screen uses it to place the
     * inventory heading consistently with the slots.
     */
    public int getPlayerInvY() {
        return getPlayerInvY(this.slotsPerSide);
    }

    /**
     * Calculates the GUI panel height from storage capacity and the player inventory area. Each storage
     * tier can therefore use the same screen class.
     */
    public int getImageHeight() {
        return getPlayerInvY() + 82;
    }

    /**
     * Keeps client menus available for synchronization, while server menus require the original block
     * entity and a player within interaction reach. Breaking, replacing, or upgrading storage invalidates
     * old inventory views.
     */
    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || (player.level().getBlockEntity(this.blockPos) == this.serverStorage
                && player.canInteractWithBlock(this.blockPos, 4.0));
    }

    /**
     * Handles shift-click between the selected face and the player inventory. Transfers never include
     * other faces, and the changed source slot is updated for synchronization and saving.
     */
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
