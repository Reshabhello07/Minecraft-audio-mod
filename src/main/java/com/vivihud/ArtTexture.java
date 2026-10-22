package com.vivihud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;

/** Holds the album art as a dynamic texture. */
public final class ArtTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("vivihud", "album_art");
    public static volatile boolean ready = false;
    private static DynamicTexture tex;

    private ArtTexture() {}

    public static void clear() {
        ready = false;
    }

    public static void set(byte[] png) {
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
                Minecraft mc = Minecraft.getInstance();
                if (tex != null) mc.getTextureManager().release(ID);
                tex = new DynamicTexture(() -> "vivihud_album_art", img);
                mc.getTextureManager().register(ID, tex);
                ready = true;
            } catch (Exception e) {
                ready = false;
            }
        });
    }
}
