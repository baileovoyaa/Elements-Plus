package com.elementsplus.client.render;

import com.elementsplus.blocks.entity.RedstoneInterfaceBlockEntity;
import com.elementsplus.core.circuit.CircuitComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public class RedstoneInterfaceRenderer<T extends RedstoneInterfaceBlockEntity>
        implements BlockEntityRenderer<T> {

    public RedstoneInterfaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
            RedstoneInterfaceBlockEntity.PinInfo pin = blockEntity.getFacePin(face);
            if (pin == null) {
                continue;
            }
            int baseColor = pin.type() == CircuitComponent.PinType.INPUT ? 0x4488FF : 0x44FF88;
            renderSquare(consumer, poseStack, face, 0.125F, 0.001F, baseColor);
            int signal = blockEntity.getFaceValue(face);
            int innerColor = pin.bitWidth() == 8
                    ? 0xFF00FF
                    : ((signal * 255 / 15) << 16);
            renderSquare(consumer, poseStack, face, 0.0625F, 0.002F, innerColor);
        }

        int runColor = runStatusColor(blockEntity);
        int averageColor = averageStatusColor(blockEntity);
        for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
            // 每个面的纹理左上角：运行状态 (2,2)，平均 MSPT 状态 (4,2)。
            renderPixelSquare(consumer, poseStack, face, 2, 2, runColor);
            renderPixelSquare(consumer, poseStack, face, 4, 2, averageColor);
        }
    }

    private static int runStatusColor(RedstoneInterfaceBlockEntity blockEntity) {
        if (blockEntity.isHibernating()) {
            return 0xFFFF0000;
        }
        if (!blockEntity.getDisplayHasCircuit() || !blockEntity.getDisplayHasBattery()) {
            return 0xFF000000;
        }
        if (blockEntity.isRunning()) {
            return 0xFF00FF00;
        }
        return 0xFF3377FF;
    }

    private static int averageStatusColor(RedstoneInterfaceBlockEntity blockEntity) {
        if (!blockEntity.isRunning()
                || !blockEntity.getDisplayHasCircuit()
                || !blockEntity.getDisplayHasBattery()) {
            return 0xFF000000;
        }
        double average = blockEntity.getDisplayAverageMs();
        int limit = blockEntity.getDisplayAverageLimit();
        if (limit <= 0) {
            return average > 0 ? 0xFFFF0000 : 0xFF00FF00;
        }
        if (average >= limit * 0.75) {
            return 0xFFFF0000;
        }
        if (average >= limit * 0.5) {
            return 0xFFFFA500;
        }
        return 0xFF00FF00;
    }

    private static void renderSquare(VertexConsumer consumer, PoseStack poseStack, Direction face,
                                     float half, float offset, int rgb) {
        float x0;
        float x1;
        float y0;
        float y1;
        float z0;
        float z1;
        switch (face) {
            case UP -> {
                x0 = 0.5F - half; x1 = 0.5F + half;
                y0 = y1 = 1.0F + offset;
                z0 = 0.5F - half; z1 = 0.5F + half;
            }
            case DOWN -> {
                x0 = 0.5F - half; x1 = 0.5F + half;
                y0 = y1 = -offset;
                z0 = 0.5F - half; z1 = 0.5F + half;
            }
            case NORTH -> {
                x0 = 0.5F - half; x1 = 0.5F + half;
                y0 = 0.5F - half; y1 = 0.5F + half;
                z0 = z1 = -offset;
            }
            case SOUTH -> {
                x0 = 0.5F - half; x1 = 0.5F + half;
                y0 = 0.5F - half; y1 = 0.5F + half;
                z0 = z1 = 1.0F + offset;
            }
            case WEST -> {
                x0 = x1 = -offset;
                y0 = 0.5F - half; y1 = 0.5F + half;
                z0 = 0.5F - half; z1 = 0.5F + half;
            }
            default -> { // EAST
                x0 = x1 = 1.0F + offset;
                y0 = 0.5F - half; y1 = 0.5F + half;
                z0 = 0.5F - half; z1 = 0.5F + half;
            }
        }
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        switch (face) {
            case UP, DOWN -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
            case NORTH, SOUTH -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
            default -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        }
    }

    private static void renderPixelSquare(VertexConsumer consumer, PoseStack poseStack, Direction face,
                                          int u, int v, int rgb) {
        float u0 = u / 16.0F;
        float u1 = (u + 1) / 16.0F;
        float v0 = v / 16.0F;
        float v1 = (v + 1) / 16.0F;
        float x0;
        float x1;
        float y0;
        float y1;
        float z0;
        float z1;
        switch (face) {
            case UP -> {
                x0 = u0; x1 = u1;
                y0 = y1 = 1.0F + 0.003F;
                z0 = v0; z1 = v1;
            }
            case DOWN -> {
                x0 = u0; x1 = u1;
                y0 = y1 = -0.003F;
                z0 = 1.0F - v1; z1 = 1.0F - v0;
            }
            case NORTH -> {
                x0 = 1.0F - u1; x1 = 1.0F - u0;
                y0 = 1.0F - v1; y1 = 1.0F - v0;
                z0 = z1 = -0.003F;
            }
            case SOUTH -> {
                x0 = u0; x1 = u1;
                y0 = 1.0F - v1; y1 = 1.0F - v0;
                z0 = z1 = 1.0F + 0.003F;
            }
            case WEST -> {
                x0 = x1 = -0.003F;
                y0 = 1.0F - v1; y1 = 1.0F - v0;
                z0 = u0; z1 = u1;
            }
            default -> { // EAST
                x0 = x1 = 1.0F + 0.003F;
                y0 = 1.0F - v1; y1 = 1.0F - v0;
                z0 = 1.0F - u1; z1 = 1.0F - u0;
            }
        }
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        switch (face) {
            case UP, DOWN -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
            case NORTH, SOUTH -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
            default -> addQuad(consumer, poseStack, r, g, b,
                    x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        }
    }

    private static void addQuad(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b,
                                float ax, float ay, float az,
                                float bx, float by, float bz,
                                float cx, float cy, float cz,
                                float dx, float dy, float dz) {
        addVertex(consumer, poseStack, ax, ay, az, r, g, b);
        addVertex(consumer, poseStack, bx, by, bz, r, g, b);
        addVertex(consumer, poseStack, cx, cy, cz, r, g, b);
        addVertex(consumer, poseStack, dx, dy, dz, r, g, b);
        // 反向绕序，保证从任意侧面都能看到彩色标记。
        addVertex(consumer, poseStack, ax, ay, az, r, g, b);
        addVertex(consumer, poseStack, dx, dy, dz, r, g, b);
        addVertex(consumer, poseStack, cx, cy, cz, r, g, b);
        addVertex(consumer, poseStack, bx, by, bz, r, g, b);
    }

    private static void addVertex(VertexConsumer consumer, PoseStack poseStack,
                                  float x, float y, float z, int r, int g, int b) {
        consumer.addVertex(poseStack.last(), x, y, z).setColor(r, g, b, 255);
    }
}
