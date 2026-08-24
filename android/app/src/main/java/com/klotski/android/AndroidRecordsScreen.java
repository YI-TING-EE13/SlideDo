package com.klotski.android;

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
import android.widget.TextView;

import com.klotski.core.PuzzleDifficulty;

import java.util.Date;

/**
 * Builder for the Android records screen.
 */
final class AndroidRecordsScreen {
    private static final int RECENT_HISTORY_DISPLAY_LIMIT = 10;

    private final Activity activity;
    private final AndroidUi ui;

    AndroidRecordsScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(RecordsDataProvider records, RecordsActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.records_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.records_title),
                activity.getString(R.string.records_subtitle));
        addExplanation(screen.content);
        addSectionTitle(screen.content, R.string.records_totals_title);
        addOverallSummary(screen.content, records.getOverallStats());
        addSectionTitle(screen.content, R.string.records_recent_title);
        addRecentHistory(screen.content, records.getHistory());
        addSectionTitle(screen.content, R.string.records_breakdown_title);
        for (int size = 3; size <= 5; size++) {
            for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
                addRecordRow(screen.content, size, difficulty,
                        records.getRecordText(size, difficulty),
                        records.getStats(size, difficulty));
            }
        }

        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL,
                v -> actions.onBack());
        backButton.setId(R.id.records_back_button);
        return screen;
    }

    private void addExplanation(LinearLayout parent) {
        TextView explanation = ui.createText(activity.getString(R.string.records_explanation),
                15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        explanation.setId(R.id.records_explanation_text);
        explanation.setLineSpacing(0, 1.12f);
        explanation.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        explanation.setBackground(ui.makePanelBackground(COLOR_PANEL_HIGHLIGHT));
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(0, 0, 0, ui.dp(16));
        parent.addView(explanation, params);
    }

    private void addSectionTitle(LinearLayout parent, int textResId) {
        TextView title = ui.createText(activity.getString(textResId),
                13, COLOR_MUTED_TEXT, Typeface.BOLD);
        ui.markAccessibilityHeading(title);
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(ui.dp(6), ui.dp(4), 0, ui.dp(10));
        parent.addView(title, params);
    }

    private void addOverallSummary(LinearLayout parent, AndroidGameStore.CompletionStats stats) {
        LinearLayout panel = createPanel();
        TextView player = ui.createText(formatPlayerCompletions(stats.playerCompletions),
                20, COLOR_POSITIVE_TEXT, Typeface.BOLD);
        player.setId(R.id.records_overall_player_text);
        TextView assisted = ui.createText(formatAssistedCompletions(stats.assistedCompletions),
                16, Color.WHITE, Typeface.NORMAL);
        assisted.setId(R.id.records_overall_assisted_text);
        TextView average = ui.createText(formatPlayerAverage(stats),
                15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        average.setId(R.id.records_overall_average_text);

        panel.addView(player, ui.fullWidthParams());
        LinearLayout.LayoutParams secondaryParams = ui.fullWidthParams();
        secondaryParams.setMargins(0, ui.dp(6), 0, 0);
        panel.addView(assisted, secondaryParams);
        LinearLayout.LayoutParams averageParams = ui.fullWidthParams();
        averageParams.setMargins(0, ui.dp(6), 0, 0);
        panel.addView(average, averageParams);
        addPanel(parent, panel);
    }

    private void addRecentHistory(LinearLayout parent, AndroidGameStore.CompletionRecord[] history) {
        AndroidGameStore.CompletionRecord[] entries = history == null
                ? new AndroidGameStore.CompletionRecord[0]
                : history;
        if (entries.length == 0) {
            TextView empty = ui.createText(activity.getString(R.string.records_recent_empty),
                    16, COLOR_MUTED_TEXT, Typeface.NORMAL);
            empty.setId(R.id.records_history_empty_text);
            empty.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
            empty.setBackground(ui.makePanelBackground(COLOR_PANEL));
            LinearLayout.LayoutParams params = ui.fullWidthParams();
            params.setMargins(0, 0, 0, ui.dp(16));
            parent.addView(empty, params);
            return;
        }

        int displayed = Math.min(entries.length, RECENT_HISTORY_DISPLAY_LIMIT);
        for (int index = 0; index < displayed; index++) {
            addHistoryRow(parent, entries[index]);
        }
    }

    private void addHistoryRow(LinearLayout parent, AndroidGameStore.CompletionRecord record) {
        LinearLayout row = createPanel();
        String date = DateFormat.getMediumDateFormat(activity).format(new Date(record.completedAt));
        TextView title = ui.createText(activity.getString(R.string.records_history_title,
                date, record.size, record.size, difficultyName(record.difficulty)),
                15, COLOR_MUTED_TEXT, Typeface.BOLD);
        TextView metrics = ui.createText(activity.getString(R.string.records_history_metrics,
                activity.getString(record.assisted
                        ? R.string.records_history_assisted
                        : R.string.records_history_player),
                formatMoves(record.moves), record.timeMs / 1000),
                17, record.assisted ? Color.WHITE : COLOR_POSITIVE_TEXT, Typeface.BOLD);
        row.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams metricsParams = ui.fullWidthParams();
        metricsParams.setMargins(0, ui.dp(6), 0, 0);
        row.addView(metrics, metricsParams);
        addPanel(parent, row);
    }

    private void addRecordRow(LinearLayout parent, int size, PuzzleDifficulty difficulty,
            String bestText, AndroidGameStore.CompletionStats stats) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));

        TextView title = ui.createText(activity.getString(R.string.records_row_title,
                size, size, difficultyName(difficulty)), 15, COLOR_MUTED_TEXT, Typeface.BOLD);
        TextView best = ui.createText(bestText, 20,
                activity.getString(R.string.records_empty).equals(bestText)
                        ? Color.WHITE : COLOR_POSITIVE_TEXT,
                Typeface.BOLD);
        TextView completions = ui.createText(activity.getString(R.string.records_scope_stats,
                formatPlayerCompletions(stats.playerCompletions),
                formatAssistedCompletions(stats.assistedCompletions)),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        TextView average = ui.createText(formatPlayerAverage(stats),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(6), 0, 0);
        row.addView(best, bestParams);
        LinearLayout.LayoutParams statsParams = ui.fullWidthParams();
        statsParams.setMargins(0, ui.dp(6), 0, 0);
        row.addView(completions, statsParams);
        LinearLayout.LayoutParams averageParams = ui.fullWidthParams();
        averageParams.setMargins(0, ui.dp(4), 0, 0);
        row.addView(average, averageParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    private LinearLayout createPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        panel.setBackground(ui.makePanelBackground(COLOR_PANEL));
        return panel;
    }

    private void addPanel(LinearLayout parent, LinearLayout panel) {
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(panel, params);
    }

    private String formatPlayerCompletions(int count) {
        return activity.getResources().getQuantityString(
                R.plurals.records_player_solves, count, count);
    }

    private String formatAssistedCompletions(int count) {
        return activity.getResources().getQuantityString(
                R.plurals.records_assisted_solves, count, count);
    }

    private String formatPlayerAverage(AndroidGameStore.CompletionStats stats) {
        if (stats.playerCompletions == 0) {
            return activity.getString(R.string.records_player_average_empty);
        }
        long averageMoves = Math.round((double) stats.playerMoves / stats.playerCompletions);
        long averageSeconds = Math.round(
                (double) stats.playerTimeMs / stats.playerCompletions / 1000.0);
        return activity.getString(R.string.records_player_average,
                formatMoves(averageMoves), averageSeconds);
    }

    private String formatMoves(long moves) {
        int quantity = moves > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) moves;
        return activity.getResources().getQuantityString(R.plurals.moves_count, quantity, moves);
    }

    private String difficultyName(PuzzleDifficulty difficulty) {
        return activity.getString(switch (difficulty) {
            case RELAXED -> R.string.difficulty_relaxed;
            case CLASSIC -> R.string.difficulty_classic;
            case CHALLENGE -> R.string.difficulty_challenge;
        });
    }

    interface RecordsDataProvider {
        String getRecordText(int size, PuzzleDifficulty difficulty);

        AndroidGameStore.CompletionStats getStats(int size, PuzzleDifficulty difficulty);

        AndroidGameStore.CompletionStats getOverallStats();

        AndroidGameStore.CompletionRecord[] getHistory();
    }

    interface RecordsActions {
        void onBack();
    }
}
