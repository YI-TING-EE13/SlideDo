package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.PersonalTrend;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.WeeklyGoalProgress;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DesktopMaterialContentTest {
    private static final String[] MATERIAL_KEYS = {
            "windowTitle", "statusInitial", "statusAccessibleDescription",
            "menuGameDescription", "menuAssistDescription", "menuSolverDescription",
            "menuHelpDescription", "homeAccessibleDescription", "homeScrollAccessibleDescription",
            "buttonActivate", "startOrLoadFirst", "saveSuccess", "saveFailure", "noSaveFile",
            "savedGameChooserPrompt", "savedGamesTitle", "savedGameSummary", "savedGamesAvailable",
            "gameLoaded", "difficultyChooserPrompt", "difficultyTitle", "scrambleMoves",
            "dailyCalendarSubtitle", "dailyLegend", "weekdaySun", "weekdayMon", "weekdayTue",
            "weekdayWed", "weekdayThu", "weekdayFri", "weekdaySat", "beginnerGuideTextName",
            "learningPageDescription", "learningPreviousDescription", "learningNextDescription",
            "learningSkipDescription", "learningPracticeDescription", "learningStartDescription",
            "practiceBoardName", "tutorialResetDescription", "tutorialStartDescription",
            "tutorialCloseDescription", "tutorialFirstMove", "tutorialWholeLine", "tutorialComplete",
            "saveCurrentFavorite", "noFavorites", "favoriteNamePrompt", "favoriteDefaultLabel",
            "favoriteSaveFailure", "favoriteSaveSuccess", "favoriteReplay", "favoriteRename",
            "favoriteDelete", "favoriteCancel", "favoriteDeletePrompt", "favoriteDeleteTitle",
            "favoriteRenamePrompt", "favoriteRenameFailure", "trendChangeScope", "trendSetGoal",
            "trendClose", "trendScopePrompt", "trendScopeTitle", "weeklyGoalPrompt",
            "weeklyGoalTitle", "weeklyGoalInvalid", "continuousEndSaved", "continuousStartPuzzles",
            "continuousSetupDescription", "continuousTitle", "continuousScopePrompt",
            "continuousScopeTitle", "lastPuzzle", "nextPuzzle", "endChallenge", "home",
            "replayPuzzle", "newSize", "favoriteResultsTitle", "resultsTitle", "continuousResultsTitle",
            "chooseLanguageDescription", "chooseThemeDescription", "reducedMotionDescription",
            "soundDescription", "resetSavedDescription", "resetRecordsDescription",
            "preferencesAccessibleDescription", "savedGamesCleared", "savedGamesClearFailure",
            "recordsCleared", "recordsClearFailure", "statusRunning", "statusBest", "statusReducedMotion",
            "statusDaily", "statusFavorite", "statusSolved", "statusFavoriteSolved", "bestNone",
            "lastPuzzleSummary", "puzzleSolved", "puzzleTitle", "difficultyLabel", "timeLabel",
            "noRecord", "favoritePuzzleTitle", "favoriteResultIsolation", "homeIndependentSaves"
    };

    @Test
    void everyAuditedMaterialKeyIsInTheExplicitCatalog() {
        java.util.List<String> registered = Arrays.asList(DesktopLocale.requiredKeys());
        for (String key : MATERIAL_KEYS) {
            assertTrue(registered.contains(key), "material key missing from registry: " + key);
        }
    }

    @Test
    void normalSaveAndStatusFormatsAreLocalizedWithPlaceholders() {
        DesktopLocale english = DesktopLocale.fromTag("en");
        for (String tag : new String[] {"zh-TW", "ja-JP"}) {
            DesktopLocale locale = DesktopLocale.fromTag(tag);
            assertFalse(locale.format("savedGameSummary", 4, 4, "Classic", 12, 34)
                    .equals(english.format("savedGameSummary", 4, 4, "Classic", 12, 34)), tag);
            assertTrue(locale.format("savedGameSummary", 4, 4, "Classic", 12, 34).contains("4"), tag);
            assertTrue(locale.format("statusRunning", 12, 34, "Classic", "Best: --",
                    "", "", "", "", "", "").contains("12"), tag);
        }
    }

    @Test
    void materialContentHelpersUseSelectedLocale() {
        DesktopLocale locale = DesktopLocale.fromTag("zh-TW");
        String trend = DesktopTrendContent.summary(4, PuzzleDifficulty.CLASSIC,
                PersonalTrend.summarize(Collections.singletonList(new PersonalTrend.Sample(12, 1000))),
                WeeklyGoalProgress.calculate(LocalDate.of(2026, 9, 7), 5, Collections.emptyList()),
                locale);
        String continuous = DesktopContinuousContent.result(
                ContinuousChallenge.start(3), 4, PuzzleDifficulty.CLASSIC, locale);
        String results = DesktopResultContent.resultsMessage(locale, 4, PuzzleDifficulty.CLASSIC,
                12, 34_000, false, false, null, null);
        String daily = DesktopDailyContent.dayAccessibilityText(LocalDate.of(2026, 9, 7),
                DesktopDailyContent.DayState.READY, locale);
        String records = DesktopHomeContent.recordsSummary(
                new com.klotski.core.SaveManager.BestRecord(12, 34_000), null,
                new com.klotski.core.SaveManager.BestRecord(56, 78_000), locale);
        String favorite = DesktopFavoriteContent.practiceSummary(locale);
        String learning = DesktopLearningContent.howToPlay(locale);

        assertTrue(trend.contains(locale.text("trendTitle")));
        assertTrue(continuous.contains(locale.text("continuousTitle")) ||
                continuous.contains(locale.text("continuousRetained")));
        assertTrue(results.contains(locale.text("puzzleSolved")));
        assertTrue(daily.contains(locale.text("dailyReady")));
        assertTrue(records.contains(locale.text("records")));
        assertTrue(favorite.contains(locale.text("favoritePracticeSummary")));
        assertTrue(learning.contains(locale.text("howToPlayTitle")));
    }
}
