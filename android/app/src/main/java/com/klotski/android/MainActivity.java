package com.klotski.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.klotski.core.AStarSolver;
import com.klotski.core.BfsSolver;
import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.GameObserver;
import com.klotski.core.IdaStarSolver;
import com.klotski.core.SaveManager;
import com.klotski.core.Solver;

import java.util.List;

/**
 * Native Android entry point for SlideDo.
 * <p>
 * The activity wires the shared {@link GameModel} to Android controls, local
 * persistence, best-record tracking, solver actions, and completion dialogs.
 * Gameplay rules remain in the shared core so Android behavior stays aligned
 * with the desktop Swing reference.
 * </p>
 */
public class MainActivity extends Activity implements GameObserver {
    private static final String PREFS = "slidedo";
    private static final String KEY_SIZE = "size";
    private static final String KEY_GRID = "grid";
    private static final String KEY_INITIAL_GRID = "initial_grid";
    private static final String KEY_MOVES = "moves";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_BEST_PREFIX = "best_";

    private GameModel model;
    private KlotskiView boardView;
    private TextView statusText;
    private PendingWin pendingWin;
    private boolean solverRunning;
    private boolean assistedSolveActive;
    private long lastWinTimeMs = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Creates the Android activity instance used by the platform launcher.
     */
    public MainActivity() {
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 1000);
        }
    };

    /**
     * Builds the Android game screen and starts from an autosave or a new 4x4 puzzle.
     *
     * @param savedInstanceState Android activity restore bundle, unused because the
     *                           game state is restored from app preferences
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = new GameModel(4);
        model.addObserver(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 24, 39));
        root.setPadding(dp(16), dp(18), dp(16), dp(16));

        TextView titleText = new TextView(this);
        titleText.setText(R.string.app_name);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(28);
        titleText.setGravity(Gravity.CENTER);
        titleText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(titleText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(4), 0, dp(12));
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        boardView = new KlotskiView(this, model);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(boardView, boardParams);

        LinearLayout sizes = new LinearLayout(this);
        sizes.setGravity(Gravity.CENTER);
        sizes.setPadding(0, dp(12), 0, 0);
        sizes.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(sizes, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addButton(sizes, R.string.button_3x3, v -> startNewGame(3));
        addButton(sizes, R.string.button_4x4, v -> startNewGame(4));
        addButton(sizes, R.string.button_5x5, v -> startNewGame(5));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(8), 0, 0);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addButton(actions, R.string.button_undo, v -> {
            if (canAcceptCommand()) {
                model.undo();
                boardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                updateStatus();
            }
        });
        addButton(actions, R.string.button_restart, v -> {
            if (canAcceptCommand()) {
                restartCurrentGame();
            }
        });

        LinearLayout saveLoad = new LinearLayout(this);
        saveLoad.setGravity(Gravity.CENTER);
        saveLoad.setPadding(0, dp(8), 0, 0);
        saveLoad.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(saveLoad, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addButton(saveLoad, R.string.button_save, v -> {
            if (canAcceptCommand()) {
                saveGame();
                Toast.makeText(this, R.string.toast_game_saved, Toast.LENGTH_SHORT).show();
            }
        });
        addButton(saveLoad, R.string.button_load, v -> {
            if (canAcceptCommand()) {
                if (loadGame()) {
                    pendingWin = null;
                    assistedSolveActive = false;
                    Toast.makeText(this, R.string.toast_game_loaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.toast_no_save, Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout solvers = new LinearLayout(this);
        solvers.setGravity(Gravity.CENTER);
        solvers.setPadding(0, dp(8), 0, 0);
        solvers.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(solvers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addButton(solvers, R.string.button_solver_bfs, v -> runSolver(new BfsSolver()));
        addButton(solvers, R.string.button_solver_astar, v -> runSolver(new AStarSolver()));
        addButton(solvers, R.string.button_solver_idastar, v -> runSolver(new IdaStarSolver()));

        setContentView(root);
        if (!loadGame()) {
            startNewGame(4);
        }
        handler.post(ticker);
    }

    /**
     * Persists the current board when Android backgrounds the activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
    }

    /**
     * Stops the status ticker when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
    }

    private void addButton(LinearLayout parent, int textResId, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(textResId);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(button, params);
    }

    private boolean canAcceptCommand() {
        return !solverRunning && !boardView.isBusy();
    }

    private void startNewGame(int size) {
        if (!canAcceptCommand()) {
            return;
        }
        model.removeObserver(this);
        model = new GameModel(size);
        model.addObserver(this);
        boardView.setModel(model);
        model.scramble(size * size * 5);
        pendingWin = null;
        assistedSolveActive = false;
        lastWinTimeMs = -1;
        boardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private void restartCurrentGame() {
        model.restartCurrentGame();
        pendingWin = null;
        assistedSolveActive = false;
        lastWinTimeMs = -1;
        boardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        updateStatus();
    }

    private void updateStatus() {
        Best best = getBest(model.getSize());
        String bestText = best == null
                ? getString(R.string.best_empty)
                : getString(R.string.best_format, formatMoves(best.moves), best.timeMs / 1000);
        if (!model.isGameRunning() && model.isSolved()) {
            long elapsed = lastWinTimeMs >= 0
                    ? lastWinTimeMs / 1000
                    : Math.max(0, System.currentTimeMillis() - model.getStartTime()) / 1000;
            statusText.setText(getString(R.string.status_solved_format, model.getMoveCount(), elapsed, bestText));
            return;
        }

        long elapsed = Math.max(0, System.currentTimeMillis() - model.getStartTime()) / 1000;
        statusText.setText(getString(R.string.status_format, formatMoves(model.getMoveCount()), elapsed, bestText));
    }

    private void saveGame() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putInt(KEY_SIZE, model.getSize());
        editor.putString(KEY_GRID, flatten(model.getGridCopy()));
        editor.putString(KEY_INITIAL_GRID, flatten(model.getInitialGridCopy()));
        editor.putInt(KEY_MOVES, model.getMoveCount());
        long elapsed = model.isSolved() && lastWinTimeMs >= 0
                ? lastWinTimeMs
                : Math.max(0, System.currentTimeMillis() - model.getStartTime());
        editor.putLong(KEY_ELAPSED, elapsed);
        editor.apply();
    }

    private boolean loadGame() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains(KEY_GRID)) {
            return false;
        }

        int size = prefs.getInt(KEY_SIZE, 4);
        if (size < 3 || size > 5) {
            return false;
        }
        int[][] grid = parseGrid(prefs.getString(KEY_GRID, ""), size);
        if (grid == null) {
            return false;
        }
        int[][] initialGrid = parseGrid(prefs.getString(KEY_INITIAL_GRID, ""), size);
        if (initialGrid == null) {
            initialGrid = copyGrid(grid);
        }

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = initialGrid;
        data.moveCount = prefs.getInt(KEY_MOVES, 0);
        data.elapsedTime = prefs.getLong(KEY_ELAPSED, 0);

        model.removeObserver(this);
        model = new GameModel(size);
        model.addObserver(this);
        boardView.setModel(model);
        model.loadState(data);
        lastWinTimeMs = model.isSolved() ? data.elapsedTime : -1;
        assistedSolveActive = false;
        updateStatus();
        return true;
    }

    private String flatten(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }

    private int[][] parseGrid(String text, int size) {
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

    private int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][grid.length];
        for (int i = 0; i < grid.length; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, grid.length);
        }
        return copy;
    }

    private Best getBest(int size) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int moves = prefs.getInt(KEY_BEST_PREFIX + size + "_moves", -1);
        long timeMs = prefs.getLong(KEY_BEST_PREFIX + size + "_time", -1);
        if (moves < 0 || timeMs < 0) {
            return null;
        }
        return new Best(moves, timeMs);
    }

    private void recordBest(int size, int moves, long timeMs) {
        Best best = getBest(size);
        if (best != null && (moves > best.moves || (moves == best.moves && timeMs >= best.timeMs))) {
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_BEST_PREFIX + size + "_moves", moves)
                .putLong(KEY_BEST_PREFIX + size + "_time", timeMs)
                .apply();
    }

    private void runSolver(Solver solver) {
        if (!canAcceptCommand() || model.isSolved()) {
            return;
        }

        int warning = solverWarningMessage(solver);
        if (warning != 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_warning_title)
                    .setMessage(warning)
                    .setPositiveButton(R.string.dialog_continue, (dialog, which) -> startSolver(solver))
                    .setNegativeButton(R.string.dialog_close, null)
                    .show();
            return;
        }

        startSolver(solver);
    }

    private int solverWarningMessage(Solver solver) {
        if (model.getSize() >= 4 && solver instanceof BfsSolver) {
            return R.string.dialog_solver_warning_bfs;
        }
        if (model.getSize() > 4 && solver instanceof AStarSolver) {
            return R.string.dialog_solver_warning_astar;
        }
        if (model.getSize() > 4 && solver instanceof IdaStarSolver) {
            return R.string.dialog_solver_warning_idastar;
        }
        return 0;
    }

    private void startSolver(Solver solver) {
        solverRunning = true;
        boardView.setInputLocked(true);
        statusText.setText(getString(R.string.status_solving, solver.getName()));
        new Thread(() -> {
            List<Direction> solution = solver.solve(model);
            handler.post(() -> finishSolver(solver, solution));
        }, "SlideDoSolver").start();
    }

    private void finishSolver(Solver solver, List<Direction> solution) {
        solverRunning = false;
        if (solution == null) {
            boardView.setInputLocked(false);
            updateStatus();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_solver_result_title)
                    .setMessage(R.string.dialog_solver_failed)
                    .setPositiveButton(R.string.dialog_close, null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_solver_result_title)
                .setMessage(getString(R.string.dialog_solver_found, solution.size()))
                .setPositiveButton(R.string.dialog_animate, (dialog, which) -> {
                    assistedSolveActive = true;
                    boardView.enqueueMoves(solution);
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> {
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .setOnCancelListener(dialog -> {
                    boardView.setInputLocked(false);
                    updateStatus();
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Updates the HUD when the shared model reports a board change.
     */
    @Override
    public void onGridChanged() {
        updateStatus();
    }

    /**
     * Updates the HUD after a single empty-tile move.
     *
     * @param dir direction the empty tile moved
     */
    @Override
    public void onMove(Direction dir) {
        updateStatus();
    }

    /**
     * Records a pending win and defers the completion dialog until animation ends.
     *
     * @param moves final move count reported by the model
     * @param timeMs elapsed play time in milliseconds
     */
    @Override
    public void onGameWon(int moves, long timeMs) {
        lastWinTimeMs = timeMs;
        pendingWin = new PendingWin(model.getSize(), moves, timeMs);
        handler.postDelayed(this::showWinWhenReady, 180);
    }

    private void showWinWhenReady() {
        if (pendingWin == null) {
            return;
        }
        if (boardView.isBusy()) {
            handler.postDelayed(this::showWinWhenReady, 80);
            return;
        }

        PendingWin win = pendingWin;
        pendingWin = null;
        if (!assistedSolveActive) {
            recordBest(win.size, win.moves, win.timeMs);
        }
        assistedSolveActive = false;
        boardView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        updateStatus();
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_solved_title)
                .setMessage(getString(R.string.dialog_solved_message, formatMoves(win.moves), win.timeMs / 1000))
                .setPositiveButton(R.string.dialog_new_game, (dialog, which) -> startNewGame(model.getSize()))
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private String formatMoves(int moves) {
        return getResources().getQuantityString(R.plurals.moves_count, moves, moves);
    }

    private static class Best {
        final int moves;
        final long timeMs;

        Best(int moves, long timeMs) {
            this.moves = moves;
            this.timeMs = timeMs;
        }
    }

    private static class PendingWin {
        final int size;
        final int moves;
        final long timeMs;

        PendingWin(int size, int moves, long timeMs) {
            this.size = size;
            this.moves = moves;
            this.timeMs = timeMs;
        }
    }
}
