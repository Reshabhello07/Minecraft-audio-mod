package com.vivihud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    private static final String[] POS = {"top", "bottom", "custom"};
    private static final String[] ALIGN = {"left", "center", "right"};
    private static final String[] THEMES = {"dark", "glass", "minimal", "neon", "spotify"};
    private static final String[] ACCENTS = {"auto", "green", "blue", "pink", "purple", "orange", "red", "white"};
    private static final Integer[] SCALE = {50, 75, 100, 125, 150, 200};
    private static final Integer[] OPACITY = {30, 45, 60, 75, 90};

    private final Screen parent;
    private int page = 0;
    private Button posBtn;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Vivi HUD"));
        this.parent = parent;
    }

    private static <T> T next(T[] a, T cur) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(cur)) return a[(i + 1) % a.length];
        return a[0];
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    private Button add(int x, int y, int w, Supplier<String> label, Runnable action) {
        Button b = Button.builder(Component.literal(label.get()), btn -> {
            action.run();
            Config.save();
            btn.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build();
        addRenderableWidget(b);
        return b;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int L = cx - 155, R = cx + 5, W = 150;
        int y = Math.max(4, (this.height - 154) / 2);

        Button title = Button.builder(Component.literal(page == 0 ? "Vivi HUD - General" : "Vivi HUD - Look & Position"), b -> {})
                .bounds(L, y, 310, 20).build();
        title.active = false;
        addRenderableWidget(title);
        y += 22;

        if (page == 0) {
            add(L, y, W, () -> "Mod: " + onOff(Config.enabled), () -> Config.enabled = !Config.enabled);
            add(R, y, W, () -> "Auto compact: " + onOff(Config.autoCompact), () -> Config.autoCompact = !Config.autoCompact);
            y += 22;
            add(L, y, W, () -> "Album art: " + onOff(Config.albumArt), () -> Config.albumArt = !Config.albumArt);
            add(R, y, W, () -> "Equalizer: " + onOff(Config.equalizer), () -> Config.equalizer = !Config.equalizer);
            y += 22;
            add(L, y, W, () -> "Scroll title: " + onOff(Config.scrollTitle), () -> Config.scrollTitle = !Config.scrollTitle);
            add(R, y, W, () -> "Lyrics: " + onOff(Config.lyrics), () -> Config.lyrics = !Config.lyrics);
            y += 22;
            add(L, y, W, () -> "Previous", () -> NowPlaying.command("prev"));
            add(R, y, W, () -> "Play / Pause", () -> NowPlaying.command("pause"));
            y += 22;
            add(L, y, W, () -> "Next", () -> NowPlaying.command("next"));
            add(R, y, W, () -> "Look & Position  >", () -> {
                page = 1;
                clearWidgets();
                init();
            });
        } else {
            add(L, y, W, () -> "Theme: " + Config.theme, () -> Config.theme = next(THEMES, Config.theme));
            add(R, y, W, () -> "Accent: " + Config.accent, () -> Config.accent = next(ACCENTS, Config.accent));
            y += 22;
            posBtn = add(L, y, W, () -> "Position: " + Config.position, () -> Config.position = next(POS, Config.position));
            add(R, y, W, () -> "Align: " + Config.align, () -> Config.align = next(ALIGN, Config.align));
            y += 22;
            add(L, y, W, () -> "Scale: " + Config.scale + "%", () -> Config.scale = next(SCALE, Config.scale));
            add(R, y, W, () -> "Opacity: " + Config.opacity + "%", () -> Config.opacity = next(OPACITY, Config.opacity));
            y += 22;
            addRenderableWidget(new PosSlider(L, y, W, "X", Config.posX, v -> {
                Config.posX = (int) Math.round(v * 100);
                setCustom();
            }));
            addRenderableWidget(new PosSlider(R, y, W, "Y", Config.posY, v -> {
                Config.posY = (int) Math.round(v * 100);
                setCustom();
            }));
            y += 22;
            add(L, y, W, () -> "<  General", () -> {
                page = 0;
                clearWidgets();
                init();
            });
        }

        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(L, y, 310, 20).build());
    }

    private void setCustom() {
        Config.position = "custom";
        if (posBtn != null) posBtn.setMessage(Component.literal("Position: custom"));
    }

    @Override
    public void onClose() {
        Config.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private static class PosSlider extends AbstractSliderButton {
        private final String label;
        private final DoubleConsumer onChange;

        PosSlider(int x, int y, int w, String label, int percent, DoubleConsumer onChange) {
            super(x, y, w, 20, Component.literal(label + ": " + percent + "%"), percent / 100.0);
            this.label = label;
            this.onChange = onChange;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + (int) Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(this.value);
        }
    }
}
