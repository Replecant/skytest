package com.replecant.skytest.registry;

import com.mojang.serialization.Codec;
import com.replecant.skytest.SkyTest;

import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SkyTest.MODID);

    /** Пролог уже показан этому игроку. Переживает смерть. */
    public static final Supplier<AttachmentType<Boolean>> PROLOGUE_SEEN = ATTACHMENT_TYPES.register("prologue_seen",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("seen"))
                    .copyOnDeath()
                    .build());

    /** Сколько разговоров с Фонарщиком уже состоялось. */
    public static final Supplier<AttachmentType<Integer>> LAMPLIGHTER_STAGE = ATTACHMENT_TYPES.register("lamplighter_stage",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT.fieldOf("stage"))
                    .copyOnDeath()
                    .build());

    /** Сюжетные флаги игрока (встретил Незнакомца и т. п.). */
    public static final Supplier<AttachmentType<Set<String>>> STORY_FLAGS = ATTACHMENT_TYPES.register("story_flags",
            () -> AttachmentType.<Set<String>>builder(() -> new HashSet<>())
                    .serialize(Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, ArrayList::new).fieldOf("flags"))
                    .copyOnDeath()
                    .build());

    /** Активное задание игрока (id или пустая строка). Синхронизируется клиенту для трекера. */
    public static final Supplier<AttachmentType<String>> ACTIVE_QUEST = ATTACHMENT_TYPES.register("active_quest",
            () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("quest"))
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
