package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopCompletionTrackerTest {
    @Test
    void oneRunCanBeClaimedOnlyOnceAndResetCreatesANewRun() {
        DesktopCompletionTracker tracker = new DesktopCompletionTracker();
        String first = tracker.runId();

        assertTrue(tracker.claim());
        assertFalse(tracker.claim());

        String second = tracker.reset();
        assertNotEquals(first, second);
        assertTrue(tracker.claim());
        assertFalse(tracker.claim());
    }
}
