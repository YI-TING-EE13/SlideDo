package com.klotski.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.GameObserver;
import com.klotski.core.IdaStarSolver;
import com.klotski.core.SaveManager;
import com.klotski.core.Solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Native Android entry point for SlideDo.
 * <p>
 * The activity owns the mobile app flow and wires the shared {@link GameModel}
 * to Android screens, local persistence, best-record tracking, solver actions,
 * and completion dialogs. Gameplay rules remain in the shared core so Android
 * behavior stays aligned with the desktop Swing reference.
 * </p>
 */
public class MainActivity extends Activity implements GameObserver {
    private static final String PREFS = "slidedo";
    private static final String KEY_SIZE = "size";
    private static final String KEY_GRID = "grid";
    private static final String KEY_INITIAL_GRID = "initial_grid";
    private static final String KEY_MOVES = "moves";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_LAST_SIZE = "last_size";
    private static final String KEY_BEST_PREFIX = "best_";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    private static final String KEY_REDUCED_MOTION = "reduced_motion";
    private static final String STATE_SCREEN = "screen";
    private static final String STATE_INFO_RETURN_SCREEN = "info_return_screen";
    private static final String STATE_GAME_STARTED = "game_started";
    private static final String STATE_ONBOARDING_PAGE = "onboarding_page";
    private static final String STATE_RESULT_AVAILABLE = "result_available";
    private static final String STATE_RESULT_SIZE = "result_size";
    private static final String STATE_RESULT_MOVES = "result_moves";
    private static final String STATE_RESULT_TIME = "result_time";
    private static final String STATE_RESULT_ASSISTED = "result_assisted";
    private static final String STATE_RESULT_NEW_BEST = "result_new_best";
    private static final String STATE_RESULT_PREVIOUS_BEST_MOVES = "result_previous_best_moves";
    private static final String STATE_RESULT_PREVIOUS_BEST_TIME = "result_previous_best_time";
    private static final int ONBOARDING_PAGE_COUNT = 4;

    private static final int COLOR_BACKGROUND = Color.rgb(17, 24, 39);
    private static final int COLOR_PANEL = Color.rgb(31, 41, 55);
    private static final int COLOR_PANEL_LIGHT = Color.rgb(55, 65, 81);
    private static final int COLOR_PRIMARY = Color.rgb(46, 125, 50);
    private static final int COLOR_ACCENT = Color.rgb(245, 158, 11);
    private static final int COLOR_MUTED_TEXT = Color.rgb(209, 213, 219);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Button> commandButtons = new ArrayList<>();

    private GameModel model;
    private KlotskiView boardView;
    private TextView statusText;
    private TextView gameTitleText;
    private PendingWin pendingWin;
    private GameResult currentResult;
    private OnBackInvokedCallback backCallback;
    private Screen currentScreen = Screen.HOME;
    private Screen infoReturnScreen = Screen.HOME;
    private int onboardingPage;
    private boolean solverRunning;
    private boolean assistedSolveActive;
    private boolean gameStarted;
    private long lastWinTimeMs = -1;

    /**
     * Creates the Android activity instance used by the platform launcher.
     */
    public MainActivity() {
    }

    private enum Screen {
        HOME,
        ONBOARDING,
        MODE_SELECT,
        HOW_TO_PLAY,
        RECORDS,
        SETTINGS,
        RESULTS,
        GAME
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (currentScreen == Screen.GAME) {
                updateStatus();
            }
            handler.postDelayed(this, 1000);
        }
    };

    /**
     * Builds the Android app shell and restores the current app screen when
     * Android recreates the activity.
     *
     * @param savedInstanceState Android activity restore bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);

        int lastSize = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_LAST_SIZE, 4);
        if (lastSize < 3 || lastSize > 5) {
            lastSize = 4;
        }
        attachModel(new GameModel(lastSize));
        registerBackHandler();
        if (savedInstanceState == null || !restoreAppScreen(savedInstanceState)) {
            if (shouldShowOnboarding()) {
                showOnboardingScreen(0);
            } else {
                showHomeScreen();
            }
        }
        handler.post(ticker);
    }

    /**
     * Persists the current navigation state before Android recreates the activity.
     *
     * @param outState Android activity state bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveGame();
        outState.putString(STATE_SCREEN, currentScreen.name());
        outState.putString(STATE_INFO_RETURN_SCREEN, infoReturnScreen.name());
        outState.putBoolean(STATE_GAME_STARTED, gameStarted);
        outState.putInt(STATE_ONBOARDING_PAGE, onboardingPage);
        saveResultState(outState);
        super.onSaveInstanceState(outState);
    }

    /**
     * Persists the current board when Android backgrounds the activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
    }

    /**
     * Stops the status ticker when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBackHandler();
        handler.removeCallbacks(ticker);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (currentScreen == Screen.GAME || currentScreen == Screen.RESULTS) {
            saveGame();
            showHomeScreen();
        } else if (currentScreen == Screen.ONBOARDING) {
            showHomeScreen();
        } else if (currentScreen == Screen.HOME) {
            finish();
        } else if ((currentScreen == Screen.HOW_TO_PLAY || currentScreen == Screen.RECORDS
                || currentScreen == Screen.SETTINGS)
                && infoReturnScreen == Screen.GAME && gameStarted) {
            showGameScreen();
        } else {
            showHomeScreen();
        }
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback = this::handleBackNavigation;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    private void unregisterBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
            backCallback = null;
        }
    }

    private void attachModel(GameModel newModel) {
        if (model != null) {
            model.removeObserver(this);
        }
        model = newModel;
        model.addObserver(this);
        if (boardView != null) {
            boardView.setModel(model);
        }
    }

    private boolean restoreAppScreen(Bundle savedInstanceState) {
        Screen savedScreen = readScreen(savedInstanceState, STATE_SCREEN, Screen.HOME);
        Screen savedReturnScreen = readScreen(savedInstanceState, STATE_INFO_RETURN_SCREEN, Screen.HOME);
        boolean savedGameStarted = savedInstanceState.getBoolean(STATE_GAME_STARTED, false);
        int savedOnboardingPage = savedInstanceState.getInt(STATE_ONBOARDING_PAGE, 0);
        currentResult = restoreResultState(savedInstanceState);

        if (savedScreen == Screen.GAME) {
            if (savedGameStarted && loadGame()) {
                showGameScreen();
                return true;
            }
            gameStarted = false;
            return false;
        }

        if (savedScreen == Screen.RESULTS) {
            if (currentResult != null) {
                showResultsScreen();
                return true;
            }
            return false;
        }

        if ((savedScreen == Screen.HOW_TO_PLAY || savedScreen == Screen.RECORDS
                || savedScreen == Screen.SETTINGS)
                && savedReturnScreen == Screen.GAME && savedGameStarted && !loadGame()) {
            savedReturnScreen = Screen.HOME;
            gameStarted = false;
        }

        switch (savedScreen) {
            case ONBOARDING -> showOnboardingScreen(savedOnboardingPage);
            case MODE_SELECT -> showModeSelectScreen();
            case HOW_TO_PLAY -> showHowToScreen(savedReturnScreen);
            case RECORDS -> showRecordsScreen(savedReturnScreen);
            case SETTINGS -> showSettingsScreen(savedReturnScreen);
            case HOME -> showHomeScreen();
            default -> {
                return false;
            }
        }
        return true;
    }

    private Screen readScreen(Bundle bundle, String key, Screen fallback) {
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

    private void saveResultState(Bundle outState) {
        if (currentResult == null) {
            outState.putBoolean(STATE_RESULT_AVAILABLE, false);
            return;
        }
        outState.putBoolean(STATE_RESULT_AVAILABLE, true);
        outState.putInt(STATE_RESULT_SIZE, currentResult.size);
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

    private GameResult restoreResultState(Bundle savedInstanceState) {
        if (!savedInstanceState.getBoolean(STATE_RESULT_AVAILABLE, false)) {
            return null;
        }
        int previousMoves = savedInstanceState.getInt(STATE_RESULT_PREVIOUS_BEST_MOVES, -1);
        long previousTime = savedInstanceState.getLong(STATE_RESULT_PREVIOUS_BEST_TIME, -1);
        Best previousBest = previousMoves < 0 || previousTime < 0
                ? null
                : new Best(previousMoves, previousTime);
        return new GameResult(
                savedInstanceState.getInt(STATE_RESULT_SIZE, 4),
                savedInstanceState.getInt(STATE_RESULT_MOVES, 0),
                savedInstanceState.getLong(STATE_RESULT_TIME, 0),
                savedInstanceState.getBoolean(STATE_RESULT_ASSISTED, false),
                savedInstanceState.getBoolean(STATE_RESULT_NEW_BEST, false),
                previousBest);
    }

    private void showHomeScreen() {
        currentScreen = Screen.HOME;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.home_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        addScreenHeader(screen.content, getString(R.string.app_name), getString(R.string.home_tagline));

        TextView summary = createText(getString(R.string.home_summary), 16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams summaryParams = fullWidthParams();
        summaryParams.setMargins(0, dp(8), 0, dp(24));
        screen.content.addView(summary, summaryParams);

        boolean hasSave = hasSavedGame();
        if (hasSave) {
            Button continueButton = addWideButton(screen.content, R.string.home_continue, COLOR_PRIMARY,
                    v -> continueSavedGame());
            continueButton.setId(R.id.home_continue_button);
        }
        Button newGameButton = addWideButton(screen.content, hasSave ? R.string.home_new_game : R.string.home_play,
                hasSave ? COLOR_PANEL_LIGHT : COLOR_PRIMARY, v -> {
                    if (shouldShowOnboarding()) {
                        showOnboardingScreen(0);
                    } else {
                        showModeSelectScreen();
                    }
                });
        newGameButton.setId(R.id.home_new_game_button);
        Button onboardingButton = addWideButton(screen.content, R.string.home_beginner_guide, COLOR_PANEL,
                v -> showOnboardingScreen(0));
        onboardingButton.setId(R.id.home_onboarding_button);
        Button howToButton = addWideButton(screen.content, R.string.home_how_to_play, COLOR_PANEL,
                v -> showHowToScreen(Screen.HOME));
        howToButton.setId(R.id.home_how_to_play_button);
        Button settingsButton = addWideButton(screen.content, R.string.home_settings, COLOR_PANEL,
                v -> showSettingsScreen(Screen.HOME));
        settingsButton.setId(R.id.home_settings_button);
        Button recordsButton = addWideButton(screen.content, R.string.home_records, COLOR_PANEL,
                v -> showRecordsScreen(Screen.HOME));
        recordsButton.setId(R.id.home_records_button);

        setContentView(screen.root);
    }

    private void showOnboardingScreen(int requestedPage) {
        currentScreen = Screen.ONBOARDING;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();
        onboardingPage = clampOnboardingPage(requestedPage);

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.onboarding_root);
        addScreenHeader(screen.content, getString(R.string.onboarding_title),
                getString(R.string.onboarding_subtitle));

        TextView progress = createText(getString(R.string.onboarding_progress,
                onboardingPage + 1, ONBOARDING_PAGE_COUNT), 14, COLOR_ACCENT, Typeface.BOLD);
        progress.setId(R.id.onboarding_progress_text);
        progress.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = fullWidthParams();
        progressParams.setMargins(0, 0, 0, dp(12));
        screen.content.addView(progress, progressParams);

        addOnboardingPage(screen.content);

        if (onboardingPage < ONBOARDING_PAGE_COUNT - 1) {
            Button nextButton = addWideButton(screen.content, R.string.onboarding_next, COLOR_PRIMARY,
                    v -> showOnboardingScreen(onboardingPage + 1));
            nextButton.setId(R.id.onboarding_next_button);
        } else {
            Button startButton = addWideButton(screen.content, R.string.onboarding_start_3, COLOR_PRIMARY,
                    v -> startFirstPuzzle());
            startButton.setId(R.id.onboarding_start_3_button);
        }

        if (onboardingPage > 0) {
            Button backButton = addWideButton(screen.content, R.string.onboarding_back, COLOR_PANEL,
                    v -> showOnboardingScreen(onboardingPage - 1));
            backButton.setId(R.id.onboarding_back_button);
        }

        Button skipButton = addWideButton(screen.content, R.string.onboarding_skip, COLOR_PANEL,
                v -> skipOnboarding());
        skipButton.setId(R.id.onboarding_skip_button);

        setContentView(screen.root);
    }

    private void showModeSelectScreen() {
        currentScreen = Screen.MODE_SELECT;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.mode_root);
        addScreenHeader(screen.content, getString(R.string.mode_title), getString(R.string.mode_subtitle));
        addModeRow(screen.content, 3, R.string.mode_easy, R.string.mode_easy_detail);
        addModeRow(screen.content, 4, R.string.mode_classic, R.string.mode_classic_detail);
        addModeRow(screen.content, 5, R.string.mode_expert, R.string.mode_expert_detail);
        Button homeButton = addWideButton(screen.content, R.string.nav_home, COLOR_PANEL, v -> showHomeScreen());
        homeButton.setId(R.id.mode_home_button);

        setContentView(screen.root);
    }

    private void showHowToScreen(Screen returnScreen) {
        currentScreen = Screen.HOW_TO_PLAY;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.how_root);
        addScreenHeader(screen.content, getString(R.string.how_title), getString(R.string.how_subtitle));
        addLearningExample(screen.content, R.id.how_goal_example, R.string.how_goal_title, R.string.how_goal_body,
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 0}}, new int[] {});
        addLearningExample(screen.content, R.id.how_tap_example, R.string.how_tap_title, R.string.how_tap_body,
                new int[][] {{1, 2, 3}, {4, 5, 0}, {7, 8, 6}}, new int[] {5});
        addLearningExample(screen.content, R.id.how_line_example, R.string.how_line_title, R.string.how_line_body,
                new int[][] {{1, 2, 3}, {0, 4, 5}, {7, 8, 6}}, new int[] {4, 5});
        addInstruction(screen.content, R.string.how_swipe_title, R.string.how_swipe_body);
        addInstruction(screen.content, R.string.how_tools_title, R.string.how_tools_body);
        addInstruction(screen.content, R.string.how_records_title, R.string.how_records_body);
        Button backButton = addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
        backButton.setId(R.id.how_back_button);

        setContentView(screen.root);
    }

    private void showRecordsScreen(Screen returnScreen) {
        currentScreen = Screen.RECORDS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.records_root);
        addScreenHeader(screen.content, getString(R.string.records_title), getString(R.string.records_subtitle));
        addRecordRow(screen.content, 3, R.string.mode_easy);
        addRecordRow(screen.content, 4, R.string.mode_classic);
        addRecordRow(screen.content, 5, R.string.mode_expert);
        Button backButton = addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
        backButton.setId(R.id.records_back_button);

        setContentView(screen.root);
    }

    private void showSettingsScreen(Screen returnScreen) {
        currentScreen = Screen.SETTINGS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.settings_root);
        addScreenHeader(screen.content, getString(R.string.settings_title), getString(R.string.settings_subtitle));
        addSettingsSwitch(screen.content, R.id.settings_haptic_switch, R.string.settings_haptic_title,
                R.string.settings_haptic_body, isHapticEnabled(), checked -> {
                    setPreference(KEY_HAPTIC_ENABLED, checked);
                    applySettingsToBoard();
                });
        addSettingsSwitch(screen.content, R.id.settings_reduced_motion_switch, R.string.settings_reduced_motion_title,
                R.string.settings_reduced_motion_body, isReducedMotionEnabled(), checked -> {
                    setPreference(KEY_REDUCED_MOTION, checked);
                    applySettingsToBoard();
                });
        Button resetSaveButton = addWideButton(screen.content, R.string.settings_reset_save, COLOR_PANEL,
                v -> confirmResetSave());
        resetSaveButton.setId(R.id.settings_reset_save_button);
        Button resetRecordsButton = addWideButton(screen.content, R.string.settings_reset_records, COLOR_PANEL,
                v -> confirmResetRecords());
        resetRecordsButton.setId(R.id.settings_reset_records_button);
        Button backButton = addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
        backButton.setId(R.id.settings_back_button);

        setContentView(screen.root);
    }

    private void showResultsScreen() {
        if (currentResult == null) {
            showHomeScreen();
            return;
        }

        currentScreen = Screen.RESULTS;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = createScreenLayout();
        screen.root.setId(R.id.results_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        addScreenHeader(screen.content, getString(R.string.results_title),
                getString(currentResult.assisted
                        ? R.string.results_assisted_subtitle
                        : R.string.results_player_subtitle));

        TextView size = createText(getString(R.string.results_size_format,
                currentResult.size, currentResult.size), 18, Color.WHITE, Typeface.BOLD);
        size.setId(R.id.results_size_text);
        size.setGravity(Gravity.CENTER);
        screen.content.addView(size, fullWidthParams());

        TextView stats = createText(getString(R.string.results_stats_format,
                formatMoves(currentResult.moves), currentResult.timeMs / 1000),
                24, Color.WHITE, Typeface.BOLD);
        stats.setId(R.id.results_stats_text);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = fullWidthParams();
        statsParams.setMargins(0, dp(10), 0, dp(10));
        screen.content.addView(stats, statsParams);

        TextView record = createText(resultRecordText(currentResult), 16,
                currentResult.newBest ? COLOR_ACCENT : COLOR_MUTED_TEXT, Typeface.BOLD);
        record.setId(R.id.results_record_text);
        record.setGravity(Gravity.CENTER);
        record.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams recordParams = fullWidthParams();
        recordParams.setMargins(0, 0, 0, dp(22));
        screen.content.addView(record, recordParams);

        Button playAgainButton = addWideButton(screen.content, R.string.results_play_again, COLOR_PRIMARY,
                v -> beginNewGame(currentResult.size));
        playAgainButton.setId(R.id.results_play_again_button);
        Button newSizeButton = addWideButton(screen.content, R.string.results_new_size, COLOR_PANEL,
                v -> {
                    saveGame();
                    showModeSelectScreen();
                });
        newSizeButton.setId(R.id.results_new_size_button);
        Button homeButton = addWideButton(screen.content, R.string.nav_home, COLOR_PANEL,
                v -> {
                    saveGame();
                    showHomeScreen();
                });
        homeButton.setId(R.id.results_home_button);

        setContentView(screen.root);
    }

    private void returnFromInfoScreen() {
        if (infoReturnScreen == Screen.GAME && gameStarted) {
            showGameScreen();
        } else {
            showHomeScreen();
        }
    }

    private void showGameScreen() {
        currentScreen = Screen.GAME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.game_root);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);
        root.setPadding(dp(12), systemBarHeight("status_bar_height") + dp(12),
                dp(12), systemBarHeight("navigation_bar_height") + dp(12));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(topBar, fullWidthParams());

        Button homeButton = createButton(getString(R.string.nav_home), COLOR_PANEL);
        homeButton.setId(R.id.game_home_button);
        homeButton.setOnClickListener(v -> {
            if (canAcceptCommand()) {
                saveGame();
                showHomeScreen();
            }
        });
        commandButtons.add(homeButton);
        topBar.addView(homeButton, fixedButtonParams(88));

        gameTitleText = createText("", 20, Color.WHITE, Typeface.BOLD);
        gameTitleText.setId(R.id.game_title_text);
        gameTitleText.setGravity(Gravity.CENTER);
        topBar.addView(gameTitleText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button menuButton = createButton(getString(R.string.game_menu), COLOR_PANEL);
        menuButton.setId(R.id.game_menu_button);
        menuButton.setOnClickListener(v -> {
            if (canAcceptCommand()) {
                showPauseMenu();
            }
        });
        commandButtons.add(menuButton);
        topBar.addView(menuButton, fixedButtonParams(88));

        statusText = createText("", 15, Color.WHITE, Typeface.NORMAL);
        statusText.setId(R.id.game_status_text);
        statusText.setGravity(Gravity.CENTER);
        statusText.setSingleLine(false);
        LinearLayout.LayoutParams statusParams = fullWidthParams();
        statusParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(statusText, statusParams);

        ensureBoardView();
        ViewParentRemover.removeFromParent(boardView);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(10), 0, 0);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, fullWidthParams());

        Button undoButton = addGameButton(actions, R.string.button_undo, v -> {
            if (canAcceptCommand()) {
                model.undo();
                performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
                updateStatus();
            }
        });
        undoButton.setId(R.id.game_undo_button);
        Button restartButton = addGameButton(actions, R.string.button_restart, v -> {
            if (canAcceptCommand()) {
                restartCurrentGame();
            }
        });
        restartButton.setId(R.id.game_restart_button);
        Button assistButton = addGameButton(actions, R.string.game_assist, v -> {
            if (canAcceptCommand()) {
                showAssistMenu();
            }
        });
        assistButton.setId(R.id.game_assist_button);

        setContentView(root);
        updateStatus();
    }

    private void ensureBoardView() {
        if (boardView == null) {
            boardView = new KlotskiView(this, model);
            boardView.setId(R.id.game_board);
            boardView.setBusyStateListener(this::updateControlsEnabled);
        } else {
            boardView.setModel(model);
            boardView.setId(R.id.game_board);
            boardView.setBusyStateListener(this::updateControlsEnabled);
        }
        applySettingsToBoard();
    }

    private void addModeRow(LinearLayout parent, int size, int difficultyResId, int detailResId) {
        LinearLayout row = new LinearLayout(this);
        if (size == 3) {
            row.setId(R.id.mode_3_button);
        } else if (size == 4) {
            row.setId(R.id.mode_4_button);
        } else if (size == 5) {
            row.setId(R.id.mode_5_button);
        }
        row.setContentDescription(getString(R.string.mode_card_title, size, size));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(makePanelBackground(COLOR_PANEL));
        row.setClickable(true);
        row.setOnClickListener(v -> beginNewGame(size));

        TextView title = createText(getString(R.string.mode_card_title, size, size), 22, Color.WHITE, Typeface.BOLD);
        TextView difficulty = createText(getString(difficultyResId), 15, COLOR_ACCENT, Typeface.BOLD);
        TextView detail = createText(getString(detailResId), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        TextView best = createText(getString(R.string.mode_best_label, formatBestForCard(size)),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, fullWidthParams());
        row.addView(difficulty, fullWidthParams());
        LinearLayout.LayoutParams detailParams = fullWidthParams();
        detailParams.setMargins(0, dp(8), 0, 0);
        row.addView(detail, detailParams);
        LinearLayout.LayoutParams bestParams = fullWidthParams();
        bestParams.setMargins(0, dp(8), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = fullWidthParams();
        rowParams.setMargins(0, 0, 0, dp(12));
        parent.addView(row, rowParams);
    }

    private void addRecordRow(LinearLayout parent, int size, int difficultyResId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(makePanelBackground(COLOR_PANEL));

        TextView title = createText(getString(R.string.records_row_title, size, size, getString(difficultyResId)),
                20, Color.WHITE, Typeface.BOLD);
        TextView best = createText(formatBestForCard(size), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, fullWidthParams());
        LinearLayout.LayoutParams bestParams = fullWidthParams();
        bestParams.setMargins(0, dp(6), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = fullWidthParams();
        rowParams.setMargins(0, 0, 0, dp(12));
        parent.addView(row, rowParams);
    }

    private void addSettingsSwitch(LinearLayout parent, int switchId, int titleResId, int bodyResId,
            boolean checked, SettingChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(makePanelBackground(COLOR_PANEL));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = createText(getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = createText(getString(bodyResId), 14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);
        copy.addView(title, fullWidthParams());
        LinearLayout.LayoutParams bodyParams = fullWidthParams();
        bodyParams.setMargins(0, dp(5), 0, 0);
        copy.addView(body, bodyParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setId(switchId);
        toggle.setChecked(checked);
        toggle.setContentDescription(getString(titleResId));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        switchParams.setMargins(dp(16), 0, 0, 0);
        row.addView(toggle, switchParams);

        LinearLayout.LayoutParams rowParams = fullWidthParams();
        rowParams.setMargins(0, 0, 0, dp(12));
        parent.addView(row, rowParams);
    }

    private void addOnboardingPage(LinearLayout parent) {
        int titleResId;
        int bodyResId;
        switch (onboardingPage) {
            case 0 -> {
                titleResId = R.string.onboarding_goal_title;
                bodyResId = R.string.onboarding_goal_body;
            }
            case 1 -> {
                titleResId = R.string.onboarding_tap_title;
                bodyResId = R.string.onboarding_tap_body;
            }
            case 2 -> {
                titleResId = R.string.onboarding_line_title;
                bodyResId = R.string.onboarding_line_body;
            }
            default -> {
                titleResId = R.string.onboarding_tools_title;
                bodyResId = R.string.onboarding_tools_body;
            }
        }
        addInstruction(parent, titleResId, bodyResId);
    }

    private void addInstruction(LinearLayout parent, int titleResId, int bodyResId) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(makePanelBackground(COLOR_PANEL));

        TextView title = createText(getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = createText(getString(bodyResId), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);

        panel.addView(title, fullWidthParams());
        LinearLayout.LayoutParams bodyParams = fullWidthParams();
        bodyParams.setMargins(0, dp(6), 0, 0);
        panel.addView(body, bodyParams);

        LinearLayout.LayoutParams panelParams = fullWidthParams();
        panelParams.setMargins(0, 0, 0, dp(12));
        parent.addView(panel, panelParams);
    }

    private void addLearningExample(LinearLayout parent, int viewId, int titleResId, int bodyResId,
            int[][] grid, int[] highlightedValues) {
        LinearLayout panel = new LinearLayout(this);
        panel.setId(viewId);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(makePanelBackground(COLOR_PANEL));

        TextView title = createText(getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = createText(getString(bodyResId), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);

        panel.addView(title, fullWidthParams());
        LinearLayout.LayoutParams bodyParams = fullWidthParams();
        bodyParams.setMargins(0, dp(6), 0, dp(12));
        panel.addView(body, bodyParams);
        panel.addView(createLearningBoard(grid, highlightedValues), centeredWrapParams());

        LinearLayout.LayoutParams panelParams = fullWidthParams();
        panelParams.setMargins(0, 0, 0, dp(12));
        parent.addView(panel, panelParams);
    }

    private GridLayout createLearningBoard(int[][] grid, int[] highlightedValues) {
        GridLayout board = new GridLayout(this);
        board.setColumnCount(3);
        board.setRowCount(3);
        board.setUseDefaultMargins(false);

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                int value = grid[row][col];
                boolean highlighted = containsValue(highlightedValues, value);
                TextView cell = createLearningCell(value, highlighted);
                GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams(
                        GridLayout.spec(row), GridLayout.spec(col));
                cellParams.width = dp(48);
                cellParams.height = dp(48);
                cellParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                board.addView(cell, cellParams);
            }
        }

        return board;
    }

    private TextView createLearningCell(int value, boolean highlighted) {
        TextView cell = createText(value == 0 ? getString(R.string.board_empty_cell_short) : String.valueOf(value),
                value == 0 ? 10 : 18,
                highlighted ? Color.BLACK : (value == 0 ? COLOR_ACCENT : Color.WHITE),
                Typeface.BOLD);
        cell.setGravity(Gravity.CENTER);
        if (value == 0) {
            cell.setBackground(makeCellBackground(COLOR_BACKGROUND, COLOR_ACCENT));
            cell.setContentDescription(getString(R.string.board_empty_cell_description));
        } else if (highlighted) {
            cell.setBackground(makeCellBackground(COLOR_ACCENT, Color.WHITE));
            cell.setContentDescription(getString(R.string.board_highlighted_tile_description, value));
        } else {
            cell.setBackground(makeCellBackground(COLOR_PANEL_LIGHT, Color.argb(80, 255, 255, 255)));
            cell.setContentDescription(getString(R.string.board_tile_description, value));
        }
        return cell;
    }

    private void showPauseMenu() {
        String[] items = new String[] {
                getString(R.string.menu_resume),
                getString(R.string.button_save),
                getString(R.string.button_load),
                getString(R.string.button_restart),
                getString(R.string.menu_new_size),
                getString(R.string.menu_quick_reminder),
                getString(R.string.home_how_to_play),
                getString(R.string.home_settings),
                getString(R.string.home_records),
                getString(R.string.nav_home)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_title)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 1 -> {
                            saveGame();
                            Toast.makeText(this, R.string.toast_game_saved, Toast.LENGTH_SHORT).show();
                        }
                        case 2 -> {
                            if (loadGame()) {
                                pendingWin = null;
                                assistedSolveActive = false;
                                showGameScreen();
                                Toast.makeText(this, R.string.toast_game_loaded, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
                            }
                        }
                        case 3 -> restartCurrentGame();
                        case 4 -> {
                            saveGame();
                            showModeSelectScreen();
                        }
                        case 5 -> showQuickReminder();
                        case 6 -> showHowToScreen(Screen.GAME);
                        case 7 -> showSettingsScreen(Screen.GAME);
                        case 8 -> showRecordsScreen(Screen.GAME);
                        case 9 -> {
                            saveGame();
                            showHomeScreen();
                        }
                        default -> {
                        }
                    }
                })
                .show();
    }

    private void showQuickReminder() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_reminder_title)
                .setMessage(R.string.quick_reminder_message)
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }

    private void confirmResetSave() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_reset_save_title)
                .setMessage(R.string.dialog_reset_save_message)
                .setPositiveButton(R.string.dialog_reset, (dialog, which) -> {
                    clearSavedGame();
                    Toast.makeText(this, R.string.toast_save_reset, Toast.LENGTH_SHORT).show();
                    showSettingsScreen(infoReturnScreen);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void confirmResetRecords() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_reset_records_title)
                .setMessage(R.string.dialog_reset_records_message)
                .setPositiveButton(R.string.dialog_reset, (dialog, which) -> {
                    clearRecords();
                    Toast.makeText(this, R.string.toast_records_reset, Toast.LENGTH_SHORT).show();
                    showSettingsScreen(infoReturnScreen);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAssistMenu() {
        String[] items = new String[] {
                getString(R.string.button_solver_bfs),
                getString(R.string.button_solver_astar),
                getString(R.string.button_solver_idastar)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.assist_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        runSolver(new BfsSolver());
                    } else if (which == 1) {
                        runSolver(new AStarSolver());
                    } else if (which == 2) {
                        runSolver(new IdaStarSolver());
                    }
                })
                .show();
    }

    private void startFirstPuzzle() {
        markOnboardingSeen();
        beginNewGame(3);
    }

    private void skipOnboarding() {
        markOnboardingSeen();
        showHomeScreen();
    }

    private boolean shouldShowOnboarding() {
        return !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    private void markOnboardingSeen() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_ONBOARDING_SEEN, true)
                .apply();
    }

    private int clampOnboardingPage(int page) {
        if (page < 0) {
            return 0;
        }
        if (page >= ONBOARDING_PAGE_COUNT) {
            return ONBOARDING_PAGE_COUNT - 1;
        }
        return page;
    }

    private void continueSavedGame() {
        if (loadGame()) {
            pendingWin = null;
            currentResult = null;
            assistedSolveActive = false;
            showGameScreen();
        } else {
            Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
            showHomeScreen();
        }
    }

    private void beginNewGame(int size) {
        if (solverRunning) {
            return;
        }
        attachModel(new GameModel(size));
        model.scramble(size * size * 5);
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        lastWinTimeMs = -1;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_LAST_SIZE, size).apply();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private void restartCurrentGame() {
        if (!canAcceptCommand()) {
            return;
        }
        model.restartCurrentGame();
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        lastWinTimeMs = -1;
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private boolean canAcceptCommand() {
        return currentScreen == Screen.GAME && boardView != null && !solverRunning && !boardView.isBusy();
    }

    private void updateStatus() {
        if (currentScreen != Screen.GAME || statusText == null || model == null) {
            return;
        }

        if (gameTitleText != null) {
            gameTitleText.setText(getString(R.string.game_title_format, model.getSize(), model.getSize()));
        }

        Best best = getBest(model.getSize());
        String bestText = best == null
                ? getString(R.string.best_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
        if (!model.isGameRunning() && model.isSolved()) {
            long elapsed = lastWinTimeMs >= 0
                    ? lastWinTimeMs / 1000
                    : Math.max(0, System.currentTimeMillis() - model.getStartTime()) / 1000;
            statusText.setText(getString(R.string.status_solved_format, model.getMoveCount(), elapsed, bestText));
            updateControlsEnabled();
            return;
        }

        long elapsed = Math.max(0, System.currentTimeMillis() - model.getStartTime()) / 1000;
        statusText.setText(getString(R.string.status_format, formatMoves(model.getMoveCount()), elapsed, bestText));
        updateControlsEnabled();
    }

    private void updateControlsEnabled() {
        boolean enabled = canAcceptCommand();
        for (Button button : commandButtons) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1f : 0.45f);
        }
    }

    private void saveGame() {
        if (!gameStarted || model == null) {
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putInt(KEY_SIZE, model.getSize());
        editor.putString(KEY_GRID, flatten(model.getGridCopy()));
        editor.putString(KEY_INITIAL_GRID, flatten(model.getInitialGridCopy()));
        editor.putInt(KEY_MOVES, model.getMoveCount());
        editor.putInt(KEY_LAST_SIZE, model.getSize());
        long elapsed = model.isSolved() && lastWinTimeMs >= 0
                ? lastWinTimeMs
                : Math.max(0, System.currentTimeMillis() - model.getStartTime());
        editor.putLong(KEY_ELAPSED, elapsed);
        editor.apply();
    }

    private boolean loadGame() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains(KEY_GRID)) {
            return false;
        }

        int size = prefs.getInt(KEY_SIZE, 4);
        if (size < 3 || size > 5) {
            return false;
        }
        int[][] grid = parseGrid(prefs.getString(KEY_GRID, ""), size);
        if (grid == null) {
            return false;
        }
        int[][] initialGrid = parseGrid(prefs.getString(KEY_INITIAL_GRID, ""), size);
        if (initialGrid == null) {
            initialGrid = copyGrid(grid);
        }

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = initialGrid;
        data.moveCount = prefs.getInt(KEY_MOVES, 0);
        data.elapsedTime = prefs.getLong(KEY_ELAPSED, 0);

        attachModel(new GameModel(size));
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = false;
        currentResult = null;
        gameStarted = true;
        return true;
    }

    private boolean hasSavedGame() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains(KEY_GRID)) {
            return false;
        }

        int size = prefs.getInt(KEY_SIZE, 4);
        return size >= 3 && size <= 5 && parseGrid(prefs.getString(KEY_GRID, ""), size) != null;
    }

    private void clearSavedGame() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .remove(KEY_SIZE)
                .remove(KEY_GRID)
                .remove(KEY_INITIAL_GRID)
                .remove(KEY_MOVES)
                .remove(KEY_ELAPSED)
                .commit();
    }

    private String flatten(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }

    private int[][] parseGrid(String text, int size) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int[][] grid = new int[size][size];
        String[] values = text.split(",");
        if (values.length != size * size) {
            return null;
        }
        try {
            for (int i = 0; i < values.length; i++) {
                grid[i / size][i % size] = Integer.parseInt(values[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return grid;
    }

    private int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][grid.length];
        for (int i = 0; i < grid.length; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, grid.length);
        }
        return copy;
    }

    private Best getBest(int size) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int moves = prefs.getInt(KEY_BEST_PREFIX + size + "_moves", -1);
        long timeMs = prefs.getLong(KEY_BEST_PREFIX + size + "_time", -1);
        if (moves < 0 || timeMs < 0) {
            return null;
        }
        return new Best(moves, timeMs);
    }

    private void recordBest(int size, int moves, long timeMs) {
        Best best = getBest(size);
        if (!isBetterRecord(best, moves, timeMs)) {
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_BEST_PREFIX + size + "_moves", moves)
                .putLong(KEY_BEST_PREFIX + size + "_time", timeMs)
                .apply();
    }

    private boolean isBetterRecord(Best best, int moves, long timeMs) {
        return best == null || moves < best.moves || (moves == best.moves && timeMs < best.timeMs);
    }

    private void clearRecords() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (int size = 3; size <= 5; size++) {
            editor.remove(KEY_BEST_PREFIX + size + "_moves");
            editor.remove(KEY_BEST_PREFIX + size + "_time");
        }
        editor.commit();
    }

    private void runSolver(Solver solver) {
        if (!canAcceptCommand() || model.isSolved()) {
            return;
        }

        int warning = solverWarningMessage(solver);
        if (warning != 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_warning_title)
                    .setMessage(warning)
                    .setPositiveButton(R.string.dialog_continue, (dialog, which) -> startSolver(solver))
                    .setNegativeButton(R.string.dialog_close, null)
                    .show();
            return;
        }

        startSolver(solver);
    }

    private int solverWarningMessage(Solver solver) {
        if (model.getSize() >= 4 && solver instanceof BfsSolver) {
            return R.string.dialog_solver_warning_bfs;
        }
        if (model.getSize() > 4 && solver instanceof AStarSolver) {
            return R.string.dialog_solver_warning_astar;
        }
        if (model.getSize() > 4 && solver instanceof IdaStarSolver) {
            return R.string.dialog_solver_warning_idastar;
        }
        return 0;
    }

    private void startSolver(Solver solver) {
        solverRunning = true;
        boardView.setInputLocked(true);
        updateControlsEnabled();
        statusText.setText(getString(R.string.status_solving, solver.getName()));
        new Thread(() -> {
            List<Direction> solution = solver.solve(model);
            handler.post(() -> finishSolver(solver, solution));
        }, "SlideDoSolver").start();
    }

    private void finishSolver(Solver solver, List<Direction> solution) {
        solverRunning = false;
        if (solution == null) {
            boardView.setInputLocked(false);
            updateStatus();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_result_title)
                    .setMessage(R.string.dialog_solver_failed)
                    .setPositiveButton(R.string.dialog_close, null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_solver_result_title)
                .setMessage(getString(R.string.dialog_solver_found, solution.size()))
                .setPositiveButton(R.string.dialog_animate, (dialog, which) -> {
                    assistedSolveActive = true;
                    boardView.enqueueMoves(solution);
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> {
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .setOnCancelListener(dialog -> {
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .show();
    }

    private ScreenLayout createScreenLayout() {
        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), systemBarHeight("status_bar_height") + dp(26),
                dp(18), systemBarHeight("navigation_bar_height") + dp(18));
        root.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        return new ScreenLayout(root, content);
    }

    private void addScreenHeader(LinearLayout parent, String title, String subtitle) {
        TextView titleText = createText(title, 34, Color.WHITE, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        parent.addView(titleText, fullWidthParams());

        TextView subtitleText = createText(subtitle, 16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams subtitleParams = fullWidthParams();
        subtitleParams.setMargins(0, dp(8), 0, dp(24));
        parent.addView(subtitleText, subtitleParams);
    }

    private Button addWideButton(LinearLayout parent, int textResId, int color, View.OnClickListener listener) {
        Button button = createButton(getString(textResId), color);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = fullWidthParams();
        params.setMargins(0, 0, 0, dp(10));
        parent.addView(button, params);
        return button;
    }

    private Button addGameButton(LinearLayout parent, int textResId, View.OnClickListener listener) {
        Button button = createButton(getString(textResId), COLOR_PANEL_LIGHT);
        button.setOnClickListener(listener);
        commandButtons.add(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(button, params);
        return button;
    }

    private Button createButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setBackground(makePanelBackground(color));
        return button;
    }

    private TextView createText(CharSequence text, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fixedButtonParams(int widthDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(44));
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private LinearLayout.LayoutParams centeredWrapParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private GradientDrawable makePanelBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable makeCellBackground(int color, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private String formatBestForCard(int size) {
        Best best = getBest(size);
        return best == null
                ? getString(R.string.records_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
    }

    private String resultRecordText(GameResult result) {
        if (result.assisted) {
            String previous = result.previousBest == null
                    ? getString(R.string.records_empty)
                    : getString(R.string.best_format, formatMoves(result.previousBest.moves),
                            result.previousBest.timeMs / 1000);
            return getString(R.string.results_assisted_record, previous);
        }
        if (result.newBest) {
            return result.previousBest == null
                    ? getString(R.string.results_first_record)
                    : getString(R.string.results_new_best,
                            getString(R.string.best_format, formatMoves(result.previousBest.moves),
                                    result.previousBest.timeMs / 1000));
        }
        Best best = getBest(result.size);
        String bestText = best == null
                ? getString(R.string.records_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
        return getString(R.string.results_no_new_best, bestText);
    }

    private void performBoardHaptic(int feedbackConstant) {
        if (boardView != null && isHapticEnabled()) {
            boardView.performHapticFeedback(feedbackConstant);
        }
    }

    private boolean isHapticEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_HAPTIC_ENABLED, true);
    }

    private boolean isReducedMotionEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_REDUCED_MOTION, false);
    }

    private void setPreference(String key, boolean value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(key, value)
                .apply();
    }

    private void applySettingsToBoard() {
        if (boardView != null) {
            boardView.setHapticFeedbackEnabled(isHapticEnabled());
            boardView.setReducedMotionEnabled(isReducedMotionEnabled());
        }
    }

    private boolean containsValue(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int systemBarHeight(String resourceName) {
        int resourceId = getResources().getIdentifier(resourceName, "dimen", "android");
        if (resourceId == 0) {
            return 0;
        }
        return getResources().getDimensionPixelSize(resourceId);
    }

    /**
     * Updates the HUD when the shared model reports a board change.
     */
    @Override
    public void onGridChanged() {
        updateStatus();
    }

    /**
     * Updates the HUD after a single empty-tile move.
     *
     * @param dir direction the empty tile moved
     */
    @Override
    public void onMove(Direction dir) {
        updateStatus();
    }

    /**
     * Records a pending win and defers the completion dialog until animation ends.
     *
     * @param moves final move count reported by the model
     * @param timeMs elapsed play time in milliseconds
     */
    @Override
    public void onGameWon(int moves, long timeMs) {
        lastWinTimeMs = timeMs;
        pendingWin = new PendingWin(model.getSize(), moves, timeMs, assistedSolveActive);
        handler.postDelayed(this::showWinWhenReady, 180);
    }

    private void showWinWhenReady() {
        if (pendingWin == null) {
            return;
        }
        if (boardView != null && boardView.isBusy()) {
            handler.postDelayed(this::showWinWhenReady, 80);
            return;
        }

        PendingWin win = pendingWin;
        pendingWin = null;
        Best previousBest = getBest(win.size);
        boolean newBest = !win.assisted && isBetterRecord(previousBest, win.moves, win.timeMs);
        if (newBest) {
            recordBest(win.size, win.moves, win.timeMs);
        }
        currentResult = new GameResult(win.size, win.moves, win.timeMs, win.assisted, newBest, previousBest);
        assistedSolveActive = false;
        performBoardHaptic(HapticFeedbackConstants.LONG_PRESS);
        updateStatus();
        showResultsScreen();
    }

    private String formatMoves(int moves) {
        return getResources().getQuantityString(R.plurals.moves_count, moves, moves);
    }

    private static class ScreenLayout {
        final ScrollView root;
        final LinearLayout content;

        ScreenLayout(ScrollView root, LinearLayout content) {
            this.root = root;
            this.content = content;
        }
    }

    private static class ViewParentRemover {
        private ViewParentRemover() {
        }

        static void removeFromParent(View view) {
            if (view != null && view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }
    }

    private static class Best {
        final int moves;
        final long timeMs;

        Best(int moves, long timeMs) {
            this.moves = moves;
            this.timeMs = timeMs;
        }
    }

    private static class PendingWin {
        final int size;
        final int moves;
        final long timeMs;
        final boolean assisted;

        PendingWin(int size, int moves, long timeMs, boolean assisted) {
            this.size = size;
            this.moves = moves;
            this.timeMs = timeMs;
            this.assisted = assisted;
        }
    }

    private static class GameResult {
        final int size;
        final int moves;
        final long timeMs;
        final boolean assisted;
        final boolean newBest;
        final Best previousBest;

        GameResult(int size, int moves, long timeMs, boolean assisted, boolean newBest, Best previousBest) {
            this.size = size;
            this.moves = moves;
            this.timeMs = timeMs;
            this.assisted = assisted;
            this.newBest = newBest;
            this.previousBest = previousBest;
        }
    }

    private interface SettingChangeListener {
        void onChanged(boolean checked);
    }
}
