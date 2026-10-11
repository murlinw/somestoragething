package com.murlinwei.sidedstorage.network;

import com.murlinwei.sidedstorage.SidedStorage;
import com.murlinwei.sidedstorage.StorageTier;
import com.murlinwei.sidedstorage.menu.SidedStorageMenu;
import com.murlinwei.sidedstorage.block.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Asks the server to reopen a Sided Storage menu on a different face.
 *
 * <p>Tab switching has to round-trip through the server: the menu is rebuilt with only the newly selected face's
 * slots, and that has to be agreed on by both sides.
 */
public record SelectFacePayload(BlockPos pos, int face) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectFacePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SidedStorage.MODID, "select_face"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectFacePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SelectFacePayload::pos,
            ByteBufCodecs.VAR_INT, SelectFacePayload::face,
            SelectFacePayload::new);

    /**
     * Identifies this message as Sided Storage's select_face payload so NeoForge can encode and route
     * face-switch requests.
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Registers the face-switch codec and server handler with NeoForge networking. Only client-to-server
     * requests are accepted for this payload.
     */
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, SelectFacePayload::handle);
    }

    /**
     * Processes a requested tab switch on the game thread. Requires a valid face, the matching open
     * storage menu, its original block entity, a loaded chunk, and a nearby player before reopening the
     * menu. Sends the tier size with the new face so both sides agree on slot indices; ordinary menu
     * cleanup safely returns cursor items.
     */
    private static void handle(SelectFacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (payload.face() < 0 || payload.face() >= StorageTier.SIDES
                    || !(player.containerMenu instanceof SidedStorageMenu menu)
                    || !menu.getBlockPos().equals(pos) || !menu.stillValid(player)
                    || !player.level().hasChunkAt(pos)) {
                return;
            }
            // Sanity check: the player must be near the block they claim to be looking at.
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) {
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof SidedStorageBlockEntity storage)) {
                return;
            }

            // No explicit close here: openMenu silently swaps the server menu
            // (the provider opts out of client-side closing), so the client
            // replaces the screen directly with no flash and no mouse reset.
            storage.setActiveSide(payload.face());
            player.openMenu(storage, buffer -> {
                buffer.writeVarInt(storage.getActiveSide());
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(storage.getTier().getSlotsPerSide());
            });
        });
    }
}
