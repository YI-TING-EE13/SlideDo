package com.klotski.core;

/**
 * Canonical reachability checks for square sliding-tile puzzles.
 * <p>
 * A board is solvable when it is a complete permutation of {@code 0..size^2-1}
 * and its inversion parity matches the solved row-major board. The empty tile
 * is excluded from the inversion count.
 * </p>
 */
public final class PuzzleSolvability {
    private PuzzleSolvability() {
    }

    /**
     * Reports whether a square grid is a valid, solvable sliding puzzle.
     *
     * @param grid candidate puzzle grid
     * @param size expected square board width and height
     * @return whether the board is a complete solvable permutation
     */
    public static boolean isSolvable(int[][] grid, int size) {
        if (size < 2 || grid == null || grid.length != size) {
            return false;
        }
        boolean[] seen = new boolean[size * size];
        int[] tiles = new int[size * size - 1];
        int tileCount = 0;
        int emptyRow = -1;
        for (int row = 0; row < size; row++) {
            if (grid[row] == null || grid[row].length != size) {
                return false;
            }
            for (int col = 0; col < size; col++) {
                int tile = grid[row][col];
                if (tile < 0 || tile >= seen.length || seen[tile]) {
                    return false;
                }
                seen[tile] = true;
                if (tile == 0) {
                    emptyRow = row;
                } else {
                    tiles[tileCount++] = tile;
                }
            }
        }
        for (boolean present : seen) {
            if (!present) {
                return false;
            }
        }

        int inversions = 0;
        for (int left = 0; left < tiles.length; left++) {
            for (int right = left + 1; right < tiles.length; right++) {
                if (tiles[left] > tiles[right]) {
                    inversions++;
                }
            }
        }
        if ((size & 1) == 1) {
            return (inversions & 1) == 0;
        }
        int emptyRowFromBottom = size - emptyRow;
        return ((inversions + emptyRowFromBottom) & 1) == 1;
    }
}
