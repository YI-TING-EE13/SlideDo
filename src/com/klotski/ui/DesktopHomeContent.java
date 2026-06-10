package com.klotski.ui;

import com.klotski.core.SaveManager;

/**
 * Shared desktop home-screen copy and small formatting helpers.
 */
final class DesktopHomeContent {
    private DesktopHomeContent() {
    }

    static String recordsSummary(SaveManager.BestRecord record3,
            SaveManager.BestRecord record4,
            SaveManager.BestRecord record5) {
        return String.join("\n",
                "Records",
                "",
                formatRecord(3, record3),
                formatRecord(4, record4),
                formatRecord(5, record5),
                "",
                "Solver-assisted completions do not replace player best records.");
    }

    static String preferencesDescription() {
        return "Desktop preferences affect only presentation. Puzzle rules and records remain unchanged.";
    }

    private static String formatRecord(int size, SaveManager.BestRecord record) {
        String value = record == null ? "--" : record.format();
        return size + "x" + size + ": " + value;
    }
}
