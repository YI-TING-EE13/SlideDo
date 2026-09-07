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
        return optionLabel(favorite, DesktopLocale.fromTag("en"));
    }

    static String optionLabel(SaveManager.FavoritePuzzle favorite, DesktopLocale locale) {
        if (favorite == null) {
            return (locale == null ? DesktopLocale.fromTag("en") : locale).text("favorites");
        }
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return favorite.label + " · " + favorite.size + "x" + favorite.size
                + " · " + difficultyLabel(favorite.difficulty, selected);
    }

    static String practiceSummary() {
        return practiceSummary(DesktopLocale.fromTag("en"));
    }

    static String practiceSummary(DesktopLocale locale) {
        return (locale == null ? DesktopLocale.fromTag("en") : locale)
                .text("favoritePracticeSummary");
    }

    private static String difficultyLabel(com.klotski.core.PuzzleDifficulty difficulty,
            DesktopLocale locale) {
        return switch (difficulty == null ? com.klotski.core.PuzzleDifficulty.CLASSIC : difficulty) {
            case RELAXED -> locale.text("difficultyRelaxed");
            case CLASSIC -> locale.text("difficultyClassic");
            case CHALLENGE -> locale.text("difficultyChallenge");
        };
    }
}
