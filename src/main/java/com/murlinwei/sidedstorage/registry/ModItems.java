package com.murlinwei.sidedstorage.registry;

import com.murlinwei.sidedstorage.SidedStorage;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registration. The blocks are simple enough that the vanilla-style block items are all we need. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SidedStorage.MODID);

    public static final DeferredItem<BlockItem> BASIC = ITEMS.registerSimpleBlockItem(
            ModBlocks.BASIC, new Item.Properties());
    public static final DeferredItem<BlockItem> DENSE = ITEMS.registerSimpleBlockItem(
            ModBlocks.DENSE, new Item.Properties());
    public static final DeferredItem<BlockItem> CAPACITY = ITEMS.registerSimpleBlockItem(
            ModBlocks.CAPACITY, new Item.Properties());

    private ModItems() {}
}