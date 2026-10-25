package com.gathin.tvhome;

/**
 * Reference geometry for the dashboard home screen, taken from the design mock-up.
 *
 * Every number is in the mock-up's own space, 2000 x 1125 (16:9). {@link #scale} converts to pixels from the
 * screen width, so the same frame fits 1280x720, 1920x1080 and 3840x2160 alike. Nothing here touches a View;
 * MainActivity reads it when config design_frame is on.
 */
final class DesignFrame {

    static final float W = 2000f;
    static final float H = 1125f;

    /** Left edge of the clock text and the right edge of the settings button. */
    static final float CONTENT_LEFT = 98f;
    static final float CONTENT_RIGHT = 1915f;
    /** Top of the header block (the clock row). */
    static final float HEADER_TOP = 60f;
    /** Height of the WiFi pill and the settings button. */
    static final float PILL_H = 72f;

    /** The three cards share one box; the widths are weather, lunar calendar, prices. */
    static final float CARDS_LEFT = 70f;
    static final float CARDS_TOP = 228f;
    static final float CARDS_RIGHT = 1935f;
    static final float CARDS_BOTTOM = 760f;
    static final float[] CARD_WIDTHS = {771f, 522f, 528f};
    static final float CARD_GAP = 22f;

    /** Top of the dock title ("SmartTube - Press OK to open"). */
    static final float TITLE_TOP = 788f;

    /** Six equal tiles in one row, with the labels under them. */
    static final int TILES = 6;
    static final float TILES_LEFT = 78f;
    static final float TILE_TOP = 865f;
    static final float TILE_W = 288f;
    static final float TILE_H = 140f;
    static final float TILE_GAP = 22f;

    private final float scale;

    DesignFrame(int screenWidthPx) {
        this.scale = screenWidthPx / W;
    }

    float scale() {
        return scale;
    }

    int px(float v) {
        return Math.round(v * scale);
    }

    int cardsLeft() { return px(CARDS_LEFT); }
    int cardsTop() { return px(CARDS_TOP); }
    int cardsWidth() { return px(CARDS_RIGHT) - px(CARDS_LEFT); }
    int cardsHeight() { return px(CARDS_BOTTOM) - px(CARDS_TOP); }

    int tileWidth() { return px(TILE_W); }
    int tileHeight() { return px(TILE_H); }
    int tileTop() { return px(TILE_TOP); }
    int titleTop() { return px(TITLE_TOP); }

    /** One grid cell: a tile plus the gap after it. The row of {@link #TILES} cells spans the tiles exactly. */
    int cellWidth() { return px(TILE_W + TILE_GAP); }

    /**
     * Padding each side of the grid so that {@link #TILES} cells put the first tile at TILES_LEFT: the tile is
     * centred in its cell, so the padding is the left edge minus half the gap.
     */
    int gridSidePadding() { return px(TILES_LEFT - TILE_GAP / 2f); }

    /** Left edge of tile i, for tests and for placing things relative to the row. */
    int tileLeft(int i) { return px(TILES_LEFT + i * (TILE_W + TILE_GAP)); }
}
