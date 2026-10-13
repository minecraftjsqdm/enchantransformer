package com.homemine.enchanttransformer.client;

import com.homemine.enchanttransformer.block.EnchantTransformerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * 三本书的渲染。
 *
 * <p>复用原版 {@link BookModel}（{@code ModelLayers.BOOK}）与附魔台的书贴图，零自制模型/贴图。
 * 每本书有一组「静止位姿」与「飞行位姿」，用方块实体的 {@code lift}（0→1）线性插值，
 * 翻开程度用 {@code open}（0→1）。两者都由 {@code bookAnimationTick} 以 ±0.1/tick 推进，
 * 因此整套动画 10 tick 到位，玩家离开时反向播回。
 *
 * <p>坐标系约定：先照抄原版附魔台的基础变换（{@code translate(0.5, 0.75, 0.5)} +
 * {@code YP.rotation(-rot)} + {@code ZP.rotationDegrees(80)}），此后<b>局部 +X 轴指向玩家</b>。
 * 表里的 (dx, dz) 就是该局部坐标系下的偏移，单位 1/16 格；yaw 是相对 +X 的偏转角。
 *
 * <p><b>待调项</b>：REST/FLY 两张表是照 Blockbench 草稿翻译的第一版数值，观感需进游戏微调
 * （只改这两张表，不动逻辑）。
 */
public class EnchantTransformerRenderer implements BlockEntityRenderer<EnchantTransformerBlockEntity> {

    public static final Material BOOK_LOCATION = new Material(
            TextureAtlas.LOCATION_BLOCKS,
            ResourceLocation.withDefaultNamespace("entity/enchanting_table_book"));

    /** 扇开半径（1/16 格）：飞起时三本按 120° 排开，半径决定相邻间距（4.5 时几乎贴在一起）。 */
    private static final float FLY_RADIUS = 7.0F;
    /** 飞起高度（1/16 格）。草稿取 8.5。 */
    private static final float FLY_LIFT = 8.5F;

    /**
     * {dx, dy, dz, yaw, tilt} —— 静止位姿：三本都竖立在底座顶面、同一排靠在书立前。
     * 左起两本（草稿 b1/b2）直立后倾 10° 直接靠书立；第三本（草稿 b3）立在两本东侧、
     * 朝**东**立向**西**斜 20°，上沿倚在中间那本上。
     *
     * <p>参数含义：{@code tilt} 是整本书的竖直倾角（0 = 竖立，90 = 完全平躺），
     * 因为原版基础变换里的 {@code ZP(80)} 已经并入这里；{@code yaw} 是相对"朝向玩家"的偏转
     * （静止时 rot→0，故 yaw 直接就是绝对方位：-90 = 面向北、0 = 面向东）。
     *
     * <p>注意：倾角方向恒为"背离书页朝向"，所以要让一本书朝西倒靠，必须让它朝东立（yaw=0）。
     *
     * <p>高度：书的中心在 y = 0.75 + dy/16 格。竖立书高约 7.7px，中心 y=12px 时底面恰在 8px
     * （底座顶面），故三本 dy 均为 0。
     */
    private static final float[][] REST = {
            {-4.4F, 0.0F, 4.2F, -90.0F, -10.0F},
            {-1.8F, 0.0F, 4.2F, -90.0F, -10.0F},
            {2.2F, 0.0F, 4.2F, 0.0F, 20.0F},
    };

    /** 飞行位姿：120° 扇开、抬起、各自朝外、完全翻开。 */
    private static final float[][] FLY = {
            {FLY_RADIUS, 0.0F, 0.0F, 0.0F, 80.0F},
            {-FLY_RADIUS * 0.5F, 0.0F, -FLY_RADIUS * 0.866F, 120.0F, 80.0F},
            {-FLY_RADIUS * 0.5F, 0.0F, FLY_RADIUS * 0.866F, -120.0F, 80.0F},
    };

    private final BookModel bookModel;

    public EnchantTransformerRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(EnchantTransformerBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float lift = Mth.lerp(partialTick, be.oLift, be.lift);
        float open = Mth.lerp(partialTick, be.oOpen, be.open);
        float flip = Mth.lerp(partialTick, be.oFlip, be.flip);
        float time = (float) be.time + partialTick;

        float rotDelta = be.rot - be.oRot;
        while (rotDelta >= (float) Math.PI) {
            rotDelta -= (float) (Math.PI * 2);
        }
        while (rotDelta < (float) -Math.PI) {
            rotDelta += (float) (Math.PI * 2);
        }
        float rot = be.oRot + rotDelta * partialTick;

        float leftFlip = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float rightFlip = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float bob = Mth.sin(time * 0.1F) * 0.01F * lift;

        VertexConsumer consumer = BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);

        for (int index = 0; index < REST.length; index++) {
            float[] rest = REST[index];
            float[] fly = FLY[index];

            float dx = Mth.lerp(lift, rest[0], fly[0]);
            float dy = Mth.lerp(lift, rest[1], fly[1]) + lift * FLY_LIFT;
            float dz = Mth.lerp(lift, rest[2], fly[2]);
            float yaw = Mth.lerp(lift, rest[3], fly[3]);
            float tilt = Mth.lerp(lift, rest[4], fly[4]);

            poseStack.pushPose();
            poseStack.translate(0.5F + dx / 16.0F, 0.75F + dy / 16.0F + bob, 0.5F + dz / 16.0F);
            poseStack.mulPose(Axis.YP.rotation(-rot));
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
            this.bookModel.setupAnim(time, leftFlip, rightFlip, open);
            this.bookModel.render(poseStack, consumer, packedLight, packedOverlay, -1);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(EnchantTransformerBlockEntity be) {
        // 书飞起后高出 1 格（草稿实测最高 17.8px），包围盒照抄附魔台放大到 y+1.5。
        return new AABB(be.getBlockPos()).inflate(1.0).expandTowards(0.0, 1.5, 0.0);
    }
}
