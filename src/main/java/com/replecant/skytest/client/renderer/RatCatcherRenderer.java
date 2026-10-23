package com.replecant.skytest.client.renderer;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.RatCatcherModel;
import com.replecant.skytest.entity.RatCatcherEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class RatCatcherRenderer extends MobRenderer<RatCatcherEntity, RatCatcherRenderState, RatCatcherModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/rat_catcher.png");
    private static final RenderType EYES =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/rat_catcher_eyes.png"));

    public RatCatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new RatCatcherModel(context.bakeLayer(RatCatcherModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(RatCatcherRenderState state) {
        return TEXTURE;
    }

    @Override
    public RatCatcherRenderState createRenderState() {
        return new RatCatcherRenderState();
    }

    @Override
    public void extractRenderState(RatCatcherEntity entity, RatCatcherRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.talking = entity.isTalking();
        state.modeTime = entity.modeTime(partialTick);
    }
}
