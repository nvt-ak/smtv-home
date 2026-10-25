package com.gathin.tvhome;

/**
 * Reference geometry for the dashboard home screen, taken from a design mock-up.
 *
 * Every number is in the mock-up's own space, 2000 x 1125 (16:9). {@link #scale} converts to pixels from the
 * screen width, so the same frame fits 1280x720, 1920x1080 and 3840x2160 alike. Nothing here touches a View;
 * MainActivity reads it when config design_frame is on. Each look has its own {@link Profile}
 * (config design_profile): "crimson" is the default, "aurora" the Aurora Midnight mock-up.
 */
final class DesignFrame {

    static final float W = 2000f;
    static final float H = 1125f;

    /** Six equal tiles in one row. */
    static final int TILES = 6;

    /** One look's measurements, all in mock-up units. */
    static final class Profile {
        final String name;
        /** Left edge of the clock text and the right edge of the settings button. */
        final float contentLeft, contentRight;
        /** Top of the header block (the clock row). */
        final float headerTop;
        /** The three cards share one box; the widths are weather, lunar calendar, prices. */
        final float cardsLeft, cardsTop, cardsRight, cardsBottom, cardGap;
        final float[] cardWidths;
        /** Top of the "<app> - Press OK" line, or 0 when the look has none (the tiles carry their own names). */
        final float titleTop;
        final float tilesLeft, tileTop, tileW, tileH, tileGap;
        /** Vertical centre of the page dots under the tiles, or 0 for none. */
        final float dotsY;
        /** The tile shows its icon and name inside, in a dark glass card, instead of the app's banner. */
        final boolean glassTiles;

        Profile(String name, float contentLeft, float contentRight, float headerTop, float cardsLeft, float cardsTop,
                float cardsRight, float cardsBottom, float[] cardWidths, float cardGap, float titleTop,
                float tilesLeft, float tileTop, float tileW, float tileH, float tileGap, float dotsY,
                boolean glassTiles) {
            this.name = name;
            this.contentLeft = contentLeft;
            this.contentRight = contentRight;
            this.headerTop = headerTop;
            this.cardsLeft = cardsLeft;
            this.cardsTop = cardsTop;
            this.cardsRight = cardsRight;
            this.cardsBottom = cardsBottom;
            this.cardWidths = cardWidths;
            this.cardGap = cardGap;
            this.titleTop = titleTop;
            this.tilesLeft = tilesLeft;
            this.tileTop = tileTop;
            this.tileW = tileW;
            this.tileH = tileH;
            this.tileGap = tileGap;
            this.dotsY = dotsY;
            this.glassTiles = glassTiles;
        }
    }

    /** The crimson mock-up (the default). */
    static final Profile CRIMSON = new Profile("crimson", 98f, 1915f, 60f,
            70f, 228f, 1935f, 760f, new float[] {771f, 522f, 528f}, 22f, 788f,
            78f, 865f, 288f, 140f, 22f, 0f, false);

    /** The Aurora Midnight mock-up: narrower cards, taller glass tiles, no title line, page dots. */
    static final Profile AURORA = new Profile("aurora", 82f, 1923f, 80f,
            80f, 298f, 1772f, 740f, new float[] {552f, 568f, 529f}, 21.5f, 0f,
            77f, 800f, 282f, 168f, 31f, 1034f, true);

    static Profile profile(String name) {
        return "aurora".equalsIgnoreCase(name == null ? "" : name.trim()) ? AURORA : CRIMSON;
    }

    private final float scale;
    final Profile p;

    DesignFrame(int screenWidthPx) {
        this(screenWidthPx, CRIMSON);
    }

    DesignFrame(int screenWidthPx, Profile profile) {
        this.scale = screenWidthPx / W;
        this.p = profile;
    }

    float scale() {
        return scale;
    }

    int px(float v) {
        return Math.round(v * scale);
    }

    int contentLeft() { return px(p.contentLeft); }
    int contentRightInset() { return px(W - p.contentRight); }
    int headerTop() { return px(p.headerTop); }

    float[] cardWidths() { return p.cardWidths; }
    float cardGap() { return p.cardGap; }
    int cardsLeft() { return px(p.cardsLeft); }
    int cardsTop() { return px(p.cardsTop); }
    int cardsWidth() { return px(p.cardsRight) - px(p.cardsLeft); }
    int cardsHeight() { return px(p.cardsBottom) - px(p.cardsTop); }

    int tileWidth() { return px(p.tileW); }
    int tileHeight() { return px(p.tileH); }
    int tileTop() { return px(p.tileTop); }
    boolean hasTitle() { return p.titleTop > 0f; }
    int titleTop() { return px(p.titleTop); }
    int dotsY() { return px(p.dotsY); }
    boolean glassTiles() { return p.glassTiles; }

    /** One grid cell: a tile plus the gap after it. The row of {@link #TILES} cells spans the tiles exactly. */
    int cellWidth() { return px(p.tileW + p.tileGap); }

    /**
     * Padding each side of the grid so that {@link #TILES} cells put the first tile at its left edge: the tile is
     * centred in its cell, so the padding is the left edge minus half the gap.
     */
    int gridSidePadding() { return px(p.tilesLeft - p.tileGap / 2f); }

    /** Left edge of tile i, for tests and for placing things relative to the row. */
    int tileLeft(int i) { return px(p.tilesLeft + i * (p.tileW + p.tileGap)); }
}
