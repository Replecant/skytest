package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Первородный змей, обвивший руку. Координаты — в пространстве руки игрока:
 * начало на оси руки у плеча, +y — вниз по руке к кисти (кисть около y = 10),
 * −z — тыльная сторона (сверху, когда рука вытянута вперёд).
 * Четыре наклонных витка вокруг предплечья, шея по тыльной стороне,
 * голова на тыльной стороне кисти, мелькающий язык.
 */
public class ArmSerpentModel extends Model<ArmSerpentModel.State> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "arm_serpent"), "main");

    /** @param lunge 0..1 — бросок головы вперёд при выстреле */
    public record State(float time, float lunge) {}

    private static final float[] RING_Y = {0.5F, 3.0F, 5.5F, 8.0F};

    private final ModelPart[] rings = new ModelPart[RING_Y.length];
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tongue;

    public ArmSerpentModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        for (int i = 0; i < rings.length; i++) {
            rings[i] = root.getChild("ring" + i);
        }
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tongue = jaw.getChild("tongue");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < RING_Y.length; i++) {
            // Квадратная «петля» вокруг руки 4×4, наклонённая, — витки складываются в спираль.
            float outer = 3.3F - i * 0.1F;
            float thick = 1.3F - i * 0.05F;
            root.addOrReplaceChild("ring" + i, CubeListBuilder.create()
                    .texOffs(0, 10).addBox(-outer, -1.0F, -outer, outer * 2, 2.0F, thick)
                    .texOffs(20, 10).addBox(-outer, -1.0F, outer - thick, outer * 2, 2.0F, thick)
                    .texOffs(40, 10).addBox(-outer, -1.0F, -outer + thick, thick, 2.0F, outer * 2 - thick * 2)
                    .texOffs(0, 20).addBox(outer - thick, -1.0F, -outer + thick, thick, 2.0F, outer * 2 - thick * 2),
                    PartPose.offsetAndRotation(0.0F, RING_Y[i], 0.0F, 0.28F, i * 0.4F, 0.0F));
        }
        // Хвост свисает из нижнего витка.
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5F, -3.5F, -0.5F, 1.0F, 3.5F, 1.0F), PartPose.offsetAndRotation(2.6F, 0.5F, 2.6F, 0.4F, 0.0F, 0.5F));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(40, 20)
                .addBox(-1.1F, 0.0F, -0.8F, 2.2F, 3.0F, 1.6F), PartPose.offset(0.0F, 8.2F, -3.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.8F, 0.0F, -1.3F, 3.6F, 4.0F, 2.6F)
                // глаза — по бокам головы
                .texOffs(40, 0).addBox(-2.1F, 1.0F, -1.2F, 0.6F, 1.0F, 1.0F)
                .texOffs(40, 0).addBox(1.5F, 1.0F, -1.2F, 0.6F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 2.6F, -0.4F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 3.5F, 1.0F), PartPose.offset(0.0F, 0.5F, 1.2F));
        jaw.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-0.4F, 0.0F, -0.2F, 0.8F, 2.5F, 0.4F), PartPose.offset(0.0F, 3.3F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(State state) {
        resetPose();
        float t = state.time();
        float lunge = state.lunge();
        // Витки чуть «дышат» — змей сжимает руку.
        for (int i = 0; i < rings.length; i++) {
            float squeeze = 1.0F + 0.04F * Mth.sin(t * 0.15F - i * 0.9F);
            rings[i].xScale = squeeze;
            rings[i].zScale = squeeze;
        }
        neck.xRot = -0.15F + 0.06F * Mth.sin(t * 0.08F) - 0.5F * lunge;
        neck.y += 1.5F * lunge;
        head.zRot = 0.12F * Mth.sin(t * 0.05F);
        head.xRot = 0.1F * Mth.sin(t * 0.11F);
        head.y += 2.5F * lunge;
        jaw.xRot = 0.15F + 0.9F * lunge;
        // Язык мелькает короткими сериями.
        float flick = (t % 40.0F) / 40.0F;
        tongue.visible = flick < 0.25F && ((int) (t * 1.5F)) % 2 == 0 || lunge > 0.3F;
        tongue.zRot = 0.3F * Mth.sin(t * 2.0F);
    }
}
