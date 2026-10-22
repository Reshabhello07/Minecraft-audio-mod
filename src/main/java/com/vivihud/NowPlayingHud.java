package com.vivihud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class NowPlayingHud {

    private static float vis = 0f;       // slide/fade 0..1
    private static float expand = 1f;    // 0 compact .. 1 full
    private static long lastNs = System.nanoTime();
    private static long changedAt = 0;
    private static String lastKey = "";

    private NowPlayingHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        long nowNs = System.nanoTime();
        float d = Math.min(0.1f, (nowNs - lastNs) / 1e9f);
        lastNs = nowNs;
        long now = System.currentTimeMillis();

        NowPlaying.Track t = NowPlaying.track;
        boolean show = Config.enabled && t != null
                && (t.playing() || now - NowPlaying.lastPlayingMs < 5000);
        vis = approach(vis, show ? 1f : 0f, d * 4f);
        if (t != null) {
            String key = t.title() + "|" + t.artist();
            if (!key.equals(lastKey)) {
                lastKey = key;
                changedAt = now;
            }
        }
        if (vis <= 0.002f || t == null) return;

        boolean wantFull = !Config.autoCompact || now - changedAt < 5000;
        expand = approach(expand, wantFull ? 1f : 0f, d * 4f);
        float ev = smooth(vis);
        float ee = smooth(expand);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float s = Config.scale / 100f;
        int vw = (int) (mc.getWindow().getGuiScaledWidth() / s);
        int vh = (int) (mc.getWindow().getGuiScaledHeight() / s);
        boolean art = Config.albumArt && ArtTexture.ready;
        int rgb = t.color() & 0xFFFFFF;

        // size
        int cx0 = 9 + (art ? 18 : 0) + (Config.equalizer ? 14 : 0);
        int compW = cx0 + Math.min(font.width(t.title()), 110) + 6;
        int w = Math.round(lerp(compW, 180, ee));
        int h = Math.round(lerp(20, 44, ee));

        int x = switch (Config.align) {
            case "center" -> (vw - w) / 2;
            case "right" -> vw - w - 6;
            default -> 6;
        };
        boolean bottom = "bottom".equals(Config.position);
        int y = bottom ? vh - h - Math.round(38 / s) : 6;
        int off = Math.round((1f - ev) * (h + 12));
        y += bottom ? off : -off;

        float o = Config.opacity / 100f;

        g.pose().pushMatrix();
        g.pose().scale(s, s);

        // shadow + background + accent
        rr(g, x - 1, y + 1, w + 2, h + 2, argb(ev * 0.25f, 0x000000));
        rr(g, x, y, w, h, argb(ev * o, 0x0E1116));
        g.fill(x + 3, y + 5, x + 5, y + h - 5, argb(ev, rgb));

        boolean full = ee >= 0.5f;
        float a = ev * (full ? (ee - 0.5f) * 2f : (0.5f - ee) * 2f);
        int white = argb(a, 0xFFFFFF);
        int grey = argb(a, 0xAAAAAA);
        int accent = argb(a, rgb);

        if (full) {
            int ax = x + 9;
            int tx = ax;
            if (art) {
                art(g, ax, y + 4, 36);
                tx = ax + 36 + 6;
            }
            int areaW = x + w - 8 - tx;
            int titleW = areaW - (Config.equalizer ? 14 : 0);
            marquee(g, font, t.title(), tx, y + 6, titleW, white, now);
            if (Config.equalizer) eq(g, x + w - 18, y + 15, t.playing(), accent, now);
            g.text(font, fit(font, t.artist(), areaW), tx, y + 17, grey, true);

            long pos = t.positionMs();
            if (t.playing()) pos += now - t.fetchedAtMs();
            if (t.durationMs() > 0 && pos > t.durationMs()) pos = t.durationMs();
            String time = (t.playing() ? "" : "Paused  ") + fmt(pos)
                    + (t.durationMs() > 0 ? " / " + fmt(t.durationMs()) : "");
            int timeW = font.width(time);
            g.text(font, time, x + w - 8 - timeW, y + 29, grey, true);

            int bx1 = tx;
            int bx2 = x + w - 8 - timeW - 6;
            int by = y + 33;
            if (bx2 > bx1) {
                g.fill(bx1, by, bx2, by + 3, argb(a, 0x444A52));
                if (t.durationMs() > 0) {
                    int filled = (int) ((bx2 - bx1) * (pos / (double) t.durationMs()));
                    g.fill(bx1, by, bx1 + filled, by + 3, accent);
                    int dx = bx1 + filled;
                    g.fill(dx - 1, by - 1, dx + 2, by + 4, white);
                    g.fill(dx - 2, by, dx + 3, by + 3, white);
                }
            }
        } else {
            int cx = x + 9;
            if (art) {
                art(g, cx, y + 3, 14);
                cx += 18;
            }
            if (Config.equalizer) {
                eq(g, cx, y + 14, t.playing(), accent, now);
                cx += 14;
            }
            marquee(g, font, t.title(), cx, y + 6, x + w - 6 - cx, white, now);
        }

        g.pose().popMatrix();
    }

    // ---- helpers ----

    private static void art(GuiGraphicsExtractor g, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, ArtTexture.ID, x, y, 0f, 0f, size, size, 64, 64, 64, 64);
    }

    private static void eq(GuiGraphicsExtractor g, int x, int baseY, boolean playing, int color, long now) {
        for (int i = 0; i < 4; i++) {
            int bh = playing ? 2 + (int) (Math.abs(Math.sin(now / 180.0 + i * 1.7)) * 7) : 2;
            g.fill(x + i * 3, baseY - bh, x + i * 3 + 2, baseY, color);
        }
    }

    private static void marquee(GuiGraphicsExtractor g, Font f, String s, int x, int y,
                                int areaW, int color, long now) {
        int tw = f.width(s);
        if (tw <= areaW) {
            g.text(f, s, x, y, color, true);
        } else if (!Config.scrollTitle) {
            g.text(f, fit(f, s, areaW), x, y, color, true);
        } else {
            int span = tw + 24;
            long ts = Math.max(0, now - changedAt - 1500);
            int off = (int) ((ts / 40) % span);
            g.enableScissor(x, y - 1, x + areaW, y + 10);
            g.text(f, s, x - off, y, color, true);
            g.text(f, s, x - off + span, y, color, true);
            g.disableScissor();
        }
    }

    /** Rounded rectangle (radius ~3) built from three disjoint bands. */
    private static void rr(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + h, color);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, color);
        g.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 3, x + 1, y + h - 3, color);
        g.fill(x + w - 1, y + 3, x + w, y + h - 3, color);
    }

    private static float approach(float v, float target, float step) {
        return v + Math.max(-step, Math.min(step, target - v));
    }

    private static float smooth(float v) {
        v = Math.max(0f, Math.min(1f, v));
        return v * v * (3f - 2f * v);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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
