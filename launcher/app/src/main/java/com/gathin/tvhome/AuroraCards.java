package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    private static final int GREEN = 0xFF4CE08A;
    private static final int RED = 0xFFFF5A56;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shape = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private Typeface regular = Typeface.DEFAULT;
    private Typeface medium = Typeface.DEFAULT;
    private Typeface bold = Typeface.DEFAULT_BOLD;
    private float u = 1f;
    private final Context ctx;

    AuroraCards(Context ctx) {
        this.ctx = ctx;
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
    void draw(Canvas c, int index, float w, float h, LiveData live) {
        u = h / CARD_H;
        border.setStrokeWidth(Math.max(1f, 1.5f * u));
        r.set(0, 0, w, h);
        c.drawRoundRect(r, 26 * u, 26 * u, fill);
        c.drawRoundRect(r, 26 * u, 26 * u, border);
        line.setStrokeWidth(Math.max(1f, u));
        if (index == 0) weather(c, w, live);
        else if (index == 1) lunar(c, w, live);
        else prices(c, w, live);
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

    private static int weatherIcon(int kind, boolean day) {
        switch (kind) {
            case LiveData.CLEAR: return day ? R.drawable.ic_wx_sun : R.drawable.ic_moon;
            case LiveData.PARTLY: return R.drawable.ic_wx_partly;
            case LiveData.RAIN: return R.drawable.ic_wx_rain;
            case LiveData.STORM: return R.drawable.ic_wx_storm;
            case LiveData.FOG: return R.drawable.ic_wx_fog;
            default: return R.drawable.ic_wx_cloud;
        }
    }

    private static String deg(double v) {
        return Math.round(v) + "°";
    }

    private void weather(Canvas c, float w, LiveData live) {
        float wm = w / u;   // card width in mock units
        LiveData.Weather wx = live == null ? null : live.weather;
        boolean day = wx == null || wx.day;
        if (!day) {
            // The moon behind the clouds, top right; by day there is no art.
            shape.setShader(new RadialGradient(wm * 0.78f * u, 100 * u, 70 * u,
                    new int[] {0x55FFFFFF, 0x14FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(wm * 0.78f * u, 100 * u, 70 * u, shape);
            shape.setShader(null);
            shape.setColor(0xFFE9EEF0);
            c.drawCircle(wm * 0.78f * u, 100 * u, 24 * u, shape);
        }
        vec(c, wx == null ? R.drawable.ic_wx_cloud : weatherIcon(wx.kind, wx.day), 62, 56, 56);
        put(c, "Thời tiết", 107, 48, 26, medium, TEXT, false, 300);
        put(c, wx == null ? "—" : wx.city, 107, 78, 18, regular, MUTED, false, 300);
        put(c, wx == null ? "—" : Math.round(wx.temp) + "°C", 36, 163, 62, bold, TEXT, false, 260);
        put(c, wx == null ? "Chưa có dữ liệu" : wx.text, 38, 205, 23, medium, TEXT, false, wm - 200);
        put(c, wx == null ? "Kiểm tra kết nối mạng" : wx.note, 38, 238, 18, regular, MUTED, false, wm - 160);
        float right = wm - 52;
        put(c, wx == null ? "▲ —" : "▲ " + deg(wx.high), right, 205, 20, medium, TEXT, true, 120);
        put(c, wx == null ? "▼ —" : "▼ " + deg(wx.low), right, 238, 20, medium, TEXT, true, 120);
        rule(c, 38, wm - 52, 263);

        float span = wm - 2 * 68f;
        for (int i = 0; i < 5; i++) {
            float cx = 68 + span * i / 4f;
            boolean has = wx != null && wx.hourLabel[i] != null;
            put(c, has ? wx.hourLabel[i] : "—", cx - 16, 310, 18, regular, MUTED, false, 60);
            if (has) {
                int hour = Integer.parseInt(wx.hourLabel[i].replace("h", ""));
                vec(c, weatherIcon(wx.hourKind[i], hour >= 6 && hour < 18), cx, 350, 46);
                put(c, deg(wx.hourTemp[i]), cx - 18, 406, 21, medium, TEXT, false, 60);
            }
        }
    }

    // ---- lunar ---------------------------------------------------------------------------------

    private void lunar(Canvas c, float w, LiveData live) {
        float wm = w / u;
        LiveData.LunarCard k = live == null ? null : live.lunar;
        vec(c, R.drawable.ic_moon, 54, 47, 38);
        put(c, "Lịch âm", 90, 54, 26, medium, TEXT, false, 240);

        // "Xem chi tiết >" chip.
        r.set((wm - 175) * u, 29 * u, (wm - 36) * u, 71 * u);
        shape.setStyle(Paint.Style.STROKE);
        shape.setColor(0x66B9CBD0);
        shape.setStrokeWidth(Math.max(1f, u));
        c.drawRoundRect(r, 21 * u, 21 * u, shape);
        shape.setStyle(Paint.Style.FILL);
        put(c, "Xem chi tiết  ›", wm - 166, 56, 16, regular, TEXT, false, 125);

        put(c, k == null ? "—" : k.line1, 37, 124, 30, medium, TEXT, false, wm - 120);
        put(c, k == null ? "" : k.line2, 37, 162, 30, medium, TEXT, false, wm - 120);
        rule(c, 37, wm - 36, 192);

        float vx = 206;
        float vw = wm - vx - 36;
        String dash = "—";
        put(c, "Dương lịch", 37, 237, 20, regular, MUTED, false, 160);
        put(c, k == null ? dash : k.solar, vx, 237, 20, regular, TEXT, false, vw);
        put(c, "Can chi", 37, 275, 20, regular, MUTED, false, 160);
        put(c, k == null ? dash : k.canChi, vx, 275, 20, regular, TEXT, false, vw);
        put(c, "Giờ hoàng đạo", 37, 314, 20, regular, MUTED, false, 165);
        put(c, k == null ? dash : k.hours1, vx, 314, 20, regular, TEXT, false, vw);
        put(c, k == null ? "" : k.hours2, vx, 342, 20, regular, TEXT, false, vw);
        put(c, "Sắp tới", 37, 386, 20, regular, MUTED, false, 165);
        put(c, k == null ? dash : k.next1, vx, 386, 20, regular, TEXT, false, vw);
        put(c, k == null ? "" : k.next2, vx, 414, 20, regular, TEXT, false, vw);
    }

    // ---- prices --------------------------------------------------------------------------------

    private static String money(double v, boolean signed) {
        long n = Math.round(Math.abs(v));
        String d = java.text.NumberFormat.getIntegerInstance(new java.util.Locale("vi", "VN")).format(n);
        return d + "đ";
    }

    private void prices(Canvas c, float w, LiveData live) {
        float wm = w / u;
        LiveData.Prices pr = live == null ? null : live.prices;
        vec(c, R.drawable.ic_chart, 56, 47, 38);
        put(c, "Giá cả hôm nay", 97, 54, 26, medium, TEXT, false, 270);
        String when = "Cập nhật —";
        if (pr != null && pr.at > 0) {
            when = "Cập nhật " + android.text.format.DateFormat.format("HH:mm", pr.at);
        }
        put(c, when, wm - 36, 52, 17, regular, MUTED, true, 190);
        rule(c, 36, wm - 36, 88);

        String[] names = {"Vàng SJC (9999)", "Vàng nhẫn (9999)", "Xăng RON 95-III", "Dầu DO 0,05S"};
        String[] units = {"1 lượng", "1 lượng", "1 lít", "1 lít"};
        int[] icons = {R.drawable.ic_gold, R.drawable.ic_ring, R.drawable.ic_fuel, R.drawable.ic_fuel};
        for (int i = 0; i < 4; i++) {
            float cy = 121 + 89 * i;
            LiveData.Row row = pr == null ? null : pr.rows[i];
            vec(c, icons[i], 66, cy, 48);
            put(c, names[i], 126, cy + 7, 20, medium, TEXT, false, wm - 300);
            put(c, units[i], 126, cy + 35, 16, regular, MUTED, false, 160);
            put(c, row == null ? "—" : money(row.value, false), wm - 36, cy + 7, 22, bold, TEXT, true, 190);
            if (row != null && !Double.isNaN(row.delta) && Math.round(row.delta) != 0) {
                boolean up = row.delta > 0;
                put(c, (up ? "▲ " : "▼ ") + money(row.delta, false), wm - 36, cy + 38, 19, medium, up ? GREEN : RED, true, 190);
            }
            if (i < 3) rule(c, 36, wm - 36, cy + 45);
        }
    }

    /** Draws a vector icon centred at cx,cy, size in mock px. */
    private void vec(Canvas c, int res, float cx, float cy, float size) {
        android.graphics.drawable.Drawable d = ctx.getDrawable(res);
        if (d == null) return;
        d.setBounds(Math.round((cx - size / 2) * u), Math.round((cy - size / 2) * u),
                Math.round((cx + size / 2) * u), Math.round((cy + size / 2) * u));
        d.draw(c);
    }
}
