package com.homemine.enchanttransformer;

import com.homemine.enchanttransformer.menu.EnchantTransformerMenu;
import com.homemine.enchanttransformer.net.TransformPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(TransformPayload.TYPE, TransformPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player instanceof ServerPlayer serverPlayer
                            && serverPlayer.containerMenu instanceof EnchantTransformerMenu menu) {
                        menu.handleTransform(serverPlayer, payload.targetSlot());
                    }
                }));
    }
}
