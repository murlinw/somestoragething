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

    StorageTier(String name, int slotsPerSide) {
        this.name = name;
        this.slotsPerSide = slotsPerSide;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** Number of slots backing a single face. */
    public int getSlotsPerSide() {
        return this.slotsPerSide;
    }

    /** Total number of slots across every face. */
    public int getTotalSlots() {
        return this.slotsPerSide * SIDES;
    }

    /** How many rows of nine the GUI shows for one face. */
    public int getRows() {
        return this.slotsPerSide / 9;
    }

    /** Registry path of the block that uses this tier, e.g. {@code sided_storage_basic}. */
    public String getBlockPath() {
        return "sided_storage_" + this.name;
    }
}