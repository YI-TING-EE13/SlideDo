package com.klotski.android;

import static com.klotski.android.AndroidUi.COLOR_ACCENT;
import static com.klotski.android.AndroidUi.COLOR_BACKGROUND;
import static com.klotski.android.AndroidUi.COLOR_PANEL;
import static com.klotski.android.AndroidUi.COLOR_PRIMARY;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputType;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.ContinuousChallenge;
import com.klotski.core.DailyCalendarMonth;
import com.klotski.core.DailyChallenge;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.GameObserver;
import com.klotski.core.IdaStarSolver;
import com.klotski.core.PuzzleIdentity;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;
import com.klotski.core.Solver;
import com.klotski.core.StrategicHint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Android entry point for SlideDo.
 * <p>
 * The activity owns the mobile app flow and wires the shared {@link GameModel}
 * to Android screens, local persistence, best-record tracking, solver actions,
 * completion results, and offline personal progress. Gameplay rules remain in
 * the shared core so Android behavior stays aligned with the desktop Swing
 * reference.
 * </p>
 */
public class MainActivity extends Activity implements GameObserver {
    private static final int REQUEST_EXPORT_BACKUP = 1401;
    private static final int REQUEST_IMPORT_BACKUP = 1402;
    private static final String STATE_CONTINUOUS_MODE = "continuous_mode";
    private static final DateTimeFormatter BACKUP_FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
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
    private AndroidMotion motion;
    private AndroidLearningContent learningContent;
    private AndroidHomeScreen homeScreen;
    private AndroidDailyCalendarScreen dailyCalendarScreen;
    private AndroidFavoritesScreen favoritesScreen;
    private AndroidModeSelectScreen modeSelectScreen;
    private AndroidRecordsScreen recordsScreen;
    private AndroidTrendsScreen trendsScreen;
    private AndroidSettingsScreen settingsScreen;
    private AndroidResultsScreen resultsScreen;
    private AndroidTutorialScreen tutorialScreen;
    private AndroidGameScreen gameScreen;
    private AndroidGameStore store;
    private AndroidSoundFeedback soundFeedback;
    private GameModel model;
    private KlotskiView boardView;
    private TextView statusText;
    private TextView gameTitleText;
    private TextView tutorialProgressText;
    private TextView tutorialInstructionText;
    private TextView tutorialStatusText;
    private PendingWin pendingWin;
    private GameResult currentResult;
    private String activeDailyDateId;
    private String activeFavoriteId;
    private ContinuousChallenge activeContinuousChallenge;
    private DailyCalendarMonth dailyCalendarMonth;
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
    private int strategicHintTile = -1;
    private boolean renderingScreenChange;
    private boolean screenTransitionRunning;
    private boolean activityResumed;
    private boolean saveSuppressedAfterReset;
    private boolean gameNavigationPending = true;
    private int timerPausingDialogCount;
    private long lastWinTimeMs = -1;

    /**
     * Creates the Android activity instance used by the platform launcher.
     */
    public MainActivity() {
    }

    /**
     * Applies the stored app language before Android inflates or creates UI resources.
     *
     * @param newBase platform-provided base context
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        String languageTag = new AndroidGameStore(newBase).getLanguageTag();
        super.attachBaseContext(AndroidAppLocale.wrap(newBase, languageTag));
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
        store = new AndroidGameStore(this);
        ui = new AndroidUi(this, commandButtons, store.getVisualTheme());
        soundFeedback = new AndroidSoundFeedback(store.isSoundEnabled());
        motion = new AndroidMotion(this);
        learningContent = new AndroidLearningContent(this, ui);
        homeScreen = new AndroidHomeScreen(this, ui);
        dailyCalendarScreen = new AndroidDailyCalendarScreen(this, ui);
        favoritesScreen = new AndroidFavoritesScreen(this, ui);
        modeSelectScreen = new AndroidModeSelectScreen(this, ui);
        recordsScreen = new AndroidRecordsScreen(this, ui);
        trendsScreen = new AndroidTrendsScreen(this, ui);
        settingsScreen = new AndroidSettingsScreen(this, ui);
        resultsScreen = new AndroidResultsScreen(this, ui);
        tutorialScreen = new AndroidTutorialScreen(this, ui);
        gameScreen = new AndroidGameScreen(this, ui, commandButtons);
        applyLegacySystemBarColors();

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
                onboardingPage, tutorialStep, currentResult, activeDailyDateId,
                activeFavoriteId,
                dailyCalendarMonth == null ? null : dailyCalendarMonth.getMonthId());
        outState.putBoolean(STATE_CONTINUOUS_MODE, activeContinuousChallenge != null);
        super.onSaveInstanceState(outState);
    }

    /**
     * Persists the current board when Android backgrounds the activity.
     */
    @Override
    protected void onPause() {
        activityResumed = false;
        syncGameTimerState();
        super.onPause();
        saveGame();
    }

    /**
     * Resumes active-play timing only when the game screen is interactive.
     */
    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        syncGameTimerState();
    }

    /**
     * Completes owner-selected Android document export and import operations.
     * A selected import is parsed before a confirmation dialog can replace
     * existing personal data.
     *
     * @param requestCode request identifier supplied when opening the picker
     * @param resultCode Android activity result code
     * @param data picker result containing the selected document URI
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri documentUri = data.getData();
        try {
            if (requestCode == REQUEST_EXPORT_BACKUP) {
                writePersonalDataBackup(documentUri);
                Toast.makeText(this, R.string.toast_backup_exported, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_IMPORT_BACKUP) {
                String archive = readPersonalDataBackup(documentUri);
                AndroidPersonalDataArchive.decode(archive);
                confirmImportPersonalData(archive);
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            showBackupError();
        }
    }

    /**
     * Stops the status ticker when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        unregisterBackHandler();
        handler.removeCallbacks(ticker);
        if (soundFeedback != null) {
            soundFeedback.release();
        }
        super.onDestroy();
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
            int backgroundColor = ui.resolveColor(COLOR_BACKGROUND);
            getWindow().setStatusBarColor(backgroundColor);
            getWindow().setNavigationBarColor(backgroundColor);
        }
    }

    private boolean deferScreenChange(Runnable renderDestination) {
        if (renderingScreenChange) {
            return false;
        }
        if (screenTransitionRunning) {
            return true;
        }

        View currentRoot = currentContentRoot();
        if (currentRoot == null || isReducedMotionEnabled()) {
            return false;
        }

        screenTransitionRunning = true;
        motion.animateExit(currentRoot, false, () -> {
            if (isFinishing() || isDestroyed()) {
                screenTransitionRunning = false;
                return;
            }
            renderingScreenChange = true;
            try {
                renderDestination.run();
            } finally {
                renderingScreenChange = false;
                screenTransitionRunning = false;
            }
        });
        return true;
    }

    private void presentContentView(View root) {
        setContentView(root);
        motion.animateEntrance(root, isReducedMotionEnabled());
    }

    private View currentContentRoot() {
        View content = findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return null;
        }
        ViewGroup contentGroup = (ViewGroup) content;
        return contentGroup.getChildCount() == 0 ? null : contentGroup.getChildAt(0);
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

    private void pauseGameForNavigation() {
        gameNavigationPending = true;
        syncGameTimerState();
    }

    private void syncGameTimerState() {
        if (model == null) {
            return;
        }
        boolean shouldRun = activityResumed
                && gameStarted
                && currentScreen == Screen.GAME
                && !gameNavigationPending
                && timerPausingDialogCount == 0;
        if (shouldRun) {
            model.resumeTimer();
        } else {
            model.pauseTimer();
        }
    }

    private void showTimerPausingDialog(AlertDialog dialog) {
        timerPausingDialogCount++;
        syncGameTimerState();
        dialog.setOnDismissListener(ignored -> {
            if (timerPausingDialogCount > 0) {
                timerPausingDialogCount--;
            }
            syncGameTimerState();
        });
        dialog.show();
    }

    private boolean restoreAppScreen(Bundle savedInstanceState) {
        AndroidActivityState.Snapshot savedState =
                AndroidActivityState.restore(savedInstanceState, TUTORIAL_FIRST_MOVE);
        Screen savedScreen = savedState.screen;
        Screen savedReturnScreen = savedState.infoReturnScreen;
        boolean savedGameStarted = savedState.gameStarted;
        currentResult = savedState.result;
        activeDailyDateId = savedState.activeDailyDateId;
        activeFavoriteId = savedState.activeFavoriteId;
        dailyCalendarMonth = restoreDailyCalendarMonth(savedState.dailyCalendarMonthId);
        boolean savedContinuousMode = savedInstanceState.getBoolean(
                STATE_CONTINUOUS_MODE, false);

        if (savedScreen == Screen.GAME) {
            if (savedGameStarted && (savedContinuousMode
                    ? loadContinuousGame() : loadGame())) {
                showGameScreen();
                return true;
            }
            gameStarted = false;
            return false;
        }

        if (savedScreen == Screen.RESULTS) {
            GameResult restoredResult = currentResult;
            boolean loaded = restoredResult != null && savedGameStarted
                    && (savedContinuousMode
                            ? loadContinuousGame()
                            : restoredResult.favoriteId != null
                            ? loadFavoriteGame(restoredResult.favoriteId)
                            : restoredResult.dailyDateId != null
                            ? loadDailyGame(restoredResult.dailyDateId)
                            : loadGame(restoredResult.size));
            if (loaded) {
                currentResult = restoredResult;
                showResultsScreen();
                return true;
            }
            return false;
        }

        if ((savedScreen == Screen.HOW_TO_PLAY || savedScreen == Screen.RECORDS
                || savedScreen == Screen.TRENDS
                || savedScreen == Screen.SETTINGS)
                && savedReturnScreen == Screen.GAME && savedGameStarted
                && !(savedContinuousMode ? loadContinuousGame() : loadGame())) {
            savedReturnScreen = Screen.HOME;
            gameStarted = false;
        }

        switch (savedScreen) {
            case ONBOARDING -> showOnboardingScreen(savedState.onboardingPage);
            case TUTORIAL -> showTutorialScreen(savedState.tutorialStep);
            case DAILY_CALENDAR -> showDailyCalendarScreen();
            case FAVORITES -> showFavoritesScreen();
            case MODE_SELECT -> showModeSelectScreen();
            case HOW_TO_PLAY -> showHowToScreen(savedReturnScreen);
            case RECORDS -> showRecordsScreen(savedReturnScreen);
            case TRENDS -> showTrendsScreen();
            case SETTINGS -> showSettingsScreen(savedReturnScreen);
            case HOME -> showHomeScreen();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void showHomeScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showHomeScreen)) {
            return;
        }
        currentScreen = Screen.HOME;
        infoReturnScreen = Screen.HOME;
        tutorialAdvancePending = false;
        hintActive = false;
        strategicHintTile = -1;
        statusText = null;
        gameTitleText = null;
        tutorialProgressText = null;
        tutorialInstructionText = null;
        tutorialStatusText = null;
        commandButtons.clear();

        DailyChallenge today = DailyChallenge.forDate(LocalDate.now());
        AndroidGameStore.DailyProgress dailyProgress = store.getDailyProgress(today.getDateId());
        AndroidGameStore.SaveMetadata dailySave = store.getDailySaveMetadata(today.getDateId());
        AndroidGameStore.ContinuousGame continuousGame = store.loadContinuousGame();
        if (continuousGame != null && continuousGame.challenge.isComplete()) {
            store.clearContinuousGame();
            continuousGame = null;
        }
        AndroidHomeScreen.DailyStatus dailyStatus = new AndroidHomeScreen.DailyStatus(
                today.getDateId(), dailySave != null && !dailySave.solved,
                dailyProgress.completedToday, dailyProgress.currentStreak, dailyProgress.bestStreak);
        AndroidHomeScreen.ContinuousStatus continuousStatus = continuousGame == null
                ? null
                : new AndroidHomeScreen.ContinuousStatus(true,
                        continuousGame.challenge.getCurrentPuzzleNumber(),
                        continuousGame.challenge.getTargetPuzzles(),
                        continuousGame.game.size, continuousGame.game.difficulty);
        ScreenLayout screen = homeScreen.build(store.getAllSaveMetadata(), dailyStatus,
                continuousStatus, store.getFavoritePuzzles().length,
                new AndroidHomeScreen.HomeActions() {
            @Override
            public void onDailyChallenge() {
                LocalDate today = LocalDate.now();
                dailyCalendarMonth = DailyCalendarMonth.showing(YearMonth.from(today), today);
                showDailyCalendarScreen();
            }

            @Override
            public void onContinuousChallenge() {
                showContinuousChallengeDialog();
            }

            @Override
            public void onContinue() {
                continueSavedGameFromHome();
            }

            @Override
            public void onPlay() {
                if (shouldShowOnboarding()) {
                    showOnboardingScreen(0);
                } else {
                    showModeSelectScreen();
                }
            }

            @Override
            public void onBeginnerGuide() {
                showOnboardingScreen(0);
            }

            @Override
            public void onPracticeTutorial() {
                startGuidedTutorial();
            }

            @Override
            public void onHowToPlay() {
                showHowToScreen(Screen.HOME);
            }

            @Override
            public void onFavorites() {
                showFavoritesScreen();
            }

            @Override
            public void onTrends() {
                showTrendsScreen();
            }

            @Override
            public void onSettings() {
                showSettingsScreen(Screen.HOME);
            }

            @Override
            public void onRecords() {
                showRecordsScreen(Screen.HOME);
            }
        });

        presentContentView(screen.root);
    }

    private void showContinuousChallengeDialog() {
        AndroidGameStore.ContinuousGame saved = store.loadContinuousGame();
        boolean resumable = saved != null && !saved.challenge.isComplete();
        int[] targets = {3, 5, 10};
        String[] labels = new String[targets.length + (resumable ? 1 : 0)];
        int offset = resumable ? 1 : 0;
        if (resumable) {
            labels[0] = getString(R.string.continuous_resume_action,
                    saved.challenge.getCurrentPuzzleNumber(),
                    saved.challenge.getTargetPuzzles());
        }
        for (int index = 0; index < targets.length; index++) {
            labels[index + offset] = getString(
                    R.string.continuous_start_action, targets[index]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.continuous_dialog_title)
                .setItems(labels, (selectedDialog, which) -> {
                    if (resumable && which == 0) {
                        resumeContinuousChallenge();
                        return;
                    }
                    int target = targets[which - offset];
                    if (resumable) {
                        confirmReplaceContinuousChallenge(target);
                    } else {
                        showContinuousScopeDialog(target);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void confirmReplaceContinuousChallenge(int target) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.continuous_replace_title)
                .setMessage(R.string.continuous_replace_message)
                .setPositiveButton(android.R.string.ok,
                        (selectedDialog, which) -> showContinuousScopeDialog(target))
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void showContinuousScopeDialog(int target) {
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        String[] labels = new String[9];
        for (int size = 3; size <= 5; size++) {
            for (PuzzleDifficulty difficulty : difficulties) {
                int index = (size - 3) * difficulties.length + difficulty.ordinal();
                labels[index] = getString(R.string.trends_scope,
                        size, size, difficultyName(difficulty));
            }
        }
        int selected = (store.getLastSize(4) - 3) * difficulties.length
                + store.getLastDifficulty().ordinal();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.continuous_scope_title)
                .setSingleChoiceItems(labels, selected, (selectedDialog, which) -> {
                    selectedDialog.dismiss();
                    startContinuousChallenge(
                            3 + which / difficulties.length,
                            difficulties[which % difficulties.length], target);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void confirmEndContinuousChallenge() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.continuous_end_title)
                .setMessage(R.string.continuous_end_message)
                .setPositiveButton(R.string.continuous_end_session, (selectedDialog, which) -> {
                    store.clearContinuousGame();
                    activeContinuousChallenge = null;
                    gameStarted = false;
                    currentResult = null;
                    showHomeScreen();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private DailyCalendarMonth restoreDailyCalendarMonth(String monthId) {
        LocalDate today = LocalDate.now();
        if (monthId != null) {
            try {
                return DailyCalendarMonth.fromMonthId(monthId, today);
            } catch (RuntimeException ignored) {
                // A malformed or obsolete Activity-state value falls back safely.
            }
        }
        return DailyCalendarMonth.showing(YearMonth.from(today), today);
    }

    private void showFavoritesScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showFavoritesScreen)) {
            return;
        }
        currentScreen = Screen.FAVORITES;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = favoritesScreen.build(store.getFavoritePuzzles(),
                new AndroidFavoritesScreen.FavoriteActions() {
                    @Override
                    public void onReplay(AndroidGameStore.FavoritePuzzle favorite) {
                        startFavoritePuzzle(favorite);
                    }

                    @Override
                    public void onRename(AndroidGameStore.FavoritePuzzle favorite) {
                        showFavoriteNameDialog(favorite);
                    }

                    @Override
                    public void onRemove(AndroidGameStore.FavoritePuzzle favorite) {
                        confirmRemoveFavorite(favorite);
                    }

                    @Override
                    public void onBack() {
                        showHomeScreen();
                    }
                });
        presentContentView(screen.root);
    }

    private void showFavoriteNameDialog(AndroidGameStore.FavoritePuzzle existing) {
        if (model == null && existing == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(40)});
        input.setHint(R.string.favorite_name_hint);
        String defaultLabel = existing == null
                ? getString(R.string.favorite_default_name, model.getSize(), model.getSize(),
                        difficultyName(model.getDifficulty()))
                : existing.label;
        input.setText(defaultLabel);
        input.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null
                        ? R.string.favorite_dialog_title : R.string.favorite_rename_title)
                .setMessage(R.string.favorite_dialog_message)
                .setView(input)
                .setPositiveButton(R.string.favorite_save_action, (selectedDialog, which) -> {
                    AndroidGameStore.FavoritePuzzle saved = existing == null
                            ? store.saveFavorite(model, input.getText().toString(),
                                    System.currentTimeMillis())
                            : renameFavorite(existing, input.getText().toString());
                    if (saved == null) {
                        Toast.makeText(this, R.string.toast_favorite_name_required,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.toast_favorite_saved,
                                Toast.LENGTH_SHORT).show();
                        if (currentScreen == Screen.FAVORITES) {
                            showFavoritesScreen();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private AndroidGameStore.FavoritePuzzle renameFavorite(
            AndroidGameStore.FavoritePuzzle favorite, String label) {
        GameModel favoriteModel = favorite.createGame();
        return store.saveFavorite(favoriteModel, label, favorite.createdAt);
    }

    private void confirmRemoveFavorite(AndroidGameStore.FavoritePuzzle favorite) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_remove_title)
                .setMessage(getString(R.string.favorite_remove_message, favorite.label))
                .setPositiveButton(R.string.favorites_remove, (selectedDialog, which) -> {
                    if (store.removeFavorite(favorite.id)) {
                        Toast.makeText(this, R.string.toast_favorite_removed,
                                Toast.LENGTH_SHORT).show();
                    }
                    showFavoritesScreen();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void showDailyCalendarScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showDailyCalendarScreen)) {
            return;
        }
        LocalDate today = LocalDate.now();
        YearMonth selectedMonth = dailyCalendarMonth == null
                ? YearMonth.from(today) : dailyCalendarMonth.getMonth();
        dailyCalendarMonth = DailyCalendarMonth.showing(selectedMonth, today);
        currentScreen = Screen.DAILY_CALENDAR;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = dailyCalendarScreen.build(dailyCalendarMonth,
                date -> dailyCalendarDayState(date, today),
                new AndroidDailyCalendarScreen.CalendarActions() {
                    @Override
                    public void onPreviousMonth() {
                        dailyCalendarMonth = dailyCalendarMonth.previous();
                        showDailyCalendarScreen();
                    }

                    @Override
                    public void onNextMonth() {
                        dailyCalendarMonth = dailyCalendarMonth.next();
                        showDailyCalendarScreen();
                    }

                    @Override
                    public void onDateSelected(LocalDate date) {
                        if (dailyCalendarMonth.isPlayable(date)) {
                            startDailyChallenge(date);
                        }
                    }

                    @Override
                    public void onBack() {
                        showHomeScreen();
                    }
                });
        presentContentView(screen.root);
    }

    private AndroidDailyCalendarScreen.DayState dailyCalendarDayState(LocalDate date,
            LocalDate today) {
        if (date.isAfter(today)) {
            return AndroidDailyCalendarScreen.DayState.FUTURE;
        }
        AndroidGameStore.DailyProgress progress = store.getDailyProgress(date.toString());
        AndroidGameStore.SaveMetadata metadata = store.getDailySaveMetadata(date.toString());
        if (progress.completedToday || (metadata != null && metadata.solved)) {
            return AndroidDailyCalendarScreen.DayState.COMPLETED;
        }
        if (metadata != null) {
            return AndroidDailyCalendarScreen.DayState.IN_PROGRESS;
        }
        return date.equals(today)
                ? AndroidDailyCalendarScreen.DayState.READY
                : AndroidDailyCalendarScreen.DayState.MISSED;
    }

    private void showDifficultyDialog(int size) {
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        String[] labels = new String[difficulties.length];
        for (int i = 0; i < difficulties.length; i++) {
            labels[i] = difficultyName(difficulties[i]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_dialog_title)
                .setSingleChoiceItems(labels, store.getLastDifficulty().ordinal(), (selectedDialog, which) -> {
                    beginNewGame(size, difficulties[which]);
                    selectedDialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void showOnboardingScreen(int requestedPage) {
        pauseGameForNavigation();
        if (deferScreenChange(() -> showOnboardingScreen(requestedPage))) {
            return;
        }
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

        presentContentView(screen.root);
    }

    private void showModeSelectScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showModeSelectScreen)) {
            return;
        }
        currentScreen = Screen.MODE_SELECT;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = modeSelectScreen.build(formatBestForCard(3),
                formatBestForCard(4), formatBestForCard(5), new AndroidModeSelectScreen.ModeActions() {
                    @Override
                    public void onModeSelected(int size) {
                        showDifficultyDialog(size);
                    }

                    @Override
                    public void onHome() {
                        showHomeScreen();
                    }
                });

        presentContentView(screen.root);
    }

    private void showHowToScreen(Screen returnScreen) {
        pauseGameForNavigation();
        if (deferScreenChange(() -> showHowToScreen(returnScreen))) {
            return;
        }
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
        Button backButton = ui.addWideButton(screen.content, R.string.nav_back, COLOR_PANEL, v -> returnFromInfoScreen());
        backButton.setId(R.id.how_back_button);

        presentContentView(screen.root);
    }

    private void showRecordsScreen(Screen returnScreen) {
        pauseGameForNavigation();
        if (deferScreenChange(() -> showRecordsScreen(returnScreen))) {
            return;
        }
        currentScreen = Screen.RECORDS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = recordsScreen.build(new AndroidRecordsScreen.RecordsDataProvider() {
            @Override
            public String getRecordText(int size, PuzzleDifficulty difficulty) {
                return formatBestForCard(size, difficulty);
            }

            @Override
            public AndroidGameStore.CompletionStats getStats(
                    int size, PuzzleDifficulty difficulty) {
                return store.getCompletionStats(size, difficulty);
            }

            @Override
            public AndroidGameStore.CompletionStats getOverallStats() {
                return store.getOverallCompletionStats();
            }

            @Override
            public AndroidGameStore.CompletionRecord[] getHistory() {
                return store.getCompletionHistory();
            }
        },
                new AndroidRecordsScreen.RecordsActions() {
                    @Override
                    public void onBack() {
                        returnFromInfoScreen();
                    }
                });

        presentContentView(screen.root);
    }

    private void showTrendsScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showTrendsScreen)) {
            return;
        }
        currentScreen = Screen.TRENDS;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        int size = store.getTrendSize();
        PuzzleDifficulty difficulty = store.getTrendDifficulty();
        ScreenLayout screen = trendsScreen.build(
                store.getWeeklyGoalProgress(LocalDate.now(), ZoneId.systemDefault()),
                size, difficulty, store.getPersonalTrend(size, difficulty),
                new AndroidTrendsScreen.TrendsActions() {
                    @Override
                    public void onSetGoal() {
                        showWeeklyGoalDialog();
                    }

                    @Override
                    public void onChooseScope() {
                        showTrendScopeDialog();
                    }

                    @Override
                    public void onBack() {
                        showHomeScreen();
                    }
                });
        presentContentView(screen.root);
    }

    private void showWeeklyGoalDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2)});
        input.setHint(R.string.dialog_weekly_goal_hint);
        input.setText(String.valueOf(store.getWeeklyGoalTarget()));
        input.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_weekly_goal_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int target;
                    try {
                        target = Integer.parseInt(input.getText().toString());
                    } catch (NumberFormatException exception) {
                        target = 0;
                    }
                    if (target < 1 || target > 50) {
                        input.setError(getString(R.string.dialog_weekly_goal_invalid));
                        return;
                    }
                    store.setWeeklyGoalTarget(target);
                    dialog.dismiss();
                    showTrendsScreen();
                }));
        showTimerPausingDialog(dialog);
    }

    private void showTrendScopeDialog() {
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        String[] labels = new String[9];
        for (int size = 3; size <= 5; size++) {
            for (PuzzleDifficulty difficulty : difficulties) {
                int index = (size - 3) * difficulties.length + difficulty.ordinal();
                labels[index] = getString(R.string.trends_scope,
                        size, size, difficultyName(difficulty));
            }
        }
        int selected = (store.getTrendSize() - 3) * difficulties.length
                + store.getTrendDifficulty().ordinal();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_trend_scope_title)
                .setSingleChoiceItems(labels, selected, (selectedDialog, which) -> {
                    store.setTrendSize(3 + which / difficulties.length);
                    store.setTrendDifficulty(difficulties[which % difficulties.length]);
                    selectedDialog.dismiss();
                    showTrendsScreen();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        showTimerPausingDialog(dialog);
    }

    private void showSettingsScreen(Screen returnScreen) {
        pauseGameForNavigation();
        if (deferScreenChange(() -> showSettingsScreen(returnScreen))) {
            return;
        }
        currentScreen = Screen.SETTINGS;
        infoReturnScreen = returnScreen;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = settingsScreen.build(isHapticEnabled(), store.isSoundEnabled(),
                isReducedMotionEnabled(), store.getLanguageTag(), store.getVisualTheme(),
                new AndroidSettingsScreen.SettingsActions() {
                    @Override
                    public void onLanguageRequested() {
                        showLanguageDialog();
                    }

                    @Override
                    public void onThemeRequested() {
                        showThemeDialog();
                    }

                    @Override
                    public void onHapticChanged(boolean checked) {
                        store.setHapticEnabled(checked);
                        applySettingsToBoard();
                    }

                    @Override
                    public void onSoundChanged(boolean checked) {
                        store.setSoundEnabled(checked);
                        soundFeedback.setEnabled(checked);
                    }

                    @Override
                    public void onReducedMotionChanged(boolean checked) {
                        store.setReducedMotionEnabled(checked);
                        applySettingsToBoard();
                    }

                    @Override
                    public void onExportBackup() {
                        showExportBackupPicker();
                    }

                    @Override
                    public void onImportBackup() {
                        showImportBackupPicker();
                    }

                    @Override
                    public void onResetSave() {
                        confirmResetSave();
                    }

                    @Override
                    public void onResetRecords() {
                        confirmResetRecords();
                    }

                    @Override
                    public void onBack() {
                        returnFromInfoScreen();
                    }
                });

        presentContentView(screen.root);
    }

    private void showResultsScreen() {
        pauseGameForNavigation();
        if (deferScreenChange(this::showResultsScreen)) {
            return;
        }
        if (currentResult == null) {
            showHomeScreen();
            return;
        }

        currentScreen = Screen.RESULTS;
        infoReturnScreen = Screen.HOME;
        statusText = null;
        gameTitleText = null;
        commandButtons.clear();

        ScreenLayout screen = resultsScreen.build(currentResult, formatMoves(currentResult.moves),
                resultRecordText(currentResult), activeContinuousChallenge,
                new AndroidResultsScreen.ResultsActions() {
                    @Override
                    public void onPlayAgain() {
                        if (activeContinuousChallenge == null) {
                            replayCurrentPuzzle();
                        } else if (activeContinuousChallenge.isComplete()) {
                            repeatContinuousChallenge();
                        } else {
                            startNextContinuousPuzzle();
                        }
                    }

                    @Override
                    public void onNewSize() {
                        if (activeContinuousChallenge == null) {
                            saveGame();
                            showModeSelectScreen();
                        } else {
                            confirmEndContinuousChallenge();
                        }
                    }

                    @Override
                    public void onFavorite() {
                        showFavoriteNameDialog(currentFavoriteForModel());
                    }

                    @Override
                    public void onHome() {
                        saveGame();
                        if (activeContinuousChallenge != null
                                && activeContinuousChallenge.isComplete()) {
                            store.clearContinuousGame();
                            activeContinuousChallenge = null;
                        }
                        showHomeScreen();
                    }
                });

        presentContentView(screen.root);
        View completionMark = screen.root.findViewById(R.id.results_completion_mark);
        boolean reducedMotion = isReducedMotionEnabled();
        if (reducedMotion) {
            motion.animateCompletionMark(completionMark, true);
        } else {
            handler.postDelayed(() -> {
                if (currentScreen == Screen.RESULTS && completionMark.isAttachedToWindow()) {
                    motion.animateCompletionMark(completionMark, false);
                }
            }, 300);
        }
    }

    private void showTutorialScreen(int requestedStep) {
        pauseGameForNavigation();
        if (deferScreenChange(() -> showTutorialScreen(requestedStep))) {
            return;
        }
        currentScreen = Screen.TUTORIAL;
        infoReturnScreen = Screen.HOME;
        tutorialAdvancePending = false;
        hintActive = false;
        strategicHintTile = -1;
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
        syncGameTimerState();

        ensureBoardView();
        boardView.setId(R.id.tutorial_board);
        applyTutorialHighlights();

        AndroidTutorialScreen.TutorialViews views = tutorialScreen.build(boardView,
                new AndroidTutorialScreen.TutorialActions() {
                    @Override
                    public void onHome() {
                        showHomeScreen();
                    }

                    @Override
                    public void onStartGame() {
                        beginNewGame(3);
                    }

                    @Override
                    public void onRestartLesson() {
                        if (canAcceptTutorialCommand()) {
                            showTutorialScreen(tutorialStep == TUTORIAL_COMPLETE
                                    ? TUTORIAL_FIRST_MOVE
                                    : tutorialStep);
                        }
                    }
                });
        tutorialProgressText = views.progressText;
        tutorialInstructionText = views.instructionText;
        tutorialStatusText = views.statusText;

        presentContentView(views.root);
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
        if (deferScreenChange(this::showGameScreen)) {
            return;
        }
        currentScreen = Screen.GAME;
        gameNavigationPending = false;
        tutorialAdvancePending = false;
        hintActive = false;
        strategicHintTile = -1;
        statusText = null;
        gameTitleText = null;
        tutorialProgressText = null;
        tutorialInstructionText = null;
        tutorialStatusText = null;
        commandButtons.clear();

        ensureBoardView();
        boardView.clearHighlights();

        AndroidGameScreen.GameViews views = gameScreen.build(boardView, new AndroidGameScreen.GameActions() {
            @Override
            public void onHome() {
                if (canAcceptCommand()) {
                    saveGame();
                    showHomeScreen();
                }
            }

            @Override
            public void onMenu() {
                if (canAcceptCommand()) {
                    showPauseMenu();
                }
            }

            @Override
            public void onUndo() {
                if (canAcceptCommand()) {
                    clearGameHint();
                    model.undo();
                    performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
                    updateStatus();
                }
            }

            @Override
            public void onRestart() {
                if (canAcceptCommand()) {
                    restartCurrentGame();
                }
            }

            @Override
            public void onAssist() {
                if (canAcceptCommand()) {
                    showAssistMenu();
                }
            }
        });
        gameTitleText = views.titleText;
        statusText = views.statusText;

        presentContentView(views.root);
        syncGameTimerState();
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
                getString(R.string.favorite_save_action),
                getString(R.string.menu_new_size),
                getString(R.string.menu_quick_reminder),
                getString(R.string.home_how_to_play),
                getString(R.string.home_settings),
                getString(R.string.home_records),
                getString(R.string.nav_home)
        };

        AlertDialog menuDialog = new AlertDialog.Builder(this)
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
                                showGameScreen();
                                Toast.makeText(this, R.string.toast_game_loaded, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
                            }
                        }
                        case 3 -> restartCurrentGame();
                        case 4 -> {
                            showFavoriteNameDialog(currentFavoriteForModel());
                        }
                        case 5 -> {
                            saveGame();
                            showModeSelectScreen();
                        }
                        case 6 -> showQuickReminder();
                        case 7 -> showHowToScreen(Screen.GAME);
                        case 8 -> showSettingsScreen(Screen.GAME);
                        case 9 -> showRecordsScreen(Screen.GAME);
                        case 10 -> {
                            saveGame();
                            showHomeScreen();
                        }
                        default -> {
                        }
                    }
                })
                .create();
        showTimerPausingDialog(menuDialog);
    }

    private void showQuickReminder() {
        AlertDialog reminderDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quick_reminder_title)
                .setMessage(R.string.quick_reminder_message)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        showTimerPausingDialog(reminderDialog);
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
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void showExportBackupPicker() {
        saveGame();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "SlideDo-backup-"
                        + LocalDateTime.now().format(BACKUP_FILE_TIME) + ".json");
        startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
    }

    @SuppressWarnings("deprecation")
    private void showImportBackupPicker() {
        saveGame();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
    }

    private void writePersonalDataBackup(Uri documentUri) throws IOException {
        byte[] archive = store.exportPersonalData().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = getContentResolver().openOutputStream(documentUri, "wt")) {
            if (output == null) {
                throw new IOException("Android did not provide a writable backup document.");
            }
            output.write(archive);
        }
    }

    private String readPersonalDataBackup(Uri documentUri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(documentUri);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IOException("Android did not provide a readable backup document.");
            }
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                totalBytes += count;
                if (totalBytes > AndroidPersonalDataArchive.MAX_ARCHIVE_CHARS * 4) {
                    throw new IOException("Backup document exceeds the supported size.");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void confirmImportPersonalData(String archive) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_restore_backup_title)
                .setMessage(R.string.dialog_restore_backup_message)
                .setPositiveButton(R.string.dialog_restore, (dialog, which) -> {
                    try {
                        store.importPersonalData(archive);
                        Toast.makeText(this, R.string.toast_backup_restored, Toast.LENGTH_SHORT).show();
                        recreate();
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        showBackupError();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showBackupError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_backup_error_title)
                .setMessage(R.string.dialog_backup_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showLanguageDialog() {
        AndroidAppLocale.LanguageOption[] options = AndroidAppLocale.getSupportedLanguages();
        String[] labels = new String[options.length];
        for (int index = 0; index < options.length; index++) {
            labels[index] = getString(options[index].displayNameResId);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_language_title)
                .setSingleChoiceItems(labels, AndroidAppLocale.indexOf(store.getLanguageTag()),
                        (dialog, which) -> {
                            String selectedTag = options[which].languageTag;
                            dialog.dismiss();
                            applyLanguage(selectedTag);
                        })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void applyLanguage(String languageTag) {
        String selectedTag = AndroidAppLocale.normalizeLanguageTag(languageTag);
        if (selectedTag.equals(store.getLanguageTag())) {
            return;
        }
        saveGame();
        store.setLanguageTag(selectedTag);
        recreate();
    }

    private void showThemeDialog() {
        AndroidVisualTheme[] themes = AndroidVisualTheme.values();
        String[] labels = new String[themes.length];
        for (int index = 0; index < themes.length; index++) {
            labels[index] = getString(themes[index] == AndroidVisualTheme.OCEAN
                    ? R.string.theme_ocean : R.string.theme_midnight);
        }

        int selectedIndex = store.getVisualTheme().ordinal();
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_theme_title)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    AndroidVisualTheme selectedTheme = themes[which];
                    dialog.dismiss();
                    applyVisualTheme(selectedTheme);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void applyVisualTheme(AndroidVisualTheme visualTheme) {
        if (visualTheme == store.getVisualTheme()) {
            return;
        }
        saveGame();
        store.setVisualTheme(visualTheme);
        recreate();
    }

    private void confirmResetRecords() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_reset_records_title)
                .setMessage(R.string.dialog_reset_records_message)
                .setPositiveButton(R.string.dialog_reset, (dialog, which) -> {
                    clearRecords();
                    Toast.makeText(this, R.string.toast_records_reset, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAssistMenu() {
        String[] items = new String[] {
                getString(R.string.button_hint_strategic),
                getString(R.string.button_hint_movable),
                getString(R.string.button_solver_tools)
        };

        AlertDialog assistDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.assist_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showStrategicHint();
                    } else if (which == 1) {
                        showMovableTilesHint();
                    } else if (which == 2) {
                        showSolverTools();
                    }
                })
                .create();
        showTimerPausingDialog(assistDialog);
    }

    private void showSolverTools() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ui.dp(20), 0, ui.dp(20), 0);

        TextView message = ui.createText(getString(R.string.solver_tools_message), 14,
                AndroidUi.COLOR_MUTED_TEXT, Typeface.NORMAL);
        message.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams messageParams = ui.fullWidthParams();
        messageParams.setMargins(0, 0, 0, ui.dp(14));
        content.addView(message, messageParams);

        Button bfsButton = ui.addWideButton(content, R.string.button_solver_bfs, COLOR_PANEL, null);
        Button astarButton = ui.addWideButton(content, R.string.button_solver_astar, COLOR_PANEL, null);
        Button idastarButton = ui.addWideButton(content, R.string.button_solver_idastar, COLOR_PANEL, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.solver_tools_title)
                .setView(content)
                .setNegativeButton(R.string.nav_back, null)
                .create();
        bfsButton.setOnClickListener(v -> {
            dialog.dismiss();
            runSolver(new BfsSolver());
        });
        astarButton.setOnClickListener(v -> {
            dialog.dismiss();
            runSolver(new AStarSolver());
        });
        idastarButton.setOnClickListener(v -> {
            dialog.dismiss();
            runSolver(new IdaStarSolver());
        });
        showTimerPausingDialog(dialog);
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

    private void continueSavedGameFromHome() {
        AndroidGameStore.SaveMetadata[] saves = store.getAllSaveMetadata();
        if (saves.length == 0) {
            Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
            showHomeScreen();
        } else if (saves.length == 1) {
            continueSavedGame(saves[0].size);
        } else {
            String[] labels = new String[saves.length];
            for (int index = 0; index < saves.length; index++) {
                labels[index] = formatSaveChoice(saves[index]);
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.home_save_picker_title)
                    .setItems(labels, (dialog, which) -> continueSavedGame(saves[which].size))
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        }
    }

    private void continueSavedGame(int size) {
        if (loadGame(size)) {
            pendingWin = null;
            currentResult = null;
            showGameScreen();
        } else {
            Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
            showHomeScreen();
        }
    }

    private String formatSaveChoice(AndroidGameStore.SaveMetadata metadata) {
        String state = metadata.solved
                ? getString(R.string.home_continue_solved)
                : (metadata.active
                        ? getString(R.string.home_continue_active)
                        : getString(R.string.home_continue_saved));
        return getString(R.string.home_save_choice_format,
                metadata.size, metadata.size, difficultyName(metadata.difficulty), state,
                formatMoves(metadata.moves), Math.max(0, metadata.elapsedMs) / 1000);
    }

    private void beginNewGame(int size) {
        beginNewGame(size, PuzzleDifficulty.CLASSIC);
    }

    private void beginNewGame(int size, PuzzleDifficulty difficulty) {
        if (solverRunning) {
            return;
        }
        attachModel(new GameModel(size));
        model.scramble(difficulty);
        activeDailyDateId = null;
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        hintActive = false;
        strategicHintTile = -1;
        lastWinTimeMs = -1;
        store.setLastSize(size);
        store.setLastDifficulty(difficulty);
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private void startContinuousChallenge(int size, PuzzleDifficulty difficulty, int target) {
        if (solverRunning || !ContinuousChallenge.isSupportedTarget(target)) {
            return;
        }
        store.clearContinuousGame();
        activeContinuousChallenge = ContinuousChallenge.start(target);
        attachModel(new GameModel(size));
        model.scramble(difficulty);
        activeDailyDateId = null;
        activeFavoriteId = null;
        saveSuppressedAfterReset = false;
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        hintActive = false;
        strategicHintTile = -1;
        lastWinTimeMs = -1;
        store.setLastSize(size);
        store.setLastDifficulty(difficulty);
        saveGame();
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private void resumeContinuousChallenge() {
        if (!loadContinuousGame()) {
            store.clearContinuousGame();
            showHomeScreen();
            return;
        }
        pendingWin = null;
        currentResult = null;
        if (activeContinuousChallenge.isComplete()) {
            store.clearContinuousGame();
            activeContinuousChallenge = null;
            showHomeScreen();
            return;
        }
        if (model.isSolved()) {
            startNextContinuousPuzzle();
        } else {
            showGameScreen();
        }
    }

    private void startNextContinuousPuzzle() {
        if (solverRunning || activeContinuousChallenge == null
                || activeContinuousChallenge.isComplete() || model == null) {
            return;
        }
        int size = model.getSize();
        PuzzleDifficulty difficulty = model.getDifficulty();
        attachModel(new GameModel(size));
        model.scramble(difficulty);
        activeDailyDateId = null;
        activeFavoriteId = null;
        saveSuppressedAfterReset = false;
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        hintActive = false;
        strategicHintTile = -1;
        lastWinTimeMs = -1;
        saveGame();
        syncGameTimerState();
        showGameScreen();
    }

    private void repeatContinuousChallenge() {
        if (activeContinuousChallenge == null || model == null) {
            return;
        }
        startContinuousChallenge(model.getSize(), model.getDifficulty(),
                activeContinuousChallenge.getTargetPuzzles());
    }

    private void restartCurrentGame() {
        if (!canAcceptCommand()) {
            return;
        }
        model.restartCurrentGame();
        pendingWin = null;
        currentResult = null;
        clearGameHint();
        lastWinTimeMs = -1;
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private void replayCurrentPuzzle() {
        if (solverRunning || currentResult == null || model == null
                || model.getSize() != currentResult.size
                || model.getDifficulty() != currentResult.difficulty) {
            return;
        }
        boolean replayAssisted = currentResult.assisted;
        model.restartCurrentGame();
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = replayAssisted;
        clearGameHint();
        lastWinTimeMs = -1;
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private void startDailyChallenge(LocalDate date) {
        if (solverRunning || date == null || date.isAfter(LocalDate.now())) {
            return;
        }
        DailyChallenge challenge = DailyChallenge.forDate(date);
        SaveManager.SaveData saved = store.loadDailyGame(challenge.getDateId());
        boolean savedAssisted = saved != null
                && store.isDailyGameAssisted(challenge.getDateId());
        if (saved == null) {
            attachModel(challenge.createGame());
        } else {
            attachModel(new GameModel(saved.size));
            model.loadState(saved);
            if (model.isSolved()) {
                model.restartCurrentGame();
            }
        }
        activeDailyDateId = challenge.getDateId();
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = savedAssisted;
        hintActive = false;
        strategicHintTile = -1;
        lastWinTimeMs = -1;
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private void startFavoritePuzzle(AndroidGameStore.FavoritePuzzle favorite) {
        if (solverRunning || favorite == null) {
            return;
        }
        attachModel(favorite.createGame());
        activeDailyDateId = null;
        activeFavoriteId = favorite.id;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        gameStarted = true;
        pendingWin = null;
        currentResult = null;
        assistedSolveActive = false;
        hintActive = false;
        strategicHintTile = -1;
        lastWinTimeMs = -1;
        syncGameTimerState();
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        showGameScreen();
    }

    private AndroidGameStore.FavoritePuzzle currentFavoriteForModel() {
        if (model == null) {
            return null;
        }
        try {
            return store.getFavoritePuzzle(PuzzleIdentity.from(model).getId());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean canAcceptCommand() {
        return currentScreen == Screen.GAME && boardView != null && !solverRunning && !boardView.isBusy();
    }

    private void updateStatus() {
        if (currentScreen != Screen.GAME || statusText == null || model == null) {
            return;
        }

        if (gameTitleText != null) {
            if (activeContinuousChallenge != null) {
                gameTitleText.setText(getString(R.string.continuous_game_title,
                        activeContinuousChallenge.getCurrentPuzzleNumber(),
                        activeContinuousChallenge.getTargetPuzzles(),
                        model.getSize(), model.getSize(),
                        difficultyName(model.getDifficulty())));
            } else if (activeFavoriteId != null) {
                AndroidGameStore.FavoritePuzzle favorite =
                        store.getFavoritePuzzle(activeFavoriteId);
                String label = favorite == null
                        ? getString(R.string.favorites_title) : favorite.label;
                gameTitleText.setText(getString(R.string.game_favorite_title_format,
                        label, model.getSize(), model.getSize(),
                        difficultyName(model.getDifficulty())));
            } else if (activeDailyDateId == null) {
                gameTitleText.setText(getString(R.string.game_title_format,
                        model.getSize(), model.getSize(), difficultyName(model.getDifficulty())));
            } else {
                gameTitleText.setText(getString(R.string.game_daily_title_format,
                        activeDailyDateId, model.getSize(), model.getSize(),
                        difficultyName(model.getDifficulty())));
            }
        }

        AndroidGameStore.Best best = getBest(model.getSize(), model.getDifficulty());
        String bestText = best == null
                ? getString(R.string.best_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
        if (!model.isGameRunning() && model.isSolved()) {
            long elapsed = lastWinTimeMs >= 0
                    ? lastWinTimeMs / 1000
                    : model.getElapsedTime() / 1000;
            statusText.setText(getString(R.string.status_solved_format, model.getMoveCount(), elapsed, bestText));
            updateControlsEnabled();
            return;
        }

        long elapsed = model.getElapsedTime() / 1000;
        String status = getString(R.string.status_format, formatMoves(model.getMoveCount()), elapsed, bestText);
        if (strategicHintTile >= 0) {
            status += "\n" + getString(R.string.status_hint_strategic, strategicHintTile);
        } else if (hintActive) {
            status += "\n" + getString(R.string.status_hint_movable);
        } else if (model.getMoveCount() == 0) {
            status += "\n" + getString(R.string.status_first_move_hint);
        }
        if (assistedSolveActive) {
            status += "\n" + getString(R.string.status_assisted_run);
        }
        if (activeFavoriteId != null) {
            status += "\n" + getString(R.string.status_favorite_practice);
        }
        if (activeContinuousChallenge != null) {
            status += "\n" + getString(R.string.continuous_status,
                    activeContinuousChallenge.getCompletedPuzzles(),
                    activeContinuousChallenge.getPlayerPuzzles(),
                    activeContinuousChallenge.getAssistedPuzzles());
        }
        statusText.setText(status);
        updateControlsEnabled();
    }

    private void showStrategicHint() {
        if (!canAcceptCommand() || model.isSolved()) {
            return;
        }
        StrategicHint.Hint hint = StrategicHint.choose(model);
        if (hint == null) {
            return;
        }
        boolean[][] highlights = new boolean[model.getSize()][model.getSize()];
        highlights[hint.getRow()][hint.getCol()] = true;
        hintActive = true;
        strategicHintTile = hint.getTile();
        assistedSolveActive = true;
        boardView.setHighlightedCells(highlights, hint.getRow(), hint.getCol());
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        saveGame();
        updateStatus();
    }

    private void showMovableTilesHint() {
        if (!canAcceptCommand() || model.isSolved()) {
            return;
        }
        hintActive = true;
        strategicHintTile = -1;
        boardView.setHighlightedCells(createAlignedHintGrid(), -1, -1);
        performBoardHaptic(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private void clearGameHint() {
        hintActive = false;
        strategicHintTile = -1;
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
        if (!gameStarted || model == null || saveSuppressedAfterReset) {
            return;
        }

        long elapsed = model.isSolved() && lastWinTimeMs >= 0
                ? lastWinTimeMs
                : model.getElapsedTime();
        if (activeContinuousChallenge != null) {
            store.saveContinuousGame(model, elapsed, assistedSolveActive,
                    activeContinuousChallenge);
        } else if (activeFavoriteId != null) {
            store.saveFavoriteRun(activeFavoriteId, model, elapsed);
        } else if (activeDailyDateId == null) {
            store.saveGame(model, elapsed, assistedSolveActive);
        } else {
            store.saveDailyGame(activeDailyDateId, model, elapsed, assistedSolveActive);
        }
    }

    private boolean loadGame() {
        if (activeContinuousChallenge != null) {
            return loadContinuousGame();
        }
        if (activeFavoriteId != null) {
            return loadFavoriteGame(activeFavoriteId);
        }
        if (activeDailyDateId != null) {
            return loadDailyGame(activeDailyDateId);
        }
        int size = model == null ? store.getLastSize(4) : model.getSize();
        return loadGame(size);
    }

    private boolean loadGame(int size) {
        SaveManager.SaveData data = store.loadSavedGame(size);
        if (data == null) {
            return false;
        }

        attachModel(new GameModel(data.size));
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = store.isSavedGameAssisted(data.size);
        currentResult = null;
        activeDailyDateId = null;
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        hintActive = false;
        strategicHintTile = -1;
        gameStarted = true;
        store.setLastSize(data.size);
        syncGameTimerState();
        return true;
    }

    private boolean loadDailyGame(String dateId) {
        SaveManager.SaveData data = store.loadDailyGame(dateId);
        if (data == null) {
            return false;
        }
        attachModel(new GameModel(data.size));
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = store.isDailyGameAssisted(dateId);
        currentResult = null;
        activeDailyDateId = dateId;
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        hintActive = false;
        strategicHintTile = -1;
        gameStarted = true;
        syncGameTimerState();
        return true;
    }

    private boolean loadFavoriteGame(String favoriteId) {
        SaveManager.SaveData data = store.loadFavoriteRun(favoriteId);
        if (data == null) {
            return false;
        }
        attachModel(new GameModel(data.size));
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = false;
        currentResult = null;
        activeDailyDateId = null;
        activeFavoriteId = favoriteId;
        activeContinuousChallenge = null;
        saveSuppressedAfterReset = false;
        hintActive = false;
        strategicHintTile = -1;
        gameStarted = true;
        syncGameTimerState();
        return true;
    }

    private boolean loadContinuousGame() {
        AndroidGameStore.ContinuousGame saved = store.loadContinuousGame();
        if (saved == null) {
            return false;
        }
        attachModel(new GameModel(saved.game.size));
        model.loadState(saved.game);
        lastWinTimeMs = model.isSolved() ? saved.game.elapsedTime : -1;
        assistedSolveActive = saved.assisted;
        activeContinuousChallenge = saved.challenge;
        currentResult = null;
        activeDailyDateId = null;
        activeFavoriteId = null;
        saveSuppressedAfterReset = false;
        hintActive = false;
        strategicHintTile = -1;
        gameStarted = true;
        syncGameTimerState();
        return true;
    }

    private void clearSavedGame() {
        store.clearSavedGame();
        saveSuppressedAfterReset = true;
    }

    private AndroidGameStore.Best getBest(int size) {
        return getBest(size, PuzzleDifficulty.CLASSIC);
    }

    private AndroidGameStore.Best getBest(int size, PuzzleDifficulty difficulty) {
        return store.getBest(size, difficulty);
    }

    private void recordBest(int size, PuzzleDifficulty difficulty, int moves, long timeMs) {
        store.recordBestIfBetter(size, difficulty, moves, timeMs);
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
            AlertDialog warningDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_warning_title)
                    .setMessage(warning)
                    .setPositiveButton(R.string.dialog_continue, (dialog, which) -> startSolver(solver))
                    .setNegativeButton(R.string.dialog_close, null)
                    .create();
            showTimerPausingDialog(warningDialog);
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
            AlertDialog failedDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_result_title)
                    .setMessage(R.string.dialog_solver_failed)
                    .setPositiveButton(R.string.dialog_close, null)
                    .create();
            showTimerPausingDialog(failedDialog);
            return;
        }

        AlertDialog solutionDialog = new AlertDialog.Builder(this)
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
                .create();
        showTimerPausingDialog(solutionDialog);
    }

    private String formatBestForCard(int size) {
        return formatBestForCard(size, PuzzleDifficulty.CLASSIC);
    }

    private String formatBestForCard(int size, PuzzleDifficulty difficulty) {
        AndroidGameStore.Best best = getBest(size, difficulty);
        return best == null
                ? getString(R.string.records_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
    }

    private String difficultyName(PuzzleDifficulty difficulty) {
        return getString(switch (difficulty) {
            case RELAXED -> R.string.difficulty_relaxed;
            case CLASSIC -> R.string.difficulty_classic;
            case CHALLENGE -> R.string.difficulty_challenge;
        });
    }

    private String resultRecordText(GameResult result) {
        if (result.favoriteId != null) {
            return getString(R.string.results_favorite_record);
        }
        String dailyPrefix = "";
        if (result.dailyDateId != null) {
            AndroidGameStore.DailyProgress progress = store.getDailyProgress(LocalDate.now().toString());
            dailyPrefix = getString(R.string.results_daily_progress,
                    progress.currentStreak, progress.bestStreak) + "\n";
        }
        if (result.assisted) {
            String previous = result.previousBest == null
                    ? getString(R.string.records_empty)
                    : getString(R.string.best_format, formatMoves(result.previousBest.moves),
                            result.previousBest.timeMs / 1000);
            return dailyPrefix + getString(R.string.results_assisted_record, previous);
        }
        if (result.newBest) {
            return dailyPrefix + (result.previousBest == null
                    ? getString(R.string.results_first_record)
                    : getString(R.string.results_new_best,
                            getString(R.string.best_format, formatMoves(result.previousBest.moves),
                                    result.previousBest.timeMs / 1000)));
        }
        AndroidGameStore.Best best = getBest(result.size, result.difficulty);
        String bestText = best == null
                ? getString(R.string.records_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
        return dailyPrefix + getString(R.string.results_no_new_best, bestText);
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
            boardView.setVisualTheme(store.getVisualTheme());
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
        if (!solverRunning && !model.isSolved()) {
            soundFeedback.playMove();
        }
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
        if (!solverRunning && !model.isSolved()) {
            soundFeedback.playMove();
        }
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
        soundFeedback.playCompletion();
        if (currentScreen == Screen.TUTORIAL) {
            handleTutorialWin();
            return;
        }
        lastWinTimeMs = timeMs;
        pendingWin = new PendingWin(model.getSize(), model.getDifficulty(), moves, timeMs,
                assistedSolveActive, activeDailyDateId, activeFavoriteId);
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
        AndroidGameStore.Best previousBest = getBest(win.size, win.difficulty);
        boolean recordEligible = win.favoriteId == null;
        if (recordEligible) {
            store.recordCompletion(win.size, win.difficulty, win.moves, win.timeMs, win.assisted);
            if (win.dailyDateId != null) {
                store.recordDailyCompletion(win.dailyDateId);
            }
        }
        boolean newBest = recordEligible && !win.assisted
                && AndroidGameStore.isBetterRecord(previousBest, win.moves, win.timeMs);
        if (newBest) {
            recordBest(win.size, win.difficulty, win.moves, win.timeMs);
        }
        if (activeContinuousChallenge != null) {
            activeContinuousChallenge = activeContinuousChallenge.completePuzzle(
                    win.moves, win.timeMs, win.assisted);
        }
        currentResult = new GameResult(win.size, win.difficulty, win.moves,
                win.timeMs, win.assisted, newBest, previousBest,
                win.dailyDateId, win.favoriteId);
        assistedSolveActive = win.assisted;
        saveGame();
        performBoardHaptic(HapticFeedbackConstants.LONG_PRESS);
        updateStatus();
        showResultsScreen();
    }

    private String formatMoves(int moves) {
        return getResources().getQuantityString(R.plurals.moves_count, moves, moves);
    }

}
