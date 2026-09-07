package com.klotski.ui;

/**
 * Small lifecycle guard for the modal Desktop Preferences editor. A restore
 * replaces the personal-data generation, so controls created before that
 * boundary must not write their stale values afterward.
 */
final class DesktopPreferencesEditorGuard {
    private long generation;

    long openEditor() {
        return generation;
    }

    void markPersonalDataRestored() {
        generation++;
    }

    boolean mayCommit(long editorGeneration) {
        return editorGeneration == generation;
    }
}
