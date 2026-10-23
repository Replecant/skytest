package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Сервер → клиенты: цель удара «замирает» на несколько тиков (хитстоп). */
public record HitstopPayload(int targetId, int attackerId, int ticks) implements CustomPacketPayload {
    public static final Type<HitstopPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "hitstop"));
    public static final StreamCodec<ByteBuf, HitstopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HitstopPayload::targetId,
            ByteBufCodecs.VAR_INT, HitstopPayload::attackerId,
            ByteBufCodecs.VAR_INT, HitstopPayload::ticks,
            HitstopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
