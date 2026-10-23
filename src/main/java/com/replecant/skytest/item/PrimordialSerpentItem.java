package com.replecant.skytest.item;

import com.replecant.skytest.entity.SerpentEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Первородный змей: обвивает руку, голова лежит на тыльной стороне ладони.
 * ПКМ — за заряд душ змей бросает вперёд своё порождение, которое кусает
 * и отравляет врага.
 */
public class PrimordialSerpentItem extends SoulCasterItem {
    public static final int COST = 2;

    public PrimordialSerpentItem(Properties properties) {
        super(properties, COST, 18);
    }

    @Override
    protected void cast(ServerLevel level, Player player, InteractionHand hand) {
        Vec3 from = palm(player, hand);
        SerpentEntity serpent = new SerpentEntity(level, player, from);
        Vec3 look = player.getLookAngle();
        serpent.shoot(look.x, look.y, look.z, 1.15F, 0.5F);
        level.addFreshEntity(serpent);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LLAMA_SPIT,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SILVERFISH_AMBIENT,
                SoundSource.PLAYERS, 0.9F, 0.5F);
    }
}
