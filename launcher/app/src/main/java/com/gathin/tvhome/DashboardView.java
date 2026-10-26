package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;

/**
 * Home dashboard: three translucent "glass" cards (weather, lunar calendar, prices) drawn on one canvas.
 *
 * The glass look costs no blur: a translucent tint over the wallpaper, a diagonal sheen and a border that is
 * brighter at the top-left edge. The view is static (it redraws only when its data or size changes), so it
 * adds no per-frame work on a 1 GB TV.
 *
 * Phase B: the numbers below are placeholders. Phase C replaces them with fetched data and the offline lunar
 * calendar.
 */
public final class DashboardView extends View {

    /** Below this height the cards would not fit even the title and one row, so the dashboard hides itself. */
    static final int MIN_HEIGHT_DP = 150;

    private static final int TEXT = 0xFFF5F6F8;
    private static final int MUTED = 0xFFA4AAB4;
    private static final int RED = 0xFFFF5A56;
    private static final int GREEN = 0xFF58D68D;

    private final float d;
    /** Card widths and the gap between them, in any shared unit; null keeps the default 1.5 : 1 : 1 with a 16dp gap. */
    private float[] widths;
    private float gapUnits;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final RectF small = new RectF();
    private final Path clip = new Path();
    private AuroraCards aurora;
    private HanoiCards hanoi;
    private HanoiRowCards hanoiRow;
    private LiveData live;
    private Typeface regular;
    private Typeface light;
    private Typeface medium;

    /** The one LayoutInflater uses: it looks up a public (Context, AttributeSet) constructor by reflection. */
    public DashboardView(Context ctx, android.util.AttributeSet attrs) {
        super(ctx, attrs);
        d = ctx.getResources().getDisplayMetrics().density;
        init(ctx);
    }

    DashboardView(Context ctx) {
        super(ctx);
        d = ctx.getResources().getDisplayMetrics().density;
        init(ctx);
    }

    private void init(Context ctx) {
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        fill.setColor(0xB8180F14);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(1f, 1.5f * d));
        chip.setColor(0x14FFFFFF);
        line.setColor(0x1AFFFFFF);
        line.setStrokeWidth(Math.max(1f, d));
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            light = ctx.getResources().getFont(R.font.outfit_light);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
        } catch (RuntimeException e) {
            regular = light = medium = Typeface.DEFAULT;
        }
    }

    /** Sizes the cards by the design's own widths (DesignFrame.CARD_WIDTHS) so they match the mock-up exactly. */
    void setCards(float[] cardWidths, float gap) {
        widths = cardWidths.clone();
        gapUnits = gap;
        invalidate();
    }

    private final MinuteTicker ticker = new MinuteTicker(this::onTick);

    private void onTick() {
        if (hanoi == null) return;
        if (live != null) live.refreshLunar();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ticker.start(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        ticker.stop(getContext());
        super.onDetachedFromWindow();
    }

    /** The numbers the Aurora cards draw; call again (on the main thread) after a refresh. */
    void setLive(LiveData data) {
        live = data;
        invalidate();
    }

    /** Switches to the Aurora card design (DesignFrame.Profile.glassTiles). */
    void setAurora(boolean on) {
        aurora = on ? new AuroraCards(getContext()) : null;
        hanoi = null;
        hanoiRow = null;
        invalidate();
    }

    /** Switches to the Hà Nội card design (DesignFrame.Profile.sidebar). */
    void setHanoi(boolean on) {
        hanoi = on ? new HanoiCards(getContext()) : null;
        hanoiRow = null;
        aurora = null;
        invalidate();
    }

    /** Switches to the first Hà Nội card design: three tall cards in a row (DesignFrame.HANOI). */
    void setHanoiRow(boolean on) {
        hanoiRow = on ? new HanoiRowCards(getContext()) : null;
        hanoi = null;
        aurora = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h < MIN_HEIGHT_DP * d) return;
        if (hanoi != null) {
            hanoi.drawAll(c, w, h, live);
            return;
        }
        float[] weights = widths != null ? widths : new float[] {1.5f, 1f, 1f};
        float total = 0;
        for (float v : weights) total += v;
        float gap;
        float unit;
        if (widths != null) {
            float whole = total + gapUnits * (weights.length - 1);
            unit = w / whole;
            gap = gapUnits * unit;
        } else {
            gap = 16 * d;
            unit = (w - 2 * gap) / total;
        }
        float x = 0;
        for (int i = 0; i < 3; i++) {
            float cw = unit * weights[i];
            box.set(x, 0, x + cw, h);
            if (aurora != null || hanoiRow != null) {
                c.save();
                c.translate(x, 0);
                if (hanoiRow != null) hanoiRow.draw(c, i, cw, h, live);
                else aurora.draw(c, i, cw, h, live);
                c.restore();
                x += cw + gap;
                continue;
            }
            glass(c, box);
            c.save();
            c.translate(x, 0);
            if (i == 0) weather(c, cw, h);
            else if (i == 1) lunar(c, cw, h);
            else prices(c, cw, h);
            c.restore();
            x += cw + gap;
        }
    }

    // ---- glass ---------------------------------------------------------------------------------

    private void glass(Canvas c, RectF r) {
        float rad = 24 * d;
        c.drawRoundRect(r, rad, rad, fill);
        clip.reset();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        // Vertical and eased out: a slanted two-stop ramp ends in a hard visible edge (dark triangles on a TV).
        sheen.setShader(new LinearGradient(0, r.top, 0, r.top + r.height() * 0.7f,
                new int[] {0x26FFFFFF, 0x0DFFFFFF, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(r, sheen);
        c.restore();
        border.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                0x66FFFFFF, 0x1AFFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, border);
    }

    // ---- text helpers --------------------------------------------------------------------------

    private void style(Typeface tf, float sp, int color) {
        text.setTypeface(tf);
        text.setTextSize(sp * d);
        text.setColor(color);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void put(Canvas c, String s, float x, float baseline, float maxW, boolean right) {
        String t = TextUtils.ellipsize(s, new android.text.TextPaint(text), maxW, TextUtils.TruncateAt.END).toString();
        text.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(t, x, baseline, text);
    }

    private void header(Canvas c, float w, String left, String right) {
        float pad = 24 * d;
        style(medium, 17, TEXT);
        put(c, left, pad, 40 * d, w * 0.6f, false);
        style(regular, 14, MUTED);
        put(c, right, w - pad, 40 * d, w * 0.4f, true);
    }

    private void rule(Canvas c, float w, float y) {
        c.drawLine(24 * d, y, w - 24 * d, y, line);
    }

    // ---- cards ---------------------------------------------------------------------------------

    private void weather(Canvas c, float w, float h) {
        float pad = 24 * d;
        header(c, w, "Hà Nội", "Thời tiết");
        style(light, 64, TEXT);
        put(c, "29°", pad, 112 * d, w, false);
        float tx = pad + 112 * d;
        style(medium, 22, TEXT);
        put(c, "Trời quang", tx, 92 * d, w - tx - pad, false);
        style(regular, 15, MUTED);
        put(c, "Cao 33° · Thấp 25°", tx, 116 * d, w - tx - pad, false);

        float y = 136 * d;
        if (h >= 250 * d) {
            String[][] chips = {{"Mưa", "12%"}, {"Độ ẩm", "73%"}, {"AQI", "91 · TB"}, {"UV", "7 · Cao"}};
            float cg = 8 * d;
            float cw = (w - 2 * pad - 3 * cg) / 4f;
            for (int i = 0; i < 4; i++) {
                float cx = pad + i * (cw + cg);
                small.set(cx, y, cx + cw, y + 56 * d);
                c.drawRoundRect(small, 12 * d, 12 * d, chip);
                style(regular, 12, MUTED);
                put(c, chips[i][0], cx + 10 * d, y + 20 * d, cw - 16 * d, false);
                style(medium, 16, TEXT);
                put(c, chips[i][1], cx + 10 * d, y + 44 * d, cw - 16 * d, false);
            }
            y += 56 * d + 14 * d;
        }
        if (h >= y + 78 * d) {
            rule(c, w, y);
            String[] days = {"T7", "CN", "T2", "T3", "T4"};
            String[] temps = {"33°/26°", "32°/26°", "32°/26°", "32°/24°", "31°/24°"};
            float cw = (w - 2 * pad) / 5f;
            for (int i = 0; i < 5; i++) {
                float cx = pad + i * cw;
                style(regular, 14, MUTED);
                put(c, days[i], cx, y + 26 * d, cw, false);
                style(medium, 15, TEXT);
                put(c, temps[i], cx, y + 54 * d, cw, false);
            }
        }
    }

    private void lunar(Canvas c, float w, float h) {
        float pad = 24 * d;
        header(c, w, "Lịch âm", "");
        style(medium, 72, RED);
        put(c, "14", pad, 118 * d, w * 0.4f, false);
        float tx = pad + 108 * d;
        style(medium, 24, TEXT);
        put(c, "Tháng 9", tx, 92 * d, w - tx - pad, false);
        style(regular, 15, MUTED);
        put(c, "Năm Bính Ngọ", tx, 116 * d, w - tx - pad, false);

        String[][] ev = {{"Rằm tháng 9", "Ngày mai"}, {"Ngày Nhà giáo VN", "còn 28 ngày"}, {"Tết Đinh Mùi", "còn 106 ngày"}};
        float y = 136 * d;
        float row = 56 * d;
        for (int i = 0; i < ev.length && y + row <= h - 8 * d; i++) {
            rule(c, w, y);
            style(medium, 16, TEXT);
            put(c, ev[i][0], pad, y + 34 * d, w * 0.55f, false);
            style(regular, 13, i == 0 ? RED : MUTED);
            put(c, ev[i][1], w - pad, y + 34 * d, w * 0.4f, true);
            y += row;
        }
    }

    private void prices(Canvas c, float w, float h) {
        float pad = 24 * d;
        header(c, w, "Giá cả hôm nay", "09:12");
        float y = 58 * d;
        String[][] rows = {
                {"Vàng miếng SJC", "139,0", "142,0"},
                {"Vàng nhẫn 9999", "138,0", "141,0"},
                {"Xăng E10 RON 95-III", "", "24.910"},
                {"Xăng E5 RON 92", "", "25.060"},
                {"Dầu DO 0,05S", "", "29.250"},
                {"USD/VND (VCB)", "", "26.040"},
        };
        float rowH = 38 * d;
        for (int i = 0; i < rows.length && y + rowH <= h - 8 * d; i++) {
            if (i == 2 || i == 5) {
                rule(c, w, y + 2 * d);
                y += 8 * d;
                if (y + rowH > h - 8 * d) break;
            }
            // Price columns first (right-aligned), then the label gets only what is left of the row.
            style(medium, 17, i >= 2 ? GREEN : TEXT);
            float sellW = text.measureText(rows[i][2]);
            float labelEnd = w - pad - sellW - 12 * d;
            if (!rows[i][1].isEmpty()) {
                style(regular, 16, MUTED);
                float buyRight = w - pad - Math.max(sellW, 56 * d) - 16 * d;
                put(c, rows[i][1], buyRight, y + 26 * d, w * 0.25f, true);
                labelEnd = buyRight - text.measureText(rows[i][1]) - 12 * d;
            }
            style(medium, 17, i >= 2 ? GREEN : TEXT);
            put(c, rows[i][2], w - pad, y + 26 * d, w * 0.3f, true);
            style(regular, 16, i >= 2 ? TEXT : MUTED);
            put(c, rows[i][0], pad, y + 26 * d, Math.max(0f, labelEnd - pad), false);
            y += rowH;
        }
    }
}
