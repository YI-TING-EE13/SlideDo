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
        return String.join("\n",
                "Records",
                "",
                formatRecord(3, record3),
                formatRecord(4, record4),
                formatRecord(5, record5),
                "",
                "Player solves only. Fewer moves rank first; ties use faster time.",
                "Solver-assisted completions do not replace player best records.");
    }

    static String recordsSummary(SaveManager.BestRecord[][] records,
            SaveManager.CompletionStats[][] stats) {
        StringBuilder text = new StringBuilder("Records\n\n");
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
                int player = completionStats == null ? 0 : completionStats.playerCompletions;
                int assisted = completionStats == null ? 0 : completionStats.assistedCompletions;
                text.append("  ").append(difficultyLabel(difficulties[column])).append(": ")
                        .append(record == null ? "--" : record.format())
                        .append(" · Player solves: ").append(player)
                        .append(" · Assisted: ").append(assisted).append("\n");
            }
        }
        text.append("\nPlayer records use size and difficulty scopes; fewer moves rank first; ties use faster time.\n")
                .append("Assisted completions remain in history/statistics but never replace player best records.");
        return text.toString();
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
                        .append(record == null ? "--" : record.format())
                        .append(" · ").append(selected.format("recordsPlayer", completionStats == null ? 0 : completionStats.playerCompletions))
                        .append(" · ").append(selected.format("recordsAssisted", completionStats == null ? 0 : completionStats.assistedCompletions)).append("\n");
            }
        }
        return text.append("\n").append(selected.text("recordsFootnote")).toString();
    }

    static String preferencesDescription() {
        return "Desktop preferences affect only presentation. Puzzle rules and records remain unchanged.";
    }

    static String preferencesDescription(DesktopLocale locale) {
        return (locale == null ? DesktopLocale.fromTag("en") : locale)
                .text("preferencesDescription");
    }

    private static String formatRecord(int size, SaveManager.BestRecord record) {
        String value = record == null ? "--" : record.format();
        return size + "x" + size + ": " + value;
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty, DesktopLocale locale) {
        return switch (difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }
}
