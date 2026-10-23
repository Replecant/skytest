package com.replecant.skytest.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.entity.RegulatorEntity;
import com.replecant.skytest.network.CutscenePayload;
import com.replecant.skytest.world.LowerPrison;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Катсцена «Регулятор двигает мост». Сервер ведёт ту же раскадровку, что и
 * клиентская камера ({@code client.cutscene.CutscenePlayer}, сцена {@link #ID}):
 * <pre>
 *   0..100   зал: Регулятор встаёт (10), хватает цепи (30), два рывка (44, 66)
 *   105..215 снаружи: мост собирается пролёт за пролётом (115..200), решётка поднимается (208)
 *   225..240 камера возвращается к игроку, «Задание выполнено» (232)
 * </pre>
 * Игрок на это время заморожен на месте.
 */
public final class RegulatorCutscene {
    public static final String ID = "regulator_bridge";
    public static final int PULL_START = 10;
    public static final int[] IMPACTS = {PULL_START + 34, PULL_START + 56};
    public static final int PULL_END = 100;
    public static final int BUILD_START = 115;
    public static final int BUILD_END = 200;
    public static final int GATE_OPEN = 208;
    public static final int QUEST_DONE = 232;
    public static final int LENGTH = 240;

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    private static final class Run {
        final RegulatorEntity regulator;
        final Vec3 anchor;
        final LowerPrison.UpperBridge bridge;
        int t;
        float stepCredit;

        Run(RegulatorEntity regulator, Vec3 anchor, LowerPrison.UpperBridge bridge) {
            this.regulator = regulator;
            this.anchor = anchor;
            this.bridge = bridge;
        }
    }

    public static boolean isRunning(ServerPlayer player) {
        return RUNS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, RegulatorEntity regulator) {
        if (isRunning(player)) {
            return;
        }
        LowerPrison.UpperBridge bridge = LowerPrison.startUpperBridge(player.level());
        RUNS.put(player.getUUID(), new Run(regulator, player.position(), bridge));
        PacketDistributor.sendToPlayer(player, new CutscenePayload(ID));
        log("start, bridge spans {}", bridge == null ? -1 : bridge.length());
    }

    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Run> entry = it.next();
            Run run = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                // Игрок вышел посреди сцены — доделать всё сразу.
                finishBridge(run);
                run.regulator.setPulling(false);
                it.remove();
                continue;
            }
            if (tick(player, run)) {
                it.remove();
            }
        }
    }

    /** true — сцена окончена. */
    private static boolean tick(ServerPlayer player, Run run) {
        ServerLevel level = player.level();
        int t = ++run.t;
        if (player.position().distanceToSqr(run.anchor) > 0.25) {
            player.teleport(new TeleportTransition(level, run.anchor, Vec3.ZERO, player.getYRot(), player.getXRot(),
                    TeleportTransition.DO_NOTHING));
        }
        RegulatorEntity regulator = run.regulator;
        Vec3 r = regulator.position();
        if (t == PULL_START) {
            regulator.setPulling(true);
            sound(level, r, SoundEvents.RAVAGER_ROAR, 1.2F, 0.45F);
        } else if (t == PULL_START + 20) {
            sound(level, r.add(0, 9, 0), SoundEvents.ARMOR_EQUIP_CHAIN.value(), 2.0F, 0.5F);
        }
        for (int impact : IMPACTS) {
            if (t == impact) {
                boolean second = impact == IMPACTS[1];
                sound(level, r, SoundEvents.ANVIL_LAND, 2.0F, second ? 0.4F : 0.55F);
                sound(level, r, SoundEvents.PISTON_EXTEND, 2.0F, 0.5F);
                sound(level, r.add(0, 12, 0), SoundEvents.ARMOR_EQUIP_CHAIN.value(), 3.0F, 0.6F);
                // С потолка сыплется пыль.
                particles(player, ParticleTypes.ASH, r.add(0, 14, 0), 80, 6.0, 2.0, 6.0, 0.02);
                particles(player, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DEEPSLATE.defaultBlockState()),
                        r.add(0, 15, 0), 40, 7.0, 1.0, 7.0, 0.0);
            }
        }
        if (t == PULL_END) {
            regulator.setPulling(false);
        }
        if (run.bridge != null && t >= BUILD_START && t <= BUILD_END) {
            run.stepCredit += (float) run.bridge.length() / (BUILD_END - BUILD_START + 1);
            while (run.stepCredit >= 1.0F) {
                run.stepCredit -= 1.0F;
                boolean more = run.bridge.step();
                Vec3 at = run.bridge.lastColumn();
                sound(level, at, SoundEvents.DEEPSLATE_BRICKS_PLACE, 4.0F, 0.6F + level.random.nextFloat() * 0.2F);
                if (level.random.nextInt(3) == 0) {
                    sound(level, at, SoundEvents.GRINDSTONE_USE, 3.0F, 0.5F);
                }
                particles(player, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState()),
                        at, 14, 0.4, 0.4, 1.4, 0.1);
                particles(player, ParticleTypes.LARGE_SMOKE, at.add(0, -1, 0), 2, 0.3, 0.2, 1.0, 0.01);
                if (!more) {
                    break;
                }
            }
        }
        if (t == GATE_OPEN) {
            finishBridge(run);
            if (run.bridge != null) {
                Vec3 gate = run.bridge.portal();
                sound(level, gate, SoundEvents.IRON_DOOR_OPEN, 4.0F, 0.5F);
                sound(level, gate, SoundEvents.ANVIL_LAND, 3.0F, 0.6F);
            }
        }
        if (t == QUEST_DONE) {
            Quests.complete(player, Quests.FIND_LARPI);
        }
        if (t >= LENGTH) {
            log("done");
            return true;
        }
        return false;
    }

    private static void finishBridge(Run run) {
        if (run.bridge == null) {
            return;
        }
        while (run.bridge.step()) {
            // доложить оставшиеся пролёты
        }
        run.bridge.finish();
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch);
    }

    /** Частицы видны издалека: камера в сцене далеко от самого игрока. */
    private static void particles(ServerPlayer player, ParticleOptions particle, Vec3 at, int count,
            double dx, double dy, double dz, double speed) {
        player.level().sendParticles(player, particle, true, true, at.x, at.y, at.z, count, dx, dy, dz, speed);
    }

    private static void log(String message, Object... args) {
        if (SmokeTour.ENABLED) {
            SkyTest.LOGGER.info("SKYTEST CUTSCENE " + message, args);
        }
    }

    private RegulatorCutscene() {}
}
