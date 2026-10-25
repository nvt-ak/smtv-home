package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * The three Aurora cards (weather, lunar calendar, prices), drawn on a canvas in the reference design's own units.
 *
 * Every number below is in mock-up pixels of the 2000x1125 design, relative to the card's top-left corner, and
 * {@code u} converts them to screen pixels (card height / 442). Phase B: the values are placeholders until the
 * launcher fetches real data.
 */
final class AuroraCards {
    static final float CARD_H = 442f;

    private static final int TEXT = 0xFFF5FBFC;
    private static final int MUTED = 0xFFB9CBD0;
    private static final int CYAN = 0xFF74E5F4;
    private static final int GREEN = 0xFF4CE08A;
    private static final int RED = 0xFFFF5A56;
    private static final int GOLD = 0xFFF2B93B;

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

    AuroraCards(Context ctx) {
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
            bold = Typeface.create(medium, Typeface.BOLD);
        } catch (RuntimeException e) {
            // The system font is fine.
        }
        fill.setColor(0xB30B1C26);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(0x557FCFE0);
        line.setColor(0x26FFFFFF);
        shape.setStyle(Paint.Style.FILL);
    }

    /** Draws card {@code index} (0 weather, 1 lunar, 2 prices) into the box 0,0,w,h of the (already translated) canvas. */
    void draw(Canvas c, int index, float w, float h) {
        u = h / CARD_H;
        border.setStrokeWidth(Math.max(1f, 1.5f * u));
        r.set(0, 0, w, h);
        c.drawRoundRect(r, 26 * u, 26 * u, fill);
        c.drawRoundRect(r, 26 * u, 26 * u, border);
        line.setStrokeWidth(Math.max(1f, u));
        if (index == 0) weather(c, w);
        else if (index == 1) lunar(c, w);
        else prices(c, w);
    }

    // ---- text ----------------------------------------------------------------------------------

    private void put(Canvas c, String s, float x, float base, float size, Typeface tf, int color, boolean right, float maxW) {
        text.setTypeface(tf);
        text.setTextSize(size * u);
        text.setColor(color);
        text.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        String t = TextUtils.ellipsize(s, text, maxW * u, TextUtils.TruncateAt.END).toString();
        c.drawText(t, x * u, base * u, text);
    }

    private void rule(Canvas c, float x1, float x2, float y) {
        c.drawLine(x1 * u, y * u, x2 * u, y * u, line);
    }

    // ---- weather -------------------------------------------------------------------------------

    private void weather(Canvas c, float w) {
        float wm = w / u;   // card width in mock units
        // The moon behind the clouds, top right.
        shape.setShader(new RadialGradient(wm * 0.78f * u, 100 * u, 70 * u,
                new int[] {0x55FFFFFF, 0x14FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(wm * 0.78f * u, 100 * u, 70 * u, shape);
        shape.setShader(null);
        shape.setColor(0xFFE9EEF0);
        c.drawCircle(wm * 0.78f * u, 100 * u, 24 * u, shape);

        cloud(c, 62, 56, 1.0f, true, false);
        put(c, "Thời tiết", 107, 48, 26, medium, TEXT, false, 300);
        put(c, "Hà Nội", 107, 78, 18, regular, MUTED, false, 300);
        put(c, "18°C", 36, 163, 62, bold, TEXT, false, 260);
        put(c, "Trời nhiều mây", 38, 205, 23, medium, TEXT, false, wm - 200);
        put(c, "Có mưa nhẹ về đêm", 38, 238, 18, regular, MUTED, false, wm - 160);
        float right = wm - 52;
        put(c, "▲ 22°", right, 205, 20, medium, TEXT, true, 120);
        put(c, "▼ 16°", right, 238, 20, medium, TEXT, true, 120);
        rule(c, 38, wm - 52, 263);

        String[] hours = {"21h", "0h", "3h", "6h", "9h"};
        String[] temps = {"18°", "17°", "17°", "16°", "19°"};
        float span = wm - 2 * 68f;
        for (int i = 0; i < 5; i++) {
            float cx = 68 + span * i / 4f;
            put(c, hours[i], cx - 16, 310, 18, regular, MUTED, false, 60);
            cloud(c, cx, 350, 0.62f, i < 3, i == 4);
            put(c, temps[i], cx - 18, 406, 21, medium, TEXT, false, 60);
        }
    }

    /** A small cloud (and optional rain or sun) centred at cx,cy, size 1 = 56 mock px wide. */
    private void cloud(Canvas c, float cx, float cy, float s, boolean rain, boolean sun) {
        if (sun) {
            shape.setColor(0xFFFFC94D);
            c.drawCircle((cx - 12 * s) * u, (cy - 12 * s) * u, 13 * s * u, shape);
        }
        shape.setColor(0xFFDCE8F2);
        c.drawCircle((cx - 11 * s) * u, (cy + 2 * s) * u, 12 * s * u, shape);
        c.drawCircle((cx + 2 * s) * u, (cy - 8 * s) * u, 16 * s * u, shape);
        c.drawCircle((cx + 15 * s) * u, (cy + 1 * s) * u, 12 * s * u, shape);
        r.set((cx - 23 * s) * u, (cy + 2 * s) * u, (cx + 27 * s) * u, (cy + 14 * s) * u);
        c.drawRoundRect(r, 6 * s * u, 6 * s * u, shape);
        if (rain) {
            shape.setColor(0xFF6EC8FF);
            for (int i = 0; i < 3; i++) {
                float x = cx + (-12 + i * 12) * s;
                r.set(x * u, (cy + 18 * s) * u, (x + 3 * s) * u, (cy + 27 * s) * u);
                c.drawRoundRect(r, 2 * u, 2 * u, shape);
            }
        }
    }

    // ---- lunar ---------------------------------------------------------------------------------

    private void lunar(Canvas c, float w) {
        float wm = w / u;
        // Crescent moon icon.
        shape.setColor(TEXT);
        c.drawCircle(54 * u, 47 * u, 16 * u, shape);
        shape.setColor(0xFF10222B);
        c.drawCircle(62 * u, 41 * u, 14 * u, shape);
        put(c, "Lịch âm", 90, 54, 26, medium, TEXT, false, 240);

        // "Xem chi tiết >" chip.
        r.set((wm - 175) * u, 29 * u, (wm - 36) * u, 71 * u);
        shape.setStyle(Paint.Style.STROKE);
        shape.setColor(0x66B9CBD0);
        shape.setStrokeWidth(Math.max(1f, u));
        c.drawRoundRect(r, 21 * u, 21 * u, shape);
        shape.setStyle(Paint.Style.FILL);
        put(c, "Xem chi tiết  ›", wm - 166, 56, 16, regular, TEXT, false, 125);

        put(c, "Ngày 15 tháng Chạp", 37, 124, 30, medium, TEXT, false, wm - 120);
        put(c, "năm Giáp Thìn", 37, 162, 30, medium, TEXT, false, wm - 120);
        rule(c, 37, wm - 36, 192);

        float vx = 206;
        float vw = wm - vx - 36;
        put(c, "Dương lịch", 37, 237, 20, regular, MUTED, false, 160);
        put(c, "Thứ Ba, 14/01/2025", vx, 237, 20, regular, TEXT, false, vw);
        put(c, "Can chi", 37, 275, 20, regular, MUTED, false, 160);
        put(c, "Kỷ Mão - Giáp Thìn", vx, 275, 20, regular, TEXT, false, vw);
        put(c, "Giờ hoàng đạo", 37, 314, 20, regular, MUTED, false, 165);
        put(c, "Tý, Sửu, Thìn, Tỵ,", vx, 314, 20, regular, TEXT, false, vw);
        put(c, "Mùi, Tuất", vx, 342, 20, regular, TEXT, false, vw);
        put(c, "Việc nên làm", 37, 386, 20, regular, MUTED, false, 165);
        put(c, "Cầu tài, xuất hành,", vx, 386, 20, regular, TEXT, false, vw);
        put(c, "ươm mầm việc tốt", vx, 414, 20, regular, TEXT, false, vw);
    }

    // ---- prices --------------------------------------------------------------------------------

    private void prices(Canvas c, float w) {
        float wm = w / u;
        // Bar-chart icon.
        shape.setColor(TEXT);
        float[][] bars = {{38, 70, 48}, {55, 56, 48}, {72, 38, 48}};
        for (float[] b : bars) {
            r.set(b[0] * u, b[1] * u, (b[0] + 10) * u, (b[2] + 12 + 10) * u);
            c.drawRoundRect(r, 2 * u, 2 * u, shape);
        }
        put(c, "Giá cả hôm nay", 97, 54, 26, medium, TEXT, false, 270);
        put(c, "Cập nhật 20:24", wm - 36, 52, 17, regular, MUTED, true, 190);
        rule(c, 36, wm - 36, 88);

        String[] names = {"Vàng SJC (9999)", "Vàng nhẫn (9999)", "Xăng RON 95-III", "Dầu DO 0,05S"};
        String[] units = {"1 lượng", "1 chỉ", "1 lít", "1 lít"};
        String[] price = {"86.500.000đ", "85.200.000đ", "21.000đ", "19.080đ"};
        String[] delta = {"▲ 500.000đ", "▲ 400.000đ", "▼ 200đ", "▼ 180đ"};
        for (int i = 0; i < 4; i++) {
            float cy = 121 + 89 * i;
            icon(c, i, 66, cy);
            put(c, names[i], 126, cy + 7, 20, medium, TEXT, false, wm - 300);
            put(c, units[i], 126, cy + 35, 16, regular, MUTED, false, 160);
            put(c, price[i], wm - 36, cy + 7, 22, bold, TEXT, true, 190);
            put(c, delta[i], wm - 36, cy + 38, 19, medium, delta[i].startsWith("▲") ? GREEN : RED, true, 190);
            if (i < 3) rule(c, 36, wm - 36, cy + 45);
        }
    }

    private void icon(Canvas c, int kind, float cx, float cy) {
        if (kind == 0) {
            shape.setColor(GOLD);
            r.set((cx - 26) * u, (cy + 2) * u, (cx - 2) * u, (cy + 20) * u);
            c.drawRoundRect(r, 3 * u, 3 * u, shape);
            r.set((cx + 1) * u, (cy + 2) * u, (cx + 27) * u, (cy + 20) * u);
            c.drawRoundRect(r, 3 * u, 3 * u, shape);
            shape.setColor(0xFFFFD66B);
            r.set((cx - 13) * u, (cy - 16) * u, (cx + 13) * u, (cy + 2) * u);
            c.drawRoundRect(r, 3 * u, 3 * u, shape);
        } else if (kind == 1) {
            shape.setColor(GOLD);
            r.set((cx - 26) * u, (cy - 12) * u, (cx + 26) * u, (cy + 16) * u);
            c.drawOval(r, shape);
            shape.setColor(0xFF10222B);
            r.set((cx - 12) * u, (cy - 5) * u, (cx + 12) * u, (cy + 9) * u);
            c.drawOval(r, shape);
        } else {
            shape.setColor(0xFFF5B83B);
            r.set((cx - 18) * u, (cy - 24) * u, (cx + 10) * u, (cy + 24) * u);
            c.drawRoundRect(r, 5 * u, 5 * u, shape);
            shape.setColor(0xFF10222B);
            r.set((cx - 12) * u, (cy - 17) * u, (cx + 4) * u, (cy - 4) * u);
            c.drawRoundRect(r, 2 * u, 2 * u, shape);
            shape.setStyle(Paint.Style.STROKE);
            shape.setColor(0xFFF5B83B);
            shape.setStrokeWidth(4 * u);
            path.reset();
            path.moveTo((cx + 10) * u, (cy - 10) * u);
            path.lineTo((cx + 22) * u, (cy - 4) * u);
            path.lineTo((cx + 22) * u, (cy + 14) * u);
            c.drawPath(path, shape);
            shape.setStyle(Paint.Style.FILL);
        }
    }
}
