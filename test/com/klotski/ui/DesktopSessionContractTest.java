package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import org.junit.jupiter.api.Test;

class DesktopSessionContractTest {
    @Test
    void allSupportedSizesAcceptEveryDifficulty() {
        for (int size : new int[] {3, 4, 5}) {
            for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
                GameModel model = DesktopGameFactory.create(size, difficulty, size * 100L);

                assertEquals(size, model.getSize());
                assertEquals(difficulty, model.getDifficulty());
                assertEquals(0, model.getMoveCount());
                assertTrue(model.isGameRunning());
            }
        }
    }

    @Test
    void equalSizeDifficultyAndSeedReproduceTheSameStartingBoard() {
        for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
            GameModel first = DesktopGameFactory.create(3, difficulty, 9042L);
            GameModel second = DesktopGameFactory.create(3, difficulty, 9042L);

            assertEquals(difficulty, first.getDifficulty());
            assertEquals(difficulty, second.getDifficulty());
            assertGridEquals(first, second);
            GameModel sharedCoreModel = new GameModel(3);
            sharedCoreModel.scramble(difficulty, 9042L);
            assertGridEquals(first, sharedCoreModel);
        }
    }

    @Test
    void replayModelRestoresInitialGridAndDifficultyWithoutHistory() {
        GameModel source = DesktopGameFactory.create(4, PuzzleDifficulty.CHALLENGE, 71L);
        int[][] initial = source.getInitialGridCopy();
        int targetCol = source.getEmptyCol() == 0
                ? 1 : source.getEmptyCol() - 1;
        assertTrue(source.slideLineTo(source.getEmptyRow(), targetCol));

        GameModel replay = MainFrame.createReplayModel(source);

        assertEquals(PuzzleDifficulty.CHALLENGE, replay.getDifficulty());
        assertEquals(0, replay.getMoveCount());
        assertTrue(replay.isGameRunning());
        assertEquals(0L, replay.getElapsedTime());
        assertGridEquals(initial, replay);
        assertFalse(replay.canUndo());
        assertFalse(replay.canRedo());
    }

    @Test
    void timerRunsOnlyForFocusedUnblockedActiveGameplay() {
        assertTrue(DesktopTimerPolicy.shouldRun(true, true, false, false, true));
        assertFalse(DesktopTimerPolicy.shouldRun(false, true, false, false, true));
        assertFalse(DesktopTimerPolicy.shouldRun(true, false, false, false, true));
        assertFalse(DesktopTimerPolicy.shouldRun(true, true, true, false, true));
        assertFalse(DesktopTimerPolicy.shouldRun(true, true, false, true, true));
        assertFalse(DesktopTimerPolicy.shouldRun(true, true, false, false, false));
    }

    @Test
    void autosaveRunsOnlyForAStableVisiblePlayerSession() {
        assertTrue(DesktopAutosavePolicy.shouldAutosave(true, false, false));
        assertFalse(DesktopAutosavePolicy.shouldAutosave(false, false, false));
        assertFalse(DesktopAutosavePolicy.shouldAutosave(true, true, false));
        assertFalse(DesktopAutosavePolicy.shouldAutosave(true, false, true));
    }

    @Test
    void solverInputLockMakesTheDesktopBoardBusy() {
        GameModel model = DesktopGameFactory.create(3, PuzzleDifficulty.CLASSIC, 17L);
        BoardPanel board = new BoardPanel(model);

        board.setInputLocked(true);
        assertTrue(board.isBusy());
        board.setInputLocked(false);
        assertFalse(board.isBusy());
    }

    @Test
    void boardExposesFocusableAccessibleCellsForKeyboardPlay() {
        BoardPanel board = new BoardPanel(new GameModel(3));
        board.setSize(DesktopAdaptivePolicy.MINIMUM_WINDOW_WIDTH,
                DesktopAdaptivePolicy.MINIMUM_WINDOW_HEIGHT);
        board.doLayout();

        assertEquals(9, board.getAccessibleCellCount());
        assertEquals("Puzzle board", board.getAccessibleContext().getAccessibleName());
        assertTrue(board.getAccessibleCellForTesting(0, 0).isFocusable());
        assertEquals("Tile 1, row 1, column 1",
                board.getAccessibleCellForTesting(0, 0).getAccessibleContext().getAccessibleName());
        assertTrue(board.getAccessibleCellForTesting(0, 0).getWidth()
                >= DesktopAdaptivePolicy.MINIMUM_FOCUS_TARGET);
        assertEquals("Empty cell, row 3, column 3",
                board.getAccessibleCellForTesting(2, 2).getAccessibleContext().getAccessibleName());
        assertTrue(board.getAccessibleCellForTesting(2, 2).getAccessibleContext()
                .getAccessibleDescription().contains("Empty cell"));
    }

    @Test
    void everySupportedBoardSizeGetsACompleteAccessibleCellSurface() {
        for (int size : new int[] {3, 4, 5}) {
            BoardPanel board = new BoardPanel(new GameModel(size));
            assertEquals(size * size, board.getAccessibleCellCount());
        }
    }

    private static void assertGridEquals(GameModel expected, GameModel actual) {
        assertGridEquals(expected.getGridCopy(), actual);
    }

    private static void assertGridEquals(int[][] expected, GameModel actual) {
        assertEquals(expected.length, actual.getSize());
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], actual.getGridCopy()[row]);
        }
    }
}
