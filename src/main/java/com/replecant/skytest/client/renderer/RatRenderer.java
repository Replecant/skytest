package com.replecant.skytest.client.renderer;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.RatModel;
import com.replecant.skytest.entity.RatEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class RatRenderer extends MobRenderer<RatEntity, RatRenderer.State, RatModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/rat.png");
    /** Ларпи: та же крыса, но с белым пятнышком на ухе. */
    private static final Identifier LARPI = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/rat_larpi.png");

    public static class State extends LivingEntityRenderState {
        boolean larpi;
    }

    public RatRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel(context.bakeLayer(RatModel.LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.larpi ? LARPI : TEXTURE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RatEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.larpi = entity.isLarpi();
    }
}
