package com.vivihud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class Keys {
    private static KeyMapping pause, next, prev;

    private Keys() {}

    public static void register() {
        KeyMapping.Category cat = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("vivihud", "main"));
        pause = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.vivihud.pause", InputConstants.Type.KEYSYM, -1, cat));
        next = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.vivihud.next", InputConstants.Type.KEYSYM, -1, cat));
        prev = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.vivihud.prev", InputConstants.Type.KEYSYM, -1, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (pause.consumeClick()) NowPlaying.command("pause");
            while (next.consumeClick()) NowPlaying.command("next");
            while (prev.consumeClick()) NowPlaying.command("prev");
        });
    }
}
