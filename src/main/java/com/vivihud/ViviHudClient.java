package com.vivihud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class ViviHudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        NowPlaying.start();
        Keys.register();
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("vivihud", "now_playing"),
                NowPlayingHud::render);
    }
}
