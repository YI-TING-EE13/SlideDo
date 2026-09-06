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

    static String progressSummary(SaveManager.DailyProgress progress) {
        if (progress == null) {
            return "Daily streak: 0 · Best: 0";
        }
        return "Daily streak: " + progress.currentStreak + " · Best: " + progress.bestStreak;
    }
}
