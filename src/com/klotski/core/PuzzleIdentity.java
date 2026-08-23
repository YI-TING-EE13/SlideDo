package com.klotski.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Immutable identity for one exact SlideDo starting puzzle.
 * <p>
 * Size, difficulty, and every starting-grid value participate in the stable
 * identifier. Platforms can therefore deduplicate local favorites without
 * depending on display labels or mutable play progress.
 * </p>
 */
public final class PuzzleIdentity {
    private final int size;
    private final PuzzleDifficulty difficulty;
    private final int[][] initialGrid;
    private final String id;

    /**
     * Creates an identity for an exact starting board.
     *
     * @param size square board width and height
     * @param difficulty scramble-intensity label stored with the puzzle
     * @param initialGrid exact starting-grid values
     * @throws IllegalArgumentException when the grid is not a square permutation
     */
    public PuzzleIdentity(int size, PuzzleDifficulty difficulty, int[][] initialGrid) {
        this.size = size;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.initialGrid = validatedCopy(size, initialGrid);
        this.id = sha256(canonicalValue());
    }

    /**
     * Creates an identity from a model's immutable starting state.
     *
     * @param model active or completed puzzle model
     * @return exact identity suitable for a favorite library
     */
    public static PuzzleIdentity from(GameModel model) {
        Objects.requireNonNull(model, "model");
        return new PuzzleIdentity(
                model.getSize(), model.getDifficulty(), model.getInitialGridCopy());
    }

    /**
     * Returns the stable identity used by platform persistence.
     *
     * @return stable lowercase SHA-256 identity
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the square board size.
     *
     * @return square board width and height
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the difficulty stored as part of the identity.
     *
     * @return stored scramble-intensity label
     */
    public PuzzleDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Copies the exact starting board retained by this identity.
     *
     * @return defensive copy of the exact starting grid
     */
    public int[][] getInitialGridCopy() {
        return copyGrid(initialGrid);
    }

    /**
     * Creates a fresh, playable model at this exact starting board.
     *
     * @return model with zero moves and elapsed time
     */
    public GameModel createGame() {
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = getInitialGridCopy();
        data.initialGrid = getInitialGridCopy();
        data.moveCount = 0;
        data.elapsedTime = 0L;
        data.updatedAt = 1L;
        data.active = true;
        data.solved = false;
        data.difficulty = difficulty;

        GameModel model = new GameModel(size);
        model.loadState(data);
        // The platform controller starts timing when the game screen is visible.
        model.pauseTimer();
        return model;
    }

    private String canonicalValue() {
        StringBuilder value = new StringBuilder();
        value.append(size).append('|').append(difficulty.getId());
        for (int[] row : initialGrid) {
            for (int tile : row) {
                value.append('|').append(tile);
            }
        }
        return value.toString();
    }

    private static int[][] validatedCopy(int size, int[][] grid) {
        if (size < 2 || grid == null || grid.length != size) {
            throw new IllegalArgumentException("Favorite puzzle must use a square grid");
        }
        boolean[] seen = new boolean[size * size];
        int[][] copy = new int[size][size];
        for (int row = 0; row < size; row++) {
            if (grid[row] == null || grid[row].length != size) {
                throw new IllegalArgumentException("Favorite puzzle must use a square grid");
            }
            for (int col = 0; col < size; col++) {
                int tile = grid[row][col];
                if (tile < 0 || tile >= seen.length || seen[tile]) {
                    throw new IllegalArgumentException(
                            "Favorite puzzle must contain each tile exactly once");
                }
                seen[tile] = true;
                copy[row][col] = tile;
            }
        }
        return copy;
    }

    private static int[][] copyGrid(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
