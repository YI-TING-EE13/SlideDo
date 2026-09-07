package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
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

    @Test
    void boardAccessibilityCopyFollowsSelectedLocaleWithoutChangingCells() {
        BoardPanel board = new BoardPanel(new GameModel(3));
        board.setLocale(DesktopLocale.fromTag("zh-TW"));

        assertEquals("拼圖棋盤", board.getAccessibleContext().getAccessibleName());
        assertTrue(board.getAccessibleCellForTesting(0, 0)
                .getAccessibleContext().getAccessibleName().contains("方塊 1"));

        board.setLocale(DesktopLocale.fromTag("ja-JP"));
        assertEquals("パズル盤面", board.getAccessibleContext().getAccessibleName());
        assertTrue(board.getAccessibleCellForTesting(2, 2)
                .getAccessibleContext().getAccessibleName().contains("空白"));
    }

    @Test
    void mouseMoveDispatchedToAccessibleCellPreservesParentCursorAffordance() {
        GameModel model = activeModel();
        BoardPanel board = configuredBoard(model);
        Point tile = movableTile(model);
        JButton cell = board.getAccessibleCellForTesting(tile.x, tile.y);

        dispatchMouse(cell, MouseEvent.MOUSE_MOVED, centerOf(cell), 0, MouseEvent.NOBUTTON);

        assertEquals(Cursor.HAND_CURSOR, board.getCursor().getType());
    }

    @Test
    void mouseGestureDispatchedToAccessibleCellPerformsOnePressAction() {
        GameModel model = activeModel();
        BoardPanel board = configuredBoard(model);
        Point tile = movableTile(model);
        JButton cell = board.getAccessibleCellForTesting(tile.x, tile.y);
        Point center = centerOf(cell);
        int startingMoves = model.getMoveCount();

        dispatchMouse(cell, MouseEvent.MOUSE_PRESSED, center,
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        assertEquals(startingMoves + 1, model.getMoveCount(),
                "the existing press-triggered mouse contract must reach the parent path");

        Point dragged = new Point(center.x + 40, center.y);
        dispatchMouse(cell, MouseEvent.MOUSE_DRAGGED, dragged,
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_RELEASED, dragged, 0, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_CLICKED, dragged, 0, MouseEvent.BUTTON1);

        assertEquals(startingMoves + 1, model.getMoveCount(),
                "child mouse dispatch must not duplicate the existing action");
        assertEquals(1, model.getActionHistory().size());
        assertFalse(model.canRedo());
    }

    @Test
    void childMouseGestureReportsOneCompletionWithoutDuplicateCallback() {
        GameModel model = new GameModel(3);
        model.scramble(1);
        BoardPanel board = configuredBoard(model);
        int[] winDialogs = {0};
        board.setWinDialogHandler((parent, moves, timeMs) -> winDialogs[0]++);
        Point tile = solvingTile(model);
        JButton cell = board.getAccessibleCellForTesting(tile.x, tile.y);
        Point center = centerOf(cell);

        dispatchMouse(cell, MouseEvent.MOUSE_PRESSED, center,
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_RELEASED, center, 0, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_CLICKED, center, 0, MouseEvent.BUTTON1);

        assertTrue(model.isSolved());
        assertEquals(1, model.getMoveCount());
        assertEquals(1, model.getActionHistory().size());
        assertEquals(1, winDialogs[0]);
    }

    @Test
    void nonAlignedMouseGestureDispatchedToAccessibleCellRemainsNoOp() {
        GameModel model = activeModel();
        BoardPanel board = configuredBoard(model);
        Point tile = nonAlignedTile(model);
        JButton cell = board.getAccessibleCellForTesting(tile.x, tile.y);
        Point center = centerOf(cell);
        Point dragged = new Point(center.x + 40, center.y);
        int startingEmptyRow = model.getEmptyRow();
        int startingEmptyCol = model.getEmptyCol();

        dispatchMouse(cell, MouseEvent.MOUSE_PRESSED, center,
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_DRAGGED, dragged,
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_RELEASED, dragged, 0, MouseEvent.BUTTON1);
        dispatchMouse(cell, MouseEvent.MOUSE_CLICKED, dragged, 0, MouseEvent.BUTTON1);

        assertEquals(0, model.getMoveCount());
        assertEquals(startingEmptyRow, model.getEmptyRow());
        assertEquals(startingEmptyCol, model.getEmptyCol());
    }

    @Test
    void accessibleCellActionStillPerformsOneKeyboardStyleActivation() {
        GameModel model = activeModel();
        BoardPanel board = configuredBoard(model);
        Point tile = movableTile(model);

        board.getAccessibleCellForTesting(tile.x, tile.y).doClick();

        assertEquals(1, model.getMoveCount());
        assertEquals(tile.x, model.getEmptyRow());
        assertEquals(tile.y, model.getEmptyCol());
    }

    private static GameModel activeModel() {
        return DesktopGameFactory.create(3, PuzzleDifficulty.CLASSIC, 17L);
    }

    private static Point movableTile(GameModel model) {
        int row = model.getEmptyRow();
        int col = model.getEmptyCol();
        if (col > 0) {
            return new Point(row, col - 1);
        }
        if (col + 1 < model.getSize()) {
            return new Point(row, col + 1);
        }
        if (row > 0) {
            return new Point(row - 1, col);
        }
        return new Point(row + 1, col);
    }

    private static Point solvingTile(GameModel model) {
        if (model.getEmptyRow() < model.getSize() - 1) {
            return new Point(model.getEmptyRow() + 1, model.getEmptyCol());
        }
        return new Point(model.getEmptyRow(), model.getEmptyCol() + 1);
    }

    private static Point nonAlignedTile(GameModel model) {
        for (int row = 0; row < model.getSize(); row++) {
            for (int col = 0; col < model.getSize(); col++) {
                if (row != model.getEmptyRow() && col != model.getEmptyCol()
                        && model.getTile(row, col) != 0) {
                    return new Point(row, col);
                }
            }
        }
        throw new AssertionError("active test board must have a non-aligned tile");
    }

    private static BoardPanel configuredBoard(GameModel model) {
        BoardPanel board = new BoardPanel(model);
        board.setReducedMotion(true);
        board.setSize(DesktopAdaptivePolicy.MINIMUM_WINDOW_WIDTH,
                DesktopAdaptivePolicy.MINIMUM_WINDOW_HEIGHT);
        board.doLayout();
        return board;
    }

    private static Point centerOf(JButton cell) {
        return new Point(cell.getWidth() / 2, cell.getHeight() / 2);
    }

    private static void dispatchMouse(JButton cell, int id, Point point,
            int modifiersEx, int button) {
        cell.dispatchEvent(new MouseEvent(cell, id, 1L, modifiersEx,
                point.x, point.y, 1, false, button));
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
