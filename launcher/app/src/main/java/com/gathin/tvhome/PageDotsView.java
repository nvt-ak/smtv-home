package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Page dots under the tile row (design_profile aurora): the active page is a wider cyan pill. Static, no animation. */
final class PageDotsView extends View {
    private static final int ACTIVE = 0xFF74E5F4;
    private static final int IDLE = 0x55B9CBD0;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private int count = 1;
    private int active;

    PageDotsView(Context ctx) {
        super(ctx);
        d = ctx.getResources().getDisplayMetrics().density;
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void set(int count, int active) {
        int c = Math.max(1, Math.min(count, 12));
        int a = Math.max(0, Math.min(active, c - 1));
        if (c == this.count && a == this.active) return;
        this.count = c;
        this.active = a;
        invalidate();
    }

    /** Width the dots need for this count, so the host can size the view. */
    static int widthFor(float density, int count) {
        int c = Math.max(1, Math.min(count, 12));
        return Math.round(((c - 1) * 18 + 28) * density);
    }

    @Override
    protected void onDraw(Canvas c) {
        float dot = 8 * d;
        float wide = 28 * d;
        float gap = 10 * d;
        float total = (count - 1) * (dot + gap) + wide;
        float x = (getWidth() - total) / 2f;
        float cy = getHeight() / 2f;
        for (int i = 0; i < count; i++) {
            boolean on = i == active;
            float w = on ? wide : dot;
            paint.setColor(on ? ACTIVE : IDLE);
            c.drawRoundRect(x, cy - dot / 2f, x + w, cy + dot / 2f, dot / 2f, dot / 2f, paint);
            x += w + gap;
        }
    }
}
