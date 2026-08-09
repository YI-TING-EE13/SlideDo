package com.klotski.benchmark;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.IdaStarSolver;
import com.klotski.core.Solver;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Deterministic, dependency-free benchmark for player-visible solver latency.
 *
 * <p>This is deliberately separate from unit tests: it validates every result,
 * but reports latency and allocation distributions instead of enforcing a
 * machine-specific timing threshold.</p>
 */
public final class SolverBenchmark {
    private static final int WARMUP_RUNS = 2;
    private static final int MEASURED_RUNS = 7;

    private static final int[][] HARD_THREE_BY_THREE = {
            { 8, 6, 7 },
            { 2, 5, 4 },
            { 3, 0, 1 }
    };
    private static final int OPTIMAL_MOVE_COUNT = 31;

    private SolverBenchmark() {
    }

    /**
     * Runs all shared solvers against the canonical hardest 3x3 position.
     *
     * @param args optional solver names: {@code BFS}, {@code A*}, or {@code IDA*}
     */
    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        AllocationMeter allocations = AllocationMeter.create();

        System.out.println("SlideDo solver benchmark");
        System.out.println("workload=hard-3x3-31 optimalMoves=" + OPTIMAL_MOVE_COUNT
                + " warmups=" + WARMUP_RUNS + " measuredRuns=" + MEASURED_RUNS);
        System.out.println("java=" + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.runtime.version"));
        System.out.println("allocationMetric=" + (allocations.isAvailable() ? "thread-bytes" : "unavailable"));
        System.out.println("solver,median_ms,min_ms,max_ms,median_alloc_mib,solution_moves");

        if (args.length == 0) {
            run("BFS", BfsSolver::new, allocations);
            run("A*", AStarSolver::new, allocations);
            run("IDA*", IdaStarSolver::new, allocations);
            return;
        }
        for (String solverName : args) {
            switch (solverName.toUpperCase(Locale.ROOT)) {
                case "BFS" -> run("BFS", BfsSolver::new, allocations);
                case "A*", "ASTAR" -> run("A*", AStarSolver::new, allocations);
                case "IDA*", "IDASTAR" -> run("IDA*", IdaStarSolver::new, allocations);
                default -> throw new IllegalArgumentException("Unknown solver: " + solverName);
            }
        }
    }

    private static void run(String name, Supplier<Solver> solverFactory, AllocationMeter allocations) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            solveAndValidate(solverFactory.get(), allocations);
        }

        long[] elapsedNanos = new long[MEASURED_RUNS];
        long[] allocatedBytes = new long[MEASURED_RUNS];
        int solutionMoves = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            RunResult result = solveAndValidate(solverFactory.get(), allocations);
            elapsedNanos[i] = result.elapsedNanos;
            allocatedBytes[i] = result.allocatedBytes;
            solutionMoves = result.solutionMoves;
        }

        Arrays.sort(elapsedNanos);
        Arrays.sort(allocatedBytes);
        String medianAllocation = allocatedBytes[MEASURED_RUNS / 2] < 0
                ? "n/a"
                : formatMib(allocatedBytes[MEASURED_RUNS / 2]);
        System.out.printf(
                "%s,%.3f,%.3f,%.3f,%s,%d%n",
                name,
                nanosToMillis(elapsedNanos[MEASURED_RUNS / 2]),
                nanosToMillis(elapsedNanos[0]),
                nanosToMillis(elapsedNanos[MEASURED_RUNS - 1]),
                medianAllocation,
                solutionMoves);
    }

    private static RunResult solveAndValidate(Solver solver, AllocationMeter allocations) {
        GameModel source = new GameModel(3);
        source.loadState(copy(HARD_THREE_BY_THREE), 0);
        int[][] before = source.getGridCopy();

        long allocationBefore = allocations.currentThreadBytes();
        long start = System.nanoTime();
        List<Direction> path = solver.solve(source);
        long elapsedNanos = System.nanoTime() - start;
        long allocationAfter = allocations.currentThreadBytes();
        if (path == null) {
            throw new IllegalStateException(solver.getName() + " timed out");
        }
        if (path.size() != OPTIMAL_MOVE_COUNT) {
            throw new IllegalStateException(solver.getName() + " returned " + path.size()
                    + " moves; expected " + OPTIMAL_MOVE_COUNT);
        }
        if (!Arrays.deepEquals(before, source.getGridCopy())) {
            throw new IllegalStateException(solver.getName() + " mutated its input model");
        }

        GameModel replay = new GameModel(3);
        replay.loadState(copy(HARD_THREE_BY_THREE), 0);
        for (Direction direction : path) {
            if (!replay.move(direction)) {
                throw new IllegalStateException(solver.getName() + " returned an invalid move");
            }
        }
        if (!replay.isSolved()) {
            throw new IllegalStateException(solver.getName() + " returned a path that does not solve the board");
        }
        long allocatedBytes = allocationBefore < 0 || allocationAfter < 0
                ? -1
                : allocationAfter - allocationBefore;
        return new RunResult(elapsedNanos, allocatedBytes, path.size());
    }

    private static int[][] copy(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = grid[row].clone();
        }
        return copy;
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static String formatMib(long bytes) {
        return String.format(Locale.ROOT, "%.3f", bytes / (1024.0 * 1024.0));
    }

    private static final class RunResult {
        final long elapsedNanos;
        final long allocatedBytes;
        final int solutionMoves;

        RunResult(long elapsedNanos, long allocatedBytes, int solutionMoves) {
            this.elapsedNanos = elapsedNanos;
            this.allocatedBytes = allocatedBytes;
            this.solutionMoves = solutionMoves;
        }
    }

    private static final class AllocationMeter {
        private final com.sun.management.ThreadMXBean bean;

        private AllocationMeter(com.sun.management.ThreadMXBean bean) {
            this.bean = bean;
        }

        static AllocationMeter create() {
            java.lang.management.ThreadMXBean candidate = ManagementFactory.getThreadMXBean();
            if (!(candidate instanceof com.sun.management.ThreadMXBean)) {
                return new AllocationMeter(null);
            }
            com.sun.management.ThreadMXBean allocationBean = (com.sun.management.ThreadMXBean) candidate;
            try {
                if (!allocationBean.isThreadAllocatedMemorySupported()) {
                    return new AllocationMeter(null);
                }
                if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
                    allocationBean.setThreadAllocatedMemoryEnabled(true);
                }
                return new AllocationMeter(allocationBean);
            } catch (SecurityException | UnsupportedOperationException exception) {
                return new AllocationMeter(null);
            }
        }

        boolean isAvailable() {
            return bean != null;
        }

        long currentThreadBytes() {
            return bean == null ? -1 : bean.getCurrentThreadAllocatedBytes();
        }
    }
}
