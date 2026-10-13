package com.homemine.enchanttransformer.client;

import com.homemine.enchanttransformer.EnchantTransformerMod;
import com.homemine.enchanttransformer.ModBlockEntities;
import com.homemine.enchanttransformer.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端注册：方块实体渲染器 + GUI。
 *
 * <p>把这两个注册放在 {@code Dist.CLIENT} 的 mod-bus 订阅者里，专用服务器不会加载到任何客户端类型。
 */
@EventBusSubscriber(modid = EnchantTransformerMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EnchantTransformerClient {

    private EnchantTransformerClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ENCHANT_TRANSFORMER.get(),
                EnchantTransformerRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENCHANT_TRANSFORMER.get(), EnchantTransformerScreen::new);
    }
}
