package com.klotski.ui;

import java.awt.Toolkit;

/** Optional, platform-native feedback for desktop moves and completions. */
public final class DesktopSoundFeedback {
    private DesktopSoundFeedback() {
    }

    /**
     * Emits a short move beep only when the preference is enabled.
     *
     * @param enabled whether sound feedback is enabled
     */
    public static void playMove(boolean enabled) {
        if (enabled) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Emits a completion beep only when the preference is enabled.
     *
     * @param enabled whether sound feedback is enabled
     */
    public static void playWin(boolean enabled) {
        if (enabled) {
            Toolkit.getDefaultToolkit().beep();
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
