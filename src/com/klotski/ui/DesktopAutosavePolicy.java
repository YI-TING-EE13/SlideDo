package com.klotski.ui;

/**
 * Pure gate for Desktop lifecycle autosaves.
 * <p>
 * Autosaves are deliberately limited to a visible, stable player session.
 * The controller must not serialize a transient animation or a solver-owned
 * session, because those states may be replaced before the next user action.
 * </p>
 */
final class DesktopAutosavePolicy {
    private DesktopAutosavePolicy() {
    }

    /**
     * Determines whether a lifecycle boundary may persist the current model.
     *
     * @param showingGame whether the Desktop game card is visible
     * @param boardBusy whether animation or queued playback is in progress
     * @param solverRunning whether a solver currently owns the session
     * @return {@code true} for a stable normal game session
     */
    static boolean shouldAutosave(boolean showingGame, boolean boardBusy,
            boolean solverRunning) {
        return showingGame && !boardBusy && !solverRunning;
    }
}
