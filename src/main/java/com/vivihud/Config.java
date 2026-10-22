package com.vivihud;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    public static boolean enabled = true;
    public static boolean albumArt = true;
    public static boolean equalizer = true;
    public static boolean scrollTitle = true;
    public static boolean autoCompact = true;
    public static String position = "top";   // top | bottom
    public static String align = "left";     // left | center | right
    public static int scale = 100;           // percent
    public static int opacity = 75;          // percent
    public static int port = 8765;

    private Config() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("vivihud.properties");
    }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) {
                save();
                return;
            }
            Properties pr = new Properties();
            try (var r = Files.newBufferedReader(p)) {
                pr.load(r);
            }
            enabled = bool(pr, "enabled", true);
            albumArt = bool(pr, "albumArt", true);
            equalizer = bool(pr, "equalizer", true);
            scrollTitle = bool(pr, "scrollTitle", true);
            autoCompact = bool(pr, "autoCompact", true);
            position = pr.getProperty("position", "top").trim().toLowerCase();
            align = pr.getProperty("align", "left").trim().toLowerCase();
            scale = Integer.parseInt(pr.getProperty("scale", "100").trim());
            opacity = Integer.parseInt(pr.getProperty("opacity", "75").trim());
            port = Integer.parseInt(pr.getProperty("port", "8765").trim());
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties pr = new Properties();
            pr.setProperty("enabled", String.valueOf(enabled));
            pr.setProperty("albumArt", String.valueOf(albumArt));
            pr.setProperty("equalizer", String.valueOf(equalizer));
            pr.setProperty("scrollTitle", String.valueOf(scrollTitle));
            pr.setProperty("autoCompact", String.valueOf(autoCompact));
            pr.setProperty("position", position);
            pr.setProperty("align", align);
            pr.setProperty("scale", String.valueOf(scale));
            pr.setProperty("opacity", String.valueOf(opacity));
            pr.setProperty("port", String.valueOf(port));
            try (var w = Files.newBufferedWriter(path())) {
                pr.store(w, "Vivi HUD");
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean bool(Properties p, String k, boolean d) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)).trim());
    }
}
