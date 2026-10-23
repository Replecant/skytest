package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Клиент → сервер: игрок закрыл диалог с собеседником. */
public record DialogueClosedPayload(int speakerId) implements CustomPacketPayload {
    public static final Type<DialogueClosedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "dialogue_closed"));
    public static final StreamCodec<ByteBuf, DialogueClosedPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DialogueClosedPayload::new, DialogueClosedPayload::speakerId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
