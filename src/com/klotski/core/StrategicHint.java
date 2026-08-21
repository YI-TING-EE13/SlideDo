package com.klotski.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Chooses a deterministic legal move that is likely to improve the puzzle.
 *
 * <p>The search uses a small fixed-depth lookahead and Manhattan distance. It
 * never changes the supplied {@link GameModel}, so presentation layers can
 * show the suggestion before the player decides whether to use it.</p>
 */
public final class StrategicHint {
    private static final int SEARCH_DEPTH = 4;

    private StrategicHint() {
    }

    /**
     * Chooses one adjacent tile for the player to move into the empty cell.
     *
     * @param model current puzzle model
     * @return a legal deterministic suggestion, or {@code null} when no hint
     *         is available
     */
    public static Hint choose(GameModel model) {
        if (model == null || model.isSolved()) {
            return null;
        }

        int[][] grid = model.getGridCopy();
        int[] empty = findEmpty(grid);
        Hint bestHint = null;
        int bestScore = Integer.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            int row = empty[0] + direction.dRow;
            int col = empty[1] + direction.dCol;
            if (!isInside(grid.length, row, col)) {
                continue;
            }

            int tile = grid[row][col];
            swap(grid, empty[0], empty[1], row, col);
            Set<String> visited = new HashSet<>();
            visited.add(key(grid));
            int score = bestReachableScore(
                    grid, row, col, SEARCH_DEPTH - 1,
                    direction.opposite(), visited);
            swap(grid, empty[0], empty[1], row, col);

            if (score < bestScore) {
                bestScore = score;
                bestHint = new Hint(direction, row, col, tile);
            }
        }
        return bestHint;
    }

    private static int bestReachableScore(int[][] grid, int emptyRow,
            int emptyCol, int remainingDepth, Direction blockedDirection,
            Set<String> visited) {
        int best = manhattanDistance(grid);
        if (best == 0 || remainingDepth == 0) {
            return best;
        }

        for (Direction direction : Direction.values()) {
            if (direction == blockedDirection) {
                continue;
            }
            int row = emptyRow + direction.dRow;
            int col = emptyCol + direction.dCol;
            if (!isInside(grid.length, row, col)) {
                continue;
            }

            swap(grid, emptyRow, emptyCol, row, col);
            String stateKey = key(grid);
            if (visited.add(stateKey)) {
                best = Math.min(best, bestReachableScore(
                        grid, row, col, remainingDepth - 1,
                        direction.opposite(), visited));
                visited.remove(stateKey);
            }
            swap(grid, emptyRow, emptyCol, row, col);
        }
        return best;
    }

    private static int manhattanDistance(int[][] grid) {
        int distance = 0;
        int size = grid.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int tile = grid[row][col];
                if (tile == 0) {
                    continue;
                }
                int goalRow = (tile - 1) / size;
                int goalCol = (tile - 1) % size;
                distance += Math.abs(row - goalRow) + Math.abs(col - goalCol);
            }
        }
        return distance;
    }

    private static int[] findEmpty(int[][] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid.length; col++) {
                if (grid[row][col] == 0) {
                    return new int[] {row, col};
                }
            }
        }
        throw new IllegalStateException("Puzzle grid has no empty cell");
    }

    private static boolean isInside(int size, int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    private static void swap(int[][] grid, int rowA, int colA,
            int rowB, int colB) {
        int value = grid[rowA][colA];
        grid[rowA][colA] = grid[rowB][colB];
        grid[rowB][colB] = value;
    }

    private static String key(int[][] grid) {
        StringBuilder result = new StringBuilder(grid.length * grid.length * 3);
        for (int[] row : grid) {
            for (int value : row) {
                result.append(value).append(',');
            }
        }
        return result.toString();
    }

    /** Immutable strategic-move suggestion. */
    public static final class Hint {
        private final Direction direction;
        private final int row;
        private final int col;
        private final int tile;

        private Hint(Direction direction, int row, int col, int tile) {
            this.direction = direction;
            this.row = row;
            this.col = col;
            this.tile = tile;
        }

        /**
         * Returns the suggested direction for the empty cell.
         *
         * @return direction in which the empty cell should move
         */
        public Direction getDirection() {
            return direction;
        }

        /**
         * Returns the row containing the suggested tile.
         *
         * @return zero-based row of the suggested tile
         */
        public int getRow() {
            return row;
        }

        /**
         * Returns the column containing the suggested tile.
         *
         * @return zero-based column of the suggested tile
         */
        public int getCol() {
            return col;
        }

        /**
         * Returns the number shown on the suggested tile.
         *
         * @return number shown on the suggested tile
         */
        public int getTile() {
            return tile;
        }
    }
}
