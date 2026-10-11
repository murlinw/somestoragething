package com.murlinwei.sidedstorage.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A storage block whose inventory is split into one partition per face. Items can only be inserted into, and taken
 * out of, the partition belonging to the face they went in through.
 */
public class SidedStorageBlock extends BaseEntityBlock {

    /** Codec used by the block-state registry. */
    public static final MapCodec<SidedStorageBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StorageTier.CODEC.fieldOf("tier").forGetter(SidedStorageBlock::getTier),
            propertiesCodec()).apply(instance, SidedStorageBlock::new));

    private final StorageTier tier;

    /**
     * Creates a placeable Sided Storage block with its tier and Minecraft block properties. The tier
     * determines the capacity of the block entity created when placed.
     */
    public SidedStorageBlock(StorageTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    /**
     * Supplies Minecraft with the codec that reconstructs this block's tier and properties. This keeps the
     * three storage variants distinguishable during block serialization.
     */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Returns this block's storage tier so its block entity can allocate the correct number of slots per
     * face.
     */
    public StorageTier getTier() {
        return this.tier;
    }

    /**
     * Creates the persistent inventory owner when a Sided Storage block is placed or loaded. All three
     * tiers use the same block entity class.
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SidedStorageBlockEntity(pos, state);
    }

    /**
     * Leaves the block-state property list empty because storage tier belongs to the registered block
     * variant. Face inventories do not require facing or rotation properties.
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // This block has no block state properties.
    }

    /**
     * Uses the normal baked block model to render Sided Storage. Its inventory GUI is handled separately
     * by SidedStorageScreen.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Advertises comparator support to Minecraft. Comparators can therefore query how full the storage
     * block is.
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * Computes a comparator signal from the combined fullness of all six partitions. Empty storage
     * produces 0, occupied storage at least 1, and completely full storage 15; different stack sizes are
     * weighted by their actual limits.
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SidedStorageBlockEntity storage)) return 0;
        var handler = storage.getItemHandler();
        double fullness = 0;
        boolean occupied = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                occupied = true;
                fullness += (double) stack.getCount() / Math.min(stack.getMaxStackSize(), handler.getSlotLimit(slot));
            }
        }
        return (int) Math.floor(fullness / handler.getSlots() * 14) + (occupied ? 1 : 0);
    }

    /**
     * Handles upgrading a placed Basic block with eight gold ingots or a Dense block with eight diamonds.
     * Copies items to the same slots on the same faces, empties the old inventory before replacement to
     * prevent spills, and restores it if replacement fails. Refreshes capabilities and comparator output,
     * and charges materials only after success; creative players upgrade for free.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState upgraded = this.tier == StorageTier.BASIC && stack.is(Items.GOLD_INGOT)
                ? ModBlocks.DENSE.get().defaultBlockState()
                : this.tier == StorageTier.DENSE && stack.is(Items.DIAMOND)
                ? ModBlocks.CAPACITY.get().defaultBlockState() : null;
        if (upgraded == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.getAbilities().instabuild && stack.getCount() < 8) return ItemInteractionResult.FAIL;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SidedStorageBlockEntity old)) return ItemInteractionResult.FAIL;

        SidedStorageBlockEntity replacement = new SidedStorageBlockEntity(pos, upgraded);
        for (Direction face : Direction.values()) {
            var source = old.getHandlerForSide(face);
            var target = replacement.getHandlerForSide(face);
            for (int slot = 0; slot < source.getSlots(); slot++) {
                target.insertItem(slot, source.getStackInSlot(slot).copy(), false);
            }
        }
        replacement.setActiveSide(old.getActiveSide());
        var source = old.getItemHandler();
        ItemStack[] backup = new ItemStack[source.getSlots()];
        for (int slot = 0; slot < source.getSlots(); slot++) {
            backup[slot] = source.getStackInSlot(slot).copy();
            source.setStackInSlot(slot, ItemStack.EMPTY);
        }
        if (!level.setBlock(pos, upgraded, Block.UPDATE_ALL)) {
            for (int slot = 0; slot < backup.length; slot++) source.setStackInSlot(slot, backup[slot]);
            return ItemInteractionResult.FAIL;
        }
        level.setBlockEntity(replacement);
        replacement.setChanged();
        level.invalidateCapabilities(pos);
        level.updateNeighbourForOutputSignal(pos, upgraded.getBlock());
        if (!player.getAbilities().instabuild) stack.shrink(8);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.0F);
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Spills all face inventories when the block is removed or replaced with a different block, then lets
     * Minecraft remove the block entity. The loot table drops the storage block itself, while this method
     * drops its contents and updates comparators. Upgrades clear the old inventory first so preserved
     * items are not also dropped.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof SidedStorageBlockEntity storage) {
            var handler = storage.getItemHandler();
            SimpleContainer spill = new SimpleContainer(handler.getSlots());
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    spill.setItem(slot, stack.copy());
                }
            }
            Containers.dropContents(level, pos, spill);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Opens the storage menu on the physical face the player clicked and plays the opening sound. The
     * server sends the face, block position, and slots per face so the client constructs a matching menu.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SidedStorageBlockEntity storage) {
            storage.setActiveSide(hit.getDirection().ordinal());
            player.openMenu(storage, buffer -> {
                buffer.writeVarInt(storage.getActiveSide());
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(storage.getTier().getSlotsPerSide());
            });
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }
}
