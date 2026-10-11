package com.murlinwei.sidedstorage.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murlinwei.sidedstorage.StorageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    public SidedStorageBlock(StorageTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public StorageTier getTier() {
        return this.tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SidedStorageBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // This block has no block state properties.
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Spills the stored items when the block is broken or replaced. Mirrors the vanilla container pattern
     * ({@code ChestBlock}/{@code BarrelBlock}): the loot table drops the block itself, this drops its contents.
     * Only runs server-side — {@code LevelChunk} never calls {@code onRemove} on the client.
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
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SidedStorageBlockEntity storage) {
            player.openMenu(storage, buffer -> {
                buffer.writeVarInt(storage.getActiveSide());
                buffer.writeBlockPos(pos);
            });
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }
}