package com.replecant.skytest.client.model;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.client.renderer.RegulatorRenderState;

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
 * Регулятор: огромный сгорбленный великан, сидящий на ржавой шестерне. Каменно-серая кожа,
 * медные наплечники и наручи, кожаный фартук, цепи на поясе и на левом предплечье, связка
 * ключей, рычаг за спиной. На лице — резная деревянная маска-тики с глубоко посаженными
 * светящимися глазами (слой regulator_eyes.png).
 *
 * <p>Геометрия и UV сгенерированы из спецификации (scratchpad/regulator/regulator.py), из
 * которой рисуется и текстура, и офлайн-превью. Модель спроектирована в масштабе ~40 px
 * сидя; рендерер должен масштабировать её на 1.6 (≈4 блока). Ступни стоят на y = 24,
 * шестерня-сиденье входит в модель.
 *
 * <p>Анимация: грустное дыхание, вздохи каждые ~9.5 с, пальцы правой руки барабанят по колену;
 * при разговоре голова поднимается к игроку, левая рука жестикулирует. Переходы по modeTime.
 */
public class RegulatorModel extends EntityModel<RegulatorRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "regulator"), "main");

    private static final float HUNCH = 0.24F;
    private static final float FORE = -1.45F;

    private final ModelPart pelvis;
    private final ModelPart keys;
    private final ModelPart chainBelt;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightShoulder;
    private final ModelPart leftShoulder;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart rightFingers;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart chainArm;
    private final ModelPart leftHand;
    private final ModelPart leftFingers;

    public RegulatorModel(ModelPart root) {
        super(root);
        this.pelvis = root.getChild("pelvis");
        this.keys = pelvis.getChild("keys");
        this.chainBelt = pelvis.getChild("chain_belt");
        this.torso = pelvis.getChild("torso");
        this.head = torso.getChild("head");
        this.rightShoulder = torso.getChild("right_shoulder");
        this.leftShoulder = torso.getChild("left_shoulder");
        this.rightArm = torso.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.rightHand = rightForearm.getChild("right_hand");
        this.rightFingers = rightHand.getChild("right_fingers");
        this.leftArm = torso.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.chainArm = leftForearm.getChild("chain_arm");
        this.leftHand = leftForearm.getChild("left_hand");
        this.leftFingers = leftHand.getChild("left_fingers");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition seat = root.addOrReplaceChild("seat", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-9.0F, -6.0F, -9.0F, 18.0F, 6.0F, 18.0F)
                .texOffs(0, 46).addBox(-2.0F, -5.5F, -12.0F, 4.0F, 5.0F, 3.0F)
                .texOffs(0, 46).addBox(-2.0F, -5.5F, 9.0F, 4.0F, 5.0F, 3.0F)
                .texOffs(234, 45).addBox(-12.0F, -5.5F, -2.0F, 3.0F, 5.0F, 4.0F)
                .texOffs(234, 45).addBox(9.0F, -5.5F, -2.0F, 3.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 24.0F, 5.0F));

        PartDefinition seatCross = seat.addOrReplaceChild("seat_cross", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-9.0F, -6.0F, -9.0F, 18.0F, 6.0F, 18.0F)
                .texOffs(0, 46).addBox(-2.0F, -5.5F, -12.0F, 4.0F, 5.0F, 3.0F)
                .texOffs(0, 46).addBox(-2.0F, -5.5F, 9.0F, 4.0F, 5.0F, 3.0F)
                .texOffs(234, 45).addBox(-12.0F, -5.5F, -2.0F, 3.0F, 5.0F, 4.0F)
                .texOffs(234, 45).addBox(9.0F, -5.5F, -2.0F, 3.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.785F, 0.0F));

        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create()
                .texOffs(136, 0).addBox(-10.0F, -7.0F, -5.0F, 20.0F, 7.0F, 11.0F)
                .texOffs(136, 18).addBox(-10.5F, -4.0F, -5.5F, 21.0F, 3.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition keys = pelvis.addOrReplaceChild("keys", CubeListBuilder.create()
                .texOffs(248, 45).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 7.0F, 1.0F),
                PartPose.offset(8.0F, -1.5F, -5.5F));

        PartDefinition chainBelt = pelvis.addOrReplaceChild("chain_belt", CubeListBuilder.create()
                .texOffs(24, 46).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 8.0F, 2.0F),
                PartPose.offset(-10.5F, -2.5F, -1.0F));

        PartDefinition apron = pelvis.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(84, 42).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -5.5F, -1.15F, 0.0F, 0.0F));

        PartDefinition torso = pelvis.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, -16.0F, -6.0F, 20.0F, 16.0F, 12.0F)
                .texOffs(60, 41).addBox(-3.0F, -19.0F, -3.5F, 6.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 1.0F, 0.24F, 0.0F, 0.0F));

        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 24).addBox(-5.0F, -9.0F, -4.0F, 10.0F, 9.0F, 8.0F)
                .texOffs(100, 24).addBox(-6.5F, -11.0F, -6.5F, 13.0F, 15.0F, 3.0F)
                .texOffs(202, 45).addBox(-7.0F, -6.0F, -8.0F, 14.0F, 2.0F, 2.0F)
                .texOffs(180, 45).addBox(-4.0F, -14.0F, -6.0F, 8.0F, 3.0F, 3.0F)
                .texOffs(30, 46).addBox(-8.5F, -9.0F, -5.5F, 2.0F, 8.0F, 1.0F)
                .texOffs(36, 46).addBox(6.5F, -9.0F, -5.5F, 2.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -17.0F, -0.5F, 0.3F, 0.0F, 0.0F));

        PartDefinition rightShoulder = torso.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(198, 0).addBox(-5.0F, -3.0F, -5.5F, 10.0F, 6.0F, 11.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(-10.0F, -14.0F, 0.0F, 0.0F, 0.0F, -0.15F));

        PartDefinition leftShoulder = torso.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(198, 0).mirror().addBox(-5.0F, -3.0F, -5.5F, 10.0F, 6.0F, 11.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(10.0F, -14.0F, 0.0F, 0.0F, 0.0F, 0.15F));

        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(132, 33).addBox(-3.5F, -2.0F, -3.5F, 7.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(-12.0F, -12.0F, 1.0F, -0.24F, 0.0F, -0.24F));

        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, -1.45F, 0.0F, 0.0F));

        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(224, 35).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 5.0F, 5.0F)
                .texOffs(14, 46).addBox(4.5F, 0.0F, 0.0F, 2.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));

        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(106, 42).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 1.5F, 0.85F, 0.0F, 0.0F));

        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(132, 33).mirror().addBox(-3.5F, -2.0F, -3.5F, 7.0F, 8.0F, 7.0F),
                PartPose.offsetAndRotation(12.0F, -12.0F, 1.0F, -0.24F, 0.0F, 0.24F));

        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 28).mirror().addBox(-4.0F, -1.0F, -4.0F, 8.0F, 10.0F, 8.0F)
                .texOffs(160, 33).addBox(-4.0F, 3.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, -1.45F, 0.0F, 0.0F));

        PartDefinition chainArm = leftForearm.addOrReplaceChild("chain_arm", CubeListBuilder.create()
                .texOffs(42, 46).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 7.0F, 2.0F),
                PartPose.offsetAndRotation(4.5F, 5.0F, 0.0F, 1.45F, 0.0F, 0.0F));

        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(224, 35).mirror().addBox(-4.5F, 0.0F, -1.0F, 9.0F, 5.0F, 5.0F)
                .texOffs(14, 46).mirror().addBox(-6.5F, 0.0F, 0.0F, 2.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));

        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(106, 42).mirror().addBox(-4.5F, 0.0F, -2.5F, 9.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 1.5F, 0.85F, 0.0F, 0.0F));

        PartDefinition lever = torso.addOrReplaceChild("lever", CubeListBuilder.create()
                .texOffs(240, 0).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 26.0F, 3.0F)
                .texOffs(160, 44).addBox(-2.5F, -21.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(3.0F, -4.0F, 6.5F, 0.0F, 0.0F, 0.5F));

        PartDefinition rightLeg = pelvis.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(202, 17).addBox(-4.0F, -4.0F, -10.0F, 8.0F, 7.0F, 11.0F),
                PartPose.offset(-7.5F, -2.0F, -2.0F));

        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(32, 28).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 11.0F, 7.0F)
                .texOffs(192, 35).addBox(-4.0F, 6.0F, -6.5F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));

        PartDefinition leftLeg = pelvis.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(202, 17).mirror().addBox(-4.0F, -4.0F, -10.0F, 8.0F, 7.0F, 11.0F),
                PartPose.offset(7.5F, -2.0F, -2.0F));

        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(32, 28).mirror().addBox(-3.5F, -3.0F, -3.5F, 7.0F, 11.0F, 7.0F)
                .texOffs(192, 35).mirror().addBox(-4.0F, 6.0F, -6.5F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));

        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(RegulatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        // 0 = грустная поза, 1 = разговор; плавный переход за 10 тиков от момента смены режима.
        float talk = ease(s.modeTime / 10.0F);
        if (!s.talking) {
            talk = 1.0F - talk;
        }
        float sad = 1.0F - talk;
        float breath = Mth.sin(age * 0.07F);          // цикл ~4.5 с
        float ph = age % 190.0F;                      // вздох каждые 9.5 с
        float sg = sighCurve(ph) * sad;                // плечи поднимаются
        float sgDrop = 0.0F;                           // тяжёлый выдох после вздоха
        if (ph >= 30.0F && ph < 56.0F) {
            sgDrop = Mth.sin((ph - 30.0F) / 26.0F * Mth.PI) * sad;
        }

        // --- корпус: сутулость, дыхание, вздох
        float hunch = Mth.lerp(talk, HUNCH, 0.12F) - 0.07F * sg + breath * 0.02F;
        torso.xRot = hunch;
        torso.y = -6.0F - breath * 0.35F - sg * 0.8F + sgDrop * 0.3F;
        torso.yRot = talk * Mth.clamp(s.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.25F;
        rightShoulder.y = -14.0F - sg * 1.6F - breath * 0.2F;
        leftShoulder.y = -14.0F - sg * 1.6F - breath * 0.2F;
        rightShoulder.zRot = -0.15F - sg * 0.12F;
        leftShoulder.zRot = 0.15F + sg * 0.12F;

        // --- голова: опущена и медленно покачивается в грусти; в разговоре смотрит на игрока
        float sadPitch = 0.3F + breath * 0.03F + Mth.sin(age * 0.021F) * 0.05F - 0.22F * sg + 0.08F * sgDrop;
        float sadYaw = Mth.sin(age * 0.016F) * 0.12F;
        float talkPitch = Mth.clamp(s.xRot, -25.0F, 35.0F) * Mth.DEG_TO_RAD - hunch + 0.05F + breath * 0.02F;
        float talkYaw = Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.lerp(talk, sadPitch, talkPitch);
        head.yRot = Mth.lerp(talk, sadYaw, talkYaw);
        head.zRot = sad * Mth.sin(age * 0.016F + 1.0F) * 0.04F;

        // --- руки: плечи остаются вертикальными независимо от сутулости
        rightArm.xRot = -hunch + 0.02F * breath;
        leftArm.xRot = -hunch + 0.02F * breath;
        rightArm.zRot = -0.24F - sg * 0.06F;
        leftArm.zRot = 0.24F + sg * 0.06F;
        rightForearm.xRot = FORE;
        leftForearm.xRot = FORE;
        // правая рука: пальцы барабанят по колену короткими сериями (только в грусти)
        float burst = Mth.clamp(Mth.sin(age * 0.05F) * 2.0F - 0.6F, 0.0F, 1.0F);
        float drum = burst * (0.5F + 0.5F * Mth.sin(age * 0.6F));
        rightFingers.xRot = 0.85F + 0.35F * drum * sad;
        rightHand.xRot = -0.1F * drum * sad;
        // левая рука: в разговоре медленно поднимается, ладонь раскрывается к собеседнику
        float gest = 0.5F - 0.5F * Mth.cos(age * 0.08F);
        gest = ease(gest) * talk;
        leftArm.xRot += -0.55F * gest;
        leftArm.yRot = -0.30F * gest;
        leftArm.zRot += 0.25F * gest;
        leftForearm.xRot = FORE - 0.75F * gest;
        leftForearm.yRot = 0.35F * gest;
        leftFingers.xRot = Mth.lerp(gest, 0.85F, -0.15F);
        leftHand.xRot = -0.15F * gest;
        // правая кисть чуть сжимается и разжимается на колене во время разговора
        rightFingers.xRot += 0.25F * talk * (0.5F - 0.5F * Mth.cos(age * 0.08F + 1.5F));

        // --- вторичное движение: цепи висят вертикально и покачиваются, ключи качаются
        chainArm.xRot = -(torso.xRot + leftArm.xRot + leftForearm.xRot) + Mth.sin(age * 0.13F) * 0.06F + gest * 0.25F;
        chainArm.zRot = -(leftArm.zRot + leftArm.yRot * 0.3F) + Mth.sin(age * 0.09F) * 0.08F + gest * 0.35F;
        chainBelt.xRot = Mth.sin(age * 0.11F) * 0.05F + sgDrop * 0.08F;
        chainBelt.zRot = Mth.sin(age * 0.085F) * 0.06F;
        keys.xRot = Mth.sin(age * 0.1F + 0.5F) * 0.08F + sgDrop * 0.1F;
    }

    /** Огибающая вздоха: плечи поднимаются 22 тика, держатся, опускаются 26 тиков; период 190 тиков. */
    private static float sighCurve(float ph) {
        if (ph < 22.0F) {
            return ease(ph / 22.0F);
        }
        if (ph < 30.0F) {
            return 1.0F;
        }
        if (ph < 56.0F) {
            return 1.0F - ease((ph - 30.0F) / 26.0F);
        }
        return 0.0F;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
