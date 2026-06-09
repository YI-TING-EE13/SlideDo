package com.klotski.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * End-to-end Android smoke coverage for the app-level SlideDo flow.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityFlowTest {
    private static final String PACKAGE_NAME = "com.klotski.android";
    private static final String PREFS = "slidedo";
    private static final long TIMEOUT_MS = 5000;
    private static final String LINE_SLIDE_GRID = "1,2,3,0,4,5,7,8,6";
    private static final String ONE_MOVE_WIN_GRID = "1,2,3,4,5,0,7,8,6";

    private Instrumentation instrumentation;
    private Activity activity;
    private UiDevice device;
    private Context targetContext;

    @Before
    public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        targetContext = instrumentation.getTargetContext();
        device.setOrientationNatural();
        clearAppPreferences();
    }

    @After
    public void tearDown() throws Exception {
        if (activity != null) {
            Activity launchedActivity = activity;
            instrumentation.runOnMainSync(launchedActivity::finish);
            instrumentation.waitForIdleSync();
            activity = null;
        }
        device.setOrientationNatural();
        device.pressHome();
    }

    @Test
    public void firstLaunchShowsOnboardingBeforeHome() throws Exception {
        launchApp();

        waitForId("onboarding_root");
        waitForText("Start with the Basics");
        waitForText("Page 1 of 4");
        assertNotNull(findById("onboarding_next_button"));
        assertNotNull(findById("onboarding_skip_button"));
        assertNull(findById("home_root"));
    }

    @Test
    public void onboardingSkipPersistsAndReturnsHome() throws Exception {
        launchApp();

        clickId(R.id.onboarding_skip_button);
        waitForId("home_root");
        assertTrue(isOnboardingSeen());

        relaunchApp();

        waitForId("home_root");
        assertNull(findById("onboarding_root"));
        waitForText("SlideDo");
        assertNotNull(findById("home_new_game_button"));
        assertNotNull(findById("home_onboarding_button"));
        assertNotNull(findById("home_tutorial_button"));
        assertNotNull(findById("home_how_to_play_button"));
        assertNotNull(findById("home_settings_button"));
        assertNotNull(findById("home_records_button"));
        assertNull(findById("home_continue_button"));
    }

    @Test
    public void onboardingStart3x3BeginsFirstPuzzle() throws Exception {
        launchApp();

        clickId(R.id.onboarding_next_button);
        waitForText("Page 2 of 4");
        clickId(R.id.onboarding_next_button);
        waitForText("Page 3 of 4");
        clickId(R.id.onboarding_next_button);
        waitForText("Page 4 of 4");
        clickId(R.id.onboarding_start_3_button);

        waitForId("game_root");
        waitForText("3x3 Puzzle");
        assertTrue(isOnboardingSeen());
    }

    @Test
    public void onboardingPracticeTutorialStartsGuidedLesson() throws Exception {
        launchApp();

        clickId(R.id.onboarding_next_button);
        waitForText("Page 2 of 4");
        clickId(R.id.onboarding_next_button);
        waitForText("Page 3 of 4");
        clickId(R.id.onboarding_next_button);
        waitForText("Page 4 of 4");
        assertNotNull(findById("onboarding_tutorial_button"));

        clickId(R.id.onboarding_tutorial_button);
        waitForId("tutorial_root");
        waitForText("Lesson 1 of 2");
        assertTrue(isOnboardingSeen());
    }

    @Test
    public void homeCanReopenBeginnerGuide() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_onboarding_button);
        waitForId("onboarding_root");
        waitForText("Start with the Basics");

        clickId(R.id.onboarding_skip_button);
        waitForId("home_root");
    }

    @Test
    public void launchDefaultHomeScreenWithoutSaveAfterOnboarding() throws Exception {
        markOnboardingSeen();
        launchApp();

        waitForId("home_root");
        waitForText("SlideDo");
        assertNotNull(findById("home_new_game_button"));
        assertNotNull(findById("home_onboarding_button"));
        assertNotNull(findById("home_tutorial_button"));
        assertNotNull(findById("home_how_to_play_button"));
        assertNotNull(findById("home_settings_button"));
        assertNotNull(findById("home_records_button"));
        assertNull(findById("home_continue_button"));
    }

    @Test
    public void practiceTutorialGuidesFirstMoveAndWholeLineSlide() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_tutorial_button);
        waitForId("tutorial_root");
        waitForText("Lesson 1 of 2");
        waitForText("Highlighted tiles share a row or column with the empty cell. Tap the emphasized 6 to make your first move.");
        assertNotNull(findById("tutorial_board"));

        tapCell(R.id.tutorial_board, 3, 2, 2);
        waitForText("Lesson 2 of 2");
        waitForText("Now tap the emphasized 5. It is farther away, so the model slides the whole row in one move.");

        tapCell(R.id.tutorial_board, 3, 1, 2);
        waitForText("Practice complete");
        waitForText("Line slide done: 1 move");

        clickId(R.id.tutorial_start_game_button);
        waitForId("game_root");
        waitForText("3x3 Puzzle");
    }

    @Test
    public void navigateHomeToModeSelectToGame() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_new_game_button);
        waitForId("mode_root");
        assertNotNull(findById("mode_3_button"));
        assertNotNull(findById("mode_4_button"));
        assertNotNull(findById("mode_5_button"));

        clickId(R.id.mode_4_button);
        waitForId("game_root");
        waitForText("4x4 Puzzle");
        assertNotNull(findById("game_board"));
        assertNotNull(findById("game_undo_button"));
        assertNotNull(findById("game_restart_button"));
        assertNotNull(findById("game_assist_button"));
        waitForContentDescriptionContaining("game_board", "4x4 board");
        waitForContentDescriptionContaining("game_board", "Empty cell at row");
        waitForContentDescriptionContaining("game_board", "Rows:");
        waitForContentDescriptionContaining("game_menu_button", "Open game menu");
        waitForContentDescriptionContaining("game_undo_button", "Undo the previous move");
        waitForContentDescriptionContaining("game_restart_button", "Restart this puzzle");
        waitForContentDescriptionContaining("game_assist_button", "Open assist actions");
    }

    @Test
    public void continueFromExistingSave() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();

        clickId(R.id.home_continue_button);
        waitForId("game_root");
        waitForText("3x3 Puzzle");
        waitForStatusContaining("0 moves");
    }

    @Test
    public void openHowToPlayFromHome() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_how_to_play_button);
        waitForId("how_root");
        waitForText("Goal");
        waitForText("Tap");
        waitForText("Whole-line slide");
        waitForText("Empty");
        assertNotNull(findById("how_goal_example"));
        assertNotNull(findById("how_tap_example"));
        assertNotNull(findById("how_line_example"));
        scrollToText("Swipe");
        scrollToText("Records");
        clickId(R.id.how_back_button);
        waitForId("home_root");
    }

    @Test
    public void settingsFromHomeTogglesPreferences() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        waitForText("Haptic feedback");
        waitForText("Reduced motion");
        assertNotNull(findById("settings_haptic_switch"));
        assertNotNull(findById("settings_reduced_motion_switch"));
        waitForContentDescriptionContaining("settings_haptic_switch",
                "Haptic feedback. Use short vibration feedback");
        waitForContentDescriptionContaining("settings_reduced_motion_switch",
                "Reduced motion. Complete board moves without transition animation");

        toggleSwitch(R.id.settings_haptic_switch);
        toggleSwitch(R.id.settings_reduced_motion_switch);

        SharedPreferences prefs = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        assertFalse(prefs.getBoolean("haptic_enabled", true));
        assertTrue(prefs.getBoolean("reduced_motion", false));
    }

    @Test
    public void settingsCanResetSavedGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        assertNotNull(findById("home_continue_button"));

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        clickId(R.id.settings_reset_save_button);
        waitForText("Reset saved game?");
        waitForText("RESET").click();
        device.waitForIdle();
        instrumentation.waitForIdleSync();

        waitForId("settings_root");
        clickId(R.id.settings_back_button);
        waitForId("home_root");
        assertNull(findById("home_continue_button"));
    }

    @Test
    public void settingsCanResetRecords() throws Exception {
        markOnboardingSeen();
        writeBestRecords();
        launchApp();

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        clickId(R.id.settings_reset_records_button);
        waitForText("Reset records?");
        waitForText("RESET").click();
        device.waitForIdle();
        instrumentation.waitForIdleSync();

        waitForId("settings_root");
        clickId(R.id.settings_back_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("No record yet");
    }

    @Test
    public void gameMenuOpensSettingsAndBackReturnsToGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        waitForText("Settings").click();
        device.waitForIdle();

        waitForId("settings_root");
        clickId(R.id.settings_back_button);
        waitForId("game_root");
    }

    @Test
    public void gameMenuShowsQuickReminder() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        UiObject2 reminder = waitForText("Quick Reminder");
        reminder.click();
        device.waitForIdle();

        waitForText("Move reminder");
        waitForText("Tap or swipe any tile aligned with the empty cell. Farther aligned tiles slide the whole line as one move. Undo backs up one gesture.");
        device.pressBack();
        waitForId("game_root");
    }

    @Test
    public void assistShowsMovableTileHintWithoutMoving() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_assist_button);
        waitForText("Show Movable Tiles").click();
        device.waitForIdle();
        instrumentation.waitForIdleSync();

        waitForStatusContaining("Hint: highlighted tiles can slide into the empty cell.");
        waitForStatusContaining("0 moves");
        waitForContentDescriptionContaining("game_board",
                "4 highlighted tiles can slide into the empty cell.");

        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");
        waitForStatusNotContaining("Hint: highlighted tiles can slide into the empty cell.");
    }

    @Test
    public void playerWinShowsResultsAndRecordsBest() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 2, 2);

        waitForId("results_root");
        waitForText("Results");
        waitForText("Puzzle solved.");
        waitForText("First player record for this size.");
        assertNotNull(findById("results_play_again_button"));
        assertNotNull(findById("results_new_size_button"));
        assertNotNull(findById("results_home_button"));

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForTextContaining("1 move");
    }

    @Test
    public void assistedWinShowsResultsWithoutRecord() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        setActivityField("assistedSolveActive", true);

        tapCell(3, 2, 2);

        waitForId("results_root");
        waitForText("Solved with assist.");
        waitForText("Assist result not saved. Player best: No record yet");

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("No record yet");
    }

    @Test
    public void resultsActionsNavigateToReplayAndModeSelect() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        tapCell(3, 2, 2);
        waitForId("results_root");

        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        waitForText("3x3 Puzzle");

        invokeActivityMethod("onGameWon", new Class<?>[] {int.class, long.class}, 1, 0L);
        waitForId("results_root");
        clickId(R.id.results_new_size_button);
        waitForId("mode_root");
    }

    @Test
    public void wholeLineMoveCountsOnceAndUndoRestoresIt() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");

        clickId(R.id.game_undo_button);
        waitForStatusContaining("0 moves");
    }

    @Test
    public void saveLoadRestoresMovedStateAfterRestart() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");
        invokeActivityMethod("saveGame");

        clickId(R.id.game_restart_button);
        waitForStatusContaining("0 moves");

        assertTrue((Boolean) invokeActivityMethod("loadGame"));
        waitForStatusContaining("1 move");
    }

    @Test
    public void rotationKeepsCurrentGameScreen() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        device.setOrientationLeft();
        waitForId("game_root");
        assertNotNull(findById("game_board"));

        device.setOrientationNatural();
        waitForId("game_root");
        assertNotNull(findById("game_board"));
    }

    private void launchApp() throws Exception {
        Intent intent = new Intent(targetContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        assertNotNull(activity);
        assertTrue("App package did not appear",
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT_MS));
        waitForForegroundApp();
        device.waitForIdle();
        Thread.sleep(750);
    }

    private void relaunchApp() throws Exception {
        if (activity != null) {
            Activity launchedActivity = activity;
            instrumentation.runOnMainSync(launchedActivity::finish);
            instrumentation.waitForIdleSync();
            activity = null;
        }
        launchApp();
    }

    private void waitForForegroundApp() throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String windowDump = device.executeShellCommand("dumpsys window");
            if ((windowDump.contains(PACKAGE_NAME + "/.MainActivity")
                    || windowDump.contains(PACKAGE_NAME + "/" + PACKAGE_NAME + ".MainActivity"))
                    && !windowDump.contains("Splash Screen " + PACKAGE_NAME)) {
                return;
            }
            Thread.sleep(100);
        }
        fail("App window did not become foreground");
    }

    private void clearAppPreferences() {
        targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        device.waitForIdle();
    }

    private void writeSavedGame(String grid, String initialGrid, int moves) {
        SharedPreferences.Editor editor = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.putInt("size", 3);
        editor.putString("grid", grid);
        editor.putString("initial_grid", initialGrid);
        editor.putInt("moves", moves);
        editor.putLong("elapsed", 0);
        editor.putInt("last_size", 3);
        editor.putBoolean("onboarding_seen", true);
        assertTrue(editor.commit());
    }

    private void markOnboardingSeen() {
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("onboarding_seen", true)
                .commit());
    }

    private boolean isOnboardingSeen() {
        return targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("onboarding_seen", false);
    }

    private void writeBestRecords() {
        SharedPreferences.Editor editor = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt("best_3_moves", 20);
        editor.putLong("best_3_time", 30000);
        editor.putInt("best_4_moves", 80);
        editor.putLong("best_4_time", 120000);
        editor.putInt("best_5_moves", 160);
        editor.putLong("best_5_time", 300000);
        assertTrue(editor.commit());
    }

    private UiObject2 waitForId(String resourceName) {
        UiObject2 object = device.wait(Until.findObject(By.res(PACKAGE_NAME, resourceName)), TIMEOUT_MS);
        assertNotNull("Missing view id: " + resourceName, object);
        return object;
    }

    private UiObject2 findById(String resourceName) {
        return device.findObject(By.res(PACKAGE_NAME, resourceName));
    }

    private UiObject2 waitForText(String text) {
        UiObject2 object = device.wait(Until.findObject(By.text(text)), TIMEOUT_MS);
        assertNotNull("Missing text: " + text, object);
        return object;
    }

    private UiObject2 waitForTextContaining(String text) {
        UiObject2 object = device.wait(Until.findObject(By.textContains(text)), TIMEOUT_MS);
        assertNotNull("Missing text containing: " + text, object);
        return object;
    }

    private void waitForContentDescriptionContaining(String resourceName, String text) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            UiObject2 object = findById(resourceName);
            CharSequence description = object == null ? null : object.getContentDescription();
            if (description != null && description.toString().contains(text)) {
                return;
            }
            Thread.sleep(100);
        }
        UiObject2 object = findById(resourceName);
        String description = object == null ? "<missing>" : String.valueOf(object.getContentDescription());
        fail("Expected content description for " + resourceName
                + " to contain \"" + text + "\" but was: " + description);
    }

    private UiObject2 scrollToText(String text) throws Exception {
        UiObject2 object = device.findObject(By.text(text));
        if (object == null) {
            new UiScrollable(new UiSelector().scrollable(true)).scrollTextIntoView(text);
            device.waitForIdle();
        }
        return waitForText(text);
    }

    private void clickId(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing view id: " + resourceId, view);
            assertTrue("View click was not handled: " + resourceId, view.performClick());
        });
        instrumentation.waitForIdleSync();
    }

    private void toggleSwitch(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing switch id: " + resourceId, view);
            assertTrue("View is not a switch: " + resourceId, view instanceof CompoundButton);
            CompoundButton switchView = (CompoundButton) view;
            switchView.setChecked(!switchView.isChecked());
        });
        instrumentation.waitForIdleSync();
    }

    private void waitForStatusContaining(String text) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            UiObject2 status = findById("game_status_text");
            if (status != null && status.getText() != null && status.getText().contains(text)) {
                return;
            }
            Thread.sleep(100);
        }
        UiObject2 status = findById("game_status_text");
        fail("Expected status to contain \"" + text + "\" but was: "
                + (status == null ? "<missing>" : status.getText()));
    }

    private void waitForStatusNotContaining(String text) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        UiObject2 status = null;
        while (System.currentTimeMillis() < deadline) {
            status = findById("game_status_text");
            if (status != null && status.getText() != null && !status.getText().contains(text)) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Expected status not to contain \"" + text + "\" but was: "
                + (status == null ? "<missing>" : status.getText()));
    }

    private void tapCell(int size, int row, int col) throws InterruptedException {
        tapCell(R.id.game_board, size, row, col);
    }

    private void tapCell(int boardResourceId, int size, int row, int col) throws InterruptedException {
        instrumentation.runOnMainSync(() -> {
            View board = activity.findViewById(boardResourceId);
            assertNotNull("Missing board view id: " + boardResourceId, board);
            float density = targetContext.getResources().getDisplayMetrics().density;
            float gap = 10f * density;
            float boardSize = Math.min(board.getWidth(), board.getHeight());
            float tileSize = (boardSize - (size + 1) * gap) / size;
            float boardLeft = (board.getWidth() - boardSize) / 2f;
            float boardTop = (board.getHeight() - boardSize) / 2f;
            float x = boardLeft + gap + col * (tileSize + gap) + tileSize / 2f;
            float y = boardTop + gap + row * (tileSize + gap) + tileSize / 2f;
            assertFalse("Computed x is outside board bounds", x < 0 || x > board.getWidth());
            assertFalse("Computed y is outside board bounds", y < 0 || y > board.getHeight());

            long downTime = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(downTime, downTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(downTime, downTime + 50,
                    MotionEvent.ACTION_UP, x, y, 0);
            board.dispatchTouchEvent(down);
            board.dispatchTouchEvent(up);
            down.recycle();
            up.recycle();
        });
        instrumentation.waitForIdleSync();
        Thread.sleep(250);
    }

    private Object invokeActivityMethod(String methodName) throws Exception {
        return invokeActivityMethod(methodName, new Class<?>[0]);
    }

    private Object invokeActivityMethod(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Object[] result = new Object[1];
        Throwable[] error = new Throwable[1];
        instrumentation.runOnMainSync(() -> {
            try {
                Method method = MainActivity.class.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                result[0] = method.invoke(activity, args);
            } catch (Throwable throwable) {
                error[0] = throwable;
            }
        });
        instrumentation.waitForIdleSync();
        if (error[0] != null) {
            throw new AssertionError("Failed to invoke MainActivity." + methodName, error[0]);
        }
        return result[0];
    }

    private void setActivityField(String fieldName, Object value) throws Exception {
        Throwable[] error = new Throwable[1];
        instrumentation.runOnMainSync(() -> {
            try {
                Field field = MainActivity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(activity, value);
            } catch (Throwable throwable) {
                error[0] = throwable;
            }
        });
        instrumentation.waitForIdleSync();
        if (error[0] != null) {
            throw new AssertionError("Failed to set MainActivity." + fieldName, error[0]);
        }
    }
}
