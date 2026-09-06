package com.klotski.ui;

import com.klotski.core.PersonalTrend;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.WeeklyGoalProgress;

/**
 * Formats shared PersonalTrend and WeeklyGoalProgress values for Desktop.
 */
final class DesktopTrendContent {
    private DesktopTrendContent() {
    }

    static String summary(int size, PuzzleDifficulty difficulty,
            PersonalTrend trend, WeeklyGoalProgress goal) {
        StringBuilder text = new StringBuilder("Trends and Weekly Goal\n\n")
                .append("Scope: ").append(size).append("x").append(size)
                .append(" · ").append(difficultyLabel(difficulty)).append("\n")
                .append("Weekly goal: ").append(goal.getCompleted()).append("/")
                .append(goal.getTarget()).append(goal.isReached() ? " · reached" : "")
                .append("\nWeek: ").append(goal.getWeekStart()).append(" to ")
                .append(goal.getWeekEnd()).append("\n\n");
        if (trend.getRecentCount() == 0) {
            return text.append("No player completions in this scope yet.\n")
                    .append("Assisted and Favorite Practice results are excluded.").toString();
        }
        text.append("Recent player solves: ").append(trend.getRecentCount())
                .append(" · average ").append(trend.getRecentAverageMoves()).append(" moves, ")
                .append(trend.getRecentAverageTimeMs() / 1000).append("s\n");
        if (trend.getPreviousCount() == 0) {
            text.append("Comparison: need at least six player solves.\n");
        } else {
            text.append("Previous window: ").append(trend.getPreviousAverageMoves())
                    .append(" moves, ").append(trend.getPreviousAverageTimeMs() / 1000)
                    .append("s\n")
                    .append("Moves: ").append(directionLabel(trend.getMoveDirection()))
                    .append(" ( ").append(trend.getMoveChangePercent()).append("% )\n")
                    .append("Time: ").append(directionLabel(trend.getTimeDirection()))
                    .append(" ( ").append(trend.getTimeChangePercent()).append("% )\n");
        }
        return text.append("\nAssisted and Favorite Practice results are excluded.").toString();
    }

    static String scopeLabel(int size, PuzzleDifficulty difficulty) {
        return size + "x" + size + " · " + difficultyLabel(difficulty);
    }

    private static String directionLabel(PersonalTrend.Direction direction) {
        return switch (direction) {
            case IMPROVING -> "improving";
            case DECLINING -> "declining";
            case STEADY -> "steady";
            case NOT_ENOUGH_DATA -> "not enough data";
        };
    }

    private static String difficultyLabel(PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }
}
