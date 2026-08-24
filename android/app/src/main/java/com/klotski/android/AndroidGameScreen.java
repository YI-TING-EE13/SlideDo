package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Builder for the Android in-game board screen.
 */
final class AndroidGameScreen {
    private final Activity activity;
    private final AndroidUi ui;
    private final List<Button> commandButtons;

    AndroidGameScreen(Activity activity, AndroidUi ui, List<Button> commandButtons) {
        this.activity = activity;
        this.ui = ui;
        this.commandButtons = commandButtons;
    }

    GameViews build(KlotskiView boardView, GameActions actions) {
        LinearLayout root = createRoot();
        root.setId(R.id.game_root);

        LinearLayout topBar = addTopBar(root);
        Button homeButton = ui.createButton(activity.getString(R.string.nav_home),
                R.drawable.ic_action_home, COLOR_PANEL);
        configureCompactNavigationButton(homeButton);
        homeButton.setId(R.id.game_home_button);
        homeButton.setContentDescription(activity.getString(R.string.accessibility_game_home));
        homeButton.setOnClickListener(v -> actions.onHome());
        commandButtons.add(homeButton);
        topBar.addView(homeButton, ui.fixedButtonParams(78));

        TextView titleText = ui.createText("", 20, Color.WHITE, Typeface.BOLD);
        titleText.setId(R.id.game_title_text);
        titleText.setGravity(Gravity.CENTER);
        topBar.addView(titleText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button menuButton = ui.createButton(activity.getString(R.string.game_menu),
                R.drawable.ic_action_menu, COLOR_PANEL);
        configureCompactNavigationButton(menuButton);
        menuButton.setId(R.id.game_menu_button);
        menuButton.setContentDescription(activity.getString(R.string.accessibility_game_menu));
        menuButton.setOnClickListener(v -> actions.onMenu());
        commandButtons.add(menuButton);
        topBar.addView(menuButton, ui.fixedButtonParams(78));

        TextView statusText = ui.createText("", 14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        statusText.setId(R.id.game_status_text);
        statusText.setGravity(Gravity.CENTER);
        statusText.setSingleLine(false);
        statusText.setLineSpacing(0, 1.08f);
        statusText.setPadding(ui.dp(12), ui.dp(9), ui.dp(12), ui.dp(9));
        statusText.setBackground(ui.makePanelBackground(COLOR_PANEL));
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams statusParams = ui.fullWidthParams();
        statusParams.setMargins(0, ui.dp(10), 0, ui.dp(8));
        root.addView(statusText, statusParams);

        ViewParentRemover.removeFromParent(boardView);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout bottomActions = new LinearLayout(activity);
        bottomActions.setGravity(Gravity.CENTER);
        bottomActions.setPadding(0, ui.dp(10), 0, 0);
        boolean landscape = activity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        bottomActions.setOrientation(landscape
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.addView(bottomActions, ui.fullWidthParams());

        LinearLayout historyActions = landscape
                ? bottomActions : createActionRow(bottomActions, 0);
        Button undoButton = ui.addGameButton(historyActions, R.string.button_undo,
                R.drawable.ic_action_undo, v -> actions.onUndo());
        undoButton.setId(R.id.game_undo_button);
        undoButton.setContentDescription(activity.getString(R.string.accessibility_game_undo));

        Button redoButton = ui.addGameButton(historyActions, R.string.button_redo,
                R.drawable.ic_action_redo, v -> actions.onRedo());
        redoButton.setId(R.id.game_redo_button);
        redoButton.setContentDescription(activity.getString(R.string.accessibility_game_redo));

        LinearLayout toolActions = landscape
                ? bottomActions : createActionRow(bottomActions, ui.dp(8));
        Button restartButton = ui.addGameButton(toolActions, R.string.button_restart,
                R.drawable.ic_action_restart, v -> actions.onRestart());
        restartButton.setId(R.id.game_restart_button);
        restartButton.setContentDescription(activity.getString(R.string.accessibility_game_restart));

        Button assistButton = ui.addGameButton(toolActions, R.string.game_assist,
                R.drawable.ic_action_assist, v -> actions.onAssist());
        assistButton.setId(R.id.game_assist_button);
        assistButton.setContentDescription(activity.getString(R.string.accessibility_game_assist));

        return new GameViews(root, titleText, statusText);
    }

    private void configureCompactNavigationButton(Button button) {
        button.setSingleLine(true);
        button.setTextSize(13);
        button.setPadding(ui.dp(8), 0, ui.dp(8), 0);
    }

    private LinearLayout createActionRow(LinearLayout parent, int topMargin) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = ui.fullWidthParams();
        params.setMargins(0, topMargin, 0, 0);
        parent.addView(row, params);
        return row;
    }

    private LinearLayout createRoot() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.resolveColor(AndroidUi.COLOR_BACKGROUND));
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

    static final class GameViews {
        final LinearLayout root;
        final TextView titleText;
        final TextView statusText;

        GameViews(LinearLayout root, TextView titleText, TextView statusText) {
            this.root = root;
            this.titleText = titleText;
            this.statusText = statusText;
        }
    }

    interface GameActions {
        void onHome();

        void onMenu();

        void onUndo();

        void onRedo();

        void onRestart();

        void onAssist();
    }
}
