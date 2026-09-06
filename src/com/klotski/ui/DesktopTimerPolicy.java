package com.klotski.ui;

/**
 * Pure Desktop timer gate for active-play lifecycle decisions.
 * <p>
 * GameModel remains the owner of elapsed milliseconds. This policy only
 * determines whether the Desktop controller is allowed to resume that timer,
 * which makes Home, modal dialogs, solver work, and inactive windows
 * deterministic to test without wall-clock sleeps.
 * </p>
 */
final class DesktopTimerPolicy {
    private DesktopTimerPolicy() {
    }

    /**
     * Determines whether active gameplay may accumulate time.
     *
     * @param showingGame whether the game card is visible
     * @param windowActive whether the Swing window is active
     * @param modalDialogOpen whether a controller-owned modal dialog is open
     * @param solverRunning whether solver computation or result handling owns
     *                      the session
     * @param gameRunning whether the shared model is still an active puzzle
     * @return {@code true} only while the player can actively play
     */
    static boolean shouldRun(boolean showingGame, boolean windowActive,
            boolean modalDialogOpen, boolean solverRunning, boolean gameRunning) {
        return showingGame
                && windowActive
                && !modalDialogOpen
                && !solverRunning
                && gameRunning;
    }
}
