package com.replecant.skytest.client.renderer;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.StrangerModel;
import com.replecant.skytest.entity.StrangerEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class StrangerRenderer extends MobRenderer<StrangerEntity, LivingEntityRenderState, StrangerModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/stranger.png");
    private static final RenderType EYES =
            RenderTypes.eyes(Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/stranger_eyes.png"));

    public StrangerRenderer(EntityRendererProvider.Context context) {
        super(context, new StrangerModel(context.bakeLayer(StrangerModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
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
}
