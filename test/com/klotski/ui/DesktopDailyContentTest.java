package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.SaveManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DesktopDailyContentTest {
    @Test
    void calendarLabelsExposeKeyboardFriendlyStateMeaning() {
        assertEquals("+21", DesktopDailyContent.dayButtonText(LocalDate.of(2026, 9, 21),
                DesktopDailyContent.DayState.COMPLETED));
        assertEquals("~21", DesktopDailyContent.dayButtonText(LocalDate.of(2026, 9, 21),
                DesktopDailyContent.DayState.IN_PROGRESS));
        assertTrue(DesktopDailyContent.dayAccessibilityText(LocalDate.of(2026, 9, 21),
                DesktopDailyContent.DayState.FUTURE).contains("Future date"));
    }

    @Test
    void streakSummaryUsesDailyProgressValues() {
        String text = DesktopDailyContent.progressSummary(
                new SaveManager.DailyProgress(true, 3, 5, "2026-09-10"));

        assertEquals("Daily streak: 3 · Best: 5", text);
    }
}
