package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
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

        TextView size = ui.createText(activity.getString(R.string.results_size_format,
                result.size, result.size), 18, Color.WHITE, Typeface.BOLD);
        size.setId(R.id.results_size_text);
        size.setGravity(Gravity.CENTER);
        screen.content.addView(size, ui.fullWidthParams());

        TextView stats = ui.createText(activity.getString(R.string.results_stats_format,
                movesText, result.timeMs / 1000), 24, Color.WHITE, Typeface.BOLD);
        stats.setId(R.id.results_stats_text);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = ui.fullWidthParams();
        statsParams.setMargins(0, ui.dp(10), 0, ui.dp(10));
        screen.content.addView(stats, statsParams);

        TextView record = ui.createText(recordText, 16,
                result.newBest ? COLOR_ACCENT : COLOR_MUTED_TEXT, Typeface.BOLD);
        record.setId(R.id.results_record_text);
        record.setGravity(Gravity.CENTER);
        record.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams recordParams = ui.fullWidthParams();
        recordParams.setMargins(0, 0, 0, ui.dp(22));
        screen.content.addView(record, recordParams);

        Button playAgainButton = ui.addWideButton(screen.content, R.string.results_play_again, COLOR_PRIMARY,
                v -> actions.onPlayAgain());
        playAgainButton.setId(R.id.results_play_again_button);
        Button newSizeButton = ui.addWideButton(screen.content, R.string.results_new_size, COLOR_PANEL,
                v -> actions.onNewSize());
        newSizeButton.setId(R.id.results_new_size_button);
        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home, COLOR_PANEL,
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
