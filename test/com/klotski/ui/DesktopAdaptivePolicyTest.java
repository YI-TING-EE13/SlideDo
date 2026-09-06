package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;

class DesktopAdaptivePolicyTest {
    @Test
    void minimumWindowPolicyUsesIndependentDimensions() {
        Dimension first = DesktopAdaptivePolicy.minimumWindowSize();
        Dimension second = DesktopAdaptivePolicy.minimumWindowSize();

        assertNotSame(first, second);
        assertEquals(DesktopAdaptivePolicy.MINIMUM_WINDOW_WIDTH, first.width);
        assertEquals(DesktopAdaptivePolicy.MINIMUM_WINDOW_HEIGHT, first.height);
        assertTrue(DesktopAdaptivePolicy.supportsPrimaryWindow(first.width, first.height));
    }

    @Test
    void themesMeetNormalTextAndFocusContrastTargets() {
        for (DesktopTheme theme : DesktopTheme.values()) {
            assertTrue(DesktopAdaptivePolicy.meetsTextContrast(
                    theme.getTileText(), theme.getTile()));
            assertTrue(DesktopAdaptivePolicy.meetsTextContrast(
                    theme.getHomeTitle(), theme.getHomeBackground()));
            assertTrue(DesktopAdaptivePolicy.meetsTextContrast(
                    theme.getHomeSecondary(), theme.getHomeBackground()));
            assertTrue(DesktopAdaptivePolicy.meetsLargeTextContrast(
                    theme.getFocusIndicator(), theme.getBoardSurface()));
        }
    }

    @Test
    void contrastRatioHandlesNullAndKnownBlackWhiteValues() {
        assertEquals(1.0, DesktopAdaptivePolicy.contrastRatio(null, Color.WHITE));
        assertEquals(21.0, DesktopAdaptivePolicy.contrastRatio(Color.BLACK, Color.WHITE), 0.01);
    }
}
