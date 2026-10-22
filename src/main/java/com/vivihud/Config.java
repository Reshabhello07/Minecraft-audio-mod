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
    public static boolean lyrics = true;
    public static String theme = "dark";     // dark | glass | minimal | neon | spotify
    public static String accent = "auto";    // auto | green | blue | pink | purple | orange | red | white
    public static int posX = 5;              // percent, used when position=custom
    public static int posY = 5;
    public static String position = "top";   // top | bottom | custom
    public static String align = "left";     // left | center | right
    public static int scale = 100;           // percent
    public static int opacity = 75;          // percent
    public static int port = 8765;

    private Config() {}

    /** Returns -1 for auto (use cover / theme color). */
    public static int accentRgb() {
        return switch (accent) {
            case "green" -> 0x55FF88;
            case "blue" -> 0x4DA3FF;
            case "pink" -> 0xFF5FA8;
            case "purple" -> 0xA66BFF;
            case "orange" -> 0xFF9F43;
            case "red" -> 0xFF4D4D;
            case "white" -> 0xFFFFFF;
            default -> -1;
        };
    }

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
            lyrics = bool(pr, "lyrics", true);
            theme = pr.getProperty("theme", "dark").trim().toLowerCase();
            accent = pr.getProperty("accent", "auto").trim().toLowerCase();
            posX = Integer.parseInt(pr.getProperty("posX", "5").trim());
            posY = Integer.parseInt(pr.getProperty("posY", "5").trim());
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
            pr.setProperty("lyrics", String.valueOf(lyrics));
            pr.setProperty("theme", theme);
            pr.setProperty("accent", accent);
            pr.setProperty("posX", String.valueOf(posX));
            pr.setProperty("posY", String.valueOf(posY));
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
