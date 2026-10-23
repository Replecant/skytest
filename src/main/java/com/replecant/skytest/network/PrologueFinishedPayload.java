package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Клиент → сервер: пролог досмотрен. */
public record PrologueFinishedPayload() implements CustomPacketPayload {
    public static final PrologueFinishedPayload INSTANCE = new PrologueFinishedPayload();
    public static final Type<PrologueFinishedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "prologue_finished"));
    public static final StreamCodec<ByteBuf, PrologueFinishedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
