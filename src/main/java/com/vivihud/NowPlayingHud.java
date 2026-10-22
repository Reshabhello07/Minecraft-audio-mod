package com.vivihud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class NowPlayingHud {

    /** border: 0 none, 1 glass (white), 2 neon (accent). fixedAccent -1 = use cover color. */
    private record Theme(int bg, float bgMul, boolean shadow, boolean stripe, int border,
                         int fixedAccent, boolean accentTitle, boolean whiteBar) {}

    private static float vis = 0f;       // slide/fade 0..1
    private static float expand = 1f;    // 0 compact .. 1 full
    private static long lastNs = System.nanoTime();
    private static long changedAt = 0;
    private static String lastKey = "";

    private NowPlayingHud() {}

    private static Theme theme() {
        return switch (Config.theme) {
            case "glass" -> new Theme(0x1B2433, 0.55f, true, false, 1, -1, false, false);
            case "minimal" -> new Theme(0x000000, 0.45f, false, false, 0, -1, false, false);
            case "neon" -> new Theme(0x05060A, 1.0f, false, false, 2, -1, true, false);
            case "spotify" -> new Theme(0x121212, 1.1f, true, false, 0, 0x1DB954, false, true);
            default -> new Theme(0x0E1116, 1.0f, true, true, 0, -1, false, false);
        };
    }

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

        long pos = t.positionMs();
        if (t.playing()) pos += now - t.fetchedAtMs();
        if (t.durationMs() > 0 && pos > t.durationMs()) pos = t.durationMs();

        boolean hasLyr = Config.lyrics && Lyrics.available();
        String lyr = hasLyr ? Lyrics.currentLine(pos) : "";
        if (lyr.isEmpty()) lyr = "...";

        Theme th = theme();
        int rgb;
        if (Config.accentRgb() >= 0) rgb = Config.accentRgb();
        else if (th.fixedAccent() >= 0) rgb = th.fixedAccent();
        else rgb = t.color() & 0xFFFFFF;

        // size
        int cx0 = 9 + (art ? 18 : 0) + (Config.equalizer ? 14 : 0);
        int compW = hasLyr ? Math.max(190, cx0 + 60) : cx0 + Math.min(font.width(t.title()), 110) + 6;
        int fullH = hasLyr ? 56 : 44;
        int w = Math.round(lerp(compW, 180, ee));
        int h = Math.round(lerp(20, fullH, ee));

        // position
        int x, y;
        boolean upward;
        if ("custom".equals(Config.position)) {
            x = Math.round((vw - w) * Config.posX / 100f);
            y = Math.round((vh - h) * Config.posY / 100f);
            upward = Config.posY < 50;
        } else {
            x = switch (Config.align) {
                case "center" -> (vw - w) / 2;
                case "right" -> vw - w - 6;
                default -> 6;
            };
            boolean bottom = "bottom".equals(Config.position);
            y = bottom ? vh - h - Math.round(38 / s) : 6;
            upward = !bottom;
        }
        int off = Math.round((1f - ev) * (h + 12));
        y += upward ? -off : off;

        float o = Config.opacity / 100f;

        g.pose().pushMatrix();
        g.pose().scale(s, s);

        // background, shadow, border, stripe
        if (th.shadow()) rr(g, x - 1, y + 1, w + 2, h + 2, argb(ev * 0.25f, 0x000000));
        rr(g, x, y, w, h, argb(Math.min(1f, ev * o * th.bgMul()), th.bg()));
        if (th.border() == 1) {
            border(g, x, y, w, h, argb(ev * 0.22f, 0xFFFFFF));
        } else if (th.border() == 2) {
            border(g, x - 1, y - 1, w + 2, h + 2, argb(ev * 0.25f, rgb));
            border(g, x, y, w, h, argb(ev, rgb));
        }
        if (th.stripe()) g.fill(x + 3, y + 5, x + 5, y + h - 5, argb(ev, rgb));

        boolean full = ee >= 0.5f;
        float a = ev * (full ? (ee - 0.5f) * 2f : (0.5f - ee) * 2f);
        int white = argb(a, 0xFFFFFF);
        int grey = argb(a, 0xAAAAAA);
        int accent = argb(a, rgb);
        int titleColor = th.accentTitle() ? accent : white;
        int barFill = th.whiteBar() ? white : accent;

        if (full) {
            int ax = x + 9;
            int tx = ax;
            if (art) {
                art(g, ax, y + 4, 36);
                tx = ax + 36 + 6;
            }
            int areaW = x + w - 8 - tx;
            int titleW = areaW - (Config.equalizer ? 14 : 0);
            marquee(g, font, t.title(), tx, y + 6, titleW, titleColor, now);
            if (Config.equalizer) eq(g, x + w - 18, y + 15, t.playing(), accent, now);
            g.text(font, fit(font, t.artist(), areaW), tx, y + 17, grey, true);

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
                    g.fill(bx1, by, bx1 + filled, by + 3, barFill);
                    int dx = bx1 + filled;
                    g.fill(dx - 1, by - 1, dx + 2, by + 4, white);
                    g.fill(dx - 2, by, dx + 3, by + 3, white);
                }
            }
            if (hasLyr) {
                g.text(font, fit(font, lyr, w - 18), x + 9, y + 46, accent, true);
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
            int areaW = x + w - 6 - cx;
            if (hasLyr) g.text(font, fit(font, lyr, areaW), cx, y + 6, white, true);
            else marquee(g, font, t.title(), cx, y + 6, areaW, titleColor, now);
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

    /** Rounded rectangle (radius ~3) built from disjoint bands. */
    private static void rr(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + h, color);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, color);
        g.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 3, x + 1, y + h - 3, color);
        g.fill(x + w - 1, y + 3, x + w, y + h - 3, color);
    }

    /** 1px outline that follows the rounded shape (corners skipped). */
    private static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + 1, color);
        g.fill(x + 3, y + h - 1, x + w - 3, y + h, color);
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
