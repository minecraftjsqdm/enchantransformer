package com.homemine.enchanttransformer.enchant;

import net.minecraft.network.chat.Component;

/**
 * 一次转移请求的可行性判定结果。每个非 OK 值都对应一条给玩家看的禁用原因。
 */
public enum TransferCheck {

    OK(""),
    NO_INPUT("gui.enchantransformer.reason.no_input"),
    NO_RECEIVER("gui.enchantransformer.reason.no_receiver"),
    NO_ENCHANTMENT("gui.enchantransformer.reason.no_enchantment"),
    INSUFFICIENT_LEVEL("gui.enchantransformer.reason.insufficient_level"),
    SAME_OR_HIGHER("gui.enchantransformer.reason.same_or_higher"),
    NOT_ENCHANTABLE("gui.enchantransformer.reason.not_enchantable"),
    INCOMPATIBLE("gui.enchantransformer.reason.incompatible");

    private final String translationKey;

    TransferCheck(String translationKey) {
        this.translationKey = translationKey;
    }

    public boolean ok() {
        return this == OK;
    }

    /** 禁用原因文案；OK 时返回空文本。 */
    public Component reason() {
        return this == OK ? Component.empty() : Component.translatable(this.translationKey);
    }
}
