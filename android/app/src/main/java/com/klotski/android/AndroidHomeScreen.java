package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klotski.core.PuzzleDifficulty;

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

    ScreenLayout build(AndroidGameStore.SaveMetadata[] saves, DailyStatus dailyStatus,
            ContinuousStatus continuousStatus, int favoriteCount,
            HomeActions actions) {
        AndroidGameStore.SaveMetadata[] availableSaves = saves == null
                ? new AndroidGameStore.SaveMetadata[0]
                : saves;
        boolean hasSave = availableSaves.length > 0;
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.home_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.app_name),
                activity.getString(R.string.home_tagline));

        Button dailyButton = ui.addWideButton(screen.content,
                R.string.home_daily_challenge, R.drawable.ic_action_play, COLOR_ACCENT,
                v -> actions.onDailyChallenge());
        dailyButton.setId(R.id.home_daily_button);
        TextView dailySummary = ui.createText(formatDailySummary(dailyStatus),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        dailySummary.setId(R.id.home_daily_summary_text);
        dailySummary.setGravity(Gravity.CENTER);
        dailySummary.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams dailySummaryParams = ui.fullWidthParams();
        dailySummaryParams.setMargins(0, 0, 0, ui.dp(14));
        screen.content.addView(dailySummary, dailySummaryParams);

        if (hasSave) {
            Button continueButton = ui.addWideButton(screen.content, R.string.home_continue,
                    R.drawable.ic_action_continue, COLOR_PRIMARY,
                    v -> actions.onContinue());
            continueButton.setId(R.id.home_continue_button);

            String summaryText = availableSaves.length == 1
                    ? formatContinueSummary(availableSaves[0])
                    : activity.getString(R.string.home_multiple_saves_summary, availableSaves.length);
            TextView continueSummary = ui.createText(summaryText,
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
        Button trendsButton = ui.addWideButton(screen.content,
                R.string.home_trends, R.drawable.ic_action_records,
                COLOR_PANEL, v -> actions.onTrends());
        trendsButton.setId(R.id.home_trends_button);
        Button favoritesButton = ui.addWideButton(screen.content,
                R.string.home_favorites, R.drawable.ic_action_records,
                COLOR_PANEL_LIGHT, v -> actions.onFavorites());
        favoritesButton.setId(R.id.home_favorites_button);
        favoritesButton.setContentDescription(activity.getResources().getQuantityString(
                R.plurals.home_favorites_count, favoriteCount, favoriteCount));
        LinearLayout personalRow = createActionRow(screen.content);
        Button settingsButton = ui.addRowButton(personalRow,
                R.string.home_settings, R.drawable.ic_action_settings,
                COLOR_PANEL, v -> actions.onSettings());
        settingsButton.setId(R.id.home_settings_button);

        Button recordsButton = ui.addRowButton(personalRow,
                R.string.home_records, R.drawable.ic_action_records,
                COLOR_PANEL, v -> actions.onRecords());
        recordsButton.setId(R.id.home_records_button);

        ContinuousStatus session = continuousStatus == null
                ? ContinuousStatus.EMPTY : continuousStatus;
        Button continuousButton = ui.addWideButton(screen.content,
                R.string.home_continuous, R.drawable.ic_action_play, COLOR_PANEL_LIGHT,
                view -> actions.onContinuousChallenge());
        continuousButton.setId(R.id.home_continuous_button);
        TextView continuousSummary = ui.createText(
                session.active
                        ? activity.getString(R.string.home_continuous_active,
                                session.currentPuzzle, session.targetPuzzles,
                                session.size, session.size,
                                difficultyName(session.difficulty))
                        : activity.getString(R.string.home_continuous_ready),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        continuousSummary.setId(R.id.home_continuous_summary_text);
        continuousSummary.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams continuousParams = ui.fullWidthParams();
        continuousParams.setMargins(0, 0, 0, ui.dp(14));
        screen.content.addView(continuousSummary, continuousParams);

        return screen;
    }

    private LinearLayout createActionRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(ui.shouldStackDenseActions()
                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
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
                difficultyName(metadata.difficulty),
                formatMoves(metadata.moves),
                Math.max(0, metadata.elapsedMs) / 1000,
                formatSavedAge(metadata.updatedAt));
    }

    private String difficultyName(com.klotski.core.PuzzleDifficulty difficulty) {
        return activity.getString(switch (difficulty) {
            case RELAXED -> R.string.difficulty_relaxed;
            case CLASSIC -> R.string.difficulty_classic;
            case CHALLENGE -> R.string.difficulty_challenge;
        });
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

    private String formatDailySummary(DailyStatus dailyStatus) {
        DailyStatus status = dailyStatus == null ? DailyStatus.EMPTY : dailyStatus;
        int stateString = status.completed
                ? R.string.home_daily_completed
                : (status.inProgress ? R.string.home_daily_in_progress : R.string.home_daily_ready);
        return activity.getString(R.string.home_daily_summary,
                status.dateId,
                activity.getString(stateString),
                status.currentStreak,
                status.bestStreak);
    }

    static final class DailyStatus {
        private static final DailyStatus EMPTY = new DailyStatus("", false, false, 0, 0);

        final String dateId;
        final boolean inProgress;
        final boolean completed;
        final int currentStreak;
        final int bestStreak;

        DailyStatus(String dateId, boolean inProgress, boolean completed,
                int currentStreak, int bestStreak) {
            this.dateId = dateId == null ? "" : dateId;
            this.inProgress = inProgress;
            this.completed = completed;
            this.currentStreak = Math.max(0, currentStreak);
            this.bestStreak = Math.max(0, bestStreak);
        }
    }

    /** Immutable summary used to render a resumable continuous session on Home. */
    static final class ContinuousStatus {
        private static final ContinuousStatus EMPTY = new ContinuousStatus(
                false, 0, 0, 4, PuzzleDifficulty.CLASSIC);

        final boolean active;
        final int currentPuzzle;
        final int targetPuzzles;
        final int size;
        final PuzzleDifficulty difficulty;

        ContinuousStatus(boolean active, int currentPuzzle, int targetPuzzles,
                int size, PuzzleDifficulty difficulty) {
            this.active = active;
            this.currentPuzzle = currentPuzzle;
            this.targetPuzzles = targetPuzzles;
            this.size = size;
            this.difficulty = difficulty == null
                    ? PuzzleDifficulty.CLASSIC : difficulty;
        }
    }

    interface HomeActions {
        void onDailyChallenge();

        void onContinuousChallenge();

        void onContinue();

        void onPlay();

        void onBeginnerGuide();

        void onPracticeTutorial();

        void onHowToPlay();

        void onFavorites();

        void onTrends();

        void onSettings();

        void onRecords();
    }
}
