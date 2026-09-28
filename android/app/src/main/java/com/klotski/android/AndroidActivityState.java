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
    private static final String STATE_ACTIVE_DAILY_DATE = "active_daily_date";
    private static final String STATE_DAILY_CALENDAR_MONTH = "daily_calendar_month";
    private static final String STATE_RESULT_DAILY_DATE = "result_daily_date";
    private static final String STATE_ACTIVE_FAVORITE_ID = "active_favorite_id";
    private static final String STATE_RESULT_FAVORITE_ID = "result_favorite_id";
    private static final String STATE_RESULT_DAILY_PROGRESS_SAVED = "result_daily_progress_saved";
    private static final String STATE_RESULT_COMPLETION_RECORDED = "result_completion_recorded";
    private static final String STATE_RESULT_RELAY_CODE = "result_relay_code";
    private static final String STATE_PENDING_WIN_AVAILABLE = "pending_win_available";
    private static final String STATE_PENDING_WIN_SIZE = "pending_win_size";
    private static final String STATE_PENDING_WIN_DIFFICULTY = "pending_win_difficulty";
    private static final String STATE_PENDING_WIN_MOVES = "pending_win_moves";
    private static final String STATE_PENDING_WIN_TIME = "pending_win_time";
    private static final String STATE_PENDING_WIN_ASSISTED = "pending_win_assisted";
    private static final String STATE_PENDING_WIN_DAILY_DATE = "pending_win_daily_date";
    private static final String STATE_PENDING_WIN_FAVORITE_ID = "pending_win_favorite_id";
    private static final String STATE_PENDING_WIN_RELAY_CODE = "pending_win_relay_code";

    private AndroidActivityState() {
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen, boolean gameStarted,
            int onboardingPage, int tutorialStep, GameResult currentResult) {
        save(outState, currentScreen, infoReturnScreen, gameStarted, onboardingPage, tutorialStep,
                currentResult, null, null, null);
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen, boolean gameStarted,
            int onboardingPage, int tutorialStep, GameResult currentResult, String activeDailyDateId) {
        save(outState, currentScreen, infoReturnScreen, gameStarted, onboardingPage, tutorialStep,
                currentResult, activeDailyDateId, null, null);
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen, boolean gameStarted,
            int onboardingPage, int tutorialStep, GameResult currentResult, String activeDailyDateId,
            String dailyCalendarMonthId) {
        save(outState, currentScreen, infoReturnScreen, gameStarted, onboardingPage, tutorialStep,
                currentResult, activeDailyDateId, null, dailyCalendarMonthId);
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen,
            boolean gameStarted, int onboardingPage, int tutorialStep, GameResult currentResult,
            String activeDailyDateId, String activeFavoriteId, String dailyCalendarMonthId) {
        save(outState, currentScreen, infoReturnScreen, gameStarted, onboardingPage,
                tutorialStep, currentResult, activeDailyDateId, activeFavoriteId,
                dailyCalendarMonthId, null);
    }

    static void save(Bundle outState, Screen currentScreen, Screen infoReturnScreen,
            boolean gameStarted, int onboardingPage, int tutorialStep, GameResult currentResult,
            String activeDailyDateId, String activeFavoriteId, String dailyCalendarMonthId,
            PendingWin pendingWin) {
        outState.putString(STATE_SCREEN, currentScreen.name());
        outState.putString(STATE_INFO_RETURN_SCREEN, infoReturnScreen.name());
        outState.putBoolean(STATE_GAME_STARTED, gameStarted);
        outState.putInt(STATE_ONBOARDING_PAGE, onboardingPage);
        outState.putInt(STATE_TUTORIAL_STEP, tutorialStep);
        outState.putString(STATE_ACTIVE_DAILY_DATE, activeDailyDateId);
        outState.putString(STATE_ACTIVE_FAVORITE_ID, activeFavoriteId);
        outState.putString(STATE_DAILY_CALENDAR_MONTH, dailyCalendarMonthId);
        saveResultState(outState, currentResult);
        savePendingWin(outState, pendingWin);
    }

    static Snapshot restore(Bundle savedInstanceState, int fallbackTutorialStep) {
        return new Snapshot(
                readScreen(savedInstanceState, STATE_SCREEN, Screen.HOME),
                readScreen(savedInstanceState, STATE_INFO_RETURN_SCREEN, Screen.HOME),
                savedInstanceState.getBoolean(STATE_GAME_STARTED, false),
                savedInstanceState.getInt(STATE_ONBOARDING_PAGE, 0),
                savedInstanceState.getInt(STATE_TUTORIAL_STEP, fallbackTutorialStep),
                restoreResultState(savedInstanceState),
                savedInstanceState.getString(STATE_ACTIVE_DAILY_DATE),
                savedInstanceState.getString(STATE_ACTIVE_FAVORITE_ID),
                savedInstanceState.getString(STATE_DAILY_CALENDAR_MONTH),
                restorePendingWin(savedInstanceState));
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
        outState.putString(STATE_RESULT_DAILY_DATE, currentResult.dailyDateId);
        outState.putString(STATE_RESULT_FAVORITE_ID, currentResult.favoriteId);
        outState.putBoolean(STATE_RESULT_DAILY_PROGRESS_SAVED,
                currentResult.dailyProgressSaved);
        outState.putBoolean(STATE_RESULT_COMPLETION_RECORDED,
                currentResult.completionRecorded);
        outState.putString(STATE_RESULT_RELAY_CODE, currentResult.relayCode);
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
                previousBest,
                savedInstanceState.getString(STATE_RESULT_DAILY_DATE),
                savedInstanceState.getString(STATE_RESULT_FAVORITE_ID),
                savedInstanceState.getBoolean(STATE_RESULT_DAILY_PROGRESS_SAVED, true),
                savedInstanceState.getBoolean(STATE_RESULT_COMPLETION_RECORDED, true),
                savedInstanceState.getString(STATE_RESULT_RELAY_CODE));
    }

    private static void savePendingWin(Bundle outState, PendingWin pendingWin) {
        outState.putBoolean(STATE_PENDING_WIN_AVAILABLE, pendingWin != null);
        if (pendingWin == null) {
            return;
        }
        outState.putInt(STATE_PENDING_WIN_SIZE, pendingWin.size);
        outState.putString(STATE_PENDING_WIN_DIFFICULTY, pendingWin.difficulty.getId());
        outState.putInt(STATE_PENDING_WIN_MOVES, pendingWin.moves);
        outState.putLong(STATE_PENDING_WIN_TIME, pendingWin.timeMs);
        outState.putBoolean(STATE_PENDING_WIN_ASSISTED, pendingWin.assisted);
        outState.putString(STATE_PENDING_WIN_DAILY_DATE, pendingWin.dailyDateId);
        outState.putString(STATE_PENDING_WIN_FAVORITE_ID, pendingWin.favoriteId);
        outState.putString(STATE_PENDING_WIN_RELAY_CODE, pendingWin.relayCode);
    }

    private static PendingWin restorePendingWin(Bundle savedInstanceState) {
        if (!savedInstanceState.getBoolean(STATE_PENDING_WIN_AVAILABLE, false)) {
            return null;
        }
        int size = savedInstanceState.getInt(STATE_PENDING_WIN_SIZE, 0);
        int moves = savedInstanceState.getInt(STATE_PENDING_WIN_MOVES, -1);
        long timeMs = savedInstanceState.getLong(STATE_PENDING_WIN_TIME, -1L);
        if (size < 3 || size > 5 || moves < 0 || timeMs < 0) {
            return null;
        }
        return new PendingWin(size,
                PuzzleDifficulty.fromId(savedInstanceState.getString(
                        STATE_PENDING_WIN_DIFFICULTY)),
                moves, timeMs,
                savedInstanceState.getBoolean(STATE_PENDING_WIN_ASSISTED, false),
                savedInstanceState.getString(STATE_PENDING_WIN_DAILY_DATE),
                savedInstanceState.getString(STATE_PENDING_WIN_FAVORITE_ID),
                savedInstanceState.getString(STATE_PENDING_WIN_RELAY_CODE));
    }

    static final class Snapshot {
        final Screen screen;
        final Screen infoReturnScreen;
        final boolean gameStarted;
        final int onboardingPage;
        final int tutorialStep;
        final GameResult result;
        final String activeDailyDateId;
        final String activeFavoriteId;
        final String dailyCalendarMonthId;
        final PendingWin pendingWin;

        Snapshot(Screen screen, Screen infoReturnScreen, boolean gameStarted, int onboardingPage,
                int tutorialStep, GameResult result, String activeDailyDateId,
                String activeFavoriteId,
                String dailyCalendarMonthId, PendingWin pendingWin) {
            this.screen = screen;
            this.infoReturnScreen = infoReturnScreen;
            this.gameStarted = gameStarted;
            this.onboardingPage = onboardingPage;
            this.tutorialStep = tutorialStep;
            this.result = result;
            this.activeDailyDateId = activeDailyDateId;
            this.activeFavoriteId = activeFavoriteId;
            this.dailyCalendarMonthId = dailyCalendarMonthId;
            this.pendingWin = pendingWin;
        }
    }
}
