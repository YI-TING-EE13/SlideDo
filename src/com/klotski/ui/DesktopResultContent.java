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
        return resultsMessage(size, PuzzleDifficulty.CLASSIC, moves, timeMs,
                assisted, newBest, previousBest, currentBest);
    }

    static String resultsMessage(int size, PuzzleDifficulty difficulty, int moves, long timeMs,
            boolean assisted, boolean newBest, SaveManager.BestRecord previousBest,
            SaveManager.BestRecord currentBest) {
        String subtitle = assisted ? "Solved with assist." : "Puzzle solved.";
        return String.join("\n",
                subtitle,
                "",
                size + "x" + size + " Puzzle",
                "Difficulty: " + difficultyLabel(difficulty),
                formatMoves(moves) + "   Time: " + (timeMs / 1000) + "s",
                "",
                recordText(assisted, newBest, previousBest, currentBest));
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

    private static String recordText(boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest) {
        if (assisted) {
            return "Assist result not saved. Player best: " + formatRecord(previousBest);
        }
        if (newBest) {
            return previousBest == null
                    ? "First player record for this size."
                    : "New best. Previous best: " + formatRecord(previousBest);
        }
        return "Best remains: " + formatRecord(currentBest);
    }

    private static String formatRecord(SaveManager.BestRecord record) {
        return record == null ? NO_RECORD : record.format();
    }
}
