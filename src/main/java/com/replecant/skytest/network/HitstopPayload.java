package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Сервер → клиенты: цель удара «замирает» на несколько тиков (хитстоп).
 * combo — ступень комбо удара (0 — не удар игрока), parried — это было парирование.
 */
public record HitstopPayload(int targetId, int attackerId, int ticks, int combo, boolean parried) implements CustomPacketPayload {
    public static final Type<HitstopPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "hitstop"));
    public static final StreamCodec<ByteBuf, HitstopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HitstopPayload::targetId,
            ByteBufCodecs.VAR_INT, HitstopPayload::attackerId,
            ByteBufCodecs.VAR_INT, HitstopPayload::ticks,
            ByteBufCodecs.VAR_INT, HitstopPayload::combo,
            ByteBufCodecs.BOOL, HitstopPayload::parried,
            HitstopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
