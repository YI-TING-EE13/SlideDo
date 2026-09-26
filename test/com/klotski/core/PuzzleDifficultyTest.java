package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void seededScrambleRestartsFromSolvedStateAfterModelWasMutated() {
        GameModel fresh = new GameModel(4);
        GameModel reused = new GameModel(4);
        reused.scramble(PuzzleDifficulty.RELAXED, 18L);
        Direction mutation = reused.getEmptyRow() == 0 ? Direction.DOWN : Direction.UP;
        assertTrue(reused.move(mutation));

        fresh.scramble(PuzzleDifficulty.CHALLENGE, 42L);
        reused.scramble(PuzzleDifficulty.CHALLENGE, 42L);

        assertArrayEquals(fresh.getGridCopy(), reused.getGridCopy());
        assertArrayEquals(fresh.getInitialGridCopy(), reused.getInitialGridCopy());
        assertEquals(fresh.getMoveCount(), reused.getMoveCount());
        assertEquals(fresh.isGameRunning(), reused.isGameRunning());
        assertEquals(fresh.getDifficulty(), reused.getDifficulty());
        assertEquals(fresh.getEncodedActionHistory(), reused.getEncodedActionHistory());
        assertEquals(fresh.getEncodedRedoHistory(), reused.getEncodedRedoHistory());
    }
}
