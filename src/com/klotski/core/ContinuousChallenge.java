package com.klotski.core;

/**
 * Immutable aggregate for one continuous multi-puzzle challenge.
 * <p>
 * Individual puzzles keep their own move, time, assisted, and record boundary.
 * This aggregate stores only session progress and totals, so presentation code
 * can resume or summarize a challenge without merging separate puzzle records.
 * </p>
 */
public final class ContinuousChallenge {
    private final int targetPuzzles;
    private final int completedPuzzles;
    private final int totalMoves;
    private final long totalTimeMs;
    private final int assistedPuzzles;

    private ContinuousChallenge(int targetPuzzles, int completedPuzzles,
            int totalMoves, long totalTimeMs, int assistedPuzzles) {
        if (!isSupportedTarget(targetPuzzles)
                || completedPuzzles < 0 || completedPuzzles > targetPuzzles
                || totalMoves < 0 || totalTimeMs < 0
                || assistedPuzzles < 0 || assistedPuzzles > completedPuzzles) {
            throw new IllegalArgumentException("Invalid continuous challenge state");
        }
        this.targetPuzzles = targetPuzzles;
        this.completedPuzzles = completedPuzzles;
        this.totalMoves = totalMoves;
        this.totalTimeMs = totalTimeMs;
        this.assistedPuzzles = assistedPuzzles;
    }

    /**
     * Starts an empty challenge.
     *
     * @param targetPuzzles supported session length: 3, 5, or 10
     * @return empty challenge aggregate
     */
    public static ContinuousChallenge start(int targetPuzzles) {
        return new ContinuousChallenge(targetPuzzles, 0, 0, 0L, 0);
    }

    /**
     * Restores a previously validated session aggregate.
     *
     * @param targetPuzzles original session target
     * @param completedPuzzles completed puzzle count
     * @param totalMoves sum of final move counts
     * @param totalTimeMs sum of final active-play milliseconds
     * @param assistedPuzzles completed puzzles marked assisted
     * @return restored immutable challenge
     */
    public static ContinuousChallenge restore(int targetPuzzles, int completedPuzzles,
            int totalMoves, long totalTimeMs, int assistedPuzzles) {
        return new ContinuousChallenge(targetPuzzles, completedPuzzles,
                totalMoves, totalTimeMs, assistedPuzzles);
    }

    /**
     * Reports whether a session length is supported by the player flow.
     *
     * @param targetPuzzles candidate session length
     * @return {@code true} for 3, 5, or 10 puzzles
     */
    public static boolean isSupportedTarget(int targetPuzzles) {
        return targetPuzzles == 3 || targetPuzzles == 5 || targetPuzzles == 10;
    }

    /**
     * Returns a new aggregate containing one completed puzzle.
     *
     * @param moves final puzzle move count
     * @param timeMs final active-play milliseconds
     * @param assisted whether the puzzle used strategic or solver assistance
     * @return updated immutable aggregate
     * @throws IllegalStateException when the session is already complete
     */
    public ContinuousChallenge completePuzzle(int moves, long timeMs, boolean assisted) {
        if (isComplete()) {
            throw new IllegalStateException("Continuous challenge is already complete");
        }
        if (moves < 0 || timeMs < 0) {
            throw new IllegalArgumentException("Puzzle metrics cannot be negative");
        }
        return new ContinuousChallenge(targetPuzzles, completedPuzzles + 1,
                Math.addExact(totalMoves, moves), Math.addExact(totalTimeMs, timeMs),
                assistedPuzzles + (assisted ? 1 : 0));
    }

    /**
     * Returns the configured session puzzle target.
     *
     * @return configured puzzle count
     */
    public int getTargetPuzzles() {
        return targetPuzzles;
    }

    /**
     * Returns the number of completed puzzles.
     *
     * @return completed puzzle count
     */
    public int getCompletedPuzzles() {
        return completedPuzzles;
    }

    /**
     * Returns the one-based current puzzle number, capped at the target.
     *
     * @return current puzzle number
     */
    public int getCurrentPuzzleNumber() {
        return Math.min(targetPuzzles, completedPuzzles + 1);
    }

    /**
     * Returns the sum of final move counts.
     *
     * @return total moves
     */
    public int getTotalMoves() {
        return totalMoves;
    }

    /**
     * Returns the sum of final active-play milliseconds.
     *
     * @return total active-play time
     */
    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    /**
     * Returns the completed puzzles marked assisted.
     *
     * @return assisted puzzle count
     */
    public int getAssistedPuzzles() {
        return assistedPuzzles;
    }

    /**
     * Returns the completed puzzles without assistance.
     *
     * @return player puzzle count
     */
    public int getPlayerPuzzles() {
        return completedPuzzles - assistedPuzzles;
    }

    /**
     * Reports whether the configured number of puzzles is complete.
     *
     * @return {@code true} when the session is complete
     */
    public boolean isComplete() {
        return completedPuzzles == targetPuzzles;
    }
}
