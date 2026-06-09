package com.klotski.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
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
 * and completion results. Gameplay rules remain in the shared core so Android
 * behavior stays aligned with the desktop Swing reference.
 * </p>
 */
public class MainActivity extends Activity implements GameObserver {
    private static final String STATE_SCREEN = "screen";
    private static final String STATE_INFO_RETURN_SCREEN = "info_return_screen";
    private static final String STATE_GAME_STARTED = "game_started";
    private static final String STATE_ONBOARDING_PAGE = "onboarding_page";
    private static final String STATE_TUTORIAL_STEP = "tutorial_step";
    private static final String STATE_RESULT_AVAILABLE = "result_available";
    private static final String STATE_RESULT_SIZE = "result_size";
    private static final String STATE_RESULT_MOVES = "result_moves";
    private static final String STATE_RESULT_TIME = "result_time";
    private static final String STATE_RESULT_ASSISTED = "result_assisted";
    private static final String STATE_RESULT_NEW_BEST = "result_new_best";
    private static final String STATE_RESULT_PREVIOUS_BEST_MOVES = "result_previous_best_moves";
    private static final String STATE_RESULT_PREVIOUS_BEST_TIME = "result_previous_best_time";
    private static final int ONBOARDING_PAGE_COUNT = 4;
    private static final int TUTORIAL_FIRST_MOVE = 0;
    private static final int TUTORIAL_LINE_SLIDE = 1;
    private static final int TUTORIAL_COMPLETE = 2;
    private static final int TUTORIAL_PAGE_COUNT = 2;
    private static final int[][] TUTORIAL_FIRST_MOVE_GRID = {
            {1, 2, 3},
            {4, 5, 0},
            {7, 8, 6}
    };
    private static final int[][] TUTORIAL_LINE_SLIDE_GRID = {
            {1, 2, 3},
            {0, 4, 5},
            {7, 8, 6}
    };
    private static final int[][] TUTORIAL_LINE_COMPLETE_GRID = {
            {1, 2, 3},
            {4, 5, 0},
            {7, 8, 6}
    };

    private static final int COLOR_BACKGROUND = Color.rgb(17, 24, 39);
    private static final int COLOR_PANEL = Color.rgb(31, 41, 55);
    private static final int COLOR_PANEL_LIGHT = Color.rgb(55, 65, 81);
    private static final int COLOR_PRIMARY = Color.rgb(46, 125, 50);
    private static final int COLOR_ACCENT = Color.rgb(245, 158, 11);
    private static final int COLOR_MUTED_TEXT = Color.rgb(209, 213, 219);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Button> commandButtons = new ArrayList<>();

    private AndroidGameStore store;
    private GameModel model;
    private KlotskiView boardView;
    private TextView statusText;
    private TextView gameTitleText;
    private TextView tutorialProgressText;
    private TextView tutorialInstructionText;
    private TextView tutorialStatusText;
    private PendingWin pendingWin;
    private GameResult currentResult;
    private OnBackInvokedCallback backCallback;
    private Screen currentScreen = Screen.HOME;
    private Screen infoReturnScreen = Screen.HOME;
    private int onboardingPage;
    private int tutorialStep = TUTORIAL_FIRST_MOVE;
    private boolean solverRunning;
    private boolean assistedSolveActive;
    private boolean gameStarted;
    private boolean tutorialAdvancePending;
    private boolean hintActive;
    private long lastWinTimeMs = -1;

    /**
     * Creates the Android activity instance used by the platform launcher.
     */
    public MainActivity() {
    }

    private enum Screen {
        HOME,
        ONBOARDING,
        TUTORIAL,
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
        applyLegacySystemBarColors();

        store = new AndroidGameStore(this);
        int lastSize = store.getLastSize(4);
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
        outState.putInt(STATE_TUTORIAL_STEP, tutorialStep);
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
    @SuppressWarnings("deprecation")
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
        } else if (currentScreen == Screen.TUTORIAL) {
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

    @SuppressWarnings("deprecation")
    private void applyLegacySystemBarColors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Android 15+ deprecates explicit system bar colors for edge-to-edge windows.
            getWindow().setStatusBarColor(COLOR_BACKGROUND);
            getWindow().setNavigationBarColor(COLOR_BACKGROUND);
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
        int savedTutorialStep = savedInstanceState.getInt(STATE_TUTORIAL_STEP, TUTORIAL_FIRST_MOVE);
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
            case TUTORIAL -> showTutorialScreen(savedTutorialStep);
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
        AndroidGameStore.Best previousBest = previousMoves < 0 || previousTime < 0
                ? null
                : new AndroidGameStore.Best(previousMoves, previousTime);
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
        tutorialAdvancePending = false;
        hintActive = false;
        statusText = null;
        gameTitleText = null;
        tutorialProgressText = null;
        tutorialInstructionText = null;
        tutorialStatusText = null;
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
        Button tutorialButton = addWideButton(screen.content, R.string.home_tutorial, COLOR_PRIMARY,
                v -> startGuidedTutorial());
        tutorialButton.setId(R.id.home_tutorial_button);
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
            Button tutorialButton = addWideButton(screen.content, R.string.onboarding_start_tutorial, COLOR_PRIMARY,
                    v -> startGuidedTutorial());
            tutorialButton.setId(R.id.onboarding_tutorial_button);
            Button startButton = addWideButton(screen.content, R.string.onboarding_start_3, COLOR_PANEL,
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
                    store.setHapticEnabled(checked);
                    applySettingsToBoard();
                });
        addSettingsSwitch(screen.content, R.id.settings_reduced_motion_switch, R.string.settings_reduced_motion_title,
                R.string.settings_reduced_motion_body, isReducedMotionEnabled(), checked -> {
                    store.setReducedMotionEnabled(checked);
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

    private void showTutorialScreen(int requestedStep) {
        currentScreen = Screen.TUTORIAL;
        infoReturnScreen = Screen.HOME;
        tutorialAdvancePending = false;
        hintActive = false;
        tutorialStep = clampTutorialStep(requestedStep);
        gameStarted = false;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        lastWinTimeMs = -1;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        loadTutorialModel(tutorialStep);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.tutorial_root);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);
        root.setPadding(dp(12), systemBarHeight("status_bar_height") + dp(12),
                dp(12), systemBarHeight("navigation_bar_height") + dp(12));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(topBar, fullWidthParams());

        Button homeButton = createButton(getString(R.string.nav_home), COLOR_PANEL);
        homeButton.setId(R.id.tutorial_home_button);
        homeButton.setOnClickListener(v -> showHomeScreen());
        topBar.addView(homeButton, fixedButtonParams(88));

        TextView title = createText(getString(R.string.tutorial_title), 20, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button startButton = createButton(getString(R.string.onboarding_start_3), COLOR_PANEL);
        startButton.setId(R.id.tutorial_start_game_button);
        startButton.setOnClickListener(v -> beginNewGame(3));
        topBar.addView(startButton, fixedButtonParams(104));

        LinearLayout lesson = new LinearLayout(this);
        lesson.setOrientation(LinearLayout.VERTICAL);
        lesson.setPadding(dp(14), dp(12), dp(14), dp(12));
        lesson.setBackground(makePanelBackground(COLOR_PANEL));
        LinearLayout.LayoutParams lessonParams = fullWidthParams();
        lessonParams.setMargins(0, dp(10), 0, dp(10));
        root.addView(lesson, lessonParams);

        tutorialProgressText = createText("", 14, COLOR_ACCENT, Typeface.BOLD);
        tutorialProgressText.setId(R.id.tutorial_progress_text);
        lesson.addView(tutorialProgressText, fullWidthParams());

        tutorialInstructionText = createText("", 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        tutorialInstructionText.setId(R.id.tutorial_instruction_text);
        tutorialInstructionText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams instructionParams = fullWidthParams();
        instructionParams.setMargins(0, dp(6), 0, 0);
        lesson.addView(tutorialInstructionText, instructionParams);

        tutorialStatusText = createText("", 15, Color.WHITE, Typeface.BOLD);
        tutorialStatusText.setId(R.id.tutorial_status_text);
        tutorialStatusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = fullWidthParams();
        statusParams.setMargins(0, 0, 0, dp(8));
        root.addView(tutorialStatusText, statusParams);

        ensureBoardView();
        boardView.setId(R.id.tutorial_board);
        applyTutorialHighlights();
        ViewParentRemover.removeFromParent(boardView);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(10), 0, 0);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, fullWidthParams());

        Button restartButton = createButton(getString(R.string.tutorial_restart_lesson), COLOR_PANEL_LIGHT);
        restartButton.setOnClickListener(v -> {
            if (canAcceptTutorialCommand()) {
                showTutorialScreen(tutorialStep == TUTORIAL_COMPLETE ? TUTORIAL_FIRST_MOVE : tutorialStep);
            }
        });
        restartButton.setId(R.id.tutorial_restart_button);
        actions.addView(restartButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        setContentView(root);
        updateTutorialStatus();
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
        tutorialAdvancePending = false;
        hintActive = false;
        statusText = null;
        gameTitleText = null;
        tutorialProgressText = null;
        tutorialInstructionText = null;
        tutorialStatusText = null;
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
        homeButton.setContentDescription(getString(R.string.accessibility_game_home));
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
        menuButton.setContentDescription(getString(R.string.accessibility_game_menu));
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
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams statusParams = fullWidthParams();
        statusParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(statusText, statusParams);

        ensureBoardView();
        boardView.clearHighlights();
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
                clearGameHint();
                model.undo();
                performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
                updateStatus();
            }
        });
        undoButton.setId(R.id.game_undo_button);
        undoButton.setContentDescription(getString(R.string.accessibility_game_undo));
        Button restartButton = addGameButton(actions, R.string.button_restart, v -> {
            if (canAcceptCommand()) {
                restartCurrentGame();
            }
        });
        restartButton.setId(R.id.game_restart_button);
        restartButton.setContentDescription(getString(R.string.accessibility_game_restart));
        Button assistButton = addGameButton(actions, R.string.game_assist, v -> {
            if (canAcceptCommand()) {
                showAssistMenu();
            }
        });
        assistButton.setId(R.id.game_assist_button);
        assistButton.setContentDescription(getString(R.string.accessibility_game_assist));

        setContentView(root);
        updateStatus();
    }

    private void ensureBoardView() {
        if (boardView == null) {
            boardView = new KlotskiView(this, model);
            boardView.setId(R.id.game_board);
            boardView.setBusyStateListener(this::updateBoardDependentControls);
        } else {
            boardView.setModel(model);
            boardView.setId(R.id.game_board);
            boardView.setBusyStateListener(this::updateBoardDependentControls);
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
        toggle.setContentDescription(getString(R.string.accessibility_settings_switch,
                getString(titleResId), getString(bodyResId)));
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

    private void startGuidedTutorial() {
        markOnboardingSeen();
        showTutorialScreen(TUTORIAL_FIRST_MOVE);
    }

    private void loadTutorialModel(int step) {
        attachModel(new GameModel(3));
        int[][] grid;
        int moves;
        if (step == TUTORIAL_LINE_SLIDE) {
            grid = TUTORIAL_LINE_SLIDE_GRID;
            moves = 0;
        } else if (step == TUTORIAL_COMPLETE) {
            grid = TUTORIAL_LINE_COMPLETE_GRID;
            moves = 1;
        } else {
            grid = TUTORIAL_FIRST_MOVE_GRID;
            moves = 0;
        }
        model.loadState(copyGrid(grid), moves);
        if (boardView != null) {
            boardView.setInputLocked(false);
        }
    }

    private int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = new int[grid[i].length];
            System.arraycopy(grid[i], 0, copy[i], 0, grid[i].length);
        }
        return copy;
    }

    private void applyTutorialHighlights() {
        if (boardView == null) {
            return;
        }
        if (tutorialStep == TUTORIAL_COMPLETE) {
            boardView.clearHighlights();
            return;
        }
        int[] target = tutorialTargetCell();
        boardView.setHighlightedCells(createAlignedHintGrid(), target[0], target[1]);
    }

    private boolean[][] createAlignedHintGrid() {
        int size = model.getSize();
        boolean[][] hints = new boolean[size][size];
        int emptyRow = model.getEmptyRow();
        int emptyCol = model.getEmptyCol();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                hints[row][col] = model.getTile(row, col) != 0 && (row == emptyRow || col == emptyCol);
            }
        }
        return hints;
    }

    private int[] tutorialTargetCell() {
        return tutorialStep == TUTORIAL_LINE_SLIDE
                ? new int[] {1, 2}
                : new int[] {2, 2};
    }

    private void updateTutorialStatus() {
        if (currentScreen != Screen.TUTORIAL || tutorialProgressText == null
                || tutorialInstructionText == null || tutorialStatusText == null || model == null) {
            return;
        }

        if (tutorialStep == TUTORIAL_COMPLETE) {
            tutorialProgressText.setText(R.string.tutorial_complete_progress);
            tutorialInstructionText.setText(R.string.tutorial_complete_instruction);
            tutorialStatusText.setText(getString(R.string.tutorial_complete_status,
                    formatMoves(model.getMoveCount())));
            return;
        }

        tutorialProgressText.setText(getString(R.string.tutorial_progress,
                tutorialStep + 1, TUTORIAL_PAGE_COUNT));
        tutorialInstructionText.setText(tutorialStep == TUTORIAL_LINE_SLIDE
                ? R.string.tutorial_line_instruction
                : R.string.tutorial_first_instruction);
        tutorialStatusText.setText(getString(R.string.tutorial_status,
                formatMoves(model.getMoveCount())));
    }

    private void scheduleTutorialStep(int nextStep) {
        if (tutorialAdvancePending) {
            return;
        }
        tutorialAdvancePending = true;
        handler.postDelayed(() -> {
            if (currentScreen == Screen.TUTORIAL && tutorialAdvancePending) {
                showTutorialScreen(nextStep);
            }
        }, 220);
    }

    private void handleTutorialWin() {
        if (tutorialStep == TUTORIAL_FIRST_MOVE) {
            scheduleTutorialStep(TUTORIAL_LINE_SLIDE);
        }
    }

    private void handleTutorialLineMove(int steps) {
        updateTutorialStatus();
        if (tutorialStep == TUTORIAL_LINE_SLIDE && steps > 1) {
            scheduleTutorialStep(TUTORIAL_COMPLETE);
        }
    }

    private boolean canAcceptTutorialCommand() {
        return currentScreen == Screen.TUTORIAL && boardView != null && !boardView.isBusy();
    }

    private int clampTutorialStep(int step) {
        if (step < TUTORIAL_FIRST_MOVE) {
            return TUTORIAL_FIRST_MOVE;
        }
        if (step > TUTORIAL_COMPLETE) {
            return TUTORIAL_COMPLETE;
        }
        return step;
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
                getString(R.string.button_hint_movable),
                getString(R.string.button_solver_bfs),
                getString(R.string.button_solver_astar),
                getString(R.string.button_solver_idastar)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.assist_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showMovableTilesHint();
                    } else if (which == 1) {
                        runSolver(new BfsSolver());
                    } else if (which == 2) {
                        runSolver(new AStarSolver());
                    } else if (which == 3) {
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
        return !store.isOnboardingSeen();
    }

    private void markOnboardingSeen() {
        store.markOnboardingSeen();
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
        hintActive = false;
        lastWinTimeMs = -1;
        store.setLastSize(size);
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
        clearGameHint();
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

        AndroidGameStore.Best best = getBest(model.getSize());
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
        String status = getString(R.string.status_format, formatMoves(model.getMoveCount()), elapsed, bestText);
        if (hintActive) {
            status += "\n" + getString(R.string.status_hint_movable);
        }
        statusText.setText(status);
        updateControlsEnabled();
    }

    private void showMovableTilesHint() {
        if (!canAcceptCommand() || model.isSolved()) {
            return;
        }
        hintActive = true;
        boardView.setHighlightedCells(createAlignedHintGrid(), -1, -1);
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private void clearGameHint() {
        hintActive = false;
        if (currentScreen == Screen.GAME && boardView != null) {
            boardView.clearHighlights();
        }
    }

    private void updateControlsEnabled() {
        boolean enabled = canAcceptCommand();
        for (Button button : commandButtons) {
            button.setEnabled(enabled);
            button.setAlpha(enabled ? 1f : 0.45f);
        }
    }

    private void updateBoardDependentControls() {
        updateControlsEnabled();
        if (currentScreen == Screen.TUTORIAL) {
            updateTutorialStatus();
        }
    }

    private void saveGame() {
        if (!gameStarted || model == null) {
            return;
        }

        long elapsed = model.isSolved() && lastWinTimeMs >= 0
                ? lastWinTimeMs
                : Math.max(0, System.currentTimeMillis() - model.getStartTime());
        store.saveGame(model, elapsed);
    }

    private boolean loadGame() {
        SaveManager.SaveData data = store.loadSavedGame();
        if (data == null) {
            return false;
        }

        attachModel(new GameModel(data.size));
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = false;
        currentResult = null;
        hintActive = false;
        gameStarted = true;
        return true;
    }

    private boolean hasSavedGame() {
        return store.hasSavedGame();
    }

    private void clearSavedGame() {
        store.clearSavedGame();
    }

    private AndroidGameStore.Best getBest(int size) {
        return store.getBest(size);
    }

    private void recordBest(int size, int moves, long timeMs) {
        store.recordBestIfBetter(size, moves, timeMs);
    }

    private void clearRecords() {
        store.clearRecords();
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
        AndroidGameStore.Best best = getBest(size);
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
        AndroidGameStore.Best best = getBest(result.size);
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
        return store.isHapticEnabled();
    }

    private boolean isReducedMotionEnabled() {
        return store.isReducedMotionEnabled();
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
        if (currentScreen == Screen.TUTORIAL) {
            updateTutorialStatus();
        } else {
            clearGameHint();
            updateStatus();
        }
    }

    /**
     * Updates the HUD after a single empty-tile move.
     *
     * @param dir direction the empty tile moved
     */
    @Override
    public void onMove(Direction dir) {
        if (currentScreen == Screen.TUTORIAL) {
            updateTutorialStatus();
        } else {
            clearGameHint();
            updateStatus();
        }
    }

    /**
     * Updates the HUD after a whole-line slide and advances tutorial practice when needed.
     *
     * @param dir direction the empty tile moved
     * @param steps number of cells the empty tile moved
     */
    @Override
    public void onLineMove(Direction dir, int steps) {
        if (currentScreen == Screen.TUTORIAL) {
            handleTutorialLineMove(steps);
        } else {
            clearGameHint();
            updateStatus();
        }
    }

    /**
     * Records a pending win and defers the results screen until animation ends.
     *
     * @param moves final move count reported by the model
     * @param timeMs elapsed play time in milliseconds
     */
    @Override
    public void onGameWon(int moves, long timeMs) {
        if (currentScreen == Screen.TUTORIAL) {
            handleTutorialWin();
            return;
        }
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
        AndroidGameStore.Best previousBest = getBest(win.size);
        boolean newBest = !win.assisted && AndroidGameStore.isBetterRecord(previousBest, win.moves, win.timeMs);
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
        final AndroidGameStore.Best previousBest;

        GameResult(int size, int moves, long timeMs, boolean assisted, boolean newBest,
                AndroidGameStore.Best previousBest) {
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
