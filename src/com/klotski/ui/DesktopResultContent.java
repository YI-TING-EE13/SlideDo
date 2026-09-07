package com.klotski.ui;

import com.klotski.core.SaveManager;
import com.klotski.core.PuzzleDifficulty;

/**
 * Formats desktop completion copy to match the Android Results wording.
 */
final class DesktopResultContent {
    private static final String NO_RECORD = "No record yet";

    private DesktopResultContent() {
    }

    static String resultsMessage(int size, int moves, long timeMs, boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest) {
        return resultsMessageInternal(size, PuzzleDifficulty.CLASSIC, moves, timeMs,
                assisted, newBest, previousBest, currentBest, false);
    }

    static String resultsMessage(int size, PuzzleDifficulty difficulty, int moves, long timeMs,
            boolean assisted, boolean newBest, SaveManager.BestRecord previousBest,
            SaveManager.BestRecord currentBest) {
        return resultsMessageInternal(size, difficulty, moves, timeMs,
                assisted, newBest, previousBest, currentBest, true);
    }

    static String resultsMessage(DesktopLocale locale, int size, PuzzleDifficulty difficulty,
            int moves, long timeMs, boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest) {
        DesktopLocale selectedLocale = locale == null ? DesktopLocale.fromTag("en") : locale;
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        String recordText;
        if (assisted) {
            recordText = selectedLocale.format("recordPlayerBest", formatRecord(previousBest));
        } else if (newBest) {
            recordText = previousBest == null
                    ? selectedLocale.text("recordFirst")
                    : selectedLocale.format("recordNewBest", formatRecord(previousBest));
        } else {
            recordText = selectedLocale.format("recordBestRemains", formatRecord(currentBest));
        }
        return String.join("\n", assisted ? selectedLocale.text("assistedResult") : "Puzzle solved.",
                "", size + "x" + size + " Puzzle",
                "Difficulty: " + difficultyLabel(selected, selectedLocale),
                formatMoves(moves, selectedLocale) + "   Time: " + (timeMs / 1000) + "s", "", recordText);
    }

    private static String resultsMessageInternal(int size, PuzzleDifficulty difficulty, int moves,
            long timeMs, boolean assisted, boolean newBest, SaveManager.BestRecord previousBest,
            SaveManager.BestRecord currentBest, boolean includeDifficultyInRecordText) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        String recordText = recordText(assisted, newBest, previousBest, currentBest,
                includeDifficultyInRecordText);
        return String.join("\n",
                assisted ? "Solved with assist." : "Puzzle solved.",
                "",
                size + "x" + size + " Puzzle",
                "Difficulty: " + difficultyLabel(selected),
                formatMoves(moves) + "   Time: " + (timeMs / 1000) + "s",
                "",
                recordText);
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        return switch (selected) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }

    static String formatMoves(int moves) {
        return moves + (moves == 1 ? " move" : " moves");
    }

    static String favoritePracticeMessage(int size, PuzzleDifficulty difficulty,
            int moves, long timeMs) {
        return String.join("\n",
                "Favorite Practice solved.",
                "",
                size + "x" + size + " Puzzle",
                "Difficulty: " + difficultyLabel(difficulty),
                formatMoves(moves) + "   Time: " + (timeMs / 1000) + "s",
                "",
                "Practice result not saved to records, history, statistics, or Daily streaks.");
    }

    static String favoritePracticeMessage(DesktopLocale locale, int size,
            PuzzleDifficulty difficulty, int moves, long timeMs) {
        DesktopLocale selectedLocale = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selectedLocale.text("favoriteSolved"), "",
                size + "x" + size + " Puzzle",
                "Difficulty: " + difficultyLabel(difficulty, selectedLocale),
                formatMoves(moves, selectedLocale) + "   Time: " + (timeMs / 1000) + "s", "",
                selectedLocale.text("favoriteIsolation"));
    }

    private static String formatMoves(int moves, DesktopLocale locale) {
        return locale.format(moves == 1 ? "moveSingular" : "movePlural", moves);
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty, DesktopLocale locale) {
        return switch (difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }

    private static String recordText(boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest,
            boolean includeDifficulty) {
        if (assisted) {
            return "Assist result not saved. Player best: " + formatRecord(previousBest);
        }
        if (newBest) {
            return previousBest == null
                    ? includeDifficulty
                            ? "First player record for this size and difficulty."
                            : "First player record for this size."
                    : "New best. Previous best: " + formatRecord(previousBest);
        }
        return "Best remains: " + formatRecord(currentBest);
    }

    private static String formatRecord(SaveManager.BestRecord record) {
        return record == null ? NO_RECORD : record.format();
    }
}
