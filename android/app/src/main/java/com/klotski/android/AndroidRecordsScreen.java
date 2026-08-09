package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_HIGHLIGHT;
import static com.klotski.android.AndroidUi.COLOR_POSITIVE_TEXT;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the Android records screen.
 */
final class AndroidRecordsScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidRecordsScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(String best3, String best4, String best5, RecordsActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.records_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.records_title),
                activity.getString(R.string.records_subtitle));
        addExplanation(screen.content);
        addRecordRow(screen.content, 3, R.string.mode_easy, best3);
        addRecordRow(screen.content, 4, R.string.mode_classic, best4);
        addRecordRow(screen.content, 5, R.string.mode_expert, best5);

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

    private void addRecordRow(LinearLayout parent, int size, int difficultyResId, String bestText) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));

        TextView title = ui.createText(activity.getString(R.string.records_row_title,
                size, size, activity.getString(difficultyResId)), 15, COLOR_MUTED_TEXT, Typeface.BOLD);
        TextView best = ui.createText(bestText, 20,
                activity.getString(R.string.records_empty).equals(bestText)
                        ? Color.WHITE : COLOR_POSITIVE_TEXT,
                Typeface.BOLD);

        row.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(6), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    interface RecordsActions {
        void onBack();
    }
}
