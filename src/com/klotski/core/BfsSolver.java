package com.klotski.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Breadth-first search solver for small sliding puzzles.
 * <p>
 * BFS guarantees a shortest solution in an unweighted state graph, but stores
 * every visited state. It is therefore intended for 3x3 puzzles and short
 * debugging scenarios rather than difficult 4x4 or 5x5 boards.
 * </p>
 */
public class BfsSolver implements Solver {
    private static final long TIME_LIMIT_NANOS = 5_000_000_000L;
    private static final int PACKED_MAX_SIZE = 4;
    private static final long TILE_MASK = 0xFL;
    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Creates a BFS solver instance.
     */
    public BfsSolver() {
    }

    @Override
    public String getName() {
        return "BFS (Breadth-First Search)";
    }

    /**
     * Searches level by level until the puzzle is solved or the time limit is reached.
     *
     * <p>Boards up to 4x4 use a compact four-bit tile representation and a
     * primitive visited set. The 5x5 compatibility path retains an array-based
     * representation because its 25 tile values cannot fit in one 64-bit word.</p>
     *
     * @param startState puzzle state to solve
     * @return shortest move sequence, or {@code null} if the search times out
     */
    @Override
    public List<Direction> solve(GameModel startState) {
        if (startState.getSize() <= PACKED_MAX_SIZE) {
            return solvePacked(startState);
        }
        return solveArray(startState);
    }

    private List<Direction> solvePacked(GameModel startState) {
        int size = startState.getSize();
        long startBoard = pack(startState.getGridCopy(), size);
        long solvedBoard = solvedBoard(size);
        Deque<PackedNode> queue = new ArrayDeque<>();
        LongHashSet visited = new LongHashSet();
        PackedNode root = new PackedNode(
                startBoard,
                startState.getEmptyRow() * size + startState.getEmptyCol(),
                null,
                null);
        queue.addLast(root);
        visited.add(startBoard);

        long deadline = System.nanoTime() + TIME_LIMIT_NANOS;
        int expanded = 0;
        while (!queue.isEmpty()) {
            if ((expanded++ & 1023) == 0 && System.nanoTime() - deadline > 0) {
                System.out.println("BFS timed out!");
                return null;
            }

            PackedNode current = queue.removeFirst();
            if (current.board == solvedBoard) {
                return current.getPath();
            }

            int emptyRow = current.emptyIndex / size;
            int emptyCol = current.emptyIndex % size;
            for (Direction direction : DIRECTIONS) {
                int newRow = emptyRow + direction.dRow;
                int newCol = emptyCol + direction.dCol;
                if (newRow < 0 || newRow >= size || newCol < 0 || newCol >= size) {
                    continue;
                }

                int nextEmpty = newRow * size + newCol;
                long nextBoard = swapEmpty(current.board, current.emptyIndex, nextEmpty);
                if (visited.add(nextBoard)) {
                    queue.addLast(new PackedNode(nextBoard, nextEmpty, current, direction));
                }
            }
        }
        return null;
    }

    private List<Direction> solveArray(GameModel startState) {
        Deque<ArrayNode> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        ArrayNode root = new ArrayNode(startState.getGridCopy(), startState.getSize(), null, null);
        queue.addLast(root);
        visited.add(root.toKey());

        long deadline = System.nanoTime() + TIME_LIMIT_NANOS;
        int expanded = 0;
        while (!queue.isEmpty()) {
            if ((expanded++ & 1023) == 0 && System.nanoTime() - deadline > 0) {
                System.out.println("BFS timed out!");
                return null;
            }

            ArrayNode current = queue.removeFirst();
            if (current.isSolved()) {
                return current.getPath();
            }

            for (Direction direction : DIRECTIONS) {
                ArrayNode next = current.move(direction);
                if (next == null) {
                    continue;
                }
                String key = next.toKey();
                if (visited.add(key)) {
                    queue.addLast(next);
                }
            }
        }
        return null;
    }

    private static long pack(int[][] grid, int size) {
        long packed = 0L;
        int index = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                packed |= ((long) grid[row][col] & TILE_MASK) << (index * 4);
                index++;
            }
        }
        return packed;
    }

    private static long solvedBoard(int size) {
        long packed = 0L;
        int tileCount = size * size;
        for (int index = 0; index < tileCount - 1; index++) {
            packed |= (long) (index + 1) << (index * 4);
        }
        return packed;
    }

    private static long swapEmpty(long board, int emptyIndex, int tileIndex) {
        int emptyShift = emptyIndex * 4;
        int tileShift = tileIndex * 4;
        long tile = (board >>> tileShift) & TILE_MASK;
        long withoutTile = board & ~(TILE_MASK << tileShift);
        return withoutTile | (tile << emptyShift);
    }

    private static List<Direction> buildPath(PackedNode node) {
        Deque<Direction> path = new ArrayDeque<>();
        PackedNode current = node;
        while (current.parent != null) {
            path.addFirst(current.moveFromParent);
            current = current.parent;
        }
        return new ArrayList<>(path);
    }

    private static final class PackedNode {
        final long board;
        final int emptyIndex;
        final PackedNode parent;
        final Direction moveFromParent;

        PackedNode(long board, int emptyIndex, PackedNode parent, Direction moveFromParent) {
            this.board = board;
            this.emptyIndex = emptyIndex;
            this.parent = parent;
            this.moveFromParent = moveFromParent;
        }

        List<Direction> getPath() {
            return buildPath(this);
        }
    }

    /** Primitive open-addressed set that avoids boxing one key per visited board. */
    private static final class LongHashSet {
        private static final float LOAD_FACTOR = 0.65f;
        private long[] table = new long[4096];
        private int resizeAt = (int) (table.length * LOAD_FACTOR);
        private int entryCount;
        private boolean containsZero;

        boolean add(long value) {
            if (value == 0L) {
                if (containsZero) {
                    return false;
                }
                containsZero = true;
                entryCount++;
                growIfNeeded();
                return true;
            }

            int mask = table.length - 1;
            int index = mix(value) & mask;
            while (table[index] != 0L) {
                if (table[index] == value) {
                    return false;
                }
                index = (index + 1) & mask;
            }
            table[index] = value;
            entryCount++;
            growIfNeeded();
            return true;
        }

        private void growIfNeeded() {
            if (entryCount < resizeAt) {
                return;
            }
            long[] oldTable = table;
            table = new long[oldTable.length * 2];
            resizeAt = (int) (table.length * LOAD_FACTOR);
            for (long value : oldTable) {
                if (value != 0L) {
                    insertRehashed(value);
                }
            }
        }

        private void insertRehashed(long value) {
            int mask = table.length - 1;
            int index = mix(value) & mask;
            while (table[index] != 0L) {
                index = (index + 1) & mask;
            }
            table[index] = value;
        }

        private static int mix(long value) {
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdL;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53L;
            value ^= value >>> 33;
            return (int) value;
        }
    }

    /** Array fallback for boards whose tile values cannot fit in four bits. */
    private static final class ArrayNode {
        final int[][] grid;
        final int size;
        final int emptyRow;
        final int emptyCol;
        final ArrayNode parent;
        final Direction moveFromParent;

        ArrayNode(int[][] grid, int size, ArrayNode parent, Direction moveFromParent) {
            this.grid = grid;
            this.size = size;
            this.parent = parent;
            this.moveFromParent = moveFromParent;

            int foundRow = -1;
            int foundCol = -1;
            for (int row = 0; row < size && foundRow < 0; row++) {
                for (int col = 0; col < size; col++) {
                    if (grid[row][col] == 0) {
                        foundRow = row;
                        foundCol = col;
                        break;
                    }
                }
            }
            emptyRow = foundRow;
            emptyCol = foundCol;
        }

        boolean isSolved() {
            int value = 1;
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    if (row == size - 1 && col == size - 1) {
                        return grid[row][col] == 0;
                    }
                    if (grid[row][col] != value++) {
                        return false;
                    }
                }
            }
            return true;
        }

        ArrayNode move(Direction direction) {
            int newRow = emptyRow + direction.dRow;
            int newCol = emptyCol + direction.dCol;
            if (newRow < 0 || newRow >= size || newCol < 0 || newCol >= size) {
                return null;
            }

            int[][] newGrid = new int[size][size];
            for (int row = 0; row < size; row++) {
                System.arraycopy(grid[row], 0, newGrid[row], 0, size);
            }
            newGrid[emptyRow][emptyCol] = newGrid[newRow][newCol];
            newGrid[newRow][newCol] = 0;
            return new ArrayNode(newGrid, size, this, direction);
        }

        String toKey() {
            StringBuilder key = new StringBuilder(size * size * 3);
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    key.append(grid[row][col]).append(',');
                }
            }
            return key.toString();
        }

        List<Direction> getPath() {
            Deque<Direction> path = new ArrayDeque<>();
            ArrayNode current = this;
            while (current.parent != null) {
                path.addFirst(current.moveFromParent);
                current = current.parent;
            }
            return new ArrayList<>(path);
        }
    }
}
