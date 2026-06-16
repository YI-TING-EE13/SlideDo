package com.klotski.android;

/**
 * Callback used by settings rows when a boolean preference changes.
 */
interface SettingChangeListener {
    void onChanged(boolean checked);
}
