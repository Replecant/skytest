package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.renderer.PrisonWardenRenderState;
import com.replecant.skytest.entity.PrisonWardenEntity.State;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Страж Тюрьмы: широкий торс в ламеллярном доспехе с наплечниками и юбкой
 * из пластин, высокий колпак, ростовой щит в левой руке и меч в правой.
 * Каждое состояние ИИ — своя поза; переходы сглажены.
 */
public class PrisonWardenModel extends EntityModel<PrisonWardenRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "prison_warden"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart sword;
    private final ModelPart shield;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PrisonWardenModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.hat = head.getChild("hat");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.sword = rightArm.getChild("sword");
        this.shield = leftArm.getChild("shield");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-6.0F, -12.0F, -3.5F, 12.0F, 12.0F, 7.0F)
                        // Юбка из ламеллярных пластин.
                        .texOffs(38, 16).addBox(-6.5F, -3.0F, -4.0F, 13.0F, 6.0F, 8.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        // Высокий островерхий колпак.
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 3.0F, 9.0F)
                        .texOffs(68, 0).addBox(-3.5F, -14.0F, -3.5F, 7.0F, 3.0F, 7.0F)
                        .texOffs(96, 0).addBox(-2.5F, -17.0F, -2.5F, 5.0F, 3.0F, 5.0F)
                        .texOffs(96, 8).addBox(-1.5F, -20.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                        .texOffs(108, 8).addBox(-0.5F, -22.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.rotation(-0.12F, 0.0F, 0.0F));

        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(80, 16).addBox(-3.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F)
                        .texOffs(80, 33).addBox(-4.0F, -3.0F, -3.5F, 6.0F, 4.0F, 7.0F),
                PartPose.offset(-8.0F, -10.0F, 0.0F));
        // Меч в руке: клинок смотрит вперёд, когда рука опущена.
        rightArm.addOrReplaceChild("sword", CubeListBuilder.create()
                        .texOffs(56, 44).addBox(-0.5F, -1.0F, -18.0F, 1.0F, 2.0F, 16.0F)
                        .texOffs(90, 44).addBox(-0.5F, -2.5F, -2.0F, 1.0F, 5.0F, 1.0F)
                        .texOffs(94, 44).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offset(-0.5F, 9.0F, 0.0F));

        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(80, 16).mirror().addBox(-2.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F)
                        .texOffs(80, 33).mirror().addBox(-2.0F, -3.0F, -3.5F, 6.0F, 4.0F, 7.0F),
                PartPose.offset(8.0F, -10.0F, 0.0F));
        // Ростовой щит: в покое висит сбоку, в стойке поворачивается лицом вперёд.
        leftArm.addOrReplaceChild("shield", CubeListBuilder.create()
                        .texOffs(0, 51).addBox(-1.0F, -12.0F, -6.0F, 2.0F, 22.0F, 12.0F)
                        .texOffs(28, 51).addBox(-2.0F, -3.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offset(3.5F, 8.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 35)
                .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F), PartPose.offset(-3.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 35).mirror()
                .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F), PartPose.offset(3.0F, 13.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(PrisonWardenRenderState s) {
        super.setupAnim(s);
        float walk = s.walkAnimationPos;
        float speed = Math.min(1.0F, s.walkAnimationSpeed);
        float age = s.ageInTicks;
        float t = s.stateTime;

        // --- база: тяжёлая походка и дыхание.
        float stride = Mth.cos(walk * 0.5F);
        rightLeg.xRot = stride * 0.9F * speed;
        leftLeg.xRot = -stride * 0.9F * speed;
        rightLeg.zRot = 0.0F;
        leftLeg.zRot = 0.0F;
        body.y = 13.0F + Math.abs(Mth.sin(walk * 0.5F)) * 1.2F * speed + Mth.sin(age * 0.08F) * 0.25F;
        body.zRot = Mth.sin(walk * 0.5F) * 0.06F * speed;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        rightLeg.y = 13.0F;
        leftLeg.y = 13.0F;

        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        hat.zRot = Mth.sin(age * 0.05F) * 0.03F;

        // Поза по умолчанию (патруль): меч опущен вперёд, щит на боку.
        rightArm.xRot = -0.35F + stride * 0.35F * speed;
        rightArm.yRot = 0.0F;
        rightArm.zRot = 0.08F;
        leftArm.xRot = -stride * 0.3F * speed;
        leftArm.yRot = 0.0F;
        leftArm.zRot = -0.08F;
        float guard = 0.0F;

        switch (s.state) {
            case PATROL -> { }
            case GUARD -> {
                guard = ease(t / 6.0F);
                crouch(guard);
                // Меч занесён за щитом, остриё смотрит на противника.
                rightArm.xRot = Mth.lerp(guard, rightArm.xRot, -1.25F);
                rightArm.yRot = Mth.lerp(guard, 0.0F, 0.35F);
                head.xRot += 0.15F * guard;
            }
            case WINDUP -> {
                float p = ease(t / State.WINDUP.duration);
                guard = 1.0F - 0.6F * p;
                crouch(guard);
                // Меч поднимается над головой, корпус откидывается назад; в конце — дрожь.
                rightArm.xRot = Mth.lerp(p, -1.25F, -2.95F);
                rightArm.zRot = Mth.lerp(p, 0.08F, -0.35F);
                body.xRot = -0.18F * p;
                body.yRot = -0.25F * p;
                if (p > 0.8F) {
                    rightArm.xRot += Mth.sin(age * 3.0F) * 0.03F;
                }
            }
            case STRIKE -> {
                float p = easeIn(t / State.STRIKE.duration);
                // Резкий рубящий удар сверху вниз с поворотом корпуса.
                rightArm.xRot = Mth.lerp(p, -2.95F, -0.15F);
                rightArm.zRot = Mth.lerp(p, -0.35F, 0.2F);
                body.xRot = Mth.lerp(p, -0.18F, 0.38F);
                body.yRot = Mth.lerp(p, -0.25F, 0.3F);
                rightLeg.xRot = -0.45F * p;
                leftLeg.xRot = 0.35F * p;
                guard = 0.4F * (1.0F - p);
            }
            case RECOVERY -> {
                float p = ease(t / State.RECOVERY.duration);
                // Отдышка: меч у земли, плечи ходят, щит опущен — окно для атаки.
                rightArm.xRot = Mth.lerp(p, -0.15F, -0.5F);
                rightArm.zRot = Mth.lerp(p, 0.2F, 0.08F);
                body.xRot = Mth.lerp(p, 0.38F, 0.08F) + Mth.sin(age * 0.45F) * 0.04F;
                body.yRot = Mth.lerp(p, 0.3F, 0.0F);
                leftArm.xRot = 0.15F;
                guard = 0.6F * ease((t - 16) / 10.0F);
            }
            case BASH -> {
                guard = 1.0F;
                if (t < 7.0F) {
                    // Замах щитом: корпус уходит назад.
                    float p = ease(t / 7.0F);
                    crouch(1.0F);
                    body.yRot = -0.35F * p;
                    shieldPose(1.0F, 3.0F * p);
                    rightArm.xRot = -1.0F;
                } else {
                    // Толчок: щит выбрасывается вперёд всем весом.
                    float p = easeIn((t - 7.0F) / 3.0F);
                    crouch(1.0F - p * 0.5F);
                    body.yRot = Mth.lerp(p, -0.35F, 0.25F);
                    body.xRot = 0.3F * p;
                    shieldPose(1.0F, Mth.lerp(p, 3.0F, -5.0F));
                    rightArm.xRot = -0.8F;
                    rightLeg.xRot = 0.4F * p;
                    leftLeg.xRot = -0.6F * p;
                }
            }
            case STAGGER -> {
                float decay = 1.0F - Mth.clamp(t / State.STAGGER.duration, 0.0F, 1.0F);
                // Пошатывается назад, руки разлетаются, щит повис.
                body.xRot = -0.4F * decay + Mth.sin(t * 0.5F) * 0.08F * decay;
                body.zRot = Mth.sin(t * 0.35F) * 0.15F * decay;
                rightArm.xRot = -0.6F - Mth.sin(t * 0.7F) * 0.5F * decay;
                rightArm.zRot = 0.5F * decay;
                leftArm.zRot = -0.6F * decay;
                leftArm.xRot = 0.3F * decay;
                head.xRot -= 0.3F * decay;
                rightLeg.xRot = -0.3F * decay;
                leftLeg.xRot = 0.25F * decay;
            }
        }
        if (s.state != State.BASH) {
            shieldPose(guard, 0.0F);
        }
    }

    /** Присед в стойке: ноги шире, корпус ниже. */
    private void crouch(float amount) {
        body.y += 1.5F * amount;
        rightLeg.zRot = 0.12F * amount;
        leftLeg.zRot = -0.12F * amount;
        rightLeg.y = 13.0F + 0.5F * amount;
        leftLeg.y = 13.0F + 0.5F * amount;
    }

    /**
     * Положение щита: 0 — висит сбоку, 1 — закрывает корпус спереди.
     * push — дополнительный вынос вперёд (отрицательный — к противнику).
     */
    private void shieldPose(float guard, float push) {
        leftArm.xRot = Mth.lerp(guard, leftArm.xRot, -0.55F);
        leftArm.yRot = Mth.lerp(guard, 0.0F, -0.25F);
        shield.x = Mth.lerp(guard, 3.5F, -7.0F);
        shield.y = Mth.lerp(guard, 8.0F, 4.0F);
        shield.z = Mth.lerp(guard, 0.0F, -6.0F) + push;
        shield.yRot = Mth.lerp(guard, 0.0F, Mth.HALF_PI);
        shield.xRot = Mth.lerp(guard, 0.0F, 0.55F);
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float easeIn(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x;
    }
}
