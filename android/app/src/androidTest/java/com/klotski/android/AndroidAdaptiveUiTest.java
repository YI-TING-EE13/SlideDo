package com.klotski.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Focused policy tests for adaptive layout and color-contrast decisions. */
@RunWith(AndroidJUnit4.class)
public class AndroidAdaptiveUiTest {
    @Test
    public void policyStacksDenseRowsForLargeTextAndBoundsLargeScreenContent() {
        Configuration largeTextPhone = new Configuration();
        largeTextPhone.screenWidthDp = 360;
        largeTextPhone.screenHeightDp = 800;
        largeTextPhone.smallestScreenWidthDp = 360;
        largeTextPhone.fontScale = 1.5f;

        AndroidUiPolicy phonePolicy = new AndroidUiPolicy(largeTextPhone);
        assertTrue(phonePolicy.isLargeText());
        assertTrue(phonePolicy.shouldStackDenseActions());
        assertFalse(phonePolicy.hasBoundedContentWidth());

        Configuration tablet = new Configuration();
        tablet.screenWidthDp = 900;
        tablet.screenHeightDp = 1200;
        tablet.smallestScreenWidthDp = 720;
        tablet.fontScale = 1.0f;

        AndroidUiPolicy tabletPolicy = new AndroidUiPolicy(tablet);
        assertFalse(tabletPolicy.shouldStackDenseActions());
        assertTrue(tabletPolicy.hasBoundedContentWidth());
        assertTrue(tabletPolicy.getContentMaxWidthDp() < tablet.screenWidthDp);
        assertTrue(tabletPolicy.getHorizontalPaddingDp() > phonePolicy.getHorizontalPaddingDp());
    }

    @Test
    public void everyThemeButtonBackgroundGetsReadableContentColor() {
        for (AndroidVisualTheme theme : AndroidVisualTheme.values()) {
            int[] backgrounds = {
                    theme.primary,
                    theme.accent,
                    theme.panel,
                    theme.panelLight,
                    theme.panelHighlight,
                    theme.dangerPanel
            };
            for (int background : backgrounds) {
                int foreground = AndroidColorContrast.readableContentColor(background);
                assertTrue("Insufficient contrast for " + theme.id + " background " + background,
                        AndroidColorContrast.contrastRatio(foreground, background) >= 4.5);
            }
            assertTrue(AndroidColorContrast.contrastRatio(theme.mutedText, theme.background) >= 4.5);
            assertTrue(AndroidColorContrast.contrastRatio(theme.mutedText, theme.panel) >= 4.5);
            assertTrue(AndroidColorContrast.contrastRatio(theme.positiveText, theme.panel) >= 4.5);
            assertTrue(AndroidColorContrast.contrastRatio(theme.accent, theme.panel) >= 4.5);
        }

        assertTrue(AndroidColorContrast.contrastRatio(
                AndroidColorContrast.readableContentColor(Color.YELLOW), Color.YELLOW) >= 4.5);
    }
}
