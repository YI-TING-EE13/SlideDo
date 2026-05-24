package com.klotski.core;

import java.util.*;

/**
 * Breadth-first search solver for small sliding puzzles.
 * <p>
 * BFS guarantees a shortest solution in an unweighted state graph, but stores
 * every visited state. It is therefore intended for 3x3 puzzles and short
 * debugging scenarios rather than difficult 4x4 or 5x5 boards.
 * </p>
 */
public class BfsSolver implements Solver {

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
     * @param startState puzzle state to solve
     * @return shortest move sequence, or {@code null} if the search times out
     */
    @Override
    public List<Direction> solve(GameModel startState) {
        // We need a way to represent the state immutably for the Set/Map keys.
        // A String representation or a deep hash of the grid is common.
        // Here we use a custom StateNode class to track the path.
        
        Queue<StateNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        StateNode root = new StateNode(startState.getGridCopy(), startState.getSize(), null, null);
        queue.add(root);
        visited.add(root.toKey());
        
        long startTime = System.currentTimeMillis();
        
        while (!queue.isEmpty()) {
            // Safety break for large puzzles
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("BFS timed out!");
                return null;
            }

            StateNode current = queue.poll();
            
            if (current.isSolved()) {
                return current.getPath();
            }
            
            for (Direction dir : Direction.values()) {
                StateNode next = current.move(dir);
                if (next != null && !visited.contains(next.toKey())) {
                    visited.add(next.toKey());
                    queue.add(next);
                }
            }
        }
        
        return null; // No solution found
    }
    
    /**
     * Immutable-style node used as a BFS frontier entry.
     */
    private static class StateNode {
        int[][] grid;
        int size;
        int emptyRow, emptyCol;
        StateNode parent;
        Direction moveFromParent;
        
        StateNode(int[][] grid, int size, StateNode parent, Direction moveFromParent) {
            this.grid = grid;
            this.size = size;
            this.parent = parent;
            this.moveFromParent = moveFromParent;
            
            // Find empty tile
            for(int r=0; r<size; r++) {
                for(int c=0; c<size; c++) {
                    if (grid[r][c] == 0) {
                        this.emptyRow = r;
                        this.emptyCol = c;
                        break;
                    }
                }
            }
        }
        
        boolean isSolved() {
            int value = 1;
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (r == size - 1 && c == size - 1) {
                        if (grid[r][c] != 0) return false;
                    } else {
                        if (grid[r][c] != value) return false;
                        value++;
                    }
                }
            }
            return true;
        }
        
        StateNode move(Direction dir) {
            int newRow = emptyRow + dir.dRow;
            int newCol = emptyCol + dir.dCol;
            
            if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size) {
                int[][] newGrid = new int[size][size];
                for(int i=0; i<size; i++) System.arraycopy(grid[i], 0, newGrid[i], 0, size);
                
                // Swap
                newGrid[emptyRow][emptyCol] = newGrid[newRow][newCol];
                newGrid[newRow][newCol] = 0;
                
                return new StateNode(newGrid, size, this, dir);
            }
            return null;
        }
        
        String toKey() {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    sb.append(grid[r][c]).append(",");
                }
            }
            return sb.toString();
        }
        
        List<Direction> getPath() {
            LinkedList<Direction> path = new LinkedList<>();
            StateNode curr = this;
            while (curr.parent != null) {
                path.addFirst(curr.moveFromParent);
                curr = curr.parent;
            }
            return path;
        }
    }
}
