package com.klotski.android;

import static org.junit.Assert.assertEquals;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * End-to-end Android smoke coverage for the app-level SlideDo flow.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityFlowTest {
    private static final String PACKAGE_NAME = "com.klotski.android";
    private static final String PREFS = "slidedo";
    private static final long TIMEOUT_MS = 15000;
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

        assertActivityButtonHasStartIcon(R.id.home_new_game_button);
        clickId(R.id.home_new_game_button);
        waitForActivityView(R.id.mode_3_button);
        assertActivityHasView(R.id.mode_4_button);
        assertActivityHasView(R.id.mode_5_button);
        assertActivityTextContains(R.id.mode_3_title_text, "3x3 Puzzle");
        assertActivityTextContains(R.id.mode_3_session_text, "1-3 minutes");
        assertActivityTextContains(R.id.mode_3_recommended_text, "Recommended");
        assertActivityTextContains(R.id.mode_4_session_text, "5-10 minutes");
        assertActivityTextContains(R.id.mode_5_session_text, "longer focused play");
        assertActivityContentDescriptionContains(R.id.mode_3_button, "Recommended first puzzle");
        assertActivityContentDescriptionContains(R.id.mode_4_button, "Best: No record yet");

        clickId(R.id.mode_4_button);
        waitForActivityView(R.id.game_board);
        assertActivityTextContains(R.id.game_title_text, "4x4 Puzzle");
        assertActivityHasView(R.id.game_undo_button);
        assertActivityHasView(R.id.game_restart_button);
        assertActivityHasView(R.id.game_assist_button);
        assertActivityContentDescriptionContains(R.id.game_board, "4x4 board");
        assertActivityContentDescriptionContains(R.id.game_board, "Empty cell at row");
        assertActivityContentDescriptionContains(R.id.game_board, "Rows:");
        assertActivityContentDescriptionContains(R.id.game_menu_button, "Open game menu");
        assertActivityContentDescriptionContains(R.id.game_undo_button, "Undo the previous move");
        assertActivityContentDescriptionContains(R.id.game_restart_button, "Restart this puzzle");
        assertActivityContentDescriptionContains(R.id.game_assist_button, "Solver Tools");
        assertActivityButtonHasStartIcon(R.id.game_menu_button);
        assertActivityButtonHasStartIcon(R.id.game_undo_button);
        assertActivityButtonHasStartIcon(R.id.game_restart_button);
        assertActivityButtonHasStartIcon(R.id.game_assist_button);
        assertActivityTextIsSingleLine(R.id.game_home_button);
        assertActivityTextIsSingleLine(R.id.game_menu_button);
    }

    @Test
    public void screenNavigationKeepsOutgoingContentUntilTransitionCompletes() throws Exception {
        markOnboardingSeen();
        launchApp();

        instrumentation.runOnMainSync(() -> {
            View play = activity.findViewById(R.id.home_new_game_button);
            assertNotNull(play);
            assertTrue(play.performClick());
            assertNotNull("Outgoing Home should remain during its exit animation",
                    activity.findViewById(R.id.home_root));
            assertFalse("Outgoing actions should be disabled during exit", play.isEnabled());
            assertNull(activity.findViewById(R.id.mode_root));
        });

        waitForId("mode_root");
    }

    @Test
    public void screenTransitionDisablesOutgoingBoardInteraction() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        instrumentation.runOnMainSync(() -> {
            View home = activity.findViewById(R.id.game_home_button);
            View board = activity.findViewById(R.id.game_board);
            assertNotNull(home);
            assertNotNull(board);
            assertTrue(home.performClick());
            assertFalse("Board should reject touch while Game exits", board.isEnabled());
            assertNotNull(activity.findViewById(R.id.game_root));
        });

        waitForId("home_root");
    }

    @Test
    public void reducedMotionSkipsScreenTransitionAnimation() throws Exception {
        markOnboardingSeen();
        targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("reduced_motion", true)
                .commit();
        launchApp();

        instrumentation.runOnMainSync(() -> {
            View play = activity.findViewById(R.id.home_new_game_button);
            assertNotNull(play);
            assertTrue(play.performClick());
            assertNotNull(activity.findViewById(R.id.mode_root));
            assertNull(activity.findViewById(R.id.home_root));
        });
    }

    @Test
    public void continueFromExistingSave() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();

        UiObject2 summary = waitForId("home_continue_summary_text");
        assertTrue(summary.getText().contains("Active save: 3x3"));
        assertTrue(summary.getText().contains("0 moves"));
        assertTrue(summary.getText().contains("0s"));
        assertTrue(summary.getText().contains("saved earlier"));
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        waitForText("3x3 Puzzle");
        waitForStatusContaining("0 moves");
    }

    @Test
    public void homeShowsContinueMetadataForCurrentSaveFormat() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 7, 123_000, true, false);
        launchApp();

        UiObject2 summary = waitForId("home_continue_summary_text");
        assertTrue(summary.getText().contains("Active save: 3x3"));
        assertTrue(summary.getText().contains("7 moves"));
        assertTrue(summary.getText().contains("123s"));
        assertTrue(summary.getText().contains("saved just now"));
    }

    @Test
    public void openHowToPlayFromHome() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_how_to_play_button);
        waitForId("how_root");
        waitForText("Goal");
        waitForText("Tap");
        scrollToText("Whole-line slide");
        scrollToText("Empty");
        assertActivityHasView(R.id.how_goal_example);
        assertActivityHasView(R.id.how_tap_example);
        assertActivityHasView(R.id.how_line_example);
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
                "Reduced motion. Skip board movement and screen transition animations");

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
        assertNull(findById("home_continue_summary_text"));
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
        assertNotNull(findById("records_explanation_text"));
        waitForTextContaining("Player solves only.");
        waitForTextContaining("Fewer moves rank first");
        waitForTextContaining("never replace these records");
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
        waitForText("Show Movable Tiles");
        waitForText("Solver Tools").click();
        waitForText("Advanced tools can finish the puzzle. Solver-assisted results never replace player records.");
        waitForText("BFS");
        waitForText("A*");
        waitForText("IDA*");
        device.pressBack();
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
        assertNotNull(findById("results_completion_mark"));
        assertActivityContentDescriptionContains(R.id.results_completion_mark, "Puzzle complete");
        assertNotNull(findById("results_play_again_button"));
        assertNotNull(findById("results_new_size_button"));
        assertNotNull(findById("results_home_button"));

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        assertNotNull(findById("records_explanation_text"));
        waitForTextContaining("Player solves only.");
        waitForTextContaining("1 move");
    }

    @Test
    public void reducedMotionSkipsCompletionMarkAnimation() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("reduced_motion", true)
                .commit());
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 2, 2);

        waitForActivityView(R.id.results_completion_mark);
        assertActivityViewScale(R.id.results_completion_mark, 1.0f);
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
        waitForTextContaining("never replace these records");
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
        waitForActivityView(R.id.game_board);

        device.setOrientationLeft();
        waitForGameBoardAfterRotation();

        device.setOrientationNatural();
        waitForGameBoardAfterRotation();
    }

    private void launchApp() throws Exception {
        RuntimeException lastLaunchError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            Intent intent = new Intent(targetContext, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                activity = instrumentation.startActivitySync(intent);
                instrumentation.waitForIdleSync();
                assertNotNull(activity);
                // AVD window hierarchies can lag startActivitySync under load; the
                // foreground window is the stable launch gate for these smoke tests.
                waitForForegroundApp();
                device.waitForIdle();
                Thread.sleep(750);
                return;
            } catch (RuntimeException error) {
                lastLaunchError = error;
                activity = null;
                device.executeShellCommand("am force-stop " + PACKAGE_NAME);
                device.pressHome();
                device.waitForIdle();
                Thread.sleep(1000L * attempt);
            }
        }
        throw lastLaunchError;
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

    private void writeSavedGameWithMetadata(String grid, String initialGrid, int moves, long elapsedMs,
            boolean active, boolean solved) {
        SharedPreferences.Editor editor = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.putInt("size", 3);
        editor.putString("grid", grid);
        editor.putString("initial_grid", initialGrid);
        editor.putInt("moves", moves);
        editor.putLong("elapsed", elapsedMs);
        editor.putLong("updated_at", System.currentTimeMillis());
        editor.putBoolean("active", active);
        editor.putBoolean("solved", solved);
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

    private void assertActivityHasView(int resourceId) {
        instrumentation.runOnMainSync(() ->
                assertNotNull("Missing activity view id: " + resourceId, activity.findViewById(resourceId)));
    }

    private void assertActivityButtonHasStartIcon(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity button id: " + resourceId, view);
            assertTrue("Activity view is not a Button: " + resourceId, view instanceof Button);
            assertNotNull("Missing start icon for activity button id: " + resourceId,
                    ((Button) view).getCompoundDrawablesRelative()[0]);
        });
    }

    private void assertActivityViewScale(int resourceId, float expectedScale) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity view id: " + resourceId, view);
            assertEquals("Unexpected scaleX for activity view id: " + resourceId,
                    expectedScale, view.getScaleX(), 0.001f);
            assertEquals("Unexpected scaleY for activity view id: " + resourceId,
                    expectedScale, view.getScaleY(), 0.001f);
        });
    }

    private void assertActivityTextIsSingleLine(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity text id: " + resourceId, view);
            assertTrue("Activity view is not a TextView: " + resourceId, view instanceof TextView);
            assertTrue("Activity text should stay on one line: " + resourceId,
                    ((TextView) view).isSingleLine());
        });
    }

    private void waitForActivityView(int resourceId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (activityHasView(resourceId)) {
                return;
            }
            Thread.sleep(100);
        }
        assertActivityHasView(resourceId);
    }

    private void waitForGameBoardAfterRotation() throws Exception {
        waitForForegroundApp();
        instrumentation.waitForIdleSync();
        waitForResumedMainActivity();
        waitForActivityView(R.id.game_board);
        assertActivityTextContains(R.id.game_title_text, "3x3 Puzzle");
    }

    private void waitForResumedMainActivity() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Activity[] resumedActivity = new Activity[1];
            instrumentation.runOnMainSync(() -> {
                Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for (Activity candidate : resumedActivities) {
                    if (candidate instanceof MainActivity) {
                        resumedActivity[0] = candidate;
                        return;
                    }
                }
            });
            if (resumedActivity[0] != null) {
                activity = resumedActivity[0];
                return;
            }
            Thread.sleep(100);
        }
        fail("MainActivity did not resume after rotation");
    }

    private boolean activityHasView(int resourceId) {
        boolean[] found = new boolean[1];
        instrumentation.runOnMainSync(() -> found[0] = activity.findViewById(resourceId) != null);
        return found[0];
    }

    private void assertActivityTextContains(int resourceId, String expectedText) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity text id: " + resourceId, view);
            assertTrue("Activity view is not a TextView: " + resourceId, view instanceof TextView);
            String actualText = ((TextView) view).getText().toString();
            assertTrue("Expected text for " + resourceId + " to contain \"" + expectedText
                    + "\" but was: " + actualText, actualText.contains(expectedText));
        });
    }

    private void assertActivityContentDescriptionContains(int resourceId, String expectedText) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity view id: " + resourceId, view);
            CharSequence description = view.getContentDescription();
            assertNotNull("Missing content description for activity view id: " + resourceId, description);
            assertTrue("Expected content description for " + resourceId + " to contain \"" + expectedText
                    + "\" but was: " + description, description.toString().contains(expectedText));
        });
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
            try {
                new UiScrollable(new UiSelector().scrollable(true)).scrollTextIntoView(text);
            } catch (UiObjectNotFoundException exception) {
                swipeUpUntilText(text);
            }
            device.waitForIdle();
        }
        return waitForText(text);
    }

    private void swipeUpUntilText(String text) throws InterruptedException {
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();
        for (int attempt = 0; attempt < 6 && device.findObject(By.text(text)) == null; attempt++) {
            device.swipe(width / 2, height * 3 / 4, width / 2, height / 4, 24);
            device.waitForIdle();
            Thread.sleep(200);
        }
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
