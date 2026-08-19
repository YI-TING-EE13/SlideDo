package com.klotski.android;

import com.klotski.core.PuzzleDifficulty;

/**
 * Immutable result-screen model for a completed game.
 */
final class GameResult {
    final int size;
    final PuzzleDifficulty difficulty;
    final int moves;
    final long timeMs;
    final boolean assisted;
    final boolean newBest;
    final AndroidGameStore.Best previousBest;

    GameResult(int size, PuzzleDifficulty difficulty, int moves, long timeMs, boolean assisted, boolean newBest,
            AndroidGameStore.Best previousBest) {
        this.size = size;
        this.difficulty = difficulty;
        this.moves = moves;
        this.timeMs = timeMs;
        this.assisted = assisted;
        this.newBest = newBest;
        this.previousBest = previousBest;
    }
}
