package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * The three cards of the Hà Nội look (weather, lunar calendar, gold and fuel prices), drawn in the reference
 * design's own units: mock-up pixels of the 2000x1125 design relative to the card's corner, scaled by
 * {@code u} (card height / 510).
 */
final class HanoiRowCards {
    static final float CARD_H = 510f;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA9B8D0;
    private static final int GOLD = 0xFFFFD84D;
    private static final int GREEN = 0xFF3DDC84;
    private static final int RED = 0xFFFF4D5E;
    private static final int PURPLE = 0xFFB07CFF;

    private final Context ctx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private Typeface regular = Typeface.DEFAULT;
    private Typeface medium = Typeface.DEFAULT;
    private Typeface bold = Typeface.DEFAULT_BOLD;
    private float u = 1f;

    HanoiRowCards(Context ctx) {
        this.ctx = ctx;
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
            bold = Typeface.create(medium, Typeface.BOLD);
        } catch (RuntimeException e) {
            // The system font is fine.
        }
        fill.setColor(0xB30A1226);
        border.setStyle(Paint.Style.STROKE);
        line.setColor(0x26FFFFFF);
        panel.setColor(0x330A1840);
    }

    void draw(Canvas c, int index, float w, float h, LiveData live) {
        u = h / CARD_H;
        r.set(0, 0, w, h);
        c.drawRoundRect(r, 28 * u, 28 * u, fill);
        border.setStrokeWidth(Math.max(1f, 1.6f * u));
        border.setShader(new LinearGradient(0, 0, w, h, 0x9974E5F4, 0x99A07CFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 28 * u, 28 * u, border);
        line.setStrokeWidth(Math.max(1f, u));
        if (index == 0) weather(c, w, live);
        else if (index == 1) lunar(c, w, live);
        else prices(c, w, live);
    }

    private void put(Canvas c, String s, float x, float base, float size, Typeface tf, int color, boolean right, float maxW) {
        text.setTypeface(tf);
        text.setTextSize(size * u);
        text.setColor(color);
        text.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        String t = TextUtils.ellipsize(s, text, maxW * u, TextUtils.TruncateAt.END).toString();
        c.drawText(t, x * u, base * u, text);
    }

    private float width(String s, float size, Typeface tf) {
        text.setTypeface(tf);
        text.setTextSize(size * u);
        return text.measureText(s) / u;
    }

    private void vec(Canvas c, int res, float cx, float cy, float size) {
        android.graphics.drawable.Drawable d = ctx.getDrawable(res);
        if (d == null) return;
        d.setBounds(Math.round((cx - size / 2) * u), Math.round((cy - size / 2) * u),
                Math.round((cx + size / 2) * u), Math.round((cy + size / 2) * u));
        d.draw(c);
    }

    private void rule(Canvas c, float x1, float x2, float y) {
        c.drawLine(x1 * u, y * u, x2 * u, y * u, line);
    }

    private static int weatherIcon(int kind, boolean day) {
        switch (kind) {
            case LiveData.CLEAR: return day ? R.drawable.ic_wx_sun : R.drawable.ic_moon;
            case LiveData.PARTLY: return R.drawable.ic_wx_big;
            case LiveData.RAIN: return R.drawable.ic_wx_rain;
            case LiveData.STORM: return R.drawable.ic_wx_storm;
            case LiveData.FOG: return R.drawable.ic_wx_fog;
            default: return R.drawable.ic_wx_cloud;
        }
    }

    // ---- weather -------------------------------------------------------------------------------

    private void weather(Canvas c, float w, LiveData live) {
        LiveData.Weather wx = live == null ? null : live.weather;
        vec(c, wx == null ? R.drawable.ic_wx_cloud : weatherIcon(wx.kind, wx.day), 50, 53, 46);
        put(c, "Thời tiết " + (wx == null ? "" : wx.city), 90, 64, 28, medium, TEXT, false, w / u - 110);
        String temp = wx == null ? "—" : String.valueOf(Math.round(wx.temp));
        put(c, temp, 34, 192, 92, bold, TEXT, false, 200);
        if (wx != null) put(c, "°C", 34 + width(temp, 92, bold) + 4, 150, 44, medium, TEXT, false, 100);
        put(c, wx == null ? "Chưa có dữ liệu" : wx.text, 36, 236, 24, regular, TEXT, false, 330);
        put(c, wx == null ? "▲ —" : "▲ " + Math.round(wx.high) + "°C", 36, 292, 22, medium, 0xFFFF6B6B, false, 120);
        put(c, wx == null ? "▼ —" : "▼ " + Math.round(wx.low) + "°C", 170, 292, 22, medium, 0xFF4DA3FF, false, 120);
        if (wx != null) vec(c, weatherIcon(wx.kind, wx.day), w / u - 112, 165, 150);

        r.set(24 * u, 330 * u, (w / u - 24) * u, 488 * u);
        c.drawRoundRect(r, 22 * u, 22 * u, panel);
        float col = (w / u - 48) / 3f;
        int[] icons = {R.drawable.ic_drop, R.drawable.ic_wind, R.drawable.ic_eye};
        String[] labels = {"Độ ẩm", "Gió", "Tầm nhìn"};
        String[] values = {
                wx == null || wx.humidity < 0 ? "—" : Math.round(wx.humidity) + "%",
                wx == null || wx.wind < 0 ? "—" : Math.round(wx.wind) + " km/h",
                wx == null || wx.visibility < 0 ? "—" : Math.round(wx.visibility) + " km"};
        for (int i = 0; i < 3; i++) {
            float x = 24 + col * i + 18;
            vec(c, icons[i], x + 16, 372, 34);
            put(c, labels[i], x, 424, 20, regular, MUTED, false, col - 24);
            put(c, values[i], x, 462, 26, bold, TEXT, false, col - 20);
            if (i > 0) c.drawLine((24 + col * i) * u, 355 * u, (24 + col * i) * u, 470 * u, line);
        }
    }

    // ---- lunar ---------------------------------------------------------------------------------

    private void lunar(Canvas c, float w, LiveData live) {
        float wm = w / u;
        LiveData.LunarCard k = live == null ? null : live.lunar;
        vec(c, R.drawable.ic_calendar, 52, 53, 42);
        put(c, "Lịch âm hôm nay", 90, 64, 28, medium, TEXT, false, wm - 110);
        String day = k == null ? "—" : k.dayNum;
        put(c, day, 34, 170, 92, bold, TEXT, false, 160);
        float mx = 34 + Math.max(width(day, 92, bold), 90) + 22;
        put(c, k == null ? "" : k.month, mx, 135, 30, medium, TEXT, false, wm - mx - 20);
        put(c, k == null ? "" : "Năm " + k.yearName, mx, 170, 24, regular, MUTED, false, wm - mx - 20);
        rule(c, 30, wm - 30, 195);

        vec(c, R.drawable.ic_yinyang, wm - 70, 275, 100);
        put(c, k == null ? "—" : k.weekday, 34, 252, 38, bold, PURPLE, false, wm - 140);
        put(c, k == null ? "" : "Ngày " + k.dayName, 36, 292, 28, medium, TEXT, false, wm - 140);
        put(c, k == null ? "" : "Tháng " + k.monthName, 36, 324, 22, regular, MUTED, false, wm - 140);
        put(c, k == null ? "" : "Năm " + k.yearName, 36, 354, 22, regular, MUTED, false, wm - 140);
        rule(c, 30, wm - 30, 376);

        put(c, "Giờ hoàng đạo", 36, 412, 24, bold, TEXT, false, wm - 60);
        if (k != null) {
            for (int i = 0; i < k.hourRanges.length; i++) {
                float x = i % 2 == 0 ? 36 : 36 + (wm - 60) / 2f;
                put(c, k.hourRanges[i], x, 444 + 28 * (i / 2), 20, regular, MUTED, false, (wm - 60) / 2f - 6);
            }
        }
    }

    // ---- prices --------------------------------------------------------------------------------

    private static String number(double v) {
        return java.text.NumberFormat.getIntegerInstance(new java.util.Locale("vi", "VN")).format(Math.round(Math.abs(v)));
    }

    private void prices(Canvas c, float w, LiveData live) {
        float wm = w / u;
        LiveData.Prices pr = live == null ? null : live.prices;
        vec(c, R.drawable.ic_chart, 52, 53, 42);
        put(c, "Giá vàng & xăng dầu", 90, 64, 28, medium, TEXT, false, wm - 100);
        String[] names = {"Vàng SJC (1 lượng)", "Vàng nhẫn 9999 (1 lượng)", "Xăng RON 95-III (1 lít)", "Dầu diesel 0,05S (1 lít)"};
        int[] icons = {R.drawable.ic_gold, R.drawable.ic_ring, R.drawable.ic_fuel, R.drawable.ic_fuel};
        for (int i = 0; i < 4; i++) {
            float y = 107 * i;
            LiveData.Row row = pr == null ? null : pr.rows[i];
            vec(c, icons[i], 56, 142 + y, 50);
            put(c, names[i], 117, 124 + y, 20, regular, MUTED, false, wm - 140);
            put(c, row == null ? "—" : number(row.value) + " đ", 116, 163 + y, 34, bold, i < 2 ? GOLD : TEXT, false, wm - 250);
            if (row != null && !Double.isNaN(row.delta) && Math.round(row.delta) != 0) {
                boolean up = row.delta > 0;
                put(c, (up ? "▲ +" : "▼ -") + number(row.delta), wm - 32, 162 + y, 20, medium, up ? GREEN : RED, true, 150);
            }
            if (i < 3) rule(c, 30, wm - 30, 190 + y);
        }
    }
}
