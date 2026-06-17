package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the interactive Android practice tutorial screen.
 */
final class AndroidTutorialScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidTutorialScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    TutorialViews build(KlotskiView boardView, TutorialActions actions) {
        LinearLayout root = createRoot();
        root.setId(R.id.tutorial_root);

        LinearLayout topBar = addTopBar(root);
        Button homeButton = ui.createButton(activity.getString(R.string.nav_home), COLOR_PANEL);
        homeButton.setId(R.id.tutorial_home_button);
        homeButton.setOnClickListener(v -> actions.onHome());
        topBar.addView(homeButton, ui.fixedButtonParams(88));

        TextView title = ui.createText(activity.getString(R.string.tutorial_title), 20, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button startButton = ui.createButton(activity.getString(R.string.onboarding_start_3), COLOR_PANEL);
        startButton.setId(R.id.tutorial_start_game_button);
        startButton.setOnClickListener(v -> actions.onStartGame());
        topBar.addView(startButton, ui.fixedButtonParams(104));

        LinearLayout lesson = new LinearLayout(activity);
        lesson.setOrientation(LinearLayout.VERTICAL);
        lesson.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        lesson.setBackground(ui.makePanelBackground(COLOR_PANEL));
        LinearLayout.LayoutParams lessonParams = ui.fullWidthParams();
        lessonParams.setMargins(0, ui.dp(10), 0, ui.dp(10));
        root.addView(lesson, lessonParams);

        TextView progressText = ui.createText("", 14, COLOR_ACCENT, Typeface.BOLD);
        progressText.setId(R.id.tutorial_progress_text);
        lesson.addView(progressText, ui.fullWidthParams());

        TextView instructionText = ui.createText("", 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        instructionText.setId(R.id.tutorial_instruction_text);
        instructionText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams instructionParams = ui.fullWidthParams();
        instructionParams.setMargins(0, ui.dp(6), 0, 0);
        lesson.addView(instructionText, instructionParams);

        TextView statusText = ui.createText("", 15, Color.WHITE, Typeface.BOLD);
        statusText.setId(R.id.tutorial_status_text);
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = ui.fullWidthParams();
        statusParams.setMargins(0, 0, 0, ui.dp(8));
        root.addView(statusText, statusParams);

        ViewParentRemover.removeFromParent(boardView);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout bottomActions = new LinearLayout(activity);
        bottomActions.setGravity(Gravity.CENTER);
        bottomActions.setPadding(0, ui.dp(10), 0, 0);
        bottomActions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(bottomActions, ui.fullWidthParams());

        Button restartButton = ui.createButton(activity.getString(R.string.tutorial_restart_lesson),
                COLOR_PANEL_LIGHT);
        restartButton.setOnClickListener(v -> actions.onRestartLesson());
        restartButton.setId(R.id.tutorial_restart_button);
        bottomActions.addView(restartButton, new LinearLayout.LayoutParams(0, ui.dp(48), 1f));

        return new TutorialViews(root, progressText, instructionText, statusText);
    }

    private LinearLayout createRoot() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AndroidUi.COLOR_BACKGROUND);
        root.setPadding(ui.dp(12), ui.systemBarHeight("status_bar_height") + ui.dp(12),
                ui.dp(12), ui.systemBarHeight("navigation_bar_height") + ui.dp(12));
        return root;
    }

    private LinearLayout addTopBar(LinearLayout root) {
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(topBar, ui.fullWidthParams());
        return topBar;
    }

    static final class TutorialViews {
        final LinearLayout root;
        final TextView progressText;
        final TextView instructionText;
        final TextView statusText;

        TutorialViews(LinearLayout root, TextView progressText, TextView instructionText, TextView statusText) {
            this.root = root;
            this.progressText = progressText;
            this.instructionText = instructionText;
            this.statusText = statusText;
        }
    }

    interface TutorialActions {
        void onHome();

        void onStartGame();

        void onRestartLesson();
    }
}
