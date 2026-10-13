package com.homemine.enchanttransformer;

import com.homemine.enchanttransformer.block.EnchantTransformerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantTransformerMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantTransformerBlockEntity>>
            ENCHANT_TRANSFORMER = BLOCK_ENTITIES.register("enchant_transformer",
            () -> BlockEntityType.Builder.of(
                    EnchantTransformerBlockEntity::new, ModBlocks.ENCHANT_TRANSFORMER.get()).build(null));

    private ModBlockEntities() {
    }
}
