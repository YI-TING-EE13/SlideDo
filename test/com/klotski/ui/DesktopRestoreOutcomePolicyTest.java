package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class DesktopRestoreOutcomePolicyTest {
    @Test
    void ordinaryFailureAndSuccessfulBoundaryRemainNormal() {
        assertEquals(DesktopRestoreOutcomePolicy.Outcome.NORMAL_SUCCESS,
                DesktopRestoreOutcomePolicy.classify(null));
        assertEquals(DesktopRestoreOutcomePolicy.Outcome.NORMAL_SUCCESS,
                DesktopRestoreOutcomePolicy.classify(new IOException("ordinary")));
        assertTrue(DesktopRestoreOutcomePolicy.shouldReconcile(
                DesktopRestoreOutcomePolicy.Outcome.NORMAL_SUCCESS));
        assertFalse(DesktopRestoreOutcomePolicy.shouldWarn(
                DesktopRestoreOutcomePolicy.Outcome.NORMAL_SUCCESS));
        assertFalse(DesktopRestoreOutcomePolicy.shouldLockPersistence(
                DesktopRestoreOutcomePolicy.Outcome.NORMAL_SUCCESS));
    }

    @Test
    void cleanupWarningIsASuccessfulRestoreThatRequiresAWarning() {
        DesktopRestoreOutcomePolicy.Outcome outcome =
                DesktopRestoreOutcomePolicy.Outcome.CLEANUP_WARNING_SUCCESS;
        assertTrue(DesktopRestoreOutcomePolicy.shouldReconcile(outcome));
        assertTrue(DesktopRestoreOutcomePolicy.shouldWarn(outcome));
        assertFalse(DesktopRestoreOutcomePolicy.shouldLockPersistence(outcome));
        assertTrue(DesktopRestoreOutcomePolicy.allowsPersistence(false));
        assertTrue(DesktopRestoreOutcomePolicy.allowsGameplay(false));
    }

    @Test
    void recoveryRequiredRejectsAllPersistenceAndGameplay() {
        DesktopRestoreOutcomePolicy.Outcome outcome =
                DesktopRestoreOutcomePolicy.Outcome.RECOVERY_REQUIRED;
        assertFalse(DesktopRestoreOutcomePolicy.shouldReconcile(outcome));
        assertFalse(DesktopRestoreOutcomePolicy.shouldWarn(outcome));
        assertTrue(DesktopRestoreOutcomePolicy.shouldLockPersistence(outcome));
        assertFalse(DesktopRestoreOutcomePolicy.allowsPersistence(true));
        assertFalse(DesktopRestoreOutcomePolicy.allowsGameplay(true));
    }

    @Test
    void preferencesGuardTokenIsInvalidatedAtEveryRestoreBoundary() {
        DesktopPreferencesEditorGuard guard = new DesktopPreferencesEditorGuard();
        long beforeSuccess = guard.openEditor();
        guard.markPersonalDataRestored();
        assertFalse(guard.mayCommit(beforeSuccess));

        long beforeRecovery = guard.openEditor();
        guard.markPersonalDataRestored();
        assertFalse(guard.mayCommit(beforeRecovery));
        assertTrue(guard.mayCommit(guard.openEditor()));
    }
}
