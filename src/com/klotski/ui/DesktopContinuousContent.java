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
        return optionLabel(challenge, size, difficulty, DesktopLocale.fromTag("en"));
    }

    static String optionLabel(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousResume", challenge.getCurrentPuzzleNumber(),
                challenge.getTargetPuzzles(), size, size,
                difficultyLabel(difficulty, selected));
    }

    static String status(ContinuousChallenge challenge) {
        return status(challenge, DesktopLocale.fromTag("en"));
    }

    static String status(ContinuousChallenge challenge, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousStatus", challenge.getCompletedPuzzles(),
                challenge.getTargetPuzzles(), challenge.getTotalMoves(),
                challenge.getTotalTimeMs() / 1000, challenge.getAssistedPuzzles());
    }

    static String result(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty) {
        return result(challenge, size, difficulty, DesktopLocale.fromTag("en"));
    }

    static String result(ContinuousChallenge challenge, int size,
            PuzzleDifficulty difficulty, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return selected.format("continuousResult", status(challenge, selected), size, size,
                difficultyLabel(difficulty, selected),
                challenge.isComplete() ? selected.text("continuousComplete")
                        : selected.text("continuousRetained"));
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty, DesktopLocale locale) {
        return switch (difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }
}
