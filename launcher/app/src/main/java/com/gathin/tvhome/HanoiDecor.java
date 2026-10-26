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
import android.view.View;

import java.util.Calendar;

/**
 * Everything of the Hà Nội look that is not a card and not a button: the sidebar's shade, the timeline behind the
 * app icons, the "Trang chủ" pill, the clock, the date, the lunar date with its Hoàng Đạo tag, the quote and the
 * two texts at the foot of the screen. One full-screen view drawn in the reference design's own 2000x1125 units;
 * it redraws once a minute (MinuteTicker) and when the live data changes, and never takes focus.
 */
final class HanoiDecor extends View {
    private static final String[] WEEKDAYS = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    private static final int RED = 0xFFE5384B;
    private static final int RED_LIGHT = 0xFFFF5A66;
    private static final int MUTED = 0xFFA9B8D0;

    private final DesignFrame frame;
    private final String quote;
    private final String foot;
    private final String footRight;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final MinuteTicker ticker = new MinuteTicker(this::onTick);
    private Typeface regular = Typeface.DEFAULT;
    private Typeface medium = Typeface.DEFAULT;
    private Typeface bold = Typeface.DEFAULT_BOLD;
    private Typeface italic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
    private LiveData live;
    private float s = 1f;

    HanoiDecor(Context ctx, DesignFrame frame, String quote, String foot, String footRight) {
        super(ctx);
        this.frame = frame;
        this.quote = quote == null ? "" : quote;
        this.foot = foot == null ? "" : foot;
        this.footRight = footRight == null ? "" : footRight;
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
            bold = Typeface.create(medium, Typeface.BOLD);
            italic = Typeface.create(regular, Typeface.ITALIC);
        } catch (RuntimeException e) {
            // The system font is fine.
        }
    }

    void setLive(LiveData data) {
        live = data;
        invalidate();
    }

    private void onTick() {
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

    // ---- drawing -------------------------------------------------------------------------------

    private void style(float size, Typeface tf, int color, Paint.Align align) {
        text.setTypeface(tf);
        text.setTextSize(size * s);
        text.setColor(color);
        text.setTextAlign(align);
    }

    private float put(Canvas c, String t, float x, float base, float size, Typeface tf, int color, Paint.Align align, float maxW) {
        style(size, tf, color, align);
        String shown = TextUtils.ellipsize(t, text, maxW * s, TextUtils.TruncateAt.END).toString();
        c.drawText(shown, x * s, base * s, text);
        return text.measureText(shown) / s;
    }

    @Override
    protected void onDraw(Canvas c) {
        s = getWidth() / DesignFrame.W;
        if (s <= 0) return;

        // The shade behind the sidebar, strongest at the edge.
        paint.setShader(new LinearGradient(0, 0, 640 * s, 0, 0xE0040813, 0x00040813, Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, 640 * s, getHeight(), paint);
        paint.setShader(null);

        timeline(c);
        homePill(c);
        header(c);
        quote(c);
        foot(c);
    }

    private void timeline(Canvas c) {
        float x = 107.5f * s;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[] {0x00FFFFFF, 0x99FFFFFF, 0x66FFFFFF, 0x33FFFFFF}, new float[] {0f, 0.06f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawLine(x, 0, x, getHeight(), paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        float firstCentre = frame.p.tileTop + frame.p.tileH / 2f;
        float pitch = frame.p.tileH + frame.p.tileGap;
        dot(c, 62);
        dot(c, 228);
        for (int k = 0; ; k++) {
            float y = firstCentre + pitch * k + pitch / 2f;
            if (y > DesignFrame.H - 20) break;
            dot(c, y);
        }
    }

    private void dot(Canvas c, float y) {
        paint.setColor(0x44FF5A66);
        c.drawCircle(107.5f * s, y * s, 9 * s, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(107.5f * s, y * s, 4.5f * s, paint);
    }

    private void homePill(Canvas c) {
        r.set(45 * s, 72 * s, 565 * s, 217 * s);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(r.left, 0, r.right, 0, 0x55B3182A, 0x331A0A18, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 36 * s, 36 * s, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x33FF5A66);
        paint.setStrokeWidth(8 * s);
        c.drawRoundRect(r, 36 * s, 36 * s, paint);
        paint.setColor(0xCCFF5A66);
        paint.setStrokeWidth(Math.max(1f, 2.5f * s));
        c.drawRoundRect(r, 36 * s, 36 * s, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55FF5A66);
        c.drawCircle(108 * s, 145 * s, 70 * s, paint);
        paint.setColor(0xFF2A0E18);
        c.drawCircle(108 * s, 145 * s, 64 * s, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 4f * s));
        paint.setColor(RED_LIGHT);
        c.drawCircle(108 * s, 145 * s, 64 * s, paint);
        android.graphics.drawable.Drawable home = getContext().getDrawable(R.drawable.ic_home);
        if (home != null) {
            home.setBounds(Math.round(76 * s), Math.round(113 * s), Math.round(140 * s), Math.round(177 * s));
            home.draw(c);
        }
        put(c, "Trang chủ", 213, 142, 40, medium, 0xFFFFFFFF, Paint.Align.LEFT, 340);
        put(c, "Khám phá thế giới giải trí của bạn", 213, 183, 22, regular, 0xFFC4CCDB, Paint.Align.LEFT, 340);
    }

    private void header(Canvas c) {
        Calendar now = Calendar.getInstance();
        String clock = String.format(java.util.Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        put(c, clock, 635, 155, 98, bold, 0xFFEAF0FF, Paint.Align.LEFT, 380);
        String date = WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + ", " + now.get(Calendar.DAY_OF_MONTH)
                + " Tháng " + (now.get(Calendar.MONTH) + 1) + ", " + now.get(Calendar.YEAR);
        put(c, date, 640, 198, 29, regular, 0xFFE8EEFA, Paint.Align.LEFT, 380);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setColor(0x55FFFFFF);
        c.drawLine(1035 * s, 93 * s, 1035 * s, 197 * s, paint);

        LiveData.LunarCard k = live == null ? null : live.lunar;
        if (k == null) return;
        put(c, k.line1, 1079, 113, 28, medium, 0xFFFFFFFF, Paint.Align.LEFT, 330);
        put(c, "Năm " + k.yearName, 1079, 148, 28, regular, 0xFFE8EEFA, Paint.Align.LEFT, 330);
        r.set(1079 * s, 166 * s, 1290 * s, 209 * s);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(k.auspicious ? 0x33E5384B : 0x22FFFFFF);
        c.drawRoundRect(r, 21.5f * s, 21.5f * s, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setColor(k.auspicious ? 0xCCFF5A66 : 0x66FFFFFF);
        c.drawRoundRect(r, 21.5f * s, 21.5f * s, paint);
        put(c, k.auspicious ? "Ngày Hoàng Đạo" : "Ngày Hắc Đạo", 1184.5f, 195, 22, regular, 0xFFFFFFFF, Paint.Align.CENTER, 190);
    }

    private void quote(Canvas c) {
        if (quote.isEmpty()) return;
        String[] lines = quote.split("\n");
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);
        c.drawRect(644 * s, 420 * s, 647.5f * s, (420 + 36 * lines.length + 8) * s, paint);
        for (int i = 0; i < lines.length; i++) {
            put(c, lines[i], 680, 442 + 36 * i, 28, italic, 0xFFD7DEEC, Paint.Align.LEFT, 470);
        }
    }

    private void foot(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        if (!foot.isEmpty()) {
            float w = put(c, foot, 488, 1068, 17.5f, regular, 0xFF9EAAC2, Paint.Align.LEFT, 520);
            paint.setColor(0x88B8C4DA);
            c.drawLine((488 + w + 22) * s, 1062 * s, (488 + w + 80) * s, 1062 * s, paint);
        }
        if (!footRight.isEmpty()) {
            float w = put(c, footRight, 1850, 1068, 17.5f, regular, 0xFF9EAAC2, Paint.Align.RIGHT, 520);
            paint.setColor(0xFFE5384B);
            c.drawLine(1867 * s, 1062 * s, 1924 * s, 1062 * s, paint);
        }
    }
}
