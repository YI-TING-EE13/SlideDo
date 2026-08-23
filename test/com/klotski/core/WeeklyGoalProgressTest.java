package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeeklyGoalProgressTest {
    @Test
    void countsOnlyCompletionsFromMondayThroughToday() {
        WeeklyGoalProgress progress = WeeklyGoalProgress.calculate(
                LocalDate.of(2026, 8, 23), 4, List.of(
                        LocalDate.of(2026, 8, 16),
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 23),
                        LocalDate.of(2026, 8, 24)));

        assertEquals(LocalDate.of(2026, 8, 17), progress.getWeekStart());
        assertEquals(LocalDate.of(2026, 8, 23), progress.getWeekEnd());
        assertEquals(3, progress.getCompleted());
        assertEquals(4, progress.getTarget());
        assertEquals(1, progress.getRemaining());
        assertFalse(progress.isReached());
    }

    @Test
    void goalCanBeExceededWithoutNegativeRemainingCount() {
        WeeklyGoalProgress progress = WeeklyGoalProgress.calculate(
                LocalDate.of(2026, 8, 19), 2, List.of(
                        LocalDate.of(2026, 8, 17),
                        LocalDate.of(2026, 8, 18),
                        LocalDate.of(2026, 8, 19)));

        assertEquals(3, progress.getCompleted());
        assertEquals(0, progress.getRemaining());
        assertTrue(progress.isReached());
    }
}
