package com.homemine.enchanttransformer;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EnchantTransformerMod.MODID);

    public static final DeferredItem<BlockItem> ENCHANT_TRANSFORMER =
            ITEMS.registerSimpleBlockItem(ModBlocks.ENCHANT_TRANSFORMER);

    private ModItems() {
    }
}
