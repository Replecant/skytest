package com.replecant.skytest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.RegulatorModel;
import com.replecant.skytest.entity.RegulatorEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class RegulatorRenderer extends MobRenderer<RegulatorEntity, RegulatorRenderState, RegulatorModel> {
    /** Масштаб модели (подбирается под её размеры, см. RegulatorModel). */
    static final float SCALE = 2.4F;
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/regulator.png");
    private static final RenderType EYES =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/regulator_eyes.png"));

    public RegulatorRenderer(EntityRendererProvider.Context context) {
        super(context, new RegulatorModel(context.bakeLayer(RegulatorModel.LAYER)), 2.1F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(RegulatorRenderState state) {
        return TEXTURE;
    }

    @Override
    public RegulatorRenderState createRenderState() {
        return new RegulatorRenderState();
    }

    @Override
    public void extractRenderState(RegulatorEntity entity, RegulatorRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.talking = entity.isTalking();
        state.modeTime = entity.modeTime(partialTick);
    }

    @Override
    protected void scale(RegulatorRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
