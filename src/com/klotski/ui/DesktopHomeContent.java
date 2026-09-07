package com.klotski.ui;

import com.klotski.core.SaveManager;
import com.klotski.core.PuzzleDifficulty;

/**
 * Shared desktop home-screen copy and small formatting helpers.
 */
final class DesktopHomeContent {
    private DesktopHomeContent() {
    }

    static String recordsSummary(SaveManager.BestRecord record3,
            SaveManager.BestRecord record4,
            SaveManager.BestRecord record5) {
        return recordsSummary(record3, record4, record5, DesktopLocale.fromTag("en"));
    }

    static String recordsSummary(SaveManager.BestRecord record3,
            SaveManager.BestRecord record4,
            SaveManager.BestRecord record5, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selected.text("records"), "",
                selected.format("recordsScopeRecord", 3, 3, formatRecord(record3, selected)),
                selected.format("recordsScopeRecord", 4, 4, formatRecord(record4, selected)),
                selected.format("recordsScopeRecord", 5, 5, formatRecord(record5, selected)), "",
                selected.text("recordsPlayerOnly"), selected.text("recordsAssistedNote"));
    }

    static String recordsSummary(SaveManager.BestRecord[][] records,
            SaveManager.CompletionStats[][] stats) {
        return recordsSummary(records, stats, DesktopLocale.fromTag("en"));
    }

    static String recordsSummary(SaveManager.BestRecord[][] records,
            SaveManager.CompletionStats[][] stats, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        StringBuilder text = new StringBuilder(selected.text("records")).append("\n\n");
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        for (int row = 0; row < 3; row++) {
            int size = row + 3;
            text.append(size).append("x").append(size).append("\n");
            for (int column = 0; column < difficulties.length; column++) {
                SaveManager.BestRecord record = records != null && row < records.length
                        && records[row] != null && column < records[row].length
                        ? records[row][column] : null;
                SaveManager.CompletionStats completionStats = stats != null && row < stats.length
                        && stats[row] != null && column < stats[row].length
                        ? stats[row][column] : null;
                text.append("  ").append(difficultyLabel(difficulties[column], selected)).append(": ")
                        .append(formatRecord(record, selected))
                        .append(" · ").append(selected.format("recordsPlayer", completionStats == null ? 0 : completionStats.playerCompletions))
                        .append(" · ").append(selected.format("recordsAssisted", completionStats == null ? 0 : completionStats.assistedCompletions)).append("\n");
            }
        }
        return text.append("\n").append(selected.text("recordsFootnote")).toString();
    }

    static String preferencesDescription() {
        return preferencesDescription(DesktopLocale.fromTag("en"));
    }

    static String preferencesDescription(DesktopLocale locale) {
        return (locale == null ? DesktopLocale.fromTag("en") : locale)
                .text("preferencesDescription");
    }

    private static String formatRecord(SaveManager.BestRecord record, DesktopLocale locale) {
        return record == null ? locale.text("bestNone")
                : locale.format("recordFormat", record.moves, record.timeMs / 1000);
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty, DesktopLocale locale) {
        return switch (difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }
}
