package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the Android mode-selection screen.
 */
final class AndroidModeSelectScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidModeSelectScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(String best3, String best4, String best5, ModeActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.mode_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.mode_title),
                activity.getString(R.string.mode_subtitle));
        addModeRow(screen.content, 3, R.string.mode_easy, R.string.mode_easy_detail,
                R.string.mode_easy_session, best3, actions);
        addModeRow(screen.content, 4, R.string.mode_classic, R.string.mode_classic_detail,
                R.string.mode_classic_session, best4, actions);
        addModeRow(screen.content, 5, R.string.mode_expert, R.string.mode_expert_detail,
                R.string.mode_expert_session, best5, actions);

        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home, COLOR_PANEL,
                v -> actions.onHome());
        homeButton.setId(R.id.mode_home_button);
        return screen;
    }

    private void addModeRow(LinearLayout parent, int size, int difficultyResId, int detailResId,
            int sessionResId, String bestText, ModeActions actions) {
        LinearLayout row = new LinearLayout(activity);
        ModeIds ids = getModeIds(size);
        row.setId(ids.buttonId);
        String difficultyText = activity.getString(difficultyResId);
        String detailText = activity.getString(detailResId);
        String sessionText = activity.getString(sessionResId);
        row.setContentDescription(activity.getString(R.string.mode_card_accessibility,
                size, size, difficultyText, detailText, sessionText, bestText));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));
        row.setClickable(true);
        row.setOnClickListener(v -> actions.onModeSelected(size));

        TextView title = ui.createText(activity.getString(R.string.mode_card_title, size, size),
                22, Color.WHITE, Typeface.BOLD);
        title.setId(ids.titleId);
        TextView difficulty = ui.createText(difficultyText, 15, COLOR_ACCENT, Typeface.BOLD);
        difficulty.setId(ids.difficultyId);
        TextView detail = ui.createText(detailText, 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        detail.setId(ids.detailId);
        TextView session = ui.createText(sessionText, 14, COLOR_ACCENT, Typeface.BOLD);
        session.setId(ids.sessionId);
        TextView best = ui.createText(activity.getString(R.string.mode_best_label, bestText),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        best.setId(ids.bestId);

        row.addView(title, ui.fullWidthParams());
        row.addView(difficulty, ui.fullWidthParams());
        LinearLayout.LayoutParams detailParams = ui.fullWidthParams();
        detailParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(detail, detailParams);
        LinearLayout.LayoutParams sessionParams = ui.fullWidthParams();
        sessionParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(session, sessionParams);
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    private ModeIds getModeIds(int size) {
        if (size == 3) {
            return new ModeIds(R.id.mode_3_button, R.id.mode_3_title_text,
                    R.id.mode_3_difficulty_text, R.id.mode_3_detail_text,
                    R.id.mode_3_session_text, R.id.mode_3_best_text);
        }
        if (size == 4) {
            return new ModeIds(R.id.mode_4_button, R.id.mode_4_title_text,
                    R.id.mode_4_difficulty_text, R.id.mode_4_detail_text,
                    R.id.mode_4_session_text, R.id.mode_4_best_text);
        }
        return new ModeIds(R.id.mode_5_button, R.id.mode_5_title_text,
                R.id.mode_5_difficulty_text, R.id.mode_5_detail_text,
                R.id.mode_5_session_text, R.id.mode_5_best_text);
    }

    private static final class ModeIds {
        final int buttonId;
        final int titleId;
        final int difficultyId;
        final int detailId;
        final int sessionId;
        final int bestId;

        ModeIds(int buttonId, int titleId, int difficultyId, int detailId, int sessionId, int bestId) {
            this.buttonId = buttonId;
            this.titleId = titleId;
            this.difficultyId = difficultyId;
            this.detailId = detailId;
            this.sessionId = sessionId;
            this.bestId = bestId;
        }
    }

    interface ModeActions {
        void onModeSelected(int size);

        void onHome();
    }
}
