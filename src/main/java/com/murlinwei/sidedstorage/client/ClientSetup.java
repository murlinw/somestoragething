package com.murlinwei.sidedstorage.client;

import com.murlinwei.sidedstorage.SidedStorage;
import com.murlinwei.sidedstorage.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only setup. */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = SidedStorage.MODID)
public final class ClientSetup {

    /**
     * Prevents instances of this client-only event subscriber. Minecraft invokes its static registration
     * method during client setup.
     */
    private ClientSetup() {}

    /**
     * Associates the registered storage menu type with SidedStorageScreen on the client. Dedicated servers
     * do not load this screen registration.
     */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SIDED_STORAGE.get(), SidedStorageScreen::new);
    }
}
