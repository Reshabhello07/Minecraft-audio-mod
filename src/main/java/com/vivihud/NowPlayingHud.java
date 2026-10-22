package com.vivihud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Polls the companion app on localhost and keeps the latest track. */
public final class NowPlaying {

    public record Track(String title, String artist, boolean playing,
                        long positionMs, long durationMs, long fetchedAtMs, int color) {}

    public static volatile Track track;
    public static volatile long lastPlayingMs;
    private static int lastArt = 0;

    private NowPlaying() {}

    public static void start() {
        Thread t = new Thread(NowPlaying::loop, "vivihud-poller");
        t.setDaemon(true);
        t.start();
    }

    /** cmd: pause | next | prev */
    public static void command(String cmd) {
        Thread t = new Thread(() -> {
            try {
                get("/cmd?c=" + cmd);
            } catch (Exception ignored) {
            }
        }, "vivihud-cmd");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            long sleep = 1000;
            if (!Config.enabled) {
                track = null;
                sleep = 2000;
            } else {
                try {
                    poll();
                } catch (Exception e) {
                    track = null;
                    sleep = 3000;
                }
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    private static byte[] get(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection)
                URI.create("http://127.0.0.1:" + Config.port + path).toURL().openConnection();
        c.setConnectTimeout(800);
        c.setReadTimeout(1500);
        try (InputStream in = c.getInputStream()) {
            return in.readAllBytes();
        } finally {
            c.disconnect();
        }
    }

    private static void poll() throws Exception {
        String body = new String(get("/now"), StandardCharsets.UTF_8);
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

        int artHash = o.has("art") ? o.get("art").getAsInt() : 0;
        int color = o.has("color") ? o.get("color").getAsInt() : 0x55FF88;
        if (artHash != lastArt) {
            lastArt = artHash;
            if (artHash != 0) {
                try {
                    ArtTexture.set(get("/art"));
                } catch (Exception e) {
                    lastArt = Integer.MIN_VALUE; // retry next poll
                }
            } else {
                ArtTexture.clear();
            }
        }

        track = new Track(title, o.get("artist").getAsString(), playing,
                o.get("position").getAsLong(), o.get("duration").getAsLong(), now, color);
    }
}
