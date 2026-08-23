package com.klotski.core;

import java.util.Collections;
import java.util.List;

/**
 * Comparable recent-play summary for one puzzle size and difficulty.
 * <p>
 * Samples are supplied newest first. At least six samples are required before
 * the class compares equally sized recent and previous windows. Each window
 * contains at most five samples, which keeps the result focused on current
 * personal play rather than mixing incompatible long-term eras.
 * </p>
 */
public final class PersonalTrend {
    private static final int MAX_WINDOW_SIZE = 5;
    private static final int MIN_COMPARABLE_SAMPLES = 6;
    private static final int STEADY_PERCENT_THRESHOLD = 5;

    /** Qualitative direction for a metric where lower values are better. */
    public enum Direction {
        /** Recent average is at least five percent lower. */
        IMPROVING,
        /** Recent average changed by less than five percent. */
        STEADY,
        /** Recent average is at least five percent higher. */
        DECLINING,
        /** Fewer than three recent and three previous samples are available. */
        NOT_ENOUGH_DATA
    }

    private final int recentCount;
    private final int previousCount;
    private final long recentAverageMoves;
    private final long previousAverageMoves;
    private final long recentAverageTimeMs;
    private final long previousAverageTimeMs;
    private final int moveChangePercent;
    private final int timeChangePercent;
    private final Direction moveDirection;
    private final Direction timeDirection;

    private PersonalTrend(int recentCount, int previousCount,
            long recentAverageMoves, long previousAverageMoves,
            long recentAverageTimeMs, long previousAverageTimeMs,
            int moveChangePercent, int timeChangePercent,
            Direction moveDirection, Direction timeDirection) {
        this.recentCount = recentCount;
        this.previousCount = previousCount;
        this.recentAverageMoves = recentAverageMoves;
        this.previousAverageMoves = previousAverageMoves;
        this.recentAverageTimeMs = recentAverageTimeMs;
        this.previousAverageTimeMs = previousAverageTimeMs;
        this.moveChangePercent = moveChangePercent;
        this.timeChangePercent = timeChangePercent;
        this.moveDirection = moveDirection;
        this.timeDirection = timeDirection;
    }

    /**
     * Summarizes samples for one already-filtered size and difficulty scope.
     *
     * @param newestFirst player-completed samples in newest-first order
     * @return immutable recent average and comparable trend summary
     */
    public static PersonalTrend summarize(List<Sample> newestFirst) {
        List<Sample> samples = newestFirst == null ? Collections.emptyList() : newestFirst;
        int recentOnlyCount = Math.min(MAX_WINDOW_SIZE, samples.size());
        long recentOnlyMoves = averageMoves(samples, 0, recentOnlyCount);
        long recentOnlyTime = averageTime(samples, 0, recentOnlyCount);
        if (samples.size() < MIN_COMPARABLE_SAMPLES) {
            return new PersonalTrend(recentOnlyCount, 0, recentOnlyMoves, 0L,
                    recentOnlyTime, 0L, 0, 0,
                    Direction.NOT_ENOUGH_DATA, Direction.NOT_ENOUGH_DATA);
        }

        int windowSize = Math.min(MAX_WINDOW_SIZE, samples.size() / 2);
        long recentMoves = averageMoves(samples, 0, windowSize);
        long previousMoves = averageMoves(samples, windowSize, windowSize);
        long recentTime = averageTime(samples, 0, windowSize);
        long previousTime = averageTime(samples, windowSize, windowSize);
        int movesPercent = percentChange(recentMoves, previousMoves);
        int timePercent = percentChange(recentTime, previousTime);
        return new PersonalTrend(windowSize, windowSize, recentMoves, previousMoves,
                recentTime, previousTime, movesPercent, timePercent,
                direction(movesPercent), direction(timePercent));
    }

    /**
     * Returns the number of samples contributing to the recent average.
     *
     * @return recent sample count
     */
    public int getRecentCount() {
        return recentCount;
    }

    /**
     * Returns the number of samples in the comparison window.
     *
     * @return previous sample count
     */
    public int getPreviousCount() {
        return previousCount;
    }

    /**
     * Returns the rounded recent average move count.
     *
     * @return recent average moves
     */
    public long getRecentAverageMoves() {
        return recentAverageMoves;
    }

    /**
     * Returns the rounded previous average move count.
     *
     * @return previous average moves
     */
    public long getPreviousAverageMoves() {
        return previousAverageMoves;
    }

    /**
     * Returns the rounded recent average elapsed milliseconds.
     *
     * @return recent average elapsed time
     */
    public long getRecentAverageTimeMs() {
        return recentAverageTimeMs;
    }

    /**
     * Returns the rounded previous average elapsed milliseconds.
     *
     * @return previous average elapsed time
     */
    public long getPreviousAverageTimeMs() {
        return previousAverageTimeMs;
    }

    /**
     * Returns the signed recent-versus-previous move change percentage.
     *
     * @return move change percentage
     */
    public int getMoveChangePercent() {
        return moveChangePercent;
    }

    /**
     * Returns the signed recent-versus-previous time change percentage.
     *
     * @return elapsed-time change percentage
     */
    public int getTimeChangePercent() {
        return timeChangePercent;
    }

    /**
     * Returns the qualitative move trend.
     *
     * @return move trend direction
     */
    public Direction getMoveDirection() {
        return moveDirection;
    }

    /**
     * Returns the qualitative elapsed-time trend.
     *
     * @return elapsed-time trend direction
     */
    public Direction getTimeDirection() {
        return timeDirection;
    }

    private static long averageMoves(List<Sample> samples, int start, int count) {
        if (count == 0) {
            return 0L;
        }
        long total = 0L;
        for (int index = start; index < start + count; index++) {
            total += samples.get(index).moves;
        }
        return Math.round((double) total / count);
    }

    private static long averageTime(List<Sample> samples, int start, int count) {
        if (count == 0) {
            return 0L;
        }
        long total = 0L;
        for (int index = start; index < start + count; index++) {
            total = Math.addExact(total, samples.get(index).timeMs);
        }
        return Math.round((double) total / count);
    }

    private static int percentChange(long recent, long previous) {
        if (previous == 0L) {
            return recent == 0L ? 0 : 100;
        }
        double magnitude = Math.abs(recent - previous) * 100.0 / previous;
        int rounded = (int) Math.round(magnitude);
        return recent < previous ? -rounded : rounded;
    }

    private static Direction direction(int percent) {
        if (percent <= -STEADY_PERCENT_THRESHOLD) {
            return Direction.IMPROVING;
        }
        if (percent >= STEADY_PERCENT_THRESHOLD) {
            return Direction.DECLINING;
        }
        return Direction.STEADY;
    }

    /** One immutable player-completion sample. */
    public static final class Sample {
        private final int moves;
        private final long timeMs;

        /**
         * Creates one trend sample.
         *
         * @param moves completed move count
         * @param timeMs completed elapsed milliseconds
         * @throws IllegalArgumentException when either metric is negative
         */
        public Sample(int moves, long timeMs) {
            if (moves < 0 || timeMs < 0) {
                throw new IllegalArgumentException("Trend metrics cannot be negative");
            }
            this.moves = moves;
            this.timeMs = timeMs;
        }
    }
}
