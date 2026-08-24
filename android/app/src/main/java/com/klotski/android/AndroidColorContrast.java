package com.klotski.android;

import android.graphics.Color;

/** WCAG contrast helpers for text and icons rendered on solid UI surfaces. */
final class AndroidColorContrast {
    private AndroidColorContrast() {
    }

    /**
     * Chooses black or white, whichever has greater contrast on the background.
     *
     * @param background opaque background color
     * @return readable black or white foreground color
     */
    static int readableContentColor(int background) {
        double blackRatio = contrastRatio(Color.BLACK, background);
        double whiteRatio = contrastRatio(Color.WHITE, background);
        return blackRatio > whiteRatio ? Color.BLACK : Color.WHITE;
    }

    /**
     * Calculates the WCAG contrast ratio for two opaque colors.
     *
     * @param foreground foreground color
     * @param background background color
     * @return ratio from 1.0 to 21.0
     */
    static double contrastRatio(int foreground, int background) {
        double first = relativeLuminance(foreground);
        double second = relativeLuminance(background);
        double lighter = Math.max(first, second);
        double darker = Math.min(first, second);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int color) {
        return 0.2126 * linearComponent(Color.red(color))
                + 0.7152 * linearComponent(Color.green(color))
                + 0.0722 * linearComponent(Color.blue(color));
    }

    private static double linearComponent(int component) {
        double value = component / 255.0;
        return value <= 0.04045
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
