package com.homemine.enchanttransformer.client;

import com.homemine.enchanttransformer.enchant.EnchantTransferLogic;
import com.homemine.enchanttransformer.enchant.TransferCheck;
import com.homemine.enchanttransformer.menu.EnchantTransformerMenu;
import com.homemine.enchanttransformer.net.TransformPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 附魔转移台 GUI（方案 A · 竖列双槽）。
 *
 * <p>面板与槽位是<b>代码绘制</b>的（没有 GUI 贴图文件），配色与斜角照原版面板：
 * 面板 #C6C6C6、左上高光 #FFFFFF、右下阴影 #555555、槽底 #8B8B8B。
 *
 * <p>两个按钮直接用原版 {@link Button}，因此「可用 / 禁用 / 悬停」三态与原版天然一致。
 */
public class EnchantTransformerScreen extends AbstractContainerScreen<EnchantTransformerMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private static final int SLOT_INPUT_X = 26;
    private static final int SLOT_INPUT_Y = 17;
    private static final int SLOT_A_X = 64;
    private static final int SLOT_A_Y = 17;
    private static final int SLOT_B_X = 64;
    private static final int SLOT_B_Y = 39;

    private static final int BUTTON_A_X = 88;
    private static final int BUTTON_A_Y = 17;
    private static final int BUTTON_B_X = 88;
    private static final int BUTTON_B_Y = 39;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 18;

    private static final int INFO_X = 26;
    private static final int INFO_Y = 61;
    private static final int INFO_W = 140;
    private static final int INFO_H = 18;

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_HIGHLIGHT = 0xFFFFFFFF;
    private static final int COLOR_SHADOW = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_ENCHANT = 0xFFA8A8FF;
    private static final int COLOR_GREEN = 0xFF3FBF3F;
    private static final int COLOR_GRAY = 0xFF9A9A9A;

    private Button buttonA;
    private Button buttonB;

    public EnchantTransformerScreen(EnchantTransformerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = PANEL_H - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        this.buttonA = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.enchantransformer.button.a"),
                        button -> PacketDistributor.sendToServer(
                                new TransformPayload((byte) EnchantTransferLogic.SLOT_RECEIVER_A)))
                .bounds(this.leftPos + BUTTON_A_X, this.topPos + BUTTON_A_Y, BUTTON_W, BUTTON_H)
                .build());
        this.buttonB = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.enchantransformer.button.b"),
                        button -> PacketDistributor.sendToServer(
                                new TransformPayload((byte) EnchantTransferLogic.SLOT_RECEIVER_B)))
                .bounds(this.leftPos + BUTTON_B_X, this.topPos + BUTTON_B_Y, BUTTON_W, BUTTON_H)
                .build());
        refreshButtons();
    }

    /** 每 tick 刷新按钮可用性；与函数式逻辑同一份判定，双端结果一致。 */
    private void refreshButtons() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        ItemStack input = this.menu.inputStack();
        int levels = this.minecraft.player.experienceLevel;
        boolean creative = this.minecraft.player.isCreative();
        this.buttonA.active = checkFor(EnchantTransferLogic.SLOT_RECEIVER_A, input, levels, creative).ok();
        this.buttonB.active = checkFor(EnchantTransferLogic.SLOT_RECEIVER_B, input, levels, creative).ok();
    }

    private TransferCheck checkFor(int receiverSlot, ItemStack input, int levels, boolean creative) {
        return EnchantTransferLogic.check(input, this.menu.receiverStack(receiverSlot), levels, creative);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_PANEL);
        graphics.fill(x, y, x + PANEL_W, y + 3, COLOR_HIGHLIGHT);
        graphics.fill(x, y, x + 3, y + PANEL_H, COLOR_HIGHLIGHT);
        graphics.fill(x, y + PANEL_H - 3, x + PANEL_W, y + PANEL_H, COLOR_SHADOW);
        graphics.fill(x + PANEL_W - 3, y, x + PANEL_W, y + PANEL_H, COLOR_SHADOW);

        drawSlot(graphics, x + SLOT_INPUT_X, y + SLOT_INPUT_Y);
        drawSlot(graphics, x + SLOT_A_X, y + SLOT_A_Y);
        drawSlot(graphics, x + SLOT_B_X, y + SLOT_B_Y);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + 8 + column * 18, y + 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + 8 + column * 18, y + 142);
        }

        drawInset(graphics, x + INFO_X, y + INFO_Y, INFO_W, INFO_H);
    }

    /** 原版凹槽：左上暗、右下亮，底色 #8B8B8B。 */
    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        graphics.fill(x, y, x + 18, y + 1, COLOR_SLOT_DARK);
        graphics.fill(x, y, x + 1, y + 18, COLOR_SLOT_DARK);
        graphics.fill(x, y + 17, x + 18, y + 18, COLOR_HIGHLIGHT);
        graphics.fill(x + 17, y, x + 18, y + 18, COLOR_HIGHLIGHT);
    }

    private void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_SLOT);
        graphics.fill(x, y, x + width, y + 1, COLOR_SLOT_DARK);
        graphics.fill(x, y, x + 1, y + height, COLOR_SLOT_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_HIGHLIGHT);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_HIGHLIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int x = INFO_X + 6;
        int y = INFO_Y + 5;
        ItemStack input = this.menu.inputStack();
        Holder<Enchantment> next = EnchantTransferLogic.nextEnchantment(input,
                this.menu.receiverStack(EnchantTransferLogic.SLOT_RECEIVER_A));
        if (next == null) {
            next = EnchantTransferLogic.nextEnchantment(input,
                    this.menu.receiverStack(EnchantTransferLogic.SLOT_RECEIVER_B));
        }
        if (next == null) {
            graphics.drawString(this.font, Component.translatable("gui.enchantransformer.no_enchantment"),
                    x, y, COLOR_GRAY, false);
            return;
        }
        Component name = Enchantment.getFullname(next, EnchantTransferLogic.levelOf(input, next));
        graphics.drawString(this.font, name, x, y, COLOR_ENCHANT, false);
        Component cost = Component.translatable("gui.enchantransformer.cost",
                EnchantTransferLogic.price(next));
        graphics.drawString(this.font, cost, x + this.font.width(name) + 6, y, COLOR_GREEN, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 1.21.1 的 AbstractContainerScreen#render 自己<b>不再</b>调用 renderTooltip（它只负责
        // 把 hoveredSlot 算出来），必须由子类在 super.render 之后显式调用一次，否则槽位里的
        // 物品完全没有悬浮提示。原版 EnchantmentScreen / ContainerScreen / GrindstoneScreen
        // 也都是这么写的（源码 EnchantmentScreen.java:199）。
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.buttonA != null && this.buttonA.isHovered() && !this.buttonA.active) {
            this.showReason(graphics, EnchantTransferLogic.SLOT_RECEIVER_A, mouseX, mouseY);
        }
        if (this.buttonB != null && this.buttonB.isHovered() && !this.buttonB.active) {
            this.showReason(graphics, EnchantTransferLogic.SLOT_RECEIVER_B, mouseX, mouseY);
        }
    }

    private void showReason(GuiGraphics graphics, int receiverSlot, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        TransferCheck check = checkFor(receiverSlot, this.menu.inputStack(),
                this.minecraft.player.experienceLevel, this.minecraft.player.isCreative());
        if (!check.ok()) {
            graphics.renderTooltip(this.font, check.reason().copy().withStyle(ChatFormatting.GRAY),
                    mouseX, mouseY);
        }
    }
}
