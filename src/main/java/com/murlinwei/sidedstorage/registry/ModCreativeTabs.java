package com.murlinwei.sidedstorage.registry;

import com.murlinwei.sidedstorage.SidedStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative mode tab, so the blocks are reachable outside of commands in a dev build. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SidedStorage.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STORAGE = TABS.register("storage", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sidedstorage"))
                    .icon(() -> ModItems.BASIC.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BASIC.get());
                        output.accept(ModItems.DENSE.get());
                        output.accept(ModItems.CAPACITY.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}