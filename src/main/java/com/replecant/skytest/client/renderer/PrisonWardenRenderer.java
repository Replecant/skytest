package com.replecant.skytest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.PrisonWardenModel;
import com.replecant.skytest.entity.PrisonWardenEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class PrisonWardenRenderer extends MobRenderer<PrisonWardenEntity, PrisonWardenRenderState, PrisonWardenModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/prison_warden.png");
    private static final RenderType EYES =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/prison_warden_eyes.png"));

    public PrisonWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new PrisonWardenModel(context.bakeLayer(PrisonWardenModel.LAYER)), 0.7F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(PrisonWardenRenderState state) {
        return TEXTURE;
    }

    @Override
    public PrisonWardenRenderState createRenderState() {
        return new PrisonWardenRenderState();
    }

    @Override
    public void extractRenderState(PrisonWardenEntity entity, PrisonWardenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.state = entity.state();
        state.stateTime = entity.stateTime(partialTick);
    }

    @Override
    protected void scale(PrisonWardenRenderState state, PoseStack poseStack) {
        poseStack.scale(1.08F, 1.08F, 1.08F);
    }
}
