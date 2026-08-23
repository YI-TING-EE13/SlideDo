package com.klotski.android;

/**
 * Pure navigation decisions for Android back handling.
 */
final class AndroidNavigation {
    private AndroidNavigation() {
    }

    enum BackTarget {
        HOME,
        GAME,
        FINISH
    }

    static BackTarget backTarget(Screen currentScreen, Screen infoReturnScreen, boolean gameStarted) {
        if (currentScreen == Screen.HOME) {
            return BackTarget.FINISH;
        }
        if ((currentScreen == Screen.HOW_TO_PLAY || currentScreen == Screen.RECORDS
                || currentScreen == Screen.TRENDS
                || currentScreen == Screen.SETTINGS)
                && infoReturnScreen == Screen.GAME && gameStarted) {
            return BackTarget.GAME;
        }
        return BackTarget.HOME;
    }

    static boolean shouldSaveBeforeBack(Screen currentScreen) {
        return currentScreen == Screen.GAME || currentScreen == Screen.RESULTS;
    }
}
