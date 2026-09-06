package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveManagerTest {
    @TempDir
    File tempDir;

    @Test
    void saveAndLoadRoundTripsJsonDataThroughSpecifiedFiles() {
        int[][] initial = {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        };
        GameModel model = new GameModel(3);
        model.loadState(initial, 0);
        model.move(Direction.UP);

        File saveFile = new File(tempDir, "save.json");
        File legacyFile = new File(tempDir, "legacy.dat");

        assertTrue(SaveManager.saveGame(model, saveFile));
        SaveManager.SaveData data = SaveManager.loadGame(saveFile, legacyFile);

        assertNotNull(data);
        assertEquals(3, data.size);
        assertEquals(1, data.moveCount);
        assertTrue(data.elapsedTime >= 0);
        assertTrue(data.updatedAt > 0);
        assertTrue(data.active);
        assertTrue(!data.solved);
        assertEquals(PuzzleDifficulty.CLASSIC, data.difficulty);
        assertArrayEquals(model.getGridCopy(), data.grid);
        assertArrayEquals(initial, data.initialGrid);
        assertEquals("U1", data.actionHistory);
        assertEquals("", data.redoHistory);

        GameModel restored = new GameModel(3);
        restored.loadState(data);
        assertTrue(restored.canUndo());
        assertTrue(restored.undo());
        assertTrue(restored.canRedo());

        GameModel restarted = new GameModel(3);
        restarted.loadState(data);
        restarted.restartCurrentGame();
        assertArrayEquals(initial, restarted.getGridCopy());
        assertEquals(0, restarted.getMoveCount());

        assertTrue(SaveManager.saveGame(restored, saveFile));
        GameModel restoredWithRedo = new GameModel(3);
        restoredWithRedo.loadState(SaveManager.loadGame(saveFile, legacyFile));
        assertTrue(restoredWithRedo.canRedo());
        assertTrue(restoredWithRedo.redo());
        assertArrayEquals(model.getGridCopy(), restoredWithRedo.getGridCopy());
    }

    @Test
    void defaultSavePathUsesConfiguredUserDataDirectory() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            GameModel model = new GameModel(3);
            model.scramble(PuzzleDifficulty.CHALLENGE, 91L);

            assertTrue(SaveManager.saveGame(model));

            File saveFile = new File(tempDir, "klotski_save_3.json");
            assertTrue(saveFile.exists());
            SaveManager.SaveData loaded = SaveManager.loadGame();
            assertNotNull(loaded);
            assertEquals(3, loaded.size);
            assertTrue(loaded.active);
            assertEquals(PuzzleDifficulty.CHALLENGE, loaded.difficulty);
        } finally {
            if (oldValue == null) {
                System.clearProperty(SaveManager.DATA_DIR_PROPERTY);
            } else {
                System.setProperty(SaveManager.DATA_DIR_PROPERTY, oldValue);
            }
        }
    }

    @Test
    void savesForDifferentSizesUseIndependentSlotsAndMetadata() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            GameModel three = new GameModel(3);
            three.scramble(PuzzleDifficulty.RELAXED, 3L);
            GameModel four = new GameModel(4);
            four.scramble(PuzzleDifficulty.CHALLENGE, 4L);
            GameModel five = new GameModel(5);
            five.scramble(PuzzleDifficulty.CLASSIC, 5L);

            assertTrue(SaveManager.saveGame(three));
            assertTrue(SaveManager.saveGame(four));
            assertTrue(SaveManager.saveGame(five));

            assertEquals(PuzzleDifficulty.RELAXED, SaveManager.loadGame(3).difficulty);
            assertEquals(PuzzleDifficulty.CHALLENGE, SaveManager.loadGame(4).difficulty);
            assertEquals(PuzzleDifficulty.CLASSIC, SaveManager.loadGame(5).difficulty);
            assertTrue(new File(tempDir, "klotski_save_3.json").exists());
            assertTrue(new File(tempDir, "klotski_save_4.json").exists());
            assertTrue(new File(tempDir, "klotski_save_5.json").exists());

            SaveManager.SaveMetadata[] metadata = SaveManager.getAllSaveMetadata();
            assertEquals(3, metadata.length);
            assertEquals(3, metadata[0].size);
            assertEquals(4, metadata[1].size);
            assertEquals(5, metadata[2].size);
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void legacyJsonMigratesWithoutDeletingSourceOrOverwritingNewerSlot() throws Exception {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            File legacy = new File(tempDir, "klotski_save.json");
            String legacyJson = "{\n"
                    + "  \"version\": 1,\n"
                    + "  \"size\": 3,\n"
                    + "  \"moveCount\": 7,\n"
                    + "  \"elapsedTime\": 7000,\n"
                    + "  \"grid\": [[1,2,3],[4,5,0],[7,8,6]]\n"
                    + "}\n";
            Files.writeString(legacy.toPath(), legacyJson, StandardCharsets.UTF_8);

            SaveManager.SaveData migrated = SaveManager.loadGame(3);
            assertNotNull(migrated);
            assertEquals(7, migrated.moveCount);
            assertEquals(PuzzleDifficulty.CLASSIC, migrated.difficulty);
            assertEquals("", migrated.actionHistory);
            assertTrue(legacy.exists());
            assertTrue(new File(tempDir, "klotski_save_3.json").exists());

            GameModel newer = new GameModel(3);
            newer.scramble(PuzzleDifficulty.CHALLENGE, 33L);
            assertTrue(SaveManager.saveGame(newer));
            Files.writeString(legacy.toPath(), legacyJson.replace("7000", "1"),
                    StandardCharsets.UTF_8);
            SaveManager.SaveData preserved = SaveManager.loadGame(3);
            assertEquals(newer.getMoveCount(), preserved.moveCount);
            assertEquals(PuzzleDifficulty.CHALLENGE, preserved.difficulty);
            assertTrue(legacy.exists());
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void legacySerializedSaveMigratesAndKeepsTheOriginalFile() throws Exception {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            File legacy = new File(tempDir, "klotski_save.dat");
            SaveManager.SaveData payload = new SaveManager.SaveData();
            payload.size = 4;
            payload.grid = new int[][] {
                    {1, 2, 3, 4},
                    {5, 6, 7, 8},
                    {9, 10, 11, 12},
                    {13, 14, 0, 15}
            };
            payload.initialGrid = payload.grid;
            payload.moveCount = 2;
            payload.elapsedTime = 4321L;
            payload.updatedAt = 77L;
            payload.active = true;
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(legacy))) {
                output.writeObject(payload);
            }

            SaveManager.SaveData migrated = SaveManager.loadGame(4);
            assertNotNull(migrated);
            assertEquals(2, migrated.moveCount);
            assertEquals(4321L, migrated.elapsedTime);
            assertEquals(PuzzleDifficulty.CLASSIC, migrated.difficulty);
            assertTrue(legacy.exists());
            assertTrue(new File(tempDir, "klotski_save_4.json").exists());
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void malformedCurrentJsonFallsBackToAtomicBackupAndHistoryIsDiscardedSafely() throws Exception {
        File saveFile = new File(tempDir, "slot.json");
        File legacyFile = new File(tempDir, "missing.dat");
        GameModel first = new GameModel(3);
        first.scramble(PuzzleDifficulty.RELAXED, 8L);
        assertTrue(SaveManager.saveGame(first, saveFile));

        GameModel second = new GameModel(3);
        second.scramble(PuzzleDifficulty.CHALLENGE, 9L);
        assertTrue(SaveManager.saveGame(second, saveFile));
        Files.writeString(saveFile.toPath(), "{ definitely incomplete", StandardCharsets.UTF_8);

        SaveManager.SaveData recovered = SaveManager.loadGame(saveFile, legacyFile);
        assertNotNull(recovered);
        assertEquals(PuzzleDifficulty.RELAXED, recovered.difficulty);
        assertArrayEquals(first.getGridCopy(), recovered.grid);

        recovered.actionHistory = "not-a-history";
        recovered.redoHistory = "also-invalid";
        GameModel restored = new GameModel(3);
        restored.loadState(recovered);
        assertFalse(restored.canUndo());
        assertFalse(restored.canRedo());
        assertArrayEquals(first.getGridCopy(), restored.getGridCopy());
    }

    @Test
    void failedAtomicSaveDoesNotReplaceAnExistingDirectoryTarget() {
        GameModel model = new GameModel(3);
        model.scramble(PuzzleDifficulty.CLASSIC, 12L);
        File directoryTarget = new File(tempDir, "not-a-file");
        assertTrue(directoryTarget.mkdirs());

        assertFalse(SaveManager.saveGame(model, directoryTarget));
        assertTrue(directoryTarget.isDirectory());
    }

    private static void restoreDataDirectoryProperty(String oldValue) {
        if (oldValue == null) {
            System.clearProperty(SaveManager.DATA_DIR_PROPERTY);
        } else {
            System.setProperty(SaveManager.DATA_DIR_PROPERTY, oldValue);
        }
    }

    @Test
    void bestRecordsPreferLowerMovesThenLowerTime() {
        File recordsFile = new File(tempDir, "records.json");

        SaveManager.BestRecord first = SaveManager.recordBest(recordsFile, 4, 30, 90_000);
        SaveManager.BestRecord worseMoves = SaveManager.recordBest(recordsFile, 4, 31, 1_000);
        SaveManager.BestRecord betterTime = SaveManager.recordBest(recordsFile, 4, 30, 80_000);
        SaveManager.BestRecord betterMoves = SaveManager.recordBest(recordsFile, 4, 29, 120_000);
        SaveManager.BestRecord saved = SaveManager.getBestRecord(recordsFile, 4);

        assertEquals(30, first.moves);
        assertEquals(90_000, first.timeMs);
        assertEquals(30, worseMoves.moves);
        assertEquals(90_000, worseMoves.timeMs);
        assertEquals(30, betterTime.moves);
        assertEquals(80_000, betterTime.timeMs);
        assertEquals(29, betterMoves.moves);
        assertEquals(120_000, betterMoves.timeMs);
        assertNotNull(saved);
        assertEquals(29, saved.moves);
        assertEquals(120_000, saved.timeMs);
    }

    @Test
    void scopedRecordsSeparateDifficultyAndMapLegacyOnlyToClassic() throws Exception {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            File legacy = new File(tempDir, "klotski_records.json");
            String legacyJson = "{\n  \"3\": {\"moves\": 20, \"timeMs\": 9000}\n}\n";
            Files.writeString(legacy.toPath(), legacyJson, StandardCharsets.UTF_8);

            SaveManager.BestRecord classic = SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC);
            assertNotNull(classic);
            assertEquals(20, classic.moves);
            assertNull(SaveManager.getBestRecord(3, PuzzleDifficulty.RELAXED));

            SaveManager.recordBest(3, PuzzleDifficulty.RELAXED, 12, 5000);
            assertEquals(12, SaveManager.getBestRecord(3, PuzzleDifficulty.RELAXED).moves);
            assertEquals(20, SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC).moves);

            SaveManager.recordBest(3, PuzzleDifficulty.CLASSIC, 18, 8000);
            assertEquals(18, SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC).moves);
            assertEquals(legacyJson, Files.readString(legacy.toPath(), StandardCharsets.UTF_8));
            assertTrue(new File(tempDir, "klotski_records_v2.json").exists());

            SaveManager.recordBest(3, PuzzleDifficulty.CLASSIC, 18, 7000);
            assertEquals(7000, SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC).timeMs);
            SaveManager.recordBest(3, PuzzleDifficulty.CLASSIC, 19, 1);
            assertEquals(7000, SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC).timeMs);
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void completionHistoryAndStatisticsAreExactlyOnceAndAssistedSafe() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            assertTrue(SaveManager.recordCompletion("player-run", 4, PuzzleDifficulty.CHALLENGE,
                    11, 2200, false, 100));
            assertFalse(SaveManager.recordCompletion("player-run", 4, PuzzleDifficulty.CHALLENGE,
                    11, 2200, false, 101));
            assertTrue(SaveManager.recordCompletion("assist-run", 4, PuzzleDifficulty.CHALLENGE,
                    4, 900, true, 102));

            SaveManager.CompletionStats stats = SaveManager.getCompletionStats(
                    4, PuzzleDifficulty.CHALLENGE);
            assertEquals(1, stats.playerCompletions);
            assertEquals(1, stats.assistedCompletions);
            assertEquals(11, stats.playerMoves);
            assertEquals(2200, stats.playerTimeMs);

            SaveManager.CompletionStats overall = SaveManager.getOverallCompletionStats();
            assertEquals(1, overall.playerCompletions);
            assertEquals(1, overall.assistedCompletions);
            SaveManager.CompletionRecord[] history = SaveManager.getCompletionHistory();
            assertEquals(2, history.length);
            assertEquals("assist-run", history[0].id);
            assertTrue(history[0].assisted);
            assertFalse(history[1].assisted);
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void completionHistoryIsBoundedWithoutLosingLifetimeCountsOrDeduplication() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            for (int index = 0; index < 51; index++) {
                assertTrue(SaveManager.recordCompletion("run-" + index, 3,
                        PuzzleDifficulty.CLASSIC, index + 1, index + 100, false, index));
            }

            assertEquals(50, SaveManager.getCompletionHistory().length);
            assertEquals(51, SaveManager.getCompletionStats(3, PuzzleDifficulty.CLASSIC)
                    .playerCompletions);
            assertFalse(SaveManager.recordCompletion("run-0", 3, PuzzleDifficulty.CLASSIC,
                    99, 99, false, 99));
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void dailySaveUsesDatedNamespaceAndPreservesNormalSlot() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            LocalDate date = LocalDate.now().minusDays(2);
            GameModel daily = DailyChallenge.forDate(date).createGame();
            assertTrue(daily.move(Direction.UP));
            assertTrue(SaveManager.saveDailyGame(date.toString(), daily, true));

            GameModel normal = new GameModel(4);
            normal.scramble(PuzzleDifficulty.RELAXED, 44L);
            assertTrue(SaveManager.saveGame(normal));
            SaveManager.recordBest(4, PuzzleDifficulty.CLASSIC, 50, 50_000);

            SaveManager.SaveData loadedDaily = SaveManager.loadDailyGame(date.toString());
            assertNotNull(loadedDaily);
            assertEquals(PuzzleDifficulty.CLASSIC, loadedDaily.difficulty);
            assertTrue(SaveManager.isDailyGameAssisted(date.toString()));
            assertEquals(normal.getMoveCount(), SaveManager.loadGame(4).moveCount);
            assertEquals(50, SaveManager.getBestRecord(4, PuzzleDifficulty.CLASSIC).moves);
            assertTrue(new File(tempDir, "klotski_daily_" + date + ".json").exists());
            assertNotNull(SaveManager.getDailySaveMetadata(date.toString()));
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void dailyFutureDatesAreRejectedAndHistoricalStreaksDoNotMoveLatestBackward() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            LocalDate today = LocalDate.of(2026, 9, 10);
            assertTrue(SaveManager.isDailyDatePlayable("2026-09-10", today));
            assertFalse(SaveManager.isDailyDatePlayable("2026-09-11", today));
            GameModel future = DailyChallenge.forDate(LocalDate.now().plusDays(1)).createGame();
            assertFalse(SaveManager.saveDailyGame(LocalDate.now().plusDays(1).toString(), future, false));
            assertTrue(SaveManager.recordDailyCompletion("2026-09-08", today));
            assertTrue(SaveManager.recordDailyCompletion("2026-09-09", today));
            assertFalse(SaveManager.recordDailyCompletion("2026-09-11", today));

            SaveManager.DailyProgress beforeHistorical = SaveManager.getDailyProgress("2026-09-10");
            assertEquals(2, beforeHistorical.currentStreak);
            assertEquals(2, beforeHistorical.bestStreak);
            assertTrue(SaveManager.recordDailyCompletion("2026-09-07", today));
            SaveManager.DailyProgress afterHistorical = SaveManager.getDailyProgress("2026-09-10");
            assertEquals(2, afterHistorical.currentStreak);
            assertEquals(2, afterHistorical.bestStreak);
            assertEquals("2026-09-09", afterHistorical.lastCompletedDateId);
            assertFalse(SaveManager.recordDailyCompletion("2026-09-08", today));
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void favoritesUseExactIdentityAndIsolatedPracticeNamespace() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            GameModel normal = new GameModel(4);
            normal.scramble(PuzzleDifficulty.CHALLENGE, 401L);
            assertTrue(SaveManager.saveGame(normal));
            GameModel daily = DailyChallenge.forDate(LocalDate.now().minusDays(1)).createGame();
            assertTrue(daily.move(Direction.UP));
            assertTrue(SaveManager.saveDailyGame(LocalDate.now().minusDays(1).toString(), daily, false));

            SaveManager.FavoritePuzzle favorite = SaveManager.saveFavorite(normal, "  Saved board  ", 11L);
            assertNotNull(favorite);
            assertEquals("Saved board", favorite.label);
            assertEquals(favorite.id, SaveManager.saveFavorite(normal, "Renamed", 12L).id);
            assertEquals(1, SaveManager.getFavoritePuzzles().length);
            assertEquals("Renamed", SaveManager.getFavoritePuzzle(favorite.id).label);

            GameModel practice = favorite.createGame();
            assertTrue(practice.move(Direction.UP));
            assertTrue(SaveManager.saveFavoriteRun(favorite.id, practice, true));
            SaveManager.SaveData restoredPractice = SaveManager.loadFavoriteRun(favorite.id);
            assertNotNull(restoredPractice);
            assertEquals(1, restoredPractice.moveCount);
            assertTrue(SaveManager.isFavoriteRunAssisted(favorite.id));
            assertEquals(normal.getMoveCount(), SaveManager.loadGame(4).moveCount);
            assertEquals(daily.getMoveCount(), SaveManager.loadDailyGame(
                    LocalDate.now().minusDays(1).toString()).moveCount);
            assertNull(SaveManager.getBestRecord(4, PuzzleDifficulty.CHALLENGE));
            assertEquals(0, SaveManager.getCompletionStats(4, PuzzleDifficulty.CHALLENGE)
                    .playerCompletions);

            assertTrue(SaveManager.renameFavorite(favorite.id, "Final label"));
            assertEquals("Final label", SaveManager.getFavoritePuzzle(favorite.id).label);
            assertTrue(SaveManager.removeFavorite(favorite.id));
            assertNull(SaveManager.getFavoritePuzzle(favorite.id));
            assertNull(SaveManager.loadFavoriteRun(favorite.id));
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void trendsAndWeeklyGoalsPersistScopeAndExcludeAssistedOrOtherScopes() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            assertTrue(SaveManager.setTrendSize(3));
            assertTrue(SaveManager.setTrendDifficulty(PuzzleDifficulty.RELAXED));
            assertTrue(SaveManager.setWeeklyGoalTarget(4));
            assertEquals(3, SaveManager.getTrendSize());
            assertEquals(PuzzleDifficulty.RELAXED, SaveManager.getTrendDifficulty());
            assertEquals(4, SaveManager.getWeeklyGoalTarget());

            long monday = LocalDate.of(2026, 9, 7).atStartOfDay(ZoneId.of("UTC"))
                    .toInstant().toEpochMilli();
            for (int index = 0; index < 6; index++) {
                assertTrue(SaveManager.recordCompletion("trend-player-" + index, 3,
                        PuzzleDifficulty.RELAXED, 30 - index, 3_000L - index * 100L,
                        false, monday + index * 60_000L));
            }
            assertTrue(SaveManager.recordCompletion("trend-assisted", 3,
                    PuzzleDifficulty.RELAXED, 1, 1, true, monday));
            assertTrue(SaveManager.recordCompletion("trend-other-scope", 4,
                    PuzzleDifficulty.RELAXED, 1, 1, false, monday));

            PersonalTrend trend = SaveManager.getPersonalTrend(3, PuzzleDifficulty.RELAXED);
            assertEquals(3, trend.getRecentCount());
            assertEquals(3, trend.getPreviousCount());
            assertEquals(PersonalTrend.Direction.IMPROVING, trend.getMoveDirection());
            WeeklyGoalProgress progress = SaveManager.getWeeklyGoalProgress(
                    LocalDate.of(2026, 9, 7), ZoneId.of("UTC"), 3, PuzzleDifficulty.RELAXED);
            assertEquals(6, progress.getCompleted());
            assertTrue(progress.isReached());
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void favoriteLibraryIsBoundedAtFiftyEntries() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            for (int index = 0; index < 51; index++) {
                GameModel model = new GameModel(3);
                model.scramble(PuzzleDifficulty.CHALLENGE, 10_000L + index);
                assertNotNull(SaveManager.saveFavorite(model, "Puzzle " + index, index));
            }
            SaveManager.FavoritePuzzle[] favorites = SaveManager.getFavoritePuzzles();
            assertEquals(50, favorites.length);
            assertEquals("Puzzle 50", favorites[0].label);
            assertEquals("Puzzle 1", favorites[49].label);
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void continuousChallengeRoundTripsAndClearsOnlyItsNamespace() {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            GameModel normal = new GameModel(3);
            normal.scramble(PuzzleDifficulty.CLASSIC, 77L);
            assertTrue(SaveManager.saveGame(normal));
            ContinuousChallenge challenge = ContinuousChallenge.start(3)
                    .completePuzzle(8, 900L, false);
            GameModel current = new GameModel(4);
            current.scramble(PuzzleDifficulty.CHALLENGE, 88L);
            assertTrue(SaveManager.saveContinuousGame(current, challenge, true));

            SaveManager.ContinuousGame restored = SaveManager.loadContinuousGame();
            assertNotNull(restored);
            assertEquals(4, restored.size);
            assertEquals(PuzzleDifficulty.CHALLENGE, restored.difficulty);
            assertEquals(1, restored.challenge.getCompletedPuzzles());
            assertEquals(8, restored.challenge.getTotalMoves());
            assertTrue(restored.assisted);
            assertEquals(current.getMoveCount(), restored.game.moveCount);

            assertTrue(SaveManager.clearContinuousGame());
            assertNull(SaveManager.loadContinuousGame());
            assertEquals(normal.getMoveCount(), SaveManager.loadGame(3).moveCount);
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }

    @Test
    void desktopPreferencesPersistAndResetsKeepTheirDocumentedDomains() throws Exception {
        String oldValue = System.getProperty(SaveManager.DATA_DIR_PROPERTY);
        System.setProperty(SaveManager.DATA_DIR_PROPERTY, tempDir.getAbsolutePath());
        try {
            assertTrue(SaveManager.setReducedMotionEnabled(true));
            assertTrue(SaveManager.setSoundEnabled(true));
            assertTrue(SaveManager.setDesktopTheme("ocean"));
            assertTrue(SaveManager.setDesktopLanguageTag("zh-TW"));
            assertTrue(SaveManager.markOnboardingSeen());
            assertTrue(SaveManager.isReducedMotionEnabled());
            assertTrue(SaveManager.isSoundEnabled());
            assertEquals("ocean", SaveManager.getDesktopTheme());
            assertEquals("zh-TW", SaveManager.getDesktopLanguageTag());
            assertTrue(SaveManager.isOnboardingSeen());
            assertFalse(SaveManager.setDesktopTheme("neon"));
            assertFalse(SaveManager.setDesktopLanguageTag("fr"));

            GameModel normal = new GameModel(3);
            normal.scramble(PuzzleDifficulty.CLASSIC, 700L);
            assertTrue(SaveManager.saveGame(normal));
            Files.writeString(new File(tempDir, "klotski_save.json").toPath(),
                    "{\"version\":1,\"size\":3,\"grid\":[[1,2,3],[4,5,6],[7,0,8]]}\n");
            LocalDate date = LocalDate.now().minusDays(1);
            GameModel daily = DailyChallenge.forDate(date).createGame();
            assertTrue(SaveManager.saveDailyGame(date.toString(), daily, false));
            SaveManager.FavoritePuzzle favorite = SaveManager.saveFavorite(normal, "Keep label", 701L);
            assertNotNull(favorite);
            assertTrue(SaveManager.saveFavoriteRun(favorite.id, favorite.createGame(), false));
            ContinuousChallenge challenge = ContinuousChallenge.start(3);
            assertTrue(SaveManager.saveContinuousGame(normal, challenge, false));
            assertNotNull(SaveManager.recordBest(3, PuzzleDifficulty.CLASSIC, 9, 900L));
            assertTrue(SaveManager.recordCompletion("reset-player", 3, PuzzleDifficulty.CLASSIC,
                    9, 900L, false));
            assertTrue(SaveManager.recordDailyCompletion(date.toString()));

            assertTrue(SaveManager.clearSavedGames());
            assertNull(SaveManager.loadGame(3));
            assertFalse(new File(tempDir, "klotski_save.json").exists());
            assertNull(SaveManager.loadDailyGame(date.toString()));
            assertNull(SaveManager.loadFavoriteRun(favorite.id));
            assertNull(SaveManager.loadContinuousGame());
            assertNotNull(SaveManager.getFavoritePuzzle(favorite.id));
            assertNotNull(SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC));
            assertEquals(1, SaveManager.getCompletionStats(3, PuzzleDifficulty.CLASSIC)
                    .playerCompletions);
            assertTrue(SaveManager.getDailyProgress(date.toString()).completed);

            // Records reset does not end an active Continuous Challenge.
            assertTrue(SaveManager.saveContinuousGame(normal, challenge, false));
            assertTrue(SaveManager.clearRecords());
            assertNull(SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC));
            assertEquals(0, SaveManager.getCompletionStats(3, PuzzleDifficulty.CLASSIC)
                    .playerCompletions);
            assertFalse(SaveManager.getDailyProgress(date.toString()).completed);
            assertNotNull(SaveManager.loadContinuousGame());
            Files.writeString(new File(tempDir, "klotski_records.json").toPath(),
                    "{\n  \"3\": {\"moves\": 1, \"timeMs\": 1}\n}\n");
            assertNull(SaveManager.getBestRecord(3, PuzzleDifficulty.CLASSIC));
        } finally {
            restoreDataDirectoryProperty(oldValue);
        }
    }
}
