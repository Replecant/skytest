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

    /** Пока идёт сцена, игрок стоит у входа в зал — вне кадра. */
    private static final Vec3 STAGE = new Vec3(38.5, 57.0, 101.5);

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    /** Куда Регулятор поворачивает мост. */
    public enum Target { UPPER, LOWER }

    /** Пролёт, который в сцене строится или разбирается по шагам. */
    private interface Job {
        /** Сделать шаг; false — больше нечего. */
        boolean step();

        void finish();

        int length();

        Vec3 last();
    }

    private static final class Run {
        final RegulatorEntity regulator;
        final Vec3 anchor;
        final Target target;
        final Job build;
        final Job remove;
        final LowerPrison.UpperBridge upper;
        final boolean completesQuest;
        int t;
        float buildCredit;
        float removeCredit;

        Run(RegulatorEntity regulator, Vec3 anchor, Target target, Job build, Job remove,
                LowerPrison.UpperBridge upper, boolean completesQuest) {
            this.regulator = regulator;
            this.anchor = anchor;
            this.target = target;
            this.build = build;
            this.remove = remove;
            this.upper = upper;
            this.completesQuest = completesQuest;
        }
    }

    public static boolean isRunning(ServerPlayer player) {
        return RUNS.containsKey(player.getUUID());
    }

    /** Ларпи вернулась: мост от руин уходит к Верхнему ярусу, задание выполнено в конце сцены. */
    public static void start(ServerPlayer player, RegulatorEntity regulator) {
        start(player, regulator, Target.UPPER, true);
    }

    /**
     * Повернуть мост. К Верхнему ярусу: пролёт к камерам разбирается, к ярусу —
     * строится; к камерам — наоборот.
     */
    public static void start(ServerPlayer player, RegulatorEntity regulator, Target target, boolean completesQuest) {
        if (isRunning(player)) {
            return;
        }
        ServerLevel level = player.level();
        LowerPrison.UpperBridge upper = LowerPrison.startUpperBridge(level);
        LowerPrison.MainSpan main = LowerPrison.mainSpan(level);
        Job mainBuild = job(main::step, main::finish, main::length, main::lastColumn);
        Job mainRemove = job(main::unstep, () -> { }, main::length, main::lastColumn);
        Job upperBuild = upper == null ? null : job(upper::step, () -> { }, upper::length, upper::lastColumn);
        Job upperRemove = upper == null ? null
                : job(upper.fromEnd()::unstep, upper::finishRemoval, upper::length, upper::lastColumn);
        boolean toUpper = target == Target.UPPER;
        // Разбирать только то, что стоит, и строить только то, чего нет.
        Job build = toUpper ? (LowerPrison.upperBridgeBuilt(level) ? null : upperBuild)
                : (LowerPrison.mainSpanBuilt(level) ? null : mainBuild);
        Job remove = toUpper ? (LowerPrison.mainSpanBuilt(level) ? mainRemove : null)
                : (LowerPrison.upperBridgeBuilt(level) ? upperRemove : null);
        if (toUpper && build != null && upper != null) {
            // Для постройки — заново с начала, fromEnd() трогал общий счётчик.
            LowerPrison.UpperBridge fresh = LowerPrison.startUpperBridge(level);
            build = job(fresh::step, () -> { }, fresh::length, fresh::lastColumn);
            upper = fresh;
        }
        RUNS.put(player.getUUID(), new Run(regulator, player.position(), target, build, remove, upper, completesQuest));
        player.setInvisible(true);
        PacketDistributor.sendToPlayer(player, new CutscenePayload(ID));
        log("start to {}, build {} spans, remove {} spans", target,
                build == null ? 0 : build.length(), remove == null ? 0 : remove.length());
    }

    private static Job job(java.util.function.BooleanSupplier step, Runnable finish,
            java.util.function.IntSupplier length, java.util.function.Supplier<Vec3> last) {
        return new Job() {
            @Override
            public boolean step() {
                return step.getAsBoolean();
            }

            @Override
            public void finish() {
                finish.run();
            }

            @Override
            public int length() {
                return length.getAsInt();
            }

            @Override
            public Vec3 last() {
                return last.get();
            }
        };
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
        Vec3 hold = t < LENGTH - 16 ? STAGE : run.anchor;
        if (player.position().distanceToSqr(hold) > 0.25) {
            player.teleport(new TeleportTransition(level, hold, Vec3.ZERO, player.getYRot(), player.getXRot(),
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
        if (t >= BUILD_START && t <= BUILD_END) {
            run.buildCredit = progress(level, player, run.build, run.buildCredit, false);
            run.removeCredit = progress(level, player, run.remove, run.removeCredit, true);
        }
        if (t == GATE_OPEN) {
            finishBridge(run);
            if (run.upper != null) {
                Vec3 gate = run.upper.portal();
                sound(level, gate, run.target == Target.UPPER ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                        4.0F, 0.5F);
                sound(level, gate, SoundEvents.ANVIL_LAND, 3.0F, 0.6F);
            }
        }
        if (t == QUEST_DONE && run.completesQuest) {
            Quests.complete(player, Quests.FIND_LARPI);
        }
        if (t >= LENGTH) {
            player.setInvisible(false);
            log("done");
            return true;
        }
        return false;
    }

    /** Равномерно растянуть шаги работы на окно постройки; возвращает остаток «кредита» шагов. */
    private static float progress(ServerLevel level, ServerPlayer player, Job job, float credit, boolean removing) {
        if (job == null) {
            return 0.0F;
        }
        credit += (float) job.length() / (BUILD_END - BUILD_START + 1);
        while (credit >= 1.0F) {
            credit -= 1.0F;
            boolean more = job.step();
            Vec3 at = job.last();
            if (removing) {
                sound(level, at, SoundEvents.DEEPSLATE_BRICKS_BREAK, 4.0F, 0.5F + level.random.nextFloat() * 0.2F);
                if (level.random.nextInt(3) == 0) {
                    sound(level, at, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 3.0F, 0.6F);
                }
                particles(player, ParticleTypes.LARGE_SMOKE, at, 4, 0.8, 0.3, 0.8, 0.02);
            } else {
                sound(level, at, SoundEvents.DEEPSLATE_BRICKS_PLACE, 4.0F, 0.6F + level.random.nextFloat() * 0.2F);
                if (level.random.nextInt(3) == 0) {
                    sound(level, at, SoundEvents.GRINDSTONE_USE, 3.0F, 0.5F);
                }
                particles(player, ParticleTypes.LARGE_SMOKE, at.add(0, -1, 0), 2, 0.3, 0.2, 1.0, 0.01);
            }
            particles(player, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState()),
                    at, 14, 0.6, 0.4, 0.6, 0.1);
            if (!more) {
                break;
            }
        }
        return credit;
    }

    private static void finishBridge(Run run) {
        if (run.remove != null) {
            while (run.remove.step()) {
                // разобрать оставшееся
            }
            run.remove.finish();
        }
        if (run.build != null) {
            while (run.build.step()) {
                // доложить оставшиеся пролёты
            }
            run.build.finish();
        }
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
