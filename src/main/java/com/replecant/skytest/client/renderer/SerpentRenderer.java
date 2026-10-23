package com.replecant.skytest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.SerpentModel;
import com.replecant.skytest.entity.SerpentEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SerpentRenderer extends EntityRenderer<SerpentEntity, SerpentRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/serpent.png");
    public static final RenderType EYES =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/serpent_eyes.png"));
    private static final float SCALE = 0.6F;

    private final SerpentModel model;

    public SerpentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SerpentModel(context.bakeLayer(SerpentModel.LAYER));
    }

    @Override
    public SerpentRenderState createRenderState() {
        return new SerpentRenderState();
    }

    @Override
    public void extractRenderState(SerpentEntity entity, SerpentRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        Vec3 v = entity.getDeltaMovement();
        state.yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        state.pitch = (float) (Mth.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * Mth.RAD_TO_DEG);
    }

    @Override
    public void submit(SerpentRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.0F, 0.2F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        pose.scale(-SCALE, -SCALE, SCALE);
        collector.submitModel(model, state, pose, model.renderType(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, 0, null);
        collector.submitModel(model, state, pose, EYES, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, null);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
