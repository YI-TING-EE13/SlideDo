package com.klotski.android;

import android.os.Bundle;

import com.klotski.core.PuzzleDifficulty;

/**
 * Bundle serialization for Activity-level navigation and result state.
 */
final class AndroidActivityState {
    private static final String STATE_SCREEN = "screen";
    private static final String STATE_INFO_RETURN_SCREEN = "info_return_screen";
    private static final String STATE_GAME_STARTED = "game_started";
    private static final String STATE_ONBOARDING_PAGE = "onboarding_page";
    private static final String STATE_TUTORIAL_STEP = "tutorial_step";
    private static final String STATE_RESULT_AVAILABLE = "result_available";
    private static final String STATE_RESULT_SIZE = "result_size";
    private static final String STATE_RESULT_DIFFICULTY = "result_difficulty";
    private static final String STATE_RESULT_MOVES = "result_moves";
    private static final String STATE_RESULT_TIME = "result_time";
    private static final String STATE_RESULT_ASSISTED = "result_assisted";
    private static final String STATE_RESULT_NEW_BEST = "result_new_best";
    private static final String STATE_RESULT_PREVIOUS_BEST_MOVES = "result_previous_best_moves";
    private static final String STATE_RESULT_PREVIOUS_BEST_TIME = "result_previous_best_time";

    private AndroidActivityState() {
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen, boolean gameStarted,
            int onboardingPage, int tutorialStep, GameResult currentResult) {
        outState.putString(STATE_SCREEN, currentScreen.name());
        outState.putString(STATE_INFO_RETURN_SCREEN, infoReturnScreen.name());
        outState.putBoolean(STATE_GAME_STARTED, gameStarted);
        outState.putInt(STATE_ONBOARDING_PAGE, onboardingPage);
        outState.putInt(STATE_TUTORIAL_STEP, tutorialStep);
        saveResultState(outState, currentResult);
    }

    static Snapshot restore(Bundle savedInstanceState, int fallbackTutorialStep) {
        return new Snapshot(
                readScreen(savedInstanceState, STATE_SCREEN, Screen.HOME),
                readScreen(savedInstanceState, STATE_INFO_RETURN_SCREEN, Screen.HOME),
                savedInstanceState.getBoolean(STATE_GAME_STARTED, false),
                savedInstanceState.getInt(STATE_ONBOARDING_PAGE, 0),
                savedInstanceState.getInt(STATE_TUTORIAL_STEP, fallbackTutorialStep),
                restoreResultState(savedInstanceState));
    }

    private static Screen readScreen(Bundle bundle, String key, Screen fallback) {
        String value = bundle.getString(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Screen.valueOf(value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static void saveResultState(Bundle outState, GameResult currentResult) {
        if (currentResult == null) {
            outState.putBoolean(STATE_RESULT_AVAILABLE, false);
            return;
        }
        outState.putBoolean(STATE_RESULT_AVAILABLE, true);
        outState.putInt(STATE_RESULT_SIZE, currentResult.size);
        outState.putString(STATE_RESULT_DIFFICULTY, currentResult.difficulty.getId());
        outState.putInt(STATE_RESULT_MOVES, currentResult.moves);
        outState.putLong(STATE_RESULT_TIME, currentResult.timeMs);
        outState.putBoolean(STATE_RESULT_ASSISTED, currentResult.assisted);
        outState.putBoolean(STATE_RESULT_NEW_BEST, currentResult.newBest);
        if (currentResult.previousBest == null) {
            outState.putInt(STATE_RESULT_PREVIOUS_BEST_MOVES, -1);
            outState.putLong(STATE_RESULT_PREVIOUS_BEST_TIME, -1);
        } else {
            outState.putInt(STATE_RESULT_PREVIOUS_BEST_MOVES, currentResult.previousBest.moves);
            outState.putLong(STATE_RESULT_PREVIOUS_BEST_TIME, currentResult.previousBest.timeMs);
        }
    }

    private static GameResult restoreResultState(Bundle savedInstanceState) {
        if (!savedInstanceState.getBoolean(STATE_RESULT_AVAILABLE, false)) {
            return null;
        }
        int previousMoves = savedInstanceState.getInt(STATE_RESULT_PREVIOUS_BEST_MOVES, -1);
        long previousTime = savedInstanceState.getLong(STATE_RESULT_PREVIOUS_BEST_TIME, -1);
        AndroidGameStore.Best previousBest = previousMoves < 0 || previousTime < 0
                ? null
                : new AndroidGameStore.Best(previousMoves, previousTime);
        return new GameResult(
                savedInstanceState.getInt(STATE_RESULT_SIZE, 4),
                PuzzleDifficulty.fromId(savedInstanceState.getString(STATE_RESULT_DIFFICULTY)),
                savedInstanceState.getInt(STATE_RESULT_MOVES, 0),
                savedInstanceState.getLong(STATE_RESULT_TIME, 0),
                savedInstanceState.getBoolean(STATE_RESULT_ASSISTED, false),
                savedInstanceState.getBoolean(STATE_RESULT_NEW_BEST, false),
                previousBest);
    }

    static final class Snapshot {
        final Screen screen;
        final Screen infoReturnScreen;
        final boolean gameStarted;
        final int onboardingPage;
        final int tutorialStep;
        final GameResult result;

        Snapshot(Screen screen, Screen infoReturnScreen, boolean gameStarted, int onboardingPage,
                int tutorialStep, GameResult result) {
            this.screen = screen;
            this.infoReturnScreen = infoReturnScreen;
            this.gameStarted = gameStarted;
            this.onboardingPage = onboardingPage;
            this.tutorialStep = tutorialStep;
            this.result = result;
        }
    }
}
