package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LiveDataTest {

    @Test
    public void weatherCodesMapToIconsAndVietnameseText() {
        assertEquals(LiveData.CLEAR, LiveData.kindOf(0));
        assertEquals(LiveData.PARTLY, LiveData.kindOf(2));
        assertEquals(LiveData.CLOUD, LiveData.kindOf(3));
        assertEquals(LiveData.RAIN, LiveData.kindOf(61));
        assertEquals(LiveData.STORM, LiveData.kindOf(96));
        assertEquals("Mưa rào", LiveData.textOf(81));
    }

    @Test
    public void walkingAYearOfLunarDatesIsFastEnoughForABackgroundThread() {
        long t0 = System.nanoTime();
        int found = 0;
        for (int i = 0; i < 400; i++) {
            int[] l = Lunar.fromSolar(1 + i % 28, 1 + (i / 28) % 12, 2026, 7);
            if (l[0] == 1 && l[1] == 1) found++;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue("took " + ms + " ms", ms < 2000);
        assertTrue(found >= 0);
    }

    @Test
    public void dayStemAndBranchMatchAKnownDay() {
        // 2000-01-01 was a Mậu Ngọ day.
        long jd = Lunar.jdFromDate(1, 1, 2000);
        assertEquals(4, Math.floorMod(jd + 9, 10L));
        assertEquals(6, Math.floorMod(jd + 1, 12L));
    }
}
