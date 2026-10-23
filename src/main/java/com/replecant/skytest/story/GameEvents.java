package com.replecant.skytest.story;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.world.AdventureWorld;
import com.replecant.skytest.world.LowerPrison;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SkyTest.MODID)
public final class GameEvents {
    private static final Set<UUID> REACHED_END = new HashSet<>();

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
            Prologue.start(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AdventureWorld.isAdventure(player.level())) {
            Prologue.teleportToCell(player);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !AdventureWorld.isAdventure(player.level())) {
            return;
        }
        // Голода в этой игре нет.
        player.getFoodData().setFoodLevel(20);

        if (player.tickCount % 10 == 0 && player instanceof ServerPlayer serverPlayer
                && LowerPrison.END_ROOM.contains(player.position())
                && REACHED_END.add(player.getUUID())) {
            serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 40));
            serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("area.skytest.test_end.subtitle").withStyle(ChatFormatting.GRAY)));
            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("area.skytest.test_end").withStyle(ChatFormatting.GOLD)));
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        Prologue.tick(event.getServer());
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("skytest")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("prologue").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Prologue.reset(player);
                    REACHED_END.remove(player.getUUID());
                    Prologue.start(player);
                    return 1;
                }))
                .then(Commands.literal("rebuild").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    level.setBlock(new net.minecraft.core.BlockPos(-12, 56, -16), Blocks.AIR.defaultBlockState(), 2);
                    LowerPrison.build(level);
                    ctx.getSource().sendSuccess(() -> Component.literal("Lower Prison rebuilt"), true);
                    return 1;
                }))
                .then(Commands.literal("cell").executes(ctx -> {
                    Prologue.teleportToCell(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private GameEvents() {}
}
