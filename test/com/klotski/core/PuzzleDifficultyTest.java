package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PuzzleDifficultyTest {
    @Test
    void difficultiesUseStableIdsAndIncreasingScrambleBudgets() {
        assertEquals("relaxed", PuzzleDifficulty.RELAXED.getId());
        assertEquals("classic", PuzzleDifficulty.CLASSIC.getId());
        assertEquals("challenge", PuzzleDifficulty.CHALLENGE.getId());

        assertEquals(27, PuzzleDifficulty.RELAXED.scrambleMovesForSize(3));
        assertEquals(45, PuzzleDifficulty.CLASSIC.scrambleMovesForSize(3));
        assertEquals(72, PuzzleDifficulty.CHALLENGE.scrambleMovesForSize(3));
    }

    @Test
    void unknownOrMissingDifficultyFallsBackToClassic() {
        assertEquals(PuzzleDifficulty.CLASSIC, PuzzleDifficulty.fromId(null));
        assertEquals(PuzzleDifficulty.CLASSIC, PuzzleDifficulty.fromId(""));
        assertEquals(PuzzleDifficulty.CLASSIC, PuzzleDifficulty.fromId("future-value"));
        assertEquals(PuzzleDifficulty.RELAXED, PuzzleDifficulty.fromId("relaxed"));
    }

    @Test
    void seededDifficultyScrambleIsReproducibleAndTracksSelection() {
        GameModel first = new GameModel(4);
        GameModel second = new GameModel(4);

        first.scramble(PuzzleDifficulty.CHALLENGE, 42L);
        second.scramble(PuzzleDifficulty.CHALLENGE, 42L);

        assertArrayEquals(first.getGridCopy(), second.getGridCopy());
        assertEquals(PuzzleDifficulty.CHALLENGE, first.getDifficulty());
        assertFalse(first.isSolved());
    }
}
