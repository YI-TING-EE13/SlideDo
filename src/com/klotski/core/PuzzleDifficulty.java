package com.klotski.core;

/**
 * Scramble-intensity preset for a SlideDo puzzle.
 * <p>
 * Difficulty changes how many valid moves are used to scramble a solved board;
 * it does not change movement rules or claim a guaranteed optimal solution
 * length. Stable IDs are used by save files and platform preferences.
 * </p>
 */
public enum PuzzleDifficulty {
    /** Shorter scramble for a lighter session. */
    RELAXED("relaxed", 3),

    /** Default scramble matching SlideDo's original behavior. */
    CLASSIC("classic", 5),

    /** Longer scramble for a more involved starting position. */
    CHALLENGE("challenge", 8);

    private final String id;
    private final int scrambleMultiplier;

    PuzzleDifficulty(String id, int scrambleMultiplier) {
        this.id = id;
        this.scrambleMultiplier = scrambleMultiplier;
    }

    /**
     * Returns the stable persistence identifier.
     *
     * @return lowercase stable ID
     */
    public String getId() {
        return id;
    }

    /**
     * Calculates the valid-move scramble budget for a board size.
     *
     * @param size square board size, at least 2
     * @return number of valid scramble moves
     * @throws IllegalArgumentException when the board size is unsupported
     */
    public int scrambleMovesForSize(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Puzzle size must be at least 2");
        }
        return Math.multiplyExact(Math.multiplyExact(size, size), scrambleMultiplier);
    }

    /**
     * Resolves a persisted ID, defaulting unknown values to {@link #CLASSIC}.
     *
     * @param id persisted ID, possibly {@code null}
     * @return matching preset or the backward-compatible default
     */
    public static PuzzleDifficulty fromId(String id) {
        if (id != null) {
            for (PuzzleDifficulty difficulty : values()) {
                if (difficulty.id.equalsIgnoreCase(id.trim())) {
                    return difficulty;
                }
            }
        }
        return CLASSIC;
    }
}
