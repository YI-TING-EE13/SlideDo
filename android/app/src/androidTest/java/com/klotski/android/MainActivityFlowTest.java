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
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
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

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.DailyChallenge;
import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

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
    private static final int[][] ONE_MOVE_WIN_GRID_4 = {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12},
            {13, 14, 0, 15}
    };

    private Instrumentation instrumentation;
    private Activity activity;
    private UiDevice device;
    private Context targetContext;

    @Before
    public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        targetContext = instrumentation.getTargetContext();
        setPortraitOrientation();
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
        setPortraitOrientation();
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
        assertActivityHasView(R.id.home_new_game_button);
        assertActivityHasView(R.id.home_onboarding_button);
        assertActivityHasView(R.id.home_tutorial_button);
        assertActivityHasView(R.id.home_how_to_play_button);
        assertActivityHasView(R.id.home_settings_button);
        assertActivityHasView(R.id.home_records_button);
        scrollToText("Settings");
        scrollToText("Records");
        assertActivityMissingView(R.id.home_continue_button);
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
        waitForText("3x3 · Classic");
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
        assertActivityHasView(R.id.home_new_game_button);
        assertActivityHasView(R.id.home_onboarding_button);
        assertActivityHasView(R.id.home_tutorial_button);
        assertActivityHasView(R.id.home_how_to_play_button);
        assertActivityHasView(R.id.home_settings_button);
        assertActivityHasView(R.id.home_records_button);
        scrollToText("Settings");
        scrollToText("Records");
        assertActivityMissingView(R.id.home_continue_button);
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
        waitForText("3x3 · Classic");
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
        assertActivityContentDescriptionContains(R.id.mode_4_button, "Classic best: No record yet");

        clickId(R.id.mode_4_button);
        waitForText("Choose Difficulty");
        waitForText("Challenge").click();
        waitForActivityView(R.id.game_board);
        assertActivityTextContains(R.id.game_title_text, "4x4 · Challenge");
        assertActivityHasView(R.id.game_undo_button);
        assertActivityHasView(R.id.game_redo_button);
        assertActivityHasView(R.id.game_restart_button);
        assertActivityHasView(R.id.game_assist_button);
        assertActivityContentDescriptionContains(R.id.game_board, "4x4 board");
        assertActivityContentDescriptionContains(R.id.game_board, "Empty cell at row");
        assertActivityContentDescriptionContains(R.id.game_board, "Rows:");
        assertActivityContentDescriptionContains(R.id.game_menu_button, "Open game menu");
        assertActivityContentDescriptionContains(R.id.game_undo_button, "Undo the previous move");
        assertActivityContentDescriptionContains(R.id.game_redo_button, "Redo the next undone move");
        assertActivityContentDescriptionContains(R.id.game_restart_button, "Restart this puzzle");
        assertActivityContentDescriptionContains(R.id.game_assist_button, "Solver Tools");
        assertActivityButtonHasStartIcon(R.id.game_menu_button);
        assertActivityButtonHasStartIcon(R.id.game_undo_button);
        assertActivityButtonHasStartIcon(R.id.game_redo_button);
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
        waitForText("3x3 · Classic");
        waitForStatusContaining("0 moves");
    }

    @Test
    public void multipleSizeSavesOfferTheCorrectContinueChoice() throws Exception {
        writeIndependentSizeSlots();
        launchApp();

        waitForId("home_root");
        waitForText("2 saved games. Choose a size to continue.");
        clickId(R.id.home_continue_button);
        waitForText("Choose saved game");
        waitForTextContaining("3x3 · Classic");
        waitForTextContaining("4x4 · Challenge").click();
        waitForId("game_root");
        waitForText("4x4 · Challenge");
        waitForStatusContaining("4 moves");

        clickId(R.id.game_home_button);
        waitForId("home_root");
        clickId(R.id.home_continue_button);
        waitForText("Choose saved game");
        waitForTextContaining("3x3 · Classic").click();
        waitForId("game_root");
        waitForText("3x3 · Classic");
        waitForStatusContaining("3 moves");
    }

    @Test
    public void multipleSaveChooserIsLocalizedInChineseAndJapanese() throws Exception {
        writeIndependentSizeSlots();
        setLanguage(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        launchApp();

        waitForText("共有 2 個存檔。請選擇要繼續的尺寸。");
        clickId(R.id.home_continue_button);
        waitForText("選擇遊戲存檔");
        waitForTextContaining("3x3 · 經典");
        waitForTextContaining("4x4 · 挑戰");
        device.pressBack();

        setLanguage(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        relaunchApp();
        waitForText("セーブデータが2件あります。続けるサイズを選んでください。");
        clickId(R.id.home_continue_button);
        waitForText("セーブデータを選択");
        waitForTextContaining("3x3 · クラシック");
        waitForTextContaining("4x4 · チャレンジ");
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
    public void continueSummaryAndGameTitlePreserveDifficulty() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 3, 12_000, true, false);
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("difficulty", "challenge")
                .commit());
        launchApp();

        UiObject2 summary = waitForId("home_continue_summary_text");
        assertTrue(summary.getText().contains("3x3 · Challenge"));
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "3x3 · Challenge");
    }

    @Test
    public void openHowToPlayFromHome() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_how_to_play_button);
        waitForId("how_root");
        waitForText("Goal");
        scrollToText("Tap");
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
        waitForText("Visual theme: Midnight");
        waitForText("Haptic feedback");
        scrollToText("Sound feedback");
        scrollToText("Reduced motion");
        assertActivityHasView(R.id.settings_theme_button);
        assertActivityHasView(R.id.settings_haptic_switch);
        assertActivityHasView(R.id.settings_sound_switch);
        assertActivityHasView(R.id.settings_reduced_motion_switch);
        assertActivityContentDescriptionContains(R.id.settings_haptic_switch,
                "Haptic feedback. Use short vibration feedback");
        assertActivityContentDescriptionContains(R.id.settings_sound_switch,
                "Sound feedback. Play short local tones");
        assertActivityContentDescriptionContains(R.id.settings_reduced_motion_switch,
                "Reduced motion. Skip board movement and screen transition animations");

        toggleSwitch(R.id.settings_haptic_switch);
        toggleSwitch(R.id.settings_sound_switch);
        toggleSwitch(R.id.settings_reduced_motion_switch);

        SharedPreferences prefs = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        assertFalse(prefs.getBoolean("haptic_enabled", true));
        assertTrue(prefs.getBoolean("sound_enabled", false));
        assertTrue(prefs.getBoolean("reduced_motion", false));
    }

    @Test
    public void settingsOffersPersonalDataBackupActions() throws Exception {
        markOnboardingSeen();
        launchApp();

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        scrollToText("Export backup");
        assertActivityHasView(R.id.settings_export_backup_button);
        scrollToText("Import backup");
        assertActivityHasView(R.id.settings_import_backup_button);
    }

    @Test
    public void confirmedPersonalDataImportReplacesSettingsAndRecreatesActivity() throws Exception {
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("onboarding_seen", true)
                .putBoolean("sound_enabled", true)
                .commit());
        launchApp();

        String backup = "{\"format\":\"slidedo-personal-data\",\"version\":1,"
                + "\"createdAt\":123,\"entries\":[{\"key\":\"onboarding_seen\","
                + "\"type\":\"boolean\",\"value\":true}]}";
        Activity activityBeforeRestore = activity;
        invokeActivityMethod("confirmImportPersonalData", new Class<?>[] {String.class}, backup);
        waitForText("Restore backup?");
        waitForText("RESTORE").click();
        waitForRecreatedMainActivity(activityBeforeRestore);
        waitForId("home_root");

        SharedPreferences restored = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        assertTrue(restored.getBoolean("onboarding_seen", false));
        assertFalse(restored.getBoolean("sound_enabled", false));
    }

    @Test
    public void themeSelectionPersistsAndPreservesActiveGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("sound_enabled", true)
                .commit());
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");

        clickId(R.id.game_menu_button);
        scrollToText("Settings").click();
        device.waitForIdle();
        waitForId("settings_root");
        waitForText("Visual theme: Midnight").click();
        waitForText("Choose visual theme");
        waitForText("Ocean").click();
        waitForResumedMainActivity();

        waitForId("settings_root");
        waitForText("Visual theme: Ocean");
        SharedPreferences prefs = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        assertEquals("ocean", prefs.getString("visual_theme", null));
        assertActivityBackgroundColor(R.id.settings_root, AndroidVisualTheme.OCEAN.background);

        clickId(R.id.settings_back_button);
        waitForId("game_root");
        waitForStatusContaining("1 move");
        assertActivityBackgroundColor(R.id.game_root, AndroidVisualTheme.OCEAN.background);

        relaunchApp();
        waitForId("home_root");
        assertActivityBackgroundColor(R.id.home_root, AndroidVisualTheme.OCEAN.background);
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        waitForStatusContaining("1 move");
    }

    @Test
    public void languageSelectionPersistsAndPreservesActiveGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");

        clickId(R.id.game_menu_button);
        scrollToText("Settings").click();
        device.waitForIdle();
        waitForId("settings_root");
        waitForText("App language: English").click();
        waitForText("Choose language");
        waitForText("繁體中文").click();
        device.waitForIdle();

        waitForId("settings_root");
        waitForText("設定");
        waitForText("應用程式語言：繁體中文");
        assertEquals("zh-TW", targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("language_tag", null));

        clickId(R.id.settings_back_button);
        waitForId("game_root");
        waitForText("3x3 · 經典");
        waitForStatusContaining("1 次移動");

        relaunchApp();
        waitForId("home_root");
        waitForText("開始新遊戲");
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        waitForStatusContaining("1 次移動");
    }

    @Test
    public void japaneseLanguageSelectionPersistsAndPreservesActiveGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");

        clickId(R.id.game_menu_button);
        scrollToText("Settings").click();
        device.waitForIdle();
        waitForId("settings_root");
        waitForText("App language: English").click();
        waitForText("Choose language");
        waitForText("日本語").click();
        device.waitForIdle();

        waitForId("settings_root");
        waitForText("設定");
        waitForText("アプリの言語：日本語");
        assertEquals("ja-JP", targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("language_tag", null));

        clickId(R.id.settings_back_button);
        waitForId("game_root");
        waitForText("3x3 · クラシック");
        waitForStatusContaining("1手");

        relaunchApp();
        waitForId("home_root");
        waitForText("新しいゲーム");
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        waitForStatusContaining("1手");
    }

    @Test
    public void traditionalChineseMajorScreensAndDialogsAreLocalized() throws Exception {
        setLanguage(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        launchApp();

        waitForId("onboarding_root");
        waitForText("從基礎開始");
        waitForText("第 1 頁，共 4 頁");
        clickId(R.id.onboarding_skip_button);
        waitForId("home_root");
        waitForText("開始遊戲");
        waitForText("新手指南");

        clickId(R.id.home_daily_button);
        waitForId("daily_calendar_root");
        waitForText("每日月曆");
        assertActivityTextContains(R.id.daily_calendar_month_text, "年");
        waitForText("上個月");
        clickId(R.id.daily_calendar_back_button);
        waitForId("home_root");

        clickId(R.id.home_tutorial_button);
        waitForId("tutorial_root");
        waitForText("練習教學");
        waitForTextContaining("點按標示的 6");
        clickId(R.id.tutorial_home_button);
        waitForId("home_root");

        clickId(R.id.home_new_game_button);
        waitForId("mode_root");
        assertActivityTextContains(R.id.mode_3_title_text, "3x3 拼圖");
        assertActivityTextContains(R.id.mode_4_title_text, "4x4 拼圖");
        assertActivityTextContains(R.id.mode_5_title_text, "5x5 拼圖");
        assertActivityTextContains(R.id.mode_3_recommended_text, "推薦");
        assertActivityContentDescriptionContains(R.id.mode_3_button, "建議從這個拼圖開始");

        clickId(R.id.mode_4_button);
        waitForText("選擇難度");
        waitForText("挑戰").click();
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "4x4 · 挑戰");
        assertActivityTextContains(R.id.game_status_text, "0 次移動");
        assertActivityContentDescriptionContains(R.id.game_board, "空格位於");
        assertActivityContentDescriptionContains(R.id.game_menu_button, "開啟遊戲選單");

        clickId(R.id.game_menu_button);
        scrollToText("快速提示").click();
        waitForText("移動提示");
        waitForTextContaining("整列一起滑動");
        device.pressBack();
        waitForId("game_root");

        clickId(R.id.game_assist_button);
        waitForText("顯示可移動方塊");
        waitForText("解題器工具").click();
        waitForTextContaining("絕不會取代玩家紀錄");
        waitForText("BFS").click();
        waitForText("警告");
        waitForTextContaining("耗盡記憶體");
        waitForText("關閉").click();
        waitForId("game_root");

        clickId(R.id.game_home_button);
        waitForId("home_root");
        clickId(R.id.home_how_to_play_button);
        waitForId("how_root");
        waitForText("目標");
        scrollToText("整列滑動");
        scrollToText("紀錄");
        clickId(R.id.how_back_button);
        waitForId("home_root");

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        waitForText("應用程式語言：繁體中文");
        clickId(R.id.settings_reset_save_button);
        waitForText("要重設所有遊戲存檔嗎？");
        waitForText("取消").click();
        clickId(R.id.settings_reset_records_button);
        waitForText("要重設紀錄嗎？");
        waitForText("取消").click();
        clickId(R.id.settings_back_button);
        waitForId("home_root");

        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("紀錄");
        waitForTextContaining("只記錄玩家自行完成的成績");
        assertActivityContainsText("尚無紀錄");
    }

    @Test
    public void traditionalChinesePlayerResultAndRecordAreLocalized() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        setLanguage(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 2, 2);

        waitForId("results_root");
        waitForText("結果");
        waitForText("拼圖完成。");
        waitForText("這是此尺寸與難度的第一筆玩家紀錄。");
        assertActivityTextContains(R.id.results_stats_text, "1 次移動");
        assertActivityTextContains(R.id.results_play_again_button, "重玩本盤");
        assertActivityContentDescriptionContains(R.id.results_completion_mark, "拼圖完成");

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        assertActivityContainsText("1 次移動");
    }

    @Test
    public void japaneseMajorScreensAndDialogsAreLocalized() throws Exception {
        setLanguage(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        launchApp();

        waitForId("onboarding_root");
        waitForText("基本から始めよう");
        waitForText("全4ページ中1ページ");
        clickId(R.id.onboarding_skip_button);
        waitForId("home_root");
        waitForText("プレイ");
        waitForText("初心者ガイド");

        clickId(R.id.home_daily_button);
        waitForId("daily_calendar_root");
        waitForText("デイリーカレンダー");
        assertActivityTextContains(R.id.daily_calendar_month_text, "年");
        waitForText("前の月");
        clickId(R.id.daily_calendar_back_button);
        waitForId("home_root");

        clickId(R.id.home_tutorial_button);
        waitForId("tutorial_root");
        waitForText("練習チュートリアル");
        waitForTextContaining("目立つように表示された6");
        clickId(R.id.tutorial_home_button);
        waitForId("home_root");

        clickId(R.id.home_new_game_button);
        waitForId("mode_root");
        assertActivityTextContains(R.id.mode_3_title_text, "3x3 パズル");
        assertActivityTextContains(R.id.mode_4_title_text, "4x4 パズル");
        assertActivityTextContains(R.id.mode_5_title_text, "5x5 パズル");
        assertActivityTextContains(R.id.mode_3_recommended_text, "おすすめ");
        assertActivityContentDescriptionContains(R.id.mode_3_button, "最初のパズルにおすすめ");

        clickId(R.id.mode_4_button);
        waitForText("難易度を選ぶ");
        waitForText("チャレンジ").click();
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "4x4 · チャレンジ");
        assertActivityTextContains(R.id.game_status_text, "0手");
        assertActivityContentDescriptionContains(R.id.game_board, "空きマスは");
        assertActivityContentDescriptionContains(R.id.game_menu_button, "ゲームメニューを開きます");

        clickId(R.id.game_menu_button);
        scrollToText("操作の確認").click();
        waitForText("動かし方");
        waitForTextContaining("まとめて動きます");
        device.pressBack();
        waitForId("game_root");

        clickId(R.id.game_assist_button);
        waitForText("動かせるタイルを表示");
        waitForText("ソルバーツール").click();
        waitForTextContaining("プレイヤーの記録を上書き");
        waitForText("BFS").click();
        waitForText("警告");
        waitForTextContaining("メモリ不足");
        waitForText("閉じる").click();
        waitForId("game_root");

        clickId(R.id.game_home_button);
        waitForId("home_root");
        clickId(R.id.home_how_to_play_button);
        waitForId("how_root");
        waitForText("目的");
        scrollToText("まとめてスライド");
        scrollToText("記録");
        clickId(R.id.how_back_button);
        waitForId("home_root");

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        waitForText("アプリの言語：日本語");
        clickId(R.id.settings_reset_save_button);
        waitForText("すべてのセーブをリセットしますか？");
        waitForText("キャンセル").click();
        clickId(R.id.settings_reset_records_button);
        waitForText("記録をリセットしますか？");
        waitForText("キャンセル").click();
        clickId(R.id.settings_back_button);
        waitForId("home_root");

        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("記録");
        waitForTextContaining("自力で解いた結果だけを記録");
        assertActivityContainsText("記録はまだありません");
    }

    @Test
    public void japanesePlayerResultAndRecordAreLocalized() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        setLanguage(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 2, 2);

        waitForId("results_root");
        waitForText("結果");
        waitForText("パズルをクリアしました。");
        waitForText("このサイズと難易度で最初のプレイヤー記録です。");
        assertActivityTextContains(R.id.results_stats_text, "1手");
        assertActivityTextContains(R.id.results_play_again_button, "同じ問題を再挑戦");
        assertActivityContentDescriptionContains(R.id.results_completion_mark, "パズルクリア");

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        assertActivityContainsText("1手");
    }

    @Test
    public void settingsCanResetAllSavedGames() throws Exception {
        writeIndependentSizeSlots();
        launchApp();
        assertNotNull(findById("home_continue_button"));

        clickId(R.id.home_settings_button);
        waitForId("settings_root");
        clickId(R.id.settings_reset_save_button);
        waitForText("Reset all saved games?");
        waitForText("RESET").click();
        device.waitForIdle();
        instrumentation.waitForIdleSync();

        waitForId("settings_root");
        clickId(R.id.settings_back_button);
        waitForId("home_root");
        assertNull(findById("home_continue_button"));
        assertNull(findById("home_continue_summary_text"));
        assertEquals(0, new AndroidGameStore(targetContext).getAllSaveMetadata().length);
    }

    @Test
    public void favoriteLibraryReplaysExactPuzzleWithoutChangingSavesOrRecords() throws Exception {
        markOnboardingSeen();
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 4);
        AndroidGameStore store = new AndroidGameStore(targetContext);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        scrollToText("Save Favorite").click();
        waitForText("Save favorite puzzle");
        UiObject2 name = device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT_MS);
        assertNotNull("Missing favorite name input", name);
        name.setText("One move finish");
        waitForText("SAVE FAVORITE").click();
        device.waitForIdle();
        instrumentation.waitForIdleSync();
        assertEquals(1, store.getFavoritePuzzles().length);

        clickId(R.id.game_home_button);
        waitForId("home_root");
        scrollToText("Favorite Puzzles");
        clickId(R.id.home_favorites_button);
        waitForId("favorites_root");
        waitForText("One move finish");
        clickActivityContentDescriptionContaining("Replay favorite One move finish");

        waitForId("game_root");
        waitForStatusContaining("Favorite practice does not change personal records or regular saves.");
        tapCell(3, 2, 2);
        waitForId("results_root");
        waitForText("Favorite practice complete.");
        waitForText("Practice result only. Personal records and statistics were not changed.");
        assertActivityHasView(R.id.results_favorite_button);

        assertEquals(4, store.loadSavedGame(3).moveCount);
        assertEquals(0, store.getCompletionHistory().length);
        assertEquals(0, store.getOverallCompletionStats().playerCompletions);
    }

    @Test
    public void settingsCanResetRecords() throws Exception {
        markOnboardingSeen();
        writeBestRecords();
        AndroidGameStore store = new AndroidGameStore(targetContext);
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 20, 30_000L, false, 100L);
        store.recordCompletion(4, PuzzleDifficulty.CHALLENGE, 80, 120_000L, true, 200L);
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
        assertActivityContainsText("No record yet");
        assertActivityContainsText("No completed puzzles yet");
        assertEquals(0, store.getCompletionHistory().length);
        assertEquals(0, store.getOverallCompletionStats().playerCompletions);
        assertEquals(0, store.getOverallCompletionStats().assistedCompletions);
    }

    @Test
    public void gameMenuOpensSettingsAndBackReturnsToGame() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        scrollToText("Settings").click();
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
        UiObject2 reminder = scrollToText("Quick Reminder");
        reminder.click();
        device.waitForIdle();

        waitForText("Move reminder");
        waitForText("Tap or swipe any tile aligned with the empty cell. Farther aligned tiles slide the whole line as one move. Undo backs up one gesture.");
        device.pressBack();
        waitForId("game_root");
    }

    @Test
    public void gameMenuPausesElapsedPlayTime() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0, 5_000L, true, false);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        waitForText("Resume");
        invokeActivityMethod("saveGame");
        long elapsedAtMenu = savedElapsedForSize(3);
        SystemClock.sleep(2_200L);
        waitForText("Save").click();
        device.waitForIdle();

        long savedElapsed = savedElapsedForSize(3);
        assertTrue("Menu time should not be added to elapsed play time: " + savedElapsed,
                savedElapsed >= elapsedAtMenu && savedElapsed - elapsedAtMenu < 1_200L);
    }

    @Test
    public void quickReminderKeepsElapsedPlayTimePausedUntilClosed() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0, 5_000L, true, false);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        scrollToText("Quick Reminder").click();
        waitForText("Move reminder");
        invokeActivityMethod("saveGame");
        long elapsedAtReminder = savedElapsedForSize(3);
        SystemClock.sleep(2_200L);
        device.pressHome();
        device.waitForIdle();

        long savedElapsed = savedElapsedForSize(3);
        assertTrue("Nested reminder time should not be added to elapsed play time: " + savedElapsed,
                savedElapsed >= elapsedAtReminder && savedElapsed - elapsedAtReminder < 1_200L);
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
        invokeActivityMethod("saveGame");
        assertFalse(new AndroidGameStore(targetContext).isSavedGameAssisted(3));

        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");
        waitForStatusNotContaining("Hint: highlighted tiles can slide into the empty cell.");
    }

    @Test
    public void strategicHintPersistsAssistanceAndProtectsPlayerBest() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        AndroidGameStore store = new AndroidGameStore(targetContext);
        assertTrue(store.recordBestIfBetter(3, PuzzleDifficulty.CLASSIC, 2, 5_000L));
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        String startingBoard = getActivityContentDescription(R.id.game_board);

        clickId(R.id.game_assist_button);
        waitForText("Strategic Hint").click();
        waitForStatusContaining("Strategic hint: try tile 6.");
        waitForStatusContaining("Hint used. This run will not update player best.");
        waitForStatusContaining("0 moves");
        assertTrue(getActivityContentDescription(R.id.game_board).startsWith(startingBoard));
        assertTrue(store.isSavedGameAssisted(3));

        setLandscapeOrientation();
        waitForForegroundApp();
        instrumentation.waitForIdleSync();
        waitForResumedMainActivity();
        waitForId("game_root");
        waitForStatusContaining("Hint used. This run will not update player best.");

        clickId(R.id.game_menu_button);
        waitForText("Restart").click();
        waitForStatusContaining("0 moves");
        waitForStatusContaining("Hint used. This run will not update player best.");

        tapCell(3, 2, 2);
        waitForId("results_root");
        waitForText("Solved with assist.");
        assertEquals(2, new AndroidGameStore(targetContext)
                .getBest(3, PuzzleDifficulty.CLASSIC).moves);

        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        waitForStatusContaining("0 moves");
        waitForStatusContaining("Hint used. This run will not update player best.");
    }

    @Test
    public void assistMenuPausesElapsedPlayTime() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0, 5_000L, true, false);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_assist_button);
        waitForText("Show Movable Tiles");
        invokeActivityMethod("saveGame");
        long elapsedAtAssistMenu = savedElapsedForSize(3);
        SystemClock.sleep(2_200L);
        device.pressHome();
        device.waitForIdle();

        long savedElapsed = savedElapsedForSize(3);
        assertTrue("Assist-menu time should not be added to elapsed play time: " + savedElapsed,
                savedElapsed >= elapsedAtAssistMenu
                        && savedElapsed - elapsedAtAssistMenu < 1_200L);
    }

    @Test
    public void backgroundAndResumeExcludeAwayTime() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0, 5_000L, true, false);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        device.pressHome();
        device.waitForIdle();
        long elapsedBeforeAway = savedElapsedForSize(3);
        SystemClock.sleep(5_000L);
        long resumeRequestedAt = SystemClock.elapsedRealtime();
        device.executeShellCommand("am start -n " + PACKAGE_NAME + "/.MainActivity");
        waitForForegroundApp();
        SystemClock.sleep(300L);
        device.pressHome();
        device.waitForIdle();
        long foregroundDuration = SystemClock.elapsedRealtime() - resumeRequestedAt;

        long savedElapsed = savedElapsedForSize(3);
        long foregroundIncrease = savedElapsed - elapsedBeforeAway;
        assertTrue("Background time should not be added after resume; foreground increase was "
                        + foregroundIncrease + "ms for " + foregroundDuration
                        + "ms of foreground time",
                foregroundIncrease >= 0L
                        && foregroundIncrease < foregroundDuration + 500L);
    }

    @Test
    public void leavingGameForSettingsPausesElapsedPlayTime() throws Exception {
        writeSavedGameWithMetadata(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0, 5_000L, true, false);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        clickId(R.id.game_menu_button);
        waitForText("Save").click();
        device.waitForIdle();
        clickId(R.id.game_menu_button);
        scrollToText("Settings").click();
        waitForId("settings_root");
        invokeActivityMethod("saveGame");
        long elapsedAtSettings = savedElapsedForSize(3);
        SystemClock.sleep(2_200L);
        device.pressHome();
        device.waitForIdle();

        long savedElapsed = savedElapsedForSize(3);
        assertTrue("Settings time should not be added to elapsed play time: " + savedElapsed,
                savedElapsed >= elapsedAtSettings && savedElapsed - elapsedAtSettings < 1_200L);
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
        waitForText("First player record for this size and difficulty.");
        assertActivityTextContains(R.id.results_play_again_button, "Replay Puzzle");
        assertNotNull(findById("results_completion_mark"));
        assertActivityContentDescriptionContains(R.id.results_completion_mark, "Puzzle complete");
        assertActivityHasView(R.id.results_play_again_button);
        assertActivityHasView(R.id.results_new_size_button);
        assertActivityHasView(R.id.results_home_button);
        scrollToText("New Size");
        scrollToText("Home");

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");
        assertNotNull(findById("records_explanation_text"));
        waitForTextContaining("Player solves only.");
        assertActivityContainsText("1 move");
    }

    @Test
    public void trendsCompareOneScopeAndWeeklyGoalCanBeChanged() throws Exception {
        markOnboardingSeen();
        AndroidGameStore store = new AndroidGameStore(targetContext);
        store.setWeeklyGoalTarget(3);
        long oldCompletion = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L;
        int[] moves = {34, 32, 30, 22, 20, 18};
        for (int index = 0; index < moves.length; index++) {
            long completedAt = index < 4 ? oldCompletion + index : System.currentTimeMillis();
            store.recordCompletion(3, PuzzleDifficulty.CLASSIC,
                    moves[index], moves[index] * 1_000L, false, completedAt);
        }
        store.setTrendSize(3);
        store.setTrendDifficulty(PuzzleDifficulty.CLASSIC);

        launchApp();
        scrollToText("Trends & Weekly Goal").click();
        waitForId("trends_root");
        waitForText("2 / 3 player solves");
        scrollToText("3x3 · Classic");
        scrollToText("Moves · 20 recent / 32 previous · 38% better");

        scrollToText("Set Weekly Goal");
        clickId(R.id.trends_set_goal_button);
        UiObject2 input = device.wait(
                Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT_MS);
        assertNotNull(input);
        input.setText("7");
        UiObject2 confirm = device.wait(Until.findObject(By.text("OK")), TIMEOUT_MS);
        assertNotNull(confirm);
        confirm.click();
        waitForText("2 / 7 player solves");

        scrollToText("Choose Size & Difficulty");
        clickId(R.id.trends_choose_scope_button);
        scrollToText("4x4 · Challenge").click();
        scrollToText("4x4 · Challenge");
        scrollToText("No player solves in this scope yet.");
        scrollToText("Back");
        clickId(R.id.trends_back_button);
        waitForId("home_root");
    }

    @Test
    public void continuousChallengeResumesRecordsOnePuzzleAndStartsTheNext() throws Exception {
        markOnboardingSeen();
        AndroidGameStore store = new AndroidGameStore(targetContext);
        GameModel nearWin = createModel(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID,
                PuzzleDifficulty.CLASSIC, 0);
        ContinuousChallenge challenge = ContinuousChallenge.start(3)
                .completePuzzle(12, 2_000L, false);
        store.saveContinuousGame(nearWin, 3_000L, false, challenge);

        launchApp();
        assertActivityTextContains(R.id.home_continuous_summary_text, "puzzle 2 of 3");
        scrollToText("Continuous Challenge");
        clickId(R.id.home_continuous_button);
        waitForText("Resume puzzle 2 of 3").click();
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "Continuous 2/3");

        tapCell(3, 2, 2);

        waitForId("results_root");
        assertActivityTextContains(R.id.results_continuous_text, "Session 2/3");
        assertEquals(1, store.getCompletionHistory().length);
        scrollToText("Next Puzzle");
        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "Continuous 3/3");
        waitForStatusContaining("0 moves");
        clickId(R.id.game_home_button);
        waitForId("home_root");
        assertActivityTextContains(R.id.home_continuous_summary_text, "puzzle 3 of 3");
        assertNull(store.loadSavedGame(3));
    }

    @Test
    public void continuousChallengeCanStartFromHomeWithoutCreatingANormalSave()
            throws Exception {
        markOnboardingSeen();
        AndroidGameStore store = new AndroidGameStore(targetContext);

        launchApp();
        scrollToText("Continuous Challenge");
        clickId(R.id.home_continuous_button);
        waitForText("Start 3-puzzle session").click();
        waitForText("3x3 · Classic").click();

        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, "Continuous 1/3");
        AndroidGameStore.ContinuousGame saved = store.loadContinuousGame();
        assertNotNull(saved);
        assertEquals(3, saved.challenge.getTargetPuzzles());
        assertEquals(0, saved.challenge.getCompletedPuzzles());
        assertNull(store.loadSavedGame(3));

        setLandscapeOrientation();
        waitForForegroundApp();
        waitForResumedMainActivity();
        waitForActivityView(R.id.game_board);
        assertActivityTextContains(R.id.game_title_text, "Continuous 1/3");
    }

    @Test
    public void continuousChallengeCompletesAndCanBeEndedWithoutDeletingRecords()
            throws Exception {
        markOnboardingSeen();
        AndroidGameStore store = new AndroidGameStore(targetContext);
        GameModel nearWin = createModel(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID,
                PuzzleDifficulty.CLASSIC, 0);
        ContinuousChallenge challenge = ContinuousChallenge.start(3)
                .completePuzzle(12, 2_000L, false)
                .completePuzzle(14, 3_000L, true);
        store.saveContinuousGame(nearWin, 4_000L, false, challenge);

        launchApp();
        scrollToText("Continuous Challenge");
        clickId(R.id.home_continuous_button);
        waitForText("Resume puzzle 3 of 3").click();
        waitForId("game_root");
        tapCell(3, 2, 2);

        waitForId("results_root");
        waitForText("Continuous challenge complete.");
        assertActivityTextContains(R.id.results_continuous_text, "Session 3/3");
        assertActivityTextContains(R.id.results_play_again_button, "Repeat Session");
        scrollToText("End Session");
        clickId(R.id.results_new_size_button);
        waitForText("End continuous challenge?");
        waitForText("END SESSION").click();
        waitForId("home_root");
        assertNull(store.loadContinuousGame());
        assertEquals(1, store.getCompletionHistory().length);
    }

    @Test
    public void dailyChallengeRunsFromHomeThroughResultsAndUpdatesStreak() throws Exception {
        markOnboardingSeen();
        DailyChallenge challenge = DailyChallenge.forDate(LocalDate.now());
        GameModel original = challenge.createGame();
        SaveManager.SaveData nearWin = new SaveManager.SaveData();
        nearWin.size = 4;
        nearWin.grid = ONE_MOVE_WIN_GRID_4;
        nearWin.initialGrid = original.getInitialGridCopy();
        nearWin.difficulty = PuzzleDifficulty.CLASSIC;
        nearWin.active = true;
        GameModel daily = new GameModel(4);
        daily.loadState(nearWin);
        AndroidGameStore store = new AndroidGameStore(targetContext);
        store.saveDailyGame(challenge.getDateId(), daily, 2_000L);

        launchApp();
        waitForId("home_root");
        assertActivityTextContains(R.id.home_daily_summary_text, challenge.getDateId());
        clickId(R.id.home_daily_button);
        waitForId("daily_calendar_root");
        clickActivityContentDescriptionContaining(challenge.getDateId());
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, challenge.getDateId());

        tapCell(4, 3, 3);

        waitForId("results_root");
        waitForText("Daily challenge complete.");
        assertActivityTextContains(R.id.results_record_text, "Daily streak: 1 · Best: 1");
        AndroidGameStore.DailyProgress progress = store.getDailyProgress(challenge.getDateId());
        assertTrue(progress.completedToday);
        assertEquals(1, progress.currentStreak);

        clickId(R.id.results_home_button);
        waitForId("home_root");
        assertActivityTextContains(R.id.home_daily_summary_text, "Completed");
    }

    @Test
    public void dailyCalendarReplaysHistoricalPuzzleAndPreservesMonthAcrossRotation() throws Exception {
        markOnboardingSeen();
        LocalDate today = LocalDate.now();
        LocalDate historicalDate = today.minusMonths(1).withDayOfMonth(15);
        DailyChallenge todayChallenge = DailyChallenge.forDate(today);
        DailyChallenge historicalChallenge = DailyChallenge.forDate(historicalDate);
        AndroidGameStore store = new AndroidGameStore(targetContext);
        store.saveDailyGame(todayChallenge.getDateId(), todayChallenge.createGame(), 1_111L);
        store.saveDailyGame(historicalChallenge.getDateId(),
                historicalChallenge.createGame(), 2_222L);
        store.recordDailyCompletion(historicalChallenge.getDateId());
        String previousMonthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
                .format(YearMonth.from(historicalDate));

        launchApp();
        clickId(R.id.home_daily_button);
        waitForId("daily_calendar_root");
        clickId(R.id.daily_calendar_previous_button);
        waitForActivityTextContaining(R.id.daily_calendar_month_text, previousMonthLabel);
        waitForActivityContentDescriptionContaining(historicalChallenge.getDateId());

        setLandscapeOrientation();
        waitForForegroundApp();
        instrumentation.waitForIdleSync();
        waitForResumedMainActivity();
        waitForId("daily_calendar_root");
        assertActivityTextContains(R.id.daily_calendar_month_text, previousMonthLabel);

        clickActivityContentDescriptionContaining(historicalChallenge.getDateId());
        waitForId("game_root");
        assertActivityTextContains(R.id.game_title_text, historicalChallenge.getDateId());
        assertEquals(1_111L, store.loadDailyGame(todayChallenge.getDateId()).elapsedTime);
        assertEquals(2_222L, store.loadDailyGame(historicalChallenge.getDateId()).elapsedTime);
    }

    @Test
    public void playerAndAssistedWinsAppearInStatsAndRecentHistory() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 2, 2);
        waitForId("results_root");
        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        setActivityField("assistedSolveActive", true);
        tapCell(3, 2, 2);
        waitForId("results_root");

        clickId(R.id.results_home_button);
        waitForId("home_root");
        clickId(R.id.home_records_button);
        waitForId("records_root");

        waitForText("1 player solve");
        waitForText("1 assisted solve");
        scrollToText("RECENT COMPLETIONS");

        AndroidGameStore store = new AndroidGameStore(targetContext);
        AndroidGameStore.CompletionRecord[] history = store.getCompletionHistory();
        assertEquals(2, history.length);
        assertActivityContainsText("Player avg: 1 move · "
                + Math.round(history[1].timeMs / 1000.0) + "s");
        assertActivityContainsText("Assisted · 1 move · " + history[0].timeMs / 1000 + "s");
        assertActivityContainsText("Player · 1 move · " + history[1].timeMs / 1000 + "s");
        assertEquals(1, store.getOverallCompletionStats().playerCompletions);
        assertEquals(1, store.getOverallCompletionStats().assistedCompletions);
        assertEquals(1, store.getBest(3, PuzzleDifficulty.CLASSIC).moves);
    }

    @Test
    public void personalStatsAndRecentHistoryAreLocalizedInChineseAndJapanese() throws Exception {
        markOnboardingSeen();
        AndroidGameStore store = new AndroidGameStore(targetContext);
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 10, 1_000L, false, 100L);
        store.recordCompletion(4, PuzzleDifficulty.CHALLENGE, 20, 2_000L, true, 200L);
        setLanguage(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        launchApp();

        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("個人總計");
        waitForText("1 次玩家完成");
        waitForText("1 次輔助完成");
        waitForText("玩家平均：10 次移動 · 1 秒");
        scrollToText("近期完成");
        assertActivityContainsText("輔助 · 20 次移動 · 2 秒");
        assertActivityContainsText("玩家 · 10 次移動 · 1 秒");

        setLanguage(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        relaunchApp();
        clickId(R.id.home_records_button);
        waitForId("records_root");
        waitForText("個人合計");
        waitForText("1回の自力クリア");
        waitForText("1回のアシストクリア");
        assertActivityContainsText("自力平均：10手・1秒");
        scrollToText("最近のクリア");
        assertActivityContainsText("アシスト・20手・2秒");
        assertActivityContainsText("自力・10手・1秒");
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
        assertActivityContainsText("No record yet");
    }

    @Test
    public void resultsActionsNavigateToReplayAndModeSelect() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        String startingBoard = getActivityContentDescription(R.id.game_board);
        tapCell(3, 2, 2);
        waitForId("results_root");
        assertActivityTextContains(R.id.results_play_again_button, "Replay Puzzle");

        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        waitForText("3x3 · Classic");
        assertEquals(startingBoard, getActivityContentDescription(R.id.game_board));
        waitForStatusContaining("0 moves");

        invokeActivityMethod("onGameWon", new Class<?>[] {int.class, long.class}, 1, 0L);
        waitForId("results_root");
        clickId(R.id.results_new_size_button);
        waitForId("mode_root");
    }

    @Test
    public void resultReplayRestoresSamePuzzleAfterRotation() throws Exception {
        writeSavedGame(ONE_MOVE_WIN_GRID, ONE_MOVE_WIN_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        String startingBoard = getActivityContentDescription(R.id.game_board);
        tapCell(3, 2, 2);
        waitForId("results_root");
        assertEquals(1, new AndroidGameStore(targetContext).getCompletionHistory().length);

        setLandscapeOrientation();
        waitForForegroundApp();
        instrumentation.waitForIdleSync();
        waitForResumedMainActivity();
        waitForActivityView(R.id.results_root);
        assertEquals(1, new AndroidGameStore(targetContext).getCompletionHistory().length);

        clickId(R.id.results_play_again_button);
        waitForId("game_root");
        assertEquals(startingBoard, getActivityContentDescription(R.id.game_board));
        waitForStatusContaining("0 moves");
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
    public void moveHistoryAndRedoPreserveWholeLineAsOneAction() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        launchApp();
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        tapCell(3, 1, 2);
        waitForStatusContaining("1 move");
        clickId(R.id.game_menu_button);
        waitForText("Move History").click();
        waitForTextContaining("1 completed actions · 0 available to redo");
        waitForTextContaining("1. Empty right × 2 (one move)");
        waitForText("CLOSE").click();

        clickId(R.id.game_undo_button);
        waitForStatusContaining("0 moves");
        clickId(R.id.game_redo_button);
        waitForStatusContaining("1 move");
    }

    @Test
    public void gameScreenDeclaresHeadingTouchTargetsAndTraversalOrder() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        markOnboardingSeen();
        launchApp();

        assertAccessibilityHeading(R.id.screen_header_title);
        clickId(R.id.home_continue_button);
        waitForId("game_root");

        assertTraversalAfter(R.id.game_menu_button, R.id.game_home_button);
        assertTraversalAfter(R.id.game_status_text, R.id.game_menu_button);
        assertTraversalAfter(R.id.game_board, R.id.game_status_text);
        assertTraversalAfter(R.id.game_undo_button, R.id.game_board);
        assertTraversalAfter(R.id.game_redo_button, R.id.game_undo_button);
        assertTraversalAfter(R.id.game_restart_button, R.id.game_redo_button);
        assertTraversalAfter(R.id.game_assist_button, R.id.game_restart_button);
        assertMinimumTouchTarget(R.id.game_home_button, 48);
        assertMinimumTouchTarget(R.id.game_menu_button, 48);
        assertMinimumTouchTarget(R.id.game_undo_button, 48);
        assertMinimumTouchTarget(R.id.game_redo_button, 48);
        assertMinimumTouchTarget(R.id.game_restart_button, 48);
        assertMinimumTouchTarget(R.id.game_assist_button, 48);
    }

    @Test
    public void boardAccessibilityNodesExposeAndActivateWholeLineMoves() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        markOnboardingSeen();
        launchApp();
        clickId(R.id.home_continue_button);
        waitForBoardReady(R.id.game_board, 3);

        instrumentation.runOnMainSync(() -> {
            KlotskiView board = activity.findViewById(R.id.game_board);
            AccessibilityNodeProvider provider = board.getAccessibilityNodeProvider();
            assertNotNull(provider);
            AccessibilityNodeInfo host = provider.createAccessibilityNodeInfo(
                    AccessibilityNodeProvider.HOST_VIEW_ID);
            assertNotNull(host);
            assertEquals(9, host.getChildCount());

            int tileId = KlotskiView.virtualIdForCell(1, 2, 3);
            AccessibilityNodeInfo tile = provider.createAccessibilityNodeInfo(tileId);
            assertNotNull(tile);
            assertTrue(tile.isClickable());
            assertTrue(tile.getContentDescription().toString().contains("Tile 5"));
            assertTrue(provider.performAction(tileId,
                    AccessibilityNodeInfo.ACTION_CLICK, null));
        });

        waitForBoardIdle(R.id.game_board);
        waitForStatusContaining("1 move");
    }

    @Test
    public void largeFontScaleStacksDenseRowsWithoutEllipsizingGameActions() throws Exception {
        writeSavedGame(LINE_SLIDE_GRID, LINE_SLIDE_GRID, 0);
        markOnboardingSeen();
        launchApp();

        float fontScale = activity.getResources().getConfiguration().fontScale;
        if (fontScale < 1.49f) {
            assertFalse(new AndroidUiPolicy(
                    activity.getResources().getConfiguration()).isLargeText());
            return;
        }

        assertParentOrientation(R.id.home_onboarding_button, LinearLayout.VERTICAL);
        assertParentOrientation(R.id.home_settings_button, LinearLayout.VERTICAL);
        assertNoEllipsis(R.id.home_onboarding_button);
        assertNoEllipsis(R.id.home_tutorial_button);
        clickId(R.id.home_continue_button);
        waitForId("game_root");
        assertNoEllipsis(R.id.game_home_button);
        assertNoEllipsis(R.id.game_menu_button);
        assertNoEllipsis(R.id.game_undo_button);
        assertNoEllipsis(R.id.game_redo_button);
        assertNoEllipsis(R.id.game_restart_button);
        assertNoEllipsis(R.id.game_assist_button);
        waitForBoardReady(R.id.game_board, 3);
    }

    @Test
    public void wideWindowCentersAndBoundsScrollableScreenContent() throws Exception {
        markOnboardingSeen();
        launchApp();

        instrumentation.runOnMainSync(() -> {
            ViewGroup root = activity.findViewById(R.id.home_root);
            assertNotNull(root);
            assertTrue(root.getChildCount() > 0);
            View content = root.getChildAt(0);
            AndroidUiPolicy policy = new AndroidUiPolicy(
                    activity.getResources().getConfiguration());
            if (!policy.hasBoundedContentWidth()) {
                assertEquals(root.getWidth(), content.getWidth());
                return;
            }
            int maximumPx = Math.round(policy.getContentMaxWidthDp()
                    * activity.getResources().getDisplayMetrics().density);
            assertTrue("Wide-screen content exceeded its adaptive bound",
                    content.getWidth() <= maximumPx);
            int leftSpace = content.getLeft();
            int rightSpace = root.getWidth() - content.getRight();
            assertTrue("Wide-screen content was not centered",
                    Math.abs(leftSpace - rightSpace) <= 2);
        });
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

        setLandscapeOrientation();
        waitForGameBoardAfterRotation();

        setPortraitOrientation();
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
                setPortraitOrientation();
                waitForResumedMainActivityInOrientation(Configuration.ORIENTATION_PORTRAIT);
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

    private void waitForPortraitOrientation() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (device.getDisplayHeight() > device.getDisplayWidth()) {
                device.waitForIdle();
                return;
            }
            Thread.sleep(100);
        }
        fail("Emulator did not return to natural portrait orientation");
    }

    private void setPortraitOrientation() throws Exception {
        device.executeShellCommand("wm user-rotation lock 0");
        waitForPortraitOrientation();
    }

    private void setLandscapeOrientation() throws Exception {
        device.executeShellCommand("wm user-rotation lock 1");
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (device.getDisplayWidth() > device.getDisplayHeight()) {
                device.waitForIdle();
                return;
            }
            Thread.sleep(100);
        }
        fail("Emulator did not enter landscape orientation");
    }

    private void clearAppPreferences() {
        targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        device.waitForIdle();
    }

    private long savedElapsedForSize(int size) {
        SaveManager.SaveData data = new AndroidGameStore(targetContext).loadSavedGame(size);
        assertNotNull("Missing saved game for size: " + size, data);
        return data.elapsedTime;
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

    private void writeIndependentSizeSlots() {
        SharedPreferences.Editor editor = targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.clear();
        writeSlot(editor, 3, "1,2,3,4,5,0,7,8,6", 3, 3_000L, "classic");
        writeSlot(editor, 4, "1,2,3,4,5,6,7,8,9,10,11,12,13,14,0,15",
                4, 4_000L, "challenge");
        editor.putInt("last_size", 4);
        editor.putBoolean("onboarding_seen", true);
        assertTrue(editor.commit());
    }

    private GameModel createModel(String gridText, String initialGridText,
            PuzzleDifficulty difficulty, int moves) {
        String[] values = gridText.split(",");
        int size = (int) Math.sqrt(values.length);
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = parseGrid(gridText, size);
        data.initialGrid = parseGrid(initialGridText, size);
        data.moveCount = moves;
        data.difficulty = difficulty;
        data.active = true;
        GameModel created = new GameModel(size);
        created.loadState(data);
        return created;
    }

    private int[][] parseGrid(String encoded, int size) {
        String[] values = encoded.split(",");
        int[][] grid = new int[size][size];
        for (int index = 0; index < values.length; index++) {
            grid[index / size][index % size] = Integer.parseInt(values[index]);
        }
        return grid;
    }

    private void writeSlot(SharedPreferences.Editor editor, int size, String grid,
            int moves, long elapsedMs, String difficulty) {
        String prefix = "save_" + size + "_";
        editor.putInt(prefix + "size", size);
        editor.putString(prefix + "grid", grid);
        editor.putString(prefix + "initial_grid", grid);
        editor.putInt(prefix + "moves", moves);
        editor.putLong(prefix + "elapsed", elapsedMs);
        editor.putLong(prefix + "updated_at", System.currentTimeMillis());
        editor.putBoolean(prefix + "active", true);
        editor.putBoolean(prefix + "solved", false);
        editor.putString(prefix + "difficulty", difficulty);
    }

    private void markOnboardingSeen() {
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("onboarding_seen", true)
                .commit());
    }

    private void setLanguage(String languageTag) {
        assertTrue(targetContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(AndroidGameStore.KEY_LANGUAGE_TAG, languageTag)
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

    private void assertActivityBackgroundColor(int resourceId, int expectedColor) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity view id: " + resourceId, view);
            assertTrue("View background is not a solid color: " + resourceId,
                    view.getBackground() instanceof ColorDrawable);
            assertEquals("Unexpected background color for activity view id: " + resourceId,
                    expectedColor, ((ColorDrawable) view.getBackground()).getColor());
        });
    }

    private void assertActivityContainsText(String expectedText) {
        instrumentation.runOnMainSync(() -> {
            View content = activity.findViewById(android.R.id.content);
            assertTrue("Missing activity text containing: " + expectedText,
                    viewTreeContainsText(content, expectedText));
        });
    }

    private boolean viewTreeContainsText(View view, String expectedText) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().contains(expectedText)) {
                return true;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (viewTreeContainsText(group.getChildAt(index), expectedText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void assertActivityMissingView(int resourceId) {
        instrumentation.runOnMainSync(() ->
                assertNull("Unexpected activity view id: " + resourceId, activity.findViewById(resourceId)));
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

    private void assertAccessibilityHeading(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing accessibility heading: " + resourceId, view);
            assertTrue("View should be exposed as an accessibility heading: " + resourceId,
                    view.isAccessibilityHeading());
        });
    }

    private void assertTraversalAfter(int resourceId, int expectedPreviousId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing traversal view: " + resourceId, view);
            assertEquals("Unexpected traversal predecessor for: " + resourceId,
                    expectedPreviousId, view.getAccessibilityTraversalAfter());
        });
    }

    private void assertMinimumTouchTarget(int resourceId, int minimumDp) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing touch target: " + resourceId, view);
            int minimumPx = Math.round(minimumDp
                    * activity.getResources().getDisplayMetrics().density);
            assertTrue("Touch target width below " + minimumDp + "dp: " + resourceId
                            + " was " + view.getWidth() + "px",
                    view.getWidth() >= minimumPx);
            assertTrue("Touch target height below " + minimumDp + "dp: " + resourceId
                            + " was " + view.getHeight() + "px",
                    view.getHeight() >= minimumPx);
        });
    }

    private void assertParentOrientation(int resourceId, int expectedOrientation) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing adaptive child: " + resourceId, view);
            assertTrue("Adaptive parent should be a LinearLayout: " + resourceId,
                    view.getParent() instanceof LinearLayout);
            assertEquals("Unexpected adaptive row orientation for: " + resourceId,
                    expectedOrientation, ((LinearLayout) view.getParent()).getOrientation());
        });
    }

    private void assertNoEllipsis(int resourceId) {
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing text view: " + resourceId, view);
            assertTrue("Expected TextView for ellipsis check: " + resourceId,
                    view instanceof TextView);
            TextView text = (TextView) view;
            Layout layout = text.getLayout();
            assertNotNull("Text has not been laid out: " + resourceId, layout);
            for (int line = 0; line < layout.getLineCount(); line++) {
                assertEquals("Ellipsized line for: " + resourceId,
                        0, layout.getEllipsisCount(line));
            }
            assertEquals("Text was clipped after max lines for: " + resourceId,
                    text.getText().length(), layout.getLineEnd(layout.getLineCount() - 1));
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
        assertActivityTextContains(R.id.game_title_text, "3x3 · Classic");
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

    private void waitForResumedMainActivityInOrientation(int orientation)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Activity[] resumedActivity = new Activity[1];
            instrumentation.runOnMainSync(() -> {
                Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for (Activity candidate : resumedActivities) {
                    if (candidate instanceof MainActivity
                            && candidate.getResources().getConfiguration().orientation
                            == orientation) {
                        resumedActivity[0] = candidate;
                        return;
                    }
                }
            });
            if (resumedActivity[0] != null) {
                activity = resumedActivity[0];
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(100);
        }
        fail("MainActivity did not resume in orientation: " + orientation);
    }

    private void waitForRecreatedMainActivity(Activity previousActivity) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Activity[] resumedActivity = new Activity[1];
            instrumentation.runOnMainSync(() -> {
                Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                for (Activity candidate : resumedActivities) {
                    if (candidate instanceof MainActivity && candidate != previousActivity) {
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
        fail("MainActivity was not recreated after restoring personal data");
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

    private void waitForActivityTextContaining(int resourceId, String expectedText)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            boolean[] found = new boolean[1];
            instrumentation.runOnMainSync(() -> {
                View view = activity.findViewById(resourceId);
                found[0] = view instanceof TextView
                        && ((TextView) view).getText().toString().contains(expectedText);
            });
            if (found[0]) {
                return;
            }
            Thread.sleep(50);
        }
        assertActivityTextContains(resourceId, expectedText);
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

    private String getActivityContentDescription(int resourceId) {
        String[] result = new String[1];
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing activity view id: " + resourceId, view);
            CharSequence description = view.getContentDescription();
            assertNotNull("Missing content description for activity view id: " + resourceId, description);
            result[0] = description.toString();
        });
        return result[0];
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

    private void waitForActivityContentDescriptionContaining(String text) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            View[] match = new View[1];
            instrumentation.runOnMainSync(() -> match[0] = findViewWithContentDescription(
                    activity.findViewById(android.R.id.content), text));
            if (match[0] != null && match[0].isShown()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Missing activity content description containing: " + text);
    }

    private void clickActivityContentDescriptionContaining(String text) throws InterruptedException {
        waitForActivityContentDescriptionContaining(text);
        instrumentation.runOnMainSync(() -> {
            View match = findViewWithContentDescription(
                    activity.findViewById(android.R.id.content), text);
            assertNotNull("Missing activity content description containing: " + text, match);
            assertTrue("Content-description click was not handled: " + text, match.performClick());
        });
        instrumentation.waitForIdleSync();
    }

    private View findViewWithContentDescription(View root, String text) {
        if (root == null) {
            return null;
        }
        CharSequence description = root.getContentDescription();
        if (description != null && description.toString().contains(text)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                View match = findViewWithContentDescription(group.getChildAt(index), text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
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

    private void clickId(int resourceId) throws InterruptedException {
        waitForActivityClickReady(resourceId);
        instrumentation.runOnMainSync(() -> {
            View view = activity.findViewById(resourceId);
            assertNotNull("Missing view id: " + resourceId, view);
            assertTrue("View click was not handled: " + resourceId, view.performClick());
        });
        instrumentation.waitForIdleSync();
    }

    private void waitForActivityClickReady(int resourceId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            boolean[] ready = new boolean[1];
            instrumentation.runOnMainSync(() -> {
                View view = activity.findViewById(resourceId);
                ready[0] = view != null
                        && view.isShown()
                        && view.isEnabled()
                        && activity.hasWindowFocus();
            });
            if (ready[0]) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(50);
        }
        fail("View did not become ready for click: " + resourceId);
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
        waitForBoardReady(boardResourceId, size);
        int[] screenPoint = new int[2];
        instrumentation.runOnMainSync(() -> {
            View board = activity.findViewById(boardResourceId);
            assertNotNull("Missing board view id: " + boardResourceId, board);
            float density = targetContext.getResources().getDisplayMetrics().density;
            float gap = 9f * density;
            float boardSize = Math.min(board.getWidth(), board.getHeight());
            float tileSize = (boardSize - (size + 1) * gap) / size;
            float boardLeft = (board.getWidth() - boardSize) / 2f;
            float boardTop = (board.getHeight() - boardSize) / 2f;
            float x = boardLeft + gap + col * (tileSize + gap) + tileSize / 2f;
            float y = boardTop + gap + row * (tileSize + gap) + tileSize / 2f;
            assertFalse("Computed x is outside board bounds", x < 0 || x > board.getWidth());
            assertFalse("Computed y is outside board bounds", y < 0 || y > board.getHeight());
            int[] boardLocation = new int[2];
            board.getLocationOnScreen(boardLocation);
            screenPoint[0] = Math.round(boardLocation[0] + x);
            screenPoint[1] = Math.round(boardLocation[1] + y);
        });
        assertTrue("Device click was not handled for board cell",
                device.click(screenPoint[0], screenPoint[1]));
        device.waitForIdle();
        waitForBoardIdle(boardResourceId);
    }

    private void waitForBoardReady(int boardResourceId, int size) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        String[] boardState = {"missing"};
        while (System.currentTimeMillis() < deadline) {
            boolean[] ready = new boolean[1];
            instrumentation.runOnMainSync(() -> {
                View board = activity.findViewById(boardResourceId);
                float density = targetContext.getResources().getDisplayMetrics().density;
                float minimumBoardSize = ((size + 1) * 9f + size) * density;
                if (board != null) {
                    boardState[0] = board.getWidth() + "x" + board.getHeight()
                            + ", minimum=" + minimumBoardSize
                            + ", display=" + activity.getResources().getDisplayMetrics().widthPixels
                            + "x" + activity.getResources().getDisplayMetrics().heightPixels
                            + ", orientation="
                            + activity.getResources().getConfiguration().orientation
                            + ", shown=" + board.isShown()
                            + ", enabled=" + board.isEnabled()
                            + ", focus=" + activity.hasWindowFocus()
                            + ", busy=" + (board instanceof KlotskiView
                                    && ((KlotskiView) board).isBusy());
                }
                ready[0] = board instanceof KlotskiView
                        && board.isShown()
                        && board.isEnabled()
                        && board.getWidth() >= minimumBoardSize
                        && board.getHeight() >= minimumBoardSize
                        && activity.hasWindowFocus()
                        && !((KlotskiView) board).isBusy();
            });
            if (ready[0]) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(50);
        }
        fail("Board did not become ready for touch: " + boardResourceId
                + " (" + boardState[0] + ")");
    }

    private void waitForBoardIdle(int boardResourceId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            boolean[] idle = new boolean[1];
            instrumentation.runOnMainSync(() -> {
                View board = activity.findViewById(boardResourceId);
                idle[0] = board == null
                        || board instanceof KlotskiView && !((KlotskiView) board).isBusy();
            });
            if (idle[0]) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(50);
        }
        fail("Board did not become idle after tapping: " + boardResourceId);
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
