package com.klotski.android;

import android.graphics.Color;

/** Persistent visual palettes available to the Android app. */
enum AndroidVisualTheme {
    MIDNIGHT("midnight",
            Color.rgb(11, 18, 32), Color.rgb(24, 34, 51), Color.rgb(37, 50, 73),
            Color.rgb(23, 54, 45), Color.rgb(47, 138, 69), Color.rgb(251, 191, 36),
            Color.rgb(190, 199, 213), Color.rgb(134, 239, 172), Color.rgb(72, 31, 40)),
    OCEAN("ocean",
            Color.rgb(5, 24, 38), Color.rgb(13, 44, 64), Color.rgb(24, 70, 96),
            Color.rgb(11, 58, 78), Color.rgb(14, 116, 144), Color.rgb(56, 189, 248),
            Color.rgb(199, 220, 232), Color.rgb(103, 232, 249), Color.rgb(83, 35, 52));

    final String id;
    final int background;
    final int panel;
    final int panelLight;
    final int panelHighlight;
    final int primary;
    final int accent;
    final int mutedText;
    final int positiveText;
    final int dangerPanel;

    AndroidVisualTheme(String id, int background, int panel, int panelLight,
            int panelHighlight, int primary, int accent, int mutedText,
            int positiveText, int dangerPanel) {
        this.id = id;
        this.background = background;
        this.panel = panel;
        this.panelLight = panelLight;
        this.panelHighlight = panelHighlight;
        this.primary = primary;
        this.accent = accent;
        this.mutedText = mutedText;
        this.positiveText = positiveText;
        this.dangerPanel = dangerPanel;
    }

    static AndroidVisualTheme fromId(String id) {
        if (id != null) {
            for (AndroidVisualTheme theme : values()) {
                if (theme.id.equals(id)) {
                    return theme;
                }
            }
        }
        return MIDNIGHT;
    }
}
