package com.replecant.skytest.client;

import java.util.Set;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.LamplighterModel;
import com.replecant.skytest.client.model.PrisonWardenModel;
import com.replecant.skytest.client.model.RatModel;
import com.replecant.skytest.client.renderer.RatRenderer;
import com.replecant.skytest.client.model.StrangerModel;
import com.replecant.skytest.client.renderer.PrisonWardenRenderer;
import com.replecant.skytest.client.renderer.LamplighterRenderer;
import com.replecant.skytest.client.renderer.StrangerRenderer;
import com.replecant.skytest.network.DoubleJumpPayload;
import com.replecant.skytest.registry.ModEntities;
import com.replecant.skytest.registry.ModItems;
import com.replecant.skytest.world.AdventureWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@Mod(value = SkyTest.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public class SkyTestClient {
    private static final double DOUBLE_JUMP_POWER = 0.5;
    private static final int MIN_AIR_TICKS = 3;
    /** Ванильные элементы HUD, которых нет в мире приключения. */
    private static final Set<Identifier> HIDDEN_HUD = Set.of(
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND);

    private static boolean wasJumpDown;

    public SkyTestClient() {
        com.replecant.skytest.item.DullSwordItem.clientParryHook =
                com.replecant.skytest.client.feel.ComboFeel::startParry;
    }
    private static boolean doubleJumpUsed;
    private static int airTicks;

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GRAPPLING_HOOK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LAMPLIGHTER.get(), LamplighterRenderer::new);
        event.registerEntityRenderer(ModEntities.STRANGER.get(), StrangerRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISON_WARDEN.get(), PrisonWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.RAT.get(), RatRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LamplighterModel.LAYER, LamplighterModel::createBodyLayer);
        event.registerLayerDefinition(StrangerModel.LAYER, StrangerModel::createBodyLayer);
        event.registerLayerDefinition(PrisonWardenModel.LAYER, PrisonWardenModel::createBodyLayer);
        event.registerLayerDefinition(RatModel.LAYER, RatModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRenderHudLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && HIDDEN_HUD.contains(event.getName()) && AdventureWorld.isAdventure(mc.level)) {
            event.setCanceled(true);
        }
    }

    /** Ванильные достижения и рецепты в этой игре не нужны — их всплывашки тоже. */
    @SubscribeEvent
    static void onToast(ToastAddEvent event) {
        if (event.getToast() instanceof AdvancementToast || event.getToast() instanceof RecipeToast
                || event.getToast() instanceof TutorialToast) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(event.getNewFovModifier() * DialogueScreen.fovModifier());
    }

    @SubscribeEvent
    static void onRenderHud(RenderGuiEvent.Post event) {
        PrologueController.renderOverlay(event.getGuiGraphics());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        PrologueController.tick(mc);

        LocalPlayer player = mc.player;
        if (player == null) {
            wasJumpDown = false;
            return;
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasJumpDown;
        wasJumpDown = jumpDown;

        if (player.onGround() || player.isInWater() || player.onClimbable() || player.isPassenger()) {
            doubleJumpUsed = false;
            airTicks = 0;
            return;
        }
        airTicks++;

        if (!jumpPressed || doubleJumpUsed || airTicks < MIN_AIR_TICKS
                || player.getAbilities().flying || player.isFallFlying()
                || !player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CIRCUS_BOOTS.get())) {
            return;
        }

        doubleJumpUsed = true;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, DOUBLE_JUMP_POWER, motion.z);
        player.resetFallDistance();
        ClientPacketDistributor.sendToServer(DoubleJumpPayload.INSTANCE);
    }
}
