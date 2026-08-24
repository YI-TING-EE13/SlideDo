package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_HIGHLIGHT;
import static com.klotski.android.AndroidUi.COLOR_POSITIVE_TEXT;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.klotski.core.PersonalTrend;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.WeeklyGoalProgress;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Builder for the offline personal trends and weekly goal screen.
 */
final class AndroidTrendsScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidTrendsScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(WeeklyGoalProgress goal, int size, PuzzleDifficulty difficulty,
            PersonalTrend trend, TrendsActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.trends_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.trends_title),
                activity.getString(R.string.trends_subtitle));

        addSectionTitle(screen.content, R.string.trends_goal_section);
        addGoalPanel(screen.content, goal, actions);
        addSectionTitle(screen.content, R.string.trends_scope_section);
        addScopePanel(screen.content, size, difficulty, actions);
        addSectionTitle(screen.content, R.string.trends_comparison_section);
        addTrendPanel(screen.content, trend);

        Button back = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL,
                view -> actions.onBack());
        back.setId(R.id.trends_back_button);
        return screen;
    }

    private void addGoalPanel(LinearLayout parent, WeeklyGoalProgress goal,
            TrendsActions actions) {
        LinearLayout panel = createPanel(COLOR_PANEL_HIGHLIGHT);
        TextView week = ui.createText(activity.getString(R.string.trends_goal_week,
                formatDate(goal.getWeekStart()), formatDate(goal.getWeekEnd())),
                14, COLOR_MUTED_TEXT, Typeface.BOLD);
        TextView summary = ui.createText(activity.getString(R.string.trends_goal_summary,
                goal.getCompleted(), goal.getTarget()), 22, Color.WHITE, Typeface.BOLD);
        summary.setId(R.id.trends_goal_summary_text);
        ProgressBar progress = new ProgressBar(activity, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setId(R.id.trends_goal_progress);
        progress.setMax(goal.getTarget());
        progress.setProgress(Math.min(goal.getCompleted(), goal.getTarget()));
        TextView status = ui.createText(goal.isReached()
                        ? activity.getString(R.string.trends_goal_reached)
                        : activity.getResources().getQuantityString(
                                R.plurals.trends_goal_remaining, goal.getRemaining(),
                                goal.getRemaining()),
                15, goal.isReached() ? COLOR_POSITIVE_TEXT : COLOR_MUTED_TEXT, Typeface.BOLD);
        status.setId(R.id.trends_goal_status_text);

        panel.addView(week, ui.fullWidthParams());
        panel.addView(summary, spacedParams(8));
        panel.addView(progress, spacedParams(10));
        panel.addView(status, spacedParams(8));
        Button setGoal = ui.addWideButton(panel, R.string.trends_set_goal,
                COLOR_ACCENT, view -> actions.onSetGoal());
        setGoal.setId(R.id.trends_set_goal_button);
        addPanel(parent, panel);
    }

    private void addScopePanel(LinearLayout parent, int size, PuzzleDifficulty difficulty,
            TrendsActions actions) {
        LinearLayout panel = createPanel(COLOR_PANEL);
        TextView scope = ui.createText(activity.getString(R.string.trends_scope,
                size, size, difficultyName(difficulty)), 20, Color.WHITE, Typeface.BOLD);
        scope.setId(R.id.trends_scope_text);
        panel.addView(scope, ui.fullWidthParams());
        Button choose = ui.addWideButton(panel, R.string.trends_choose_scope,
                COLOR_PANEL_HIGHLIGHT, view -> actions.onChooseScope());
        choose.setId(R.id.trends_choose_scope_button);
        addPanel(parent, panel);
    }

    private void addTrendPanel(LinearLayout parent, PersonalTrend trend) {
        LinearLayout panel = createPanel(COLOR_PANEL);
        if (trend.getRecentCount() == 0) {
            TextView empty = ui.createText(activity.getString(R.string.trends_empty),
                    16, COLOR_MUTED_TEXT, Typeface.NORMAL);
            empty.setId(R.id.trends_moves_text);
            panel.addView(empty, ui.fullWidthParams());
            addPanel(parent, panel);
            return;
        }

        TextView moves = ui.createText(formatMetric(R.string.trends_metric_moves,
                trend.getRecentAverageMoves(), trend.getPreviousAverageMoves(),
                trend.getMoveChangePercent(), trend.getMoveDirection()),
                17, directionColor(trend.getMoveDirection()), Typeface.BOLD);
        moves.setId(R.id.trends_moves_text);
        TextView time = ui.createText(formatMetric(R.string.trends_metric_time,
                Math.round(trend.getRecentAverageTimeMs() / 1000.0),
                Math.round(trend.getPreviousAverageTimeMs() / 1000.0),
                trend.getTimeChangePercent(), trend.getTimeDirection()),
                17, directionColor(trend.getTimeDirection()), Typeface.BOLD);
        time.setId(R.id.trends_time_text);
        panel.addView(moves, ui.fullWidthParams());
        panel.addView(time, spacedParams(10));
        if (trend.getMoveDirection() == PersonalTrend.Direction.NOT_ENOUGH_DATA) {
            int remaining = Math.max(0, 6 - trend.getRecentCount());
            TextView note = ui.createText(activity.getResources().getQuantityString(
                    R.plurals.trends_more_needed, remaining, remaining),
                    14, COLOR_MUTED_TEXT, Typeface.NORMAL);
            panel.addView(note, spacedParams(10));
        }
        addPanel(parent, panel);
    }

    private String formatMetric(int labelRes, long recent, long previous, int change,
            PersonalTrend.Direction direction) {
        String label = activity.getString(labelRes);
        if (direction == PersonalTrend.Direction.NOT_ENOUGH_DATA) {
            return activity.getString(R.string.trends_metric_recent, label, recent);
        }
        return activity.getString(R.string.trends_metric_compare,
                label, recent, previous, formatDirection(direction, change));
    }

    private String formatDirection(PersonalTrend.Direction direction, int change) {
        int magnitude = Math.abs(change);
        return switch (direction) {
            case IMPROVING -> activity.getString(R.string.trends_improving, magnitude);
            case DECLINING -> activity.getString(R.string.trends_declining, magnitude);
            case STEADY -> activity.getString(R.string.trends_steady, magnitude);
            case NOT_ENOUGH_DATA -> "";
        };
    }

    private int directionColor(PersonalTrend.Direction direction) {
        return direction == PersonalTrend.Direction.IMPROVING
                ? COLOR_POSITIVE_TEXT : Color.WHITE;
    }

    private LinearLayout createPanel(int color) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        panel.setBackground(ui.makePanelBackground(color));
        return panel;
    }

    private void addSectionTitle(LinearLayout parent, int textResId) {
        TextView title = ui.createText(activity.getString(textResId),
                13, COLOR_MUTED_TEXT, Typeface.BOLD);
        ui.markAccessibilityHeading(title);
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(ui.dp(6), ui.dp(4), 0, ui.dp(10));
        parent.addView(title, params);
    }

    private void addPanel(LinearLayout parent, LinearLayout panel) {
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(0, 0, 0, ui.dp(14));
        parent.addView(panel, params);
    }

    private LinearLayout.LayoutParams spacedParams(int topDp) {
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(0, ui.dp(topDp), 0, 0);
        return params;
    }

    private String formatDate(LocalDate date) {
        Date instant = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return DateFormat.getDateFormat(activity).format(instant);
    }

    private String difficultyName(PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null
                ? PuzzleDifficulty.CLASSIC : difficulty;
        return activity.getString(switch (selected) {
            case RELAXED -> R.string.difficulty_relaxed;
            case CLASSIC -> R.string.difficulty_classic;
            case CHALLENGE -> R.string.difficulty_challenge;
        });
    }

    interface TrendsActions {
        void onSetGoal();

        void onChooseScope();

        void onBack();
    }
}
