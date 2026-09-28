package com.klotski.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable platform-neutral definition of one Offline Puzzle Relay challenge.
 * <p>
 * The target is a user-defined maximum number of player actions. It is not an
 * optimal-move count, a par value, or a solver-derived score.
 * </p>
 */
public final class RelayChallengeSpec {
    /** Current stable wire-format version. */
    public static final int FORMAT_VERSION = 1;
    /** Smallest supported board size. */
    public static final int MIN_SIZE = 3;
    /** Largest supported board size. */
    public static final int MAX_SIZE = 5;
    /** Smallest accepted user-defined move target. */
    public static final int MIN_TARGET_MOVES = 1;
    /** Largest accepted user-defined move target. */
    public static final int MAX_TARGET_MOVES = 9999;

    private final int size;
    private final PuzzleDifficulty difficulty;
    private final int[][] initialGrid;
    private final int targetMoves;

    /**
     * Creates an immutable Relay challenge definition.
     *
     * @param size supported square board size
     * @param difficulty stable puzzle difficulty metadata
     * @param initialGrid exact solvable starting board
     * @param targetMoves maximum player action count accepted by this challenge
     * @throws IllegalArgumentException when the size, target, or board is invalid
     * @throws NullPointerException when difficulty is null
     */
    public RelayChallengeSpec(int size, PuzzleDifficulty difficulty,
            int[][] initialGrid, int targetMoves) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("Relay board size must be from 3 through 5");
        }
        if (targetMoves < MIN_TARGET_MOVES || targetMoves > MAX_TARGET_MOVES) {
            throw new IllegalArgumentException("Relay target must be from 1 through 9999 moves");
        }
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        if (!PuzzleSolvability.isSolvable(initialGrid, size)) {
            throw new IllegalArgumentException("Relay board must be a valid solvable puzzle");
        }
        this.size = size;
        this.initialGrid = copyGrid(initialGrid);
        this.targetMoves = targetMoves;
    }

    /**
     * Returns the stable format version represented by this specification.
     *
     * @return the stable format version represented by this specification
     */
    public int getFormatVersion() {
        return FORMAT_VERSION;
    }

    /**
     * Returns the square board width and height.
     *
     * @return square board width and height
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns difficulty metadata retained from the source puzzle.
     *
     * @return difficulty metadata retained from the source puzzle
     */
    public PuzzleDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Returns a defensive copy of the exact initial board.
     *
     * @return defensive copy of the exact initial board
     */
    public int[][] getInitialGridCopy() {
        return copyGrid(initialGrid);
    }

    /**
     * Returns the user-defined maximum number of player moves.
     *
     * @return user-defined maximum number of player moves
     */
    public int getTargetMoves() {
        return targetMoves;
    }

    /**
     * Reports whether a completed player-action count satisfies this challenge.
     *
     * @param playerMoves nonnegative completed action count
     * @return whether the player used at most the configured target
     */
    public boolean isTargetMet(int playerMoves) {
        return playerMoves >= 0 && playerMoves <= targetMoves;
    }

    /**
     * Creates a fresh game at this exact starting board.
     *
     * @return zero-move game model with this challenge's initial grid
     */
    public GameModel createGame() {
        return new PuzzleIdentity(size, difficulty, initialGrid).createGame();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RelayChallengeSpec that)) {
            return false;
        }
        return size == that.size
                && targetMoves == that.targetMoves
                && difficulty == that.difficulty
                && Arrays.deepEquals(initialGrid, that.initialGrid);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(FORMAT_VERSION, size, difficulty, targetMoves);
        return 31 * result + Arrays.deepHashCode(initialGrid);
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }
}
