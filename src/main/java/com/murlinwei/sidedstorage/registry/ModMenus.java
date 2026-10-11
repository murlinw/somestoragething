package com.murlinwei.sidedstorage.registry;

import com.murlinwei.sidedstorage.SidedStorage;
import com.murlinwei.sidedstorage.menu.SidedStorageMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu registration. */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SidedStorage.MODID);

    /**
     * The factory rebuilds the client menu from the face, block position, and slots per face sent by the server.
     * Its temporary inventory receives synchronized stacks without relying on a client-side block entity.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<SidedStorageMenu>> SIDED_STORAGE =
            MENUS.register("sided_storage", () -> IMenuTypeExtension.<SidedStorageMenu>create(
                    (id, inventory, buffer) -> new SidedStorageMenu(id, inventory, buffer)));

    /**
     * Prevents instances of this registry holder. Its static menu registration connects server-side
     * storage views to the client menu factory.
     */
    private ModMenus() {}
}
