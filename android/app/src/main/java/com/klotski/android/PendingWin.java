package com.klotski.android;

/**
 * Win payload held until board animation has finished.
 */
final class PendingWin {
    final int size;
    final int moves;
    final long timeMs;
    final boolean assisted;

    PendingWin(int size, int moves, long timeMs, boolean assisted) {
        this.size = size;
        this.moves = moves;
        this.timeMs = timeMs;
        this.assisted = assisted;
    }
}
