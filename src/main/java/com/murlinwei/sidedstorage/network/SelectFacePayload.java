package com.murlinwei.sidedstorage.network;

import com.murlinwei.sidedstorage.SidedStorage;
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registers this payload. Called from the mod constructor. */
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, SelectFacePayload::handle);
    }

    private static void handle(SelectFacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
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
            });
        });
    }
}