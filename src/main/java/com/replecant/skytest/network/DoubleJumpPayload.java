package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Клиент сообщает серверу, что сделал двойной прыжок. */
public record DoubleJumpPayload() implements CustomPacketPayload {
    public static final DoubleJumpPayload INSTANCE = new DoubleJumpPayload();
    public static final Type<DoubleJumpPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "double_jump"));
    public static final StreamCodec<ByteBuf, DoubleJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
