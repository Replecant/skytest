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
 * Сгорбленная фигура: тонкие ноги, наклонённый вперёд торс в лохмотьях,
 * длинная обмотанная шея, вытянутая вперёд, маленькая голова под капюшоном.
 */
public class LamplighterModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "lamplighter"), "main");

    private static final float BODY_LEAN = 0.6F;
    private static final float NECK_LEAN = 0.55F;
    private static final float HEAD_LEVEL = -(BODY_LEAN + NECK_LEAN);
    private static final float ARM_HANG = -BODY_LEAN + 0.05F;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public LamplighterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offset(-2.0F, 10.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.offset(2.0F, 10.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(8, 0).addBox(-4.0F, -11.0F, -2.0F, 8.0F, 11.0F, 4.0F)
                        // Рваная юбка из тряпья.
                        .texOffs(0, 16).addBox(-4.5F, -3.0F, -2.5F, 9.0F, 7.0F, 5.0F)
                        // Накидка на плечах.
                        .texOffs(0, 28).addBox(-4.5F, -11.5F, -2.5F, 9.0F, 4.0F, 5.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, BODY_LEAN, 0.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(36, 32).addBox(-1.5F, -12.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -10.5F, -0.5F, NECK_LEAN, 0.0F, 0.0F));

        neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 5.0F, 6.0F)
                        // Капюшон закрывает верх головы, оставляя щель для глаза.
                        .texOffs(36, 16).addBox(-3.5F, -5.5F, -5.5F, 7.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -12.0F, 0.0F, HEAD_LEVEL, 0.0F, 0.0F));

        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(28, 16)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, -10.0F, 0.0F, ARM_HANG, 0.0F, 0.1F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 16).mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(5.0F, -10.0F, 0.0F, ARM_HANG, 0.0F, -0.1F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;

        // Медленное «дыхание» и покачивание шеи.
        body.xRot = BODY_LEAN + Mth.sin(age * 0.04F) * 0.025F;
        neck.xRot = NECK_LEAN + Mth.sin(age * 0.05F + 1.0F) * 0.05F;
        neck.zRot = Mth.sin(age * 0.023F) * 0.04F;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = HEAD_LEVEL + state.xRot * Mth.DEG_TO_RAD - (neck.xRot - NECK_LEAN);

        rightArm.xRot = ARM_HANG + Mth.cos(walk * 0.6662F) * 0.6F * walkSpeed + Mth.sin(age * 0.06F) * 0.03F;
        leftArm.xRot = ARM_HANG - Mth.cos(walk * 0.6662F) * 0.6F * walkSpeed + Mth.sin(age * 0.06F + 2.0F) * 0.03F;
        rightArm.zRot = 0.1F;
        leftArm.zRot = -0.1F;

        rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.0F * walkSpeed;
        leftLeg.xRot = -Mth.cos(walk * 0.6662F) * 1.0F * walkSpeed;
    }
}
