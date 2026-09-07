package com.klotski.ui;

import com.klotski.core.SaveManager;

import java.time.LocalDate;

/**
 * Small, platform-neutral pieces of Desktop Daily Calendar presentation.
 * Swing owns the dialog and buttons; this helper keeps state labels testable.
 */
final class DesktopDailyContent {
    enum DayState {
        COMPLETED,
        IN_PROGRESS,
        READY,
        MISSED,
        FUTURE
    }

    private DesktopDailyContent() {
    }

    static String stateLabel(DayState state) {
        return switch (state) {
            case COMPLETED -> "Completed";
            case IN_PROGRESS -> "In progress";
            case READY -> "Ready";
            case MISSED -> "Missed";
            case FUTURE -> "Future date";
        };
    }

    static String stateLabel(DayState state, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        return switch (state) {
            case COMPLETED -> selected.text("dailyCompleted");
            case IN_PROGRESS -> selected.text("dailyInProgress");
            case READY -> selected.text("dailyReady");
            case MISSED -> selected.text("dailyMissed");
            case FUTURE -> selected.text("dailyFuture");
        };
    }

    static String dayButtonText(LocalDate date, DayState state) {
        String marker = switch (state) {
            case COMPLETED -> "+";
            case IN_PROGRESS -> "~";
            case READY -> "";
            case MISSED -> "!";
            case FUTURE -> "·";
        };
        return marker + date.getDayOfMonth();
    }

    static String dayAccessibilityText(LocalDate date, DayState state) {
        return date + ". " + stateLabel(state) + ". Daily 4x4 Classic puzzle.";
    }

    static String dayAccessibilityText(LocalDate date, DayState state, DesktopLocale locale) {
        return date + ". " + stateLabel(state, locale) + ". "
                + (locale == null ? DesktopLocale.fromTag("en") : locale).text("dailyPuzzle");
    }

    static String progressSummary(SaveManager.DailyProgress progress) {
        if (progress == null) {
            return "Daily streak: 0 · Best: 0";
        }
        return "Daily streak: " + progress.currentStreak + " · Best: " + progress.bestStreak;
    }

    static String progressSummary(SaveManager.DailyProgress progress, DesktopLocale locale) {
        DesktopLocale selected = locale == null ? DesktopLocale.fromTag("en") : locale;
        if (progress == null) {
            return selected.format("dailyProgress", 0, 0);
        }
        return selected.format("dailyProgress", progress.currentStreak, progress.bestStreak);
    }
}
