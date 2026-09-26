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

    @Test
    void aggregateOverflowIsRejectedWithoutChangingTheExistingSession() {
        ContinuousChallenge moveOverflow = ContinuousChallenge.restore(
                3, 0, Integer.MAX_VALUE, 0L, 0);
        assertThrows(ArithmeticException.class,
                () -> moveOverflow.completePuzzle(1, 0L, false));
        assertEquals(0, moveOverflow.getCompletedPuzzles());
        assertEquals(Integer.MAX_VALUE, moveOverflow.getTotalMoves());

        ContinuousChallenge timeOverflow = ContinuousChallenge.restore(
                3, 0, 0, Long.MAX_VALUE, 0);
        assertThrows(ArithmeticException.class,
                () -> timeOverflow.completePuzzle(0, 1L, false));
        assertEquals(0, timeOverflow.getCompletedPuzzles());
        assertEquals(Long.MAX_VALUE, timeOverflow.getTotalTimeMs());
    }
}
