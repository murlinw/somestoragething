package com.murlinwei.sidedstorage.gametest;

import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.block.SidedStorageBlockEntity;
import com.murlinwei.sidedstorage.menu.SidedStorageMenu;
import com.murlinwei.sidedstorage.network.SelectFacePayload;
import com.murlinwei.sidedstorage.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

@GameTestHolder("sidedstorage")
@PrefixGameTestTemplate(false)
public class StorageTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest(template = "empty")
    public static void partitionsAndPersistence(GameTestHelper helper) {
        for (Block block : new Block[]{ModBlocks.BASIC.get(), ModBlocks.DENSE.get(), ModBlocks.CAPACITY.get()}) {
            helper.setBlock(POS, block);
            SidedStorageBlockEntity storage = helper.getBlockEntity(POS);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(POS), null) == null, "Unsided automation exposed inventory");
            for (Direction face : Direction.values()) {
                var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                        helper.absolutePos(POS), face);
                helper.assertTrue(handler != null && handler.getSlots() == storage.getTier().getSlotsPerSide(), "Wrong face size");
                handler.insertItem(handler.getSlots() - 1, new ItemStack(Items.APPLE, face.ordinal() + 1), false);
            }
            var restored = (SidedStorageBlockEntity) BlockEntity.loadStatic(storage.getBlockPos(),
                    storage.getBlockState(), storage.saveWithFullMetadata(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            for (Direction face : Direction.values()) {
                var handler = restored.getHandlerForSide(face);
                helper.assertTrue(handler.getStackInSlot(handler.getSlots() - 1).getCount() == face.ordinal() + 1,
                        "Save/reload lost or merged face " + face);
                helper.assertTrue(handler.getStackInSlot(0).isEmpty(), "Face ranges overlap");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void miningAndComparator(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Block block : new Block[]{ModBlocks.BASIC.get(), ModBlocks.DENSE.get(), ModBlocks.CAPACITY.get()}) {
            helper.setBlock(POS, block);
            var state = helper.getBlockState(POS);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_PICKAXE));
            helper.assertTrue(player.hasCorrectToolForDrops(state, helper.getLevel(), helper.absolutePos(POS)), "Pickaxe cannot harvest");
            helper.assertTrue(Block.getDrops(state, helper.getLevel(), helper.absolutePos(POS), helper.getBlockEntity(POS),
                    player, player.getMainHandItem()).stream().anyMatch(stack -> stack.is(block.asItem())), "Missing block loot");
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            helper.assertTrue(!player.hasCorrectToolForDrops(state, helper.getLevel(), helper.absolutePos(POS)), "Bare hands can harvest");
            SidedStorageBlockEntity storage = helper.getBlockEntity(POS);
            helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS)) == 0, "Empty comparator output");
            for (int i = 0; i < storage.getItemHandler().getSlots(); i++) storage.getItemHandler().setStackInSlot(i, new ItemStack(Items.APPLE, 64));
            helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS)) == 15, "Full comparator output");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradesPreserveFacesAndCost(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BASIC.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        SidedStorageBlockEntity storage = helper.getBlockEntity(POS);
        for (Direction face : Direction.values()) storage.getHandlerForSide(face).insertItem(8, new ItemStack(Items.APPLE, face.ordinal() + 1), false);
        for (var material : new net.minecraft.world.item.Item[]{Items.GOLD_INGOT, Items.DIAMOND}) {
            var state = helper.getBlockState(POS);
            var hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(POS)), Direction.NORTH, helper.absolutePos(POS), false);
            ItemStack insufficient = new ItemStack(material, 7);
            helper.assertTrue(state.useItemOn(insufficient, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                    == ItemInteractionResult.FAIL, "Underpaid upgrade succeeded");
            ItemStack cost = new ItemStack(material, 8);
            helper.assertTrue(state.useItemOn(cost, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                    == ItemInteractionResult.CONSUME && cost.isEmpty(), "Upgrade cost not consumed");
            SidedStorageBlockEntity upgraded = helper.getBlockEntity(POS);
            helper.assertTrue(upgraded != storage, "Old block entity reused");
            for (Direction face : Direction.values()) {
                var handler = upgraded.getHandlerForSide(face);
                helper.assertTrue(handler.getStackInSlot(8).getCount() == face.ordinal() + 1, "Upgrade moved/lost face contents");
                helper.assertTrue(handler.getStackInSlot(9).isEmpty(), "New slots are not empty");
            }
            storage = upgraded;
        }
        helper.assertTrue(storage.getTier() == StorageTier.CAPACITY, "Upgrade chain incomplete");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hopperOnlyExtractsBottomFace(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BASIC.get());
        helper.setBlock(POS.below(), Blocks.HOPPER);
        SidedStorageBlockEntity storage = helper.getBlockEntity(POS);
        storage.getHandlerForSide(Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND), false);
        storage.getHandlerForSide(Direction.DOWN).insertItem(0, new ItemStack(Items.APPLE), false);
        helper.runAfterDelay(25, () -> {
            HopperBlockEntity hopper = helper.getBlockEntity(POS.below());
            helper.assertTrue(hopper.getItem(0).is(Items.APPLE), "Hopper did not extract bottom face");
            helper.assertTrue(storage.getHandlerForSide(Direction.UP).getStackInSlot(0).is(Items.DIAMOND), "Hopper extracted another face");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void menusStayIndependentAndRejectReplacement(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BASIC.get());
        var first = helper.makeMockPlayer(GameType.SURVIVAL);
        var second = helper.makeMockPlayer(GameType.SURVIVAL);
        first.setPos(Vec3.atCenterOf(helper.absolutePos(POS)));
        second.setPos(first.position());
        SidedStorageBlockEntity storage = helper.getBlockEntity(POS);
        var firstMenu = new SidedStorageMenu(1, first.getInventory(), storage, Direction.UP.ordinal());
        var secondMenu = new SidedStorageMenu(1, second.getInventory(), storage, Direction.DOWN.ordinal());
        storage.setActiveSide(Direction.EAST.ordinal());
        helper.assertTrue(firstMenu.getActiveSide() == Direction.UP.ordinal()
                && secondMenu.getActiveSide() == Direction.DOWN.ordinal(), "Player tabs interfere");
        helper.assertTrue(firstMenu.stillValid(first), "Original menu invalid");
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, ModBlocks.BASIC.get());
        helper.assertTrue(!firstMenu.stillValid(first), "Old menu accepted replacement inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabPacketsValidateAndReturnCursorItems(GameTestHelper helper) throws Exception {
        helper.setBlock(POS, ModBlocks.CAPACITY.get());
        var player = helper.makeMockServerPlayerInLevel();
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
        try {
            BlockPos absolute = helper.absolutePos(POS);
            player.setPos(Vec3.atCenterOf(absolute));
            var handle = SelectFacePayload.class.getDeclaredMethod("handle", SelectFacePayload.class, IPayloadContext.class);
            handle.setAccessible(true);
            IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                    new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                        if (method.getName().equals("player")) return player;
                        if (method.getName().equals("enqueueWork")) {
                            ((Runnable) args[0]).run();
                            return CompletableFuture.completedFuture(null);
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            var closedMenu = player.containerMenu;
            handle.invoke(null, new SelectFacePayload(absolute, 0), context);
            helper.assertTrue(player.containerMenu == closedMenu, "Packet opened storage without an existing menu");
            for (Direction face : Direction.values()) {
                helper.getBlockState(POS).useWithoutItem(helper.getLevel(), player,
                        new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false));
                helper.assertTrue(((SidedStorageMenu) player.containerMenu).getActiveSide() == face.ordinal(), "Clicked face did not open");
            }
            var openMenu = player.containerMenu;
            handle.invoke(null, new SelectFacePayload(absolute, -1), context);
            handle.invoke(null, new SelectFacePayload(absolute, 6), context);
            handle.invoke(null, new SelectFacePayload(absolute.east(), 0), context);
            helper.assertTrue(player.containerMenu == openMenu, "Invalid packet replaced menu");
            player.setPos(Vec3.atCenterOf(absolute.offset(20, 0, 0)));
            handle.invoke(null, new SelectFacePayload(absolute, 0), context);
            helper.assertTrue(player.containerMenu == openMenu, "Out-of-reach packet accepted");
            player.setPos(Vec3.atCenterOf(absolute));
            openMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
            int before = player.getInventory().countItem(Items.DIAMOND);
            handle.invoke(null, new SelectFacePayload(absolute, Direction.NORTH.ordinal()), context);
            helper.assertTrue(((SidedStorageMenu) player.containerMenu).getActiveSide() == Direction.NORTH.ordinal(), "Valid tab switch rejected");
            helper.assertTrue(player.containerMenu.getCarried().isEmpty()
                    && player.getInventory().countItem(Items.DIAMOND) == before + 3, "Cursor items lost or duplicated on tab switch");
            helper.assertTrue(((SidedStorageMenu) player.containerMenu).getSlotsPerSide() == 27, "Wrong tier after switch");
        } finally {
            player.closeContainer();
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
