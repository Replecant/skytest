package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Клиент → сервер: игрок сделал рывок (сервер даёт кадры неуязвимости). */
public record DashPayload() implements CustomPacketPayload {
    public static final DashPayload INSTANCE = new DashPayload();
    public static final Type<DashPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "dash"));
    public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
