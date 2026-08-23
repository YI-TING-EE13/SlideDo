package com.klotski.core;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable month selection for the offline daily-challenge calendar.
 *
 * <p>The calendar uses Sunday as column zero and prevents navigation or play
 * beyond the caller-supplied local date. The caller owns the clock and time
 * zone, matching {@link DailyChallenge}'s platform-independent date contract.</p>
 */
public final class DailyCalendarMonth {
    private final YearMonth month;
    private final LocalDate today;
    private final List<LocalDate> dates;

    private DailyCalendarMonth(YearMonth requestedMonth, LocalDate today) {
        this.today = Objects.requireNonNull(today, "today");
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth selected = Objects.requireNonNull(requestedMonth, "requestedMonth");
        this.month = selected.isAfter(currentMonth) ? currentMonth : selected;
        List<LocalDate> monthDates = new ArrayList<>(month.lengthOfMonth());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            monthDates.add(month.atDay(day));
        }
        this.dates = Collections.unmodifiableList(monthDates);
    }

    /**
     * Selects a calendar month and clamps future months to the current month.
     *
     * @param requestedMonth month requested by the user or restored state
     * @param today caller-selected local date
     * @return immutable calendar month
     */
    public static DailyCalendarMonth showing(YearMonth requestedMonth, LocalDate today) {
        return new DailyCalendarMonth(requestedMonth, today);
    }

    /**
     * Restores a calendar month from its stable {@code YYYY-MM} identity.
     *
     * @param monthId ISO year-month identity
     * @param today caller-selected local date
     * @return immutable calendar month
     */
    public static DailyCalendarMonth fromMonthId(String monthId, LocalDate today) {
        return showing(YearMonth.parse(Objects.requireNonNull(monthId, "monthId")), today);
    }

    /**
     * Returns the selected year and month.
     *
     * @return selected month
     */
    public YearMonth getMonth() {
        return month;
    }

    /**
     * Returns the stable {@code YYYY-MM} identity used for Activity state.
     *
     * @return selected month identity
     */
    public String getMonthId() {
        return month.toString();
    }

    /**
     * Returns zero-based leading cells for a Sunday-first calendar grid.
     *
     * @return value from zero for Sunday through six for Saturday
     */
    public int getFirstDayOffset() {
        return month.atDay(1).getDayOfWeek().getValue() % 7;
    }

    /**
     * Returns every date in the selected month in ascending order.
     *
     * @return unmodifiable month dates
     */
    public List<LocalDate> getDates() {
        return dates;
    }

    /**
     * Selects the previous calendar month.
     *
     * @return previous month using the same local-date boundary
     */
    public DailyCalendarMonth previous() {
        return showing(month.minusMonths(1), today);
    }

    /**
     * Selects the next month without moving beyond the current month.
     *
     * @return next month, or this month when already current
     */
    public DailyCalendarMonth next() {
        return canGoNext() ? showing(month.plusMonths(1), today) : this;
    }

    /**
     * Reports whether the selected month precedes the current month.
     *
     * @return {@code true} when next-month navigation is allowed
     */
    public boolean canGoNext() {
        return month.isBefore(YearMonth.from(today));
    }

    /**
     * Reports whether a date belongs to this month and is not in the future.
     *
     * @param date date requested for daily play
     * @return {@code true} when the date can be opened
     */
    public boolean isPlayable(LocalDate date) {
        return date != null && YearMonth.from(date).equals(month) && !date.isAfter(today);
    }
}
