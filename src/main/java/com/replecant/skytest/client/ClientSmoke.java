package com.replecant.skytest.client;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Визуальная проверка на CI (SKYTEST_CLIENT_SMOKE=true): снимает скриншоты
 * пролога, диалогов, тура по локации и события в зале, затем закрывает игру.
 * Тур ведёт сервер ({@link com.replecant.skytest.story.SmokeTour}).
 */
public final class ClientSmoke {
    public static final boolean ENABLED = "true".equals(System.getenv("SKYTEST_CLIENT_SMOKE"));

    private static final int[] WAKE_SHOTS = {25, 120, 630, 750};
    private static final String[] WAKE_NAMES = {"04_waking", "05_cell_awake", "07_corridor_gates", "08_gate_closed"};

    private static int clientTicks;
    private static int wakeTicks = -1;
    private static int dialogues;
    private static int strangerOpened = -1;
    private static int strangerClosed = -1;

    static void onPrologueTick(int ticks) {
        if (!ENABLED) {
            return;
        }
        switch (ticks) {
            case 45 -> shot("01_prologue_beam");
            case 255 -> shot("03_prologue_text_full");
            default -> { }
        }
    }

    static void onWake() {
        if (ENABLED) {
            wakeTicks = 0;
        }
    }

    static void onDialogueOpened() {
        if (ENABLED) {
            dialogues++;
            if (dialogues == 2) {
                strangerOpened = clientTicks;
            }
        }
    }

    static void onDialogueClosed() {
        if (ENABLED && dialogues == 2 && strangerClosed < 0) {
            strangerClosed = clientTicks;
        }
    }

    static void tick(Minecraft mc) {
        if (!ENABLED) {
            return;
        }
        clientTicks++;
        if (clientTicks % 200 == 0) {
            SkyTest.LOGGER.info("SKYTEST DEBUG t={} screen={} dialogues={}", clientTicks,
                    mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(), dialogues);
        }
        if (clientTicks == 9000) {
            SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE TIMEOUT");
            mc.stop();
        }
        if (wakeTicks >= 0) {
            wakeTicks++;
            for (int i = 0; i < WAKE_SHOTS.length; i++) {
                if (wakeTicks == WAKE_SHOTS[i]) {
                    shot(WAKE_NAMES[i]);
                }
            }
            if (wakeTicks == 706) {
                shot("08c_rat");
            }
            if (wakeTicks == 200) {
                shot("06_lamplighter_dialogue");
            }
            movementTest(mc);
        }
        if (strangerOpened >= 0) {
            int t = clientTicks - strangerOpened;
            if (t == 30) {
                shot("09_stranger_dialogue");
            } else if (t == 80 && mc.player != null) {
                // Ночное зрение только на клиенте — чтобы рассмотреть модель.
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            } else if (t == 100) {
                shot("09b_stranger_nightvision");
            } else if (t == 110 && mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            } else if (t == 220) {
                shot("09c_stranger_dialogue_late");
            }
        }
        if (strangerClosed >= 0) {
            int t = clientTicks - strangerClosed;
            switch (t) {
                case 6 -> shot("10_sword_thrown");
                case 25 -> shot("11_stranger_leaving");
                case 75 -> shot("12_skeleton");
                case 265 -> shot("15_bridge");
                case 305 -> shot("16_prison_from_bridge");
                case 345 -> shot("17_abyss");
                case 362 -> shot("18_warden_a");
                case 378 -> shot("18_warden_b");
                case 394 -> shot("18_warden_c");
                case 410 -> shot("18_warden_d");
                case 426 -> shot("18_warden_e");
                case 460 -> {
                    SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE DONE");
                    mc.stop();
                }
                default -> { }
            }
            if (t >= 80) {
                fightTest(mc);
            }
        }
    }

    /** Разгон с места бегом и торможение: скорость по тикам в лог. */
    private static void movementTest(Minecraft mc) {
        if (mc.player == null || wakeTicks < 640 || wakeTicks > 690) {
            return;
        }
        if (wakeTicks == 640) {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        } else if (wakeTicks == 655) {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
        } else if (wakeTicks == 670) {
            SkyTest.LOGGER.info("SKYTEST MOVE dash={}", com.replecant.skytest.client.feel.Dash.tryDash(mc));
        }
        var v = mc.player.getDeltaMovement();
        SkyTest.LOGGER.info("SKYTEST MOVE t={} keys={} speed={}", wakeTicks - 640, wakeTicks < 655 ? "W+sprint" : wakeTicks >= 670 ? "dash" : "none",
                String.format("%.3f", Math.sqrt(v.x * v.x + v.z * v.z)));
    }

    private static net.minecraft.world.entity.LivingEntity lastHit;
    private static int hitTicks;
    private static int hits;
    private static boolean dodgeTried;

    /** Бьём поднимающегося скелета мечом и логируем отброс. */
    private static void fightTest(Minecraft mc) {
        var player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (lastHit != null && ++hitTicks <= 2) {
            var v = lastHit.getDeltaMovement();
            SkyTest.LOGGER.info("SKYTEST HIT +{} target={} health={} knockback={}", hitTicks,
                    lastHit.getType().toShortString(), String.format("%.1f", lastHit.getHealth()),
                    String.format("%.3f", Math.sqrt(v.x * v.x + v.z * v.z)));
        }
        var target = mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                player.getBoundingBox().inflate(3.0), e -> e instanceof net.minecraft.world.entity.monster.skeleton.Skeleton
                        || e instanceof com.replecant.skytest.entity.PrisonWardenEntity).stream().findFirst().orElse(null);
        if (target == null || !target.isAlive()) {
            return;
        }
        var d = target.getEyePosition().subtract(player.getEyePosition());
        player.setYRot((float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F);
        player.setXRot((float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI));
        // Уклониться рывком назад, когда Страж заносит меч.
        if (target instanceof com.replecant.skytest.entity.PrisonWardenEntity warden
                && warden.state() == com.replecant.skytest.entity.PrisonWardenEntity.State.WINDUP
                && warden.stateTime(0.0F) > 11.0F && !dodgeTried) {
            dodgeTried = true;
            SkyTest.LOGGER.info("SKYTEST DODGE attempt dash={}", com.replecant.skytest.client.feel.Dash.tryDash(mc, 0.0, -1.0));
            return;
        }
        if (player.getAttackStrengthScale(0.0F) >= 0.95F && player.distanceTo(target) < 2.8F) {
            mc.gameMode.attack(player, target);
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            lastHit = target;
            hitTicks = 0;
            hits++;
            SkyTest.LOGGER.info("SKYTEST HIT #{} at distance {}", hits, String.format("%.2f", player.distanceTo(target)));
            if (hits == 1) {
                shot("14_hit");
            }
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
                message -> SkyTest.LOGGER.info("Screenshot {}: {}", name, message.getString()));
    }

    private ClientSmoke() {}
}
