package com.klotski.ui;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.IdaStarSolver;
import com.klotski.core.Solver;

/** Deterministic policy shared by the desktop Solver Tools menu and tests. */
final class DesktopSolverPolicy {
    private DesktopSolverPolicy() {
    }

    /**
     * Returns the localization key for a resource warning, or {@code null}
     * when the selected algorithm is within its normal desktop scope.
     */
    static String warningKey(Solver solver, int size) {
        if (solver instanceof BfsSolver && size >= 4) {
            return "solverWarningBfs";
        }
        if (solver instanceof AStarSolver && size > 4) {
            return "solverWarningAStar";
        }
        if (solver instanceof IdaStarSolver && size > 4) {
            return "solverWarningIdaStar";
        }
        return null;
    }
}
