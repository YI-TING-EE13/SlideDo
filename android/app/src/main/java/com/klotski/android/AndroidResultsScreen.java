package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_HIGHLIGHT;
import static com.klotski.android.AndroidUi.COLOR_POSITIVE_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the Android post-completion results screen.
 */
final class AndroidResultsScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidResultsScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(GameResult result, String movesText, String recordText, ResultsActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.results_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.results_title),
                activity.getString(result.assisted
                        ? R.string.results_assisted_subtitle
                        : R.string.results_player_subtitle));

        TextView completionMark = ui.createText(activity.getString(R.string.results_completion_mark),
                30, COLOR_POSITIVE_TEXT, Typeface.BOLD);
        completionMark.setId(R.id.results_completion_mark);
        completionMark.setGravity(Gravity.CENTER);
        completionMark.setContentDescription(activity.getString(R.string.results_completion_description));
        completionMark.setBackground(ui.makeCircleBackground(
                COLOR_PANEL_HIGHLIGHT, COLOR_POSITIVE_TEXT));
        completionMark.setScaleX(0.86f);
        completionMark.setScaleY(0.86f);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(ui.dp(62), ui.dp(62));
        markParams.gravity = Gravity.CENTER_HORIZONTAL;
        markParams.setMargins(0, 0, 0, ui.dp(16));
        screen.content.addView(completionMark, markParams);

        LinearLayout summary = new LinearLayout(activity);
        summary.setGravity(Gravity.CENTER_HORIZONTAL);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(ui.dp(18), ui.dp(18), ui.dp(18), ui.dp(18));
        summary.setBackground(ui.makePanelBackground(COLOR_PANEL));

        TextView size = ui.createText(activity.getString(R.string.results_size_format,
                result.size, result.size), 18, Color.WHITE, Typeface.BOLD);
        size.setId(R.id.results_size_text);
        size.setGravity(Gravity.CENTER);
        summary.addView(size, ui.fullWidthParams());

        TextView stats = ui.createText(activity.getString(R.string.results_stats_format,
                movesText, result.timeMs / 1000), 24, Color.WHITE, Typeface.BOLD);
        stats.setId(R.id.results_stats_text);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = ui.fullWidthParams();
        statsParams.setMargins(0, ui.dp(10), 0, ui.dp(10));
        summary.addView(stats, statsParams);

        TextView record = ui.createText(recordText, 16,
                result.newBest ? COLOR_ACCENT : COLOR_MUTED_TEXT, Typeface.BOLD);
        record.setId(R.id.results_record_text);
        record.setGravity(Gravity.CENTER);
        record.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams recordParams = ui.fullWidthParams();
        summary.addView(record, recordParams);

        LinearLayout.LayoutParams summaryParams = ui.fullWidthParams();
        summaryParams.setMargins(0, 0, 0, ui.dp(18));
        screen.content.addView(summary, summaryParams);

        Button playAgainButton = ui.addWideButton(screen.content, R.string.results_play_again,
                R.drawable.ic_action_play, COLOR_PRIMARY,
                v -> actions.onPlayAgain());
        playAgainButton.setId(R.id.results_play_again_button);
        Button newSizeButton = ui.addWideButton(screen.content, R.string.results_new_size,
                R.drawable.ic_action_restart, COLOR_PANEL,
                v -> actions.onNewSize());
        newSizeButton.setId(R.id.results_new_size_button);
        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home,
                R.drawable.ic_action_home, COLOR_PANEL,
                v -> actions.onHome());
        homeButton.setId(R.id.results_home_button);
        return screen;
    }

    interface ResultsActions {
        void onPlayAgain();

        void onNewSize();

        void onHome();
    }
}
