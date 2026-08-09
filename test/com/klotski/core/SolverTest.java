package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SolverTest {
    @ParameterizedTest
    @MethodSource("solvers")
    void solversReturnEmptyPathForSolvedThreeByThree(Solver solver) {
        GameModel model = new GameModel(3);

        List<Direction> path = solver.solve(model);

        assertNotNull(path, solver.getName());
        assertTrue(path.isEmpty(), solver.getName());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    void solversFindSolutionForOneMoveAwayThreeByThree(Solver solver) {
        GameModel model = new GameModel(3);
        model.loadState(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 0, 8 }
        }, 0);

        List<Direction> path = solver.solve(model);

        assertNotNull(path, solver.getName());
        assertTrue(path.size() <= 1, solver.getName());
        for (Direction direction : path) {
            assertTrue(model.move(direction), solver.getName());
        }
        assertTrue(model.isSolved(), solver.getName());
    }

    @Test
    void bfsSolvesCanonicalHardestThreeByThreeOptimallyWithoutMutatingInput() {
        int[][] hardest = {
                { 8, 6, 7 },
                { 2, 5, 4 },
                { 3, 0, 1 }
        };
        GameModel model = new GameModel(3);
        model.loadState(hardest, 0);
        int[][] before = model.getGridCopy();

        List<Direction> path = new BfsSolver().solve(model);

        assertNotNull(path);
        assertEquals(31, path.size());
        assertTrue(Arrays.deepEquals(before, model.getGridCopy()));
        applyAndAssertSolved(model, path);
    }

    @Test
    void bfsSupportsPackedFourByFourTileValues() {
        GameModel model = new GameModel(4);
        model.loadState(new int[][] {
                { 1, 2, 3, 4 },
                { 5, 6, 7, 8 },
                { 9, 10, 11, 12 },
                { 13, 14, 0, 15 }
        }, 0);

        List<Direction> path = new BfsSolver().solve(model);

        assertNotNull(path);
        assertEquals(1, path.size());
        applyAndAssertSolved(model, path);
    }

    @Test
    void bfsRetainsFiveByFiveCompatibility() {
        GameModel model = new GameModel(5);
        model.loadState(new int[][] {
                { 1, 2, 3, 4, 5 },
                { 6, 7, 8, 9, 10 },
                { 11, 12, 13, 14, 15 },
                { 16, 17, 18, 19, 20 },
                { 21, 22, 23, 0, 24 }
        }, 0);

        List<Direction> path = new BfsSolver().solve(model);

        assertNotNull(path);
        assertEquals(1, path.size());
        applyAndAssertSolved(model, path);
    }

    private static void applyAndAssertSolved(GameModel model, List<Direction> path) {
        for (Direction direction : path) {
            assertTrue(model.move(direction));
        }
        assertTrue(model.isSolved());
    }

    private static Stream<Solver> solvers() {
        return Stream.of(new BfsSolver(), new AStarSolver(), new IdaStarSolver());
    }
}
