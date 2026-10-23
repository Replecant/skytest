package com.replecant.skytest.client.renderer;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.RatModel;
import com.replecant.skytest.entity.RatEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class RatRenderer extends MobRenderer<RatEntity, LivingEntityRenderState, RatModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/rat.png");

    public RatRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel(context.bakeLayer(RatModel.LAYER)), 0.25F);
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
