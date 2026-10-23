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
 * Страж Тюрьмы: приземистый широкий воин в китайском ламеллярном доспехе —
 * наплечники со звериными масками, зерцало на груди, пластинчатая юбка из четырёх
 * подвижных панелей, высокий тёмно-фиолетовый колпак с загнутым кончиком, маска с
 * светящимися прорезями глаз. Ростовой щит в левой руке, дао в правой.
 *
 * <p>Геометрия и UV сгенерированы из спецификации (scratchpad/warden_v2/warden.py),
 * той же, из которой рисуется текстура и офлайн-превью. Ступни стоят на y = 24.
 * Каждое состояние ИИ — своя поза; переходы сглажены.
 */
public class PrisonWardenModel extends EntityModel<PrisonWardenRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "prison_warden"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart cap;
    private final ModelPart capTop;
    private final ModelPart capTip;
    private final ModelPart rightPauldron;
    private final ModelPart leftPauldron;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart sword;
    private final ModelPart shield;
    private final ModelPart skirtFront;
    private final ModelPart skirtBack;
    private final ModelPart skirtRight;
    private final ModelPart skirtLeft;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PrisonWardenModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.cap = head.getChild("cap");
        this.capTop = cap.getChild("cap_top");
        this.capTip = capTop.getChild("cap_tip");
        this.rightPauldron = body.getChild("right_pauldron");
        this.leftPauldron = body.getChild("left_pauldron");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.sword = rightArm.getChild("sword");
        this.shield = leftArm.getChild("shield");
        this.skirtFront = body.getChild("skirt_front");
        this.skirtBack = body.getChild("skirt_back");
        this.skirtRight = body.getChild("skirt_right");
        this.skirtLeft = body.getChild("skirt_left");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -10.0F, -3.5F, 12.0F, 10.0F, 7.0F)
                .texOffs(70, 50).addBox(-4.5F, -11.5F, -3.0F, 9.0F, 2.0F, 6.0F)
                .texOffs(48, 22).addBox(-2.5F, -8.5F, -4.5F, 5.0F, 5.0F, 1.0F)
                .texOffs(66, 17).addBox(-6.5F, -3.0F, -4.5F, 13.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F)
                .texOffs(0, 17).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -10.0F, 0.0F));

        PartDefinition cap = head.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(32, 28).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 2.0F, 10.0F)
                .texOffs(72, 28).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 3.0F, 8.0F)
                .texOffs(102, 44).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, -0.05F, 0.0F, 0.0F));

        PartDefinition capTop = cap.addOrReplaceChild("cap_top", CubeListBuilder.create()
                .texOffs(32, 22).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.35F, 0.0F, 0.0F));

        PartDefinition capTip = capTop.addOrReplaceChild("cap_tip", CubeListBuilder.create()
                .texOffs(20, 48).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F)
                .texOffs(20, 53).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, 0.55F, 0.0F, 0.0F));

        PartDefinition rightPauldron = body.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -2.0F, -3.5F, 8.0F, 4.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(-6.5F, -9.5F, 0.0F, 0.0F, 0.0F, -0.22F));

        PartDefinition leftPauldron = body.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(72, 39).addBox(-4.0F, -2.0F, -3.5F, 8.0F, 4.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(6.5F, -9.5F, 0.0F, 0.0F, 0.0F, 0.22F));

        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(104, 28).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 11.0F, 5.0F),
                PartPose.offset(-8.5F, -9.0F, 0.0F));

        PartDefinition sword = rightArm.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(66, 0).addBox(-0.5F, -1.5F, -17.0F, 1.0F, 3.0F, 14.0F)
                .texOffs(12, 57).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 1.0F)
                .texOffs(114, 52).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 5.0F)
                .texOffs(32, 17).addBox(-1.0F, -1.0F, 3.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 8.5F, 0.0F, 0.75F, 0.0F, 0.0F));

        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(30, 40).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 11.0F, 5.0F),
                PartPose.offset(8.5F, -9.0F, 0.0F));

        PartDefinition shield = leftArm.addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(38, 0).addBox(-6.0F, -11.0F, -1.5F, 12.0F, 20.0F, 2.0F)
                .texOffs(20, 43).addBox(-2.0F, -3.0F, -2.5F, 4.0F, 4.0F, 1.0F)
                .texOffs(60, 22).addBox(-1.0F, -3.0F, 0.5F, 2.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 7.0F, 0.0F, 0.0F, -Mth.HALF_PI, 0.0F));

        PartDefinition skirtFront = body.addOrReplaceChild("skirt_front", CubeListBuilder.create()
                .texOffs(20, 56).addBox(-5.0F, 0.0F, -4.5F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition skirtBack = body.addOrReplaceChild("skirt_back", CubeListBuilder.create()
                .texOffs(42, 56).addBox(-5.0F, 0.0F, 3.5F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition skirtRight = body.addOrReplaceChild("skirt_right", CubeListBuilder.create()
                .texOffs(110, 15).addBox(-6.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition skirtLeft = body.addOrReplaceChild("skirt_left", CubeListBuilder.create()
                .texOffs(100, 52).addBox(5.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(50, 40).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F)
                .texOffs(0, 57).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 2.0F, 1.0F),
                PartPose.offset(-3.0F, 15.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 43).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F)
                .texOffs(64, 58).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 2.0F, 1.0F),
                PartPose.offset(3.0F, 15.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(PrisonWardenRenderState s) {
        super.setupAnim(s);
        float walk = s.walkAnimationPos;
        float speed = Math.min(1.0F, s.walkAnimationSpeed);
        float age = s.ageInTicks;
        float t = s.stateTime;

        // --- база: тяжёлая походка, дыхание, качание колпака и юбки.
        float stride = Mth.cos(walk * 0.5F);
        rightLeg.xRot = stride * 0.9F * speed;
        leftLeg.xRot = -stride * 0.9F * speed;
        rightLeg.zRot = 0.0F;
        leftLeg.zRot = 0.0F;
        rightLeg.y = 15.0F;
        leftLeg.y = 15.0F;
        body.y = 15.0F + Math.abs(Mth.sin(walk * 0.5F)) * 1.2F * speed + Mth.sin(age * 0.08F) * 0.25F;
        body.zRot = Mth.sin(walk * 0.5F) * 0.06F * speed;
        body.xRot = 0.0F;
        body.yRot = 0.0F;

        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;

        float swing = Math.abs(stride) * speed;
        skirtFront.xRot = -0.45F * swing;
        skirtBack.xRot = 0.35F * swing;
        skirtRight.zRot = 0.0F;
        skirtLeft.zRot = 0.0F;

        capTop.xRot = 0.35F + Mth.sin(age * 0.07F) * 0.04F + 0.15F * speed;
        capTop.zRot = Mth.sin(age * 0.05F) * 0.05F;
        capTip.xRot = 0.55F + Mth.sin(age * 0.07F + 1.0F) * 0.08F + 0.25F * speed;
        capTip.zRot = Mth.sin(age * 0.05F + 0.7F) * 0.08F;
        rightPauldron.zRot = -0.22F;
        leftPauldron.zRot = 0.22F;

        // Поза по умолчанию (патруль): меч опущен остриём вперёд-вниз, щит висит на боку.
        rightArm.xRot = -0.3F + stride * 0.35F * speed;
        rightArm.yRot = 0.0F;
        rightArm.zRot = 0.1F;
        leftArm.xRot = -stride * 0.3F * speed;
        leftArm.yRot = 0.0F;
        leftArm.zRot = -0.1F;
        // Клинок держит постоянный угол к земле, пока рука качается при ходьбе.
        sword.xRot = 0.75F - stride * 0.35F * speed;
        float guard = 0.0F;

        switch (s.state) {
            case PATROL -> { }
            case GUARD -> {
                guard = ease(t / 6.0F);
                crouch(guard);
                // Меч занесён за щитом, остриё смотрит на противника.
                rightArm.xRot = Mth.lerp(guard, rightArm.xRot, -1.25F);
                rightArm.yRot = Mth.lerp(guard, 0.0F, 0.35F);
                sword.xRot = Mth.lerp(guard, sword.xRot, 0.6F);
                head.xRot += 0.15F * guard;
                body.xRot = 0.08F * guard;
            }
            case WINDUP -> {
                float p = ease(t / State.WINDUP.duration);
                guard = 1.0F - 0.6F * p;
                crouch(guard);
                // Меч поднимается над головой, корпус откидывается назад; в конце — дрожь.
                rightArm.xRot = Mth.lerp(p, -1.25F, -2.95F);
                rightArm.zRot = Mth.lerp(p, 0.1F, -0.35F);
                sword.xRot = Mth.lerp(p, 0.6F, 0.9F);
                body.xRot = -0.18F * p;
                body.yRot = -0.25F * p;
                rightPauldron.zRot = -0.22F - 0.3F * p;
                capTop.xRot += 0.2F * p;
                if (p > 0.8F) {
                    rightArm.xRot += Mth.sin(age * 3.0F) * 0.03F;
                }
            }
            case STRIKE -> {
                float p = easeIn(t / State.STRIKE.duration);
                // Резкий рубящий удар сверху вниз с поворотом корпуса; колпак и юбка летят следом.
                rightArm.xRot = Mth.lerp(p, -2.95F, -0.15F);
                rightArm.zRot = Mth.lerp(p, -0.35F, 0.2F);
                sword.xRot = Mth.lerp(p, 0.9F, 0.45F);
                body.xRot = Mth.lerp(p, -0.18F, 0.38F);
                body.yRot = Mth.lerp(p, -0.25F, 0.3F);
                rightLeg.xRot = -0.45F * p;
                leftLeg.xRot = 0.35F * p;
                skirtFront.xRot = -0.5F * p;
                skirtBack.xRot = 0.3F * p;
                capTop.xRot = 0.2F - 0.4F * p;
                capTip.xRot = 0.4F - 0.5F * p;
                guard = 0.4F * (1.0F - p);
            }
            case RECOVERY -> {
                float p = ease(t / State.RECOVERY.duration);
                float breath = Mth.sin(age * 0.45F);
                // Отдышка: меч у земли, плечи ходят, щит опущен — окно для атаки.
                rightArm.xRot = Mth.lerp(p, -0.15F, -0.5F);
                rightArm.zRot = Mth.lerp(p, 0.2F, 0.1F);
                sword.xRot = Mth.lerp(p, 0.45F, 0.75F);
                body.xRot = Mth.lerp(p, 0.38F, 0.08F) + breath * 0.04F;
                body.yRot = Mth.lerp(p, 0.3F, 0.0F);
                rightPauldron.zRot = -0.22F - breath * 0.05F;
                leftPauldron.zRot = 0.22F + breath * 0.05F;
                leftArm.xRot = 0.15F;
                head.xRot += 0.2F * (1.0F - p);
                guard = 0.6F * ease((t - 16) / 10.0F);
            }
            case BASH -> {
                guard = 1.0F;
                if (t < 5.0F) {
                    // Замах щитом: корпус уходит назад.
                    float p = ease(t / 5.0F);
                    crouch(1.0F);
                    body.yRot = -0.35F * p;
                    shieldPose(1.0F, 3.0F * p);
                    rightArm.xRot = -1.0F;
                    sword.xRot = 0.6F;
                } else {
                    // Толчок: щит выбрасывается вперёд всем весом.
                    float p = easeIn((t - 5.0F) / 3.0F);
                    crouch(1.0F - p * 0.5F);
                    body.yRot = Mth.lerp(p, -0.35F, 0.25F);
                    body.xRot = 0.3F * p;
                    shieldPose(1.0F, Mth.lerp(p, 3.0F, -5.0F));
                    rightArm.xRot = -0.8F;
                    sword.xRot = 0.6F;
                    rightLeg.xRot = 0.4F * p;
                    leftLeg.xRot = -0.6F * p;
                    capTop.xRot = 0.35F - 0.3F * p;
                }
            }
            case PARRIED -> {
                // Парирован: падает на колено, опираясь на меч; щит повис. В конце — поднимается.
                float duration = State.PARRIED.duration;
                float k = ease(t / 4.0F) * (1.0F - ease((t - (duration - 10.0F)) / 10.0F));
                body.y += 4.5F * k;
                body.xRot = 0.35F * k + Mth.sin(age * 0.4F) * 0.03F * k;
                rightLeg.y = 15.0F + 4.5F * k;
                leftLeg.y = 15.0F + 4.5F * k;
                rightLeg.xRot = -1.35F * k;   // колено вперёд
                leftLeg.xRot = 0.95F * k;     // другое колено на полу
                rightArm.xRot = Mth.lerp(k, rightArm.xRot, -0.55F);
                rightArm.zRot = 0.1F;
                sword.xRot = Mth.lerp(k, sword.xRot, 1.55F); // клинок воткнут в пол
                leftArm.xRot = 0.35F * k;
                leftArm.zRot = -0.35F * k;
                head.xRot += 0.45F * k;
                capTop.xRot = 0.35F + 0.45F * k;
                capTip.xRot = 0.55F + 0.35F * k;
            }
            case STAGGER -> {
                float decay = 1.0F - Mth.clamp(t / State.STAGGER.duration, 0.0F, 1.0F);
                // Пошатывается назад, руки разлетаются, щит повис, колпак мотается.
                body.xRot = -0.4F * decay + Mth.sin(t * 0.5F) * 0.08F * decay;
                body.zRot = Mth.sin(t * 0.35F) * 0.15F * decay;
                rightArm.xRot = -0.6F - Mth.sin(t * 0.7F) * 0.5F * decay;
                rightArm.zRot = 0.5F * decay;
                leftArm.zRot = -0.6F * decay;
                leftArm.xRot = 0.3F * decay;
                head.xRot -= 0.3F * decay;
                rightLeg.xRot = -0.3F * decay;
                leftLeg.xRot = 0.25F * decay;
                capTop.xRot = 0.35F + 0.35F * decay;
                capTip.xRot = 0.55F + Mth.sin(t * 0.6F) * 0.3F * decay;
            }
        }
        if (s.state != State.BASH) {
            shieldPose(guard, 0.0F);
        }
    }

    /** Присед в стойке: ноги шире, корпус ниже, юбка расходится; ступни остаются на земле. */
    private void crouch(float amount) {
        body.y += 1.5F * amount;
        rightLeg.zRot = 0.12F * amount;
        leftLeg.zRot = -0.12F * amount;
        rightLeg.y = 15.0F - 0.4F * amount;
        leftLeg.y = 15.0F - 0.4F * amount;
        skirtRight.zRot = 0.1F * amount;
        skirtLeft.zRot = -0.1F * amount;
        skirtFront.xRot -= 0.2F * amount;
    }

    /**
     * Положение щита: 0 — висит на боку лицевой стороной наружу, 1 — закрывает корпус спереди.
     * push — дополнительный вынос вперёд (отрицательный — к противнику).
     */
    private void shieldPose(float guard, float push) {
        leftArm.xRot = Mth.lerp(guard, leftArm.xRot, -0.55F);
        leftArm.yRot = Mth.lerp(guard, 0.0F, -0.25F);
        shield.x = Mth.lerp(guard, 3.0F, -7.5F);
        shield.y = Mth.lerp(guard, 7.0F, 3.0F);
        shield.z = Mth.lerp(guard, 0.0F, -4.5F) + push;
        shield.yRot = Mth.lerp(guard, -Mth.HALF_PI, 0.0F);
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
