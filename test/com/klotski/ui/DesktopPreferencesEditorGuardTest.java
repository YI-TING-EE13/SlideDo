package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopPreferencesEditorGuardTest {
    @Test
    void restoreInvalidatesOnlyEditorsCreatedBeforeTheRestore() {
        DesktopPreferencesEditorGuard guard = new DesktopPreferencesEditorGuard();
        long oldEditor = guard.openEditor();
        assertTrue(guard.mayCommit(oldEditor));

        guard.markPersonalDataRestored();
        assertFalse(guard.mayCommit(oldEditor));

        long freshEditor = guard.openEditor();
        assertTrue(guard.mayCommit(freshEditor));
    }
}
