package com.klotski.android;

import android.content.res.Configuration;

/**
 * Central adaptive-layout decisions derived from the current Android window.
 * <p>
 * SlideDo uses programmatic Views, so this policy keeps width and font-scale
 * breakpoints out of individual screen builders while preserving their existing
 * navigation and gameplay ownership.
 * </p>
 */
final class AndroidUiPolicy {
    private static final float LARGE_TEXT_SCALE = 1.3f;
    private static final int NARROW_WIDTH_DP = 360;

    private final int screenWidthDp;
    private final int smallestScreenWidthDp;
    private final float fontScale;

    AndroidUiPolicy(Configuration configuration) {
        Configuration current = configuration == null
                ? new Configuration() : configuration;
        screenWidthDp = Math.max(0, current.screenWidthDp);
        smallestScreenWidthDp = Math.max(0, current.smallestScreenWidthDp);
        fontScale = current.fontScale <= 0f ? 1f : current.fontScale;
    }

    boolean isLargeText() {
        return fontScale >= LARGE_TEXT_SCALE;
    }

    boolean shouldStackDenseActions() {
        return isLargeText() || screenWidthDp > 0 && screenWidthDp < NARROW_WIDTH_DP;
    }

    boolean hasBoundedContentWidth() {
        return getContentMaxWidthDp() > 0;
    }

    int getContentMaxWidthDp() {
        int effectiveWidth = Math.max(screenWidthDp, smallestScreenWidthDp);
        if (effectiveWidth >= 1_000) {
            return 840;
        }
        if (effectiveWidth >= 840) {
            return 720;
        }
        if (smallestScreenWidthDp >= 600 || effectiveWidth >= 600) {
            return 560;
        }
        return 0;
    }

    int getHorizontalPaddingDp() {
        return smallestScreenWidthDp >= 600 || screenWidthDp >= 600 ? 28 : 18;
    }

    int getCompactNavigationWidthDp() {
        return isLargeText() ? 88 : 78;
    }
}
