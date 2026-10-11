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
     * The client-side copy of the menu is rebuilt from the block position the server writes when the menu is opened,
     * so it can find the same block entity and size its slot list identically.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<SidedStorageMenu>> SIDED_STORAGE =
            MENUS.register("sided_storage", () -> IMenuTypeExtension.<SidedStorageMenu>create(
                    (id, inventory, buffer) -> new SidedStorageMenu(id, inventory, buffer)));

    private ModMenus() {}
}