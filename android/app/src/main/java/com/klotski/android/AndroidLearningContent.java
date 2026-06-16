package com.klotski.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builders for onboarding and How to Play learning content.
 */
final class AndroidLearningContent {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidLearningContent(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    void addInstruction(LinearLayout parent, int titleResId, int bodyResId) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        panel.setBackground(ui.makePanelBackground(AndroidUi.COLOR_PANEL));

        TextView title = ui.createText(activity.getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = ui.createText(activity.getString(bodyResId), 15, AndroidUi.COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);
        panel.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bodyParams = ui.fullWidthParams();
        bodyParams.setMargins(0, ui.dp(6), 0, 0);
        panel.addView(body, bodyParams);

        LinearLayout.LayoutParams panelParams = ui.fullWidthParams();
        panelParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(panel, panelParams);
    }

    void addLearningExample(LinearLayout parent, int viewId, int titleResId, int bodyResId,
            int[][] grid, int[] highlightedValues) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setId(viewId);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        panel.setBackground(ui.makePanelBackground(AndroidUi.COLOR_PANEL));

        TextView title = ui.createText(activity.getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = ui.createText(activity.getString(bodyResId), 15, AndroidUi.COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);
        panel.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bodyParams = ui.fullWidthParams();
        bodyParams.setMargins(0, ui.dp(6), 0, ui.dp(12));
        panel.addView(body, bodyParams);
        panel.addView(createLearningBoard(grid, highlightedValues), ui.centeredWrapParams());

        LinearLayout.LayoutParams panelParams = ui.fullWidthParams();
        panelParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(panel, panelParams);
    }

    private GridLayout createLearningBoard(int[][] grid, int[] highlightedValues) {
        GridLayout board = new GridLayout(activity);
        board.setColumnCount(3);
        board.setRowCount(3);
        board.setUseDefaultMargins(false);

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                int value = grid[row][col];
                boolean highlighted = containsValue(highlightedValues, value);
                TextView cell = createLearningCell(value, highlighted);
                GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams(
                        GridLayout.spec(row), GridLayout.spec(col));
                cellParams.width = ui.dp(48);
                cellParams.height = ui.dp(48);
                cellParams.setMargins(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3));
                board.addView(cell, cellParams);
            }
        }
        return board;
    }

    private TextView createLearningCell(int value, boolean highlighted) {
        TextView cell = ui.createText(value == 0
                        ? activity.getString(R.string.board_empty_cell_short)
                        : String.valueOf(value),
                value == 0 ? 10 : 18,
                highlighted ? Color.BLACK : (value == 0 ? AndroidUi.COLOR_ACCENT : Color.WHITE),
                Typeface.BOLD);
        cell.setGravity(android.view.Gravity.CENTER);
        if (value == 0) {
            cell.setBackground(ui.makeCellBackground(AndroidUi.COLOR_BACKGROUND, AndroidUi.COLOR_ACCENT));
            cell.setContentDescription(activity.getString(R.string.board_empty_cell_description));
        } else if (highlighted) {
            cell.setBackground(ui.makeCellBackground(AndroidUi.COLOR_ACCENT, Color.WHITE));
            cell.setContentDescription(activity.getString(R.string.board_highlighted_tile_description, value));
        } else {
            cell.setBackground(ui.makeCellBackground(AndroidUi.COLOR_PANEL_LIGHT,
                    Color.argb(80, 255, 255, 255)));
            cell.setContentDescription(activity.getString(R.string.board_tile_description, value));
        }
        return cell;
    }

    private static boolean containsValue(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
