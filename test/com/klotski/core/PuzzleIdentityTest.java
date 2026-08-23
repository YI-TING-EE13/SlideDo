package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PuzzleIdentityTest {
    private static final int[][] STARTING_GRID = {
            { 1, 2, 3 },
            { 4, 0, 6 },
            { 7, 5, 8 }
    };

    @Test
    void stableIdIncludesBoardSizeDifficultyAndExactStartingGrid() {
        PuzzleIdentity first = new PuzzleIdentity(3, PuzzleDifficulty.CLASSIC, STARTING_GRID);
        PuzzleIdentity same = new PuzzleIdentity(3, PuzzleDifficulty.CLASSIC, STARTING_GRID);
        PuzzleIdentity differentDifficulty =
                new PuzzleIdentity(3, PuzzleDifficulty.CHALLENGE, STARTING_GRID);
        PuzzleIdentity differentGrid = new PuzzleIdentity(3, PuzzleDifficulty.CLASSIC, new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 0, 8 }
        });

        assertEquals(first.getId(), same.getId());
        assertNotEquals(first.getId(), differentDifficulty.getId());
        assertNotEquals(first.getId(), differentGrid.getId());
    }

    @Test
    void createsFreshPracticeGameFromDefensiveStartingGrid() {
        PuzzleIdentity identity =
                new PuzzleIdentity(3, PuzzleDifficulty.CHALLENGE, STARTING_GRID);

        int[][] exported = identity.getInitialGridCopy();
        exported[0][0] = 99;
        GameModel replay = identity.createGame();

        assertArrayEquals(STARTING_GRID, identity.getInitialGridCopy());
        assertArrayEquals(STARTING_GRID, replay.getGridCopy());
        assertArrayEquals(STARTING_GRID, replay.getInitialGridCopy());
        assertEquals(PuzzleDifficulty.CHALLENGE, replay.getDifficulty());
        assertEquals(0, replay.getMoveCount());
        assertEquals(0L, replay.getElapsedTime());
    }
}
