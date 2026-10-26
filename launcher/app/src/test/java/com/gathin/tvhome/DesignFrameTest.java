package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DesignFrameTest {

    /** The looks with a row of tiles under the cards. */
    private static final DesignFrame.Profile[] PROFILES = {DesignFrame.CRIMSON, DesignFrame.AURORA};
    private static final DesignFrame.Profile[] ALL = {DesignFrame.CRIMSON, DesignFrame.AURORA, DesignFrame.HANOI};

    @Test
    public void scalesFromTheScreenWidth() {
        assertEquals(0.96f, new DesignFrame(1920).scale(), 1e-6f);
        assertEquals(0.64f, new DesignFrame(1280).scale(), 1e-6f);
        assertEquals(1.92f, new DesignFrame(3840).scale(), 1e-6f);
    }

    @Test
    public void profileNamesResolve() {
        assertSame(DesignFrame.AURORA, DesignFrame.profile("aurora"));
        assertSame(DesignFrame.AURORA, DesignFrame.profile(" Aurora "));
        assertSame(DesignFrame.HANOI, DesignFrame.profile("HaNoi".toLowerCase()));
        assertSame(DesignFrame.CRIMSON, DesignFrame.profile(""));
        assertSame(DesignFrame.CRIMSON, DesignFrame.profile(null));
        assertSame(DesignFrame.CRIMSON, DesignFrame.profile("anything else"));
    }

    @Test
    public void cardsFillTheirBoxExactly() {
        for (DesignFrame.Profile p : ALL) {
            float sum = p.cardGap * (p.cardWidths.length - 1);
            for (float w : p.cardWidths) sum += w;
            assertEquals(p.name, p.cardsRight - p.cardsLeft, sum, 1e-3f);
        }
    }

    @Test
    public void tilesStopAtTheContentEdge() {
        for (DesignFrame.Profile p : PROFILES) {
            float last = p.tilesLeft + DesignFrame.TILES * p.tileW + (DesignFrame.TILES - 1) * p.tileGap;
            assertEquals(p.name, p.contentRight, last, 1.5f);
        }
    }

    @Test
    public void tileRowSitsBelowTheCardsAndTheTitle() {
        for (DesignFrame.Profile p : PROFILES) {
            if (p.titleTop > 0f) {
                assertTrue(p.name, p.titleTop > p.cardsBottom);
                assertTrue(p.name, p.tileTop > p.titleTop);
            } else {
                assertTrue(p.name, p.tileTop > p.cardsBottom);
            }
            assertTrue(p.name, p.tileTop + p.tileH < DesignFrame.H);
            if (p.dotsY > 0f) assertTrue(p.name, p.dotsY > p.tileTop + p.tileH);
        }
    }

    @Test
    public void gridCellsPutTheFirstTileAtItsLeftEdge() {
        for (DesignFrame.Profile p : PROFILES) {
            DesignFrame f = new DesignFrame(1920, p);
            // The tile is centred in its cell, so it starts (cell - tile) / 2 after the grid padding.
            int first = f.gridSidePadding() + (f.cellWidth() - f.tileWidth()) / 2;
            assertEquals(p.name, f.tileLeft(0), first, 1);
            // The last tile ends where the sixth cell ends, less the same half gap.
            int lastRight = f.gridSidePadding() + f.cellWidth() * DesignFrame.TILES - (f.cellWidth() - f.tileWidth()) / 2;
            assertEquals(p.name, f.tileLeft(5) + f.tileWidth(), lastRight, 2);
        }
    }

    @Test
    public void sidebarListFitsTheScreenAndSitsLeftOfTheCards() {
        DesignFrame.Profile p = DesignFrame.HANOI;
        assertTrue(p.sidebar);
        assertTrue(p.tilesLeft + p.tileW < 640f);
        assertTrue(p.cardsLeft > 640f);
        assertTrue(p.cardsBottom < DesignFrame.H);
        // Six rows fit the screen height.
        assertTrue(p.tileTop + 6 * (p.tileH + p.tileGap) - p.tileGap <= DesignFrame.H);
    }

    @Test
    public void sameFractionsAtEveryResolution() {
        for (DesignFrame.Profile p : ALL) {
            DesignFrame hd = new DesignFrame(1280, p);
            DesignFrame fhd = new DesignFrame(1920, p);
            assertEquals(p.name, (float) hd.cardsTop() / hd.px(DesignFrame.H), (float) fhd.cardsTop() / fhd.px(DesignFrame.H), 0.002f);
            assertEquals(p.name, (float) hd.tileTop() / hd.px(DesignFrame.H), (float) fhd.tileTop() / fhd.px(DesignFrame.H), 0.002f);
        }
    }
}
