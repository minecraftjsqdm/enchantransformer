package com.homemine.enchanttransformer.block;

import com.homemine.enchanttransformer.ModBlockEntities;
import com.homemine.enchanttransformer.enchant.EnchantTransferLogic;
import com.homemine.enchanttransformer.menu.EnchantTransformerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 附魔转移台的方块实体：3 个槽位（0 输入 / 1 接收 A / 2 接收 B）+ 客户端书本动画状态。
 *
 * <p>动画字段只在客户端有意义；服务端只负责槽位与转移逻辑（见 {@link EnchantTransformerMenu}）。
 * 算法以原版 {@code EnchantingTableBlockEntity#bookAnimationTick} 为骨架，另外加一条 {@code lift}
 * 表示"三本书抬起并扇开"的程度（与 {@code open} 同步 ±0.1/tick）。
 *
 * <p><b>与原版的一处刻意差异</b>：玩家不在 3 格内时，原版会让 {@code tRot += 0.02} 空转，
 * 这里改为收敛到 {@link #REST_ROT} 并停住 —— 因为静止位姿（三本竖立、按 rot = 0 摆在书立前）是按
 * {@code rot = 0} 摆的，空转会让书绕着方块滑动、也使"没人时静止摆放"这一观感不成立。
 */
public class EnchantTransformerBlockEntity extends BlockEntity implements MenuProvider {

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_SLOT = "Slot";
    private static final RandomSource RANDOM = RandomSource.create();

    private final ItemStackHandler items = new ItemStackHandler(EnchantTransferLogic.SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    // ---- 客户端动画状态（照抄附魔台）----
    public int time;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    /** 抬起/扇开程度，0 = 归位静止，1 = 完全飞起翻开。 */
    public float lift;
    public float oLift;

    /** 静止朝向（弧度）：0 即草稿里的摆放朝向；玩家离开后 rot 收敛到这里并停住。 */
    public static final float REST_ROT = 0.0F;

    public EnchantTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANT_TRANSFORMER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enchantransformer.enchant_transformer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EnchantTransformerMenu(containerId, inventory, items, this.worldPosition);
    }

    /** 破坏方块时把三个槽位的内容撒出来（不吞玩家物品）。 */
    public void dropContents() {
        if (this.level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5,
                        this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
            }
        }
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte(KEY_SLOT, (byte) slot);
                entry.put("Item", stack.save(registries));
                list.add(entry);
            }
        }
        tag.put(KEY_ITEMS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < items.getSlots(); slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        ListTag list = tag.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte(KEY_SLOT) & 0xFF;
            if (slot >= 0 && slot < items.getSlots()) {
                ItemStack.parse(registries, entry.get("Item"))
                        .ifPresent(stack -> items.setStackInSlot(slot, stack));
            }
        }
    }

    // ------------------------------------------------------------------
    // 动画（仅客户端）
    // ------------------------------------------------------------------

    /**
     * 逐行照抄原版附魔台的书本动画，外加 {@code lift} 插值。
     *
     * <p>靠近判定与原版完全一致：{@code getNearestPlayer(x+0.5, y+0.5, z+0.5, 3.0, false)} —— 半径 3 格。
     */
    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state,
                                         EnchantTransformerBlockEntity be) {
        be.oOpen = be.open;
        be.oRot = be.rot;
        be.oLift = be.lift;

        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                3.0, false);
        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            be.tRot = (float) Mth.atan2(dz, dx);
            be.open += 0.1F;
            be.lift += 0.1F;
            if (be.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float previous = be.flipT;
                do {
                    be.flipT = be.flipT + (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (previous == be.flipT);
            }
        } else {
            // 玩家离开：不像原版那样空转（tRot += 0.02），而是平滑回到静止朝向并彻底停住。
            // 另外 REST 位姿是按 rot = 0 从草稿翻译的，rot 不为 0 时"倾靠"的两本会绕方块滑动。
            be.tRot = REST_ROT;
            be.open -= 0.1F;
            be.lift -= 0.1F;
        }

        while (be.rot >= (float) Math.PI) {
            be.rot -= (float) (Math.PI * 2);
        }
        while (be.rot < (float) -Math.PI) {
            be.rot += (float) (Math.PI * 2);
        }
        while (be.tRot >= (float) Math.PI) {
            be.tRot -= (float) (Math.PI * 2);
        }
        while (be.tRot < (float) -Math.PI) {
            be.tRot += (float) (Math.PI * 2);
        }

        float delta = be.tRot - be.rot;
        while (delta >= (float) Math.PI) {
            delta -= (float) (Math.PI * 2);
        }
        while (delta < (float) -Math.PI) {
            delta += (float) (Math.PI * 2);
        }

        be.rot += delta * 0.4F;
        be.open = Mth.clamp(be.open, 0.0F, 1.0F);
        be.lift = Mth.clamp(be.lift, 0.0F, 1.0F);
        be.time++;

        be.oFlip = be.flip;
        float flipDelta = (be.flipT - be.flip) * 0.4F;
        flipDelta = Mth.clamp(flipDelta, -0.2F, 0.2F);
        be.flipA = be.flipA + (flipDelta - be.flipA) * 0.9F;
        be.flip = be.flip + be.flipA;
    }
}
