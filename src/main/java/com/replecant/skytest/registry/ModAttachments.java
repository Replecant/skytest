package com.replecant.skytest.registry;

import com.mojang.serialization.Codec;
import com.replecant.skytest.SkyTest;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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

    private ModAttachments() {}
}
