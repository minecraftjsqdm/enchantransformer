package com.homemine.enchanttransformer;

import com.homemine.enchanttransformer.block.EnchantTransformerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EnchantTransformerMod.MODID);

    /**
     * 附魔转移台：半砖高的黑曜石基座（贴图取自附魔台）+ 下界合金 bookend。
     *
     * <p>硬度取附魔台一档；抗爆取黑曜石；发光 7（基座里有附魔光）。
     * {@code noOcclusion()} 是必须的——bookend 高出半砖，模型不是完整立方体。
     */
    public static final DeferredBlock<EnchantTransformerBlock> ENCHANT_TRANSFORMER = BLOCKS.registerBlock(
            "enchant_transformer",
            EnchantTransformerBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.STONE)
                    .noOcclusion());

    private ModBlocks() {
    }
}
