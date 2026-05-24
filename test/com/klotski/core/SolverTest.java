package com.klotski.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

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

    private static Stream<Solver> solvers() {
        return Stream.of(new BfsSolver(), new AStarSolver(), new IdaStarSolver());
    }
}
