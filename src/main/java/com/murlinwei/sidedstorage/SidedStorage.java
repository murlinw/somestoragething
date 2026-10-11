package com.murlinwei.sidedstorage;

import com.murlinwei.sidedstorage.network.SelectFacePayload;
import com.murlinwei.sidedstorage.registry.ModBlockEntities;
import com.murlinwei.sidedstorage.registry.ModBlocks;
import com.murlinwei.sidedstorage.registry.ModCreativeTabs;
import com.murlinwei.sidedstorage.registry.ModItems;
import com.murlinwei.sidedstorage.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Mod entrypoint.
 *
 * <p>Registration lives in small {@code Mod*} holder classes so that ordering stays obvious: blocks must exist
 * before the block entity type that lists them.
 */
@Mod(SidedStorage.MODID)
public class SidedStorage {

    public static final String MODID = "sidedstorage";

    /**
     * Initializes Sided Storage by attaching block, item, inventory, menu, and creative-tab registries to
     * the mod event bus. Also connects automation capability registration and the face-switch network
     * protocol.
     */
    public SidedStorage(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloads);
    }

    /**
     * Exposes each storage block entity through NeoForge item capabilities. Directional queries receive
     * only that face's partition; unsided queries receive no inventory, preserving the mod's
     * face-isolation rule.
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SIDED_STORAGE.get(),
                (be, side) -> be.getHandlerForSide(side));
    }

    /**
     * Registers the client-to-server face-switch payload with protocol version 2. This version accompanies
     * menu-opening data that includes the tier's slots per face.
     */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        SelectFacePayload.register(event.registrar("2"));
    }
}
