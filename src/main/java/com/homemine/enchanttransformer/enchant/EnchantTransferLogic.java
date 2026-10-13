package com.homemine.enchanttransformer.enchant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 附魔转移的全部纯逻辑：价格、排序、读写、接收可行性。
 *
 * <p>这里刻意不碰 {@code Player}/{@code Level}，好让服务端校验与客户端按钮状态走<b>同一份</b>代码，
 * 也便于单测。经验扣减、音效、粒子由调用方（菜单/方块实体）负责。
 *
 * <p>三个从原版源码核实过的关键点（违反任意一条都会出 bug）：
 * <ol>
 *   <li>{@link EnchantmentHelper#updateEnchantments} 在物品<b>原本没有</b>附魔组件时直接返回
 *       {@code EMPTY} 且不写入（源码 {@code EnchantmentHelper.java:69-76}），而
 *       {@code DataComponents.ENCHANTMENTS} 是用 {@code .persistent(...)} 注册的、<b>没有默认值</b>，
 *       所以对"从未附魔过"的物品调用它等于什么都没做 ⇒ 必须走
 *       {@code getOrDefault(type, EMPTY)} + {@code Mutable} + {@code set(type, ...)}。</li>
 *   <li>{@link ItemStack#isEnchantable()} 只在物品<b>当前没有任何附魔</b>时为真
 *       （源码 {@code ItemStack.java:942-947}），拿它当接收条件会让"升级"永远不成立 ⇒
 *       只在 {@code current.isEmpty()} 时才查它。</li>
 *   <li>{@link Enchantment#areCompatible} 对<b>同一个附魔</b>返回 false
 *       （源码 {@code Enchantment.java:195-196}），互斥检查必须跳过附魔自身。</li>
 *   <li>{@link EnchantmentHelper#getComponentType} 只把 {@code ENCHANTED_BOOK} 映射到
 *       {@code STORED_ENCHANTMENTS}，<b>普通书</b>会落到 {@code ENCHANTMENTS}
 *       （源码 {@code EnchantmentHelper.java:95-97}）——但书上的附魔只认
 *       {@code STORED_ENCHANTMENTS}，所以普通书必须单独判（见 {@link #componentType}）。</li>
 * </ol>
 *
 * <p>另外：书本的<b>物品类型</b>跟附魔有无是绑定的（普通书 ⇄ 附魔书），附魔被搬空或
 * 首次写入后必须把物品换掉，见 {@link #normalizeBookType}。
 */
public final class EnchantTransferLogic {

    /** 接收槽数量：0 = 输入，1 = 接收 A，2 = 接收 B。 */
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RECEIVER_A = 1;
    public static final int SLOT_RECEIVER_B = 2;
    public static final int SLOT_COUNT = 3;

    public static final int PRICE_NORMAL = 4;
    public static final int PRICE_TREASURE = 6;
    public static final int PRICE_CURSE = 7;

    private EnchantTransferLogic() {
    }

    /**
     * 单条附魔的转移价格（经验等级）。
     *
     * <p><b>顺序不可交换</b>：1.21.1 的两个诅咒（绑定/消失诅咒）同时带 {@code #minecraft:treasure}，
     * 先判宝藏会让诅咒只收 6 级。诅咒必须排在最前。
     */
    public static int price(Holder<Enchantment> enchantment) {
        if (enchantment.is(EnchantmentTags.CURSE)) {
            return PRICE_CURSE;
        }
        if (enchantment.is(EnchantmentTags.TREASURE)) {
            return PRICE_TREASURE;
        }
        return PRICE_NORMAL;
    }

    /** 普通书或附魔书：两者都靠 {@code STORED_ENCHANTMENTS} 承载附魔。 */
    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /**
     * 该物品用来承载附魔的组件类型。
     *
     * <p>不能直接用 {@link EnchantmentHelper#getComponentType} 的结果：它只把
     * {@code ENCHANTED_BOOK} 映射到 {@code STORED_ENCHANTMENTS}，<b>普通书</b>会被它
     * 判成 {@code ENCHANTMENTS}（即"装备上生效的附魔"）——写在书上既不会生效，
     * 也不会让书显示成附魔书。普通书必须走 {@code STORED_ENCHANTMENTS}。
     */
    private static DataComponentType<ItemEnchantments> componentType(ItemStack stack) {
        if (stack.is(Items.BOOK)) {
            return DataComponents.STORED_ENCHANTMENTS;
        }
        return EnchantmentHelper.getComponentType(stack);
    }

    /** 物品的附魔视图：装备读 {@code ENCHANTMENTS}，附魔书/普通书读 {@code STORED_ENCHANTMENTS}。 */
    public static ItemEnchantments read(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemEnchantments.EMPTY;
        }
        return stack.getOrDefault(componentType(stack), ItemEnchantments.EMPTY);
    }

    /**
     * 写入或删除一条附魔（{@code level <= 0} 即删除，见 {@code ItemEnchantments.Mutable#set}）。
     *
     * <p>必须用这个写法而不是 {@code EnchantmentHelper.updateEnchantments}，原因见类注释。
     *
     * <p><b>只改组件、不改物品类型</b>：普通书写入后仍是普通书，要靠
     * {@link #normalizeBookType} 换成附魔书。
     */
    public static void write(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        DataComponentType<ItemEnchantments> type = componentType(stack);
        ItemEnchantments current = stack.getOrDefault(type, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(enchantment, level);
        stack.set(type, mutable.toImmutable());
    }

    /**
     * 归一化书本类型：附魔书被搬空 → 普通书；普通书写入附魔 → 附魔书。
     *
     * <p>{@code ItemStack} 的物品本身不可变，所以一旦发生类型变化必须返回<b>新实例</b>，
     * 调用方要把它写回槽位（见 {@code EnchantTransformerMenu#handleTransform}）。
     * 没发生类型变化就原样返回同一个实例，调用方照旧写回即可。
     *
     * <p>转换用 {@link ItemStack#transmuteCopy} 保留其它组件（自定义名称、Lore 等），
     * 只在"降级成普通书"时顺手摘掉已经空掉的 {@code STORED_ENCHANTMENTS}——
     * 原版附魔书永远至少带一条附魔，留着一个空组件既没意义、也会让物品在
     * 组件比对时和真普通书不相等（影响堆叠与合成判定）。
     */
    public static ItemStack normalizeBookType(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            if (read(stack).isEmpty()) {
                ItemStack plain = stack.transmuteCopy(Items.BOOK);
                plain.remove(DataComponents.STORED_ENCHANTMENTS);
                return plain;
            }
            return stack;
        }
        if (stack.is(Items.BOOK)) {
            ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                    ItemEnchantments.EMPTY);
            if (!stored.isEmpty()) {
                ItemStack book = stack.transmuteCopy(Items.ENCHANTED_BOOK);
                book.set(DataComponents.STORED_ENCHANTMENTS, stored);
                return book;
            }
        }
        return stack;
    }

    public static int levelOf(ItemStack stack, Holder<Enchantment> enchantment) {
        return read(stack).getLevel(enchantment);
    }

    /**
     * 输入物上可转移的附魔，按<b>附魔注册 ID 字典序</b>排序。
     *
     * <p>枚举必须走 {@link ItemEnchantments#keySet()}（即被其它模组过滤后的视图），
     * 否则会转出玩家根本看不见的附魔。
     */
    public static List<Holder<Enchantment>> transferable(ItemStack input) {
        ItemEnchantments enchantments = read(input);
        if (enchantments.isEmpty()) {
            return List.of();
        }
        List<Holder<Enchantment>> list = new ArrayList<>(enchantments.keySet());
        list.sort(Comparator.comparing(EnchantTransferLogic::sortKey));
        return list;
    }

    private static String sortKey(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    /** 只有输入物与目标接收槽都就位、且输入有附魔时才谈"下一条"，否则返回 null。 */
    public static Holder<Enchantment> nextEnchantment(ItemStack input, ItemStack receiver) {
        if (input.isEmpty() || receiver.isEmpty()) {
            return null;
        }
        List<Holder<Enchantment>> list = transferable(input);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 接收可行性（规格 §7）。
     *
     * <p>注意判定顺序：先同名等级，再书本短路（附魔书与普通书都放行），最后才是
     * "空物品必须可附魔"+互斥。
     */
    public static TransferCheck checkReceivable(Holder<Enchantment> enchantment, int incomingLevel,
                                                ItemStack receiver) {
        if (receiver.isEmpty()) {
            return TransferCheck.NO_RECEIVER;
        }
        ItemEnchantments current = read(receiver);
        int currentLevel = current.getLevel(enchantment);
        if (currentLevel > 0 && currentLevel >= incomingLevel) {
            return TransferCheck.SAME_OR_HIGHER;
        }
        if (isBook(receiver)) {
            // 附魔书与普通书都能承载任意附魔组合（书上没有互斥概念），普通书在写入后
            // 会被 normalizeBookType 换成附魔书，所以这里直接放行。
            return TransferCheck.OK;
        }
        if (current.isEmpty() && !receiver.isEnchantable()) {
            return TransferCheck.NOT_ENCHANTABLE;
        }
        for (Holder<Enchantment> other : current.keySet()) {
            if (other.equals(enchantment)) {
                continue;
            }
            if (!Enchantment.areCompatible(enchantment, other)) {
                return TransferCheck.INCOMPATIBLE;
            }
        }
        return TransferCheck.OK;
    }

    /** 一次完整可行性判定（含输入侧与经验）。客户端据此决定按钮是否可用。 */
    public static TransferCheck check(ItemStack input, ItemStack receiver, int availableLevels,
                                      boolean creative) {
        if (input.isEmpty()) {
            return TransferCheck.NO_INPUT;
        }
        if (receiver.isEmpty()) {
            return TransferCheck.NO_RECEIVER;
        }
        Holder<Enchantment> enchantment = nextEnchantment(input, receiver);
        if (enchantment == null) {
            return TransferCheck.NO_ENCHANTMENT;
        }
        int price = price(enchantment);
        if (!creative && availableLevels < price) {
            return TransferCheck.INSUFFICIENT_LEVEL;
        }
        return checkReceivable(enchantment, levelOf(input, enchantment), receiver);
    }

    /**
     * 真正执行一次转移：从输入物删除该条、写入/升级到接收物。
     *
     * <p>经验扣减、音效与粒子不在这里做。
     *
     * <p><b>调用方必须紧接着对两个栈各调一次 {@link #normalizeBookType}</b>，否则
     * "附魔书被搬空"和"普通书写入附魔"两种情况下的物品类型不会跟着变。
     */
    public static void transfer(ItemStack input, ItemStack receiver, Holder<Enchantment> enchantment) {
        int level = levelOf(input, enchantment);
        if (level <= 0) {
            return;
        }
        write(input, enchantment, 0);
        write(receiver, enchantment, level);
    }
}
