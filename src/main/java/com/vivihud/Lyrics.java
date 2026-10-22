package com.vivihud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses synced LRC lyrics and finds the current line. */
public final class Lyrics {
    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\\]");

    private record Entry(long t, String s) {}

    private static volatile long[] times = new long[0];
    private static volatile String[] texts = new String[0];

    private Lyrics() {}

    public static void clear() {
        times = new long[0];
        texts = new String[0];
    }

    public static boolean available() {
        return times.length > 0;
    }

    public static void set(String lrc) {
        List<Entry> list = new ArrayList<>();
        for (String line : lrc.split("\\r?\\n")) {
            Matcher m = TS.matcher(line);
            List<Long> stamps = new ArrayList<>();
            while (m.find()) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                String f = m.group(3);
                long ms = f == null ? 0 : Long.parseLong((f + "00").substring(0, 3));
                stamps.add(min * 60000 + sec * 1000 + ms);
            }
            if (stamps.isEmpty()) continue;
            String text = TS.matcher(line).replaceAll("").trim();
            for (long st : stamps) list.add(new Entry(st, text));
        }
        list.sort(Comparator.comparingLong(Entry::t));
        long[] tt = new long[list.size()];
        String[] ss = new String[list.size()];
        for (int i = 0; i < tt.length; i++) {
            tt[i] = list.get(i).t();
            ss[i] = list.get(i).s();
        }
        texts = ss;
        times = tt;
    }

    /** Current line for the given playback position ("" before the first line). */
    public static String currentLine(long posMs) {
        long[] tt = times;
        String[] ss = texts;
        if (tt.length == 0 || tt.length != ss.length) return "";
        int lo = 0, hi = tt.length - 1, idx = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (tt[mid] <= posMs) {
                idx = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return idx < 0 ? "" : ss[idx];
    }
}
