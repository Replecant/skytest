package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Незнакомец: человеческая фигура в длиннополом кожаном плаще, охотничья шляпа
 * с широкими полями и пером, шарф закрывает низ лица.
 */
public class StrangerModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "stranger"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart scarfTail;

    public StrangerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.scarfTail = body.getChild("scarf_tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        // Шарф закрывает нижнюю половину лица.
                        .texOffs(68, 0).addBox(-4.5F, -4.0F, -4.5F, 9.0F, 4.0F, 9.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 3.0F, 9.0F)
                        .texOffs(0, 48).addBox(-7.0F, -8.5F, -7.0F, 14.0F, 1.0F, 14.0F),
                PartPose.ZERO);
        hat.addOrReplaceChild("feather", CubeListBuilder.create()
                        .texOffs(84, 16).addBox(-0.5F, -6.0F, -1.5F, 1.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(4.6F, -9.0F, 1.0F, -0.5F, 0.0F, 0.35F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                        // Полы плаща.
                        .texOffs(24, 16).addBox(-4.5F, 10.0F, -2.5F, 9.0F, 8.0F, 5.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("scarf_tail", CubeListBuilder.create()
                        .texOffs(104, 0).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 8.0F, 1.0F),
                PartPose.offset(2.0F, 0.5F, -2.6F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 16).mirror()
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(68, 16)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(68, 16).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        float age = state.ageInTicks;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        rightArm.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 2.0F * speed * 0.5F + Mth.sin(age * 0.067F) * 0.03F;
        leftArm.xRot = Mth.cos(walk * 0.6662F) * 2.0F * speed * 0.5F - Mth.sin(age * 0.067F) * 0.03F;
        rightArm.zRot = Mth.cos(age * 0.09F) * 0.04F + 0.04F;
        leftArm.zRot = -Mth.cos(age * 0.09F) * 0.04F - 0.04F;
        rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.4F * speed;
        leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.4F * speed;

        // Хвост шарфа развевается на бегу.
        scarfTail.xRot = -Math.min(1.2F, speed * 1.5F) - Mth.sin(age * 0.1F) * 0.05F;
    }
}
