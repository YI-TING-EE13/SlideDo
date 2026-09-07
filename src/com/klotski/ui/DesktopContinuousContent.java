package com.klotski.ui;

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.PuzzleDifficulty;

/**
 * Formats the isolated Continuous Challenge status and result copy.
 */
final class DesktopContinuousContent {
    private DesktopContinuousContent() {
    }

    static String optionLabel(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty) {
        return "Resume " + challenge.getCurrentPuzzleNumber() + "/"
                + challenge.getTargetPuzzles() + " · " + size + "x" + size
                + " · " + difficultyLabel(difficulty);
    }

    static String optionLabel(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousResume", challenge.getCurrentPuzzleNumber(),
                challenge.getTargetPuzzles(), size, size,
                difficultyLabel(difficulty, selected));
    }

    static String status(ContinuousChallenge challenge) {
        return "Continuous: " + challenge.getCompletedPuzzles() + "/"
                + challenge.getTargetPuzzles() + " puzzles · "
                + challenge.getTotalMoves() + " moves · "
                + (challenge.getTotalTimeMs() / 1000) + "s · Assisted: "
                + challenge.getAssistedPuzzles();
    }

    static String status(ContinuousChallenge challenge, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousStatus", challenge.getCompletedPuzzles(),
                challenge.getTargetPuzzles(), challenge.getTotalMoves(),
                challenge.getTotalTimeMs() / 1000, challenge.getAssistedPuzzles());
    }

    static String result(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty) {
        return "Continuous Challenge\n\n" + status(challenge) + "\nScope: "
                + size + "x" + size + " · " + difficultyLabel(difficulty) + "\n\n"
                + (challenge.isComplete()
                        ? "Challenge complete."
                        : "The solved puzzle is retained for resume or Next Puzzle.");
    }

    static String result(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousResult", status(challenge, selected), size, size,
                difficultyLabel(difficulty, selected),
                challenge.isComplete() ? selected.text("continuousComplete")
                        : selected.text("continuousRetained"));
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
