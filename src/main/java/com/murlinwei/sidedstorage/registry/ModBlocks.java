package com.murlinwei.sidedstorage.registry;

import com.murlinwei.sidedstorage.SidedStorage;
import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.block.SidedStorageBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registration. */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SidedStorage.MODID);

    public static final DeferredBlock<SidedStorageBlock> BASIC = register(StorageTier.BASIC);
    public static final DeferredBlock<SidedStorageBlock> DENSE = register(StorageTier.DENSE);
    public static final DeferredBlock<SidedStorageBlock> CAPACITY = register(StorageTier.CAPACITY);

    /**
     * Prevents instances of this registry holder. The mod entrypoint registers its shared block registry
     * with NeoForge.
     */
    private ModBlocks() {}

    /**
     * Registers one Sided Storage tier with its own name and common metal block properties. Correct-tool
     * harvesting works together with the mod's pickaxe mining tag.
     */
    private static DeferredBlock<SidedStorageBlock> register(StorageTier tier) {
        return BLOCKS.registerBlock(
                tier.getBlockPath(),
                props -> new SidedStorageBlock(tier, props),
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(3.0F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops());
    }
}
