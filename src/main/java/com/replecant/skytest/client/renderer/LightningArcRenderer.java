package com.replecant.skytest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.replecant.skytest.client.magic.Bolt;
import com.replecant.skytest.entity.LightningArcEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Разряд молнии: ломаная дуга, которая дрожит и гаснет за несколько тиков. */
public class LightningArcRenderer extends EntityRenderer<LightningArcEntity, LightningArcRenderer.State> {
    public static class State extends EntityRenderState {
        Vec3 offset = Vec3.ZERO;
        float age;
        int seed;
    }

    public LightningArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(LightningArcEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LightningArcEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.offset = entity.offset();
        state.age = entity.tickCount + partialTick;
        state.seed = entity.getId();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float alpha = 1.0F - Math.min(1.0F, state.age / (LightningArcEntity.LIFETIME + 1.0F));
        if (alpha <= 0.0F) {
            return;
        }
        Vec3 offset = state.offset;
        // Каждые полтика — новая ломаная: разряд трещит.
        long seed = state.seed * 7919L + (long) (state.age * 0.7F);
        float width = 0.035F + 0.03F * alpha;
        collector.submitCustomGeometry(pose, RenderTypes.lightning(),
                (p, vc) -> Bolt.draw(p, vc, Vec3.ZERO, offset, seed, width, alpha, 2));
    }
}
