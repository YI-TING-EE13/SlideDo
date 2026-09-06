package com.klotski.ui;

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.PersonalTrend;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;
import com.klotski.core.WeeklyGoalProgress;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopPersonalPlayContentTest {
    @Test
    void favoriteCopyStatesIsolationContract() {
        assertTrue(DesktopFavoriteContent.practiceSummary().contains("isolated"));
    }

    @Test
    void trendCopyUsesSharedScopeAndExclusionLanguage() {
        PersonalTrend trend = PersonalTrend.summarize(Collections.singletonList(
                new PersonalTrend.Sample(12, 1_000L)));
        WeeklyGoalProgress goal = WeeklyGoalProgress.calculate(
                LocalDate.of(2026, 9, 7), 5, Collections.emptyList());
        String text = DesktopTrendContent.summary(4, PuzzleDifficulty.CLASSIC, trend, goal);
        assertTrue(text.contains("4x4"));
        assertTrue(text.contains("Assisted"));
    }

    @Test
    void continuousCopyIncludesAggregateAndFixedScope() {
        String text = DesktopContinuousContent.result(
                ContinuousChallenge.start(3), 5, PuzzleDifficulty.CHALLENGE);
        assertTrue(text.contains("0/3"));
        assertTrue(text.contains("5x5"));
    }
}
