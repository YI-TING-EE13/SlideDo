package com.klotski.core;

import java.util.List;

/**
 * Contract implemented by puzzle solving algorithms.
 * <p>
 * Solvers receive a snapshot-capable {@link GameModel} and must return moves
 * in the model's coordinate system: each {@link Direction} describes where the
 * empty tile moves, not where the numbered tile visually travels.
 * Implementations read a defensive board snapshot and do not mutate the live
 * model. A {@code null} result means the bounded search was interrupted,
 * timed out, or found no solution within its algorithm-specific limit.
 * </p>
 */
public interface Solver {
    /**
     * Attempts to solve the given game state.
     *
     * @param startState non-null puzzle state to solve; implementations should not mutate it
     * @return the ordered move list, or {@code null} when the search is interrupted,
     *         reaches its algorithm-specific limit, or finds no solution
     */
    List<Direction> solve(GameModel startState);
    
    /**
     * Returns a user-facing algorithm name.
     *
     * @return the display name used in menus and dialogs
     */
    String getName();
}
