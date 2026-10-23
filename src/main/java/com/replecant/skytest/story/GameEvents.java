package com.replecant.skytest.story;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.world.AdventureWorld;
import com.replecant.skytest.world.LowerPrison;
import com.replecant.skytest.world.RatSpawner;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SkyTest.MODID)
public final class GameEvents {

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!AdventureWorld.isAdventure(server)) {
            SkyTest.LOGGER.info("Not an adventure world, Lower Prison is not generated");
            return;
        }
        ServerLevel level = server.overworld();
        AdventureWorld.applyGameRules(level);
        if (!LowerPrison.isBuilt(level)) {
            LowerPrison.build(level);
        }
        if ("true".equals(System.getenv("SKYTEST_SMOKE"))) {
            SmokeTest.run(server);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && AdventureWorld.isAdventure(player.level().getServer())
                && !Prologue.isSeen(player)) {
            Prologue.onFirstLogin(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AdventureWorld.isAdventure(player.level())) {
            if (LowerPrison.mainSpanBuilt(player.level())) {
                Prologue.teleportToCell(player);
            } else {
                // Мост повёрнут к Верхнему ярусу — из камеры не выбраться; возрождаемся у Регулятора.
                player.teleport(new TeleportTransition(player.level(), LowerPrison.REGULATOR_ENTRANCE, Vec3.ZERO,
                        180.0F, 0.0F, TeleportTransition.DO_NOTHING));
            }
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !AdventureWorld.isAdventure(player.level())) {
            return;
        }
        // Падение в бездну: возврат на начало моста (или в камеру) с уроном.
        if (player.getY() < LowerPrison.ABYSS_Y && !player.isSpectator() && !player.getAbilities().flying) {
            Vec3 back = !HallEvent.hasFlag(player, "reached_bridge") ? LowerPrison.PLAYER_SPAWN
                    : LowerPrison.mainSpanBuilt(player.level()) ? LowerPrison.BRIDGE_START : LowerPrison.RUINS_POINT;
            player.teleport(new TeleportTransition(player.level().getServer().overworld(), back, Vec3.ZERO,
                    0.0F, 0.0F, TeleportTransition.DO_NOTHING));
            player.resetFallDistance();
            player.hurtServer(player.level(), player.damageSources().fellOutOfWorld(), 4.0F);
            return;
        }

        if (player.tickCount % 5 == 0) {
            HallEvent.checkTrigger(player);
            HallEvent.checkRegulator(player);
            HallEvent.checkRatCatcher(player);
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        // Надписи — только для основных уровней тюрьмы, по разу на игрока.
        Vec3 pos = player.position();
        if (LowerPrison.BRIDGE.contains(pos) && !HallEvent.hasFlag(player, "reached_bridge")) {
            HallEvent.setFlag(player, "reached_bridge");
        }
        if (!com.replecant.skytest.Config.PROLOGUE_ENABLED.get() && LowerPrison.CELL_BLOCK.contains(pos)) {
            showAreaOnce(player, "lower_prison");
        }
        if (LowerPrison.UPPER_HALL.contains(pos)) {
            showAreaOnce(player, "upper_tier");
        }
    }

    private static void showAreaOnce(ServerPlayer player, String area) {
        String flag = "seen_area_" + area;
        if (!HallEvent.hasFlag(player, flag)) {
            HallEvent.setFlag(player, flag);
            showTitle(player, "area.skytest." + area, 25, 80, 35);
        }
    }

    private static void showTitle(ServerPlayer player, String key, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable(key + ".subtitle").withStyle(ChatFormatting.GRAY)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable(key).withStyle(ChatFormatting.GOLD)));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        Prologue.tick(event.getServer());
        HallEvent.tick(event.getServer());
        RegulatorCutscene.tick(event.getServer());
        if (AdventureWorld.isAdventure(event.getServer())) {
            RatSpawner.tick(event.getServer().overworld());
        }
    }

    /** Скелеты-узники ничего не роняют: в этой игре нет ванильного лута. */
    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(HallEvent.PRISONER_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("skytest")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("prologue").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Prologue.reset(player);
                    Prologue.start(player);
                    return 1;
                }))
                .then(Commands.literal("rebuild").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    LowerPrison.clear(level);
                    LowerPrison.build(level);
                    ctx.getSource().sendSuccess(() -> Component.literal("Lower Prison rebuilt"), true);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Prologue.reset(player);
                    player.setData(com.replecant.skytest.registry.ModAttachments.STORY_FLAGS, new java.util.HashSet<>());
                    com.replecant.skytest.story.Quests.reset(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Story progress reset (use /skytest rebuild to respawn NPCs)"), false);
                    return 1;
                }))
                .then(Commands.literal("upperbridge").executes(ctx -> {
                    boolean built = LowerPrison.buildUpperBridge(ctx.getSource().getServer().overworld());
                    ctx.getSource().sendSuccess(() -> Component.literal(built
                            ? "Bridge to the Upper Tier built" : "Upper Tier portal not found (try /skytest rebuild)"), true);
                    return built ? 1 : 0;
                }))
                .then(Commands.literal("cell").executes(ctx -> {
                    Prologue.teleportToCell(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private GameEvents() {}
}
