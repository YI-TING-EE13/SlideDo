package com.klotski.ui;

import com.klotski.core.DesktopPersonalDataArchive;

/**
 * Pure controller policy for the three Desktop personal-data restore outcomes.
 * The archive service owns transaction durability; this policy owns whether
 * the Swing shell may reconcile and persist after that transaction returns.
 */
final class DesktopRestoreOutcomePolicy {
    enum Outcome {
        /** Target state is valid and transaction cleanup completed. */
        NORMAL_SUCCESS,
        /** Target state is valid, but retained transaction cleanup needs review. */
        CLEANUP_WARNING_SUCCESS,
        /** Target state cannot be trusted and retained recovery material is authoritative. */
        RECOVERY_REQUIRED
    }

    private DesktopRestoreOutcomePolicy() {
    }

    /** Classifies the typed archive result without inspecting exception messages. */
    static Outcome classify(Throwable failure) {
        if (failure instanceof DesktopPersonalDataArchive.RestoreRecoveryRequiredException) {
            return Outcome.RECOVERY_REQUIRED;
        }
        if (failure instanceof DesktopPersonalDataArchive.RestoreCleanupWarningException) {
            return Outcome.CLEANUP_WARNING_SUCCESS;
        }
        return Outcome.NORMAL_SUCCESS;
    }

    /** Returns whether the restored target may be loaded into the active shell. */
    static boolean shouldReconcile(Outcome outcome) {
        return outcome != Outcome.RECOVERY_REQUIRED;
    }

    /** Returns whether the UI should explain retained cleanup material. */
    static boolean shouldWarn(Outcome outcome) {
        return outcome == Outcome.CLEANUP_WARNING_SUCCESS;
    }

    /** Returns whether all controller persistence and gameplay must be locked. */
    static boolean shouldLockPersistence(Outcome outcome) {
        return outcome == Outcome.RECOVERY_REQUIRED;
    }

    /** Fail-safe gate shared by manual, lifecycle, and mode-specific writes. */
    static boolean allowsPersistence(boolean recoveryRequired) {
        return !recoveryRequired;
    }

    /** Recovery-required state cannot continue ordinary gameplay. */
    static boolean allowsGameplay(boolean recoveryRequired) {
        return !recoveryRequired;
    }
}
