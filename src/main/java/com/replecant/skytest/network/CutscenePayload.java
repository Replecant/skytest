package com.replecant.skytest.network;

import com.replecant.skytest.SkyTest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Сервер → клиент: проиграть катсцену с данным id. */
public record CutscenePayload(String id) implements CustomPacketPayload {
    public static final Type<CutscenePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SkyTest.MODID, "cutscene"));
    public static final StreamCodec<ByteBuf, CutscenePayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(CutscenePayload::new, CutscenePayload::id);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
