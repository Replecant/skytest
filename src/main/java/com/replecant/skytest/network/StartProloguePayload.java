package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Сервер → клиент: показать пролог. */
public record StartProloguePayload() implements CustomPacketPayload {
    public static final StartProloguePayload INSTANCE = new StartProloguePayload();
    public static final Type<StartProloguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "start_prologue"));
    public static final StreamCodec<ByteBuf, StartProloguePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
