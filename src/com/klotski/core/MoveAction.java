package com.klotski.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * One counted player or solver action in the current puzzle run.
 * <p>
 * Direction follows {@link GameModel}'s empty-cell convention. A single-tile
 * move has one step, while a whole-line slide keeps all of its cells in one
 * action with a larger step count.
 * </p>
 */
public final class MoveAction implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Direction in which the empty cell moved. */
    private final Direction direction;
    /** Number of cells traversed by the empty cell during this action. */
    private final int steps;

    /**
     * Creates one immutable move-history action.
     *
     * @param direction direction moved by the empty cell
     * @param steps number of cells moved, at least one
     */
    public MoveAction(Direction direction, int steps) {
        this.direction = Objects.requireNonNull(direction, "direction");
        if (steps < 1) {
            throw new IllegalArgumentException("Move action steps must be positive");
        }
        this.steps = steps;
    }

    /**
     * Returns the direction moved by the empty cell.
     *
     * @return direction moved by the empty cell
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the number of cells moved in this one action.
     *
     * @return number of cells moved in this one action
     */
    public int getSteps() {
        return steps;
    }
}
