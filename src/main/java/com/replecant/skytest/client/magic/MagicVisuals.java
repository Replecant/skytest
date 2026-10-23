package com.replecant.skytest.client.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.model.ArmSerpentModel;
import com.replecant.skytest.client.model.SoulSphereModel;
import com.replecant.skytest.client.renderer.SerpentRenderer;
import com.replecant.skytest.item.LightningSphereItem;
import com.replecant.skytest.item.PrimordialSerpentItem;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Магия на руке: сфера молний над кистью и змей, обвивший предплечье.
 * Рисуется в пространстве руки модели игрока (и в первом, и в третьем лице).
 */
public final class MagicVisuals {
    private static final Identifier CORE = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/magic/sphere_core.png");
    private static final Identifier SWIRL = Identifier.fromNamespaceAndPath(SkyTest.MODID, "textures/entity/magic/sphere_swirl.png");
    private static final RenderType CORE_TYPE = RenderTypes.eyes(CORE);

    private static SoulSphereModel sphere;
    private static ArmSerpentModel serpent;

    static void bake(EntityModelSet models) {
        sphere = new SoulSphereModel(models.bakeLayer(SoulSphereModel.LAYER));
        serpent = new ArmSerpentModel(models.bakeLayer(ArmSerpentModel.LAYER));
    }

    public static boolean isMagic(ItemStack stack) {
        return stack.getItem() instanceof LightningSphereItem || stack.getItem() instanceof PrimordialSerpentItem;
    }

    /**
     * @param pose  уже в пространстве руки (после translateAndRotate части руки)
     * @param lunge 0..1 — отдача от выстрела
     * @param left  левая рука (ось руки сдвинута в другую сторону)
     */
    public static void renderOnArm(ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light,
            float time, float lunge, boolean left) {
        if (sphere == null) {
            return;
        }
        pose.pushPose();
        pose.translate((left ? 1.0F : -1.0F) / 16.0F, 0.0F, 0.0F);
        if (stack.getItem() instanceof LightningSphereItem) {
            float bob = 0.025F * Mth.sin(time * 0.12F);
            pose.translate(0.0F, (10.5F + 2.0F * lunge) / 16.0F, -4.8F / 16.0F + bob);
            float t = time;
            collector.submitModel(sphere, new SoulSphereModel.State(t, false), pose, CORE_TYPE,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0, null);
            collector.submitModel(sphere, new SoulSphereModel.State(t, true), pose,
                    RenderTypes.energySwirl(SWIRL, (t * 0.02F) % 1.0F, (t * 0.013F) % 1.0F),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0, null);
            // Мелкие разряды из сферы — новые каждые два тика.
            long seed = (long) (time / 2.0F);
            float strength = 0.6F + 0.4F * lunge;
            collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
                RandomSource random = RandomSource.create(seed);
                int arcs = 2 + random.nextInt(3);
                for (int i = 0; i < arcs; i++) {
                    Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                    double reach = 0.16 + random.nextDouble() * 0.12;
                    Bolt.draw(p, vc, dir.scale(0.05), dir.scale(reach), random.nextLong(), 0.006F, strength, 0);
                }
            });
        } else {
            collector.submitModel(serpent, new ArmSerpentModel.State(time, lunge), pose,
                    serpent.renderType(SerpentRenderer.TEXTURE), light, OverlayTexture.NO_OVERLAY, 0, null);
            collector.submitModel(serpent, new ArmSerpentModel.State(time, lunge), pose, SerpentRenderer.EYES,
                    light, OverlayTexture.NO_OVERLAY, 0, null);
        }
        pose.popPose();
    }

    /** Резкий бросок в первые доли перезарядки, затем возврат. */
    public static float lunge(float cooldownPercent) {
        if (cooldownPercent <= 0.0F) {
            return 0.0F;
        }
        float p = 1.0F - cooldownPercent;
        return p < 0.35F ? Mth.sin(p / 0.35F * Mth.PI) : 0.0F;
    }

    private MagicVisuals() {}
}
