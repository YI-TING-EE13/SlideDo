package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_DANGER_PANEL;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builder for the owner-only local favorite puzzle library.
 */
final class AndroidFavoritesScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidFavoritesScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(AndroidGameStore.FavoritePuzzle[] favorites, FavoriteActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.favorites_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.favorites_title),
                activity.getString(R.string.favorites_subtitle));

        AndroidGameStore.FavoritePuzzle[] entries = favorites == null
                ? new AndroidGameStore.FavoritePuzzle[0] : favorites;
        if (entries.length == 0) {
            TextView empty = ui.createText(activity.getString(R.string.favorites_empty),
                    16, COLOR_MUTED_TEXT, Typeface.NORMAL);
            empty.setId(R.id.favorites_empty_text);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(ui.dp(18), ui.dp(20), ui.dp(18), ui.dp(20));
            empty.setBackground(ui.makePanelBackground(COLOR_PANEL));
            LinearLayout.LayoutParams emptyParams = ui.fullWidthParams();
            emptyParams.setMargins(0, 0, 0, ui.dp(16));
            screen.content.addView(empty, emptyParams);
        } else {
            for (AndroidGameStore.FavoritePuzzle favorite : entries) {
                addFavoriteCard(screen.content, favorite, actions);
            }
        }

        Button back = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL,
                view -> actions.onBack());
        back.setId(R.id.favorites_back_button);
        return screen;
    }

    private void addFavoriteCard(LinearLayout parent, AndroidGameStore.FavoritePuzzle favorite,
            FavoriteActions actions) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        card.setBackground(ui.makePanelBackground(COLOR_PANEL));

        TextView label = ui.createText(favorite.label, 20, Color.WHITE, Typeface.BOLD);
        TextView detail = ui.createText(activity.getString(R.string.favorites_detail,
                favorite.size, favorite.size, difficultyName(favorite.difficulty)),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        card.addView(label, ui.fullWidthParams());
        LinearLayout.LayoutParams detailParams = ui.fullWidthParams();
        detailParams.setMargins(0, ui.dp(5), 0, ui.dp(10));
        card.addView(detail, detailParams);

        LinearLayout actionsRow = new LinearLayout(activity);
        Button replay = ui.createButton(activity.getString(R.string.favorites_replay), COLOR_PRIMARY);
        replay.setContentDescription(activity.getString(
                R.string.favorites_replay_accessibility, favorite.label));
        replay.setOnClickListener(view -> actions.onReplay(favorite));
        actionsRow.addView(replay, rowButtonParams());

        Button rename = ui.createButton(activity.getString(R.string.favorites_rename), COLOR_PANEL_LIGHT);
        rename.setContentDescription(activity.getString(
                R.string.favorites_rename_accessibility, favorite.label));
        rename.setOnClickListener(view -> actions.onRename(favorite));
        actionsRow.addView(rename, rowButtonParams());

        Button remove = ui.createButton(activity.getString(R.string.favorites_remove), COLOR_DANGER_PANEL);
        remove.setContentDescription(activity.getString(
                R.string.favorites_remove_accessibility, favorite.label));
        remove.setOnClickListener(view -> actions.onRemove(favorite));
        actionsRow.addView(remove, rowButtonParams());
        card.addView(actionsRow, ui.fullWidthParams());

        LinearLayout.LayoutParams cardParams = ui.fullWidthParams();
        cardParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(card, cardParams);
    }

    private LinearLayout.LayoutParams rowButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ui.dp(48), 1f);
        params.setMargins(ui.dp(3), 0, ui.dp(3), 0);
        return params;
    }

    private String difficultyName(com.klotski.core.PuzzleDifficulty difficulty) {
        return activity.getString(switch (difficulty) {
            case RELAXED -> R.string.difficulty_relaxed;
            case CLASSIC -> R.string.difficulty_classic;
            case CHALLENGE -> R.string.difficulty_challenge;
        });
    }

    interface FavoriteActions {
        void onReplay(AndroidGameStore.FavoritePuzzle favorite);

        void onRename(AndroidGameStore.FavoritePuzzle favorite);

        void onRemove(AndroidGameStore.FavoritePuzzle favorite);

        void onBack();
    }
}
