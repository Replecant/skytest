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
 *
 * <p>Катсцена (pulling/pullTime): встаёт с шестерни, тянется вверх, хватает две цепи с потолка
 * ({@code right_chain_links}/{@code left_chain_links}, видны только в руках) и дважды дёргает их вниз
 * (удары на pullTime = 34 и 56), затем держит; при сбросе флага отпускает цепи (они уезжают вверх)
 * и за 25 тиков садится обратно. Все суставы рук вращаются только вокруг X, поэтому цепь
 * удерживается вертикальной простой компенсацией углов.
 */
public class RegulatorModel extends EntityModel<RegulatorRenderState> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(SkyTest.MODID, "regulator"), "main");

    private static final float HUNCH = 0.24F;
    private static final float FORE = -1.45F;

    // --- катсцена с цепями (см. applyPull)
    private static final float RELEASE_T = 25.0F;   // тиков на то, чтобы отпустить цепи и сесть
    private static final float STAND_L = 1.35F;     // угол бедра стоя (0 = сидя), колени чуть согнуты
    private static final float STAND_T = 0.08F;     // наклон корпуса стоя
    private static final float STAND_H = -0.05F;    // наклон головы стоя
    private static final float UP_A = -2.9F;        // руки подняты: плечо
    private static final float UP_F = -0.25F;       // руки подняты: предплечье
    private static final float Y1_A = -0.3F;        // низ первого рывка: кулаки у груди
    private static final float Y1_F = -2.2F;
    private static final float Y1_H = -0.2F;
    private static final float Y1_L = 1.25F;
    private static final float Y1_T = 0.25F;
    private static final float Y2_A = 1.0F;         // низ второго рывка: локти назад, кулаки у живота
    private static final float Y2_F = -2.8F;
    private static final float Y2_H = -0.5F;
    private static final float Y2_L = 0.9F;
    private static final float Y2_T = 0.4F;
    private static final float FIST = 1.3F;         // пальцы сжаты на цепи
    private static final float GRIP_Y = 3.5F;        // точка захвата цепи в кулаке (локально для кисти)
    private static final float GRIP_Z = 5.5F;

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
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;
    private final ModelPart rightChain;
    private final ModelPart rightChainLinks;
    private final ModelPart leftChain;
    private final ModelPart leftChainLinks;

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
        this.rightLeg = pelvis.getChild("right_leg");
        this.rightShin = rightLeg.getChild("right_shin");
        this.leftLeg = pelvis.getChild("left_leg");
        this.leftShin = leftLeg.getChild("left_shin");
        this.rightChain = rightHand.getChild("right_chain");
        this.rightChainLinks = rightChain.getChild("right_chain_links");
        this.leftChain = leftHand.getChild("left_chain");
        this.leftChainLinks = leftChain.getChild("left_chain_links");
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

        PartDefinition rightChain = rightHand.addOrReplaceChild("right_chain", CubeListBuilder.create(),
                PartPose.offset(0.0F, 3.5F, 5.5F));

        PartDefinition rightChainLinks = rightChain.addOrReplaceChild("right_chain_links", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-2.0F, -58.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -58.0F, -2.0F, 1.0F, 60.0F, 4.0F)
                .texOffs(0, 64).addBox(-2.0F, -118.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -118.0F, -2.0F, 1.0F, 60.0F, 4.0F)
                .texOffs(0, 64).addBox(-2.0F, -178.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -178.0F, -2.0F, 1.0F, 60.0F, 4.0F),
                PartPose.ZERO);

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

        PartDefinition leftChain = leftHand.addOrReplaceChild("left_chain", CubeListBuilder.create(),
                PartPose.offset(0.0F, 3.5F, 5.5F));

        PartDefinition leftChainLinks = leftChain.addOrReplaceChild("left_chain_links", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-2.0F, -58.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -58.0F, -2.0F, 1.0F, 60.0F, 4.0F)
                .texOffs(0, 64).addBox(-2.0F, -118.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -118.0F, -2.0F, 1.0F, 60.0F, 4.0F)
                .texOffs(0, 64).addBox(-2.0F, -178.0F, -0.5F, 4.0F, 60.0F, 1.0F)
                .texOffs(12, 64).addBox(-0.5F, -178.0F, -2.0F, 1.0F, 60.0F, 4.0F),
                PartPose.ZERO);

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

        // --- катсцена: встаёт и тянет цепи с потолка (поверх позы выше, с плавным смешиванием)
        rightChainLinks.visible = false;
        leftChainLinks.visible = false;
        if (s.pulling || s.pullTime < RELEASE_T) {
            applyPull(s.pulling, s.pullTime, age);
        }
        // цепи с потолка остаются вертикальными при любом положении рук (суставы рук вращаются вокруг X)
        rightChain.xRot = -(torso.xRot + rightArm.xRot + rightForearm.xRot + rightHand.xRot);
        rightChain.zRot = -rightArm.zRot;
        leftChain.xRot = -(torso.xRot + leftArm.xRot + leftForearm.xRot + leftHand.xRot);
        leftChain.zRot = -leftArm.zRot;

        // --- вторичное движение: цепи висят вертикально и покачиваются, ключи качаются
        chainArm.xRot = -(torso.xRot + leftArm.xRot + leftForearm.xRot) + Mth.sin(age * 0.13F) * 0.06F + gest * 0.25F;
        chainArm.zRot = -(leftArm.zRot + leftArm.yRot * 0.3F) + Mth.sin(age * 0.09F) * 0.08F + gest * 0.35F;
        chainBelt.xRot = Mth.sin(age * 0.11F) * 0.05F + sgDrop * 0.08F;
        chainBelt.zRot = Mth.sin(age * 0.085F) * 0.06F;
        keys.xRot = Mth.sin(age * 0.1F + 0.5F) * 0.08F + sgDrop * 0.1F;
    }

    /**
     * Поза катсцены как функция pullTime (тики с момента смены флага pulling). Пока pulling:
     * 0–18 встаёт, 18–28 тянется к цепям и сжимает кулаки, 28–34 первый рывок (удар на 34),
     * 34–48 перехватывает цепи выше (правая рука первой), 48–56 второй рывок (удар на 56),
     * дальше держит цепи внизу, тяжело дыша. После сброса флага: 0–25 отпускает цепи и садится.
     * Ноги и таз задаются напрямую (ступни остаются на полу), остальное смешивается с позой покоя.
     */
    private void applyPull(boolean pulling, float pt, float age) {
        float w = 1.0F;                // вес позы катсцены поверх позы покоя
        float wz = 1.0F;               // то же для разворота плеч (держим 0, пока цепи в руках)
        float l = STAND_L;             // угол бедра
        float t = STAND_T;             // наклон корпуса
        float hx = STAND_H;            // наклон головы
        float shY = 0.0F;              // подъём плеч
        float shZ = 0.0F;              // разворот наплечников
        float za = 0.0F;               // разворот плеч наружу
        float aR = -0.1F, aL = -0.1F;  // плечо (руки опущены)
        float fR = -0.5F, fL = -0.5F;  // предплечье
        float hR = 0.0F, hL = 0.0F;    // кисть
        float fiR = 0.85F, fiL = 0.85F; // пальцы
        float linksR = 0.0F, linksL = 0.0F; // сдвиг цепи вдоль её оси (+ = цепь ниже относительно кулака)
        boolean chains = false;
        float heave = 0.5F + 0.5F * Mth.sin(age * 0.42F);  // тяжёлое дыхание в удержании
        float trem = Mth.sin(age * 2.3F) * 0.03F;           // дрожь от усилия
        // опорные высоты захвата: руки подняты и низ первого рывка
        float gUp = gripY(pelvisY(STAND_L), -6.0F, STAND_T, UP_A, UP_F, 0.0F);
        float gY1 = gripY(pelvisY(Y1_L), -6.0F, Y1_T, Y1_A, Y1_F, Y1_H);
        float d1 = gY1 - gUp;          // на сколько первый рывок стянул цепь вниз

        if (pulling) {
            if (pt < 18.0F) {
                // встаёт с сиденья: сначала качается вперёд, потом выпрямляется
                float r = ease(pt / 18.0F);
                w = r;
                wz = r;
                l = STAND_L * r;
                float rock = Mth.sin(r * Mth.PI);
                t = STAND_T + 0.28F * rock;
                hx = STAND_H - 0.25F * rock;
                za = 0.2F;
            } else if (pt < 28.0F) {
                // тянется вверх и сжимает кулаки на цепях
                float u = ease((pt - 18.0F) / 10.0F);
                aR = aL = Mth.lerp(u, -0.1F, UP_A);
                fR = fL = Mth.lerp(u, -0.5F, UP_F);
                hx = Mth.lerp(u, STAND_H, -0.45F);
                za = Mth.lerp(ease((pt - 18.0F) / 3.0F), 0.2F, 0.0F);
                float fi = Mth.lerp(ease((pt - 18.0F) / 5.0F), 0.85F, 0.3F);
                fiR = fiL = Mth.lerp(ease((pt - 24.0F) / 4.0F), fi, FIST);
                chains = pt >= 21.0F;
                float gNow = gripY(pelvisY(l), -6.0F, t, aR, fR, hR);
                linksR = linksL = gUp - gNow;      // цепь висит неподвижно, пока руки к ней поднимаются
            } else if (pt < 34.0F) {
                // первый рывок: ускоряющийся рывок к груди, колени проседают, удар на 34
                float x = (pt - 28.0F) / 6.0F;
                float y = x * x;
                aR = aL = Mth.lerp(y, UP_A, Y1_A);
                fR = fL = Mth.lerp(y, UP_F, Y1_F);
                hR = hL = Mth.lerp(y, 0.0F, Y1_H);
                l = Mth.lerp(y, STAND_L, Y1_L);
                t = Mth.lerp(y, STAND_T, Y1_T);
                hx = Mth.lerp(y, -0.45F, -0.15F);
                shY = -2.0F * y;
                shZ = 0.2F * y;
                fiR = fiL = FIST;
                chains = true;
            } else if (pt < 48.0F) {
                // перехват: правая рука скользит по цепи вверх первой, затем левая
                float rr = ease((pt - 34.0F) / 10.0F);
                float rl = ease((pt - 37.0F) / 11.0F);
                float rc = ease((pt - 34.0F) / 14.0F);
                aR = Mth.lerp(rr, Y1_A, UP_A);
                fR = Mth.lerp(rr, Y1_F, UP_F);
                hR = Mth.lerp(rr, Y1_H, 0.0F);
                aL = Mth.lerp(rl, Y1_A, UP_A);
                fL = Mth.lerp(rl, Y1_F, UP_F);
                hL = Mth.lerp(rl, Y1_H, 0.0F);
                l = Mth.lerp(rc, Y1_L, STAND_L);
                t = Mth.lerp(rc, Y1_T, STAND_T);
                hx = Mth.lerp(rc, -0.15F, -0.45F);
                shY = -2.0F * (1.0F - rc);
                shZ = 0.2F * (1.0F - rc);
                fiR = Mth.lerp(Mth.sin(rr * Mth.PI), FIST, 0.6F);
                fiL = Mth.lerp(Mth.sin(rl * Mth.PI), FIST, 0.6F);
                chains = true;
                float py = pelvisY(l);
                linksR = gY1 - gripY(py, -6.0F, t, aR, fR, hR);
                linksL = gY1 - gripY(py, -6.0F, t, aL, fL, hL);
            } else if (pt < 56.0F) {
                // второй рывок, сильнее: удар на 56
                float x = (pt - 48.0F) / 8.0F;
                float y = x * x;
                aR = aL = Mth.lerp(y, UP_A, Y2_A);
                fR = fL = Mth.lerp(y, UP_F, Y2_F);
                hR = hL = Mth.lerp(y, 0.0F, Y2_H);
                l = Mth.lerp(y, STAND_L, Y2_L);
                t = Mth.lerp(y, STAND_T, Y2_T);
                hx = Mth.lerp(y, -0.45F, -0.1F);
                shY = -2.5F * y;
                shZ = 0.25F * y;
                fiR = fiL = FIST;
                chains = true;
                linksR = linksL = d1;
            } else {
                // держит цепи внизу: тяжело дышит, дрожит, смотрит вверх на потолок
                float look = ease((pt - 56.0F) / 14.0F);
                aR = aL = Y2_A + trem;
                fR = fL = Y2_F - trem;
                hR = hL = Y2_H;
                l = Y2_L + 0.03F * heave;
                t = Y2_T + 0.05F * heave;
                hx = Mth.lerp(look, -0.1F, -0.6F) + 0.03F * heave;
                shY = -2.5F - 1.4F * heave;
                shZ = 0.25F;
                fiR = fiL = FIST;
                chains = true;
                linksR = linksL = d1;
            }
        } else {
            // отпускает: кулаки раскрываются, цепи улетают вверх из рук, он опускается на сиденье
            float k = ease(pt / RELEASE_T);
            w = 1.0F - k;
            wz = 1.0F - ease((pt - 10.0F) / 15.0F);
            aR = aL = Y2_A + trem * w;
            fR = fL = Y2_F - trem * w;
            hR = hL = Y2_H;
            l = (Y2_L + 0.03F * heave) * w;
            t = Y2_T + 0.05F * heave;
            hx = -0.6F + 0.03F * heave;
            shY = -2.5F - 1.4F * heave;
            shZ = 0.25F;
            fiR = fiL = Mth.lerp(ease(pt / 6.0F), FIST, 0.3F);
            chains = pt < 12.0F;
            float x = pt / 12.0F;
            linksR = linksL = d1 - 320.0F * x * x;
        }

        // ноги и таз напрямую из угла бедра (ступни остаются на полу)
        pelvis.y = pelvisY(l);
        pelvis.z = pelvisZ(l);
        rightLeg.xRot = l;
        leftLeg.xRot = l;
        rightShin.xRot = -l;
        leftShin.xRot = -l;
        // остальное смешивается с позой покоя/разговора
        torso.xRot = Mth.lerp(w, torso.xRot, t);
        torso.y = Mth.lerp(w, torso.y, -6.0F);
        torso.yRot = Mth.lerp(w, torso.yRot, 0.0F);
        head.xRot = Mth.lerp(w, head.xRot, hx);
        head.yRot = Mth.lerp(w, head.yRot, 0.0F);
        head.zRot = Mth.lerp(w, head.zRot, 0.0F);
        rightShoulder.y = Mth.lerp(w, rightShoulder.y, -14.0F + shY);
        leftShoulder.y = Mth.lerp(w, leftShoulder.y, -14.0F + shY);
        rightShoulder.zRot = Mth.lerp(w, rightShoulder.zRot, -0.15F - shZ);
        leftShoulder.zRot = Mth.lerp(w, leftShoulder.zRot, 0.15F + shZ);
        rightArm.xRot = Mth.lerp(w, rightArm.xRot, aR);
        leftArm.xRot = Mth.lerp(w, leftArm.xRot, aL);
        rightArm.yRot = Mth.lerp(w, rightArm.yRot, 0.0F);
        leftArm.yRot = Mth.lerp(w, leftArm.yRot, 0.0F);
        rightArm.zRot = Mth.lerp(wz, rightArm.zRot, -za);
        leftArm.zRot = Mth.lerp(wz, leftArm.zRot, za);
        rightForearm.xRot = Mth.lerp(w, rightForearm.xRot, fR);
        leftForearm.xRot = Mth.lerp(w, leftForearm.xRot, fL);
        leftForearm.yRot = Mth.lerp(w, leftForearm.yRot, 0.0F);
        rightHand.xRot = Mth.lerp(w, rightHand.xRot, hR);
        leftHand.xRot = Mth.lerp(w, leftHand.xRot, hL);
        rightFingers.xRot = Mth.lerp(w, rightFingers.xRot, fiR);
        leftFingers.xRot = Mth.lerp(w, leftFingers.xRot, fiL);
        rightChainLinks.visible = chains;
        leftChainLinks.visible = chains;
        rightChainLinks.y = linksR;
        leftChainLinks.y = linksL;
    }

    /** Высота таза (модельный y) при угле бедра l: ступни стоят на полу y = 24. */
    private static float pelvisY(float l) {
        return 18.0F - 8.0F * Mth.sin(l);
    }

    /** Смещение таза по z при угле бедра l: ступни остаются на месте. */
    private static float pelvisZ(float l) {
        return -8.0F + 8.0F * Mth.cos(l);
    }

    /**
     * Модельный y точки захвата цепи в кулаке: прямая кинематика таз → корпус (наклон t) → плечо (a)
     * → предплечье (f) → кисть (h). Все суставы вращаются вокруг X, разворот плеча zRot не учитывается.
     */
    private static float gripY(float pelvisY, float torsoY, float t, float a, float f, float h) {
        float ca = Mth.cos(a);
        float sa = Mth.sin(a);
        float caf = Mth.cos(a + f);
        float saf = Mth.sin(a + f);
        float cg = Mth.cos(a + f + h);
        float sg = Mth.sin(a + f + h);
        float gy = -12.0F + 5.5F * ca + 9.0F * caf + GRIP_Y * cg - GRIP_Z * sg;
        float gz = 1.0F + 5.5F * sa + 9.0F * saf + GRIP_Y * sg + GRIP_Z * cg;
        return pelvisY + torsoY + Mth.cos(t) * gy - Mth.sin(t) * gz;
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
