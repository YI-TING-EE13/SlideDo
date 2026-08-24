package com.klotski.core;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Platform-independent state container and rules engine for Number Klotski.
 * <p>
 * The model owns the board, move count, active-play timer, action history,
 * undo/redo stacks, and win detection. It emits observer callbacks but contains no Swing or Android code,
 * which keeps the same rules usable by both desktop and mobile front ends.
 * </p>
 */
public class GameModel {
    private int[][] grid;
    private int size; // e.g., 4 for a 4x4 grid
    private int emptyRow;
    private int emptyCol;
    private boolean isSolved;
    private int moveCount;
    private long elapsedTimeMs;
    private long timerStartedAtMs;
    private boolean timerRunning;
    private boolean isGameRunning;
    private PuzzleDifficulty difficulty = PuzzleDifficulty.CLASSIC;
    private int[][] initialGrid;
    private final Deque<ActionSnapshot> undoStack = new ArrayDeque<>();
    private final Deque<ActionSnapshot> redoStack = new ArrayDeque<>();

    private final List<GameObserver> observers = new ArrayList<>();
    private final Random random = new Random();
    private final LongSupplier timeSource;

    /**
     * Creates a model for a square puzzle.
     *
     * @param size board width and height
     */
    public GameModel(int size) {
        this(size, System::currentTimeMillis);
    }

    GameModel(int size, LongSupplier timeSource) {
        this.size = size;
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        reset();
    }

    /**
     * Resets the board to the solved state and stops active gameplay.
     */
    public void reset() {
        grid = new int[size][size];
        int value = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = value;
                value++;
            }
        }
        // The last cell is empty (represented by 0)
        grid[size - 1][size - 1] = 0;
        emptyRow = size - 1;
        emptyCol = size - 1;

        isSolved = true;
        moveCount = 0;
        isGameRunning = false;
        resetTimer(false, 0L);
        initialGrid = copyGrid(grid);
        undoStack.clear();
        redoStack.clear();
        notifyGridChanged();
    }

    /**
     * Scrambles the board by applying random valid moves from the solved state.
     * <p>
     * This guarantees a solvable puzzle because every generated board is
     * reachable from the solved position.
     * </p>
     * 
     * @param moves number of random moves to apply
     */
    public void scramble(int moves) {
        scramble(moves, random, PuzzleDifficulty.CLASSIC);
    }

    /**
     * Scrambles the board using a named intensity preset.
     *
     * @param difficulty scramble-intensity preset
     */
    public void scramble(PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = Objects.requireNonNull(difficulty, "difficulty");
        scramble(selected.scrambleMovesForSize(size), random, selected);
    }

    /**
     * Scrambles the board reproducibly using a named intensity preset.
     * Equal sizes, difficulties, and seeds produce equal starting grids.
     *
     * @param difficulty scramble-intensity preset
     * @param seed deterministic random seed
     */
    public void scramble(PuzzleDifficulty difficulty, long seed) {
        PuzzleDifficulty selected = Objects.requireNonNull(difficulty, "difficulty");
        scramble(selected.scrambleMovesForSize(size), new Random(seed), selected);
    }

    private void scramble(int moves, Random scrambleRandom, PuzzleDifficulty selectedDifficulty) {
        Direction lastDir = null;
        for (int i = 0; i < moves; i++) {
            List<Direction> validMoves = getValidMoves();
            // Try not to undo the move we just made to make scrambling more effective
            if (lastDir != null && validMoves.size() > 1) {
                validMoves.remove(lastDir.opposite());
            }

            Direction dir = validMoves.get(scrambleRandom.nextInt(validMoves.size()));
            moveInternal(dir, false, false, false, false);
            lastDir = dir;
        }
        if (moves > 0 && isSolvedGrid()) {
            List<Direction> validMoves = getValidMoves();
            if (lastDir != null && validMoves.size() > 1) {
                validMoves.remove(lastDir.opposite());
            }
            Direction dir = validMoves.get(scrambleRandom.nextInt(validMoves.size()));
            moveInternal(dir, false, false, false, false);
        }
        moveCount = 0; // Reset moves after scramble
        isGameRunning = true;
        resetTimer(true, 0L);
        isSolved = false;
        difficulty = selectedDifficulty;
        initialGrid = copyGrid(grid);
        undoStack.clear();
        redoStack.clear();
        notifyGridChanged();
    }

    /**
     * Moves the empty tile in the specified direction.
     * <p>
     * Direction is expressed from the empty tile's perspective. For example,
     * {@link Direction#UP} means the empty tile moves upward and the numbered
     * tile above it visually moves downward.
     * </p>
     * 
     * @param dir direction to move the empty tile
     * @return {@code true} when the move is valid and was executed
     */
    public boolean move(Direction dir) {
        return moveInternal(dir, true, true, true, true);
    }

    /**
     * Slides every tile between the empty cell and the selected cell in one
     * logical user action.
     * <p>
     * The selected cell must be in the same row or column as the empty cell.
     * The whole line shift increments the move counter once and pushes one undo
     * snapshot, which matches the desktop and mobile tap interaction.
     * </p>
     *
     * @param row selected tile row
     * @param col selected tile column
     * @return {@code true} when a line slide was performed
     */
    public boolean slideLineTo(int row, int col) {
        if (!isGameRunning || !isValid(row, col) || (row == emptyRow && col == emptyCol)) {
            return false;
        }

        Direction dir;
        int steps;
        if (row == emptyRow) {
            dir = col < emptyCol ? Direction.LEFT : Direction.RIGHT;
            steps = Math.abs(col - emptyCol);
        } else if (col == emptyCol) {
            dir = row < emptyRow ? Direction.UP : Direction.DOWN;
            steps = Math.abs(row - emptyRow);
        } else {
            return false;
        }

        int[][] beforeGrid = copyGrid(grid);
        for (int i = 0; i < steps; i++) {
            moveInternal(dir, false, false, false, false);
        }

        recordAction(beforeGrid, dir, steps);
        moveCount++;
        notifyLineMove(dir, steps);
        notifyGridChanged();
        checkWin();
        return true;
    }

    private boolean moveInternal(Direction dir, boolean notifyMoveEvent, boolean notifyGridEvent, boolean countRunningMove, boolean pushUndo) {
        int newRow = emptyRow + dir.dRow;
        int newCol = emptyCol + dir.dCol;

        if (isValid(newRow, newCol)) {
            boolean shouldCheckWin = isGameRunning && countRunningMove;
            int[][] beforeGrid = shouldCheckWin && pushUndo ? copyGrid(grid) : null;

            // Swap
            grid[emptyRow][emptyCol] = grid[newRow][newCol];
            grid[newRow][newCol] = 0;

            emptyRow = newRow;
            emptyCol = newCol;

            if (shouldCheckWin) {
                moveCount++;
                if (pushUndo) {
                    recordAction(beforeGrid, dir, 1);
                }
            }

            if (notifyMoveEvent) {
                notifyMove(dir);
            }
            if (notifyGridEvent) {
                notifyGridChanged();
            }
            if (shouldCheckWin) {
                checkWin();
            }
            return true;
        }
        return false;
    }

    /**
     * Restores the previous board snapshot, if one is available.
     *
     * @return {@code true} when a previous move was undone
     */
    public boolean undo() {
        if (undoStack.isEmpty() || !isGameRunning) {
            return false;
        }

        ActionSnapshot action = undoStack.pop();
        grid = copyGrid(action.beforeGrid);
        redoStack.push(action);
        findEmptyTile();
        if (moveCount > 0) {
            moveCount--;
        }
        isSolved = false;
        notifyGridChanged();
        return true;
    }

    /**
     * Checks whether the current game can be undone.
     *
     * @return {@code true} when an undo snapshot exists during active gameplay
     */
    public boolean canUndo() {
        return !undoStack.isEmpty() && isGameRunning;
    }

    /**
     * Reapplies the most recently undone action as one counted move.
     *
     * @return {@code true} when an undone action was restored
     */
    public boolean redo() {
        if (redoStack.isEmpty() || !isGameRunning) {
            return false;
        }

        ActionSnapshot action = redoStack.pop();
        grid = copyGrid(action.afterGrid);
        findEmptyTile();
        undoStack.push(action);
        moveCount++;
        if (action.action.getSteps() == 1) {
            notifyMove(action.action.getDirection());
        } else {
            notifyLineMove(action.action.getDirection(), action.action.getSteps());
        }
        notifyGridChanged();
        checkWin();
        return true;
    }

    /**
     * Checks whether the current game has an undone action available.
     *
     * @return {@code true} when Redo can reapply one action
     */
    public boolean canRedo() {
        return !redoStack.isEmpty() && isGameRunning;
    }

    /**
     * Returns completed actions in oldest-first order.
     *
     * @return defensive action-history list
     */
    public List<MoveAction> getActionHistory() {
        List<MoveAction> history = new ArrayList<>();
        for (ActionSnapshot action : undoStack) {
            history.add(action.action);
        }
        Collections.reverse(history);
        return history;
    }

    /**
     * Returns undone actions in next-redo-first order.
     *
     * @return defensive redo-history list
     */
    public List<MoveAction> getRedoHistory() {
        List<MoveAction> history = new ArrayList<>();
        for (ActionSnapshot action : redoStack) {
            history.add(action.action);
        }
        return history;
    }

    /**
     * Encodes the completed action history for persistence.
     *
     * @return compact persisted form of the oldest-first completed actions
     */
    public String getEncodedActionHistory() {
        return encodeActions(getActionHistory());
    }

    /**
     * Encodes the redo action history for persistence.
     *
     * @return compact persisted form of the next-redo-first undone actions
     */
    public String getEncodedRedoHistory() {
        return encodeActions(getRedoHistory());
    }

    /**
     * Restarts the current puzzle from the post-scramble starting grid.
     */
    public void restartCurrentGame() {
        if (initialGrid == null) {
            reset();
            return;
        }

        grid = copyGrid(initialGrid);
        findEmptyTile();
        moveCount = 0;
        isSolved = isSolvedGrid();
        isGameRunning = !isSolved;
        resetTimer(isGameRunning, 0L);
        undoStack.clear();
        redoStack.clear();
        notifyGridChanged();
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    private List<Direction> getValidMoves() {
        List<Direction> moves = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (isValid(emptyRow + dir.dRow, emptyCol + dir.dCol)) {
                moves.add(dir);
            }
        }
        return moves;
    }

    private void findEmptyTile() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (grid[r][c] == 0) {
                    emptyRow = r;
                    emptyCol = c;
                    return;
                }
            }
        }
    }

    private int[][] copyGrid(int[][] source) {
        int[][] copy = new int[source.length][source.length];
        for (int i = 0; i < source.length; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, source.length);
        }
        return copy;
    }

    private boolean isSolvedGrid() {
        int value = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == size - 1 && c == size - 1) {
                    if (grid[r][c] != 0) {
                        return false;
                    }
                } else if (grid[r][c] != value++) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkWin() {
        int value = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                // Last cell should be 0
                if (r == size - 1 && c == size - 1) {
                    if (grid[r][c] != 0)
                        return;
                } else {
                    if (grid[r][c] != value)
                        return;
                    value++;
                }
            }
        }

        pauseTimer();
        isSolved = true;
        isGameRunning = false;
        long timeTaken = getElapsedTime();
        notifyGameWon(timeTaken);
    }

    // --- Observer Pattern ---

    /**
     * Registers an observer for model events.
     *
     * @param observer observer to notify
     */
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes a previously registered observer.
     *
     * @param observer observer to remove
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyMove(Direction dir) {
        for (GameObserver obs : observers) {
            obs.onMove(dir);
        }
    }

    private void notifyLineMove(Direction dir, int steps) {
        for (GameObserver obs : observers) {
            obs.onLineMove(dir, steps);
        }
    }

    private void notifyGridChanged() {
        for (GameObserver obs : observers) {
            obs.onGridChanged();
        }
    }

    private void notifyGameWon(long timeTaken) {
        for (GameObserver obs : observers) {
            obs.onGameWon(moveCount, timeTaken);
        }
    }

    // --- Getters ---

    /**
     * Returns the board width and height.
     *
     * @return square board size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the tile value at a grid coordinate.
     *
     * @param r row index
     * @param c column index
     * @return tile value, with {@code 0} representing the empty cell
     */
    public int getTile(int r, int c) {
        return grid[r][c];
    }

    /**
     * Returns the current empty tile row.
     *
     * @return empty row index
     */
    public int getEmptyRow() {
        return emptyRow;
    }

    /**
     * Returns the current empty tile column.
     *
     * @return empty column index
     */
    public int getEmptyCol() {
        return emptyCol;
    }

    /**
     * Returns the current move count.
     *
     * @return number of counted user moves
     */
    public int getMoveCount() {
        return moveCount;
    }

    /**
     * Checks whether the board is in solved order.
     *
     * @return {@code true} when the puzzle is solved
     */
    public boolean isSolved() {
        return isSolved;
    }

    /**
     * Checks whether an active puzzle is running.
     *
     * @return {@code true} after scramble/load and before the puzzle is solved
     */
    public boolean isGameRunning() {
        return isGameRunning;
    }

    /**
     * Returns the scramble preset associated with the current puzzle.
     *
     * @return current difficulty, defaulting to Classic for legacy state
     */
    public PuzzleDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Returns the effective timer anchor used by legacy timer consumers.
     * While paused, the anchor advances so subtracting it from the current time
     * continues to equal active elapsed time.
     *
     * @return effective start timestamp in milliseconds since epoch
     */
    public long getStartTime() {
        return timeSource.getAsLong() - getElapsedTime();
    }

    /**
     * Returns elapsed active-play time for the current game.
     *
     * @return elapsed time in milliseconds
     */
    public long getElapsedTime() {
        if (timerRunning) {
            return elapsedTimeMs + Math.max(0L, timeSource.getAsLong() - timerStartedAtMs);
        }
        return elapsedTimeMs;
    }

    /**
     * Stops active-play time accumulation without changing the puzzle state.
     * Repeated calls are safe.
     */
    public void pauseTimer() {
        if (!timerRunning) {
            return;
        }
        elapsedTimeMs = getElapsedTime();
        timerRunning = false;
    }

    /**
     * Resumes active-play time accumulation for an unsolved running puzzle.
     * Repeated calls are safe.
     */
    public void resumeTimer() {
        if (!isGameRunning || isSolved || timerRunning) {
            return;
        }
        timerStartedAtMs = timeSource.getAsLong();
        timerRunning = true;
    }

    /**
     * Checks whether active-play time is currently accumulating.
     *
     * @return {@code true} while the game timer is running
     */
    public boolean isTimerRunning() {
        return timerRunning;
    }

    private void resetTimer(boolean running, long elapsedMs) {
        elapsedTimeMs = Math.max(0L, elapsedMs);
        timerStartedAtMs = timeSource.getAsLong();
        timerRunning = running;
    }

    /**
     * Loads a saved game state from a grid and move count.
     *
     * @param savedGrid persisted grid values
     * @param savedMoveCount persisted move count
     */
    public void loadState(int[][] savedGrid, int savedMoveCount) {
        this.size = savedGrid.length;
        this.grid = copyGrid(savedGrid);
        this.moveCount = savedMoveCount;

        // Find empty tile
        findEmptyTile();

        this.isGameRunning = true;
        this.isSolved = isSolvedGrid();
        this.isGameRunning = !isSolved;
        resetTimer(isGameRunning, 0L);
        this.initialGrid = copyGrid(savedGrid);
        this.undoStack.clear();
        this.redoStack.clear();
        notifyGridChanged();
    }

    /**
     * Loads a saved game state from a SaveData object.
     *
     * @param data parsed save payload
     */
    public void loadState(SaveManager.SaveData data) {
        this.size = data.grid.length;
        this.grid = copyGrid(data.grid);
        this.moveCount = data.moveCount;
        findEmptyTile();
        this.isSolved = data.solved || isSolvedGrid();
        this.isGameRunning = !isSolved && (data.active || data.updatedAt == 0);
        this.difficulty = data.difficulty == null ? PuzzleDifficulty.CLASSIC : data.difficulty;
        resetTimer(isGameRunning, data.elapsedTime);
        if (data.initialGrid != null) {
            this.initialGrid = copyGrid(data.initialGrid);
        } else {
            this.initialGrid = copyGrid(data.grid);
        }
        this.undoStack.clear();
        this.redoStack.clear();
        restoreActionHistory(data.actionHistory, data.redoHistory);
        notifyGridChanged();
    }

    /**
     * Returns a defensive copy of the current grid.
     *
     * @return a square grid copy suitable for solvers and save operations
     */
    public int[][] getGridCopy() {
        return copyGrid(grid);
    }

    /**
     * Returns a defensive copy of the current puzzle's starting grid.
     *
     * @return the grid used by restart, or the solved grid after reset
     */
    public int[][] getInitialGridCopy() {
        return copyGrid(initialGrid);
    }

    private void recordAction(int[][] beforeGrid, Direction direction, int steps) {
        undoStack.push(new ActionSnapshot(beforeGrid, copyGrid(grid),
                new MoveAction(direction, steps)));
        redoStack.clear();
    }

    private void restoreActionHistory(String encodedHistory, String encodedRedo) {
        if ((encodedHistory == null || encodedHistory.isEmpty())
                && (encodedRedo == null || encodedRedo.isEmpty())) {
            return;
        }
        try {
            List<MoveAction> history = decodeActions(encodedHistory);
            if (history.size() != moveCount) {
                return;
            }
            int[][] reconstructed = copyGrid(initialGrid);
            List<ActionSnapshot> restoredUndo = new ArrayList<>();
            for (MoveAction action : history) {
                int[][] before = copyGrid(reconstructed);
                reconstructed = applyAction(reconstructed, action);
                restoredUndo.add(new ActionSnapshot(before, copyGrid(reconstructed), action));
            }
            if (!java.util.Arrays.deepEquals(reconstructed, grid)) {
                return;
            }

            List<ActionSnapshot> restoredRedo = new ArrayList<>();
            for (MoveAction action : decodeActions(encodedRedo)) {
                int[][] before = copyGrid(reconstructed);
                reconstructed = applyAction(reconstructed, action);
                restoredRedo.add(new ActionSnapshot(before, copyGrid(reconstructed), action));
            }
            for (ActionSnapshot action : restoredUndo) {
                undoStack.push(action);
            }
            redoStack.addAll(restoredRedo);
        } catch (RuntimeException exception) {
            undoStack.clear();
            redoStack.clear();
        }
    }

    private int[][] applyAction(int[][] source, MoveAction action) {
        int[][] result = copyGrid(source);
        int emptyR = -1;
        int emptyC = -1;
        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                if (result[row][col] == 0) {
                    emptyR = row;
                    emptyC = col;
                }
            }
        }
        for (int step = 0; step < action.getSteps(); step++) {
            int nextRow = emptyR + action.getDirection().dRow;
            int nextCol = emptyC + action.getDirection().dCol;
            if (!isValid(nextRow, nextCol)) {
                throw new IllegalArgumentException("Move history leaves the board");
            }
            result[emptyR][emptyC] = result[nextRow][nextCol];
            result[nextRow][nextCol] = 0;
            emptyR = nextRow;
            emptyC = nextCol;
        }
        return result;
    }

    private static String encodeActions(List<MoveAction> actions) {
        StringBuilder encoded = new StringBuilder();
        for (MoveAction action : actions) {
            if (encoded.length() > 0) {
                encoded.append(',');
            }
            encoded.append(action.getDirection().name().charAt(0))
                    .append(action.getSteps());
        }
        return encoded.toString();
    }

    private static List<MoveAction> decodeActions(String encoded) {
        List<MoveAction> actions = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return actions;
        }
        for (String token : encoded.split(",")) {
            if (token.length() < 2) {
                throw new IllegalArgumentException("Invalid move history token");
            }
            Direction direction = switch (token.charAt(0)) {
                case 'U' -> Direction.UP;
                case 'D' -> Direction.DOWN;
                case 'L' -> Direction.LEFT;
                case 'R' -> Direction.RIGHT;
                default -> throw new IllegalArgumentException("Invalid move history direction");
            };
            actions.add(new MoveAction(direction, Integer.parseInt(token.substring(1))));
        }
        return actions;
    }

    private static final class ActionSnapshot {
        private final int[][] beforeGrid;
        private final int[][] afterGrid;
        private final MoveAction action;

        private ActionSnapshot(int[][] beforeGrid, int[][] afterGrid, MoveAction action) {
            this.beforeGrid = beforeGrid;
            this.afterGrid = afterGrid;
            this.action = action;
        }
    }
}
