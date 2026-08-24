package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContinuousChallengeTest {
    @Test
    void recordsPuzzleBoundariesAndFinishesAtTarget() {
        ContinuousChallenge challenge = ContinuousChallenge.start(3)
                .completePuzzle(20, 10_000L, false)
                .completePuzzle(30, 20_000L, true);

        assertEquals(2, challenge.getCompletedPuzzles());
        assertEquals(3, challenge.getCurrentPuzzleNumber());
        assertEquals(50, challenge.getTotalMoves());
        assertEquals(30_000L, challenge.getTotalTimeMs());
        assertEquals(1, challenge.getAssistedPuzzles());
        assertFalse(challenge.isComplete());

        ContinuousChallenge complete = challenge.completePuzzle(40, 30_000L, false);
        assertTrue(complete.isComplete());
        assertEquals(2, complete.getPlayerPuzzles());
        assertThrows(IllegalStateException.class,
                () -> complete.completePuzzle(1, 1L, false));
    }

    @Test
    void onlySupportedSessionLengthsCanStartOrRestore() {
        assertTrue(ContinuousChallenge.isSupportedTarget(3));
        assertTrue(ContinuousChallenge.isSupportedTarget(5));
        assertTrue(ContinuousChallenge.isSupportedTarget(10));
        assertFalse(ContinuousChallenge.isSupportedTarget(4));
        assertThrows(IllegalArgumentException.class, () -> ContinuousChallenge.start(4));
        assertThrows(IllegalArgumentException.class,
                () -> ContinuousChallenge.restore(3, 4, 0, 0L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ContinuousChallenge.restore(3, 2, 0, 0L, 3));
    }
}
