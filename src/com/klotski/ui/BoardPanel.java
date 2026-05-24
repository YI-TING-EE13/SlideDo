
package com.klotski.ui;

import com.klotski.core.Direction;
import com.klotski.core.GameModel;
import com.klotski.core.GameObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Swing component that renders the puzzle board and translates desktop input
 * into model moves.
 * <p>
 * The panel owns only view state: tile animation positions, mouse gesture
 * tracking, and queued solver playback. The authoritative board state remains
 * in {@link GameModel}.
 * </p>
 */
public class BoardPanel extends JPanel implements GameObserver {

    private static final boolean DEBUG_LOGGING = false;

    /** Active game model rendered by this board. */
    private GameModel model;

    /** Pixel gap between neighboring tiles. */
    private final int TILE_GAP = 10;

    /** Fill color for numbered tiles. */
    private final Color TILE_COLOR = new Color(60, 179, 113);

    /** Text color for numbered tile labels. */
    private final Color TILE_TEXT_COLOR = Color.WHITE;

    /** Panel background color outside the board. */
    private final Color BG_COLOR = new Color(40, 40, 40);

    /** Indicates whether a tile or line animation is currently active. */
    private boolean isAnimating = false;

    /** Deferred win move count while final animation is still running. */
    private Integer pendingWinMoves = null;

    /** Deferred win elapsed time while final animation is still running. */
    private long pendingWinTimeMs = 0;

    /** Render state for every board cell. */
    private Tile[][] tiles;

    /** Queued empty-tile moves used for keyboard and solver playback. */
    private final Deque<Direction> moveQueue = new ArrayDeque<>();

    /** Mouse press location used to distinguish click and swipe gestures. */
    private Point pressPoint;

    /** Board cell selected at mouse press time. */
    private Point pressTile;

    /** Tracks whether mouse press already triggered movement. */
    private boolean pressTriggeredMove = false;

    /** Prevents duplicate movement from the follow-up mouseClicked event. */
    private boolean suppressNextMouseClicked = false;

    /**
     * Creates a board view bound to the supplied model.
     *
     * @param model initial game model to observe and render
     */
    public BoardPanel(GameModel model) {
        this.model = model;
        model.addObserver(this);
        setBackground(BG_COLOR);
        setFocusable(true);
        setupKeyBindings();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                suppressNextMouseClicked = false;
                pressPoint = e.getPoint();
                pressTile = getTileAt(e.getX(), e.getY());
                pressTriggeredMove = false;
                logMouse("pressed", e.getX(), e.getY(), pressTile);
                if (SwingUtilities.isLeftMouseButton(e) && pressTile != null && isMovableTile(pressTile.x, pressTile.y)) {
                    log("press-triggered click tile=(" + pressTile.x + "," + pressTile.y + ")");
                    slideLineToTile(pressTile.x, pressTile.y);
                    pressTriggeredMove = true;
                    suppressNextMouseClicked = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressTriggeredMove) {
                    logMouse("released-skipped-press-triggered", e.getX(), e.getY(), getTileAt(e.getX(), e.getY()));
                    pressPoint = null;
                    pressTile = null;
                    pressTriggeredMove = false;
                    return;
                }

                if (model.isSolved()) {
                    logMouse("released-solved-ignored", e.getX(), e.getY(), getTileAt(e.getX(), e.getY()));
                    pressPoint = null;
                    pressTile = null;
                    return;
                }

                if (pressPoint == null) {
                    log("mouseReleased fallback-click x=" + e.getX()
                            + " y=" + e.getY()
                            + " tile=" + formatPoint(getTileAt(e.getX(), e.getY()))
                            + " empty=(" + model.getEmptyRow() + "," + model.getEmptyCol() + ")");
                    handleMouseClick(e.getX(), e.getY());
                    pressTile = null;
                    return;
                }

                int dx = e.getX() - pressPoint.x;
                int dy = e.getY() - pressPoint.y;
                Point releasedTile = getTileAt(e.getX(), e.getY());
                log("mouseReleased x=" + e.getX()
                        + " y=" + e.getY()
                        + " releasedTile=" + formatPoint(releasedTile)
                        + " pressTile=" + formatPoint(pressTile)
                        + " dx=" + dx
                        + " dy=" + dy
                        + " empty=(" + model.getEmptyRow() + "," + model.getEmptyCol() + ")"
                        + " solved=" + model.isSolved()
                        + " running=" + model.isGameRunning());
                if (Math.max(Math.abs(dx), Math.abs(dy)) > 25) {
                    handleSwipe(dx, dy);
                } else {
                    handleMouseClick(e.getX(), e.getY());
                }
                pressPoint = null;
                pressTile = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (suppressNextMouseClicked) {
                    logMouse("clicked-suppressed", e.getX(), e.getY(), getTileAt(e.getX(), e.getY()));
                    suppressNextMouseClicked = false;
                    return;
                }

                logMouse("clicked-fallback", e.getX(), e.getY(), getTileAt(e.getX(), e.getY()));
                if (SwingUtilities.isLeftMouseButton(e) && !model.isSolved()) {
                    handleMouseClick(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point tile = getTileAt(e.getX(), e.getY());
                boolean movable = tile != null && isMovableTile(tile.x, tile.y);
                setCursor(Cursor.getPredefinedCursor(movable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        initTiles();
    }

    private void setupKeyBindings() {
        bindKey("moveUp", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), Direction.UP);
        bindKey("moveDown", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), Direction.DOWN);
        bindKey("moveLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), Direction.LEFT);
        bindKey("moveRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), Direction.RIGHT);
    }

    private void bindKey(String name, KeyStroke keyStroke, Direction dir) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                enqueueMove(dir.opposite());
            }
        });
    }

    private void initTiles() {
        int size = model.getSize();
        tiles = new Tile[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                tiles[r][c] = new Tile(model.getTile(r, c), r, c);
            }
        }
    }

    /**
     * Rebinds the panel to a different game model.
     * <p>
     * Used when the player starts or loads a puzzle with a different size.
     * Pending animations are cleared and the rendered tiles are rebuilt.
     * </p>
     *
     * @param model replacement game model
     */
    public void setModel(GameModel model) {
        if (this.model != null) {
            this.model.removeObserver(this);
        }
        moveQueue.clear();
        isAnimating = false;
        pendingWinMoves = null;
        this.model = model;
        this.model.addObserver(this);
        initTiles();
        repaint();
    }

    private void handleMouseClick(int x, int y) {
        Point tilePoint = getTileAt(x, y);
        if (tilePoint == null) {
            log("click ignored: outside board x=" + x + " y=" + y);
            return;
        }
        log("click tile=(" + tilePoint.x + "," + tilePoint.y + ") value=" + model.getTile(tilePoint.x, tilePoint.y)
                + " empty=(" + model.getEmptyRow() + "," + model.getEmptyCol() + ") movable="
                + isMovableTile(tilePoint.x, tilePoint.y));
        slideLineToTile(tilePoint.x, tilePoint.y);
    }

    private void handleSwipe(int dx, int dy) {
        if (pressTile == null) {
            log("swipe ignored: pressTile is null");
            return;
        }

        Direction tileDirection;
        if (Math.abs(dx) > Math.abs(dy)) {
            tileDirection = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            tileDirection = dy > 0 ? Direction.DOWN : Direction.UP;
        }

        int row = pressTile.x;
        int col = pressTile.y;
        Direction emptyDirection = tileDirection.opposite();
        log("swipe tile=(" + row + "," + col + ") tileDirection=" + tileDirection
                + " emptyDirection=" + emptyDirection + " canSlide=" + canSlideTile(row, col, tileDirection));
        if (canSlideTile(row, col, tileDirection)) {
            slideLineToTile(row, col);
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

    private boolean isMovableTile(int row, int col) {
        return model.isGameRunning()
                && !isBusy()
                && !(row == model.getEmptyRow() && col == model.getEmptyCol())
                && (row == model.getEmptyRow() || col == model.getEmptyCol());
    }

    private void slideLineToTile(int row, int col) {
        if (isBusy()) {
            log("slide ignored: board is busy");
            return;
        }

        if (row == model.getEmptyRow() && col == model.getEmptyCol()) {
            log("slide ignored: clicked empty tile");
            return;
        }

        if (row == model.getEmptyRow()) {
            Direction dir = col < model.getEmptyCol() ? Direction.LEFT : Direction.RIGHT;
            log("slide row steps=" + Math.abs(col - model.getEmptyCol()) + " dir=" + dir);
            log("slide result moved=" + model.slideLineTo(row, col));
        } else if (col == model.getEmptyCol()) {
            Direction dir = row < model.getEmptyRow() ? Direction.UP : Direction.DOWN;
            log("slide column steps=" + Math.abs(row - model.getEmptyRow()) + " dir=" + dir);
            log("slide result moved=" + model.slideLineTo(row, col));
        } else {
            log("slide ignored: tile is not aligned with empty");
        }
    }

    /**
     * Adds a single empty-tile move to the animation queue.
     *
     * @param dir direction the empty tile should move
     */
    public void enqueueMove(Direction dir) {
        if (model.isSolved()) {
            log("enqueueMove ignored: model solved");
            return;
        }
        moveQueue.add(dir);
        log("queued single move dir=" + dir + " queueSize=" + moveQueue.size());
        playQueuedMove();
    }

    /**
     * Adds a solver-generated move sequence to the animation queue.
     *
     * @param dirs ordered empty-tile moves to animate
     */
    public void enqueueMoves(List<Direction> dirs) {
        if (model.isSolved()) {
            log("enqueueMoves ignored: model solved");
            return;
        }
        moveQueue.addAll(dirs);
        log("queued solution moves count=" + dirs.size() + " queueSize=" + moveQueue.size());
        playQueuedMove();
    }

    /**
     * Checks whether the board is currently animating or playing queued moves.
     *
     * @return {@code true} when user actions should be temporarily ignored
     */
    public boolean isBusy() {
        return isAnimating || !moveQueue.isEmpty();
    }

    private void playQueuedMove() {
        if (isAnimating || moveQueue.isEmpty() || model.isSolved()) {
            log("playQueuedMove skipped animating=" + isAnimating + " queueSize=" + moveQueue.size()
                    + " solved=" + model.isSolved());
            return;
        }

        Direction dir = moveQueue.poll();
        boolean moved = model.move(dir);
        log("playQueuedMove dir=" + dir + " moved=" + moved
                + " empty=(" + model.getEmptyRow() + "," + model.getEmptyCol() + ")"
                + " moves=" + model.getMoveCount());
        if (!moved) {
            playQueuedMove();
        }
    }

    private Point getTileAt(int x, int y) {
        int size = model.getSize();
        if (size == 0) return null;

        int panelW = getWidth();
        int panelH = getHeight();
        int tileSize = Math.min((panelW - (size + 1) * TILE_GAP) / size, (panelH - (size + 1) * TILE_GAP) / size);
        int boardW = size * tileSize + (size + 1) * TILE_GAP;
        int boardH = size * tileSize + (size + 1) * TILE_GAP;
        int startX = (panelW - boardW) / 2;
        int startY = (panelH - boardH) / 2;

        if (tileSize <= 0) return null;
        if (x < startX || x > startX + boardW || y < startY || y > startY + boardH) return null;

        int localX = x - startX;
        int localY = y - startY;
        if (localX < TILE_GAP || localY < TILE_GAP) return null;

        int stride = tileSize + TILE_GAP;
        int col = (localX - TILE_GAP) / stride;
        int row = (localY - TILE_GAP) / stride;
        int inCellX = (localX - TILE_GAP) % stride;
        int inCellY = (localY - TILE_GAP) % stride;

        if (row >= size || col >= size || inCellX >= tileSize || inCellY >= tileSize) return null;

        return new Point(row, col);
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < model.getSize() && c >= 0 && c < model.getSize();
    }

    private void logMouse(String event, int x, int y, Point tile) {
        log("mouse " + event
                + " x=" + x
                + " y=" + y
                + " tile=" + formatPoint(tile)
                + " empty=(" + model.getEmptyRow() + "," + model.getEmptyCol() + ")"
                + " solved=" + model.isSolved()
                + " running=" + model.isGameRunning());
    }

    private String formatPoint(Point point) {
        return point == null ? "null" : "(" + point.x + "," + point.y + ")";
    }

    private void log(String message) {
        if (!DEBUG_LOGGING) {
            return;
        }
        try (PrintWriter out = new PrintWriter(new FileWriter("klotski_debug.log", true))) {
            out.println(System.currentTimeMillis() + " " + message);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onGridChanged() {
        if (!isAnimating) {
            if (tiles == null || tiles.length != model.getSize()) {
                initTiles();
            }
            for (int r = 0; r < model.getSize(); r++) {
                for (int c = 0; c < model.getSize(); c++) {
                    tiles[r][c].value = model.getTile(r, c);
                    tiles[r][c].snapTo(r, c);
                }
            }
            repaint();
        }
    }

    @Override
    public void onMove(Direction dir) {
        isAnimating = true;
        int newEmptyR = model.getEmptyRow();
        int newEmptyC = model.getEmptyCol();
        int oldEmptyR = newEmptyR - dir.dRow;
        int oldEmptyC = newEmptyC - dir.dCol;

        Tile tile = tiles[newEmptyR][newEmptyC];
        tile.animate(oldEmptyR, oldEmptyC, () -> {
            isAnimating = false;
            onGridChanged();
            if (!showPendingWinIfNeeded()) {
                playQueuedMove();
            }
        });
    }

    @Override
    public void onLineMove(Direction dir, int steps) {
        if (steps <= 0) {
            onGridChanged();
            return;
        }

        isAnimating = true;
        int newEmptyR = model.getEmptyRow();
        int newEmptyC = model.getEmptyCol();
        int oldEmptyR = newEmptyR - dir.dRow * steps;
        int oldEmptyC = newEmptyC - dir.dCol * steps;
        int[] remaining = {steps};

        log("onLineMove dir=" + dir + " steps=" + steps
                + " oldEmpty=(" + oldEmptyR + "," + oldEmptyC + ")"
                + " newEmpty=(" + newEmptyR + "," + newEmptyC + ")");

        for (int i = 1; i <= steps; i++) {
            int sourceR = oldEmptyR + dir.dRow * i;
            int sourceC = oldEmptyC + dir.dCol * i;
            int targetR = sourceR - dir.dRow;
            int targetC = sourceC - dir.dCol;

            if (!isValid(sourceR, sourceC) || !isValid(targetR, targetC)) {
                remaining[0]--;
                continue;
            }

            Tile tile = tiles[sourceR][sourceC];
            tile.animate(targetR, targetC, () -> {
                remaining[0]--;
                if (remaining[0] == 0) {
                    isAnimating = false;
                    onGridChanged();
                    if (!showPendingWinIfNeeded()) {
                        playQueuedMove();
                    }
                }
            });
        }

        if (remaining[0] == 0) {
            isAnimating = false;
            onGridChanged();
            if (!showPendingWinIfNeeded()) {
                playQueuedMove();
            }
        }
    }

    @Override
    public void onGameWon(int moves, long timeMs) {
        if (isAnimating) {
            pendingWinMoves = moves;
            pendingWinTimeMs = timeMs;
            return;
        }
        showWinDialog(moves, timeMs);
    }

    private boolean showPendingWinIfNeeded() {
        if (pendingWinMoves != null) {
            showWinDialog(pendingWinMoves, pendingWinTimeMs);
            pendingWinMoves = null;
            return true;
        }
        return false;
    }

    private void showWinDialog(int moves, long timeMs) {
        JOptionPane.showMessageDialog(this,
                "Congratulations! You won in " + moves + " moves.\nTime: " + (timeMs / 1000) + "s",
                "Winner!", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = model.getSize();
        if (size == 0) return;

        int panelW = getWidth();
        int panelH = getHeight();
        int tileSize = Math.min((panelW - (size + 1) * TILE_GAP) / size, (panelH - (size + 1) * TILE_GAP) / size);
        int boardW = size * tileSize + (size + 1) * TILE_GAP;
        int boardH = size * tileSize + (size + 1) * TILE_GAP;
        int startX = (panelW - boardW) / 2;
        int startY = (panelH - boardH) / 2;

        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(startX, startY, boardW, boardH, 15, 15);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                tiles[r][c].draw(g2, startX, startY, tileSize);
            }
        }
    }

    private void drawTile(Graphics2D g2, int x, int y, int size, int val) {
        g2.setColor(TILE_COLOR);
        g2.fillRoundRect(x, y, size, size, 10, 10);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.fillRoundRect(x, y, size, size / 2, 10, 10);
        g2.setColor(TILE_TEXT_COLOR);
        g2.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        String text = String.valueOf(val);
        int textX = x + (size - fm.stringWidth(text)) / 2;
        int textY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, textX, textY);
    }

    /**
     * Lightweight render object for one tile.
     */
    private class Tile {
        public int value;
        private double currentR, currentC;
        private int targetR, targetC;
        private Timer timer;

        public Tile(int value, int r, int c) {
            this.value = value;
            this.currentR = this.targetR = r;
            this.currentC = this.targetC = c;
        }

        public void snapTo(int r, int c) {
            this.currentR = this.targetR = r;
            this.currentC = this.targetC = c;
        }

        public void animate(int r, int c, Runnable onFinish) {
            this.targetR = r;
            this.targetC = c;
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            timer = new Timer(10, e -> {
                currentR += (targetR - currentR) * 0.2;
                currentC += (targetC - currentC) * 0.2;
                if (Math.abs(currentR - targetR) < 0.01 && Math.abs(currentC - targetC) < 0.01) {
                    currentR = targetR;
                    currentC = targetC;
                    timer.stop();
                    onFinish.run();
                }
                repaint();
            });
            timer.start();
        }

        public void draw(Graphics2D g2, int startX, int startY, int tileSize) {
            if (value == 0) return;
            int x = startX + TILE_GAP + (int) (currentC * (tileSize + TILE_GAP));
            int y = startY + TILE_GAP + (int) (currentR * (tileSize + TILE_GAP));
            drawTile(g2, x, y, tileSize, value);
        }
    }
}

