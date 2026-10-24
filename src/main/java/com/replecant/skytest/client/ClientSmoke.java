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

    private static int regulatorOpened = -1;
    private static int regulatorClosed = -1;

    static void onDialogueOpened() {
        if (ENABLED) {
            dialogues++;
            if (dialogues == 2) {
                strangerOpened = clientTicks;
            } else if (dialogues == 3) {
                regulatorOpened = clientTicks;
            }
        }
    }

    static void onDialogueClosed() {
        if (ENABLED && dialogues == 2 && strangerClosed < 0) {
            strangerClosed = clientTicks;
        } else if (ENABLED && dialogues == 3 && regulatorClosed < 0) {
            regulatorClosed = clientTicks;
        }
    }

    private static void regulatorShots(Minecraft mc) {
        if (regulatorOpened >= 0 && regulatorClosed < 0) {
            int t = clientTicks - regulatorOpened;
            if (t == 30) {
                shot("21_regulator_dialogue");
            } else if (t == 90 && mc.player != null) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            } else if (t == 110) {
                shot("21b_regulator_nightvision");
            } else if (t == 120 && mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            } else if (t == 260) {
                shot("21c_regulator_dialogue_late");
            }
        }
        if (regulatorClosed >= 0) {
            int t = clientTicks - regulatorClosed;
            if (t == 25) {
                shot("22_quest_started");
            }
            if (t >= 100) {
                magicTest(mc, t - 100);
            }
            if (t == 1100) {
                SkyTest.LOGGER.info("SKYTEST CLIENT SMOKE DONE");
                mc.stop();
            }
        }
    }

    private static void tourTo(net.minecraft.server.MinecraftServer server, java.util.UUID uuid,
            net.minecraft.world.phys.Vec3 pos, float yaw, float pitch) {
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getAbilities().flying = player.getAbilities().mayfly;
                player.onUpdateAbilities();
                player.teleport(new net.minecraft.world.level.portal.TeleportTransition(server.overworld(), pos,
                        net.minecraft.world.phys.Vec3.ZERO, yaw, pitch, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            }
        });
    }

    private static java.util.UUID magicTarget;

    /** Сфера молний в правой руке, змей в левой: каст по крысам, кадры в первом и третьем лице. */
    private static void magicTest(Minecraft mc, int t) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        var uuid = mc.player.getUUID();
        if (t == 28) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
        }
        if (t == 30 || t == 52) {
            boolean first = t == 30;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) {
                    return;
                }
                if (first) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.item.ItemStack(com.replecant.skytest.registry.ModItems.LIGHTNING_SPHERE.get()));
                    player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
                            new net.minecraft.world.item.ItemStack(com.replecant.skytest.registry.ModItems.PRIMORDIAL_SERPENT.get()));
                    com.replecant.skytest.soul.SoulCharge.set(player, 50);
                }
                var rat = com.replecant.skytest.registry.ModEntities.RAT.get().create(player.level(),
                        net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (rat != null) {
                    var look = player.getLookAngle();
                    var at = player.position().add(look.x * 4.0, 0.0, look.z * 4.0).add(first ? 1.0 : -1.0, 0.0, 0.0);
                    rat.snapTo(at.x, player.getY(), at.z, 0.0F, 0.0F);
                    rat.setNoAi(true);
                    player.level().addFreshEntity(rat);
                    magicTarget = rat.getUUID();
                }
            });
        }
        if ((t == 36 || t == 58) && magicTarget != null && mc.level != null) {
            for (var e : mc.level.entitiesForRendering()) {
                if (e.getUUID().equals(magicTarget)) {
                    var d = e.position().add(0, 0.2, 0).subtract(mc.player.getEyePosition());
                    mc.player.setYRot((float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F);
                    mc.player.setXRot((float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI));
                }
            }
        }
        switch (t) {
            case 40 -> shot("23_magic_hands");
            case 44 -> SkyTest.LOGGER.info("SKYTEST MAGIC lightning {}", mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND));
            case 45 -> shot("24_lightning");
            case 47 -> shot("24b_lightning");
            case 62 -> SkyTest.LOGGER.info("SKYTEST MAGIC serpent {}", mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.OFF_HAND));
            case 63 -> shot("25_serpent_lunge");
            case 66 -> shot("25b_serpent_flying");
            case 69 -> shot("25c_serpent_flying");
            case 78 -> mc.player.setYRot(mc.player.getYRot() + 180.0F);
            case 80 -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            case 86 -> shot("26_magic_third_person");
            case 88 -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            case 100 -> SkyTest.LOGGER.info("SKYTEST MAGIC souls left {}",
                    mc.player.getData(com.replecant.skytest.registry.ModAttachments.SOUL_CHARGE));
            case 110 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 72.5), -60.0F, -22.0F);
            case 120 -> shot("27_upper_tier_from_ruins");
            case 125 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 46.5), -50.0F, -12.0F);
            case 135 -> shot("27b_upper_tier_far");
            // Задание «Найти Ларпи» целиком: Крысолов → приманка → Ларпи → Регулятор.
            case 140 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(22.5, 55, 12.5), -90.0F, 5.0F);
            case 165 -> shot("30_rat_catcher_dialogue");
            case 172, 300 -> {
                if (mc.screen instanceof com.replecant.skytest.client.DialogueScreen dialogue) {
                    dialogue.onClose();
                }
            }
            case 185 -> server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    SkyTest.LOGGER.info("SKYTEST LARPI quest {}", player.getData(com.replecant.skytest.registry.ModAttachments.ACTIVE_QUEST));
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.item.ItemStack(com.replecant.skytest.registry.ModItems.RAT_BAIT.get(), 6));
                    player.setXRot(30.0F);
                }
            });
            case 186 -> mc.player.setXRot(30.0F);
            case 232 -> shot("31_bait_swarm");
            case 236 -> server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) {
                    return;
                }
                player.level().getEntitiesOfClass(com.replecant.skytest.entity.RatEntity.class, player.getBoundingBox().inflate(12.0),
                                r -> !r.isLarpi()).stream().min(java.util.Comparator.comparingDouble(r -> r.distanceToSqr(player)))
                        .ifPresent(r -> r.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND));
            });
            case 246 -> server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) {
                    return;
                }
                player.level().getEntitiesOfClass(com.replecant.skytest.entity.RatEntity.class, player.getBoundingBox().inflate(40.0),
                        com.replecant.skytest.entity.RatEntity::isLarpi).stream().findFirst().ifPresent(larpi -> {
                            larpi.setNoAi(true);
                            larpi.snapTo(26.5, 55.0, 10.5, 30.0F, 0.0F);
                            var eye = new net.minecraft.world.phys.Vec3(26.0, 55.0, 12.3);
                            var d = larpi.position().subtract(eye.add(0, player.getEyeHeight(), 0));
                            float yaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F;
                            float pitch = (float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI);
                            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(server.overworld(), eye,
                                    net.minecraft.world.phys.Vec3.ZERO, yaw, pitch,
                                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
                            magicTarget = larpi.getUUID();
                        });
            });
            case 256 -> shot("32_larpi");
            case 260 -> server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                var level = server.overworld();
                if (player != null && magicTarget != null && level.getEntity(magicTarget) instanceof com.replecant.skytest.entity.RatEntity larpi) {
                    larpi.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            });
            case 270 -> {
                tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 114.5), 0.0F, -12.0F);
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        player.level().getEntitiesOfClass(com.replecant.skytest.entity.RegulatorEntity.class,
                                player.getBoundingBox().inflate(30.0)).stream().findFirst().ifPresent(r -> r.talkTo(player));
                    }
                });
            }
            case 292 -> shot("33_regulator_larpi");
            // Катсцена: Регулятор встаёт и тянет цепи, снаружи собирается мост.
            case 298 -> mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 700, 0, false, false));
            case 325 -> shot("35_cutscene_rise");
            case 336 -> shot("35a_cutscene_grab");
            case 349 -> shot("35b_cutscene_pull");
            case 371 -> shot("35c_cutscene_pull2");
            case 420 -> shot("36_cutscene_bridge_a");
            case 460 -> shot("36b_cutscene_bridge_b");
            case 500 -> shot("36c_cutscene_bridge_done");
            case 545 -> shot("34_quest_done");
            case 560 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 72.5), -70.0F, -14.0F);
            case 572 -> shot("28_upper_bridge");
            case 576 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 46.5), -50.0F, -10.0F);
            case 588 -> shot("28b_upper_bridge_far");
            case 592 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(89.5, 74, 71.5), -90.0F, 0.0F);
            case 606 -> shot("29_upper_hall");
            // Повернуть мост обратно к камерам: снова к Регулятору.
            case 615 -> {
                tourTo(server, uuid, new net.minecraft.world.phys.Vec3(38.5, 57, 114.5), 0.0F, -12.0F);
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        player.level().getEntitiesOfClass(com.replecant.skytest.entity.RegulatorEntity.class,
                                player.getBoundingBox().inflate(30.0)).stream().findFirst().ifPresent(r -> r.talkTo(player));
                    }
                });
            }
            case 630 -> shot("37_regulator_to_lower");
            case 640 -> {
                if (mc.screen instanceof com.replecant.skytest.client.DialogueScreen dialogue) {
                    dialogue.onClose();
                }
            }
            case 765 -> shot("38_bridge_back_a");
            case 805 -> shot("38b_bridge_back_b");
            // Тайник Копателя: щель в решётке и лаз до края скалы.
            case 900 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(18.5, 55, 15.3), 135.0F, 40.0F);
            case 912 -> shot("39a_digger_gap");
            case 916 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(14.5, 47, 29.5), 0.0F, 5.0F);
            case 930 -> shot("39b_digger_tunnel");
            case 934 -> tourTo(server, uuid, new net.minecraft.world.phys.Vec3(14.5, 47, 31.5), 0.0F, 25.0F);
            case 948 -> shot("39c_digger_stash");
            case 960 -> server.execute(() -> SkyTest.LOGGER.info("SKYTEST BRIDGE main={} upper={}",
                    com.replecant.skytest.world.LowerPrison.mainSpanBuilt(server.overworld()),
                    com.replecant.skytest.world.LowerPrison.upperBridgeBuilt(server.overworld())));
            default -> { }
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
            } else if (wakeTicks == 724) {
                shot("08d_soul_orbs");
            } else if (wakeTicks == 765) {
                shot("08e_soul_hud");
                SkyTest.LOGGER.info("SKYTEST SOUL client charge={}", mc.player == null ? -1
                        : mc.player.getData(com.replecant.skytest.registry.ModAttachments.SOUL_CHARGE));
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
        regulatorShots(mc);
        if (strangerClosed >= 0) {
            int t = clientTicks - strangerClosed;
            switch (t) {
                case 6 -> shot("10_sword_thrown");
                case 25 -> shot("11_stranger_leaving");
                case 75 -> shot("12_skeleton");
                case 265 -> shot("15_bridge");
                case 300 -> shot("16_prison_from_bridge");
                case 322 -> shot("16b_regulator_rock");
                case 345 -> shot("17_abyss");
                case 362 -> shot("18_warden_a");
                case 378 -> shot("18_warden_b");
                case 394 -> shot("18_warden_c");
                case 410 -> shot("18_warden_d");
                case 426 -> shot("18_warden_e");
                case 555 -> shot("20_regulator_hall");
                case 5000 -> {
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
    private static boolean parryTried;
    private static int parriedShot = -1;
    private static float dodgeHealth;
    private static int dodgeTick = -1;

    /** Бьём поднимающегося скелета мечом и логируем отброс. */
    private static void fightTest(Minecraft mc) {
        var player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (dodgeTick >= 0 && clientTicks == dodgeTick + 20) {
            SkyTest.LOGGER.info("SKYTEST {} result hp {} -> {}", dodgeTried ? "DODGE" : "PARRY", dodgeHealth, player.getHealth());
        }
        if (parriedShot >= 0 && clientTicks == parriedShot + 8) {
            shot("19_warden_parried");
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
        if (parriedShot < 0 && target instanceof com.replecant.skytest.entity.PrisonWardenEntity w
                && w.state() == com.replecant.skytest.entity.PrisonWardenEntity.State.PARRIED) {
            parriedShot = clientTicks;
        }
        var d = target.getEyePosition().subtract(player.getEyePosition());
        player.setYRot((float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F);
        player.setXRot((float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI));
        // Во время замаха Стража не бьём, а уклоняемся рывком назад перед самым ударом.
        if (target instanceof com.replecant.skytest.entity.PrisonWardenEntity warden
                && warden.state() == com.replecant.skytest.entity.PrisonWardenEntity.State.WINDUP) {
            if (!parryTried && warden.stateTime(0.0F) >= 10.0F) {
                parryTried = true;
                dodgeHealth = player.getHealth();
                dodgeTick = clientTicks;
                var result = mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                SkyTest.LOGGER.info("SKYTEST PARRY attempt {} holding {} hp={}", result,
                        player.getMainHandItem().getItem(), dodgeHealth);
            } else if (parryTried && clientTicks - dodgeTick > 40 && !dodgeTried && warden.stateTime(0.0F) >= 10.0F) {
                dodgeTried = true;
                dodgeHealth = player.getHealth();
                dodgeTick = clientTicks;
                SkyTest.LOGGER.info("SKYTEST DODGE attempt dash={} hp={}",
                        com.replecant.skytest.client.feel.Dash.tryDash(mc, 0.0, -1.0), dodgeHealth);
            }
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
