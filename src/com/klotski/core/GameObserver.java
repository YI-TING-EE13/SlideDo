package com.klotski.core;

/**
 * Observer interface for {@link GameModel} state changes.
 * <p>
 * The core model deliberately does not depend on Swing or Android. UI layers
 * subscribe through this interface and decide how to render grid changes,
 * single-step moves, line slides, and win events.
 * </p>
 */
public interface GameObserver {
    /**
     * Called after the grid state changes.
     * <p>
     * This event is emitted after resets, loads, undo/redo operations, and
     * completed moves. Views that are not animating should snap their rendered
     * tiles to the model state here.
     * </p>
     */
    void onGridChanged();

    /**
     * Called when the empty tile moves one cell.
     *
     * @param dir the direction the empty tile moved
     */
    void onMove(Direction dir);

    /**
     * Called when a whole row or column slides as one user action.
     *
     * @param dir the direction the empty tile moved
     * @param steps number of cells the empty tile moved
     */
    default void onLineMove(Direction dir, int steps) {
        onMove(dir);
    }

    /**
     * Called when the game is won.
     *
     * @param moves the final move count
     * @param timeMs elapsed play time in milliseconds
     */
    void onGameWon(int moves, long timeMs);
}
