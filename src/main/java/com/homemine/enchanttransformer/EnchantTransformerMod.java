package com.homemine.enchanttransformer;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/**
 * 附魔转移台（enchantransformer）
 *
 * <p>一台方块设备：把输入槽物品上的附魔<b>逐条</b>转移到两个接收槽之一的物品上，每次转移消耗经验等级
 * （普通 4 / 宝藏 6 / 诅咒 7）。设计规格见
 * {@code docs/superpowers/specs/2026-09-27-enchantransformer-design.md}。
 */
@Mod(EnchantTransformerMod.MODID)
public class EnchantTransformerMod {

    public static final String MODID = "enchantransformer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchantTransformerMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::addCreativeTabItems);
        modBus.addListener(ModNetwork::register);
    }

    /** 把方块放进原版「功能方块」标签页。 */
    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.ENCHANT_TRANSFORMER.get());
        }
    }
}
