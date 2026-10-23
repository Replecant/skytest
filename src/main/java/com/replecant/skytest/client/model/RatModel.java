package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Крыса: вытянутое тельце, острая морда, уши, лапки и длинный хвост из двух звеньев. */
public class RatModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "rat"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public RatModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.tail = root.getChild("tail");
        this.tailTip = tail.getChild("tail_tip");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.5F, -2.0F, -4.0F, 5.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(40, 0).addBox(-1.0F, -0.5F, -5.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 20.5F, -4.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(46, 0)
                .addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(1.2F, -1.5F, -1.5F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(46, 0)
                .addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.2F, -1.5F, -1.5F));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 21.0F, 4.0F));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 5.0F));
        for (String side : new String[] {"left", "right"}) {
            float x = side.equals("left") ? 1.5F : -1.5F;
            root.addOrReplaceChild("front_" + side + "_leg", CubeListBuilder.create().texOffs(12, 12)
                    .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(x, 22.0F, -2.5F));
            root.addOrReplaceChild("back_" + side + "_leg", CubeListBuilder.create().texOffs(12, 12)
                    .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(x, 22.0F, 2.5F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float age = state.ageInTicks;
        float idle = 1.0F - speed;

        // Семенящие лапки: диагональные пары, быстрый шаг.
        float step = Mth.cos(walk * 1.9F) * 1.3F * speed;
        frontLeftLeg.xRot = step;
        backRightLeg.xRot = step;
        frontRightLeg.xRot = -step;
        backLeftLeg.xRot = -step;

        // Тельце подпрыгивает на бегу.
        float bob = Math.abs(Mth.sin(walk * 1.9F)) * 0.6F * speed;
        body.y = 21.0F - bob;
        head.y = 20.5F - bob;

        // Голова следит, а в покое крыса быстро принюхивается.
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD + Mth.sin(age * 1.4F) * 0.06F * idle;
        float twitch = Mth.sin(age * 0.9F) > 0.92F ? 0.4F : 0.0F;
        leftEar.zRot = 0.2F + twitch;
        rightEar.zRot = -0.2F - twitch;

        // Хвост: волна от основания к кончику.
        tail.xRot = -0.25F + 0.15F * speed;
        tail.yRot = Mth.sin(age * 0.25F) * 0.25F * idle + Mth.sin(walk * 1.9F) * 0.3F * speed;
        tailTip.xRot = 0.35F;
        tailTip.yRot = Mth.sin(age * 0.25F - 0.8F) * 0.35F * idle + Mth.sin(walk * 1.9F - 0.8F) * 0.4F * speed;
    }
}
