package com.klotski.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.klotski.core.GameModel;
import com.klotski.core.SaveManager;

/**
 * SharedPreferences-backed Android state store for app-level data.
 * <p>
 * This class owns the Android persistence schema for saves, records, settings,
 * onboarding, and the last selected puzzle size. Puzzle rules and validation
 * still belong to {@link GameModel}; persisted game data is loaded back into
 * the shared model before any gameplay behavior runs.
 * </p>
 */
final class AndroidGameStore {
    static final String PREFS_NAME = "slidedo";

    private static final String KEY_SIZE = "size";
    private static final String KEY_GRID = "grid";
    private static final String KEY_INITIAL_GRID = "initial_grid";
    private static final String KEY_MOVES = "moves";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_LAST_SIZE = "last_size";
    private static final String KEY_BEST_PREFIX = "best_";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    private static final String KEY_REDUCED_MOTION = "reduced_motion";

    private final SharedPreferences prefs;

    AndroidGameStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    int getLastSize(int fallback) {
        int size = prefs.getInt(KEY_LAST_SIZE, fallback);
        return isSupportedSize(size) ? size : fallback;
    }

    void setLastSize(int size) {
        if (!isSupportedSize(size)) {
            return;
        }
        prefs.edit().putInt(KEY_LAST_SIZE, size).apply();
    }

    boolean isOnboardingSeen() {
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    void markOnboardingSeen() {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
    }

    boolean isHapticEnabled() {
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true);
    }

    void setHapticEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply();
    }

    boolean isReducedMotionEnabled() {
        return prefs.getBoolean(KEY_REDUCED_MOTION, false);
    }

    void setReducedMotionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply();
    }

    void saveGame(GameModel model, long elapsedMs) {
        prefs.edit()
                .putInt(KEY_SIZE, model.getSize())
                .putString(KEY_GRID, flatten(model.getGridCopy()))
                .putString(KEY_INITIAL_GRID, flatten(model.getInitialGridCopy()))
                .putInt(KEY_MOVES, model.getMoveCount())
                .putInt(KEY_LAST_SIZE, model.getSize())
                .putLong(KEY_ELAPSED, Math.max(0, elapsedMs))
                .apply();
    }

    SaveManager.SaveData loadSavedGame() {
        if (!prefs.contains(KEY_GRID)) {
            return null;
        }

        int size = prefs.getInt(KEY_SIZE, 4);
        if (!isSupportedSize(size)) {
            return null;
        }
        int[][] grid = parseGrid(prefs.getString(KEY_GRID, ""), size);
        if (grid == null) {
            return null;
        }
        int[][] initialGrid = parseGrid(prefs.getString(KEY_INITIAL_GRID, ""), size);
        if (initialGrid == null) {
            // Older Android saves did not always persist the restart grid.
            initialGrid = copyGrid(grid);
        }

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = initialGrid;
        data.moveCount = prefs.getInt(KEY_MOVES, 0);
        data.elapsedTime = prefs.getLong(KEY_ELAPSED, 0);
        return data;
    }

    boolean hasSavedGame() {
        return loadSavedGame() != null;
    }

    void clearSavedGame() {
        prefs.edit()
                .remove(KEY_SIZE)
                .remove(KEY_GRID)
                .remove(KEY_INITIAL_GRID)
                .remove(KEY_MOVES)
                .remove(KEY_ELAPSED)
                .commit();
    }

    Best getBest(int size) {
        int moves = prefs.getInt(KEY_BEST_PREFIX + size + "_moves", -1);
        long timeMs = prefs.getLong(KEY_BEST_PREFIX + size + "_time", -1);
        if (moves < 0 || timeMs < 0) {
            return null;
        }
        return new Best(moves, timeMs);
    }

    boolean recordBestIfBetter(int size, int moves, long timeMs) {
        Best best = getBest(size);
        if (!isBetterRecord(best, moves, timeMs)) {
            return false;
        }

        prefs.edit()
                .putInt(KEY_BEST_PREFIX + size + "_moves", moves)
                .putLong(KEY_BEST_PREFIX + size + "_time", timeMs)
                .apply();
        return true;
    }

    void clearRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int size = 3; size <= 5; size++) {
            editor.remove(KEY_BEST_PREFIX + size + "_moves");
            editor.remove(KEY_BEST_PREFIX + size + "_time");
        }
        editor.commit();
    }

    static boolean isBetterRecord(Best best, int moves, long timeMs) {
        // Record ranking matches the shared desktop behavior: moves first, then time.
        return best == null || moves < best.moves || (moves == best.moves && timeMs < best.timeMs);
    }

    private static boolean isSupportedSize(int size) {
        return size >= 3 && size <= 5;
    }

    private static String flatten(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : grid) {
            for (int value : row) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private static int[][] parseGrid(String text, int size) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int[][] grid = new int[size][size];
        String[] values = text.split(",");
        if (values.length != size * size) {
            return null;
        }
        try {
            for (int i = 0; i < values.length; i++) {
                grid[i / size][i % size] = Integer.parseInt(values[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return grid;
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = new int[grid[i].length];
            System.arraycopy(grid[i], 0, copy[i], 0, grid[i].length);
        }
        return copy;
    }

    /**
     * Immutable local best record payload.
     */
    static final class Best {
        final int moves;
        final long timeMs;

        Best(int moves, long timeMs) {
            this.moves = moves;
            this.timeMs = timeMs;
        }
    }
}
