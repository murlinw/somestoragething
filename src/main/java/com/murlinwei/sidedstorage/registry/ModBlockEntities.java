package com.murlinwei.sidedstorage.registry;

import com.murlinwei.sidedstorage.SidedStorage;
import com.murlinwei.sidedstorage.block.SidedStorageBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity type registration. All three tiers share a single type. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SidedStorage.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SidedStorageBlockEntity>> SIDED_STORAGE =
            BLOCK_ENTITIES.register("sided_storage", () -> BlockEntityType.Builder.of(
                    SidedStorageBlockEntity::new,
                    ModBlocks.BASIC.get(),
                    ModBlocks.DENSE.get(),
                    ModBlocks.CAPACITY.get())
                    .build(null));

    private ModBlockEntities() {}
}