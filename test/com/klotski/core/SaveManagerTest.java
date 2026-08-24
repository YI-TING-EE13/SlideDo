package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

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

            File saveFile = new File(tempDir, "klotski_save.json");
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
