package com.replecant.skytest.client.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.replecant.skytest.SkyTest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Руки с магией. В первом лице вместо предмета рисуется настоящая рука игрока
 * (как с пустой ладонью), а на ней — сфера или змей. В третьем лице то же
 * добавляет слой на модель игрока; сам предмет в руке не рисуется.
 */
@EventBusSubscriber(modid = SkyTest.MODID, value = Dist.CLIENT)
public final class MagicHands {
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!MagicVisuals.isMagic(stack) || player == null) {
            return;
        }
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        pose.pushPose();
        // Та же поза, что у пустой руки в ванильном первом лице.
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1.0F : -1.0F;
        float swing = event.getSwingProgress();
        float equip = event.getEquipProgress();
        float sq = Mth.sqrt(swing);
        pose.translate(f * (-0.3F * Mth.sin(sq * Mth.PI) + 0.64F), 0.4F * Mth.sin(sq * Mth.TWO_PI) - 0.6F - equip * 0.6F,
                -0.4F * Mth.sin(swing * Mth.PI) - 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * Mth.sin(sq * Mth.PI) * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(f * Mth.sin(swing * swing * Mth.PI) * -20.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);

        AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        Identifier skin = player.getSkin().body().texturePath();
        int light = event.getPackedLight();
        if (right) {
            renderer.renderRightHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
        } else {
            renderer.renderLeftHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
        }
        PlayerModel model = renderer.getModel();
        ModelPart armPart = right ? model.rightArm : model.leftArm;
        armPart.translateAndRotate(pose);
        float time = player.tickCount + event.getPartialTick();
        float lunge = MagicVisuals.lunge(player.getCooldowns().getCooldownPercent(stack, event.getPartialTick()));
        MagicVisuals.renderOnArm(stack, pose, collector, light, time, lunge, !right);
        pose.popPose();
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        MagicVisuals.bake(event.getEntityModels());
        for (PlayerModelType type : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
            if (renderer != null) {
                renderer.addLayer(new Layer(renderer));
            }
        }
    }

    /** Третье лицо: сфера и змей на руке модели игрока. */
    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                float yRot, float xRot) {
            submitArm(pose, collector, light, state, HumanoidArm.RIGHT, state.rightHandItemStack);
            submitArm(pose, collector, light, state, HumanoidArm.LEFT, state.leftHandItemStack);
        }

        private void submitArm(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
                HumanoidArm arm, ItemStack stack) {
            if (stack == null || !MagicVisuals.isMagic(stack)) {
                return;
            }
            float lunge = 0.0F;
            Minecraft mc = Minecraft.getInstance();
            Entity entity = mc.level == null ? null : mc.level.getEntity(state.id);
            if (entity instanceof Player player) {
                lunge = MagicVisuals.lunge(player.getCooldowns().getCooldownPercent(stack, state.partialTick));
            }
            pose.pushPose();
            getParentModel().translateToHand(state, arm, pose);
            MagicVisuals.renderOnArm(stack, pose, collector, light, state.ageInTicks, lunge, arm == HumanoidArm.LEFT);
            pose.popPose();
        }
    }

    private MagicHands() {}
}
