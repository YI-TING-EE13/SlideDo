package com.klotski.ui;

import java.util.UUID;

/**
 * Guards the Desktop win callback so one completed run contributes one result.
 * A new run id is created for every new, replayed, restarted, or loaded puzzle.
 */
final class DesktopCompletionTracker {
    private String runId;
    private boolean claimed;

    DesktopCompletionTracker() {
        reset();
    }

    /**
     * Starts a fresh completion scope.
     *
     * @return the new persistence id
     */
    String reset() {
        runId = UUID.randomUUID().toString();
        claimed = false;
        return runId;
    }

    /**
     * Claims the current run for its first win callback.
     *
     * @return {@code true} only for the first claim
     */
    boolean claim() {
        if (claimed) {
            return false;
        }
        claimed = true;
        return true;
    }

    String runId() {
        return runId;
    }
}
