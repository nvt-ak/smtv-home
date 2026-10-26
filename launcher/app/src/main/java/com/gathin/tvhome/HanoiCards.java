package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.Locale;

/**
 * The right-hand column of the Hà Nội look: the weather card, the gold and fuel cards side by side, and the month
 * with today's lunar date. Drawn in the reference design's own units: mock-up pixels of the 2000x1125 design,
 * relative to the column's top-left corner (1165, 265) and scaled by {@code u} (view width / 795).
 */
final class HanoiCards {
    /** Width of the column in mock-up units; the view's width maps to it. */
    static final float COLUMN_W = 795f;
    /** Height of the column in mock-up units (weather 213, gap 15, prices 155, gap 15, month 349). */
    static final float COLUMN_H = 747f;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA9B8D0;
    private static final int DIM = 0x66A9B8D0;
    private static final int UP = 0xFFFF5D5D;
    private static final int DOWN = 0xFF3DDC84;
    private static final int TODAY = 0xFFE5384B;
    private static final String[] WEEK = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final Context ctx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shape = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private Typeface regular = Typeface.DEFAULT;
    private Typeface medium = Typeface.DEFAULT;
    private Typeface bold = Typeface.DEFAULT_BOLD;
    private float u = 1f;

    HanoiCards(Context ctx) {
        this.ctx = ctx;
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
            bold = Typeface.create(medium, Typeface.BOLD);
        } catch (RuntimeException e) {
            // The system font is fine.
        }
        fill.setColor(0xB00A1120);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(0x2EFFFFFF);
        line.setColor(0x26FFFFFF);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Draws the column into a {@code w} x {@code h} view. Widgets the theme switched off ({@code hidden}: weather, gold,
     * fuel, calendar) leave no hole: the rest move up, and a lone gold or fuel card takes the whole row.
     */
    void drawAll(Canvas c, float w, float h, LiveData live, java.util.Set<String> hidden) {
        u = w / COLUMN_W;
        border.setStrokeWidth(Math.max(1f, 1.5f * u));
        line.setStrokeWidth(Math.max(1f, 1.5f * u));
        float y = 0;
        if (!hidden.contains("weather")) {
            card(c, 0, y, 795, 213);
            c.save();
            c.translate(0, y * u);
            weather(c, live);
            c.restore();
            y += 213 + 15;
        }
        boolean gold = !hidden.contains("gold") && !hidden.contains("prices");
        boolean fuel = !hidden.contains("fuel") && !hidden.contains("prices");
        if (gold || fuel) {
            LiveData.Prices p = live == null ? null : live.prices;
            float goldW = gold && fuel ? 388 : 795;
            if (gold) {
                card(c, 0, y, goldW, 155);
                price(c, 0, y, R.drawable.ic_gold, "Giá vàng (SJC)", p == null ? null : p.rows[0], "đ/lượng");
            }
            if (fuel) {
                float x = gold ? 405 : 0;
                card(c, x, y, gold ? 390 : 795, 155);
                price(c, x, y, R.drawable.ic_fuel, "Giá xăng (RON 95-III)", p == null ? null : p.rows[2], "đ/lít");
            }
            y += 155 + 15;
        }
        if (!hidden.contains("calendar")) {
            card(c, 0, y, 795, 349);
            c.save();
            c.translate(0, y * u);
            month(c, live);
            c.restore();
        }
    }

    private void card(Canvas c, float x, float y, float w, float h) {
        r.set(x * u, y * u, (x + w) * u, (y + h) * u);
        c.drawRoundRect(r, 24 * u, 24 * u, fill);
        c.drawRoundRect(r, 24 * u, 24 * u, border);
    }

    // ---- helpers -------------------------------------------------------------------------------

    private void style(float size, Typeface tf, int color, Paint.Align align) {
        text.setTypeface(tf);
        text.setTextSize(size * u);
        text.setColor(color);
        text.setTextAlign(align);
    }

    private void put(Canvas c, String s, float x, float base, float size, Typeface tf, int color, float maxW) {
        style(size, tf, color, Paint.Align.LEFT);
        c.drawText(TextUtils.ellipsize(s, text, maxW * u, TextUtils.TruncateAt.END).toString(), x * u, base * u, text);
    }

    private void putRight(Canvas c, String s, float right, float base, float size, Typeface tf, int color, float maxW) {
        style(size, tf, color, Paint.Align.RIGHT);
        c.drawText(TextUtils.ellipsize(s, text, maxW * u, TextUtils.TruncateAt.END).toString(), right * u, base * u, text);
    }

    private void putCenter(Canvas c, String s, float cx, float base, float size, Typeface tf, int color, float maxW) {
        style(size, tf, color, Paint.Align.CENTER);
        c.drawText(TextUtils.ellipsize(s, text, maxW * u, TextUtils.TruncateAt.END).toString(), cx * u, base * u, text);
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

    /** A map pin, light blue, with its centre on (cx, cy). */
    private void pin(Canvas c, float cx, float cy, float s) {
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(0xFF8EB8FF);
        path.reset();
        path.addCircle(cx * u, (cy - s * 0.12f) * u, s * 0.36f * u, Path.Direction.CW);
        path.moveTo((cx - s * 0.32f) * u, (cy + s * 0.08f) * u);
        path.lineTo(cx * u, (cy + s * 0.5f) * u);
        path.lineTo((cx + s * 0.32f) * u, (cy + s * 0.08f) * u);
        path.close();
        c.drawPath(path, shape);
        shape.setColor(0xFF0A1120);
        c.drawCircle(cx * u, (cy - s * 0.12f) * u, s * 0.13f * u, shape);
    }

    /** A bold arrow pointing up or down, centred on (cx, cy). */
    private void arrow(Canvas c, float cx, float cy, float s, boolean up, int color) {
        shape.setStyle(Paint.Style.STROKE);
        shape.setStrokeCap(Paint.Cap.ROUND);
        shape.setStrokeJoin(Paint.Join.ROUND);
        shape.setStrokeWidth(Math.max(1.5f, 3f * u));
        shape.setColor(color);
        float d = up ? -1 : 1;
        path.reset();
        path.moveTo(cx * u, (cy + d * s * 0.5f) * u);
        path.lineTo(cx * u, (cy - d * s * 0.5f) * u);
        path.moveTo((cx - s * 0.32f) * u, (cy - d * s * 0.18f) * u);
        path.lineTo(cx * u, (cy - d * s * 0.5f) * u);
        path.lineTo((cx + s * 0.32f) * u, (cy - d * s * 0.18f) * u);
        c.drawPath(path, shape);
    }

    /** A solid triangle pointing up or down. */
    private void triangle(Canvas c, float cx, float cy, float s, boolean up, int color) {
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(color);
        path.reset();
        float d = up ? -1 : 1;
        path.moveTo(cx * u, (cy + d * s * 0.5f) * u);
        path.lineTo((cx - s * 0.6f) * u, (cy - d * s * 0.5f) * u);
        path.lineTo((cx + s * 0.6f) * u, (cy - d * s * 0.5f) * u);
        path.close();
        c.drawPath(path, shape);
    }

    /** A leaf, in the colour of the air quality band. */
    private void leaf(Canvas c, float cx, float cy, float s, int color) {
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(color);
        path.reset();
        path.moveTo((cx - s * 0.5f) * u, (cy + s * 0.5f) * u);
        path.quadTo((cx - s * 0.55f) * u, (cy - s * 0.45f) * u, (cx + s * 0.5f) * u, (cy - s * 0.5f) * u);
        path.quadTo((cx + s * 0.55f) * u, (cy + s * 0.45f) * u, (cx - s * 0.5f) * u, (cy + s * 0.5f) * u);
        path.close();
        c.drawPath(path, shape);
    }

    private static String number(double v) {
        return java.text.NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(Math.round(Math.abs(v)));
    }

    // ---- weather -------------------------------------------------------------------------------

    private void weather(Canvas c, LiveData live) {
        LiveData.Weather wx = live == null ? null : live.weather;
        pin(c, 36, 38, 28);
        put(c, wx == null || wx.city.isEmpty() ? "—" : wx.city, 62, 47, 27, bold, TEXT, 300);
        vec(c, wx == null ? R.drawable.ic_wx_cloud : weatherIcon(wx.kind, wx.day), 76, 132, 112);
        String temp = wx == null ? "—" : String.valueOf(Math.round(wx.temp));
        put(c, temp, 172, 142, 74, bold, TEXT, 200);
        if (wx != null) put(c, "°C", 172 + width(temp, 74, bold) + 4, 106, 38, medium, TEXT, 80);
        put(c, wx == null ? "Chưa có dữ liệu" : wx.text, 172, 182, 24, regular, MUTED, 220);

        arrow(c, 358, 99, 24, true, UP);
        put(c, wx == null ? "—" : Math.round(wx.high) + "°", 384, 108, 25, medium, TEXT, 90);
        arrow(c, 358, 148, 24, false, 0xFF4DA3FF);
        put(c, wx == null ? "—" : Math.round(wx.low) + "°", 384, 157, 25, medium, TEXT, 90);

        c.drawLine(473 * u, 25 * u, 473 * u, 190 * u, line);
        float ix = 521, lx = 549, vx = 683;
        vec(c, R.drawable.ic_drop, ix, 53, 30);
        put(c, "Độ ẩm", lx, 61, 21, regular, MUTED, vx - lx - 8);
        put(c, wx == null || wx.humidity < 0 ? "—" : Math.round(wx.humidity) + "%", vx, 61, 23, medium, TEXT, 100);
        vec(c, R.drawable.ic_wind, ix, 100, 30);
        put(c, "Gió", lx, 108, 21, regular, MUTED, vx - lx - 8);
        put(c, wx == null || wx.wind < 0 ? "—" : Math.round(wx.wind) + " km/h", vx, 108, 23, medium, TEXT, 105);
        int aqi = live == null ? -1 : live.aqi;
        leaf(c, ix, 146, 26, LiveData.aqiColor(aqi));
        put(c, "Chất lượng", lx, 150, 21, regular, MUTED, vx - lx - 8);
        put(c, "không khí", lx, 177, 21, regular, MUTED, vx - lx - 8);
        put(c, LiveData.aqiText(aqi), vx, 150, 23, medium, LiveData.aqiColor(aqi), 105);
    }

    // ---- gold and fuel ---------------------------------------------------------------------------

    private void price(Canvas c, float ox, float oy, int icon, String title, LiveData.Row row, String unit) {
        c.save();
        c.translate(ox * u, oy * u);
        vec(c, icon, 48, 48, 58);
        put(c, title, 100, 46, 21, regular, MUTED, 275);
        String value = row == null ? "—" : number(row.value);
        put(c, value, 100, 90, 34, bold, TEXT, 190);
        if (row != null) put(c, unit, 100 + width(value, 34, bold) + 8, 90, 21, regular, MUTED, 90);
        if (row != null && !Double.isNaN(row.delta) && Math.round(row.delta) != 0) {
            boolean up = row.delta > 0;
            double before = row.value - row.delta;
            String pct = before > 0 ? String.format(new Locale("vi", "VN"), " (%s%.2f%%)", up ? "+" : "-", Math.abs(row.delta) / before * 100) : "";
            triangle(c, 112, 122, 15, up, up ? UP : DOWN);
            put(c, (up ? "+" : "-") + number(row.delta) + pct, 134, 130, 22, medium, up ? UP : DOWN, 240);
        }
        c.restore();
    }

    // ---- month ---------------------------------------------------------------------------------

    private void month(Canvas c, LiveData live) {
        c.save();
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR), mon = now.get(Calendar.MONTH), today = now.get(Calendar.DAY_OF_MONTH);
        put(c, "Tháng " + (mon + 1) + ", " + year, 26, 49, 27, bold, TEXT, 440);

        Calendar first = Calendar.getInstance();
        first.clear();
        first.set(year, mon, 1);
        int lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int days = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar prev = (Calendar) first.clone();
        prev.add(Calendar.MONTH, -1);
        int prevDays = prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (lead + days + 6) / 7;
        float pitch = rows >= 6 ? 36f : 43.5f;
        for (int i = 0; i < 7; i++) {
            putCenter(c, WEEK[i], 45 + 66 * i, 92, 19, regular, MUTED, 60);
        }
        for (int cell = 0; cell < rows * 7; cell++) {
            int col = cell % 7, row = cell / 7;
            int dayNo = cell - lead + 1;
            String s;
            int color = TEXT;
            if (dayNo < 1) {
                s = String.valueOf(prevDays + dayNo);
                color = DIM;
            } else if (dayNo > days) {
                s = String.valueOf(dayNo - days);
                color = DIM;
            } else {
                s = String.valueOf(dayNo);
            }
            float cx = 45 + 66 * col, cy = 135 + pitch * row;
            if (dayNo == today) {
                shape.setStyle(Paint.Style.FILL);
                shape.setColor(TODAY);
                c.drawCircle(cx * u, cy * u, Math.min(24f, pitch / 2f + 2f) * u, shape);
                color = TEXT;
            }
            putCenter(c, s, cx, cy + 8, 22, dayNo == today ? medium : regular, color, 60);
        }

        c.drawLine(498 * u, 25 * u, 498 * u, 327 * u, line);
        LiveData.LunarCard k = live == null ? null : live.lunar;
        put(c, "Hôm nay", 525, 52, 20, regular, MUTED, 250);
        put(c, now.get(Calendar.DAY_OF_MONTH) + " tháng " + (mon + 1) + ", " + year, 525, 88, 27, bold, TEXT, 255);
        put(c, k == null ? "" : "(" + k.dayNum + " tháng " + k.monthShort + " ÂL)", 525, 120, 20, regular, MUTED, 250);
        String[] items = k == null ? new String[0]
                : new String[] {"Ngày " + k.dayName, "Tháng " + k.monthName, "Năm " + k.yearName};
        for (int i = 0; i < items.length; i++) {
            float y = 160 + 37 * i;
            shape.setStyle(Paint.Style.FILL);
            shape.setColor(TODAY);
            c.drawCircle(537 * u, (y - 6) * u, 5 * u, shape);
            put(c, items[i], 560, y, 22, regular, TEXT, 225);
        }
        if (k != null && k.next1 != null && !k.next1.isEmpty() && !"—".equals(k.next1)) {
            r.set(530 * u, 265 * u, 772 * u, 315 * u);
            shape.setStyle(Paint.Style.FILL);
            shape.setColor(0x14FFFFFF);
            c.drawRoundRect(r, 25 * u, 25 * u, shape);
            c.drawRoundRect(r, 25 * u, 25 * u, border);
            putCenter(c, k.next1, 651, 297, 19, medium, TEXT, 222);
        }
        c.restore();
    }
}
