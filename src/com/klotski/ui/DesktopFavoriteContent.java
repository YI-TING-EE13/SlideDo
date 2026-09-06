package com.klotski.ui;

import com.klotski.core.SaveManager;

/**
 * Formats the small Swing favorite-library dialogs without owning puzzle
 * rules or persistence decisions.
 */
final class DesktopFavoriteContent {
    private DesktopFavoriteContent() {
    }

    static String optionLabel(SaveManager.FavoritePuzzle favorite) {
        if (favorite == null) {
            return "Favorite";
        }
        return favorite.label + " · " + favorite.size + "x" + favorite.size
                + " · " + difficultyLabel(favorite.difficulty);
    }

    static String practiceSummary() {
        return "Favorite Practice is isolated: saves, records, completion history, "
                + "lifetime statistics, and Daily streaks are not changed.";
    }

    private static String difficultyLabel(com.klotski.core.PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }
}
