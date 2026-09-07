package com.klotski.ui;

import com.klotski.core.SaveManager;
import com.klotski.core.PuzzleDifficulty;

/**
 * Formats desktop completion copy to match the Android Results wording.
 */
final class DesktopResultContent {
    private DesktopResultContent() {
    }

    static String resultsMessage(int size, int moves, long timeMs, boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest) {
        return resultsMessage(DesktopLocale.fromTag("en"), size, PuzzleDifficulty.CLASSIC, moves,
                timeMs, assisted, newBest, previousBest, currentBest, false);
    }

    static String resultsMessage(int size, PuzzleDifficulty difficulty, int moves, long timeMs,
            boolean assisted, boolean newBest, SaveManager.BestRecord previousBest,
            SaveManager.BestRecord currentBest) {
        return resultsMessage(DesktopLocale.fromTag("en"), size, difficulty, moves, timeMs,
                assisted, newBest, previousBest, currentBest, true);
    }

    static String resultsMessage(DesktopLocale locale, int size, PuzzleDifficulty difficulty,
            int moves, long timeMs, boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest) {
        return resultsMessage(locale, size, difficulty, moves, timeMs, assisted, newBest,
                previousBest, currentBest, true);
    }

    private static String resultsMessage(DesktopLocale locale, int size, PuzzleDifficulty difficulty,
            int moves, long timeMs, boolean assisted, boolean newBest,
            SaveManager.BestRecord previousBest, SaveManager.BestRecord currentBest,
            boolean includeDifficulty) {
        DesktopLocale selectedLocale = locale == null ? DesktopLocale.fromTag("en") : locale;
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        String recordText;
        if (assisted) {
            recordText = selectedLocale.format("recordPlayerBest", formatRecord(previousBest, selectedLocale));
        } else if (newBest) {
            recordText = previousBest == null
                    ? selectedLocale.text(includeDifficulty ? "recordFirst" : "recordFirstSize")
                    : selectedLocale.format("recordNewBest", formatRecord(previousBest, selectedLocale));
        } else {
            recordText = selectedLocale.format("recordBestRemains", formatRecord(currentBest, selectedLocale));
        }
        return String.join("\n", assisted ? selectedLocale.text("assistedResult")
                        : selectedLocale.text("puzzleSolved"),
                "", selectedLocale.format("puzzleTitle", size, size),
                selectedLocale.format("difficultyLabel", difficultyLabel(selected, selectedLocale)),
                selectedLocale.format("timeLabel", formatMoves(moves, selectedLocale), timeMs / 1000),
                "", recordText);
    }

    static String formatMoves(int moves) {
        return formatMoves(moves, DesktopLocale.fromTag("en"));
    }

    static String favoritePracticeMessage(int size, PuzzleDifficulty difficulty,
            int moves, long timeMs) {
        return favoritePracticeMessage(DesktopLocale.fromTag("en"), size, difficulty, moves, timeMs);
    }

    static String favoritePracticeMessage(DesktopLocale locale, int size,
            PuzzleDifficulty difficulty, int moves, long timeMs) {
        DesktopLocale selectedLocale = locale == null ? DesktopLocale.fromTag("en") : locale;
        return String.join("\n", selectedLocale.text("favoriteSolved"), "",
                selectedLocale.format("favoritePuzzleTitle", size, size),
                selectedLocale.format("difficultyLabel", difficultyLabel(difficulty, selectedLocale)),
                selectedLocale.format("timeLabel", formatMoves(moves, selectedLocale), timeMs / 1000), "",
                selectedLocale.text("favoriteIsolation"));
    }

    static String formatMoves(int moves, DesktopLocale locale) {
        return locale.format(moves == 1 ? "moveSingular" : "movePlural", moves);
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty, DesktopLocale locale) {
        return switch (difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }

    private static String formatRecord(SaveManager.BestRecord record, DesktopLocale locale) {
        return record == null ? locale.text("noRecord")
                : locale.format("recordFormat", record.moves, record.timeMs / 1000);
    }
}
