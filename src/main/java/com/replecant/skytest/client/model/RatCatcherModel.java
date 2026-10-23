package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.renderer.RatCatcherRenderState;

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
 * Крысолов: тощий сгорбленный старик, сидящий по-турецки на полу камеры. Длинный латаный
 * серо-бурый плащ с карманами, рваный шарф, кожаный картуз с ушами, большие круглые латунные
 * очки с янтарными стёклами (светятся через слой rat_catcher_eyes.png), длинный кривой нос,
 * седая щетина, костлявые пальцы. На левом плече сидит крыса, вторая лежит у него на коленях.
 * Рядом в пол воткнут шест с железным фонарём-клеткой (огонёк тоже светится).
 *
 * <p>Геометрия и UV сгенерированы из спецификации (scratchpad/rat_catcher/rat_catcher.py), из
 * которой рисуется и текстура, и офлайн-превью. Масштаб 1.0: пол на y = 24, глаза на y ≈ 5.5.
 * Сущность никогда не ходит — только сидит.
 *
 * <p>Анимация: в покое — медленное покачивание и дыхание, голова склонена к крысе на коленях,
 * правая рука гладит её, крыса на плече озирается, принюхивается и дёргает хвостом; в разговоре
 * голова поднимается к игроку, правая рука жестикулирует. Переходы по modeTime.
 */
public class RatCatcherModel extends EntityModel<RatCatcherRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "rat_catcher"), "main");

    private static final float HUNCH = 0.40F;
    private static final float R_ARM_X = -0.30F;
    private static final float R_ARM_Z = -0.10F;
    private static final float R_FORE = -1.60F;
    private static final float R_FORE_YAW = -0.45F;
    private static final float L_ARM_X = -0.30F;
    private static final float L_FORE = -1.35F;

    private final ModelPart torso;
    private final ModelPart scarfTail;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart rightFingers;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftFingers;
    private final ModelPart shoulderRat;
    private final ModelPart shoulderRatHead;
    private final ModelPart shoulderRatTail;
    private final ModelPart shoulderRatTailTip;
    private final ModelPart lapRatHead;
    private final ModelPart lapRatTail;
    private final ModelPart lapRatTailTip;
    private final ModelPart lantern;

    public RatCatcherModel(ModelPart root) {
        super(root);
        ModelPart pelvis = root.getChild("pelvis");
        this.torso = pelvis.getChild("torso");
        this.scarfTail = torso.getChild("scarf_tail");
        this.head = torso.getChild("head");
        this.rightArm = torso.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.rightHand = rightForearm.getChild("right_hand");
        this.rightFingers = rightHand.getChild("right_fingers");
        this.leftArm = torso.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.leftFingers = leftForearm.getChild("left_hand").getChild("left_fingers");
        this.shoulderRat = torso.getChild("shoulder_rat");
        this.shoulderRatHead = shoulderRat.getChild("shoulder_rat_head");
        this.shoulderRatTail = shoulderRat.getChild("shoulder_rat_tail");
        this.shoulderRatTailTip = shoulderRatTail.getChild("shoulder_rat_tail_tip");
        ModelPart lapRat = pelvis.getChild("lap_rat");
        this.lapRatHead = lapRat.getChild("lap_rat_head");
        this.lapRatTail = lapRat.getChild("lap_rat_tail");
        this.lapRatTailTip = lapRatTail.getChild("lap_rat_tail_tip");
        this.lantern = root.getChild("pole").getChild("lantern");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create()
                .texOffs(74, 0).addBox(-5.0F, -6.0F, -3.5F, 10.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition rightThigh = pelvis.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(92, 24).addBox(-2.5F, -2.5F, -7.0F, 5.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(-3.5F, -3.0F, -2.0F, 0.0F, 0.62F, 0.0F));

        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, -2.0F, -9.0F, 4.0F, 4.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, -6.5F, 0.0F, -2.05F, 0.0F));

        PartDefinition leftThigh = pelvis.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(92, 24).mirror().addBox(-2.5F, -2.5F, -7.0F, 5.0F, 5.0F, 7.0F),
                PartPose.offsetAndRotation(3.5F, -3.0F, -2.0F, 0.0F, -0.62F, 0.0F));

        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, -2.0F, -9.0F, 4.0F, 4.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -6.5F, 0.0F, 2.05F, 0.0F));

        PartDefinition lap = pelvis.addOrReplaceChild("lap", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-6.5F, -6.5F, -8.5F, 13.0F, 3.0F, 8.0F),
                PartPose.ZERO);

        PartDefinition torso = pelvis.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -10.0F, -3.0F, 10.0F, 10.0F, 6.0F)
                .texOffs(74, 33).addBox(-1.5F, -12.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(60, 13).addBox(-5.5F, -11.0F, -3.5F, 11.0F, 3.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.5F, 0.4F, 0.0F, 0.0F));

        PartDefinition scarfTail = torso.addOrReplaceChild("scarf_tail", CubeListBuilder.create()
                .texOffs(116, 34).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -9.5F, -3.8F, 0.1F, 0.0F, 0.1F));

        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(32, 11).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F)
                .texOffs(108, 9).addBox(-3.0F, -0.5F, -4.5F, 6.0F, 3.0F, 1.0F)
                .texOffs(60, 23).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.3F))
                .texOffs(10, 36).addBox(-4.25F, -5.5F, -4.75F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.25F))
                .texOffs(10, 36).addBox(0.25F, -5.5F, -4.75F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.25F))
                .texOffs(96, 13).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.35F))
                .texOffs(86, 36).addBox(-3.5F, -6.5F, -6.5F, 7.0F, 1.0F, 2.0F)
                .texOffs(0, 29).addBox(-5.3F, -6.5F, -2.5F, 1.0F, 5.0F, 4.0F)
                .texOffs(0, 29).mirror().addBox(4.3F, -6.5F, -2.5F, 1.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -10.5F, -0.5F, 0.45F, 0.0F, 0.0F));

        PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(0, 38).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -3.25F, -3.5F, 0.35F, 0.0F, 0.12F));

        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(116, 24).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(-5.5F, -9.0F, 0.0F, -0.3F, 0.0F, -0.1F));

        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(42, 25).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, -1.6F, -0.45F, 0.0F));

        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(20, 38).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(122, 9).addBox(2.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 5.5F, 0.0F));

        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(58, 38).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 1.0F, 0.0F, 0.0F));

        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(116, 24).mirror().addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(5.5F, -9.0F, 0.0F, -0.3F, 0.0F, 0.1F));

        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(42, 25).mirror().addBox(-1.5F, -0.5F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, -1.35F, 0.5F, 0.0F));

        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(20, 38).mirror().addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(122, 9).mirror().addBox(-3.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 5.5F, 0.0F));

        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(58, 38).mirror().addBox(-2.5F, 0.0F, -0.5F, 5.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.9F, 0.0F, 0.0F));

        PartDefinition shoulderRat = torso.addOrReplaceChild("shoulder_rat", CubeListBuilder.create()
                .texOffs(26, 25).addBox(-1.5F, -3.0F, -2.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(4.5F, -11.2F, -0.5F, -0.4F, -0.35F, 0.0F));

        PartDefinition shoulderRatHead = shoulderRat.addOrReplaceChild("shoulder_rat_head", CubeListBuilder.create()
                .texOffs(104, 36).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F)
                .texOffs(60, 11).addBox(-0.5F, -0.5F, -3.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(64, 11).addBox(-1.5F, -1.8F, -1.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(64, 11).addBox(0.5F, -1.8F, -1.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -1.5F, -2.5F));

        PartDefinition shoulderRatTail = shoulderRat.addOrReplaceChild("shoulder_rat_tail", CubeListBuilder.create()
                .texOffs(70, 39).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -0.7F, 2.5F, -1.0F, 0.0F, 0.0F));

        PartDefinition shoulderRatTailTip = shoulderRatTail.addOrReplaceChild("shoulder_rat_tail_tip", CubeListBuilder.create()
                .texOffs(70, 39).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, -0.5F, 0.0F, 0.0F));

        PartDefinition lapRat = pelvis.addOrReplaceChild("lap_rat", CubeListBuilder.create()
                .texOffs(108, 0).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(1.0F, -6.5F, -8.0F, 0.0F, -1.15F, 0.0F));

        PartDefinition lapRatHead = lapRat.addOrReplaceChild("lap_rat_head", CubeListBuilder.create()
                .texOffs(104, 36).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F)
                .texOffs(60, 11).addBox(-0.5F, -0.5F, -3.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(64, 11).addBox(-1.5F, -1.8F, -1.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(64, 11).addBox(0.5F, -1.8F, -1.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -3.0F, 0.1F, 0.0F, 0.0F));

        PartDefinition lapRatTail = lapRat.addOrReplaceChild("lap_rat_tail", CubeListBuilder.create()
                .texOffs(70, 39).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -0.7F, 3.0F, -0.9F, 0.0F, 0.0F));

        PartDefinition lapRatTailTip = lapRatTail.addOrReplaceChild("lap_rat_tail_tip", CubeListBuilder.create()
                .texOffs(70, 39).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, -0.6F, 0.0F, 0.0F));

        PartDefinition pole = root.addOrReplaceChild("pole", CubeListBuilder.create()
                .texOffs(54, 25).addBox(-0.5F, -26.0F, -0.5F, 1.0F, 26.0F, 1.0F)
                .texOffs(38, 34).addBox(-0.5F, -25.5F, -4.5F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-8.5F, 24.0F, 1.5F, 0.0F, 0.0F, -0.12F));

        PartDefinition lantern = pole.addOrReplaceChild("lantern", CubeListBuilder.create()
                .texOffs(68, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                .texOffs(22, 33).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 1.0F, 4.0F)
                .texOffs(10, 29).addBox(-1.5F, 2.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                .texOffs(26, 16).addBox(-0.5F, 3.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                .texOffs(58, 33).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -25.0F, -4.0F, 0.0F, 0.0F, 0.12F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(RatCatcherRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        // 0 = возится с крысами, 1 = разговор; плавный переход за 10 тиков от момента смены режима.
        float talk = ease(s.modeTime / 10.0F);
        if (!s.talking) {
            talk = 1.0F - talk;
        }
        float idle = 1.0F - talk;
        float breath = Mth.sin(age * 0.09F);          // дыхание, ~3.5 с
        float rock = Mth.sin(age * 0.042F);           // медленное покачивание, ~7.5 с

        // --- корпус: сутулость, покачивание (в покое), дыхание; в разговоре чуть выпрямляется
        float hunch = Mth.lerp(talk, HUNCH, 0.26F) + rock * 0.035F * idle + breath * 0.015F;
        torso.xRot = hunch;
        torso.zRot = rock * 0.03F * idle;
        torso.y = -5.5F - breath * 0.2F;
        torso.yRot = talk * Mth.clamp(s.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.2F;

        // --- голова: склонена к крысе на коленях; в разговоре смотрит на игрока
        float idlePitch = 0.16F + Mth.sin(age * 0.031F) * 0.06F + breath * 0.02F;
        float idleYaw = -0.18F + Mth.sin(age * 0.019F) * 0.1F;
        float talkPitch = Mth.clamp(s.xRot, -25.0F, 35.0F) * Mth.DEG_TO_RAD - hunch + 0.1F + breath * 0.015F;
        float talkYaw = Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * 0.8F;
        head.xRot = Mth.lerp(talk, idlePitch, talkPitch);
        head.yRot = Mth.lerp(talk, idleYaw, talkYaw);
        head.zRot = idle * Mth.sin(age * 0.025F + 1.0F) * 0.05F + talk * 0.06F;

        // --- правая рука: гладит крысу (покой), жестикулирует (разговор)
        float stroke = Mth.sin(age * 0.14F);          // ~2.2 с на одно поглаживание
        rightArm.xRot = R_ARM_X - hunch + HUNCH + stroke * 0.10F * idle;
        rightArm.zRot = R_ARM_Z + stroke * 0.05F * idle;
        rightForearm.xRot = R_FORE - stroke * 0.22F * idle;
        rightHand.xRot = 0.15F * idle + stroke * 0.12F * idle;
        rightFingers.xRot = 1.0F + 0.2F * stroke * idle;
        float gest = ease(0.5F - 0.5F * Mth.cos(age * 0.075F)) * talk;   // 0..1, период ~4 с
        rightArm.xRot += -0.75F * gest - 0.1F * talk;
        rightArm.yRot = -0.2F * gest;
        rightArm.zRot += -0.55F * gest;
        rightForearm.xRot += -0.35F * gest + 0.05F * talk;
        rightForearm.yRot = R_FORE_YAW + 0.15F * gest;
        rightHand.zRot = 0.2F * gest;
        rightFingers.xRot += Mth.lerp(gest, 0.0F, -0.8F) - 0.3F * talk;

        // --- левая рука: держит крысу на коленях, дышит вместе с корпусом
        leftArm.xRot = L_ARM_X - hunch + HUNCH + breath * 0.02F;
        leftForearm.xRot = L_FORE + breath * 0.03F - 0.1F * talk;
        leftFingers.xRot = 0.9F + 0.1F * talk;

        // --- крыса на плече: озирается, принюхивается, дёргает хвостом; сидит горизонтально
        float look = Mth.sin(age * 0.05F) * 0.6F + Mth.sin(age * 0.013F) * 0.4F;
        shoulderRatHead.yRot = look;
        float sniff = Mth.sin(age * 1.1F);
        shoulderRatHead.xRot = (sniff > 0.8F ? 0.12F : 0.0F) + Mth.sin(age * 0.09F) * 0.08F;
        shoulderRat.xRot = -hunch;
        shoulderRatTail.yRot = Mth.sin(age * 0.21F) * 0.35F;
        shoulderRatTailTip.yRot = Mth.sin(age * 0.21F - 0.9F) * 0.45F;
        shoulderRatTail.xRot = -1.0F + hunch * 0.5F;

        // --- крыса на коленях: дремлет, хвост покачивается, поднимает голову, когда хозяин говорит
        lapRatHead.xRot = 0.1F - 0.15F * talk + (Mth.sin(age * 0.9F + 2.0F) > 0.85F ? 0.08F : 0.0F);
        lapRatTail.yRot = Mth.sin(age * 0.17F) * 0.25F;
        lapRatTailTip.yRot = Mth.sin(age * 0.17F - 0.8F) * 0.4F;

        // --- вторичное движение: хвост шарфа, фонарь качается на крюке
        scarfTail.xRot = 0.1F + Mth.sin(age * 0.08F) * 0.05F + gest * 0.1F;
        scarfTail.zRot = 0.1F + Mth.sin(age * 0.06F) * 0.06F;
        lantern.zRot = 0.12F + Mth.sin(age * 0.11F) * 0.07F;
        lantern.xRot = Mth.sin(age * 0.083F) * 0.06F;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
