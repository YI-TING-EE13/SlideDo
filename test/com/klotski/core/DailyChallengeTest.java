package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DailyChallengeTest {
    @Test
    void fixedDateCreatesOneStableClassicPuzzle() {
        DailyChallenge challenge = DailyChallenge.forDate(LocalDate.of(2026, 8, 21));

        assertEquals("2026-08-21", challenge.getDateId());
        assertEquals(4, challenge.getSize());
        assertEquals(PuzzleDifficulty.CLASSIC, challenge.getDifficulty());
        assertEquals(6_002_252_960_946_724_814L, challenge.getSeed());

        GameModel first = challenge.createGame();
        GameModel second = challenge.createGame();
        assertArrayEquals(first.getGridCopy(), second.getGridCopy());
        assertFalse(first.isSolved());
    }

    @Test
    void persistedDateIdRestoresTheSameChallenge() {
        DailyChallenge restored = DailyChallenge.fromDateId("2026-08-21");

        assertEquals("2026-08-21", restored.getDateId());
        assertEquals(6_002_252_960_946_724_814L, restored.getSeed());
    }
}
