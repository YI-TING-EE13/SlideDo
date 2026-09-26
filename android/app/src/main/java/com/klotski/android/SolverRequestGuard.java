package com.klotski.android;

/** Request identity for rejecting solver callbacks that no longer own the UI. */
final class SolverRequestGuard {
    private long generation;

    long begin() {
        return ++generation;
    }

    void invalidate() {
        generation++;
    }

    boolean isCurrent(long requestId) {
        return requestId == generation;
    }
}
