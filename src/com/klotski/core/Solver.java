package com.klotski.core;

import java.util.List;

/**
 * Contract implemented by puzzle solving algorithms.
 * <p>
 * Solvers receive a snapshot-capable {@link GameModel} and must return moves
 * in the model's coordinate system: each {@link Direction} describes where the
 * empty tile moves, not where the numbered tile visually travels.
 * </p>
 */
public interface Solver {
    /**
     * Attempts to solve the given game state.
     *
     * @param startState the puzzle state to solve; implementations should not mutate it
     * @return the ordered move list, or {@code null} when no solution is found before the solver's limit
     */
    List<Direction> solve(GameModel startState);
    
    /**
     * Returns a user-facing algorithm name.
     *
     * @return the display name used in menus and dialogs
     */
    String getName();
}
