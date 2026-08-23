package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class DailyCalendarMonthTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 23);

    @Test
    void monthExposesSundayFirstDatesAndDeterministicNavigation() {
        DailyCalendarMonth july = DailyCalendarMonth.showing(
                YearMonth.of(2026, 7), TODAY);

        assertEquals("2026-07", july.getMonthId());
        assertEquals(3, july.getFirstDayOffset());
        assertEquals(31, july.getDates().size());
        assertEquals(LocalDate.of(2026, 7, 1), july.getDates().get(0));
        assertEquals(LocalDate.of(2026, 7, 31), july.getDates().get(30));
        assertEquals("2026-06", july.previous().getMonthId());
        assertEquals("2026-08", july.next().getMonthId());
        assertTrue(july.canGoNext());
    }

    @Test
    void futureMonthsAndDatesAreNotPlayable() {
        DailyCalendarMonth current = DailyCalendarMonth.showing(
                YearMonth.of(2026, 10), TODAY);

        assertEquals("2026-08", current.getMonthId());
        assertFalse(current.canGoNext());
        assertEquals("2026-08", current.next().getMonthId());
        assertTrue(current.isPlayable(LocalDate.of(2026, 8, 23)));
        assertFalse(current.isPlayable(LocalDate.of(2026, 8, 24)));
        assertFalse(current.isPlayable(LocalDate.of(2026, 7, 31)));
    }
}
