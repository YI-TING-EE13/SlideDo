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
        addModeRow(screen.content, 3, R.string.mode_easy, R.string.mode_easy_detail, best3, actions);
        addModeRow(screen.content, 4, R.string.mode_classic, R.string.mode_classic_detail, best4, actions);
        addModeRow(screen.content, 5, R.string.mode_expert, R.string.mode_expert_detail, best5, actions);

        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home, COLOR_PANEL,
                v -> actions.onHome());
        homeButton.setId(R.id.mode_home_button);
        return screen;
    }

    private void addModeRow(LinearLayout parent, int size, int difficultyResId, int detailResId,
            String bestText, ModeActions actions) {
        LinearLayout row = new LinearLayout(activity);
        if (size == 3) {
            row.setId(R.id.mode_3_button);
        } else if (size == 4) {
            row.setId(R.id.mode_4_button);
        } else if (size == 5) {
            row.setId(R.id.mode_5_button);
        }
        row.setContentDescription(activity.getString(R.string.mode_card_title, size, size));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));
        row.setClickable(true);
        row.setOnClickListener(v -> actions.onModeSelected(size));

        TextView title = ui.createText(activity.getString(R.string.mode_card_title, size, size),
                22, Color.WHITE, Typeface.BOLD);
        TextView difficulty = ui.createText(activity.getString(difficultyResId), 15, COLOR_ACCENT, Typeface.BOLD);
        TextView detail = ui.createText(activity.getString(detailResId), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        TextView best = ui.createText(activity.getString(R.string.mode_best_label, bestText),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, ui.fullWidthParams());
        row.addView(difficulty, ui.fullWidthParams());
        LinearLayout.LayoutParams detailParams = ui.fullWidthParams();
        detailParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(detail, detailParams);
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    interface ModeActions {
        void onModeSelected(int size);

        void onHome();
    }
}
