package com.klotski.android;

import com.klotski.core.PuzzleDifficulty;

/**
 * Win payload held until board animation has finished.
 */
final class PendingWin {
    final int size;
    final PuzzleDifficulty difficulty;
    final int moves;
    final long timeMs;
    final boolean assisted;
    final String dailyDateId;

    PendingWin(int size, PuzzleDifficulty difficulty, int moves, long timeMs, boolean assisted) {
        this(size, difficulty, moves, timeMs, assisted, null);
    }

    PendingWin(int size, PuzzleDifficulty difficulty, int moves, long timeMs, boolean assisted,
            String dailyDateId) {
        this.size = size;
        this.difficulty = difficulty;
        this.moves = moves;
        this.timeMs = timeMs;
        this.assisted = assisted;
        this.dailyDateId = dailyDateId;
    }
}
