package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameModelTest {
    @Test
    void elapsedTimeStopsWhilePausedAndContinuesAfterResume() {
        MutableTimeSource time = new MutableTimeSource(1_000L);
        GameModel model = new GameModel(3, time::now);
        model.loadState(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        }, 0);

        time.advance(1_500L);
        assertEquals(1_500L, model.getElapsedTime());

        model.pauseTimer();
        time.advance(5_000L);
        assertEquals(1_500L, model.getElapsedTime());

        model.resumeTimer();
        time.advance(500L);
        assertEquals(2_000L, model.getElapsedTime());
    }

    @Test
    void pauseAndResumeAreIdempotent() {
        MutableTimeSource time = new MutableTimeSource(10_000L);
        GameModel model = new GameModel(3, time::now);
        model.loadState(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        }, 0);

        time.advance(1_000L);
        model.pauseTimer();
        model.pauseTimer();
        time.advance(4_000L);
        model.resumeTimer();
        model.resumeTimer();
        time.advance(750L);

        assertEquals(1_750L, model.getElapsedTime());
        assertTrue(model.isTimerRunning());
    }

    @Test
    void loadedElapsedTimeContinuesWithoutCountingPausedTime() {
        MutableTimeSource time = new MutableTimeSource(20_000L);
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.grid = new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        };
        data.initialGrid = data.grid;
        data.moveCount = 4;
        data.elapsedTime = 12_000L;
        data.active = true;
        data.updatedAt = 1L;

        GameModel model = new GameModel(3, time::now);
        model.loadState(data);
        time.advance(1_000L);
        model.pauseTimer();
        time.advance(9_000L);

        assertEquals(13_000L, model.getElapsedTime());
    }

    @Test
    void winningTimeContainsOnlyActivePlayTime() {
        MutableTimeSource time = new MutableTimeSource(30_000L);
        GameModel model = new GameModel(3, time::now);
        model.loadState(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        }, 0);
        ObserverSpy observer = new ObserverSpy();
        model.addObserver(observer);

        time.advance(1_000L);
        model.pauseTimer();
        time.advance(5_000L);
        model.resumeTimer();
        time.advance(2_000L);
        assertTrue(model.move(Direction.DOWN));

        assertEquals(3_000L, observer.lastWinTimeMs);
        assertEquals(3_000L, model.getElapsedTime());
        assertFalse(model.isTimerRunning());
    }

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
    void redoRestoresWholeLineAsOneActionAndNewMoveClearsRedo() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        });

        assertTrue(model.slideLineTo(1, 0));
        assertEquals(1, model.getActionHistory().size());
        assertEquals(Direction.LEFT, model.getActionHistory().get(0).getDirection());
        assertEquals(2, model.getActionHistory().get(0).getSteps());
        assertTrue(model.undo());
        assertTrue(model.canRedo());
        assertEquals(1, model.getRedoHistory().size());

        assertTrue(model.redo());
        assertEquals(1, model.getMoveCount());
        assertFalse(model.canRedo());
        assertGridEquals(new int[][] {
                { 1, 2, 3 },
                { 0, 4, 5 },
                { 7, 8, 6 }
        }, model);

        assertTrue(model.undo());
        assertTrue(model.move(Direction.UP));
        assertFalse(model.canRedo());
        assertEquals(1, model.getActionHistory().size());
    }

    @Test
    void restartClearsActionAndRedoHistory() {
        GameModel model = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 0, 6 },
                { 7, 5, 8 }
        });

        assertTrue(model.move(Direction.UP));
        assertTrue(model.undo());
        assertTrue(model.canRedo());

        model.restartCurrentGame();

        assertTrue(model.getActionHistory().isEmpty());
        assertTrue(model.getRedoHistory().isEmpty());
        assertFalse(model.canUndo());
        assertFalse(model.canRedo());
    }

    @Test
    void loadRestoresValidatedActionAndRedoHistory() {
        GameModel source = runningModel(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        });
        assertTrue(source.slideLineTo(1, 0));
        assertTrue(source.move(Direction.UP));
        assertTrue(source.undo());

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = source.getSize();
        data.grid = source.getGridCopy();
        data.initialGrid = source.getInitialGridCopy();
        data.moveCount = source.getMoveCount();
        data.active = true;
        data.updatedAt = 1L;
        data.difficulty = source.getDifficulty();
        data.actionHistory = source.getEncodedActionHistory();
        data.redoHistory = source.getEncodedRedoHistory();

        GameModel restored = new GameModel(3);
        restored.loadState(data);

        assertEquals(1, restored.getActionHistory().size());
        assertTrue(restored.canUndo());
        assertTrue(restored.canRedo());
        assertTrue(restored.redo());
        assertEquals(2, restored.getMoveCount());
        assertGridEquals(new int[][] {
                { 0, 2, 3 },
                { 1, 4, 5 },
                { 7, 8, 6 }
        }, restored);
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

    @Test
    void restartAfterWinReplaysExactStartingGridAndResetsRunState() {
        MutableTimeSource time = new MutableTimeSource(50_000L);
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.grid = new int[][] {
                { 1, 2, 3 },
                { 4, 5, 0 },
                { 7, 8, 6 }
        };
        data.initialGrid = data.grid;
        data.active = true;
        data.updatedAt = 1L;
        data.difficulty = PuzzleDifficulty.CHALLENGE;
        GameModel model = new GameModel(3, time::now);
        model.loadState(data);

        time.advance(2_000L);
        assertTrue(model.move(Direction.DOWN));
        assertTrue(model.isSolved());

        model.restartCurrentGame();

        assertGridEquals(data.initialGrid, model);
        assertEquals(PuzzleDifficulty.CHALLENGE, model.getDifficulty());
        assertEquals(0, model.getMoveCount());
        assertEquals(0L, model.getElapsedTime());
        assertTrue(model.isGameRunning());
        assertTrue(model.isTimerRunning());
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
        private long lastWinTimeMs = -1L;

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
            lastWinTimeMs = timeMs;
        }
    }

    private static final class MutableTimeSource {
        private long now;

        MutableTimeSource(long now) {
            this.now = now;
        }

        long now() {
            return now;
        }

        void advance(long milliseconds) {
            now += milliseconds;
        }
    }
}
