package com.klotski.core;

import java.util.*;

/**
 * A* solver using Manhattan distance as the heuristic.
 * <p>
 * Each node is scored as {@code f(n) = g(n) + h(n)}, where {@code g(n)} is
 * the number of moves from the start and {@code h(n)} is the estimated distance
 * to the goal. This is more directed than BFS, but can still be expensive on
 * hard 4x4 and 5x5 boards.
 * </p>
 */
public class AStarSolver implements Solver {

    /**
     * Creates an A* solver instance.
     */
    public AStarSolver() {
    }

    @Override
    public String getName() {
        return "A* (A-Star Search)";
    }

    /**
     * Attempts to solve a puzzle using an informed priority queue search.
     *
     * @param startState puzzle state to solve
     * @return move sequence, or {@code null} if the solver reaches its time limit
     */
    @Override
    public List<Direction> solve(GameModel startState) {
        PriorityQueue<StateNode> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        
        StateNode root = new StateNode(startState.getGridCopy(), startState.getSize(), null, null, 0);
        openSet.add(root);
        
        long startTime = System.currentTimeMillis();
        
        while (!openSet.isEmpty()) {
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("A* timed out!");
                return null;
            }

            StateNode current = openSet.poll();
            
            if (current.isSolved()) {
                return current.getPath();
            }
            
            closedSet.add(current.toKey());
            
            for (Direction dir : Direction.values()) {
                StateNode neighbor = current.move(dir);
                if (neighbor != null && !closedSet.contains(neighbor.toKey())) {
                    openSet.add(neighbor);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Search node ordered by the A* total cost.
     */
    private static class StateNode implements Comparable<StateNode> {
        int[][] grid;
        int size;
        int emptyRow, emptyCol;
        StateNode parent;
        Direction moveFromParent;
        int g; // Cost from start
        int h; // Heuristic cost
        int f; // Total cost
        
        StateNode(int[][] grid, int size, StateNode parent, Direction moveFromParent, int g) {
            this.grid = grid;
            this.size = size;
            this.parent = parent;
            this.moveFromParent = moveFromParent;
            this.g = g;
            
            // Find empty tile and calculate Heuristic
            this.h = 0;
            for(int r=0; r<size; r++) {
                for(int c=0; c<size; c++) {
                    int val = grid[r][c];
                    if (val == 0) {
                        this.emptyRow = r;
                        this.emptyCol = c;
                    } else {
                        // Calculate target position for this value
                        // Value 1 is at (0,0), 2 at (0,1)...
                        int targetRow = (val - 1) / size;
                        int targetCol = (val - 1) % size;
                        this.h += Math.abs(r - targetRow) + Math.abs(c - targetCol);
                    }
                }
            }
            this.f = this.g + this.h;
        }
        
        boolean isSolved() {
            return h == 0;
        }
        
        StateNode move(Direction dir) {
            int newRow = emptyRow + dir.dRow;
            int newCol = emptyCol + dir.dCol;
            
            if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size) {
                int[][] newGrid = new int[size][size];
                for(int i=0; i<size; i++) System.arraycopy(grid[i], 0, newGrid[i], 0, size);
                
                newGrid[emptyRow][emptyCol] = newGrid[newRow][newCol];
                newGrid[newRow][newCol] = 0;
                
                return new StateNode(newGrid, size, this, dir, g + 1);
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

        @Override
        public int compareTo(StateNode other) {
            return Integer.compare(this.f, other.f);
        }
    }
}
