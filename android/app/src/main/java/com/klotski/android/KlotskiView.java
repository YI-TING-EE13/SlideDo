package com.klotski.android;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.animation.PathInterpolator;

import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.GameObserver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Custom Android board view for SlideDo.
 * <p>
 * The view renders the current {@link GameModel}, translates taps and swipes
 * into shared model moves, and owns animation state. Puzzle rules remain in
 * the model so Android gestures match the desktop reference behavior.
 * </p>
 */
public class KlotskiView extends View implements GameObserver {
    private static final long MOVE_ANIMATION_DURATION_MS = 170;

    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Deque<Direction> moveQueue = new ArrayDeque<>();
    private final RectF rect = new RectF();

    private GameModel model;
    private float gap;
    private float tileSize;
    private float boardLeft;
    private float boardTop;
    private float downX;
    private float downY;
    private int downRow = -1;
    private int downCol = -1;
    private boolean isAnimating;
    private int movingValue;
    private int skipRow = -1;
    private int skipCol = -1;
    private final List<AnimatedTile> animatedTiles = new ArrayList<>();
    private float animFromRow;
    private float animFromCol;
    private float animToRow;
    private float animToCol;
    private float animationProgress = 1f;
    private boolean inputLocked;
    private boolean trackingTouch;
    private boolean reducedMotionEnabled;
    private Runnable busyStateListener;
    private boolean[][] highlightedCells;
    private int targetRow = -1;
    private int targetCol = -1;

    /**
     * Creates an unbound board view for programmatic construction.
     *
     * @param context Android context
     */
    public KlotskiView(Context context) {
        this(context, (AttributeSet) null);
    }

    /**
     * Creates an unbound board view with Android XML attributes.
     *
     * @param context Android context
     * @param attrs optional XML attributes
     */
    public KlotskiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Creates a board view bound to a shared game model.
     *
     * @param context Android context
     * @param model game model to observe and render
     */
    public KlotskiView(Context context, GameModel model) {
        super(context);
        this.model = model;
        model.addObserver(this);
        init();
    }

    private void init() {
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setHapticFeedbackEnabled(true);

        gap = dp(9);
        tileHighlightPaint.setColor(Color.argb(42, 255, 255, 255));
        hintFillPaint.setStyle(Paint.Style.FILL);
        hintStrokePaint.setStyle(Paint.Style.STROKE);
        hintStrokePaint.setStrokeWidth(dp(3));
        targetStrokePaint.setColor(Color.WHITE);
        targetStrokePaint.setStyle(Paint.Style.STROKE);
        targetStrokePaint.setStrokeWidth(dp(4));
        emptyCellPaint.setColor(Color.argb(78, 255, 255, 255));
        emptyCellPaint.setStyle(Paint.Style.STROKE);
        emptyCellPaint.setStrokeWidth(dp(2));
        emptyDotPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        setVisualTheme(AndroidVisualTheme.MIDNIGHT);
        refreshAccessibilityDescription();
    }

    /**
     * Applies presentation colors without changing the bound puzzle state.
     *
     * @param theme selected Android visual theme
     */
    void setVisualTheme(AndroidVisualTheme theme) {
        AndroidVisualTheme selected = theme == null ? AndroidVisualTheme.MIDNIGHT : theme;
        boardPaint.setColor(selected.panel);
        tilePaint.setColor(selected.primary);
        hintFillPaint.setColor(withAlpha(selected.accent, 64));
        hintStrokePaint.setColor(withAlpha(selected.accent, 230));
        emptyDotPaint.setColor(withAlpha(selected.accent, 150));
        invalidate();
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Rebinds the view to a new game model and clears pending animation state.
     *
     * @param model replacement model to observe
     */
    public void setModel(GameModel model) {
        if (this.model != null) {
            this.model.removeObserver(this);
        }
        moveQueue.clear();
        isAnimating = false;
        movingValue = 0;
        skipRow = -1;
        skipCol = -1;
        animatedTiles.clear();
        inputLocked = false;
        trackingTouch = false;
        this.model = model;
        this.model.addObserver(this);
        refreshAccessibilityDescription();
        invalidate();
        notifyBusyStateChanged();
    }

    /**
     * Updates render-only hint cells used by guided lessons.
     * <p>
     * These highlights do not affect touch validation or puzzle rules; gestures
     * are still accepted or rejected by the shared model path.
     * </p>
     *
     * @param highlightedCells true values mark cells to outline
     * @param targetRow row for the emphasized target cell, or {@code -1}
     * @param targetCol column for the emphasized target cell, or {@code -1}
     */
    public void setHighlightedCells(boolean[][] highlightedCells, int targetRow, int targetCol) {
        if (highlightedCells == null) {
            clearHighlights();
            return;
        }
        this.highlightedCells = new boolean[highlightedCells.length][];
        for (int i = 0; i < highlightedCells.length; i++) {
            this.highlightedCells[i] = new boolean[highlightedCells[i].length];
            System.arraycopy(highlightedCells[i], 0, this.highlightedCells[i], 0, highlightedCells[i].length);
        }
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        refreshAccessibilityDescription();
        invalidate();
    }

    /**
     * Clears render-only lesson hints from the board.
     */
    public void clearHighlights() {
        highlightedCells = null;
        targetRow = -1;
        targetCol = -1;
        refreshAccessibilityDescription();
        invalidate();
    }

    /**
     * Checks whether the board should reject user commands.
     *
     * @return {@code true} while animation, solver playback, or an external lock is active
     */
    public boolean isBusy() {
        return inputLocked || isAnimating || !moveQueue.isEmpty();
    }

    /**
     * Locks or unlocks touch input while controller-owned work is in progress.
     *
     * @param inputLocked {@code true} to reject gestures until unlocked
     */
    public void setInputLocked(boolean inputLocked) {
        this.inputLocked = inputLocked;
        refreshAccessibilityDescription();
        notifyBusyStateChanged();
    }

    /**
     * Registers a callback for animation, solver playback, and input lock changes.
     *
     * @param busyStateListener callback to run when {@link #isBusy()} may have changed
     */
    public void setBusyStateListener(Runnable busyStateListener) {
        this.busyStateListener = busyStateListener;
    }

    /**
     * Enables or disables board movement animation for reduced-motion users.
     *
     * @param reducedMotionEnabled {@code true} to complete moves without transition animation
     */
    public void setReducedMotionEnabled(boolean reducedMotionEnabled) {
        this.reducedMotionEnabled = reducedMotionEnabled;
    }

    /**
     * Queues solver-generated single-step moves for animated playback.
     *
     * @param dirs ordered empty-tile movement directions
     */
    public void enqueueMoves(List<Direction> dirs) {
        if (model == null || model.isSolved() || dirs == null || dirs.isEmpty()) {
            return;
        }
        moveQueue.addAll(dirs);
        refreshAccessibilityDescription();
        notifyBusyStateChanged();
        playQueuedMove();
    }

    /**
     * Draws the board, static tiles, and any active animation frame.
     *
     * @param canvas Android canvas supplied by the rendering pipeline
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (model == null) {
            return;
        }
        int size = model.getSize();
        if (size <= 0) {
            return;
        }

        float available = Math.min(getWidth(), getHeight());
        tileSize = (available - (size + 1) * gap) / size;
        if (tileSize <= 0) {
            return;
        }

        float boardSize = size * tileSize + (size + 1) * gap;
        boardLeft = (getWidth() - boardSize) / 2f;
        boardTop = (getHeight() - boardSize) / 2f;

        rect.set(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize);
        canvas.drawRoundRect(rect, dp(14), dp(14), boardPaint);

        textPaint.setTextSize(tileSize * 0.42f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float textOffset = -(metrics.ascent + metrics.descent) / 2f;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                boolean highlighted = isHighlightedCell(r, c);
                if (highlighted) {
                    drawCellHint(canvas, r, c, false);
                }
                if (shouldSkipCell(r, c)) {
                    continue;
                }
                int value = model.getTile(r, c);
                if (value != 0) {
                    drawTile(canvas, r, c, value, textOffset);
                } else if (!isAnimating) {
                    drawEmptyCell(canvas, r, c);
                }
                if (highlighted) {
                    drawCellHint(canvas, r, c, true);
                }
            }
        }

        if (isAnimating && !animatedTiles.isEmpty()) {
            for (AnimatedTile tile : animatedTiles) {
                float row = lerp(tile.fromRow, tile.toRow, animationProgress);
                float col = lerp(tile.fromCol, tile.toCol, animationProgress);
                drawTileAt(canvas, row, col, tile.value, textOffset);
            }
        }

        if (isAnimating && movingValue != 0) {
            float row = lerp(animFromRow, animToRow, animationProgress);
            float col = lerp(animFromCol, animToCol, animationProgress);
            drawTileAt(canvas, row, col, movingValue, textOffset);
        }
    }

    private boolean shouldSkipCell(int row, int col) {
        if (row == skipRow && col == skipCol) {
            return true;
        }
        for (AnimatedTile tile : animatedTiles) {
            if (tile.toRow == row && tile.toCol == col) {
                return true;
            }
        }
        return false;
    }

    private void drawTile(Canvas canvas, float row, float col, int value, float textOffset) {
        drawTileAt(canvas, row, col, value, textOffset);
    }

    private void drawTileAt(Canvas canvas, float row, float col, int value, float textOffset) {
        float x = boardLeft + gap + col * (tileSize + gap);
        float y = boardTop + gap + row * (tileSize + gap);
        rect.set(x, y, x + tileSize, y + tileSize);
        canvas.drawRoundRect(rect, dp(10), dp(10), tilePaint);

        RectF shine = new RectF(x, y, x + tileSize, y + tileSize * 0.45f);
        canvas.drawRoundRect(shine, dp(10), dp(10), tileHighlightPaint);
        canvas.drawText(String.valueOf(value), x + tileSize / 2f, y + tileSize / 2f + textOffset, textPaint);
    }

    private void drawCellHint(Canvas canvas, int row, int col, boolean strokeOnly) {
        float x = boardLeft + gap + col * (tileSize + gap);
        float y = boardTop + gap + row * (tileSize + gap);
        float inset = strokeOnly ? dp(2) : 0f;
        rect.set(x - inset, y - inset, x + tileSize + inset, y + tileSize + inset);
        if (!strokeOnly) {
            canvas.drawRoundRect(rect, dp(12), dp(12), hintFillPaint);
            return;
        }
        canvas.drawRoundRect(rect, dp(12), dp(12),
                row == targetRow && col == targetCol ? targetStrokePaint : hintStrokePaint);
    }

    private void drawEmptyCell(Canvas canvas, int row, int col) {
        float x = boardLeft + gap + col * (tileSize + gap);
        float y = boardTop + gap + row * (tileSize + gap);
        float inset = Math.max(dp(5), tileSize * 0.08f);
        rect.set(x + inset, y + inset, x + tileSize - inset, y + tileSize - inset);
        canvas.drawRoundRect(rect, dp(8), dp(8), emptyCellPaint);
        canvas.drawCircle(x + tileSize / 2f, y + tileSize / 2f,
                Math.max(dp(2), tileSize * 0.035f), emptyDotPaint);
    }

    private boolean isHighlightedCell(int row, int col) {
        return highlightedCells != null
                && row >= 0
                && row < highlightedCells.length
                && col >= 0
                && col < highlightedCells[row].length
                && highlightedCells[row][col];
    }

    /**
     * Handles tap and swipe gestures against the rendered board.
     *
     * @param event Android touch event
     * @return {@code true} because the view consumes board gestures
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || model == null) {
            clearTouchState();
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isBusy() || model.isSolved()) {
                    clearTouchState();
                    return true;
                }
                downX = event.getX();
                downY = event.getY();
                int[] tile = tileAt(downX, downY);
                downRow = tile[0];
                downCol = tile[1];
                trackingTouch = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (!trackingTouch || isBusy() || model.isSolved()) {
                    clearTouchState();
                    return true;
                }
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > dp(25)) {
                    handleSwipe(dx, dy);
                } else {
                    performClick();
                    int[] tapped = tileAt(event.getX(), event.getY());
                    enqueueMovesToTile(tapped[0], tapped[1]);
                }
                clearTouchState();
                return true;
            case MotionEvent.ACTION_CANCEL:
                clearTouchState();
                return true;
            default:
                return true;
        }
    }

    /**
     * Supports Android accessibility click dispatch for tap gestures.
     *
     * @return {@code true} after delegating to the base implementation
     */
    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void clearTouchState() {
        trackingTouch = false;
        downRow = -1;
        downCol = -1;
        downX = 0f;
        downY = 0f;
    }

    private void handleSwipe(float dx, float dy) {
        if (downRow < 0 || downCol < 0) {
            return;
        }

        Direction tileDirection;
        if (Math.abs(dx) > Math.abs(dy)) {
            tileDirection = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            tileDirection = dy > 0 ? Direction.DOWN : Direction.UP;
        }

        if (canSlideTile(downRow, downCol, tileDirection)) {
            slideLineToTile(downRow, downCol);
        }
    }

    private boolean canSlideTile(int row, int col, Direction tileDirection) {
        return switch (tileDirection) {
            case UP -> col == model.getEmptyCol() && model.getEmptyRow() < row;
            case DOWN -> col == model.getEmptyCol() && model.getEmptyRow() > row;
            case LEFT -> row == model.getEmptyRow() && model.getEmptyCol() < col;
            case RIGHT -> row == model.getEmptyRow() && model.getEmptyCol() > col;
        };
    }

    private void enqueueMovesToTile(int row, int col) {
        if (row < 0 || col < 0 || (row == model.getEmptyRow() && col == model.getEmptyCol())) {
            return;
        }

        if (row == model.getEmptyRow()) {
            slideLineToTile(row, col);
        } else if (col == model.getEmptyCol()) {
            slideLineToTile(row, col);
        }
    }

    private void slideLineToTile(int row, int col) {
        if (isBusy()) {
            return;
        }
        if (model.slideLineTo(row, col)) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void playQueuedMove() {
        if (isAnimating || moveQueue.isEmpty() || model.isSolved()) {
            return;
        }

        Direction dir = moveQueue.poll();
        if (!model.move(dir)) {
            playQueuedMove();
        }
    }

    private int[] tileAt(float x, float y) {
        int size = model.getSize();
        float boardSize = size * tileSize + (size + 1) * gap;
        if (tileSize <= 0 || x < boardLeft || x > boardLeft + boardSize || y < boardTop || y > boardTop + boardSize) {
            return new int[]{-1, -1};
        }

        float localX = x - boardLeft;
        float localY = y - boardTop;
        if (localX < gap || localY < gap) {
            return new int[]{-1, -1};
        }

        float stride = tileSize + gap;
        int col = (int) ((localX - gap) / stride);
        int row = (int) ((localY - gap) / stride);
        float inCellX = (localX - gap) % stride;
        float inCellY = (localY - gap) % stride;
        if (row >= size || col >= size || inCellX >= tileSize || inCellY >= tileSize) {
            return new int[]{-1, -1};
        }
        return new int[]{row, col};
    }

    /**
     * Redraws the board after model changes that are not already animated.
     */
    @Override
    public void onGridChanged() {
        refreshAccessibilityDescription();
        if (!isAnimating) {
            invalidate();
        }
    }

    /**
     * Animates one numbered tile after a single empty-cell move.
     *
     * @param dir direction the empty tile moved
     */
    @Override
    public void onMove(Direction dir) {
        int newEmptyR = model.getEmptyRow();
        int newEmptyC = model.getEmptyCol();
        int oldEmptyR = newEmptyR - dir.dRow;
        int oldEmptyC = newEmptyC - dir.dCol;

        movingValue = model.getTile(oldEmptyR, oldEmptyC);
        skipRow = oldEmptyR;
        skipCol = oldEmptyC;
        animatedTiles.clear();
        animFromRow = newEmptyR;
        animFromCol = newEmptyC;
        animToRow = oldEmptyR;
        animToCol = oldEmptyC;
        animationProgress = 0f;
        isAnimating = true;
        refreshAccessibilityDescription();
        notifyBusyStateChanged();
        if (reducedMotionEnabled) {
            finishSingleMoveAnimation();
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(MOVE_ANIMATION_DURATION_MS);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(valueAnimator -> {
            animationProgress = (float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finishSingleMoveAnimation();
            }
        });
        animator.start();
    }

    private void finishSingleMoveAnimation() {
        isAnimating = false;
        movingValue = 0;
        skipRow = -1;
        skipCol = -1;
        refreshAccessibilityDescription();
        invalidate();
        playQueuedMove();
        notifyBusyStateChanged();
    }

    /**
     * Animates all affected tiles for a whole-line slide.
     *
     * @param dir direction the empty tile moved
     * @param steps number of cells the empty tile moved
     */
    @Override
    public void onLineMove(Direction dir, int steps) {
        if (steps <= 0) {
            invalidate();
            return;
        }

        int newEmptyR = model.getEmptyRow();
        int newEmptyC = model.getEmptyCol();
        int oldEmptyR = newEmptyR - dir.dRow * steps;
        int oldEmptyC = newEmptyC - dir.dCol * steps;

        movingValue = 0;
        skipRow = -1;
        skipCol = -1;
        animatedTiles.clear();

        for (int i = 1; i <= steps; i++) {
            int fromRow = oldEmptyR + dir.dRow * i;
            int fromCol = oldEmptyC + dir.dCol * i;
            int toRow = fromRow - dir.dRow;
            int toCol = fromCol - dir.dCol;
            if (!isValidCell(fromRow, fromCol) || !isValidCell(toRow, toCol)) {
                continue;
            }
            int value = model.getTile(toRow, toCol);
            if (value != 0) {
                animatedTiles.add(new AnimatedTile(value, fromRow, fromCol, toRow, toCol));
            }
        }

        if (animatedTiles.isEmpty()) {
            invalidate();
            return;
        }

        animationProgress = 0f;
        isAnimating = true;
        refreshAccessibilityDescription();
        notifyBusyStateChanged();
        if (reducedMotionEnabled) {
            finishLineMoveAnimation();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(MOVE_ANIMATION_DURATION_MS);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(valueAnimator -> {
            animationProgress = (float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finishLineMoveAnimation();
            }
        });
        animator.start();
    }

    private void finishLineMoveAnimation() {
        isAnimating = false;
        animatedTiles.clear();
        refreshAccessibilityDescription();
        invalidate();
        playQueuedMove();
        notifyBusyStateChanged();
    }

    /**
     * Stops queued solver playback when the puzzle is solved.
     *
     * @param moves final move count
     * @param timeMs elapsed play time in milliseconds
     */
    @Override
    public void onGameWon(int moves, long timeMs) {
        moveQueue.clear();
        refreshAccessibilityDescription();
        notifyBusyStateChanged();
    }

    private void refreshAccessibilityDescription() {
        if (model == null) {
            setContentDescription(getResources().getString(R.string.board_accessibility_no_game));
            return;
        }

        int size = model.getSize();
        int moves = model.getMoveCount();
        String moveText = getResources().getQuantityString(R.plurals.moves_count, moves, moves);
        String description = getResources().getString(
                R.string.board_accessibility_description,
                size,
                size,
                model.getEmptyRow() + 1,
                model.getEmptyCol() + 1,
                moveText,
                buildRowsDescription());

        int highlightCount = countHighlightedCells();
        if (highlightCount > 0) {
            description += " " + getResources().getQuantityString(
                    R.plurals.board_accessibility_highlight_count,
                    highlightCount,
                    highlightCount);
        }
        if (isBusy()) {
            description += " " + getResources().getString(R.string.board_accessibility_busy);
        }
        setContentDescription(description);
    }

    private String buildRowsDescription() {
        int size = model.getSize();
        StringBuilder rows = new StringBuilder(getResources().getString(R.string.board_accessibility_rows_prefix));
        for (int row = 0; row < size; row++) {
            if (row > 0) {
                rows.append("; ");
            }
            rows.append(getResources().getString(R.string.board_accessibility_row_prefix, row + 1));
            for (int col = 0; col < size; col++) {
                if (col > 0) {
                    rows.append(", ");
                }
                int value = model.getTile(row, col);
                if (value == 0) {
                    rows.append(getResources().getString(R.string.board_empty_cell_short));
                } else {
                    rows.append(value);
                }
            }
        }
        rows.append(".");
        return rows.toString();
    }

    private int countHighlightedCells() {
        if (highlightedCells == null) {
            return 0;
        }
        int count = 0;
        for (boolean[] row : highlightedCells) {
            for (boolean cell : row) {
                if (cell) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isValidCell(int row, int col) {
        return row >= 0 && row < model.getSize() && col >= 0 && col < model.getSize();
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void notifyBusyStateChanged() {
        if (busyStateListener != null) {
            busyStateListener.run();
        }
    }

    /**
     * Render-only description of one tile participating in a synchronized line animation.
     */
    private static class AnimatedTile {
        final int value;
        final int fromRow;
        final int fromCol;
        final int toRow;
        final int toCol;

        AnimatedTile(int value, int fromRow, int fromCol, int toRow, int toCol) {
            this.value = value;
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
        }
    }
}
