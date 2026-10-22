package com.vivihud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    private static final String[] POS = {"top", "bottom"};
    private static final String[] ALIGN = {"left", "center", "right"};
    private static final Integer[] SCALE = {50, 75, 100, 125, 150, 200};
    private static final Integer[] OPACITY = {30, 45, 60, 75, 90};

    private final Screen parent;

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

    private void add(int x, int y, int w, Supplier<String> label, Runnable action) {
        Button b = Button.builder(Component.literal(label.get()), btn -> {
            action.run();
            Config.save();
            btn.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build();
        addRenderableWidget(b);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int L = cx - 155, R = cx + 5, W = 150;
        int y = Math.max(4, (this.height - 176) / 2);

        Button title = Button.builder(Component.literal("Vivi HUD Settings"), b -> {})
                .bounds(L, y, 310, 20).build();
        title.active = false;
        addRenderableWidget(title);

        y += 22;
        add(L, y, W, () -> "Mod: " + onOff(Config.enabled), () -> Config.enabled = !Config.enabled);
        add(R, y, W, () -> "Position: " + Config.position, () -> Config.position = next(POS, Config.position));
        y += 22;
        add(L, y, W, () -> "Align: " + Config.align, () -> Config.align = next(ALIGN, Config.align));
        add(R, y, W, () -> "Scale: " + Config.scale + "%", () -> Config.scale = next(SCALE, Config.scale));
        y += 22;
        add(L, y, W, () -> "Opacity: " + Config.opacity + "%", () -> Config.opacity = next(OPACITY, Config.opacity));
        add(R, y, W, () -> "Album art: " + onOff(Config.albumArt), () -> Config.albumArt = !Config.albumArt);
        y += 22;
        add(L, y, W, () -> "Equalizer: " + onOff(Config.equalizer), () -> Config.equalizer = !Config.equalizer);
        add(R, y, W, () -> "Scroll title: " + onOff(Config.scrollTitle), () -> Config.scrollTitle = !Config.scrollTitle);
        y += 22;
        add(L, y, W, () -> "Auto compact: " + onOff(Config.autoCompact), () -> Config.autoCompact = !Config.autoCompact);
        add(R, y, W, () -> "Play / Pause", () -> NowPlaying.command("pause"));
        y += 22;
        add(L, y, W, () -> "Previous", () -> NowPlaying.command("prev"));
        add(R, y, W, () -> "Next", () -> NowPlaying.command("next"));
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(L, y, 310, 20).build());
    }

    @Override
    public void onClose() {
        Config.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}