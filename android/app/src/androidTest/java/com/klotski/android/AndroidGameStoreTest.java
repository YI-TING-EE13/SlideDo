package com.klotski.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Locale;

/**
 * Focused instrumentation coverage for Android app-state persistence.
 */
@RunWith(AndroidJUnit4.class)
public class AndroidGameStoreTest {
    private Context targetContext;
    private SharedPreferences prefs;
    private AndroidGameStore store;

    @Before
    public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = targetContext.getSharedPreferences(AndroidGameStore.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        store = new AndroidGameStore(targetContext);
    }

    @After
    public void tearDown() {
        prefs.edit().clear().commit();
    }

    @Test
    public void saveRoundTripPreservesInitialGridForRestart() {
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = 3;
        data.initialGrid = new int[][] {
                {1, 2, 3},
                {0, 4, 5},
                {7, 8, 6}
        };
        data.grid = new int[][] {
                {1, 2, 3},
                {4, 0, 5},
                {7, 8, 6}
        };
        data.moveCount = 1;
        data.elapsedTime = 1234L;
        data.difficulty = PuzzleDifficulty.CHALLENGE;

        GameModel model = new GameModel(3);
        model.loadState(data);
        store.saveGame(model, 5678L);

        SaveManager.SaveData loaded = store.loadSavedGame();
        assertNotNull(loaded);
        assertEquals(3, loaded.size);
        assertEquals(1, loaded.moveCount);
        assertEquals(5678L, loaded.elapsedTime);
        assertTrue(loaded.updatedAt > 0);
        assertTrue(loaded.active);
        assertFalse(loaded.solved);
        assertEquals(PuzzleDifficulty.CHALLENGE, loaded.difficulty);
        assertTrue(Arrays.deepEquals(data.grid, loaded.grid));
        assertTrue(Arrays.deepEquals(data.initialGrid, loaded.initialGrid));

        AndroidGameStore.SaveMetadata metadata = store.getSaveMetadata();
        assertNotNull(metadata);
        assertEquals(loaded.updatedAt, metadata.updatedAt);
        assertEquals(3, metadata.size);
        assertEquals(1, metadata.moves);
        assertEquals(5678L, metadata.elapsedMs);
        assertTrue(metadata.active);
        assertFalse(metadata.solved);
        assertEquals(PuzzleDifficulty.CHALLENGE, metadata.difficulty);

        GameModel restored = new GameModel(3);
        restored.loadState(loaded);
        restored.restartCurrentGame();
        assertTrue(Arrays.deepEquals(data.initialGrid, restored.getGridCopy()));
    }

    @Test
    public void savesForDifferentSizesUseIndependentSlots() {
        store.saveGame(createSavedModel(3, PuzzleDifficulty.RELAXED, 3), 3_000L);
        store.saveGame(createSavedModel(4, PuzzleDifficulty.CHALLENGE, 4), 4_000L);
        store.saveGame(createSavedModel(5, PuzzleDifficulty.CLASSIC, 5), 5_000L);

        assertTrue(prefs.contains("save_3_grid"));
        assertTrue(prefs.contains("save_4_grid"));
        assertTrue(prefs.contains("save_5_grid"));
        assertEquals("relaxed", prefs.getString("save_3_difficulty", null));
        assertEquals("challenge", prefs.getString("save_4_difficulty", null));
        assertEquals(3_000L, prefs.getLong("save_3_elapsed", -1L));
        assertEquals(4_000L, prefs.getLong("save_4_elapsed", -1L));
        assertEquals(5_000L, prefs.getLong("save_5_elapsed", -1L));

        SaveManager.SaveData three = store.loadSavedGame(3);
        SaveManager.SaveData four = store.loadSavedGame(4);
        SaveManager.SaveData five = store.loadSavedGame(5);
        assertNotNull(three);
        assertNotNull(four);
        assertNotNull(five);
        assertEquals(3, three.moveCount);
        assertEquals(4, four.moveCount);
        assertEquals(PuzzleDifficulty.RELAXED, three.difficulty);
        assertEquals(PuzzleDifficulty.CHALLENGE, four.difficulty);
        assertEquals(PuzzleDifficulty.CLASSIC, five.difficulty);
        assertEquals(5, store.loadSavedGame().size);

        AndroidGameStore.SaveMetadata[] saves = store.getAllSaveMetadata();
        assertEquals(3, saves.length);
        assertEquals(3, saves[0].size);
        assertEquals(4, saves[1].size);
        assertEquals(5, saves[2].size);
    }

    @Test
    public void legacySingleSaveMigratesIntoItsSizeSlot() {
        prefs.edit()
                .putInt("size", 3)
                .putString("grid", "1,2,3,4,5,0,7,8,6")
                .putString("initial_grid", "1,2,3,4,5,0,7,8,6")
                .putInt("moves", 7)
                .putLong("elapsed", 7_000L)
                .putLong("updated_at", 77L)
                .putBoolean("active", true)
                .putBoolean("solved", false)
                .putString("difficulty", "challenge")
                .commit();

        SaveManager.SaveData migrated = store.loadSavedGame();

        assertNotNull(migrated);
        assertEquals(3, migrated.size);
        assertEquals(7, migrated.moveCount);
        assertEquals(PuzzleDifficulty.CHALLENGE, migrated.difficulty);
        assertTrue(prefs.contains("save_3_grid"));
        assertFalse(prefs.contains("grid"));
    }

    @Test
    public void olderLegacySaveDoesNotOverwriteNewerSizeSlot() {
        prefs.edit()
                .putInt("save_3_size", 3)
                .putString("save_3_grid", "1,2,3,4,5,0,7,8,6")
                .putString("save_3_initial_grid", "1,2,3,4,5,0,7,8,6")
                .putInt("save_3_moves", 2)
                .putLong("save_3_elapsed", 2_000L)
                .putLong("save_3_updated_at", 200L)
                .putString("save_3_difficulty", "classic")
                .putInt("size", 3)
                .putString("grid", "1,2,3,4,5,0,7,8,6")
                .putString("initial_grid", "1,2,3,4,5,0,7,8,6")
                .putInt("moves", 9)
                .putLong("elapsed", 9_000L)
                .putLong("updated_at", 100L)
                .putString("difficulty", "challenge")
                .commit();

        SaveManager.SaveData loaded = store.loadSavedGame(3);

        assertNotNull(loaded);
        assertEquals(2, loaded.moveCount);
        assertEquals(2_000L, loaded.elapsedTime);
        assertEquals(PuzzleDifficulty.CLASSIC, loaded.difficulty);
        assertFalse(prefs.contains("grid"));
    }

    @Test
    public void clearSavedGameRemovesEverySizeSlot() {
        prefs.edit()
                .putString("save_3_grid", "slot-three")
                .putString("save_4_grid", "slot-four")
                .putString("save_5_grid", "slot-five")
                .putString("grid", "legacy")
                .commit();

        store.clearSavedGame();

        assertFalse(prefs.contains("save_3_grid"));
        assertFalse(prefs.contains("save_4_grid"));
        assertFalse(prefs.contains("save_5_grid"));
        assertFalse(prefs.contains("grid"));
    }

    @Test
    public void difficultyPreferenceAndScopedBestRecordsRemainIndependent() {
        assertEquals(PuzzleDifficulty.CLASSIC, store.getLastDifficulty());
        store.setLastDifficulty(PuzzleDifficulty.CHALLENGE);
        assertEquals(PuzzleDifficulty.CHALLENGE, store.getLastDifficulty());

        assertTrue(store.recordBestIfBetter(4, PuzzleDifficulty.RELAXED, 10, 5_000L));
        assertTrue(store.recordBestIfBetter(4, PuzzleDifficulty.CHALLENGE, 20, 9_000L));
        assertEquals(10, store.getBest(4, PuzzleDifficulty.RELAXED).moves);
        assertEquals(20, store.getBest(4, PuzzleDifficulty.CHALLENGE).moves);
        assertNull(store.getBest(4, PuzzleDifficulty.CLASSIC));
    }

    @Test
    public void invalidSaveIsIgnored() {
        prefs.edit()
                .putInt("size", 9)
                .putString("grid", "1,2,3")
                .commit();

        assertFalse(store.hasSavedGame());
        assertNull(store.loadSavedGame());
    }

    @Test
    public void bestRecordsPreferFewerMovesThenLowerTime() {
        assertTrue(store.recordBestIfBetter(3, 20, 10000L));
        assertFalse(store.recordBestIfBetter(3, 21, 1000L));
        assertFalse(store.recordBestIfBetter(3, 20, 11000L));
        assertTrue(store.recordBestIfBetter(3, 20, 9000L));

        AndroidGameStore.Best best = store.getBest(3);
        assertNotNull(best);
        assertEquals(20, best.moves);
        assertEquals(9000L, best.timeMs);

        store.clearRecords();
        assertNull(store.getBest(3));
    }

    @Test
    public void settingsOnboardingAndLastSizeUseStableDefaults() {
        assertFalse(store.isOnboardingSeen());
        store.markOnboardingSeen();
        assertTrue(store.isOnboardingSeen());

        assertTrue(store.isHapticEnabled());
        store.setHapticEnabled(false);
        assertFalse(store.isHapticEnabled());

        assertFalse(store.isReducedMotionEnabled());
        store.setReducedMotionEnabled(true);
        assertTrue(store.isReducedMotionEnabled());

        assertEquals(4, store.getLastSize(4));
        store.setLastSize(5);
        assertEquals(5, store.getLastSize(4));
        store.setLastSize(9);
        assertEquals(5, store.getLastSize(4));

        assertEquals(AndroidAppLocale.DEFAULT_LANGUAGE_TAG, store.getLanguageTag());
        store.setLanguageTag(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        assertEquals(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG, store.getLanguageTag());
        store.setLanguageTag(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        assertEquals(AndroidAppLocale.JAPANESE_LANGUAGE_TAG, store.getLanguageTag());
    }

    @Test
    public void unsupportedStoredLanguageFallsBackToEnglish() {
        prefs.edit().putString("language_tag", "fr-FR").commit();

        assertEquals(AndroidAppLocale.DEFAULT_LANGUAGE_TAG, store.getLanguageTag());
    }

    @Test
    public void appLanguageOverridesDeviceContextLanguage() {
        Configuration deviceConfiguration = new Configuration(targetContext.getResources().getConfiguration());
        deviceConfiguration.setLocales(new LocaleList(Locale.forLanguageTag("zh-TW")));
        Context chineseDeviceContext = targetContext.createConfigurationContext(deviceConfiguration);

        Context defaultContext = AndroidAppLocale.wrap(chineseDeviceContext, store.getLanguageTag());
        assertEquals("Settings", defaultContext.getString(R.string.settings_title));

        Context traditionalChineseContext = AndroidAppLocale.wrap(
                chineseDeviceContext, AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        assertEquals("設定", traditionalChineseContext.getString(R.string.settings_title));

        Context japaneseContext = AndroidAppLocale.wrap(
                chineseDeviceContext, AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        assertEquals("設定", japaneseContext.getString(R.string.settings_title));
    }

    private GameModel createSavedModel(int size, PuzzleDifficulty difficulty, int moves) {
        int[][] grid = new int[size][size];
        int value = 1;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                grid[row][col] = value++;
            }
        }
        grid[size - 1][size - 2] = 0;
        grid[size - 1][size - 1] = size * size - 1;

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = grid;
        data.moveCount = moves;
        data.active = true;
        data.difficulty = difficulty;
        GameModel model = new GameModel(size);
        model.loadState(data);
        return model;
    }
}
