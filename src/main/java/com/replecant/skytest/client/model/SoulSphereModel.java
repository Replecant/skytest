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
 * Электрическая сфера над ладонью: яркое пульсирующее ядро и две оболочки,
 * вращающиеся вокруг разных осей. Ядро и оболочки рисуются разными типами
 * отрисовки, поэтому в состоянии указано, какую часть показывать.
 */
public class SoulSphereModel extends Model<SoulSphereModel.State> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "lightning_sphere"), "main");

    public record State(float time, boolean shells) {}

    private final ModelPart core;
    private final ModelPart shellA;
    private final ModelPart shellB;
    private final ModelPart shellC;

    public SoulSphereModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.core = root.getChild("core");
        this.shellA = root.getChild("shell_a");
        this.shellB = root.getChild("shell_b");
        this.shellC = root.getChild("shell_c");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Ядро — три повёрнутых друг к другу кубика: издалека читается как шар.
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.2F, -1.2F, -1.2F, 2.4F, 2.4F, 2.4F)
                .addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F)
                .addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F), PartPose.ZERO);
        root.addOrReplaceChild("shell_a", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2.2F, -2.2F, -2.2F, 4.4F, 4.4F, 4.4F), PartPose.ZERO);
        root.addOrReplaceChild("shell_b", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2.2F, -2.2F, -2.2F, 4.4F, 4.4F, 4.4F), PartPose.rotation(0.6F, 0.0F, 0.785F));
        root.addOrReplaceChild("shell_c", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-2.2F, -2.2F, -2.2F, 4.4F, 4.4F, 4.4F), PartPose.rotation(0.785F, 0.785F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State state) {
        resetPose();
        float t = state.time();
        core.visible = !state.shells();
        shellA.visible = state.shells();
        shellB.visible = state.shells();
        shellC.visible = state.shells();
        float pulse = 1.0F + 0.12F * Mth.sin(t * 0.7F) + 0.05F * Mth.sin(t * 2.3F);
        core.xScale = pulse;
        core.yScale = pulse;
        core.zScale = pulse;
        core.yRot = t * 0.2F;
        shellA.yRot = t * 0.17F;
        shellA.xRot = t * 0.11F;
        shellB.zRot += -t * 0.14F;
        shellB.yRot = -t * 0.09F;
        shellC.xRot += t * 0.13F;
        shellC.zRot = t * 0.08F;
    }
}
