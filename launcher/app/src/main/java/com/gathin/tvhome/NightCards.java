package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

/**
 * The cards of the third Hà Nội look (design_profile hanoi3): a tall weather card with a five day forecast on the left,
 * and on its right the gold and fuel prices above today's lunar date. Drawn in the reference design's own units: mock-up
 * pixels of the 2000x1125 design, relative to the box's top-left corner (65, 282) and scaled by {@code u} (view width /
 * 1432). Widgets the theme switched off leave no hole: the right-hand cards move up, and without the weather card they
 * move to the left edge.
 */
final class NightCards {
    /** Width of the box in mock-up units; the view's width maps to it. */
    static final float BOX_W = 1432f;
    /** Height of the box in mock-up units. */
    static final float BOX_H = 635f;

    private static final float WEATHER_W = 723f, WEATHER_H = 595f;
    private static final float COLUMN_X = 745f, COLUMN_W = 687f;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB7C0D0;
    private static final int GOLD = 0xFFFFD27A;
    private static final int UP = 0xFF3DDC84;
    private static final int DOWN = 0xFFFF5D5D;
    private static final int BLUE = 0xFF8EB8FF;
    private static final String[] WEEK = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

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

    NightCards(Context ctx) {
        this.ctx = ctx;
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
            bold = Typeface.create(medium, Typeface.BOLD);
        } catch (RuntimeException e) {
            // The system font is fine.
        }
        fill.setColor(0xB3090E19);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(0x2EFFFFFF);
        line.setColor(0x33FFFFFF);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    void drawAll(Canvas c, float w, float h, LiveData live, Set<String> hidden) {
        u = w / BOX_W;
        border.setStrokeWidth(Math.max(1f, 1.5f * u));
        line.setStrokeWidth(Math.max(1f, 1.5f * u));
        boolean showWeather = !hidden.contains("weather");
        boolean gold = !hidden.contains("gold") && !hidden.contains("prices");
        boolean fuel = !hidden.contains("fuel") && !hidden.contains("prices");
        boolean calendar = !hidden.contains("calendar");

        if (showWeather) {
            card(c, 0, 0, WEATHER_W, WEATHER_H, 24);
            at(c, 0, 0);
            weather(c, live);
            c.restore();
        }
        float x = showWeather ? COLUMN_X : 0;
        float y = 0;
        if (gold || fuel) {
            int rows = (gold ? 1 : 0) + (fuel ? 1 : 0);
            float height = 98 + 125 * rows;
            card(c, x, y, COLUMN_W, height, 24);
            at(c, x, y);
            market(c, live, gold, fuel);
            c.restore();
            y += height + 18;
        }
        if (calendar) {
            card(c, x, y, COLUMN_W, 267, 24);
            at(c, x, y);
            lunar(c, live);
            c.restore();
        }
    }

    // ---- helpers -------------------------------------------------------------------------------

    private void at(Canvas c, float x, float y) {
        c.save();
        c.translate(x * u, y * u);
    }

    private void card(Canvas c, float x, float y, float w, float h, float radius) {
        r.set(x * u, y * u, (x + w) * u, (y + h) * u);
        c.drawRoundRect(r, radius * u, radius * u, fill);
        c.drawRoundRect(r, radius * u, radius * u, border);
    }

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
            case LiveData.PARTLY: return day ? R.drawable.ic_wx_partly : R.drawable.ic_wx_big;
            case LiveData.RAIN: return R.drawable.ic_wx_rain;
            case LiveData.STORM: return R.drawable.ic_wx_storm;
            case LiveData.FOG: return R.drawable.ic_wx_fog;
            default: return R.drawable.ic_wx_cloud;
        }
    }

    private void pin(Canvas c, float cx, float cy, float s) {
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(BLUE);
        path.reset();
        path.addCircle(cx * u, (cy - s * 0.12f) * u, s * 0.36f * u, Path.Direction.CW);
        path.moveTo((cx - s * 0.32f) * u, (cy + s * 0.08f) * u);
        path.lineTo(cx * u, (cy + s * 0.5f) * u);
        path.lineTo((cx + s * 0.32f) * u, (cy + s * 0.08f) * u);
        path.close();
        c.drawPath(path, shape);
        shape.setColor(0xFF1B2D52);
        c.drawCircle(cx * u, (cy - s * 0.12f) * u, s * 0.13f * u, shape);
    }

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

    /** A thin chevron pointing right, centred on (cx, cy). */
    private void chevron(Canvas c, float cx, float cy, float s) {
        shape.setStyle(Paint.Style.STROKE);
        shape.setStrokeCap(Paint.Cap.ROUND);
        shape.setStrokeJoin(Paint.Join.ROUND);
        shape.setStrokeWidth(Math.max(1.5f, 2.6f * u));
        shape.setColor(0xCCFFFFFF);
        path.reset();
        path.moveTo((cx - s * 0.3f) * u, (cy - s * 0.5f) * u);
        path.lineTo((cx + s * 0.3f) * u, cy * u);
        path.lineTo((cx - s * 0.3f) * u, (cy + s * 0.5f) * u);
        c.drawPath(path, shape);
    }

    private static String number(double v) {
        return java.text.NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(Math.round(Math.abs(v)));
    }

    // ---- weather -------------------------------------------------------------------------------

    private void weather(Canvas c, LiveData live) {
        LiveData.Weather wx = live == null ? null : live.weather;
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(0xFF1B2D52);
        c.drawCircle(53 * u, 53 * u, 22 * u, shape);
        pin(c, 53, 53, 24);
        put(c, wx == null || wx.city.isEmpty() ? "—" : wx.city, 98, 62, 26, medium, TEXT, 330);

        sky(c, wx, 495, 163);

        String temp = wx == null ? "—" : Math.round(wx.temp) + "°";
        put(c, temp, 36, 213, 132, bold, TEXT, 330);
        put(c, wx == null ? "Chưa có dữ liệu" : wx.text, 38, 262, 27, regular, TEXT, 330);

        // "Cảm giác như 27°  |  Độ ẩm 78%  |  Gió 8 km/h"
        String[] facts = {
                wx == null || Double.isNaN(wx.feels) ? null : "Cảm giác như " + Math.round(wx.feels) + "°",
                wx == null || wx.humidity < 0 ? null : "Độ ẩm " + Math.round(wx.humidity) + "%",
                wx == null || wx.wind < 0 ? null : "Gió " + Math.round(wx.wind) + " km/h"};
        float x = 38;
        boolean first = true;
        for (String f : facts) {
            if (f == null) continue;
            if (!first) {
                c.drawLine((x + 22) * u, 318 * u, (x + 22) * u, 344 * u, line);
                x += 44;
            }
            put(c, f, x, 337, 24, regular, 0xFFDDE3EE, 650 - x);
            x += width(f, 24, regular);
            first = false;
        }

        Calendar day = Calendar.getInstance();
        for (int k = 0; k < 5; k++) {
            float tx = 32 + 135.3f * k;
            r.set(tx * u, 383 * u, (tx + 120) * u, 560 * u);
            shape.setStyle(Paint.Style.FILL);
            shape.setColor(0x16FFFFFF);
            c.drawRoundRect(r, 18 * u, 18 * u, shape);
            c.drawRoundRect(r, 18 * u, 18 * u, border);
            float cx = tx + 60;
            putCenter(c, WEEK[day.get(Calendar.DAY_OF_WEEK) - 1], cx, 424, 20, regular, TEXT, 110);
            day.add(Calendar.DAY_OF_MONTH, 1);
            if (wx != null && k < wx.days) {
                vec(c, k == 0 ? weatherIcon(wx.kind, wx.day) : weatherIcon(wx.dayKind[k], true), cx, 468, 56);
                putCenter(c, Math.round(wx.dayLow[k]) + "°", cx - 28, 533, 22, regular, 0xFFB9C4D8, 56);
                putCenter(c, Math.round(wx.dayHigh[k]) + "°", cx + 28, 533, 22, medium, TEXT, 56);
            } else {
                putCenter(c, "—", cx, 478, 26, regular, MUTED, 100);
            }
        }
    }

    /** The big picture beside the temperature: the moon (with cloud) at night, the sun by day, the weather icon otherwise. */
    private void sky(Canvas c, LiveData.Weather wx, float cx, float cy) {
        if (wx == null) {
            vec(c, R.drawable.ic_wx_cloud, cx, cy, 190);
            return;
        }
        boolean moonlit = !wx.day && (wx.kind == LiveData.CLEAR || wx.kind == LiveData.PARTLY);
        if (!moonlit) {
            vec(c, weatherIcon(wx.kind, wx.day), cx, cy, 190);
            return;
        }
        float rad = 82;
        shape.setStyle(Paint.Style.FILL);
        shape.setShader(new RadialGradient(cx * u, cy * u, rad * 2.1f * u,
                new int[] {0x44FFE9B0, 0x00FFE9B0}, null, Shader.TileMode.CLAMP));
        c.drawCircle(cx * u, cy * u, rad * 2.1f * u, shape);
        shape.setShader(new RadialGradient((cx - 22) * u, (cy - 24) * u, rad * 1.6f * u,
                new int[] {0xFFFFF3D2, 0xFFE9D3A0, 0xFFB09660}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx * u, cy * u, rad * u, shape);
        shape.setShader(null);
        shape.setColor(0x229A8450);
        c.drawCircle((cx - 30) * u, (cy - 14) * u, 17 * u, shape);
        c.drawCircle((cx + 24) * u, (cy + 20) * u, 22 * u, shape);
        c.drawCircle((cx + 8) * u, (cy - 40) * u, 11 * u, shape);
        if (wx.kind == LiveData.PARTLY) {
            cloud(c, cx - 70, cy + 62, 1.25f);
            cloud(c, cx + 52, cy + 38, 0.9f);
        }
    }

    /** A soft cloud bank made of overlapping discs, its underside dark and its top edge lit by the moon. */
    private void cloud(Canvas c, float cx, float cy, float k) {
        path.reset();
        path.addCircle((cx - 62 * k) * u, (cy + 6 * k) * u, 30 * k * u, Path.Direction.CW);
        path.addCircle((cx - 22 * k) * u, (cy - 12 * k) * u, 42 * k * u, Path.Direction.CW);
        path.addCircle((cx + 28 * k) * u, (cy - 2 * k) * u, 36 * k * u, Path.Direction.CW);
        path.addCircle((cx + 66 * k) * u, (cy + 12 * k) * u, 26 * k * u, Path.Direction.CW);
        shape.setStyle(Paint.Style.FILL);
        shape.setShader(new LinearGradient(0, (cy - 50 * k) * u, 0, (cy + 40 * k) * u,
                new int[] {0xEE9FB2CE, 0xEE3A4B6B, 0xEE1D2840}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(path, shape);
        shape.setShader(null);
    }

    // ---- gold and fuel ---------------------------------------------------------------------------

    private void market(Canvas c, LiveData live, boolean gold, boolean fuel) {
        LiveData.Prices p = live == null ? null : live.prices;
        // The title's mark: three bars of a chart.
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(BLUE);
        float[] bars = {14, 24, 34};
        for (int i = 0; i < 3; i++) {
            r.set((40 + 11 * i) * u, (63 - bars[i]) * u, (47 + 11 * i) * u, 63 * u);
            c.drawRoundRect(r, 2 * u, 2 * u, shape);
        }
        put(c, "Thị trường hôm nay", 95, 57, 26, medium, TEXT, 480);
        float top = 90;
        if (gold) {
            priceRow(c, top, R.drawable.ic_gold, "Vàng SJC (1 lượng)", p == null ? null : p.rows[0]);
            top += 125;
        }
        if (fuel) priceRow(c, top, R.drawable.ic_fuel, "Xăng RON95 (1 lít)", p == null ? null : p.rows[2]);
    }

    private void priceRow(Canvas c, float top, int icon, String label, LiveData.Row row) {
        float left = 20, w = COLUMN_W - 40, h = 110;
        r.set(left * u, top * u, (left + w) * u, (top + h) * u);
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(0x40000000);
        c.drawRoundRect(r, 18 * u, 18 * u, shape);
        c.drawRoundRect(r, 18 * u, 18 * u, border);

        vec(c, icon, left + 62, top + 55, 84);
        put(c, label, left + 127, top + 41, 22, regular, MUTED, 300);
        String value = row == null ? "—" : number(row.value) + " đ";
        put(c, value, left + 127, top + 87, 30, bold, GOLD, 235);
        chevron(c, left + w - 26, top + h / 2f, 22);
        if (row == null || Double.isNaN(row.delta) || Math.round(row.delta) == 0) return;

        boolean up = row.delta > 0;
        int color = up ? UP : DOWN;
        double before = row.value - row.delta;
        String delta = (up ? "+" : "-") + number(row.delta);
        String pct = before > 0 ? String.format(new Locale("vi", "VN"), "%s%.2f%%", up ? "+" : "-", Math.abs(row.delta) / before * 100) : "";
        float tri = left + w - 290;
        triangle(c, tri, top + 66, 17, up, color);
        float dx = tri + 30;
        put(c, delta, dx, top + 75, 22, regular, color, 130);
        if (!pct.isEmpty()) put(c, pct, dx + Math.min(130f, width(delta, 22, regular)) + 24, top + 75, 22, regular, color, 100);
    }

    // ---- lunar date ------------------------------------------------------------------------------

    private void lunar(Canvas c, LiveData live) {
        LiveData.LunarCard k = live == null ? null : live.lunar;
        lotus(c, 618, 112);
        shape.setStyle(Paint.Style.STROKE);
        shape.setStrokeCap(Paint.Cap.ROUND);
        shape.setStrokeWidth(Math.max(1.5f, 2.4f * u));
        shape.setColor(BLUE);
        r.set(40 * u, 36 * u, 64 * u, 58 * u);
        c.drawRoundRect(r, 4 * u, 4 * u, shape);
        c.drawLine(40 * u, 43 * u, 64 * u, 43 * u, shape);
        c.drawLine(47 * u, 31 * u, 47 * u, 38 * u, shape);
        c.drawLine(57 * u, 31 * u, 57 * u, 38 * u, shape);
        put(c, "Lịch âm dương", 95, 56, 26, medium, TEXT, 480);

        Calendar now = Calendar.getInstance();
        putCenter(c, k == null ? "—" : k.dayNum, 80, 160, 90, bold, TEXT, 120);
        put(c, k == null ? "" : "Tháng " + k.monthShort + " (ÂL)", 133, 123, 26, medium, TEXT, 170);
        put(c, k == null ? "" : "Năm " + k.yearName, 133, 158, 24, regular, MUTED, 170);
        c.drawLine(302 * u, 90 * u, 302 * u, 166 * u, line);
        String[] weekday = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
        put(c, weekday[now.get(Calendar.DAY_OF_WEEK) - 1], 334, 121, 26, medium, TEXT, 300);
        put(c, now.get(Calendar.DAY_OF_MONTH) + " tháng " + (now.get(Calendar.MONTH) + 1) + ", " + now.get(Calendar.YEAR),
                334, 156, 24, regular, 0xFFDDE3EE, 300);

        c.drawLine(40 * u, 195 * u, (COLUMN_W - 40) * u, 195 * u, line);
        String advice = k == null ? "" : k.auspicious ? "Ngày tốt: Xuất hành, gặp gỡ, ký kết"
                : "Ngày thường: nên làm việc nhỏ, tránh việc lớn";
        put(c, advice, 56, 232, 22, regular, MUTED, COLUMN_W - 150);
        chevron(c, COLUMN_W - 50, 222, 20);
    }

    /** A faint lotus: five petals fanned out from one point, the colour of old gold. */
    private void lotus(Canvas c, float cx, float cy) {
        shape.setStyle(Paint.Style.FILL);
        shape.setColor(0x1FD9A24A);
        for (int i = -2; i <= 2; i++) {
            c.save();
            c.rotate(i * 28f, cx * u, (cy + 40) * u);
            path.reset();
            path.moveTo(cx * u, (cy + 40) * u);
            path.quadTo((cx - 24) * u, (cy - 10) * u, cx * u, (cy - 62) * u);
            path.quadTo((cx + 24) * u, (cy - 10) * u, cx * u, (cy + 40) * u);
            c.drawPath(path, shape);
            c.restore();
        }
    }
}
