package com.klotski.core;

/**
 * Represents the four possible directions a tile can move.
 * <p>
 * <b>Educational Note:</b> Using an Enum is type-safe and more readable than using integers (0, 1, 2, 3).
 * We can also add methods to the Enum, like getting the opposite direction.
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
