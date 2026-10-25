package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DesignFrameTest {

    @Test
    public void scalesFromTheScreenWidth() {
        assertEquals(0.96f, new DesignFrame(1920).scale(), 1e-6f);
        assertEquals(0.64f, new DesignFrame(1280).scale(), 1e-6f);
        assertEquals(1.92f, new DesignFrame(3840).scale(), 1e-6f);
    }

    @Test
    public void cardsFillTheirBoxExactly() {
        float sum = DesignFrame.CARD_GAP * (DesignFrame.CARD_WIDTHS.length - 1);
        for (float w : DesignFrame.CARD_WIDTHS) sum += w;
        assertEquals(DesignFrame.CARDS_RIGHT - DesignFrame.CARDS_LEFT, sum, 1e-3f);
    }

    @Test
    public void tilesStopAtTheContentEdge() {
        float last = DesignFrame.TILES_LEFT + DesignFrame.TILES * DesignFrame.TILE_W
                + (DesignFrame.TILES - 1) * DesignFrame.TILE_GAP;
        assertEquals(DesignFrame.CONTENT_RIGHT, last, 1.5f);
    }

    @Test
    public void tileRowSitsBelowTheCardsAndTheTitle() {
        assertTrue(DesignFrame.TITLE_TOP > DesignFrame.CARDS_BOTTOM);
        assertTrue(DesignFrame.TILE_TOP > DesignFrame.TITLE_TOP);
        assertTrue(DesignFrame.TILE_TOP + DesignFrame.TILE_H < DesignFrame.H);
    }

    @Test
    public void gridCellsPutTheFirstTileAtItsLeftEdge() {
        DesignFrame f = new DesignFrame(1920);
        // The tile is centred in its cell, so it starts (cell - tile) / 2 after the grid padding.
        int first = f.gridSidePadding() + (f.cellWidth() - f.tileWidth()) / 2;
        assertEquals(f.tileLeft(0), first, 1);
        // The last tile ends where the sixth cell ends, less the same half gap.
        int lastRight = f.gridSidePadding() + f.cellWidth() * DesignFrame.TILES - (f.cellWidth() - f.tileWidth()) / 2;
        assertEquals(f.tileLeft(5) + f.tileWidth(), lastRight, 2);
    }

    @Test
    public void sameFractionsAtEveryResolution() {
        DesignFrame hd = new DesignFrame(1280);
        DesignFrame fhd = new DesignFrame(1920);
        assertEquals((float) hd.cardsTop() / hd.px(DesignFrame.H), (float) fhd.cardsTop() / fhd.px(DesignFrame.H), 0.002f);
        assertEquals((float) hd.tileTop() / hd.px(DesignFrame.H), (float) fhd.tileTop() / fhd.px(DesignFrame.H), 0.002f);
    }
}
