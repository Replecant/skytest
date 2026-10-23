package com.replecant.skytest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.LamplighterModel;
import com.replecant.skytest.entity.LamplighterEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class LamplighterRenderer extends MobRenderer<LamplighterEntity, LivingEntityRenderState, LamplighterModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/lamplighter.png");
    private static final Identifier EYE_TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/lamplighter_eye.png");
    private static final RenderType EYE_RENDER_TYPE = RenderTypes.eyes(EYE_TEXTURE);

    public LamplighterRenderer(EntityRendererProvider.Context context) {
        super(context, new LamplighterModel(context.bakeLayer(LamplighterModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYE_RENDER_TYPE;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }
}
