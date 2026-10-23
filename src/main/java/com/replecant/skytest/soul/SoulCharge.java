package com.replecant.skytest.soul;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.PrisonWardenEntity;
import com.replecant.skytest.entity.RatEntity;
import com.replecant.skytest.entity.SoulOrbEntity;
import com.replecant.skytest.registry.ModAttachments;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * Заряд душ вместо опыта. Поверженные существа отпускают души — мелкие
 * фиолетовые искры, которые сами летят к игроку. Ванильного опыта в мире нет.
 * Пока заряд только копится; позже на него будут работать магия и артефакты.
 */
@EventBusSubscriber(modid = SkyTest.MODID)
public final class SoulCharge {
    public static final int MAX = 100;

    /** Сколько душ отпускает существо. */
    public static int soulsOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }
        if (entity instanceof RatEntity) {
            return 1;
        }
        if (entity instanceof PrisonWardenEntity) {
            return 12;
        }
        if (entity instanceof Enemy) {
            return Math.max(2, Math.round(entity.getMaxHealth() / 7.0F));
        }
        return 1;
    }

    public static int get(Player player) {
        return player.getData(ModAttachments.SOUL_CHARGE);
    }

    public static void set(Player player, int charge) {
        player.setData(ModAttachments.SOUL_CHARGE, Math.clamp(charge, 0, MAX));
    }

    /** Потратить заряд (для будущей магии). false — не хватает. */
    public static boolean spend(Player player, int amount) {
        int charge = get(player);
        if (charge < amount) {
            return false;
        }
        set(player, charge - amount);
        return true;
    }

    /** Душа долетела до игрока. */
    public static void absorb(ServerPlayer player, int value, Vec3 at) {
        set(player, get(player) + value);
        ServerLevel level = player.level();
        level.sendParticles(new DustParticleOptions(SoulOrbEntity.CORE_COLOR, 0.6F),
                at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.5F, 1.3F + level.random.nextFloat() * 0.4F);
        if (com.replecant.skytest.story.SmokeTour.ENABLED) {
            SkyTest.LOGGER.info("SKYTEST SOUL +{} -> {}", value, get(player));
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || event.isCanceled()) {
            return;
        }
        int souls = soulsOf(entity);
        if (souls <= 0) {
            return;
        }
        // Не больше шести сгустков: крупная добыча — более «тяжёлые» души.
        int orbs = Math.min(souls, 6);
        Vec3 at = entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
        for (int i = 0; i < orbs; i++) {
            int value = souls / orbs + (i < souls % orbs ? 1 : 0);
            SoulOrbEntity.spawn(level, at, value);
        }
    }

    /** Ванильного опыта больше нет: ни с мобов, ни откуда-либо ещё. */
    @SubscribeEvent
    static void onExperienceDrop(LivingExperienceDropEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCanceled(true);
        }
    }

    private SoulCharge() {}
}
