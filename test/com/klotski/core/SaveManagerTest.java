package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
}
