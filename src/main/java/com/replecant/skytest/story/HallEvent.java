package com.replecant.skytest.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.StrangerEntity;
import com.replecant.skytest.registry.ModAttachments;
import com.replecant.skytest.world.LowerPrison;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.phys.Vec3;

/**
 * Событие в зале: Незнакомец заговаривает с вошедшим, бросает меч и убегает,
 * а затем из спуска вниз поднимается скелет.
 */
public final class HallEvent {
    public static final String MET_STRANGER = "met_stranger";
    /** Метка скелетов-узников: с них ничего не выпадает. */
    public static final String PRISONER_TAG = SkyTest.MODID + "_prisoner";
    private static final int SKELETON_DELAY = 50;

    private static final Map<UUID, Integer> PENDING_SKELETONS = new HashMap<>();

    /** Каждые несколько тиков: не вошёл ли игрок в зал. */
    static void checkTrigger(ServerPlayer player) {
        if (hasFlag(player, MET_STRANGER) || !LowerPrison.HALL.contains(player.position())) {
            return;
        }
        player.level().getEntitiesOfClass(StrangerEntity.class, player.getBoundingBox().inflate(32.0),
                        stranger -> !stranger.isLeaving())
                .stream().findFirst()
                .ifPresent(stranger -> meet(player, stranger));
    }

    public static void meet(ServerPlayer player, StrangerEntity stranger) {
        setFlag(player, MET_STRANGER);
        stranger.talkTo(player);
    }

    public static void onStrangerLeft(ServerPlayer player) {
        PENDING_SKELETONS.put(player.getUUID(), 0);
    }

    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> it = PENDING_SKELETONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);
            if (ticks >= SKELETON_DELAY) {
                spawnSkeleton(player);
                it.remove();
            }
        }
    }

    private static void spawnSkeleton(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Skeleton skeleton = EntityType.SKELETON.create(level, EntitySpawnReason.EVENT);
        if (skeleton == null) {
            return;
        }
        Vec3 pos = LowerPrison.DESCENT_BOTTOM;
        // Без finalizeSpawn скелет не получает лук и дерётся вблизи.
        skeleton.snapTo(pos.x, pos.y, pos.z, 180.0F, 0.0F);
        skeleton.setPersistenceRequired();
        skeleton.addTag(PRISONER_TAG);
        skeleton.setTarget(player);
        level.addFreshEntity(skeleton);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.7F);
        SmokeTour.onSkeletonSpawned(player, skeleton);
    }

    public static boolean hasFlag(ServerPlayer player, String flag) {
        return player.getData(ModAttachments.STORY_FLAGS).contains(flag);
    }

    private static void setFlag(ServerPlayer player, String flag) {
        java.util.Set<String> flags = new java.util.HashSet<>(player.getData(ModAttachments.STORY_FLAGS));
        flags.add(flag);
        player.setData(ModAttachments.STORY_FLAGS, flags);
    }

    private HallEvent() {}
}
