package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DesktopThemeTest {
    @Test
    void themeIdsRoundTripAndUnknownValuesStaySafe() {
        assertEquals("ocean", DesktopTheme.fromId("OCEAN").getId());
        assertEquals(DesktopTheme.MIDNIGHT, DesktopTheme.fromId("future-theme"));
        assertNotNull(DesktopTheme.OCEAN.getTile());
        assertNotNull(DesktopTheme.MIDNIGHT.getHomeBackground());
    }
}
