package com.vivihud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Polls the companion app on localhost and keeps the latest track. */
public final class NowPlaying {

    public record Track(String title, String artist, boolean playing,
                        long positionMs, long durationMs, long fetchedAtMs) {}

    public static volatile Track track;
    public static volatile long lastPlayingMs;

    // config (config/vivihud.properties)
    public static String position = "top";   // top | bottom
    public static String align = "left";     // left | center | right
    public static int port = 8765;

    private NowPlaying() {}

    public static void start() {
        loadConfig();
        Thread t = new Thread(NowPlaying::loop, "vivihud-poller");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            long sleep = 1000;
            try {
                poll();
            } catch (Exception e) {
                track = null;
                sleep = 3000;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    private static void poll() throws Exception {
        HttpURLConnection c = (HttpURLConnection)
                URI.create("http://127.0.0.1:" + port + "/now").toURL().openConnection();
        c.setConnectTimeout(800);
        c.setReadTimeout(800);
        try (InputStream in = c.getInputStream()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            if (!o.has("ok") || !o.get("ok").getAsBoolean()) {
                track = null;
                return;
            }
            String title = o.get("title").getAsString();
            if (title.isEmpty()) {
                track = null;
                return;
            }
            boolean playing = o.get("playing").getAsBoolean();
            long now = System.currentTimeMillis();
            if (playing) lastPlayingMs = now;
            track = new Track(title, o.get("artist").getAsString(), playing,
                    o.get("position").getAsLong(), o.get("duration").getAsLong(), now);
        } finally {
            c.disconnect();
        }
    }

    private static void loadConfig() {
        try {
            Path p = FabricLoader.getInstance().getConfigDir().resolve("vivihud.properties");
            if (!Files.exists(p)) {
                Files.writeString(p, """
                        # position: top or bottom
                        position=top
                        # align: left, center or right
                        align=left
                        # must match the companion app
                        port=8765
                        """);
            }
            Properties props = new Properties();
            try (var r = Files.newBufferedReader(p)) {
                props.load(r);
            }
            position = props.getProperty("position", "top").trim().toLowerCase();
            align = props.getProperty("align", "left").trim().toLowerCase();
            port = Integer.parseInt(props.getProperty("port", "8765").trim());
        } catch (Exception ignored) {
        }
    }
}
