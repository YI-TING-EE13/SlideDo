package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the Android Home screen.
 */
final class AndroidHomeScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidHomeScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(AndroidGameStore.SaveMetadata metadata, HomeActions actions) {
        boolean hasSave = metadata != null;
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.home_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.app_name),
                activity.getString(R.string.home_tagline));

        TextView summary = ui.createText(activity.getString(R.string.home_summary),
                16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams summaryParams = ui.fullWidthParams();
        summaryParams.setMargins(0, 0, 0, ui.dp(20));
        screen.content.addView(summary, summaryParams);

        if (hasSave) {
            Button continueButton = ui.addWideButton(screen.content, R.string.home_continue,
                    R.drawable.ic_action_continue, COLOR_PRIMARY,
                    v -> actions.onContinue());
            continueButton.setId(R.id.home_continue_button);

            TextView continueSummary = ui.createText(formatContinueSummary(metadata),
                    14, COLOR_MUTED_TEXT, Typeface.NORMAL);
            continueSummary.setId(R.id.home_continue_summary_text);
            continueSummary.setGravity(Gravity.CENTER);
            continueSummary.setLineSpacing(0, 1.12f);
            LinearLayout.LayoutParams continueSummaryParams = ui.fullWidthParams();
            continueSummaryParams.setMargins(0, 0, 0, ui.dp(14));
            screen.content.addView(continueSummary, continueSummaryParams);
        }

        Button newGameButton = ui.addWideButton(screen.content,
                hasSave ? R.string.home_new_game : R.string.home_play,
                R.drawable.ic_action_play,
                hasSave ? COLOR_PANEL_LIGHT : COLOR_PRIMARY,
                v -> actions.onPlay());
        newGameButton.setId(R.id.home_new_game_button);

        ui.addSectionLabel(screen.content, R.string.home_section_learn);
        LinearLayout learningRow = createActionRow(screen.content);
        Button onboardingButton = ui.addRowButton(learningRow,
                R.string.home_beginner_guide, R.drawable.ic_action_guide,
                COLOR_PANEL, v -> actions.onBeginnerGuide());
        onboardingButton.setId(R.id.home_onboarding_button);

        Button tutorialButton = ui.addRowButton(learningRow,
                R.string.home_tutorial, R.drawable.ic_action_tutorial,
                COLOR_PANEL_LIGHT, v -> actions.onPracticeTutorial());
        tutorialButton.setId(R.id.home_tutorial_button);

        Button howToButton = ui.addWideButton(screen.content,
                R.string.home_how_to_play, R.drawable.ic_action_help,
                COLOR_PANEL, v -> actions.onHowToPlay());
        howToButton.setId(R.id.home_how_to_play_button);

        ui.addSectionLabel(screen.content, R.string.home_section_your_game);
        LinearLayout personalRow = createActionRow(screen.content);
        Button settingsButton = ui.addRowButton(personalRow,
                R.string.home_settings, R.drawable.ic_action_settings,
                COLOR_PANEL, v -> actions.onSettings());
        settingsButton.setId(R.id.home_settings_button);

        Button recordsButton = ui.addRowButton(personalRow,
                R.string.home_records, R.drawable.ic_action_records,
                COLOR_PANEL, v -> actions.onRecords());
        recordsButton.setId(R.id.home_records_button);

        return screen;
    }

    private LinearLayout createActionRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(-ui.dp(4), 0, -ui.dp(4), ui.dp(10));
        parent.addView(row, params);
        return row;
    }

    private String formatContinueSummary(AndroidGameStore.SaveMetadata metadata) {
        String state = metadata.solved
                ? activity.getString(R.string.home_continue_solved)
                : (metadata.active
                        ? activity.getString(R.string.home_continue_active)
                        : activity.getString(R.string.home_continue_saved));
        return activity.getString(R.string.home_continue_summary,
                state,
                metadata.size,
                metadata.size,
                formatMoves(metadata.moves),
                Math.max(0, metadata.elapsedMs) / 1000,
                formatSavedAge(metadata.updatedAt));
    }

    private String formatSavedAge(long updatedAt) {
        if (updatedAt <= 0) {
            return activity.getString(R.string.home_continue_updated_unknown);
        }

        long ageSeconds = Math.max(0, (System.currentTimeMillis() - updatedAt) / 1000);
        if (ageSeconds < 60) {
            return activity.getString(R.string.home_continue_updated_now);
        }

        long ageMinutes = ageSeconds / 60;
        if (ageMinutes < 60) {
            return activity.getString(R.string.home_continue_updated_minutes, ageMinutes);
        }

        long ageHours = ageMinutes / 60;
        if (ageHours < 24) {
            return activity.getString(R.string.home_continue_updated_hours, ageHours);
        }

        return activity.getString(R.string.home_continue_updated_days, ageHours / 24);
    }

    private String formatMoves(int moves) {
        return activity.getResources().getQuantityString(R.plurals.moves_count, moves, moves);
    }

    interface HomeActions {
        void onContinue();

        void onPlay();

        void onBeginnerGuide();

        void onPracticeTutorial();

        void onHowToPlay();

        void onSettings();

        void onRecords();
    }
}
