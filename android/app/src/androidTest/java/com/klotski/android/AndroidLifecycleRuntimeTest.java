package com.klotski.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.klotski.core.DailyChallenge;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;
import com.klotski.core.Solver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Runtime qualification for Activity recreation while asynchronous work or a win is pending. */
@RunWith(AndroidJUnit4.class)
public class AndroidLifecycleRuntimeTest {
    private static final String PACKAGE_NAME = "com.klotski.android";
    private static final String PREFS = "slidedo";
    private static final long TIMEOUT_MS = 15_000L;
    private static final String NORMAL_WIN_GRID = "1,2,3,4,5,6,7,0,8";
    private static final String NON_WIN_GRID = "1,2,3,4,0,6,7,5,8";
    private static final int[][] DAILY_NEAR_WIN_GRID = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12},
            {13, 14, 0, 15}
    };

    private Instrumentation instrumentation;
    private Context targetContext;
    private UiDevice device;
    private MainActivity activity;
    private final List<ControlledSolver> solvers = new ArrayList<>();
    private final List<Thread> solverThreads = new ArrayList<>();

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        targetContext = instrumentation.getTargetContext();
        device = UiDevice.getInstance(instrumentation);
        SharedPreferences preferences = targetContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        assertTrue(preferences.edit().clear().putBoolean("onboarding_seen", true).commit());
    }

    @After
    public void tearDown() throws Exception {
        for (ControlledSolver solver : solvers) {
            solver.release.countDown();
        }
        for (Thread worker : solverThreads) {
            worker.join(5_000L);
        }
        instrumentation.runOnMainSync(() -> {
            for (Activity candidate : new ArrayList<>(resumedActivitiesOnMain())) {
                candidate.finish();
            }
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        });
        instrumentation.waitForIdleSync();
        activity = null;
        device.pressHome();
    }

    @Test
    public void solverWorkerCompletingAfterRecreationCannotMutateOldOrNewActivity()
            throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        MainActivity destroyedActivity = activity;
        ControlledSolver solver = new ControlledSolver("Blocked runtime solver", true);
        Thread worker = startSolver(destroyedActivity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(solver.isWaiting.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        MainActivity recreatedActivity = recreateActivity(destroyedActivity);
        assertTrue(solver.interrupted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(isDestroyed(destroyedActivity));
        String statusAfterDestruction = readText(destroyedActivity, R.id.game_status_text);
        waitForId("game_root");
        assertEquals(Screen.GAME, readField(recreatedActivity, "currentScreen"));

        solver.release.countDown();
        worker.join(TIMEOUT_MS);
        assertFalse("The old solver worker did not finish", worker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(statusAfterDestruction,
                readText(destroyedActivity, R.id.game_status_text));
        assertEquals(Screen.GAME, readField(recreatedActivity, "currentScreen"));
        assertFalse((Boolean) readField(recreatedActivity, "solverRunning"));
        assertNull(device.findObject(By.text(solverResultMessage(1))));
        assertNull(device.findObject(By.res(PACKAGE_NAME, "results_root")));
    }

    @Test
    public void lateSolverResultAfterBackNavigationCannotPresentOnHome() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        ControlledSolver solver = new ControlledSolver("Held navigation solver", true);
        Thread worker = startSolver(activity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(solver.isWaiting.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        device.pressBack();
        waitForId("home_root");
        assertFalse("Back navigation destroyed the Activity", isDestroyed(activity));

        solver.release.countDown();
        worker.join(TIMEOUT_MS);
        assertFalse("The held solver worker did not finish", worker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(Screen.HOME, readField(activity, "currentScreen"));
        assertNull("A result from the abandoned Game screen appeared on Home",
                device.findObject(By.text(solverResultMessage(1))));
    }

    @Test
    public void userCanCancelSolverWithoutChangingPuzzleAndContinuePlaying() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        GameModel model = (GameModel) readField(activity, "model");
        int[][] gridBefore = model.getGridCopy();
        List<?> actionsBefore = model.getActionHistory();
        List<?> redoBefore = model.getRedoHistory();
        int moveCountBefore = model.getMoveCount();
        boolean assistedBefore = (Boolean) readField(activity, "assistedSolveActive");
        assertTrue("The active game timer was not running", model.isTimerRunning());

        AndroidGameStore storeBefore = new AndroidGameStore(targetContext);
        int historySizeBefore = storeBefore.getCompletionHistory().length;
        AndroidGameStore.Best bestBefore = storeBefore.getBest(3, PuzzleDifficulty.CLASSIC);
        AndroidGameStore.OverallCompletionStats statsBefore =
                storeBefore.getOverallCompletionStats();

        ControlledSolver solver = new ControlledSolver("Cancelable runtime solver", true);
        Thread worker = startSolver(activity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(solver.isWaiting.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        UiObject2 cancelButton = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "game_cancel_solver_button")), TIMEOUT_MS);
        assertNotNull("The running solver has no visible Cancel Solver action", cancelButton);
        assertEquals(activity.getString(R.string.accessibility_cancel_solver),
                cancelButton.getContentDescription());
        cancelButton.click();

        assertTrue("Cancel did not interrupt the solver worker",
                solver.interrupted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse((Boolean) readField(activity, "solverRunning"));
        assertNull(readField(activity, "solverThread"));
        assertNull(device.findObject(By.res(PACKAGE_NAME, "game_cancel_solver_button")));
        assertTrue("Game controls were not restored after cancellation",
                device.findObject(By.res(PACKAGE_NAME, "game_restart_button")).isEnabled());
        assertTrue("The game timer did not resume after cancellation",
                model.isTimerRunning());
        assertTrue(Arrays.deepEquals(gridBefore, model.getGridCopy()));
        assertEquals(moveCountBefore, model.getMoveCount());
        assertEquals(actionsBefore, model.getActionHistory());
        assertEquals(redoBefore, model.getRedoHistory());
        assertEquals(assistedBefore, readField(activity, "assistedSolveActive"));
        assertNull(device.findObject(By.text(solverResultMessage(1))));

        solver.release.countDown();
        worker.join(TIMEOUT_MS);
        assertFalse("The canceled solver worker did not finish", worker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(Screen.GAME, readField(activity, "currentScreen"));
        assertTrue(Arrays.deepEquals(gridBefore, model.getGridCopy()));
        assertEquals(moveCountBefore, model.getMoveCount());
        assertNull("The late canceled result appeared after release",
                device.findObject(By.text(solverResultMessage(1))));

        AndroidGameStore storeAfter = new AndroidGameStore(targetContext);
        assertEquals(historySizeBefore, storeAfter.getCompletionHistory().length);
        assertEquals(bestBefore, storeAfter.getBest(3, PuzzleDifficulty.CLASSIC));
        AndroidGameStore.OverallCompletionStats statsAfter =
                storeAfter.getOverallCompletionStats();
        assertEquals(statsBefore.available, statsAfter.available);
        if (statsBefore.available) {
            assertEquals(statsBefore.stats.playerCompletions,
                    statsAfter.stats.playerCompletions);
            assertEquals(statsBefore.stats.assistedCompletions,
                    statsAfter.stats.assistedCompletions);
            assertEquals(statsBefore.stats.playerMoves, statsAfter.stats.playerMoves);
            assertEquals(statsBefore.stats.playerTimeMs, statsAfter.stats.playerTimeMs);
        }

        clickBoardCell(2, 1, 3);
        assertEquals("The game did not accept a move after cancellation",
                moveCountBefore + 1, model.getMoveCount());
    }

    @Test
    public void lateSolverResultCannotMutateTutorialModelAfterRealNavigation() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        GameModel gameModel = (GameModel) readField(activity, "model");
        ControlledSolver solver = new ControlledSolver("Held model replacement solver", true);
        Thread worker = startSolver(activity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(solver.isWaiting.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        device.pressBack();
        waitForId("home_root");
        assertTrue(solver.interrupted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        clickActivityView(R.id.home_tutorial_button);
        waitForId("tutorial_root");

        GameModel tutorialModel = (GameModel) readField(activity, "model");
        assertNotSame(gameModel, tutorialModel);
        int[][] tutorialGridBefore = tutorialModel.getGridCopy();
        solver.release.countDown();
        worker.join(TIMEOUT_MS);
        assertFalse("The obsolete solver worker did not finish", worker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(Screen.TUTORIAL, readField(activity, "currentScreen"));
        assertTrue(Arrays.deepEquals(tutorialGridBefore, tutorialModel.getGridCopy()));
        assertNull("The abandoned game solver showed a result on Tutorial",
                device.findObject(By.text(solverResultMessage(1))));
    }

    @Test
    public void canceledSolverCannotReplaceALaterSolverResult() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        ControlledSolver obsoleteSolver = new ControlledSolver("Obsolete request", true,
                Arrays.asList(Direction.UP, Direction.DOWN));
        Thread obsoleteWorker = startSolver(activity, obsoleteSolver);
        assertTrue(obsoleteSolver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(obsoleteSolver.isWaiting.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        UiObject2 cancelButton = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "game_cancel_solver_button")),
                TIMEOUT_MS);
        assertNotNull(cancelButton);
        cancelButton.click();
        assertTrue(obsoleteSolver.interrupted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        ControlledSolver currentSolver = new ControlledSolver("Current request", false);
        Thread currentWorker = startSolver(activity, currentSolver);
        assertTrue(currentSolver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        currentWorker.join(TIMEOUT_MS);
        assertFalse("The current solver worker did not finish", currentWorker.isAlive());
        waitForText(solverResultMessage(1));

        obsoleteSolver.release.countDown();
        obsoleteWorker.join(TIMEOUT_MS);
        assertFalse("The obsolete solver worker did not finish", obsoleteWorker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(Screen.GAME, readField(activity, "currentScreen"));
        assertNotNull(device.findObject(By.text(solverResultMessage(1))));
        assertNull("The obsolete result replaced the current solver result",
                device.findObject(By.text(solverResultMessage(2))));
    }

    @Test
    public void successfulSolverStillShowsConfirmationAndPlaysSolution() throws Exception {
        seedNormalGame(NORMAL_WIN_GRID);
        launchApp();
        openNormalGame();

        ControlledSolver solver = new ControlledSolver("Successful runtime solver", false,
                Collections.singletonList(Direction.RIGHT));
        Thread worker = startSolver(activity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        worker.join(TIMEOUT_MS);
        assertFalse("The successful solver worker did not finish", worker.isAlive());
        waitForText(solverResultMessage(1));

        UiObject2 animateButton = device.wait(
                Until.findObject(By.res("android", "button1")), TIMEOUT_MS);
        assertNotNull("The normal solver confirmation action is missing", animateButton);
        animateButton.click();
        waitForId("results_root");

        GameModel model = (GameModel) readField(activity, "model");
        assertTrue(model.isSolved());
        assertEquals(1, model.getMoveCount());
        assertTrue((Boolean) readField(activity, "assistedSolveActive"));
    }

    @Test
    public void alreadyPostedSolverCallbackIsRejectedAfterActivityRecreation()
            throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        MainActivity destroyedActivity = activity;
        DeferredMainHandler handler = installDeferredHandler(destroyedActivity);
        ControlledSolver solver = new ControlledSolver("Posted runtime solver", false);
        Thread worker = startSolver(destroyedActivity, solver);
        assertTrue(solver.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue("Solver completion was not queued on the old main Handler",
                handler.captured.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        worker.join(TIMEOUT_MS);
        assertFalse("The completed solver worker remained alive", worker.isAlive());

        MainActivity recreatedActivity = recreateActivity(destroyedActivity);
        handler.dispatchCapturedNow();
        instrumentation.waitForIdleSync();

        assertTrue(isDestroyed(destroyedActivity));
        assertEquals(Screen.GAME, readField(recreatedActivity, "currentScreen"));
        assertFalse((Boolean) readField(recreatedActivity, "solverRunning"));
        assertNull(device.findObject(By.text(solverResultMessage(1))));
        assertNull(device.findObject(By.res(PACKAGE_NAME, "results_root")));
    }

    @Test
    public void lateOldRequestCannotReplaceNewActivitySolverResult() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        MainActivity firstActivity = activity;
        ControlledSolver firstRequest = new ControlledSolver("Old runtime request", true);
        Thread firstWorker = startSolver(firstActivity, firstRequest);
        assertTrue(firstRequest.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        MainActivity secondActivity = recreateActivity(firstActivity);
        assertTrue(firstRequest.interrupted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        ControlledSolver secondRequest = new ControlledSolver("Current runtime request", false);
        Thread secondWorker = startSolver(secondActivity, secondRequest);
        assertTrue(secondRequest.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        secondWorker.join(TIMEOUT_MS);
        assertFalse("The current solver worker did not finish", secondWorker.isAlive());
        waitForText(solverResultMessage(1));

        firstRequest.release.countDown();
        firstWorker.join(TIMEOUT_MS);
        assertFalse("The obsolete solver worker did not finish", firstWorker.isAlive());
        instrumentation.waitForIdleSync();

        assertEquals(Screen.GAME, readField(secondActivity, "currentScreen"));
        assertFalse((Boolean) readField(secondActivity, "solverRunning"));
        assertEquals(1, device.findObjects(By.text(solverResultMessage(1))).size());
        assertTrue(isDestroyed(firstActivity));
    }

    @Test
    public void pendingPlayerWinContinuesOnRecreatedActivity() throws Exception {
        seedNormalGame(NORMAL_WIN_GRID);
        launchApp();
        openNormalGame();

        MainActivity oldActivity = activity;
        makeNormalWinningMoveAndRequestRecreation(oldActivity);
        MainActivity newActivity = awaitResumedActivityAfter(oldActivity);
        activity = newActivity;
        waitForId("results_root");

        assertEquals(Screen.RESULTS, readField(newActivity, "currentScreen"));
        assertNull(readField(newActivity, "pendingWin"));
        assertEquals(Screen.GAME, readField(oldActivity, "currentScreen"));
        assertNull(readField(oldActivity, "currentResult"));
        assertEquals(1, new AndroidGameStore(targetContext).getCompletionHistory().length);
        assertEquals(1, device.findObjects(By.res(PACKAGE_NAME, "results_root")).size());
    }

    @Test
    public void dailyWinSurvivesRapidRecreationAndPersistsExactlyOnce() throws Exception {
        String dateId = seedDailyNearWin();
        launchApp();
        openDailyGame(dateId);

        MainActivity oldActivity = activity;
        makeDailyWinningMoveAndRequestTwoRecreations(oldActivity);
        waitForId("results_root");
        MainActivity resultActivity = activity;

        assertEquals(Screen.RESULTS, readField(resultActivity, "currentScreen"));
        assertNull(readField(resultActivity, "pendingWin"));
        assertEquals(Screen.GAME, readField(oldActivity, "currentScreen"));
        assertNull(readField(oldActivity, "currentResult"));
        GameResult result = (GameResult) readField(resultActivity, "currentResult");
        assertNotNull(result);
        assertTrue(result.completionRecorded);
        assertEquals(dateId, result.dailyDateId);
        assertDailyCompletionPersistedExactlyOnce(dateId);
        assertEquals(1, device.findObjects(By.res(PACKAGE_NAME, "results_root")).size());
    }

    @Test
    public void processedDailyWinDoesNotReplayAfterActivityRecreation() throws Exception {
        String dateId = seedDailyNearWin();
        launchApp();
        openDailyGame(dateId);
        makeDailyWinningMove(activity);
        waitForId("results_root");

        assertDailyCompletionPersistedExactlyOnce(dateId);
        MainActivity resultActivity = recreateActivity(activity);
        waitForId("results_root");

        assertEquals(Screen.RESULTS, readField(resultActivity, "currentScreen"));
        assertNull(readField(resultActivity, "pendingWin"));
        GameResult restoredResult = (GameResult) readField(resultActivity, "currentResult");
        assertNotNull(restoredResult);
        assertTrue(restoredResult.completionRecorded);
        assertEquals(dateId, restoredResult.dailyDateId);
        assertDailyCompletionPersistedExactlyOnce(dateId);
        assertEquals(1, device.findObjects(By.res(PACKAGE_NAME, "results_root")).size());
    }

    @Test
    public void nonWinningGameStateAndAutosaveSurviveActivityRecreation() throws Exception {
        seedNormalGame(NON_WIN_GRID);
        launchApp();
        openNormalGame();

        GameModel model = (GameModel) readField(activity, "model");
        final int[][][] movedGrid = new int[1][][];
        instrumentation.runOnMainSync(() -> {
            assertTrue(model.move(Direction.UP));
            assertFalse(model.isSolved());
            movedGrid[0] = model.getGridCopy();
        });

        MainActivity restoredActivity = recreateActivity(activity);
        waitForId("game_root");
        GameModel restoredModel = (GameModel) readField(restoredActivity, "model");
        assertTrue(java.util.Arrays.deepEquals(movedGrid[0], restoredModel.getGridCopy()));
        assertEquals(1, restoredModel.getMoveCount());
        assertFalse(restoredModel.isSolved());
        assertEquals(Screen.GAME, readField(restoredActivity, "currentScreen"));
        assertNull(readField(restoredActivity, "pendingWin"));
        assertNull(readField(restoredActivity, "currentResult"));
        assertFalse((Boolean) readField(restoredActivity, "solverRunning"));
        assertNull(device.findObject(By.res(PACKAGE_NAME, "results_root")));
    }

    private void seedNormalGame(String grid) {
        SharedPreferences preferences = targetContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        assertTrue(preferences.edit().clear().putBoolean("onboarding_seen", true).commit());
        GameModel model = createModel(grid, 3, PuzzleDifficulty.CLASSIC);
        new AndroidGameStore(targetContext).saveGame(model, 0L);
    }

    private String seedDailyNearWin() {
        SharedPreferences preferences = targetContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        assertTrue(preferences.edit().clear().putBoolean("onboarding_seen", true).commit());
        DailyChallenge challenge = DailyChallenge.forDate(LocalDate.now());
        SaveManager.SaveData saved = new SaveManager.SaveData();
        saved.size = 4;
        saved.grid = copyGrid(DAILY_NEAR_WIN_GRID);
        saved.initialGrid = challenge.createGame().getInitialGridCopy();
        saved.moveCount = 0;
        saved.difficulty = challenge.getDifficulty();
        saved.active = true;
        saved.solved = false;
        GameModel daily = new GameModel(4);
        daily.loadState(saved);
        new AndroidGameStore(targetContext).saveDailyGame(challenge.getDateId(), daily, 2_000L);
        return challenge.getDateId();
    }

    private GameModel createModel(String encodedGrid, int size, PuzzleDifficulty difficulty) {
        SaveManager.SaveData saved = new SaveManager.SaveData();
        saved.size = size;
        saved.grid = parseGrid(encodedGrid, size);
        saved.initialGrid = parseGrid(encodedGrid, size);
        saved.moveCount = 0;
        saved.difficulty = difficulty;
        saved.active = true;
        saved.solved = false;
        GameModel model = new GameModel(size);
        model.loadState(saved);
        return model;
    }

    private void launchApp() throws Exception {
        Intent intent = new Intent(targetContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = (MainActivity) instrumentation.startActivitySync(intent);
        assertNotNull(activity);
        waitForId("home_root");
    }

    private void openNormalGame() throws Exception {
        clickActivityView(R.id.home_continue_button);
        waitForId("game_root");
    }

    private void openDailyGame(String dateId) throws Exception {
        clickActivityView(R.id.home_daily_button);
        waitForId("daily_calendar_root");
        UiObject2 dateButton = device.wait(Until.findObject(By.descContains(dateId)), TIMEOUT_MS);
        assertNotNull("Missing daily calendar action for " + dateId, dateButton);
        dateButton.click();
        waitForId("game_root");
    }

    private void clickActivityView(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing Activity view " + resourceId, view);
            assertTrue("Activity view did not handle click " + resourceId, view.performClick());
        });
    }

    private void clickBoardCell(int row, int col, int size) {
        AtomicReference<Boolean> clicked = new AtomicReference<>(false);
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(R.id.game_board);
            assertTrue(view instanceof KlotskiView);
            AccessibilityNodeProvider provider =
                    ((KlotskiView) view).getAccessibilityNodeProvider();
            assertNotNull(provider);
            int virtualId = KlotskiView.virtualIdForCell(row, col, size);
            clicked.set(provider.performAction(virtualId,
                    AccessibilityNodeInfo.ACTION_CLICK, null));
        });
        assertTrue("The movable tile did not accept its accessibility click", clicked.get());
        instrumentation.waitForIdleSync();
    }

    private MainActivity recreateActivity(MainActivity previous) throws Exception {
        instrumentation.runOnMainSync(previous::recreate);
        MainActivity recreated = awaitResumedActivityAfter(previous);
        activity = recreated;
        return recreated;
    }

    private MainActivity awaitResumedActivityAfter(MainActivity previous) throws Exception {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            AtomicReference<MainActivity> resumed = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                for (Activity candidate : resumedActivitiesOnMain()) {
                    if (candidate instanceof MainActivity && candidate != previous) {
                        resumed.set((MainActivity) candidate);
                        return;
                    }
                }
            });
            if (resumed.get() != null) {
                return resumed.get();
            }
            Thread.sleep(100L);
        }
        fail("MainActivity was not recreated and resumed");
        return null;
    }

    private void makeNormalWinningMoveAndRequestRecreation(MainActivity oldActivity) {
        instrumentation.runOnMainSync(() -> {
            GameModel model = (GameModel) fieldValue(oldActivity, "model");
            assertTrue(model.slideLineTo(2, 2));
            assertTrue(model.isSolved());
            assertNotNull(fieldValue(oldActivity, "pendingWin"));
            oldActivity.recreate();
        });
    }

    private void makeDailyWinningMoveAndRequestTwoRecreations(MainActivity firstActivity)
            throws Exception {
        CountDownLatch sequenceFinished = new CountDownLatch(1);
        AtomicReference<MainActivity> finalActivity = new AtomicReference<>();
        AtomicReference<Throwable> sequenceFailure = new AtomicReference<>();
        AtomicReference<MainActivity> lastActivity = new AtomicReference<>(firstActivity);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        int[] additionalRecreations = {1};

        Runnable recreateWhilePending = new Runnable() {
            @Override
            public void run() {
                try {
                    MainActivity current = resumedMainActivityOnMain();
                    assertNotNull("No resumed MainActivity during pending-win recreation", current);
                    assertNotSame("Activity was not recreated", lastActivity.get(), current);
                    assertNotNull("Pending win was lost during recreation",
                            fieldValue(current, "pendingWin"));

                    if (additionalRecreations[0] > 0) {
                        Handler currentHandler = (Handler) fieldValue(current, "handler");
                        Runnable winRunnable = (Runnable) fieldValue(
                                current, "winResultRunnable");
                        currentHandler.removeCallbacks(winRunnable);
                        additionalRecreations[0]--;
                        lastActivity.set(current);
                        current.recreate();
                        mainHandler.post(this);
                    } else {
                        finalActivity.set(current);
                        sequenceFinished.countDown();
                    }
                } catch (Throwable failure) {
                    sequenceFailure.set(failure);
                    sequenceFinished.countDown();
                }
            }
        };

        instrumentation.runOnMainSync(() -> {
            GameModel model = (GameModel) fieldValue(firstActivity, "model");
            assertTrue(model.slideLineTo(3, 3));
            assertTrue(model.isSolved());
            assertNotNull(fieldValue(firstActivity, "pendingWin"));
            firstActivity.recreate();
            mainHandler.post(recreateWhilePending);
        });
        assertTrue("Rapid pending-win recreations did not complete",
                sequenceFinished.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        if (sequenceFailure.get() != null) {
            throw new AssertionError("Rapid pending-win recreation failed",
                    sequenceFailure.get());
        }
        assertNotNull(finalActivity.get());
        activity = finalActivity.get();
    }

    private void makeDailyWinningMove(MainActivity targetActivity) {
        instrumentation.runOnMainSync(() -> {
            GameModel model = (GameModel) fieldValue(targetActivity, "model");
            assertTrue(model.slideLineTo(3, 3));
            assertTrue(model.isSolved());
            assertNotNull(fieldValue(targetActivity, "pendingWin"));
        });
    }

    private void assertDailyCompletionPersistedExactlyOnce(String dateId) {
        AndroidGameStore reloadedStore = new AndroidGameStore(targetContext);
        AndroidGameStore.DailyProgress progress = reloadedStore.getDailyProgress(dateId);
        assertTrue(progress.completedToday);
        assertEquals(1, progress.currentStreak);
        assertEquals(1, progress.bestStreak);
        assertEquals(dateId, progress.lastCompletedDateId);

        Set<String> completedDates = targetContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE).getStringSet(
                        "daily_completed_dates_v1", Collections.emptySet());
        assertTrue(completedDates.contains(dateId));
        assertEquals(1, completedDates.size());
        assertEquals(1, reloadedStore.getCompletionHistory().length);
        AndroidGameStore.OverallCompletionStats overall =
                reloadedStore.getOverallCompletionStats();
        assertTrue(overall.available);
        assertEquals(1, overall.stats.playerCompletions);
        assertEquals(1, reloadedStore.getBest(4, PuzzleDifficulty.CLASSIC).moves);
        SaveManager.SaveData persistedDaily = reloadedStore.loadDailyGame(dateId);
        assertNotNull(persistedDaily);
        assertTrue(persistedDaily.solved);
        assertEquals(1, persistedDaily.moveCount);
    }

    private Thread startSolver(MainActivity targetActivity, ControlledSolver solver)
            throws Exception {
        solvers.add(solver);
        AtomicReference<Thread> worker = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            try {
                Method startSolver = MainActivity.class.getDeclaredMethod(
                        "startSolver", Solver.class);
                startSolver.setAccessible(true);
                startSolver.invoke(targetActivity, solver);
                worker.set((Thread) fieldValue(targetActivity, "solverThread"));
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Could not start production solver flow", failure.get());
        }
        assertNotNull("Production solver did not create a worker", worker.get());
        solverThreads.add(worker.get());
        return worker.get();
    }

    private DeferredMainHandler installDeferredHandler(MainActivity targetActivity) {
        DeferredMainHandler replacement = new DeferredMainHandler();
        instrumentation.runOnMainSync(() -> {
            Handler original = (Handler) fieldValue(targetActivity, "handler");
            Runnable ticker = (Runnable) fieldValue(targetActivity, "ticker");
            original.removeCallbacks(ticker);
            setFieldValue(targetActivity, "handler", replacement);
        });
        return replacement;
    }

    private MainActivity resumedMainActivityOnMain() {
        for (Activity candidate : resumedActivitiesOnMain()) {
            if (candidate instanceof MainActivity) {
                return (MainActivity) candidate;
            }
        }
        return null;
    }

    private Collection<Activity> resumedActivitiesOnMain() {
        return ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED);
    }

    private boolean isDestroyed(MainActivity targetActivity) {
        AtomicReference<Boolean> destroyed = new AtomicReference<>(false);
        instrumentation.runOnMainSync(() -> destroyed.set(targetActivity.isDestroyed()));
        return destroyed.get();
    }

    private String readText(MainActivity targetActivity, int resourceId) {
        AtomicReference<String> text = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            View view = targetActivity.findViewById(resourceId);
            assertNotNull(view);
            assertTrue(view instanceof android.widget.TextView);
            text.set(((android.widget.TextView) view).getText().toString());
        });
        return text.get();
    }

    private Object readField(MainActivity targetActivity, String name) {
        AtomicReference<Object> value = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> value.set(fieldValue(targetActivity, name)));
        return value.get();
    }

    private static Object fieldValue(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Could not read " + name, failure);
        }
    }

    private static void setFieldValue(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Could not set " + name, failure);
        }
    }

    private String solverResultMessage(int moves) {
        AtomicReference<String> message = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> message.set(
                activity.getString(R.string.dialog_solver_found, moves)));
        return message.get();
    }

    private void waitForId(String resourceName) {
        UiObject2 object = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, resourceName)), TIMEOUT_MS);
        assertNotNull("Missing view id: " + resourceName, object);
    }

    private void waitForText(String text) {
        UiObject2 object = device.wait(Until.findObject(By.text(text)), TIMEOUT_MS);
        assertNotNull("Missing text: " + text, object);
    }

    private static int[][] parseGrid(String encoded, int size) {
        String[] values = encoded.split(",");
        int[][] grid = new int[size][size];
        for (int index = 0; index < values.length; index++) {
            grid[index / size][index % size] = Integer.parseInt(values[index]);
        }
        return grid;
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    private static final class ControlledSolver implements Solver {
        private final String name;
        private final boolean waitForRelease;
        private final List<Direction> solution;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch isWaiting = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private ControlledSolver(String name, boolean waitForRelease) {
            this(name, waitForRelease, Collections.singletonList(Direction.UP));
        }

        private ControlledSolver(String name, boolean waitForRelease,
                List<Direction> solution) {
            this.name = name;
            this.waitForRelease = waitForRelease;
            this.solution = new ArrayList<>(solution);
        }

        @Override
        public List<Direction> solve(GameModel startState) {
            started.countDown();
            isWaiting.countDown();
            if (waitForRelease) {
                long deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
                while (release.getCount() > 0
                        && SystemClock.elapsedRealtime() < deadline) {
                    try {
                        release.await(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                        interrupted.countDown();
                    }
                }
            }
            return new ArrayList<>(solution);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static final class DeferredMainHandler extends Handler {
        private final CountDownLatch captured = new CountDownLatch(1);
        private final AtomicReference<Runnable> callback = new AtomicReference<>();
        private volatile boolean captureNextPost = true;

        private DeferredMainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public boolean sendMessageAtTime(Message message, long uptimeMillis) {
            Runnable runnable = message.getCallback();
            if (captureNextPost && runnable != null) {
                captureNextPost = false;
                callback.set(runnable);
                captured.countDown();
                return super.sendMessageAtTime(
                        message, SystemClock.uptimeMillis() + 60_000L);
            }
            return super.sendMessageAtTime(message, uptimeMillis);
        }

        private void dispatchCapturedNow() {
            Runnable queued = callback.getAndSet(null);
            assertNotNull("No solver callback was captured", queued);
            removeCallbacks(queued);
            assertTrue("Could not dispatch the captured solver callback", super.post(queued));
        }
    }
}
