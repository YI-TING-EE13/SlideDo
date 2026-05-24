package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameModelTest {
    @Test
    void moveUpdatesEmptyTileMoveCountAndUndoSnapshot() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        });

        assertTrue(model.move(Direction.UP));

        assertEquals(0, model.getEmptyRow());
        assertEquals(1, model.getEmptyCol());
        assertEquals(1, model.getMoveCount());
        assertTrue(model.canUndo());
        assertGridEquals(new int[][] {
                { 1, 0, 3 },
                { 4, 2, 6 },
                { 7, 5, 8 }
        }, model);

        assertTrue(model.undo());

        assertEquals(1, model.getEmptyRow());
        assertEquals(1, model.getEmptyCol());
        assertEquals(0, model.getMoveCount());
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        }, model);
    }

    @Test
    void slideLineToRejectsUnalignedTiles() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        });

        assertFalse(model.slideLineTo(0, 0));

        assertEquals(0, model.getMoveCount());
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        }, model);
    }

    @Test
    void slideLineToSlidesRowAsOneMoveAndOneUndoStep() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        });
        ObserverSpy observer = new ObserverSpy();
        model.addObserver(observer);

        assertTrue(model.slideLineTo(1, 0));

        assertEquals(1, model.getMoveCount());
        assertEquals(Direction.LEFT, observer.lastLineDirection);
        assertEquals(2, observer.lastLineSteps);
        assertEquals(1, observer.lineMoveCount);
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 0, 4, 5 },
                { 7, 8, 6 }
        }, model);

        assertTrue(model.undo());

        assertEquals(0, model.getMoveCount());
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        }, model);
    }

    @Test
    void slideLineToSlidesColumnAsOneMoveAndOneUndoStep() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 0, 8 }
        });
        ObserverSpy observer = new ObserverSpy();
        model.addObserver(observer);

        assertTrue(model.slideLineTo(0, 1));

        assertEquals(1, model.getMoveCount());
        assertEquals(Direction.UP, observer.lastLineDirection);
        assertEquals(2, observer.lastLineSteps);
        assertEquals(1, observer.lineMoveCount);
        assertGridEquals(new int[][] {
                { 1, 0, 3 },
                { 4, 2, 6 },
                { 7, 5, 8 }
        }, model);

        assertTrue(model.undo());

        assertEquals(0, model.getMoveCount());
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 0, 8 }
        }, model);
    }

    @Test
    void restartCurrentGameRestoresInitialLoadedGrid() {
        int[][] initial = {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        };
        GameModel model = runningModel(initial);

        assertTrue(model.move(Direction.UP));
        assertEquals(1, model.getMoveCount());

        model.restartCurrentGame();

        assertEquals(0, model.getMoveCount());
        assertTrue(model.isGameRunning());
        assertGridEquals(initial, model);
    }

    private static GameModel runningModel(int[][] grid) {
        GameModel model = new GameModel(grid.length);
        model.loadState(grid, 0);
        return model;
    }

    private static void assertGridEquals(int[][] expected, GameModel model) {
        assertArrayEquals(expected, model.getGridCopy());
    }

    private static class ObserverSpy implements GameObserver {
        private Direction lastLineDirection;
        private int lastLineSteps;
        private int lineMoveCount;

        @Override
        public void onGridChanged() {
        }

        @Override
        public void onMove(Direction dir) {
        }

        @Override
        public void onLineMove(Direction dir, int steps) {
            lastLineDirection = dir;
            lastLineSteps = steps;
            lineMoveCount++;
        }

        @Override
        public void onGameWon(int moves, long timeMs) {
        }
    }
}
