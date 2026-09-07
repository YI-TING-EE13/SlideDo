package com.klotski.core;

/**
 * Represents the four possible directions in which the empty cell can move.
 * <p>
 * A direction is part of the shared action-history and solver contract: it is
 * not the visual direction travelled by the numbered tile that is displaced.
 * Using an enum keeps that convention type-safe for both platform front ends.
 * </p>
 */
public enum Direction {
    /** Move the empty tile one row upward. */
    UP(-1, 0),

    /** Move the empty tile one row downward. */
    DOWN(1, 0),

    /** Move the empty tile one column left. */
    LEFT(0, -1),

    /** Move the empty tile one column right. */
    RIGHT(0, 1);

    /** Row delta applied to the empty tile. */
    public final int dRow;

    /** Column delta applied to the empty tile. */
    public final int dCol;

    Direction(int dRow, int dCol) {
        this.dRow = dRow;
        this.dCol = dCol;
    }

    /**
     * Returns the opposite direction.
     *
     * @return the direction that reverses this direction
     */
    public Direction opposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: throw new IllegalStateException("Unknown direction");
        }
    }
}
