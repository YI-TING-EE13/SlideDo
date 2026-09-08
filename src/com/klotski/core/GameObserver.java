package com.klotski.core;

/**
 * Observer interface for {@link GameModel} state changes.
 * <p>
 * The core model deliberately does not depend on Swing or Android. UI layers
 * subscribe through this interface and decide how to render grid changes,
 * single-step moves, line slides, and win events.
 * </p>
 * <p>Callbacks are synchronous and occur on the thread that changed the
 * model. Swing and Android adapters are responsible for entering their UI
 * thread before mutating widgets.</p>
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
     * <b>Implementation note:</b> The callback follows the model's empty-cell direction
     *           convention; it is not the visual direction of the numbered
     *           tile that was displaced.
     */
    void onMove(Direction dir);

    /**
     * Called when a whole row or column slides as one user action.
     *
     * @param dir the direction the empty tile moved
     * @param steps number of cells the empty tile moved
     * <b>Implementation note:</b> The default implementation preserves compatibility for views
     *           that only animate one-step callbacks. A line-aware view should
     *           override it to keep the whole-line action atomic visually.
     */
    default void onLineMove(Direction dir, int steps) {
        onMove(dir);
    }

    /**
     * Called when the game is won.
     *
     * @param moves the final move count
     * @param timeMs elapsed play time in milliseconds
     * <b>Implementation note:</b> The model emits this once when it recognizes the solved board;
     *           controllers that persist results must add their own run-id
     *           guard when lifecycle recreation could repeat a callback.
     */
    void onGameWon(int moves, long timeMs);
}
