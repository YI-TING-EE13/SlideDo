package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_DANGER_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Builder for the Android settings screen.
 */
final class AndroidSettingsScreen {
    private final Activity activity;
    private final AndroidUi ui;

    AndroidSettingsScreen(Activity activity, AndroidUi ui) {
        this.activity = activity;
        this.ui = ui;
    }

    ScreenLayout build(boolean hapticEnabled, boolean soundEnabled,
            boolean reducedMotionEnabled, String languageTag,
            AndroidVisualTheme visualTheme, SettingsActions actions) {
        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.settings_root);
        ui.addScreenHeader(screen.content,
                activity.getString(R.string.settings_title),
                activity.getString(R.string.settings_subtitle));
        ui.addSectionLabel(screen.content, R.string.settings_section_language);
        String languageName = activity.getString(AndroidAppLocale.getDisplayNameResId(languageTag));
        Button languageButton = ui.createButton(
                activity.getString(R.string.settings_language_button, languageName), COLOR_PANEL);
        languageButton.setId(R.id.settings_language_button);
        languageButton.setContentDescription(activity.getString(
                R.string.settings_language_accessibility, languageName));
        languageButton.setOnClickListener(v -> actions.onLanguageRequested());
        LinearLayout.LayoutParams languageParams = ui.fullWidthParams();
        languageParams.setMargins(0, 0, 0, ui.dp(10));
        screen.content.addView(languageButton, languageParams);

        ui.addSectionLabel(screen.content, R.string.settings_section_preferences);
        String themeName = activity.getString(visualTheme == AndroidVisualTheme.OCEAN
                ? R.string.theme_ocean : R.string.theme_midnight);
        Button themeButton = ui.createButton(
                activity.getString(R.string.settings_theme_button, themeName), COLOR_PANEL);
        themeButton.setId(R.id.settings_theme_button);
        themeButton.setContentDescription(activity.getString(
                R.string.settings_theme_accessibility, themeName));
        themeButton.setOnClickListener(v -> actions.onThemeRequested());
        LinearLayout.LayoutParams themeParams = ui.fullWidthParams();
        themeParams.setMargins(0, 0, 0, ui.dp(10));
        screen.content.addView(themeButton, themeParams);

        addSettingsSwitch(screen.content, R.id.settings_haptic_switch, R.string.settings_haptic_title,
                R.string.settings_haptic_body, hapticEnabled, actions::onHapticChanged);
        addSettingsSwitch(screen.content, R.id.settings_sound_switch, R.string.settings_sound_title,
                R.string.settings_sound_body, soundEnabled, actions::onSoundChanged);
        addSettingsSwitch(screen.content, R.id.settings_reduced_motion_switch,
                R.string.settings_reduced_motion_title, R.string.settings_reduced_motion_body,
                reducedMotionEnabled, actions::onReducedMotionChanged);

        ui.addSectionLabel(screen.content, R.string.settings_section_local_data);
        Button resetSaveButton = ui.addWideButton(screen.content, R.string.settings_reset_save, COLOR_DANGER_PANEL,
                v -> actions.onResetSave());
        resetSaveButton.setId(R.id.settings_reset_save_button);
        Button resetRecordsButton = ui.addWideButton(screen.content, R.string.settings_reset_records, COLOR_DANGER_PANEL,
                v -> actions.onResetRecords());
        resetRecordsButton.setId(R.id.settings_reset_records_button);
        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL,
                v -> actions.onBack());
        backButton.setId(R.id.settings_back_button);
        return screen;
    }

    private void addSettingsSwitch(LinearLayout parent, int switchId, int titleResId, int bodyResId,
            boolean checked, SettingChangeListener listener) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = ui.createText(activity.getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = ui.createText(activity.getString(bodyResId), 14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);
        copy.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bodyParams = ui.fullWidthParams();
        bodyParams.setMargins(0, ui.dp(5), 0, 0);
        copy.addView(body, bodyParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(activity);
        toggle.setId(switchId);
        toggle.setChecked(checked);
        toggle.setContentDescription(activity.getString(R.string.accessibility_settings_switch,
                activity.getString(titleResId), activity.getString(bodyResId)));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        switchParams.setMargins(ui.dp(16), 0, 0, 0);
        row.addView(toggle, switchParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    interface SettingsActions {
        void onLanguageRequested();

        void onThemeRequested();

        void onHapticChanged(boolean checked);

        void onSoundChanged(boolean checked);

        void onReducedMotionChanged(boolean checked);

        void onResetSave();

        void onResetRecords();

        void onBack();
    }
}
