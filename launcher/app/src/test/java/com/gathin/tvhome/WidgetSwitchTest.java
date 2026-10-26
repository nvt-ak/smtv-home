package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class WidgetSwitchTest {
    @Test
    public void pricesCardShowsUnlessEverythingInItIsOff() {
        assertFalse(DashboardView.pricesHidden(Collections.emptySet()));
        assertFalse(DashboardView.pricesHidden(new HashSet<>(Arrays.asList("gold"))));
        assertFalse(DashboardView.pricesHidden(new HashSet<>(Arrays.asList("fuel"))));
        assertTrue(DashboardView.pricesHidden(new HashSet<>(Arrays.asList("prices"))));
        assertTrue(DashboardView.pricesHidden(new HashSet<>(Arrays.asList("gold", "fuel"))));
    }
}
