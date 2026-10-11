package com.murlinwei.sidedstorage;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The three tiers of the Sided Storage block.
 *
 * <p>Each tier only differs in how many slots each of the six faces gets. The tier is stored on the
 * {@link net.minecraft.world.level.block.state.BlockState} block codec so that it survives datapack round-trips.
 */
public enum StorageTier implements StringRepresentable {
    BASIC("basic", 9),
    DENSE("dense", 18),
    CAPACITY("capacity", 27);

    /** The six faces of a block, in {@link net.minecraft.core.Direction#values()} order. */
    public static final int SIDES = 6;

    public static final Codec<StorageTier> CODEC = StringRepresentable.fromEnum(StorageTier::values);

    private final String name;
    private final int slotsPerSide;

    /**
     * Defines a storage tier's stable name and capacity per face. Basic, Dense, and Capacity use these
     * values to build their inventories, registry names, and GUI layouts.
     */
    StorageTier(String name, int slotsPerSide) {
        this.name = name;
        this.slotsPerSide = slotsPerSide;
    }

    /**
     * Returns the tier name used by Minecraft's string codec. This lets the mod serialize its block tier
     * consistently.
     */
    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Returns the capacity of one face's partition. Inventory wrappers and menus use this to keep Basic,
     * Dense, and Capacity slots separate.
     */
    public int getSlotsPerSide() {
        return this.slotsPerSide;
    }

    /**
     * Returns the combined capacity of all six faces. The block entity uses this size for its backing
     * inventory.
     */
    public int getTotalSlots() {
        return this.slotsPerSide * SIDES;
    }

    /**
     * Returns the number of nine-slot rows needed for one face. This describes the tier's storage grid
     * layout.
     */
    public int getRows() {
        return this.slotsPerSide / 9;
    }

    /**
     * Builds the tier-specific block registry path, such as sided_storage_basic. The mod also uses this
     * path for translated block titles.
     */
    public String getBlockPath() {
        return "sided_storage_" + this.name;
    }
}
