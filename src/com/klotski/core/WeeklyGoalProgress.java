package com.klotski.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Offline progress for an owner-defined weekly player-completion goal.
 * Weeks run from Monday through Sunday. Only completions from the start of the
 * current week through the supplied current date contribute, so future-dated
 * or stale records cannot inflate progress.
 */
public final class WeeklyGoalProgress {
    /** Minimum configurable weekly player-completion target. */
    public static final int MIN_TARGET = 1;

    /** Maximum configurable weekly player-completion target. */
    public static final int MAX_TARGET = 50;

    private final LocalDate weekStart;
    private final LocalDate weekEnd;
    private final int completed;
    private final int target;

    private WeeklyGoalProgress(LocalDate weekStart, LocalDate weekEnd,
            int completed, int target) {
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.completed = completed;
        this.target = target;
    }

    /**
     * Reports whether a value can be stored as a weekly goal target.
     *
     * @param target candidate number of player completions
     * @return {@code true} when the target is from 1 through 50
     */
    public static boolean isValidTarget(int target) {
        return target >= MIN_TARGET && target <= MAX_TARGET;
    }

    /**
     * Calculates progress from local completion dates.
     *
     * @param today device-local current date
     * @param target owner-selected target from 1 through 50
     * @param completionDates player completion dates; duplicate dates count as
     *        separate completed puzzles
     * @return immutable current-week goal progress
     */
    public static WeeklyGoalProgress calculate(LocalDate today, int target,
            List<LocalDate> completionDates) {
        Objects.requireNonNull(today, "today");
        if (!isValidTarget(target)) {
            throw new IllegalArgumentException("Weekly goal must be from 1 through 50");
        }
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        int count = 0;
        List<LocalDate> dates = completionDates == null
                ? Collections.emptyList() : completionDates;
        for (LocalDate date : dates) {
            if (date != null && !date.isBefore(start) && !date.isAfter(today)) {
                count++;
            }
        }
        return new WeeklyGoalProgress(start, end, count, target);
    }

    /**
     * Returns the Monday that starts the represented local week.
     *
     * @return local week start
     */
    public LocalDate getWeekStart() {
        return weekStart;
    }

    /**
     * Returns the Sunday that ends the represented local week.
     *
     * @return local week end
     */
    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    /**
     * Returns player completions counted from Monday through today.
     *
     * @return current-week player completion count
     */
    public int getCompleted() {
        return completed;
    }

    /**
     * Returns the owner-selected weekly target.
     *
     * @return weekly player completion target
     */
    public int getTarget() {
        return target;
    }

    /**
     * Returns the additional completions needed, never negative.
     *
     * @return remaining player completions
     */
    public int getRemaining() {
        return Math.max(0, target - completed);
    }

    /**
     * Reports whether current completions meet or exceed the target.
     *
     * @return {@code true} when the weekly goal is reached
     */
    public boolean isReached() {
        return completed >= target;
    }
}
