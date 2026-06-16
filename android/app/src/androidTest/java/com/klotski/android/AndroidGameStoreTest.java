package com.klotski.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.klotski.core.GameModel;
import com.klotski.core.SaveManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

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

        GameModel restored = new GameModel(3);
        restored.loadState(loaded);
        restored.restartCurrentGame();
        assertTrue(Arrays.deepEquals(data.initialGrid, restored.getGridCopy()));
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
    }
}
