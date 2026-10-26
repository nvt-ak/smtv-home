package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

/**
 * Live numbers for the Aurora cards: the weather (Open-Meteo), gold (vang.today) and fuel (Petrolimex) prices, and
 * the lunar card, which is computed offline.
 *
 * One fetch thread at a time, only while Home is in front and the last answer is older than its refresh interval.
 * Raw answers are cached in SharedPreferences so a TV that boots offline still shows the last numbers with their own
 * "updated" time. A value that could not be fetched stays null and is drawn as a dash, never as a made-up number.
 */
final class LiveData {
    static final int CLEAR = 0, PARTLY = 1, CLOUD = 2, RAIN = 3, STORM = 4, FOG = 5;

    private static final long WEATHER_MS = 20 * 60_000L;
    private static final long PRICES_MS = 30 * 60_000L;
    private static final String GOLD_URL = "https://www.vang.today/api/prices";
    private static final String FUEL_URL = "https://portals.petrolimex.com.vn/~apis/portals/cms.item/search?x-request=";
    private static final String FUEL_FILTER = "{\"FilterBy\":{\"And\":[{\"SystemID\":{\"Equals\":\"6783dc1271ff449e95b74a9520964169\"}},"
            + "{\"RepositoryID\":{\"Equals\":\"a95451e23b474fe5886bfb7cf843f53c\"}},"
            + "{\"RepositoryEntityID\":{\"Equals\":\"3801378fe1e045b1afa10de7c5776124\"}}]}}";

    static final class Weather {
        String city = "";
        String text = "";
        String note = "";
        double temp, high, low;
        /** Relative humidity in percent, wind in km/h and visibility in km; negative when the API gave none. */
        double humidity = -1, wind = -1, visibility = -1;
        boolean day = true;
        int kind;
        final String[] hourLabel = new String[5];
        final double[] hourTemp = new double[5];
        final int[] hourKind = new int[5];
        long at;
    }

    static final class Row {
        String name, unit;
        double value;
        /** Change since the previous price; NaN when unknown. */
        double delta = Double.NaN;
    }

    static final class Prices {
        Row[] rows = new Row[4];
        long at;
    }

    static final class LunarCard {
        String line1, line2, solar, canChi, hours1, hours2, next1, next2;
        /** The Hà Nội card's pieces: day number, month, weekday, day / month / year stem-branch, hours with their range. */
        String dayNum, month, weekday, dayName, monthName, yearName;
        /** The month as in "27 tháng 3": the number (or Giêng / Chạp) with " nhuận" for a leap month. */
        String monthShort = "";
        /** True when the day is a hoàng đạo day (the day spirit is one of the six lucky ones). */
        boolean auspicious;
        String[] hourRanges = new String[0];
        int dayKey;
    }

    volatile Weather weather;
    volatile Prices prices;
    volatile LunarCard lunar;
    /** US AQI from Open-Meteo's air-quality API, or -1 when unknown. */
    volatile int aqi = -1;

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String city;
    private final String lat;
    private final String lon;
    private final int lunarOffset;
    private volatile boolean busy;

    LiveData(Context ctx, String city, double lat, double lon, int lunarOffset) {
        this.ctx = ctx.getApplicationContext();
        this.prefs = this.ctx.getSharedPreferences("live", Context.MODE_PRIVATE);
        this.city = city;
        this.lat = String.format(Locale.US, "%.3f", lat);
        this.lon = String.format(Locale.US, "%.3f", lon);
        this.lunarOffset = lunarOffset;
        try {
            parseWeather(prefs.getString("weather", null), prefs.getLong("weather_at", 0));
            parseGold(prefs.getString("gold", null));
            parseFuel(prefs.getString("fuel", null), prefs.getLong("prices_at", 0));
            parseAqi(prefs.getString("aqi", null));
        } catch (Exception ignored) {
            // A damaged cache is the same as none.
        }
        // Offline and instant: the header shows the lunar date from the first frame.
        lunar = computeLunar();
    }

    /** Recomputes the lunar card when the day has changed (cheap, offline); true when it did. */
    boolean refreshLunar() {
        LunarCard k = lunar;
        if (k != null && k.dayKey == dayKey()) return false;
        lunar = computeLunar();
        return true;
    }

    /** Fetches whatever is stale, off the main thread, then runs {@code done} on it. */
    void refreshIfStale(Runnable done) {
        if (busy) return;
        busy = true;
        new Thread(() -> {
            try {
                long now = System.currentTimeMillis();
                Weather w = weather;
                if (w == null || now - w.at > WEATHER_MS) fetchWeather();
                Prices p = prices;
                if (p == null || now - p.at > PRICES_MS) fetchPrices();
                int key = dayKey();
                if (lunar == null || lunar.dayKey != key) lunar = computeLunar();
            } finally {
                busy = false;
                main.post(done);
            }
        }, "live-data").start();
    }

    private static int dayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    // ---- network -------------------------------------------------------------------------------

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "smtv-home");
            if (c.getResponseCode() != 200) throw new java.io.IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0 && out.size() < 512 * 1024) out.write(buf, 0, n);
                return out.toString("UTF-8");
            }
        } finally {
            c.disconnect();
        }
    }

    private void fetchWeather() {
        try {
            String raw = get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m,visibility&hourly=temperature_2m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&timezone=auto&forecast_days=3");
            long at = System.currentTimeMillis();
            parseWeather(raw, at);
            prefs.edit().putString("weather", raw).putLong("weather_at", at).apply();
        } catch (Exception ignored) {
            // Keep the cached numbers.
        }
        try {
            String raw = get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lon
                    + "&current=us_aqi");
            parseAqi(raw);
            prefs.edit().putString("aqi", raw).apply();
        } catch (Exception ignored) {
            // Keep the cached number.
        }
    }

    private void parseAqi(String raw) throws Exception {
        if (raw == null) return;
        JSONObject cur = new JSONObject(raw).getJSONObject("current");
        aqi = cur.isNull("us_aqi") ? -1 : (int) Math.round(cur.getDouble("us_aqi"));
    }

    /** The US AQI band in Vietnamese: Tốt, Trung bình, Kém, Xấu, Rất xấu, Nguy hại. */
    static String aqiText(int aqi) {
        if (aqi < 0) return "—";
        if (aqi <= 50) return "Tốt";
        if (aqi <= 100) return "Trung bình";
        if (aqi <= 150) return "Kém";
        if (aqi <= 200) return "Xấu";
        if (aqi <= 300) return "Rất xấu";
        return "Nguy hại";
    }

    /** The colour of that band: green, yellow, orange, red, purple, maroon. */
    static int aqiColor(int aqi) {
        if (aqi < 0) return 0xFFA9B8D0;
        if (aqi <= 50) return 0xFF3DDC84;
        if (aqi <= 100) return 0xFFFFD84D;
        if (aqi <= 150) return 0xFFFF9F43;
        if (aqi <= 200) return 0xFFFF4D5E;
        if (aqi <= 300) return 0xFFB07CFF;
        return 0xFFB5324A;
    }

    /**
     * Whether a day is hoàng đạo: the twelve day spirits run through the day branches starting at a branch that
     * depends on the lunar month (months 1 and 7 start at Tý, 2 and 8 at Dần, 3 and 9 at Thìn, 4 and 10 at Ngọ,
     * 5 and 11 at Thân, 6 and 12 at Tuất); Thanh Long, Minh Đường, Kim Quỹ, Bảo Quang, Ngọc Đường and Tư Mệnh are
     * the lucky ones, at offsets 0, 1, 4, 5, 7 and 10.
     */
    static boolean hoangDao(int lunarMonth, int dayBranch) {
        int start = ((lunarMonth - 1) % 6) * 2;
        int offset = Math.floorMod(dayBranch - start, 12);
        return offset == 0 || offset == 1 || offset == 4 || offset == 5 || offset == 7 || offset == 10;
    }

    private void fetchPrices() {
        long at = System.currentTimeMillis();
        boolean any = false;
        try {
            String raw = get(GOLD_URL);
            parseGold(raw);
            prefs.edit().putString("gold", raw).apply();
            any = true;
        } catch (Exception ignored) {
            // Keep the cached numbers.
        }
        try {
            String q = Base64.encodeToString(FUEL_FILTER.getBytes("UTF-8"), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String raw = get(FUEL_URL + q);
            parseFuel(raw, at);
            prefs.edit().putString("fuel", raw).apply();
            any = true;
        } catch (Exception ignored) {
            // Keep the cached numbers.
        }
        if (any) {
            prefs.edit().putLong("prices_at", at).apply();
            Prices p = prices;
            if (p != null) p.at = at;
        }
    }

    // ---- parsing -------------------------------------------------------------------------------

    private void parseWeather(String raw, long at) throws Exception {
        if (raw == null) return;
        JSONObject j = new JSONObject(raw);
        JSONObject cur = j.getJSONObject("current");
        JSONObject daily = j.getJSONObject("daily");
        Weather w = new Weather();
        w.city = city;
        w.at = at;
        w.temp = cur.getDouble("temperature_2m");
        w.day = cur.optInt("is_day", 1) == 1;
        w.humidity = cur.optDouble("relative_humidity_2m", -1);
        w.wind = cur.optDouble("wind_speed_10m", -1);
        double vis = cur.optDouble("visibility", -1);
        w.visibility = vis < 0 ? -1 : vis / 1000.0;
        int code = cur.getInt("weather_code");
        w.kind = kindOf(code);
        w.text = textOf(code);
        w.high = daily.getJSONArray("temperature_2m_max").getDouble(0);
        w.low = daily.getJSONArray("temperature_2m_min").getDouble(0);
        int rain = daily.getJSONArray("precipitation_probability_max").optInt(0, -1);
        w.note = rain >= 0 ? "Khả năng mưa " + rain + "%" : "";
        JSONObject hourly = j.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temps = hourly.getJSONArray("temperature_2m");
        JSONArray codes = hourly.getJSONArray("weather_code");
        String now = cur.getString("time");
        int start = -1;
        for (int i = 0; i < times.length(); i++) {
            String t = times.getString(i);
            if (t.compareTo(now) > 0 && Integer.parseInt(t.substring(11, 13)) % 3 == 0) {
                start = i;
                break;
            }
        }
        if (start >= 0) {
            for (int k = 0; k < 5 && start + 3 * k < times.length(); k++) {
                int i = start + 3 * k;
                w.hourLabel[k] = Integer.parseInt(times.getString(i).substring(11, 13)) + "h";
                w.hourTemp[k] = temps.getDouble(i);
                w.hourKind[k] = kindOf(codes.getInt(i));
            }
        }
        weather = w;
    }

    static int kindOf(int code) {
        if (code == 0) return CLEAR;
        if (code <= 2) return PARTLY;
        if (code == 3) return CLOUD;
        if (code == 45 || code == 48) return FOG;
        if (code >= 95) return STORM;
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return RAIN;
        return CLOUD;
    }

    static String textOf(int code) {
        if (code == 0) return "Trời quang";
        if (code == 1) return "Ít mây";
        if (code == 2) return "Có mây";
        if (code == 3) return "Nhiều mây";
        if (code == 45 || code == 48) return "Sương mù";
        if (code >= 51 && code <= 57) return "Mưa phùn";
        if (code >= 61 && code <= 67) return "Mưa";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Tuyết";
        if (code >= 80 && code <= 82) return "Mưa rào";
        if (code >= 95) return "Dông";
        return "Nhiều mây";
    }

    private Prices ensurePrices() {
        Prices p = prices;
        if (p == null) {
            p = new Prices();
            prices = p;
        }
        return p;
    }

    private void parseGold(String raw) throws Exception {
        if (raw == null) return;
        JSONObject items = new JSONObject(raw).getJSONObject("prices");
        Prices p = ensurePrices();
        p.rows[0] = goldRow(items.optJSONObject("SJL1L10"), "Vàng SJC (9999)");
        p.rows[1] = goldRow(items.optJSONObject("SJ9999"), "Vàng nhẫn (9999)");
        p.at = Math.max(p.at, prefs.getLong("prices_at", 0));
    }

    private static Row goldRow(JSONObject o, String name) {
        if (o == null) return null;
        Row r = new Row();
        r.name = name;
        r.unit = "1 lượng";
        r.value = o.optDouble("sell", 0);
        if (r.value <= 0) return null;
        if (o.has("change_sell")) r.delta = o.optDouble("change_sell", Double.NaN);
        return r;
    }

    private void parseFuel(String raw, long at) throws Exception {
        if (raw == null) return;
        JSONArray objs = new JSONObject(raw).getJSONArray("Objects");
        Prices p = ensurePrices();
        for (int i = 0; i < objs.length(); i++) {
            JSONObject o = objs.getJSONObject(i);
            String title = o.optString("Title");
            int slot = title.contains("RON 95-III") ? 2 : title.startsWith("DO 0,05S") ? 3 : -1;
            double price = o.optDouble("Zone1Price", 0);
            if (slot < 0 || price <= 0) continue;
            Row r = new Row();
            r.name = slot == 2 ? "Xăng RON 95-III" : "Dầu DO 0,05S";
            r.unit = "1 lít";
            r.value = price;
            // The API has no previous price: remember the one before the last change ourselves.
            String key = "fuel_cur_" + slot;
            double was = Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(0)));
            double prev = Double.longBitsToDouble(prefs.getLong("fuel_prev_" + slot, Double.doubleToLongBits(0)));
            if (was > 0 && was != price) {
                prev = was;
                prefs.edit().putLong("fuel_prev_" + slot, Double.doubleToLongBits(prev)).apply();
            }
            if (was != price) prefs.edit().putLong(key, Double.doubleToLongBits(price)).apply();
            if (prev > 0) r.delta = price - prev;
            p.rows[slot] = r;
        }
        if (at > p.at) p.at = at;
    }

    // ---- lunar card (offline) ------------------------------------------------------------------

    private static final String[] STEMS = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] BRANCHES = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    private static final String[] WEEKDAYS = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    /** Auspicious hour branches by day branch pair (Tý/Ngọ, Sửu/Mùi, Dần/Thân, Mão/Dậu, Thìn/Tuất, Tỵ/Hợi). */
    private static final int[][] GOOD_HOURS = {
            {0, 1, 3, 6, 8, 9}, {2, 3, 5, 8, 10, 11}, {0, 1, 4, 5, 7, 10},
            {0, 2, 3, 6, 7, 9}, {2, 4, 5, 8, 9, 11}, {1, 4, 6, 7, 10, 11}};

    private static String monthName(int m) {
        return m == 1 ? "Giêng" : m == 12 ? "Chạp" : String.valueOf(m);
    }

    private LunarCard computeLunar() {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_MONTH), m = c.get(Calendar.MONTH) + 1, y = c.get(Calendar.YEAR);
        int[] l = Lunar.fromSolar(d, m, y, lunarOffset);
        long jd = Lunar.jdFromDate(d, m, y);
        LunarCard k = new LunarCard();
        k.dayKey = dayKey();
        k.dayNum = String.valueOf(l[0]);
        k.month = "Tháng " + monthName(l[1]) + (l[3] == 1 ? " nhuận" : "");
        k.weekday = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
        k.dayName = STEMS[(int) Math.floorMod(jd + 9, 10L)] + " " + BRANCHES[(int) Math.floorMod(jd + 1, 12L)];
        k.monthName = STEMS[Math.floorMod(l[2] * 12 + l[1] + 3, 10)] + " " + BRANCHES[Math.floorMod(l[1] + 1, 12)];
        k.yearName = STEMS[Math.floorMod(l[2] + 6, 10)] + " " + BRANCHES[Math.floorMod(l[2] + 8, 12)];
        String yearName = STEMS[Math.floorMod(l[2] + 6, 10)] + " " + BRANCHES[Math.floorMod(l[2] + 8, 12)];
        k.monthShort = monthName(l[1]) + (l[3] == 1 ? " nhuận" : "");
        k.auspicious = hoangDao(l[1], (int) Math.floorMod(jd + 1, 12L));
        k.line1 = "Ngày " + l[0] + " tháng " + monthName(l[1]) + (l[3] == 1 ? " nhuận" : "");
        k.line2 = "năm " + yearName;
        k.solar = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + String.format(Locale.US, "%02d/%02d/%d", d, m, y);
        int dayBranch = (int) Math.floorMod(jd + 1, 12L);
        k.canChi = STEMS[(int) Math.floorMod(jd + 9, 10L)] + " " + BRANCHES[dayBranch] + " - " + yearName;
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        int[] good = GOOD_HOURS[dayBranch % 6];
        for (int i = 0; i < good.length; i++) (i < 4 ? a : b).append(i == 0 || i == 4 ? "" : ", ").append(BRANCHES[good[i]]);
        k.hourRanges = new String[good.length];
        for (int i = 0; i < good.length; i++) {
            int from = Math.floorMod(good[i] * 2 - 1, 24);
            k.hourRanges[i] = BRANCHES[good[i]] + " (" + from + "-" + (from + 2) % 24 + "h)";
        }
        k.hours1 = a + ",";
        k.hours2 = b.toString();
        // Next new moon / full moon, and Tết, found by walking forward day by day.
        Calendar t = (Calendar) c.clone();
        String moon = null, tet = null;
        for (int i = 1; i <= 400 && (moon == null || tet == null); i++) {
            t.add(Calendar.DAY_OF_YEAR, 1);
            int[] x = Lunar.fromSolar(t.get(Calendar.DAY_OF_MONTH), t.get(Calendar.MONTH) + 1, t.get(Calendar.YEAR), lunarOffset);
            String when = i == 1 ? "ngày mai" : "còn " + i + " ngày";
            if (moon == null && (x[0] == 1 || x[0] == 15)) moon = (x[0] == 1 ? "Mùng 1 tháng " : "Rằm tháng ") + monthName(x[1]) + " · " + when;
            if (tet == null && x[0] == 1 && x[1] == 1 && x[3] == 0) tet = "Tết Nguyên Đán · " + when;
        }
        if (l[0] == 1 || l[0] == 15) moon = "Hôm nay " + (l[0] == 1 ? "mùng 1" : "rằm");
        k.next1 = moon == null ? "—" : moon;
        k.next2 = tet == null ? "" : tet;
        return k;
    }
}
