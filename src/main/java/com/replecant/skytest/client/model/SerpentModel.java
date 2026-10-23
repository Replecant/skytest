package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.renderer.SerpentRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Летящий змей: голова (+z — вперёд) и цепочка сужающихся сегментов, по которой бежит волна. */
public class SerpentModel extends EntityModel<SerpentRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "serpent"), "main");

    private static final int SEGMENTS = 5;

    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public SerpentModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        ModelPart parent = root;
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i] = parent.getChild("segment" + i);
            parent = segments[i];
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 5.0F)
                .texOffs(40, 0).addBox(-2.3F, -1.6F, 2.5F, 0.6F, 1.0F, 1.0F)
                .texOffs(40, 0).addBox(1.7F, -1.6F, 2.5F, 0.6F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.7F, 0.0F, 0.0F, 3.4F, 1.0F, 4.5F), PartPose.offset(0.0F, 1.0F, 0.3F));
        float[][] sizes = {{3.2F, 2.6F, 5.0F}, {2.8F, 2.2F, 5.0F}, {2.3F, 1.9F, 5.0F}, {1.7F, 1.4F, 4.5F}, {1.0F, 0.9F, 4.5F}};
        int[][] tex = {{0, 10}, {20, 10}, {40, 10}, {0, 20}, {20, 20}};
        PartDefinition parent = root;
        float z = 0.0F;
        for (int i = 0; i < SEGMENTS; i++) {
            float[] s = sizes[i];
            parent = parent.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(tex[i][0], tex[i][1])
                    .addBox(-s[0] / 2, -s[1] / 2, -s[2], s[0], s[1], s[2]), PartPose.offset(0.0F, 0.0F, z));
            z = -s[2];
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SerpentRenderState state) {
        super.setupAnim(state);
        float t = state.age;
        for (int i = 0; i < SEGMENTS; i++) {
            segments[i].yRot = 0.55F * Mth.sin(t * 0.9F - i * 1.1F);
        }
        head.yRot = -0.35F * Mth.sin(t * 0.9F + 0.6F);
        jaw.xRot = 0.25F + 0.25F * Mth.sin(t * 0.5F);
    }
}
