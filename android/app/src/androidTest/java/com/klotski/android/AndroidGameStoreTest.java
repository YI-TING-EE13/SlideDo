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

import com.klotski.core.DailyChallenge;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.PersonalTrend;
import com.klotski.core.SaveManager;
import com.klotski.core.WeeklyGoalProgress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Locale;
import java.time.LocalDate;
import java.time.ZoneId;

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
        if (prefs != null) {
            prefs.edit().clear().commit();
        }
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
    public void dailySaveRemainsIndependentFromRegularSizeSlots() {
        GameModel regular = createSavedModel(4, PuzzleDifficulty.CHALLENGE, 7);
        store.saveGame(regular, 7_000L);
        DailyChallenge challenge = DailyChallenge.fromDateId("2026-08-21");
        GameModel daily = challenge.createGame();

        store.saveDailyGame(challenge.getDateId(), daily, 1_234L);

        SaveManager.SaveData regularLoaded = store.loadSavedGame(4);
        SaveManager.SaveData dailyLoaded = store.loadDailyGame("2026-08-21");
        assertNotNull(regularLoaded);
        assertNotNull(dailyLoaded);
        assertEquals(7, regularLoaded.moveCount);
        assertEquals(PuzzleDifficulty.CHALLENGE, regularLoaded.difficulty);
        assertEquals(0, dailyLoaded.moveCount);
        assertEquals(1_234L, dailyLoaded.elapsedTime);
        assertEquals(PuzzleDifficulty.CLASSIC, dailyLoaded.difficulty);
        assertTrue(Arrays.deepEquals(daily.getInitialGridCopy(), dailyLoaded.initialGrid));
        assertNull(store.loadDailyGame("2026-08-22"));
    }

    @Test
    public void favoritesUseExactIdentityAndRenameInsteadOfDuplicating() {
        GameModel model = createSavedModel(3, PuzzleDifficulty.CHALLENGE, 7);

        AndroidGameStore.FavoritePuzzle first =
                store.saveFavorite(model, "  Train the last row  ", 100L);
        AndroidGameStore.FavoritePuzzle renamed =
                store.saveFavorite(model, "Last row rematch", 200L);

        assertNotNull(first);
        assertNotNull(renamed);
        assertEquals(first.id, renamed.id);
        assertEquals(1, store.getFavoritePuzzles().length);
        assertEquals("Last row rematch", store.getFavoritePuzzles()[0].label);
        assertEquals(100L, store.getFavoritePuzzles()[0].createdAt);

        int[][] exported = renamed.getInitialGridCopy();
        exported[0][0] = 99;
        GameModel replay = renamed.createGame();
        assertTrue(Arrays.deepEquals(model.getInitialGridCopy(), replay.getGridCopy()));
        assertEquals(PuzzleDifficulty.CHALLENGE, replay.getDifficulty());
        assertEquals(0, replay.getMoveCount());
    }

    @Test
    public void favoritePracticeProgressNeverReplacesNormalOrDailySaves() {
        GameModel regular = createSavedModel(3, PuzzleDifficulty.CLASSIC, 8);
        store.saveGame(regular, 8_000L);
        DailyChallenge daily = DailyChallenge.fromDateId("2026-08-21");
        store.saveDailyGame(daily.getDateId(), daily.createGame(), 2_000L);

        GameModel favoriteModel = createSavedModel(4, PuzzleDifficulty.CHALLENGE, 0);
        AndroidGameStore.FavoritePuzzle favorite =
                store.saveFavorite(favoriteModel, "Four by four", 123L);
        GameModel practice = favorite.createGame();
        assertTrue(practice.move(Direction.LEFT));
        store.saveFavoriteRun(favorite.id, practice, 3_000L);

        SaveManager.SaveData restoredPractice = store.loadFavoriteRun(favorite.id);
        assertNotNull(restoredPractice);
        assertEquals(1, restoredPractice.moveCount);
        assertEquals(3_000L, restoredPractice.elapsedTime);
        assertEquals(8, store.loadSavedGame(3).moveCount);
        assertEquals(2_000L, store.loadDailyGame(daily.getDateId()).elapsedTime);

        store.clearSavedGame();
        assertNull(store.loadSavedGame(3));
        assertNull(store.loadDailyGame(daily.getDateId()));
        assertNull(store.loadFavoriteRun(favorite.id));
        assertEquals(1, store.getFavoritePuzzles().length);

        store.clearRecords();
        assertEquals(1, store.getFavoritePuzzles().length);
    }

    @Test
    public void removingFavoriteAlsoRemovesOnlyItsPracticeProgress() {
        GameModel model = createSavedModel(3, PuzzleDifficulty.RELAXED, 0);
        AndroidGameStore.FavoritePuzzle favorite = store.saveFavorite(model, "Warm-up", 10L);
        store.saveFavoriteRun(favorite.id, favorite.createGame(), 500L);

        assertTrue(store.removeFavorite(favorite.id));

        assertEquals(0, store.getFavoritePuzzles().length);
        assertNull(store.loadFavoriteRun(favorite.id));
        assertFalse(store.removeFavorite(favorite.id));
    }

    @Test
    public void malformedFavoriteRowsAreIgnoredWithoutLosingValidRows() {
        GameModel model = createSavedModel(3, PuzzleDifficulty.CLASSIC, 0);
        AndroidGameStore.FavoritePuzzle favorite = store.saveFavorite(model, "Valid", 10L);
        String validEncoding = prefs.getString("favorite_puzzles_v1", "");
        prefs.edit().putString("favorite_puzzles_v1",
                "broken-row\n" + validEncoding + "\nnot,a,valid,favorite").commit();

        AndroidGameStore.FavoritePuzzle[] favorites = store.getFavoritePuzzles();

        assertEquals(1, favorites.length);
        assertEquals(favorite.id, favorites[0].id);
    }

    @Test
    public void favoriteLibraryKeepsNewestFiftyExactPuzzles() {
        for (int index = 0; index < 55; index++) {
            GameModel model = new GameModel(5);
            model.scramble(PuzzleDifficulty.CHALLENGE, index);
            assertNotNull(store.saveFavorite(model, "Puzzle " + index, index));
        }

        AndroidGameStore.FavoritePuzzle[] favorites = store.getFavoritePuzzles();

        assertEquals(50, favorites.length);
        assertEquals("Puzzle 54", favorites[0].label);
        assertEquals("Puzzle 5", favorites[49].label);
    }

    @Test
    public void dailySavesRemainIndependentByCalendarDate() {
        DailyChallenge firstChallenge = DailyChallenge.fromDateId("2026-07-31");
        DailyChallenge secondChallenge = DailyChallenge.fromDateId("2026-08-01");

        store.saveDailyGame(firstChallenge.getDateId(), firstChallenge.createGame(), 1_000L, true);
        store.saveDailyGame(secondChallenge.getDateId(), secondChallenge.createGame(), 2_000L, false);

        SaveManager.SaveData first = store.loadDailyGame(firstChallenge.getDateId());
        SaveManager.SaveData second = store.loadDailyGame(secondChallenge.getDateId());
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(1_000L, first.elapsedTime);
        assertEquals(2_000L, second.elapsedTime);
        assertTrue(Arrays.deepEquals(firstChallenge.createGame().getInitialGridCopy(),
                first.initialGrid));
        assertTrue(Arrays.deepEquals(secondChallenge.createGame().getInitialGridCopy(),
                second.initialGrid));
        assertTrue(store.isDailyGameAssisted(firstChallenge.getDateId()));
        assertFalse(store.isDailyGameAssisted(secondChallenge.getDateId()));

        store.clearSavedGame();
        assertNull(store.loadDailyGame(firstChallenge.getDateId()));
        assertNull(store.loadDailyGame(secondChallenge.getDateId()));
    }

    @Test
    public void legacySingleDailySaveMigratesIntoItsDateSlot() {
        DailyChallenge challenge = DailyChallenge.fromDateId("2026-08-21");
        int[][] initialGrid = challenge.createGame().getInitialGridCopy();
        assertTrue(prefs.edit()
                .putString("daily_save_date", challenge.getDateId())
                .putInt("daily_save_size", 4)
                .putString("daily_save_grid", flattenGrid(initialGrid))
                .putString("daily_save_initial_grid", flattenGrid(initialGrid))
                .putInt("daily_save_moves", 0)
                .putLong("daily_save_elapsed", 4_321L)
                .putLong("daily_save_updated_at", 123L)
                .putBoolean("daily_save_active", true)
                .putBoolean("daily_save_solved", false)
                .putString("daily_save_difficulty", PuzzleDifficulty.CLASSIC.getId())
                .putBoolean("daily_save_assisted", true)
                .commit());

        SaveManager.SaveData migrated = store.loadDailyGame(challenge.getDateId());

        assertNotNull(migrated);
        assertEquals(4_321L, migrated.elapsedTime);
        assertTrue(store.isDailyGameAssisted(challenge.getDateId()));
        assertTrue(prefs.contains("daily_save_v2_2026-08-21_grid"));
        assertFalse(prefs.contains("daily_save_grid"));
        assertFalse(prefs.contains("daily_save_date"));
    }

    @Test
    public void assistedStateRoundTripsWithRegularAndDailySaves() {
        GameModel regular = createSavedModel(3, PuzzleDifficulty.CLASSIC, 2);
        DailyChallenge challenge = DailyChallenge.fromDateId("2026-08-21");

        store.saveGame(regular, 2_000L, true);
        store.saveDailyGame(challenge.getDateId(), challenge.createGame(), 3_000L, true);

        assertTrue(store.isSavedGameAssisted(3));
        assertTrue(store.isDailyGameAssisted(challenge.getDateId()));
        assertFalse(store.isSavedGameAssisted(4));
        assertFalse(store.isDailyGameAssisted("2026-08-22"));

        store.clearSavedGame();
        assertFalse(store.isSavedGameAssisted(3));
        assertFalse(store.isDailyGameAssisted(challenge.getDateId()));
    }

    @Test
    public void dailyCompletionsAreIdempotentAndAdvanceConsecutiveStreak() {
        assertTrue(store.recordDailyCompletion("2026-08-20"));
        assertFalse(store.recordDailyCompletion("2026-08-20"));

        AndroidGameStore.DailyProgress first = store.getDailyProgress("2026-08-20");
        assertTrue(first.completedToday);
        assertEquals(1, first.currentStreak);
        assertEquals(1, first.bestStreak);

        assertTrue(store.recordDailyCompletion("2026-08-21"));
        AndroidGameStore.DailyProgress second = store.getDailyProgress("2026-08-21");
        assertTrue(second.completedToday);
        assertEquals(2, second.currentStreak);
        assertEquals(2, second.bestStreak);
        assertEquals("2026-08-21", second.lastCompletedDateId);
    }

    @Test
    public void dailyStreakResetsAfterAGapButPreservesTheBest() {
        store.recordDailyCompletion("2026-08-20");
        store.recordDailyCompletion("2026-08-21");
        store.recordDailyCompletion("2026-08-23");

        AndroidGameStore.DailyProgress onGapDay = store.getDailyProgress("2026-08-23");
        assertEquals(1, onGapDay.currentStreak);
        assertEquals(2, onGapDay.bestStreak);

        AndroidGameStore.DailyProgress nextDay = store.getDailyProgress("2026-08-24");
        assertFalse(nextDay.completedToday);
        assertEquals(1, nextDay.currentStreak);

        AndroidGameStore.DailyProgress afterAnotherGap = store.getDailyProgress("2026-08-25");
        assertEquals(0, afterAnotherGap.currentStreak);
        assertEquals(2, afterAnotherGap.bestStreak);
    }

    @Test
    public void completingHistoricalDateDoesNotRewriteCurrentStreak() {
        store.recordDailyCompletion("2026-08-22");
        store.recordDailyCompletion("2026-08-23");

        assertTrue(store.recordDailyCompletion("2026-07-10"));

        AndroidGameStore.DailyProgress current = store.getDailyProgress("2026-08-23");
        assertEquals(2, current.currentStreak);
        assertEquals(2, current.bestStreak);
        assertEquals("2026-08-23", current.lastCompletedDateId);
        assertTrue(store.getDailyProgress("2026-07-10").completedToday);
    }

    @Test
    public void saveAndRecordResetsKeepDailyDataInTheirOwnDomains() {
        DailyChallenge challenge = DailyChallenge.fromDateId("2026-08-21");
        store.saveDailyGame(challenge.getDateId(), challenge.createGame(), 2_000L);
        store.recordDailyCompletion(challenge.getDateId());

        store.clearSavedGame();
        assertNull(store.loadDailyGame(challenge.getDateId()));
        assertTrue(store.getDailyProgress(challenge.getDateId()).completedToday);

        store.saveDailyGame(challenge.getDateId(), challenge.createGame(), 3_000L);
        store.clearRecords();
        assertNotNull(store.loadDailyGame(challenge.getDateId()));
        AndroidGameStore.DailyProgress cleared = store.getDailyProgress(challenge.getDateId());
        assertFalse(cleared.completedToday);
        assertEquals(0, cleared.currentStreak);
        assertEquals(0, cleared.bestStreak);
    }

    @Test
    public void malformedDailyDateIdsAreIgnored() {
        GameModel model = new GameModel(4);
        store.saveDailyGame("not-a-date", model, 1_000L);

        assertNull(store.loadDailyGame("not-a-date"));
        assertFalse(store.recordDailyCompletion("not-a-date"));
        AndroidGameStore.DailyProgress progress = store.getDailyProgress("not-a-date");
        assertFalse(progress.completedToday);
        assertEquals(0, progress.currentStreak);
        assertEquals(0, progress.bestStreak);
    }

    @Test
    public void completionHistoryAndLifetimeStatsRemainIndependentByScope() {
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 10, 1_000L, false, 100L);
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 20, 3_000L, false, 200L);
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 50, 5_000L, true, 300L);
        store.recordCompletion(4, PuzzleDifficulty.CHALLENGE, 40, 8_000L, false, 400L);

        AndroidGameStore.CompletionRecord[] history = store.getCompletionHistory();
        assertEquals(4, history.length);
        assertEquals(400L, history[0].completedAt);
        assertEquals(4, history[0].size);
        assertEquals(PuzzleDifficulty.CHALLENGE, history[0].difficulty);
        assertFalse(history[0].assisted);
        assertEquals(100L, history[3].completedAt);

        AndroidGameStore.CompletionStats classic =
                store.getCompletionStats(3, PuzzleDifficulty.CLASSIC);
        assertEquals(2, classic.playerCompletions);
        assertEquals(1, classic.assistedCompletions);
        assertEquals(30L, classic.playerMoves);
        assertEquals(4_000L, classic.playerTimeMs);

        AndroidGameStore.CompletionStats challenge =
                store.getCompletionStats(4, PuzzleDifficulty.CHALLENGE);
        assertEquals(1, challenge.playerCompletions);
        assertEquals(0, challenge.assistedCompletions);
        assertEquals(40L, challenge.playerMoves);
        assertEquals(8_000L, challenge.playerTimeMs);

        AndroidGameStore.CompletionStats overall = store.getOverallCompletionStats();
        assertEquals(3, overall.playerCompletions);
        assertEquals(1, overall.assistedCompletions);
        assertEquals(70L, overall.playerMoves);
        assertEquals(12_000L, overall.playerTimeMs);
    }

    @Test
    public void personalTrendUsesOnlyPlayerCompletionsInOneScope() {
        int[] oldestToNewestMoves = {34, 32, 30, 22, 20, 18};
        for (int index = 0; index < oldestToNewestMoves.length; index++) {
            int moves = oldestToNewestMoves[index];
            store.recordCompletion(3, PuzzleDifficulty.CLASSIC,
                    moves, moves * 1_000L, false, 100L + index);
        }
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 1, 1L, true, 1_000L);
        store.recordCompletion(4, PuzzleDifficulty.CLASSIC, 1, 1L, false, 2_000L);

        PersonalTrend trend = store.getPersonalTrend(3, PuzzleDifficulty.CLASSIC);

        assertEquals(20L, trend.getRecentAverageMoves());
        assertEquals(32L, trend.getPreviousAverageMoves());
        assertEquals(PersonalTrend.Direction.IMPROVING, trend.getMoveDirection());
    }

    @Test
    public void weeklyGoalIsOwnerConfigurableAndCountsOnlyPlayerSolves() {
        ZoneId utc = ZoneId.of("UTC");
        assertEquals(5, store.getWeeklyGoalTarget());
        store.setWeeklyGoalTarget(3);
        store.setWeeklyGoalTarget(99);
        assertEquals(3, store.getWeeklyGoalTarget());
        store.setTrendSize(5);
        store.setTrendSize(9);
        store.setTrendDifficulty(PuzzleDifficulty.CHALLENGE);
        assertEquals(5, store.getTrendSize());
        assertEquals(PuzzleDifficulty.CHALLENGE, store.getTrendDifficulty());

        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 10, 1_000L, false,
                LocalDate.of(2026, 8, 17).atStartOfDay(utc).toInstant().toEpochMilli());
        store.recordCompletion(4, PuzzleDifficulty.CHALLENGE, 20, 2_000L, false,
                LocalDate.of(2026, 8, 19).atStartOfDay(utc).toInstant().toEpochMilli());
        store.recordCompletion(5, PuzzleDifficulty.RELAXED, 30, 3_000L, true,
                LocalDate.of(2026, 8, 20).atStartOfDay(utc).toInstant().toEpochMilli());
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 40, 4_000L, false,
                LocalDate.of(2026, 8, 16).atStartOfDay(utc).toInstant().toEpochMilli());

        WeeklyGoalProgress progress = store.getWeeklyGoalProgress(
                LocalDate.of(2026, 8, 23), utc);

        assertEquals(2, progress.getCompleted());
        assertEquals(3, progress.getTarget());
        assertEquals(1, progress.getRemaining());
    }

    @Test
    public void completionHistoryIsBoundedWithoutTruncatingLifetimeStats() {
        for (int index = 0; index < 55; index++) {
            store.recordCompletion(5, PuzzleDifficulty.RELAXED,
                    100 + index, 1_000L + index, false, index);
        }

        AndroidGameStore.CompletionRecord[] history = store.getCompletionHistory();
        assertEquals(50, history.length);
        assertEquals(54L, history[0].completedAt);
        assertEquals(5L, history[49].completedAt);

        AndroidGameStore.CompletionStats stats =
                store.getCompletionStats(5, PuzzleDifficulty.RELAXED);
        assertEquals(55, stats.playerCompletions);
        assertEquals(0, stats.assistedCompletions);
    }

    @Test
    public void clearRecordsRemovesBestsHistoryAndLifetimeStats() {
        assertTrue(store.recordBestIfBetter(3, PuzzleDifficulty.CLASSIC, 10, 1_000L));
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 10, 1_000L, false, 100L);
        store.recordCompletion(3, PuzzleDifficulty.CLASSIC, 20, 2_000L, true, 200L);

        store.clearRecords();

        assertNull(store.getBest(3, PuzzleDifficulty.CLASSIC));
        assertEquals(0, store.getCompletionHistory().length);
        AndroidGameStore.CompletionStats stats =
                store.getCompletionStats(3, PuzzleDifficulty.CLASSIC);
        assertEquals(0, stats.playerCompletions);
        assertEquals(0, stats.assistedCompletions);
        assertEquals(0L, stats.playerMoves);
        assertEquals(0L, stats.playerTimeMs);
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
    public void soundAndVisualThemeUseAccessibleDefaultsAndPersist() {
        assertFalse(store.isSoundEnabled());
        assertEquals(AndroidVisualTheme.MIDNIGHT, store.getVisualTheme());

        store.setSoundEnabled(true);
        store.setVisualTheme(AndroidVisualTheme.OCEAN);

        AndroidGameStore reopened = new AndroidGameStore(targetContext);
        assertTrue(reopened.isSoundEnabled());
        assertEquals(AndroidVisualTheme.OCEAN, reopened.getVisualTheme());
    }

    @Test
    public void unsupportedStoredLanguageFallsBackToEnglish() {
        prefs.edit().putString("language_tag", "fr-FR").commit();

        assertEquals(AndroidAppLocale.DEFAULT_LANGUAGE_TAG, store.getLanguageTag());
    }

    @Test
    public void personalDataBackupRoundTripRestoresSavesRecordsAndSettings() {
        store.markOnboardingSeen();
        store.setLanguageTag(AndroidAppLocale.JAPANESE_LANGUAGE_TAG);
        store.setSoundEnabled(true);
        store.setVisualTheme(AndroidVisualTheme.OCEAN);
        store.saveGame(createSavedModel(4, PuzzleDifficulty.CHALLENGE, 12), 34_000L);
        store.recordBestIfBetter(4, PuzzleDifficulty.CHALLENGE, 12, 34_000L);
        store.recordCompletion(4, PuzzleDifficulty.CHALLENGE, 12, 34_000L, false, 123L);
        store.recordDailyCompletion("2026-08-22");
        AndroidGameStore.FavoritePuzzle favorite = store.saveFavorite(
                createSavedModel(3, PuzzleDifficulty.RELAXED, 0), "Backup favorite", 456L);

        String backup = store.exportPersonalData();
        assertTrue(prefs.edit().clear().commit());
        store.setSoundEnabled(false);

        store.importPersonalData(backup);

        AndroidGameStore restored = new AndroidGameStore(targetContext);
        assertTrue(restored.isOnboardingSeen());
        assertEquals(AndroidAppLocale.JAPANESE_LANGUAGE_TAG, restored.getLanguageTag());
        assertTrue(restored.isSoundEnabled());
        assertEquals(AndroidVisualTheme.OCEAN, restored.getVisualTheme());
        SaveManager.SaveData savedGame = restored.loadSavedGame(4);
        assertNotNull(savedGame);
        assertEquals(12, savedGame.moveCount);
        assertEquals(34_000L, savedGame.elapsedTime);
        assertEquals(PuzzleDifficulty.CHALLENGE, savedGame.difficulty);
        AndroidGameStore.Best best = restored.getBest(4, PuzzleDifficulty.CHALLENGE);
        assertNotNull(best);
        assertEquals(12, best.moves);
        assertEquals(34_000L, best.timeMs);
        AndroidGameStore.CompletionRecord[] history = restored.getCompletionHistory();
        assertEquals(1, history.length);
        assertEquals(123L, history[0].completedAt);
        assertTrue(restored.getDailyProgress("2026-08-22").completedToday);
        assertEquals(1, restored.getFavoritePuzzles().length);
        assertEquals(favorite.id, restored.getFavoritePuzzles()[0].id);
    }

    @Test
    public void invalidPersonalDataBackupsDoNotReplaceExistingData() {
        store.setLanguageTag(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG);
        store.setSoundEnabled(true);

        String duplicateKey = "{\"format\":\"slidedo-personal-data\",\"version\":1,"
                + "\"createdAt\":1,\"entries\":[{\"key\":\"duplicate\",\"type\":\"int\","
                + "\"value\":1},{\"key\":\"duplicate\",\"type\":\"int\",\"value\":2}]}";
        String unsupportedType = "{\"format\":\"slidedo-personal-data\",\"version\":1,"
                + "\"createdAt\":1,\"entries\":[{\"key\":\"value\",\"type\":\"bytes\","
                + "\"value\":\"AA==\"}]}";
        String[] invalidBackups = {
                "{}",
                "{\"format\":\"slidedo-personal-data\",\"version\":99,\"entries\":[]}",
                duplicateKey,
                unsupportedType,
                "x".repeat(AndroidPersonalDataArchive.MAX_ARCHIVE_CHARS + 1)
        };

        for (int index = 0; index < invalidBackups.length; index++) {
            boolean rejected = false;
            try {
                store.importPersonalData(invalidBackups[index]);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }

            assertTrue("Invalid backup was accepted at index " + index, rejected);
            AndroidGameStore unchanged = new AndroidGameStore(targetContext);
            assertEquals(AndroidAppLocale.TRADITIONAL_CHINESE_LANGUAGE_TAG,
                    unchanged.getLanguageTag());
            assertTrue(unchanged.isSoundEnabled());
        }
    }

    @Test
    public void personalDataBackupClearsPreferencesMissingFromArchive() {
        store.setSoundEnabled(true);

        store.importPersonalData("{\"format\":\"slidedo-personal-data\",\"version\":1,"
                + "\"createdAt\":123,\"entries\":[{\"key\":\"onboarding_seen\","
                + "\"type\":\"boolean\",\"value\":true}]}");

        AndroidGameStore restored = new AndroidGameStore(targetContext);
        assertTrue(restored.isOnboardingSeen());
        assertFalse(restored.isSoundEnabled());
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

    private static String flattenGrid(int[][] grid) {
        StringBuilder flattened = new StringBuilder();
        for (int[] row : grid) {
            for (int value : row) {
                if (flattened.length() > 0) {
                    flattened.append(',');
                }
                flattened.append(value);
            }
        }
        return flattened.toString();
    }
}
