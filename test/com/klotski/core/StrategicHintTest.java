package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StrategicHintTest {
    @Test
    void choosesTheWinningAdjacentTileWithoutMutatingTheModel() {
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = 4;
        data.grid = new int[][] {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 0, 15}
        };
        data.initialGrid = data.grid;
        data.active = true;
        GameModel model = new GameModel(4);
        model.loadState(data);
        int[][] before = model.getGridCopy();

        StrategicHint.Hint hint = StrategicHint.choose(model);

        assertNotNull(hint);
        assertEquals(Direction.RIGHT, hint.getDirection());
        assertEquals(3, hint.getRow());
        assertEquals(3, hint.getCol());
        assertEquals(15, hint.getTile());
        assertArrayEquals(before, model.getGridCopy());
    }

    @Test
    void repeatedCallsReturnTheSameLegalSuggestion() {
        GameModel model = new GameModel(5);
        model.scramble(PuzzleDifficulty.CHALLENGE, 12345L);

        StrategicHint.Hint first = StrategicHint.choose(model);
        StrategicHint.Hint second = StrategicHint.choose(model);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getDirection(), second.getDirection());
        assertEquals(first.getTile(), second.getTile());
        assertEquals(0, model.getMoveCount());
    }
}
