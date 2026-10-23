package com.replecant.skytest.network;

import java.util.List;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Сервер → клиент: открыть диалоговое окно с репликами собеседника. */
public record OpenDialoguePayload(int speakerId, String speakerName, List<String> lines) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "open_dialogue"));
    public static final StreamCodec<ByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenDialoguePayload::speakerId,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::speakerName,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)), OpenDialoguePayload::lines,
            OpenDialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
