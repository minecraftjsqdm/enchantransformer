package com.homemine.enchanttransformer.menu;

import com.homemine.enchanttransformer.ModMenus;
import com.homemine.enchanttransformer.enchant.EnchantTransferLogic;
import com.homemine.enchanttransformer.enchant.TransferCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 附魔转移台 GUI：1 输入槽 + 2 接收槽 + 玩家背包。
 *
 * <p>服务端持有真实的 {@link ItemStackHandler}；客户端通过 {@code IMenuTypeExtension} 的
 * {@code (int, Inventory, RegistryFriendlyByteBuf)} 构造器建一个同尺寸的占位 handler，
 * 槽位内容由原版容器同步补齐。
 *
 * <p>按钮可用性由客户端用 {@link EnchantTransferLogic} 自行算出（双端同一份纯逻辑），
 * 服务端在 {@link #handleTransform} 里<b>再独立校验一遍</b>，因此改客户端也点不出非法转移。
 */
public class EnchantTransformerMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = EnchantTransferLogic.SLOT_COUNT;
    private static final int INV_START = MACHINE_SLOTS;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    /** 与原版 176×166 面板一致：背包 3 行在 y=84/102/120，快捷栏在 y=142。 */
    private static final int INV_X = 8;
    private static final int INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final ItemStackHandler items;
    private final BlockPos pos;

    /** 服务端构造器：绑定方块实体里真实的物品栏。 */
    public EnchantTransformerMenu(int containerId, Inventory playerInventory, ItemStackHandler items,
                                  BlockPos pos) {
        super(ModMenus.ENCHANT_TRANSFORMER.get(), containerId);
        this.items = items;
        this.pos = pos;

        this.addSlot(new SlotItemHandler(items, EnchantTransferLogic.SLOT_INPUT, 26, 17));
        this.addSlot(new SlotItemHandler(items, EnchantTransferLogic.SLOT_RECEIVER_A, 64, 17));
        this.addSlot(new SlotItemHandler(items, EnchantTransferLogic.SLOT_RECEIVER_B, 64, 39));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        INV_X + column * 18, INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, INV_X + column * 18, HOTBAR_Y));
        }
    }

    /** 客户端构造器（IMenuTypeExtension）：占位 handler，尺寸必须与服务端一致。 */
    public EnchantTransformerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, new ItemStackHandler(MACHINE_SLOTS), BlockPos.ZERO);
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Override
    public boolean stillValid(Player player) {
        // 与工作区里其它方块 GUI 一致：不做距离校验（服务端每次操作仍独立校验）。
        return true;
    }

    /** 服务端执行一次转移；返回实际结果便于测试与日志。 */
    public TransferCheck handleTransform(ServerPlayer player, byte targetSlot) {
        if (targetSlot != EnchantTransferLogic.SLOT_RECEIVER_A
                && targetSlot != EnchantTransferLogic.SLOT_RECEIVER_B) {
            return TransferCheck.NO_RECEIVER;
        }
        ItemStack input = this.items.getStackInSlot(EnchantTransferLogic.SLOT_INPUT);
        ItemStack receiver = this.items.getStackInSlot(targetSlot);

        TransferCheck check = EnchantTransferLogic.check(input, receiver, player.experienceLevel,
                player.isCreative());
        if (!check.ok()) {
            return check;
        }
        Holder<Enchantment> enchantment = EnchantTransferLogic.nextEnchantment(input, receiver);
        if (enchantment == null) {
            return TransferCheck.NO_ENCHANTMENT;
        }
        int price = EnchantTransferLogic.price(enchantment);

        EnchantTransferLogic.transfer(input, receiver, enchantment);
        // 书本类型归一化：附魔书被搬空要变回普通书，普通书拿到附魔要变成附魔书。
        // ItemStack 的物品不可变，类型变了 normalizeBookType 会返回新实例，必须重新赋值再写回。
        input = EnchantTransferLogic.normalizeBookType(input);
        receiver = EnchantTransferLogic.normalizeBookType(receiver);
        // 就地改过的 ItemStack 必须显式回写，才会触发 onContentsChanged / 标脏 / 同步。
        this.items.setStackInSlot(EnchantTransferLogic.SLOT_INPUT, input);
        this.items.setStackInSlot(targetSlot, receiver);

        if (!player.isCreative()) {
            player.giveExperienceLevels(-price);
        }

        player.level().playSound(null, this.pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    this.pos.getX() + 0.5, this.pos.getY() + 1.1, this.pos.getZ() + 0.5,
                    12, 0.3, 0.25, 0.3, 0.02);
        }
        this.broadcastChanges();
        return TransferCheck.OK;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    /** 供客户端显示用：输入槽当前物品。 */
    public ItemStack inputStack() {
        return this.items.getStackInSlot(EnchantTransferLogic.SLOT_INPUT);
    }

    public ItemStack receiverStack(int slot) {
        return this.items.getStackInSlot(slot);
    }
}
