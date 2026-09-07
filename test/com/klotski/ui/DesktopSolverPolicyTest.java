package com.klotski.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.IdaStarSolver;
import org.junit.jupiter.api.Test;

class DesktopSolverPolicyTest {
    @Test
    void warningsAreBoundedByAlgorithmAndBoardSize() {
        assertNull(DesktopSolverPolicy.warningKey(new BfsSolver(), 3));
        assertEquals("solverWarningBfs", DesktopSolverPolicy.warningKey(new BfsSolver(), 4));
        assertNull(DesktopSolverPolicy.warningKey(new AStarSolver(), 4));
        assertEquals("solverWarningAStar", DesktopSolverPolicy.warningKey(new AStarSolver(), 5));
        assertNull(DesktopSolverPolicy.warningKey(new IdaStarSolver(), 4));
        assertEquals("solverWarningIdaStar", DesktopSolverPolicy.warningKey(new IdaStarSolver(), 5));
    }
}
