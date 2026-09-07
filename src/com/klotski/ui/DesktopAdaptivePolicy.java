package com.klotski.ui;

import java.awt.Color;
import java.awt.Dimension;

/**
 * Pure desktop presentation policies used by the Swing UI and its tests.
 *
 * <p>The policy deliberately contains no Swing components.  It records the
 * minimum window target, focus-target sizing, and WCAG-style contrast checks
 * that the desktop UI can enforce without changing puzzle rules.</p>
 */
public final class DesktopAdaptivePolicy {
    /** Minimum usable desktop window target before a user resizes the frame. */
    public static final int MINIMUM_WINDOW_WIDTH = 460;

    /** Minimum usable desktop window target before a user resizes the frame. */
    public static final int MINIMUM_WINDOW_HEIGHT = 560;

    /** Minimum keyboard target dimension for an actionable control. */
    public static final int MINIMUM_FOCUS_TARGET = 44;

    private DesktopAdaptivePolicy() {
    }

    /**
     * Creates a fresh minimum-window dimension so callers cannot mutate a
     * shared policy object.
     *
     * @return minimum desktop window dimension
     */
    public static Dimension minimumWindowSize() {
        return new Dimension(MINIMUM_WINDOW_WIDTH, MINIMUM_WINDOW_HEIGHT);
    }

    /**
     * Checks whether a content area can display the primary desktop controls
     * without relying on horizontal clipping.
     *
     * @param width available width in pixels
     * @param height available height in pixels
     * @return {@code true} when the minimum desktop target is available
     */
    public static boolean supportsPrimaryWindow(int width, int height) {
        return width >= MINIMUM_WINDOW_WIDTH && height >= MINIMUM_WINDOW_HEIGHT;
    }

    /**
     * Computes the relative luminance contrast ratio for two sRGB colors.
     *
     * @param foreground foreground color
     * @param background background color
     * @return contrast ratio, or {@code 1.0} when either color is null
     */
    public static double contrastRatio(Color foreground, Color background) {
        if (foreground == null || background == null) {
            return 1.0;
        }
        double first = relativeLuminance(foreground);
        double second = relativeLuminance(background);
        double lighter = Math.max(first, second);
        double darker = Math.min(first, second);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Checks the normal-text WCAG contrast threshold used by desktop labels.
     *
     * @param foreground foreground color
     * @param background background color
     * @return whether the pair reaches a 4.5:1 ratio
     */
    public static boolean meetsTextContrast(Color foreground, Color background) {
        return contrastRatio(foreground, background) >= 4.5;
    }

    /**
     * Checks the large-text or focus-indicator WCAG contrast threshold.
     *
     * @param foreground foreground color
     * @param background background color
     * @return whether the pair reaches a 3:1 ratio
     */
    public static boolean meetsLargeTextContrast(Color foreground, Color background) {
        return contrastRatio(foreground, background) >= 3.0;
    }

    private static double relativeLuminance(Color color) {
        double red = linearize(color.getRed() / 255.0);
        double green = linearize(color.getGreen() / 255.0);
        double blue = linearize(color.getBlue() / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
