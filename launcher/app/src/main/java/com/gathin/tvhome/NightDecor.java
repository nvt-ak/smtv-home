package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

/**
 * Everything of the third Hà Nội look (design_profile hanoi3) that is not a card and not an app row: the dark panel
 * behind the list on the right, the clock with the weekday and date, the greeting, the network mark with the city and
 * the small line under it, and the voice-assistant hint at the bottom left. One full-screen view drawn in the reference
 * design's own 2000x1125 units; it redraws once a minute (MinuteTicker) and when the live data changes, and never takes
 * focus.
 */
final class NightDecor extends View {
    private static final String[] WEEKDAYS = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    private static final float PANEL_LEFT = 1562f;

    private final String greeting;
    private final String hint;
    private final String subline;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final MinuteTicker ticker = new MinuteTicker(this::onTick);
    private Typeface regular = Typeface.DEFAULT;
    private Typeface medium = Typeface.DEFAULT;
    private LiveData live;
    private float s = 1f;

    NightDecor(Context ctx, String tagline, String hint, String subline, Set<String> hidden) {
        super(ctx);
        String wish = tagline == null || tagline.trim().isEmpty() ? "Chúc bạn một ngày thật tốt!" : tagline.trim().replace('\n', ' ');
        this.greeting = wish;
        this.hint = hint == null || hidden.contains("footer") ? "" : hint;
        this.subline = subline == null || hidden.contains("footer") ? "" : subline;
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        try {
            regular = ctx.getResources().getFont(R.font.outfit_regular);
            medium = ctx.getResources().getFont(R.font.outfit_medium);
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

    private float put(Canvas c, String t, float x, float base, float size, Typeface tf, int color, Paint.Align align, float maxW) {
        text.setTypeface(tf);
        text.setTextSize(size * s);
        text.setColor(color);
        text.setTextAlign(align);
        String shown = TextUtils.ellipsize(t, text, maxW * s, TextUtils.TruncateAt.END).toString();
        c.drawText(shown, x * s, base * s, text);
        return text.measureText(shown) / s;
    }

    @Override
    protected void onDraw(Canvas c) {
        s = getWidth() / DesignFrame.W;
        if (s <= 0) return;
        panel(c);
        header(c);
        corner(c);
        assistant(c);
    }

    private void panel(Canvas c) {
        // A soft shade ahead of the panel so the cards and the wallpaper meet it without a hard edge.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(1500 * s, 0, PANEL_LEFT * s, 0, 0x00040608, 0x66040608, Shader.TileMode.CLAMP));
        c.drawRect(1500 * s, 0, PANEL_LEFT * s, getHeight(), paint);
        paint.setShader(null);
        paint.setColor(0xF2090A0E);
        c.drawRect(PANEL_LEFT * s, 0, getWidth(), getHeight(), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[] {0x00FFFFFF, 0x66FFFFFF, 0x33FFFFFF, 0x1AFFFFFF}, new float[] {0f, 0.1f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawLine(PANEL_LEFT * s, 0, PANEL_LEFT * s, getHeight(), paint);
        paint.setShader(null);
    }

    private void header(Canvas c) {
        // A soft shadow keeps the clock and date readable where the wallpaper's moon is bright.
        text.setShadowLayer(10f * s, 0f, 2f * s, 0xAA000000);
        drawHeader(c);
        text.clearShadowLayer();
    }

    private void drawHeader(Canvas c) {
        Calendar now = Calendar.getInstance();
        String clock = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        text.setShader(new LinearGradient(0, 70 * s, 0, 170 * s, 0xFFFFF6E3, 0xFFF3CF94, Shader.TileMode.CLAMP));
        put(c, clock, 82, 166, 118, medium, 0xFFFFFFFF, Paint.Align.LEFT, 330);
        text.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setColor(0x66FFFFFF);
        c.drawLine(415 * s, 70 * s, 415 * s, 172 * s, paint);
        put(c, WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1], 458, 114, 38, medium, 0xFFF0CD96, Paint.Align.LEFT, 380);
        put(c, now.get(Calendar.DAY_OF_MONTH) + " tháng " + (now.get(Calendar.MONTH) + 1) + ", " + now.get(Calendar.YEAR),
                458, 159, 36, regular, 0xFFFFFFFF, Paint.Align.LEFT, 400);
        put(c, "Chào " + part(now.get(Calendar.HOUR_OF_DAY)) + ", " + greeting, 88, 221, 26, regular, 0xFFE8EAF0, Paint.Align.LEFT, 760);
    }

    private static String part(int hour) {
        if (hour < 5) return "buổi tối";
        if (hour < 11) return "buổi sáng";
        if (hour < 14) return "buổi trưa";
        if (hour < 18) return "buổi chiều";
        return "buổi tối";
    }

    /** The network mark, the city with a status dot, and the small line under them. */
    private void corner(Canvas c) {
        LiveData.Weather wx = live == null ? null : live.weather;
        String city = wx == null ? "" : wx.city;
        android.graphics.drawable.Drawable wifi = getContext().getDrawable(R.drawable.ic_wifi);
        if (wifi != null) {
            wifi.setBounds(Math.round(1336 * s), Math.round(70 * s), Math.round(1372 * s), Math.round(106 * s));
            wifi.draw(c);
        }
        float w = put(c, city, 1395, 100, 24, regular, 0xFFFFFFFF, Paint.Align.LEFT, 200);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(wx == null ? 0xFF8A93A6 : 0xFFA6E34B);
        c.drawCircle((1395 + w + 22) * s, 91 * s, 8 * s, paint);
        if (!subline.isEmpty()) put(c, subline, 1497, 142, 22, regular, 0xFFD8DCE6, Paint.Align.RIGHT, 420);
    }

    /** The voice-assistant hint: four coloured dots in a ring, and the line beside them. */
    private void assistant(Canvas c) {
        if (hint.isEmpty()) return;
        float cx = 92 * s, cy = 1046 * s;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xCC0B1226);
        c.drawCircle(cx, cy, 29 * s, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, 2f * s));
        paint.setColor(0xFF3E78E6);
        c.drawCircle(cx, cy, 29 * s, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF4285F4);
        c.drawCircle(cx - 10 * s, cy - 4 * s, 9 * s, paint);
        paint.setColor(0xFFEA4335);
        c.drawCircle(cx + 8 * s, cy - 5 * s, 6.5f * s, paint);
        paint.setColor(0xFFFBBC05);
        c.drawCircle(cx + 11 * s, cy + 9 * s, 6 * s, paint);
        paint.setColor(0xFF34A853);
        c.drawCircle(cx - 2 * s, cy + 11 * s, 5.5f * s, paint);
        put(c, hint, 142, 1054, 22, regular, 0xFFD2D8E4, Paint.Align.LEFT, 720);
    }
}
