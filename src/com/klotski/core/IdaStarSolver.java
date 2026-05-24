package com.klotski.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterative Deepening A* solver for memory-conscious solving.
 * <p>
 * IDA* searches depth-first while gradually raising an {@code f = g + h}
 * threshold. It uses far less memory than regular A*, making it a better fit
 * for Android and other memory-constrained environments.
 * </p>
 */
public class IdaStarSolver implements Solver {
    private static final int FOUND = -1;
    private static final int INF = Integer.MAX_VALUE / 4;
    private static final long TIME_LIMIT_MS = 15_000;

    private int size;
    private int[] board;
    private int emptyIndex;
    private long startTime;
    private final List<Direction> path = new ArrayList<>();
    private boolean timedOut;

    /**
     * Creates an IDA* solver instance.
     */
    public IdaStarSolver() {
    }

    @Override
    public String getName() {
        return "IDA* (Iterative Deepening A*)";
    }

    /**
     * Attempts to solve the current board with iterative deepening.
     *
     * @param startState puzzle state to solve
     * @return move sequence, or {@code null} when the time limit is reached
     */
    @Override
    public List<Direction> solve(GameModel startState) {
        size = startState.getSize();
        int[][] grid = startState.getGridCopy();
        board = new int[size * size];
        path.clear();
        timedOut = false;

        int index = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                board[index] = grid[r][c];
                if (board[index] == 0) {
                    emptyIndex = index;
                }
                index++;
            }
        }

        startTime = System.currentTimeMillis();
        int bound = heuristic();
        while (!timedOut && bound < INF) {
            int nextBound = search(0, bound, null);
            if (nextBound == FOUND) {
                return new ArrayList<>(path);
            }
            bound = nextBound;
        }
        return null;
    }

    /**
     * Performs one bounded depth-first search pass.
     *
     * @param depth current depth in the search tree
     * @param bound maximum allowed {@code g + h} score for this pass
     * @param previousMove last empty-tile move, used to avoid immediate backtracking
     * @return {@link #FOUND}, the next bound to try, or {@link #INF} on timeout
     */
    private int search(int depth, int bound, Direction previousMove) {
        if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
            timedOut = true;
            return INF;
        }

        int h = heuristic();
        int f = depth + h;
        if (f > bound) {
            return f;
        }
        if (h == 0) {
            return FOUND;
        }

        int min = INF;
        for (Direction dir : Direction.values()) {
            if (previousMove != null && dir == previousMove.opposite()) {
                continue;
            }

            int nextEmpty = nextEmptyIndex(dir);
            if (nextEmpty < 0) {
                continue;
            }

            swap(emptyIndex, nextEmpty);
            int oldEmpty = emptyIndex;
            emptyIndex = nextEmpty;
            path.add(dir);

            int result = search(depth + 1, bound, dir);
            if (result == FOUND) {
                return FOUND;
            }
            if (result < min) {
                min = result;
            }

            path.remove(path.size() - 1);
            emptyIndex = oldEmpty;
            swap(emptyIndex, nextEmpty);
        }

        return min;
    }

    private int nextEmptyIndex(Direction dir) {
        int row = emptyIndex / size;
        int col = emptyIndex % size;
        int newRow = row + dir.dRow;
        int newCol = col + dir.dCol;
        if (newRow < 0 || newRow >= size || newCol < 0 || newCol >= size) {
            return -1;
        }
        return newRow * size + newCol;
    }

    private void swap(int a, int b) {
        int temp = board[a];
        board[a] = board[b];
        board[b] = temp;
    }

    private int heuristic() {
        return manhattanDistance() + linearConflict();
    }

    private int manhattanDistance() {
        int distance = 0;
        for (int i = 0; i < board.length; i++) {
            int value = board[i];
            if (value == 0) {
                continue;
            }
            int target = value - 1;
            distance += Math.abs(i / size - target / size);
            distance += Math.abs(i % size - target % size);
        }
        return distance;
    }

    private int linearConflict() {
        int conflicts = 0;

        for (int row = 0; row < size; row++) {
            for (int c1 = 0; c1 < size; c1++) {
                int value1 = board[row * size + c1];
                if (value1 == 0 || (value1 - 1) / size != row) {
                    continue;
                }
                int targetCol1 = (value1 - 1) % size;
                for (int c2 = c1 + 1; c2 < size; c2++) {
                    int value2 = board[row * size + c2];
                    if (value2 == 0 || (value2 - 1) / size != row) {
                        continue;
                    }
                    int targetCol2 = (value2 - 1) % size;
                    if (targetCol1 > targetCol2) {
                        conflicts += 2;
                    }
                }
            }
        }

        for (int col = 0; col < size; col++) {
            for (int r1 = 0; r1 < size; r1++) {
                int value1 = board[r1 * size + col];
                if (value1 == 0 || (value1 - 1) % size != col) {
                    continue;
                }
                int targetRow1 = (value1 - 1) / size;
                for (int r2 = r1 + 1; r2 < size; r2++) {
                    int value2 = board[r2 * size + col];
                    if (value2 == 0 || (value2 - 1) % size != col) {
                        continue;
                    }
                    int targetRow2 = (value2 - 1) / size;
                    if (targetRow1 > targetRow2) {
                        conflicts += 2;
                    }
                }
            }
        }

        return conflicts;
    }
}
