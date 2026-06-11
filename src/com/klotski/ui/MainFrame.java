package com.klotski.ui;

import com.klotski.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Main desktop window for the Swing edition.
 * <p>
 * The frame wires menus, status text, solver actions, save/load commands, and
 * the {@link BoardPanel}. The rules remain inside {@link GameModel}; this class
 * acts as desktop-specific application glue.
 * </p>
 */
public class MainFrame extends JFrame implements GameObserver {
    private static final String HOME_CARD = "home";
    private static final String GAME_CARD = "game";

    /** Active game model backing the desktop window. */
    private GameModel model;

    /** Swing board component responsible for rendering and input. */
    private BoardPanel boardPanel;

    /** Card layout that switches between the desktop home and game board. */
    private CardLayout contentLayout;

    /** Container managed by {@link #contentLayout}. */
    private JPanel contentPanel;

    /** Status label showing moves, timer, and best record. */
    private JLabel statusLabel;

    /** Timer that refreshes desktop status text once per second. */
    private Timer gameTimer;

    /** Desktop UI timer anchor used for status display. */
    private long startTime;

    /** Tracks whether the current completion was produced by solver playback. */
    private boolean assistedSolveActive;

    /** Tracks whether desktop assist highlights are currently visible. */
    private boolean movableHintActive;

    /** Tracks whether the board card is visible. */
    private boolean showingGame;

    /** Desktop presentation preference for snapping tile movement. */
    private boolean reducedMotionEnabled;

    /** Prepared Results message shown after the board finishes its win animation. */
    private String pendingResultMessage;

    /**
     * Creates and shows the desktop application window.
     */
    public MainFrame() {
        setTitle("Number Klotski - Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(null);

        // Initialize Model
        model = new GameModel(4); // Default 4x4
        model.addObserver(this);

        // Initialize View
        boardPanel = new BoardPanel(model);
        boardPanel.setWinDialogHandler((parent, moves, timeMs) -> showResultsDialog(moves, timeMs));

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.add(createHomePanel(), HOME_CARD);
        contentPanel.add(boardPanel, GAME_CARD);

        statusLabel = new JLabel("Moves: 0 | Time: 0s");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        // Layout
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Menus
        setupMenu();

        // Timer for UI update
        gameTimer = new Timer(1000, e -> updateStatus());

        showHome();
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Game Menu
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGame3 = new JMenuItem("New 3x3");
        newGame3.addActionListener(e -> startNewGame(3));
        gameMenu.add(newGame3);

        JMenuItem newGame4 = new JMenuItem("New 4x4");
        newGame4.addActionListener(e -> startNewGame(4));
        gameMenu.add(newGame4);

        JMenuItem newGame5 = new JMenuItem("New 5x5");
        newGame5.addActionListener(e -> startNewGame(5));
        gameMenu.add(newGame5);

        gameMenu.addSeparator();

        JMenuItem restartItem = new JMenuItem("Restart This Puzzle");
        restartItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        restartItem.addActionListener(e -> restartCurrentGame());
        gameMenu.add(restartItem);

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undoMove());
        gameMenu.add(undoItem);

        gameMenu.addSeparator();

        JMenuItem saveItem = new JMenuItem("Save Game");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> {
            if (!showingGame) {
                JOptionPane.showMessageDialog(this, "Start or load a game first.");
                return;
            }
            boolean saved = SaveManager.saveGame(model);
            JOptionPane.showMessageDialog(this, saved ? "Game saved." : "Could not save game.");
        });
        gameMenu.add(saveItem);

        JMenuItem loadItem = new JMenuItem("Load Game");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(e -> loadGame());
        gameMenu.add(loadItem);

        JMenuItem recordsItem = new JMenuItem("Records");
        recordsItem.addActionListener(e -> showRecordsDialog());
        gameMenu.add(recordsItem);

        JMenuItem preferencesItem = new JMenuItem("Preferences");
        preferencesItem.addActionListener(e -> showPreferencesDialog());
        gameMenu.add(preferencesItem);

        gameMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);

        // Assist Menu
        JMenu assistMenu = new JMenu("Assist");

        JMenuItem showMovableItem = new JMenuItem("Show Movable Tiles");
        showMovableItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
        showMovableItem.addActionListener(e -> showMovableTiles());
        assistMenu.add(showMovableItem);

        menuBar.add(assistMenu);

        // Solver Menu
        JMenu solverMenu = new JMenu("Solver");

        JMenuItem bfsItem = new JMenuItem("Solve with BFS (Best for 3x3)");
        bfsItem.addActionListener(e -> runSolver(new BfsSolver()));
        solverMenu.add(bfsItem);

        JMenuItem aStarItem = new JMenuItem("Solve with A* (Best for 4x4+)");
        aStarItem.addActionListener(e -> runSolver(new AStarSolver()));
        solverMenu.add(aStarItem);

        JMenuItem idaStarItem = new JMenuItem("Solve with IDA* (Mobile-friendly core)");
        idaStarItem.addActionListener(e -> runSolver(new IdaStarSolver()));
        solverMenu.add(idaStarItem);

        menuBar.add(solverMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");

        JMenuItem howToPlayItem = new JMenuItem("How to Play");
        howToPlayItem.addActionListener(e -> showHelpDialog("How to Play", DesktopHelpContent.howToPlay()));
        helpMenu.add(howToPlayItem);

        JMenuItem practiceTutorialItem = new JMenuItem("Practice Tutorial");
        practiceTutorialItem.addActionListener(
                e -> showHelpDialog("Practice Tutorial", DesktopHelpContent.practiceTutorial()));
        helpMenu.add(practiceTutorialItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
        panel.setBackground(new Color(245, 247, 250));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("SlideDo", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        title.setForeground(new Color(32, 40, 48));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(title, gbc);

        JLabel subtitle = new JLabel("Choose a puzzle or continue your last desktop save.", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(new Color(86, 96, 108));
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        panel.add(subtitle, gbc);

        JPanel sizePanel = new JPanel(new GridLayout(1, 3, 10, 0));
        sizePanel.setOpaque(false);
        sizePanel.add(createHomeButton("3x3", () -> startNewGame(3)));
        sizePanel.add(createHomeButton("4x4", () -> startNewGame(4)));
        sizePanel.add(createHomeButton("5x5", () -> startNewGame(5)));
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(sizePanel, gbc);

        panel.add(createHomeButton("Continue / Load", this::loadGame), nextHomeRow(gbc));
        panel.add(createHomeButton("How to Play", () -> showHelpDialog("How to Play", DesktopHelpContent.howToPlay())),
                nextHomeRow(gbc));
        panel.add(createHomeButton("Practice Tutorial",
                () -> showHelpDialog("Practice Tutorial", DesktopHelpContent.practiceTutorial())), nextHomeRow(gbc));
        panel.add(createHomeButton("Records", this::showRecordsDialog), nextHomeRow(gbc));
        panel.add(createHomeButton("Preferences", this::showPreferencesDialog), nextHomeRow(gbc));

        return panel;
    }

    private GridBagConstraints nextHomeRow(GridBagConstraints gbc) {
        GridBagConstraints row = (GridBagConstraints) gbc.clone();
        row.gridy++;
        row.insets = new Insets(0, 0, 10, 0);
        gbc.gridy = row.gridy;
        return row;
    }

    private JButton createHomeButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(120, 42));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void startNewGame(int size) {
        clearMovableHint();
        model.removeObserver(this);
        model = new GameModel(size);
        model.addObserver(this);
        boardPanel.setModel(model);

        // Scramble based on size to ensure difficulty but playability
        int scrambleMoves = size * size * 5;
        model.scramble(scrambleMoves);

        startTime = System.currentTimeMillis();
        assistedSolveActive = false;
        pendingResultMessage = null;
        showGame();
    }

    private void restartCurrentGame() {
        if (!showingGame) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        model.restartCurrentGame();
        startTime = model.getStartTime();
        assistedSolveActive = false;
        gameTimer.start();
        updateStatus();
    }

    private void undoMove() {
        if (!showingGame) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        model.undo();
        updateStatus();
    }

    private void loadGame() {
        SaveManager.SaveData data = SaveManager.loadGame();
        if (data != null) {
            clearMovableHint();
            if (model.getSize() != data.size) {
                model.removeObserver(this);
                model = new GameModel(data.size);
                model.addObserver(this);
                boardPanel.setModel(model);
            }
            model.loadState(data);
            startTime = model.getStartTime();
            assistedSolveActive = false;
            pendingResultMessage = null;
            showGame();
            JOptionPane.showMessageDialog(this, "Game loaded!");
        } else {
            JOptionPane.showMessageDialog(this, "No save file found.");
        }
    }

    private void runSolver(Solver solver) {
        if (!showingGame) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        if (model.getSize() >= 4 && solver instanceof BfsSolver) {
            int choice = JOptionPane.showConfirmDialog(this, "BFS on 4x4 or larger may crash or freeze. Continue?", "Warning",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }
        if (model.getSize() > 4 && solver instanceof AStarSolver) {
            int choice = JOptionPane.showConfirmDialog(this, "A* on 5x5 can be very slow or memory-heavy. Continue?", "Warning",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }
        if (model.getSize() > 4 && solver instanceof IdaStarSolver) {
            int choice = JOptionPane.showConfirmDialog(this, "IDA* on 5x5 can take a long time. Continue?", "Warning",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        new SwingWorker<List<Direction>, Void>() {
            @Override
            protected List<Direction> doInBackground() throws Exception {
                setTitle("Solving with " + solver.getName() + "...");
                return solver.solve(model);
            }

            @Override
            protected void done() {
                try {
                    List<Direction> solution = get();
                    if (solution != null) {
                        int choice = JOptionPane.showConfirmDialog(MainFrame.this,
                            "Solution found: " + solution.size() + " moves.\nAnimate it now?",
                                "Solution", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            assistedSolveActive = true;
                            boardPanel.enqueueMoves(solution);
                        }
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "No solution found or timed out.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setTitle("Number Klotski - Java Edition");
            }
        }.execute();
    }

    private void showMovableTiles() {
        if (!showingGame || boardPanel.isBusy() || !model.isGameRunning() || model.isSolved()) {
            return;
        }

        int size = model.getSize();
        boolean[][] highlights = new boolean[size][size];
        int emptyRow = model.getEmptyRow();
        int emptyCol = model.getEmptyCol();
        boolean hasMovableTile = false;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                boolean aligned = (r == emptyRow || c == emptyCol) && !(r == emptyRow && c == emptyCol);
                if (aligned && model.getTile(r, c) != 0) {
                    highlights[r][c] = true;
                    hasMovableTile = true;
                }
            }
        }

        if (hasMovableTile) {
            movableHintActive = true;
            boardPanel.setHighlightedCells(highlights);
            updateStatus();
        }
    }

    private void clearMovableHint() {
        movableHintActive = false;
        if (boardPanel != null) {
            boardPanel.clearHighlights();
        }
    }

    private void showHelpDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRecordsDialog() {
        String message = DesktopHomeContent.recordsSummary(
                SaveManager.getBestRecord(3),
                SaveManager.getBestRecord(4),
                SaveManager.getBestRecord(5));
        JOptionPane.showMessageDialog(this, message, "Records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showResultsDialog(int moves, long timeMs) {
        String message = pendingResultMessage == null
                ? DesktopResultContent.resultsMessage(model.getSize(), moves, timeMs,
                        false, false, null, SaveManager.getBestRecord(model.getSize()))
                : pendingResultMessage;
        pendingResultMessage = null;

        Object[] options = {"Play Again", "New Size", "Home"};
        int choice = JOptionPane.showOptionDialog(this, message, "Results",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            startNewGame(model.getSize());
        } else if (choice == 1 || choice == 2) {
            showHome();
        }
    }

    private void showPreferencesDialog() {
        JCheckBox reducedMotionBox = new JCheckBox("Reduce motion", reducedMotionEnabled);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel(DesktopHomeContent.preferencesDescription()), BorderLayout.NORTH);
        panel.add(reducedMotionBox, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Preferences",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            reducedMotionEnabled = reducedMotionBox.isSelected();
            boardPanel.setReducedMotion(reducedMotionEnabled);
            updateStatus();
        }
    }

    private void showHome() {
        showingGame = false;
        clearMovableHint();
        gameTimer.stop();
        contentLayout.show(contentPanel, HOME_CARD);
        statusLabel.setText("Home | New Game, Continue, How to Play, Records, Preferences");
    }

    private void showGame() {
        showingGame = true;
        contentLayout.show(contentPanel, GAME_CARD);
        gameTimer.start();
        updateStatus();
        SwingUtilities.invokeLater(() -> boardPanel.requestFocusInWindow());
    }

    private void updateStatus() {
        if (showingGame && model.isGameRunning()) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            SaveManager.BestRecord best = SaveManager.getBestRecord(model.getSize());
            String bestText = best == null ? "Best: --" : "Best: " + best.format();
            String hintText = movableHintActive ? " | Hint: highlighted tiles can slide into the empty cell" : "";
            String motionText = reducedMotionEnabled ? " | Reduced motion" : "";
            statusLabel.setText(String.format("Moves: %d | Time: %ds | %s%s%s",
                    model.getMoveCount(), elapsed, bestText, hintText, motionText));
        }
    }

    @Override
    public void onGridChanged() {
        boardPanel.repaint();
        clearMovableHint();
        updateStatus();
    }

    @Override
    public void onMove(Direction dir) {
        clearMovableHint();
        updateStatus();
    }

    @Override
    public void onGameWon(int moves, long timeMs) {
        gameTimer.stop();
        int size = model.getSize();
        boolean assisted = assistedSolveActive;
        SaveManager.BestRecord previousBest = SaveManager.getBestRecord(size);
        SaveManager.BestRecord candidate = new SaveManager.BestRecord(moves, timeMs);
        boolean newBest = !assisted && (previousBest == null || candidate.isBetterThan(previousBest));
        SaveManager.BestRecord best = assisted ? previousBest : SaveManager.recordBest(size, moves, timeMs);
        assistedSolveActive = false;
        pendingResultMessage = DesktopResultContent.resultsMessage(
                size, moves, timeMs, assisted, newBest, previousBest, best);
        String bestText = best == null ? "--" : best.format();
        statusLabel.setText(String.format("Solved! Moves: %d | Time: %ds | Best: %s",
                moves, timeMs / 1000, bestText));
        // BoardPanel invokes the Results dialog after the final animation ends.
    }

    /**
     * Application entry point for the desktop edition.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        // Set FlatLaf or System L&F
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(MainFrame::new);
    }
}
