package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_BACKGROUND;
import static com.klotski.android.AndroidUi.COLOR_MUTED_TEXT;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PANEL_LIGHT;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Button> commandButtons = new ArrayList<>();

    private AndroidUi ui;
    private AndroidLearningContent learningContent;
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
        ui = new AndroidUi(this, commandButtons);
        learningContent = new AndroidLearningContent(this, ui);
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
        AndroidActivityState.save(outState, currentScreen, infoReturnScreen, gameStarted,
                onboardingPage, tutorialStep, currentResult);
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
        if (AndroidNavigation.shouldSaveBeforeBack(currentScreen)) {
            saveGame();
        }
        switch (AndroidNavigation.backTarget(currentScreen, infoReturnScreen, gameStarted)) {
            case GAME -> showGameScreen();
            case FINISH -> finish();
            case HOME -> showHomeScreen();
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
        AndroidActivityState.Snapshot savedState =
                AndroidActivityState.restore(savedInstanceState, TUTORIAL_FIRST_MOVE);
        Screen savedScreen = savedState.screen;
        Screen savedReturnScreen = savedState.infoReturnScreen;
        boolean savedGameStarted = savedState.gameStarted;
        currentResult = savedState.result;

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
            case ONBOARDING -> showOnboardingScreen(savedState.onboardingPage);
            case TUTORIAL -> showTutorialScreen(savedState.tutorialStep);
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

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.home_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        ui.addScreenHeader(screen.content, getString(R.string.app_name), getString(R.string.home_tagline));

        TextView summary = ui.createText(getString(R.string.home_summary), 16, COLOR_MUTED_TEXT, Typeface.NORMAL);
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams summaryParams = ui.fullWidthParams();
        summaryParams.setMargins(0, ui.dp(8), 0, ui.dp(24));
        screen.content.addView(summary, summaryParams);

        boolean hasSave = hasSavedGame();
        if (hasSave) {
            Button continueButton = ui.addWideButton(screen.content, R.string.home_continue, COLOR_PRIMARY,
                    v -> continueSavedGame());
            continueButton.setId(R.id.home_continue_button);
        }
        Button newGameButton = ui.addWideButton(screen.content, hasSave ? R.string.home_new_game : R.string.home_play,
                hasSave ? COLOR_PANEL_LIGHT : COLOR_PRIMARY, v -> {
                    if (shouldShowOnboarding()) {
                        showOnboardingScreen(0);
                    } else {
                        showModeSelectScreen();
                    }
                });
        newGameButton.setId(R.id.home_new_game_button);
        Button onboardingButton = ui.addWideButton(screen.content, R.string.home_beginner_guide, COLOR_PANEL,
                v -> showOnboardingScreen(0));
        onboardingButton.setId(R.id.home_onboarding_button);
        Button tutorialButton = ui.addWideButton(screen.content, R.string.home_tutorial, COLOR_PRIMARY,
                v -> startGuidedTutorial());
        tutorialButton.setId(R.id.home_tutorial_button);
        Button howToButton = ui.addWideButton(screen.content, R.string.home_how_to_play, COLOR_PANEL,
                v -> showHowToScreen(Screen.HOME));
        howToButton.setId(R.id.home_how_to_play_button);
        Button settingsButton = ui.addWideButton(screen.content, R.string.home_settings, COLOR_PANEL,
                v -> showSettingsScreen(Screen.HOME));
        settingsButton.setId(R.id.home_settings_button);
        Button recordsButton = ui.addWideButton(screen.content, R.string.home_records, COLOR_PANEL,
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

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.onboarding_root);
        ui.addScreenHeader(screen.content, getString(R.string.onboarding_title),
                getString(R.string.onboarding_subtitle));

        TextView progress = ui.createText(getString(R.string.onboarding_progress,
                onboardingPage + 1, ONBOARDING_PAGE_COUNT), 14, COLOR_ACCENT, Typeface.BOLD);
        progress.setId(R.id.onboarding_progress_text);
        progress.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = ui.fullWidthParams();
        progressParams.setMargins(0, 0, 0, ui.dp(12));
        screen.content.addView(progress, progressParams);

        addOnboardingPage(screen.content);

        if (onboardingPage < ONBOARDING_PAGE_COUNT - 1) {
            Button nextButton = ui.addWideButton(screen.content, R.string.onboarding_next, COLOR_PRIMARY,
                    v -> showOnboardingScreen(onboardingPage + 1));
            nextButton.setId(R.id.onboarding_next_button);
        } else {
            Button tutorialButton = ui.addWideButton(screen.content, R.string.onboarding_start_tutorial, COLOR_PRIMARY,
                    v -> startGuidedTutorial());
            tutorialButton.setId(R.id.onboarding_tutorial_button);
            Button startButton = ui.addWideButton(screen.content, R.string.onboarding_start_3, COLOR_PANEL,
                    v -> startFirstPuzzle());
            startButton.setId(R.id.onboarding_start_3_button);
        }

        if (onboardingPage > 0) {
            Button backButton = ui.addWideButton(screen.content, R.string.onboarding_back, COLOR_PANEL,
                    v -> showOnboardingScreen(onboardingPage - 1));
            backButton.setId(R.id.onboarding_back_button);
        }

        Button skipButton = ui.addWideButton(screen.content, R.string.onboarding_skip, COLOR_PANEL,
                v -> skipOnboarding());
        skipButton.setId(R.id.onboarding_skip_button);

        setContentView(screen.root);
    }

    private void showModeSelectScreen() {
        currentScreen = Screen.MODE_SELECT;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.mode_root);
        ui.addScreenHeader(screen.content, getString(R.string.mode_title), getString(R.string.mode_subtitle));
        addModeRow(screen.content, 3, R.string.mode_easy, R.string.mode_easy_detail);
        addModeRow(screen.content, 4, R.string.mode_classic, R.string.mode_classic_detail);
        addModeRow(screen.content, 5, R.string.mode_expert, R.string.mode_expert_detail);
        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home, COLOR_PANEL, v -> showHomeScreen());
        homeButton.setId(R.id.mode_home_button);

        setContentView(screen.root);
    }

    private void showHowToScreen(Screen returnScreen) {
        currentScreen = Screen.HOW_TO_PLAY;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.how_root);
        ui.addScreenHeader(screen.content, getString(R.string.how_title), getString(R.string.how_subtitle));
        learningContent.addLearningExample(screen.content, R.id.how_goal_example,
                R.string.how_goal_title, R.string.how_goal_body,
                new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 0}}, new int[] {});
        learningContent.addLearningExample(screen.content, R.id.how_tap_example,
                R.string.how_tap_title, R.string.how_tap_body,
                new int[][] {{1, 2, 3}, {4, 5, 0}, {7, 8, 6}}, new int[] {5});
        learningContent.addLearningExample(screen.content, R.id.how_line_example,
                R.string.how_line_title, R.string.how_line_body,
                new int[][] {{1, 2, 3}, {0, 4, 5}, {7, 8, 6}}, new int[] {4, 5});
        learningContent.addInstruction(screen.content, R.string.how_swipe_title, R.string.how_swipe_body);
        learningContent.addInstruction(screen.content, R.string.how_tools_title, R.string.how_tools_body);
        learningContent.addInstruction(screen.content, R.string.how_records_title, R.string.how_records_body);
        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
        backButton.setId(R.id.how_back_button);

        setContentView(screen.root);
    }

    private void showRecordsScreen(Screen returnScreen) {
        currentScreen = Screen.RECORDS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.records_root);
        ui.addScreenHeader(screen.content, getString(R.string.records_title), getString(R.string.records_subtitle));
        addRecordRow(screen.content, 3, R.string.mode_easy);
        addRecordRow(screen.content, 4, R.string.mode_classic);
        addRecordRow(screen.content, 5, R.string.mode_expert);
        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
        backButton.setId(R.id.records_back_button);

        setContentView(screen.root);
    }

    private void showSettingsScreen(Screen returnScreen) {
        currentScreen = Screen.SETTINGS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.settings_root);
        ui.addScreenHeader(screen.content, getString(R.string.settings_title), getString(R.string.settings_subtitle));
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
        Button resetSaveButton = ui.addWideButton(screen.content, R.string.settings_reset_save, COLOR_PANEL,
                v -> confirmResetSave());
        resetSaveButton.setId(R.id.settings_reset_save_button);
        Button resetRecordsButton = ui.addWideButton(screen.content, R.string.settings_reset_records, COLOR_PANEL,
                v -> confirmResetRecords());
        resetRecordsButton.setId(R.id.settings_reset_records_button);
        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PRIMARY, v -> returnFromInfoScreen());
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

        ScreenLayout screen = ui.createScreenLayout();
        screen.root.setId(R.id.results_root);
        screen.content.setGravity(Gravity.CENTER_HORIZONTAL);
        ui.addScreenHeader(screen.content, getString(R.string.results_title),
                getString(currentResult.assisted
                        ? R.string.results_assisted_subtitle
                        : R.string.results_player_subtitle));

        TextView size = ui.createText(getString(R.string.results_size_format,
                currentResult.size, currentResult.size), 18, Color.WHITE, Typeface.BOLD);
        size.setId(R.id.results_size_text);
        size.setGravity(Gravity.CENTER);
        screen.content.addView(size, ui.fullWidthParams());

        TextView stats = ui.createText(getString(R.string.results_stats_format,
                formatMoves(currentResult.moves), currentResult.timeMs / 1000),
                24, Color.WHITE, Typeface.BOLD);
        stats.setId(R.id.results_stats_text);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = ui.fullWidthParams();
        statsParams.setMargins(0, ui.dp(10), 0, ui.dp(10));
        screen.content.addView(stats, statsParams);

        TextView record = ui.createText(resultRecordText(currentResult), 16,
                currentResult.newBest ? COLOR_ACCENT : COLOR_MUTED_TEXT, Typeface.BOLD);
        record.setId(R.id.results_record_text);
        record.setGravity(Gravity.CENTER);
        record.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams recordParams = ui.fullWidthParams();
        recordParams.setMargins(0, 0, 0, ui.dp(22));
        screen.content.addView(record, recordParams);

        Button playAgainButton = ui.addWideButton(screen.content, R.string.results_play_again, COLOR_PRIMARY,
                v -> beginNewGame(currentResult.size));
        playAgainButton.setId(R.id.results_play_again_button);
        Button newSizeButton = ui.addWideButton(screen.content, R.string.results_new_size, COLOR_PANEL,
                v -> {
                    saveGame();
                    showModeSelectScreen();
                });
        newSizeButton.setId(R.id.results_new_size_button);
        Button homeButton = ui.addWideButton(screen.content, R.string.nav_home, COLOR_PANEL,
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
        root.setPadding(ui.dp(12), ui.systemBarHeight("status_bar_height") + ui.dp(12),
                ui.dp(12), ui.systemBarHeight("navigation_bar_height") + ui.dp(12));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(topBar, ui.fullWidthParams());

        Button homeButton = ui.createButton(getString(R.string.nav_home), COLOR_PANEL);
        homeButton.setId(R.id.tutorial_home_button);
        homeButton.setOnClickListener(v -> showHomeScreen());
        topBar.addView(homeButton, ui.fixedButtonParams(88));

        TextView title = ui.createText(getString(R.string.tutorial_title), 20, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button startButton = ui.createButton(getString(R.string.onboarding_start_3), COLOR_PANEL);
        startButton.setId(R.id.tutorial_start_game_button);
        startButton.setOnClickListener(v -> beginNewGame(3));
        topBar.addView(startButton, ui.fixedButtonParams(104));

        LinearLayout lesson = new LinearLayout(this);
        lesson.setOrientation(LinearLayout.VERTICAL);
        lesson.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        lesson.setBackground(ui.makePanelBackground(COLOR_PANEL));
        LinearLayout.LayoutParams lessonParams = ui.fullWidthParams();
        lessonParams.setMargins(0, ui.dp(10), 0, ui.dp(10));
        root.addView(lesson, lessonParams);

        tutorialProgressText = ui.createText("", 14, COLOR_ACCENT, Typeface.BOLD);
        tutorialProgressText.setId(R.id.tutorial_progress_text);
        lesson.addView(tutorialProgressText, ui.fullWidthParams());

        tutorialInstructionText = ui.createText("", 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        tutorialInstructionText.setId(R.id.tutorial_instruction_text);
        tutorialInstructionText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams instructionParams = ui.fullWidthParams();
        instructionParams.setMargins(0, ui.dp(6), 0, 0);
        lesson.addView(tutorialInstructionText, instructionParams);

        tutorialStatusText = ui.createText("", 15, Color.WHITE, Typeface.BOLD);
        tutorialStatusText.setId(R.id.tutorial_status_text);
        tutorialStatusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = ui.fullWidthParams();
        statusParams.setMargins(0, 0, 0, ui.dp(8));
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
        actions.setPadding(0, ui.dp(10), 0, 0);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, ui.fullWidthParams());

        Button restartButton = ui.createButton(getString(R.string.tutorial_restart_lesson), COLOR_PANEL_LIGHT);
        restartButton.setOnClickListener(v -> {
            if (canAcceptTutorialCommand()) {
                showTutorialScreen(tutorialStep == TUTORIAL_COMPLETE ? TUTORIAL_FIRST_MOVE : tutorialStep);
            }
        });
        restartButton.setId(R.id.tutorial_restart_button);
        actions.addView(restartButton, new LinearLayout.LayoutParams(0, ui.dp(48), 1f));

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
        root.setPadding(ui.dp(12), ui.systemBarHeight("status_bar_height") + ui.dp(12),
                ui.dp(12), ui.systemBarHeight("navigation_bar_height") + ui.dp(12));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(topBar, ui.fullWidthParams());

        Button homeButton = ui.createButton(getString(R.string.nav_home), COLOR_PANEL);
        homeButton.setId(R.id.game_home_button);
        homeButton.setContentDescription(getString(R.string.accessibility_game_home));
        homeButton.setOnClickListener(v -> {
            if (canAcceptCommand()) {
                saveGame();
                showHomeScreen();
            }
        });
        commandButtons.add(homeButton);
        topBar.addView(homeButton, ui.fixedButtonParams(88));

        gameTitleText = ui.createText("", 20, Color.WHITE, Typeface.BOLD);
        gameTitleText.setId(R.id.game_title_text);
        gameTitleText.setGravity(Gravity.CENTER);
        topBar.addView(gameTitleText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button menuButton = ui.createButton(getString(R.string.game_menu), COLOR_PANEL);
        menuButton.setId(R.id.game_menu_button);
        menuButton.setContentDescription(getString(R.string.accessibility_game_menu));
        menuButton.setOnClickListener(v -> {
            if (canAcceptCommand()) {
                showPauseMenu();
            }
        });
        commandButtons.add(menuButton);
        topBar.addView(menuButton, ui.fixedButtonParams(88));

        statusText = ui.createText("", 15, Color.WHITE, Typeface.NORMAL);
        statusText.setId(R.id.game_status_text);
        statusText.setGravity(Gravity.CENTER);
        statusText.setSingleLine(false);
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams statusParams = ui.fullWidthParams();
        statusParams.setMargins(0, ui.dp(8), 0, ui.dp(8));
        root.addView(statusText, statusParams);

        ensureBoardView();
        boardView.clearHighlights();
        ViewParentRemover.removeFromParent(boardView);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, ui.dp(10), 0, 0);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, ui.fullWidthParams());

        Button undoButton = ui.addGameButton(actions, R.string.button_undo, v -> {
            if (canAcceptCommand()) {
                clearGameHint();
                model.undo();
                performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
                updateStatus();
            }
        });
        undoButton.setId(R.id.game_undo_button);
        undoButton.setContentDescription(getString(R.string.accessibility_game_undo));
        Button restartButton = ui.addGameButton(actions, R.string.button_restart, v -> {
            if (canAcceptCommand()) {
                restartCurrentGame();
            }
        });
        restartButton.setId(R.id.game_restart_button);
        restartButton.setContentDescription(getString(R.string.accessibility_game_restart));
        Button assistButton = ui.addGameButton(actions, R.string.game_assist, v -> {
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
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));
        row.setClickable(true);
        row.setOnClickListener(v -> beginNewGame(size));

        TextView title = ui.createText(getString(R.string.mode_card_title, size, size), 22, Color.WHITE, Typeface.BOLD);
        TextView difficulty = ui.createText(getString(difficultyResId), 15, COLOR_ACCENT, Typeface.BOLD);
        TextView detail = ui.createText(getString(detailResId), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);
        TextView best = ui.createText(getString(R.string.mode_best_label, formatBestForCard(size)),
                14, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, ui.fullWidthParams());
        row.addView(difficulty, ui.fullWidthParams());
        LinearLayout.LayoutParams detailParams = ui.fullWidthParams();
        detailParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(detail, detailParams);
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(8), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    private void addRecordRow(LinearLayout parent, int size, int difficultyResId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));

        TextView title = ui.createText(getString(R.string.records_row_title, size, size, getString(difficultyResId)),
                20, Color.WHITE, Typeface.BOLD);
        TextView best = ui.createText(formatBestForCard(size), 15, COLOR_MUTED_TEXT, Typeface.NORMAL);

        row.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bestParams = ui.fullWidthParams();
        bestParams.setMargins(0, ui.dp(6), 0, 0);
        row.addView(best, bestParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
        parent.addView(row, rowParams);
    }

    private void addSettingsSwitch(LinearLayout parent, int switchId, int titleResId, int bodyResId,
            boolean checked, SettingChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        row.setBackground(ui.makePanelBackground(COLOR_PANEL));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = ui.createText(getString(titleResId), 18, Color.WHITE, Typeface.BOLD);
        TextView body = ui.createText(getString(bodyResId), 14, COLOR_MUTED_TEXT, Typeface.NORMAL);
        body.setLineSpacing(0, 1.12f);
        copy.addView(title, ui.fullWidthParams());
        LinearLayout.LayoutParams bodyParams = ui.fullWidthParams();
        bodyParams.setMargins(0, ui.dp(5), 0, 0);
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
        switchParams.setMargins(ui.dp(16), 0, 0, 0);
        row.addView(toggle, switchParams);

        LinearLayout.LayoutParams rowParams = ui.fullWidthParams();
        rowParams.setMargins(0, 0, 0, ui.dp(12));
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
        learningContent.addInstruction(parent, titleResId, bodyResId);
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
}
