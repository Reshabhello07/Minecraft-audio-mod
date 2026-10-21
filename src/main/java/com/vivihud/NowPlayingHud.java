package com.vivihud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class NowPlayingHud {

    private NowPlayingHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        NowPlaying.Track t = NowPlaying.track;
        if (t == null) return;

        long now = System.currentTimeMillis();
        float fade = 1f;
        if (!t.playing()) {
            long idle = now - NowPlaying.lastPlayingMs;
            if (idle > 6000) return;
            if (idle > 5000) fade = 1f - (idle - 5000) / 1000f;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        long pos = t.positionMs();
        if (t.playing()) pos += now - t.fetchedAtMs();
        if (t.durationMs() > 0 && pos > t.durationMs()) pos = t.durationMs();

        String time = fmt(pos) + (t.durationMs() > 0 ? " / " + fmt(t.durationMs()) : "");
        if (!t.playing()) time = "Paused  " + time;

        int pad = 6;
        int maxText = 170;
        String title = fit(font, t.title(), maxText);
        String artist = fit(font, t.artist(), maxText);

        int bw = Math.max(130, Math.max(font.width(title), font.width(artist)) + pad * 2 + 4);
        bw = Math.max(bw, font.width(time) + pad * 2 + 70);
        int bh = pad + 9 + 2 + 9 + 4 + 9 + pad;

        int x = switch (NowPlaying.align) {
            case "center" -> (sw - bw) / 2;
            case "right" -> sw - bw - 6;
            default -> 6;
        };
        int y = "bottom".equals(NowPlaying.position) ? sh - bh - 40 : 6;

        int bg = argb(fade * 0.6f, 0x000000);
        int accent = argb(fade, 0x55FF88);
        int white = argb(fade, 0xFFFFFF);
        int grey = argb(fade, 0xAAAAAA);
        int barBg = argb(fade, 0x555555);

        g.fill(x, y, x + bw, y + bh, bg);
        g.fill(x, y, x + 2, y + bh, accent);

        int tx = x + pad + 2;
        g.text(font, title, tx, y + pad, white, true);
        g.text(font, artist, tx, y + pad + 11, grey, true);

        int ry = y + pad + 11 + 9 + 4;
        int timeW = font.width(time);
        g.text(font, time, x + bw - pad - timeW, ry, grey, true);

        int barX1 = tx;
        int barX2 = x + bw - pad - timeW - 6;
        int barY = ry + 3;
        g.fill(barX1, barY, barX2, barY + 3, barBg);
        if (t.durationMs() > 0 && barX2 > barX1) {
            int filled = (int) ((barX2 - barX1) * (pos / (double) t.durationMs()));
            g.fill(barX1, barY, barX1 + filled, barY + 3, accent);
        }
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static String fit(Font f, String s, int max) {
        if (f.width(s) <= max) return s;
        String e = "...";
        int ew = f.width(e);
        while (!s.isEmpty() && f.width(s) + ew > max) s = s.substring(0, s.length() - 1);
        return s + e;
    }
}
