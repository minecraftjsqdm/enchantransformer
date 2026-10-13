package com.homemine.enchanttransformer.net;

import com.homemine.enchanttransformer.EnchantTransformerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：玩家点了「转移到 A / B」。
 *
 * <p>只带目标槽位这一个字节；其余全部由服务端从自己那份菜单状态读取，
 * 所以改客户端也点不出别的效果。服务端仍会独立跑一遍校验。
 */
public record TransformPayload(byte targetSlot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TransformPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(EnchantTransformerMod.MODID, "transform"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformPayload> STREAM_CODEC =
            StreamCodec.of(TransformPayload::encode, TransformPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, TransformPayload payload) {
        buffer.writeByte(payload.targetSlot());
    }

    private static TransformPayload decode(RegistryFriendlyByteBuf buffer) {
        return new TransformPayload(buffer.readByte());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
