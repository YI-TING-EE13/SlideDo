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

    static String status(ContinuousChallenge challenge) {
        return "Continuous: " + challenge.getCompletedPuzzles() + "/"
                + challenge.getTargetPuzzles() + " puzzles · "
                + challenge.getTotalMoves() + " moves · "
                + (challenge.getTotalTimeMs() / 1000) + "s · Assisted: "
                + challenge.getAssistedPuzzles();
    }

    static String result(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty) {
        return "Continuous Challenge\n\n" + status(challenge) + "\nScope: "
                + size + "x" + size + " · " + difficultyLabel(difficulty) + "\n\n"
                + (challenge.isComplete()
                        ? "Challenge complete."
                        : "The solved puzzle is retained for resume or Next Puzzle.");
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }
}
