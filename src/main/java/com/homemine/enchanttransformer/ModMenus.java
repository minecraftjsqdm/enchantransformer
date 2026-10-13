package com.homemine.enchanttransformer;

import com.homemine.enchanttransformer.menu.EnchantTransformerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnchantTransformerMod.MODID);

    /**
     * 带附加数据的菜单类型：客户端用 {@code (containerId, Inventory, RegistryFriendlyByteBuf)}
     * 构造器创建占位版本，真实内容由原版容器同步补齐。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<EnchantTransformerMenu>> ENCHANT_TRANSFORMER =
            MENUS.register("enchant_transformer", () -> IMenuTypeExtension.create(EnchantTransformerMenu::new));

    private ModMenus() {
    }
}
