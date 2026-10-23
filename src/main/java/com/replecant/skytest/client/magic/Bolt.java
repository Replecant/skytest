package com.replecant.skytest.client.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Рисует ломаный электрический разряд светящимися лентами (тип отрисовки «молния»). */
public final class Bolt {
    /**
     * @param seed    одна и та же ломаная при одном seed; меняя его, разряд «дрожит»
     * @param width   полуширина яркой сердцевины
     * @param depth   сколько уровней ответвлений
     */
    public static void draw(PoseStack.Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, long seed, float width, float alpha,
            int depth) {
        RandomSource random = RandomSource.create(seed);
        Vec3 d = b.subtract(a);
        double length = d.length();
        if (length < 1.0E-3) {
            return;
        }
        int segments = Math.max(3, (int) (length * 3.0));
        Vec3[] axes = axes(d.scale(1.0 / length));
        double amplitude = Math.min(0.6, length * 0.07 + 0.03);
        Vec3[] points = new Vec3[segments + 1];
        points[0] = a;
        points[segments] = b;
        for (int i = 1; i < segments; i++) {
            double t = (double) i / segments;
            double envelope = Math.sqrt(Math.sin(t * Math.PI));
            points[i] = a.add(d.scale(t))
                    .add(axes[0].scale(random.nextGaussian() * amplitude * envelope))
                    .add(axes[1].scale(random.nextGaussian() * amplitude * envelope));
        }
        for (int i = 0; i < segments; i++) {
            // Мягкое голубое свечение и почти белая сердцевина.
            ribbon(pose, vc, points[i], points[i + 1], width * 3.0F, 0.35F, 0.55F, 1.0F, alpha * 0.3F);
            ribbon(pose, vc, points[i], points[i + 1], width, 0.85F, 0.93F, 1.0F, alpha);
        }
        if (depth > 0) {
            int branches = 1 + random.nextInt(2);
            for (int i = 0; i < branches; i++) {
                Vec3 start = points[1 + random.nextInt(Math.max(1, segments - 1))];
                Vec3 end = start.add(d.scale(0.15 + random.nextDouble() * 0.2))
                        .add(axes[0].scale(random.nextGaussian() * length * 0.15))
                        .add(axes[1].scale(random.nextGaussian() * length * 0.15));
                draw(pose, vc, start, end, random.nextLong(), width * 0.55F, alpha * 0.8F, depth - 1);
            }
        }
    }

    private static Vec3[] axes(Vec3 dir) {
        Vec3 helper = Math.abs(dir.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 u = dir.cross(helper).normalize();
        Vec3 v = dir.cross(u).normalize();
        return new Vec3[] {u, v};
    }

    /** Две скрещённые двусторонние ленты вдоль отрезка — видны с любой стороны. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, Vec3 q, float w,
            float r, float g, float b, float a) {
        Vec3 dir = q.subtract(p);
        if (dir.lengthSqr() < 1.0E-8) {
            return;
        }
        for (Vec3 axis : axes(dir.normalize())) {
            Vec3 o = axis.scale(w);
            quad(pose, vc, p.add(o), p.subtract(o), q.subtract(o), q.add(o), r, g, b, a);
            quad(pose, vc, q.add(o), q.subtract(o), p.subtract(o), p.add(o), r, g, b, a);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3,
            float r, float g, float b, float a) {
        for (Vec3 v : new Vec3[] {v0, v1, v2, v3}) {
            vc.addVertex(pose, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a);
        }
    }

    private Bolt() {}
}
