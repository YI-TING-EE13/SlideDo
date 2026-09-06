package com.klotski.ui;

import com.klotski.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main desktop window for the Swing edition.
 * <p>
 * The frame wires menus, status text, solver actions, save/load commands,
 * Undo/Redo and move-history presentation, and the {@link BoardPanel}. The
 * rules remain inside {@link GameModel}; this class acts as desktop-specific
 * application glue.
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

    /** Tracks whether the current completion was produced by solver playback. */
    private boolean assistedSolveActive;

    /** Ensures duplicate observer callbacks cannot record one run twice. */
    private final DesktopCompletionTracker completionTracker = new DesktopCompletionTracker();

    /** Active dated Daily Challenge namespace, or {@code null} for normal play. */
    private String activeDailyDateId;

    /** Last selected month shown by the Desktop Daily Calendar. */
    private YearMonth dailyCalendarMonth;

    /** Preserves assisted eligibility for a solved Daily save after Results. */
    private boolean completedAssisted;

    /** Tracks whether desktop assist highlights are currently visible. */
    private boolean movableHintActive;

    /** Tracks whether the board card is visible. */
    private boolean showingGame;

    /** Desktop presentation preference for snapping tile movement. */
    private boolean reducedMotionEnabled;

    /** Prepared Results message shown after the board finishes its win animation. */
    private String pendingResultMessage;

    /** True while the Swing window has focus and can accept active play. */
    private boolean windowActive = true;

    /** Number of modal/controller operations that pause active play. */
    private int timerPauseDepth;

    /** True while a solver owns the current session. */
    private boolean solverRunning;

    /** Home summary for the independent normal save slots. */
    private JLabel continueSummaryLabel;

    /**
     * Creates and shows the desktop application window.
     */
    public MainFrame() {
        setTitle("Number Klotski - Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(null);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent event) {
                windowActive = true;
                syncGameTimerState();
            }

            @Override
            public void windowDeactivated(WindowEvent event) {
                windowActive = false;
                syncGameTimerState();
                autosaveCurrentGameIfSafe();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                autosaveCurrentGameIfSafe();
            }
        });

        model = new GameModel(4); // Default 4x4
        model.addObserver(this);

        boardPanel = new BoardPanel(model);
        boardPanel.setWinDialogHandler((parent, moves, timeMs) -> showResultsDialog(moves, timeMs));

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.add(createHomePanel(), HOME_CARD);
        contentPanel.add(boardPanel, GAME_CARD);

        statusLabel = new JLabel("Moves: 0 | Time: 0s");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setupMenu();

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

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> redoMove());
        gameMenu.add(redoItem);

        JMenuItem moveHistoryItem = new JMenuItem("Move History");
        moveHistoryItem.addActionListener(e -> showMoveHistoryDialog());
        gameMenu.add(moveHistoryItem);

        gameMenu.addSeparator();

        JMenuItem saveItem = new JMenuItem("Save Game");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> {
            if (solverRunning) {
                return;
            }
            if (!showingGame) {
                showMessageDialog("Start or load a game first.",
                        "Save Game", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            runWithPausedTimer(() -> {
                boolean saved = saveCurrentGame();
                showMessageDialog(saved ? "Game saved." : "Could not save game.",
                        "Save Game", JOptionPane.INFORMATION_MESSAGE);
            });
        });
        gameMenu.add(saveItem);

        JMenuItem loadItem = new JMenuItem("Load Game");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(e -> loadGame());
        gameMenu.add(loadItem);

        JMenuItem recordsItem = new JMenuItem("Records");
        recordsItem.addActionListener(e -> showRecordsDialog());
        gameMenu.add(recordsItem);

        JMenuItem dailyItem = new JMenuItem("Daily Calendar");
        dailyItem.addActionListener(e -> showDailyCalendarDialog());
        gameMenu.add(dailyItem);

        JMenuItem preferencesItem = new JMenuItem("Preferences");
        preferencesItem.addActionListener(e -> showPreferencesDialog());
        gameMenu.add(preferencesItem);

        gameMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> {
            autosaveCurrentGameIfSafe();
            System.exit(0);
        });
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

        JLabel subtitle = new JLabel("Choose a puzzle, daily challenge, or continue your last desktop save.",
                SwingConstants.CENTER);
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
        continueSummaryLabel = new JLabel("", SwingConstants.CENTER);
        continueSummaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        continueSummaryLabel.setForeground(new Color(86, 96, 108));
        panel.add(continueSummaryLabel, nextHomeRow(gbc));
        panel.add(createHomeButton("Daily Calendar", this::showDailyCalendarDialog), nextHomeRow(gbc));
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
        if (solverRunning) {
            return;
        }
        PuzzleDifficulty difficulty = chooseDifficulty(size);
        if (difficulty != null) {
            startNewGame(size, difficulty);
        }
    }

    private void startNewGame(int size, PuzzleDifficulty difficulty) {
        if (solverRunning) {
            return;
        }
        autosaveCurrentGameIfSafe();
        clearMovableHint();
        model.removeObserver(this);
        model = DesktopGameFactory.create(size, difficulty);
        model.addObserver(this);
        boardPanel.setModel(model);

        activeDailyDateId = null;
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        showGame();
    }

    private PuzzleDifficulty chooseDifficulty(int size) {
        String[] options = new String[PuzzleDifficulty.values().length];
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        for (int index = 0; index < difficulties.length; index++) {
            PuzzleDifficulty difficulty = difficulties[index];
            options[index] = difficultyLabel(difficulty) + " — "
                    + difficulty.scrambleMovesForSize(size) + " scramble moves";
        }

        int choice = showOptionDialog(
                "Choose a difficulty for the " + size + "x" + size + " puzzle.",
                "Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[PuzzleDifficulty.CLASSIC.ordinal()]);
        if (choice < 0 || choice >= difficulties.length) {
            return null;
        }
        return difficulties[choice];
    }

    private String difficultyLabel(PuzzleDifficulty difficulty) {
        return switch (difficulty) {
            case RELAXED -> "Relaxed";
            case CLASSIC -> "Classic";
            case CHALLENGE -> "Challenge";
        };
    }

    static GameModel createReplayModel(GameModel completedModel) {
        return PuzzleIdentity.from(completedModel).createGame();
    }

    private void replayCurrentPuzzle() {
        if (model == null || solverRunning) {
            return;
        }
        clearMovableHint();
        model.removeObserver(this);
        model = createReplayModel(model);
        model.addObserver(this);
        boardPanel.setModel(model);
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        showGame();
    }

    private void restartCurrentGame() {
        if (!showingGame || solverRunning) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        model.restartCurrentGame();
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        syncGameTimerState();
        updateStatus();
    }

    private void undoMove() {
        if (!showingGame || solverRunning) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        model.undo();
        updateStatus();
    }

    private void redoMove() {
        if (!showingGame || solverRunning || boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        model.redo();
        updateStatus();
    }

    private void showMoveHistoryDialog() {
        if (solverRunning) {
            return;
        }
        if (!showingGame) {
            showMessageDialog("Start or load a game first.",
                    "Move History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        runWithPausedTimer(() -> {
            List<MoveAction> history = model.getActionHistory();
            List<MoveAction> redo = model.getRedoHistory();
            if (history.isEmpty() && redo.isEmpty()) {
                showMessageDialog("No moves yet. Your current run history will appear here.",
                        "Move History", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder text = new StringBuilder();
            text.append(history.size()).append(" completed actions · ")
                    .append(redo.size()).append(" available to redo");
            if (history.isEmpty()) {
                text.append("\n\nAll completed actions are currently undone.");
            }
            int first = Math.max(0, history.size() - 50);
            if (first > 0) {
                text.append("\n\nShowing the latest 50 actions.");
            }
            for (int index = first; index < history.size(); index++) {
                MoveAction action = history.get(index);
                text.append("\n").append(index + 1).append(". Empty ")
                        .append(action.getDirection().name().toLowerCase());
                if (action.getSteps() > 1) {
                    text.append(" x ").append(action.getSteps()).append(" (one move)");
                }
            }
            JTextArea historyText = new JTextArea(text.toString(), 18, 36);
            historyText.setEditable(false);
            historyText.setCaretPosition(0);
            showMessageDialog(new JScrollPane(historyText),
                    "Move History", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void loadGame() {
        if (solverRunning) {
            return;
        }
        autosaveCurrentGameIfSafe();
        runWithPausedTimer(() -> {
            SaveManager.SaveMetadata[] saves = SaveManager.getAllSaveMetadata();
            if (saves.length == 0) {
                showMessageDialog("No save file found.", "SlideDo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int selected = saves.length == 1 ? 0 : chooseSavedGame(saves);
            if (selected >= 0) {
                loadGameWhilePaused(saves[selected].size);
            }
        });
    }

    private int chooseSavedGame(SaveManager.SaveMetadata[] saves) {
        Object[] options = new Object[saves.length];
        for (int index = 0; index < saves.length; index++) {
            SaveManager.SaveMetadata metadata = saves[index];
            options[index] = metadata.size + "x" + metadata.size + " · "
                    + difficultyLabel(metadata.difficulty) + " · "
                    + metadata.moves + " moves · " + (metadata.elapsedMs / 1000) + "s";
        }
        return showOptionDialog("Choose a saved game to continue.", "Saved Games",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
    }

    private void loadGameWhilePaused(int size) {
        SaveManager.SaveData data = SaveManager.loadGame(size);
        if (data != null) {
            clearMovableHint();
            if (model.getSize() != data.size) {
                model.removeObserver(this);
                model = new GameModel(data.size);
                model.addObserver(this);
                boardPanel.setModel(model);
            }
            model.loadState(data);
            activeDailyDateId = null;
            assistedSolveActive = false;
            completedAssisted = false;
            completionTracker.reset();
            pendingResultMessage = null;
            solverRunning = false;
            showGame();
            showMessageDialog("Game loaded!", "SlideDo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            showMessageDialog("No save file found.", "SlideDo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean saveCurrentGame() {
        if (model == null) {
            return false;
        }
        if (activeDailyDateId != null) {
            boolean assisted = assistedSolveActive || (model.isSolved() && completedAssisted);
            return SaveManager.saveDailyGame(activeDailyDateId, model, assisted);
        }
        return SaveManager.saveGame(model);
    }

    private void showDailyCalendarDialog() {
        if (solverRunning) {
            return;
        }
        if (showingGame) {
            showHome();
        }
        LocalDate today = LocalDate.now();
        YearMonth requested = dailyCalendarMonth == null
                ? YearMonth.from(today) : dailyCalendarMonth;
        showDailyCalendarDialog(DailyCalendarMonth.showing(requested, today));
    }

    private void showDailyCalendarDialog(DailyCalendarMonth calendar) {
        dailyCalendarMonth = calendar.getMonth();
        LocalDate today = LocalDate.now();
        JDialog dialog = new JDialog(this, "Daily Calendar", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(createDailyCalendarPanel(dialog, calendar, today));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        runWithPausedTimer(() -> dialog.setVisible(true));
    }

    private JPanel createDailyCalendarPanel(JDialog dialog, DailyCalendarMonth calendar,
            LocalDate today) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        JLabel title = new JLabel("Daily Calendar", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("4x4 Classic · choose today or replay an earlier offline puzzle.",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        panel.add(heading, BorderLayout.NORTH);

        JPanel calendarBody = new JPanel(new BorderLayout(0, 8));
        JPanel monthNavigation = new JPanel(new BorderLayout(8, 0));
        JButton previous = new JButton("Previous");
        previous.setEnabled(true);
        previous.addActionListener(event -> {
            dialog.dispose();
            dailyCalendarMonth = calendar.getMonth().minusMonths(1);
            SwingUtilities.invokeLater(() -> showDailyCalendarDialog(
                    DailyCalendarMonth.showing(dailyCalendarMonth, LocalDate.now())));
        });
        JLabel month = new JLabel(calendar.getMonthId(), SwingConstants.CENTER);
        month.setFont(new Font("SansSerif", Font.BOLD, 16));
        JButton next = new JButton("Next");
        next.setEnabled(calendar.canGoNext());
        next.addActionListener(event -> {
            dialog.dispose();
            dailyCalendarMonth = calendar.getMonth().plusMonths(1);
            SwingUtilities.invokeLater(() -> showDailyCalendarDialog(
                    DailyCalendarMonth.showing(dailyCalendarMonth, LocalDate.now())));
        });
        monthNavigation.add(previous, BorderLayout.WEST);
        monthNavigation.add(month, BorderLayout.CENTER);
        monthNavigation.add(next, BorderLayout.EAST);
        calendarBody.add(monthNavigation, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String weekday : weekdays) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            grid.add(label);
        }
        for (int index = 0; index < calendar.getFirstDayOffset(); index++) {
            grid.add(new JLabel());
        }
        for (LocalDate date : calendar.getDates()) {
            DesktopDailyContent.DayState state = dailyDayState(date, today);
            JButton day = new JButton(DesktopDailyContent.dayButtonText(date, state));
            day.setToolTipText(DesktopDailyContent.dayAccessibilityText(date, state));
            day.getAccessibleContext().setAccessibleName(
                    DesktopDailyContent.dayAccessibilityText(date, state));
            day.setEnabled(state != DesktopDailyContent.DayState.FUTURE);
            day.setForeground(state == DesktopDailyContent.DayState.COMPLETED
                    ? new Color(35, 120, 70) : Color.DARK_GRAY);
            day.addActionListener(event -> {
                dialog.dispose();
                startDailyChallenge(date);
            });
            grid.add(day);
        }
        calendarBody.add(grid, BorderLayout.CENTER);

        JLabel legend = new JLabel("+ Completed   ~ In progress   ! Missed   · Future",
                SwingConstants.CENTER);
        legend.setFont(new Font("SansSerif", Font.PLAIN, 12));
        calendarBody.add(legend, BorderLayout.SOUTH);
        panel.add(calendarBody, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        SaveManager.DailyProgress progress = SaveManager.getDailyProgress(today.toString());
        JLabel streak = new JLabel(DesktopDailyContent.progressSummary(progress));
        JButton close = new JButton("Back");
        close.addActionListener(event -> dialog.dispose());
        footer.add(streak, BorderLayout.CENTER);
        footer.add(close, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private DesktopDailyContent.DayState dailyDayState(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) {
            return DesktopDailyContent.DayState.FUTURE;
        }
        SaveManager.DailyProgress progress = SaveManager.getDailyProgress(date.toString());
        SaveManager.SaveMetadata metadata = SaveManager.getDailySaveMetadata(date.toString());
        if (progress.completed || (metadata != null && metadata.solved)) {
            return DesktopDailyContent.DayState.COMPLETED;
        }
        if (metadata != null) {
            return DesktopDailyContent.DayState.IN_PROGRESS;
        }
        return date.equals(today)
                ? DesktopDailyContent.DayState.READY : DesktopDailyContent.DayState.MISSED;
    }

    private void startDailyChallenge(LocalDate date) {
        if (solverRunning || date == null || date.isAfter(LocalDate.now())) {
            return;
        }
        DailyChallenge challenge = DailyChallenge.forDate(date);
        SaveManager.SaveData saved = SaveManager.loadDailyGame(challenge.getDateId());
        boolean savedAssisted = saved != null && SaveManager.isDailyGameAssisted(challenge.getDateId());
        clearMovableHint();
        model.removeObserver(this);
        model = saved == null ? challenge.createGame() : new GameModel(saved.size);
        model.addObserver(this);
        boardPanel.setModel(model);
        if (saved != null) {
            model.loadState(saved);
            if (model.isSolved()) {
                model.restartCurrentGame();
            }
        }
        activeDailyDateId = challenge.getDateId();
        completedAssisted = savedAssisted;
        assistedSolveActive = savedAssisted;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        showGame();
    }

    private void runSolver(Solver solver) {
        if (!showingGame || solverRunning) {
            return;
        }
        if (boardPanel.isBusy()) {
            return;
        }
        clearMovableHint();
        if (model.getSize() >= 4 && solver instanceof BfsSolver) {
            int choice = showConfirmDialog(
                    "BFS on 4x4 or larger may crash or freeze. Continue?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }
        if (model.getSize() > 4 && solver instanceof AStarSolver) {
            int choice = showConfirmDialog(
                    "A* on 5x5 can be very slow or memory-heavy. Continue?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }
        if (model.getSize() > 4 && solver instanceof IdaStarSolver) {
            int choice = showConfirmDialog(
                    "IDA* on 5x5 can take a long time. Continue?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION)
                return;
        }

        solverRunning = true;
        boardPanel.setInputLocked(true);
        syncGameTimerState();
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
                        int choice = showConfirmDialog(
                            "Solution found: " + solution.size() + " moves.\nAnimate it now?",
                                "Solution", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            assistedSolveActive = true;
                            boardPanel.enqueueMoves(solution);
                        }
                    } else {
                        showMessageDialog("No solution found or timed out.",
                                "Solver", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    boardPanel.setInputLocked(false);
                    solverRunning = false;
                    syncGameTimerState();
                    setTitle("Number Klotski - Java Edition");
                }
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
        showMessageDialog(message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRecordsDialog() {
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        SaveManager.BestRecord[][] records = new SaveManager.BestRecord[3][difficulties.length];
        SaveManager.CompletionStats[][] stats = new SaveManager.CompletionStats[3][difficulties.length];
        for (int row = 0; row < records.length; row++) {
            int size = row + 3;
            for (int column = 0; column < difficulties.length; column++) {
                records[row][column] = SaveManager.getBestRecord(size, difficulties[column]);
                stats[row][column] = SaveManager.getCompletionStats(size, difficulties[column]);
            }
        }
        String message = DesktopHomeContent.recordsSummary(records, stats);
        showMessageDialog(message, "Records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showResultsDialog(int moves, long timeMs) {
        if (activeDailyDateId != null && model != null && model.isSolved()) {
            saveCurrentGame();
        }
        String message = pendingResultMessage == null
                ? DesktopResultContent.resultsMessage(model.getSize(), model.getDifficulty(), moves, timeMs,
                        false, false, null, SaveManager.getBestRecord(model.getSize(), model.getDifficulty()))
                : pendingResultMessage;
        pendingResultMessage = null;

        Object[] options = {"Replay Puzzle", "New Size", "Home"};
        int choice = showOptionDialog(message, "Results",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            replayCurrentPuzzle();
        } else if (choice == 1 || choice == 2) {
            showHome();
        }
    }

    private void showPreferencesDialog() {
        JCheckBox reducedMotionBox = new JCheckBox("Reduce motion", reducedMotionEnabled);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel(DesktopHomeContent.preferencesDescription()), BorderLayout.NORTH);
        panel.add(reducedMotionBox, BorderLayout.CENTER);

        int result = showConfirmDialog(panel, "Preferences",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            reducedMotionEnabled = reducedMotionBox.isSelected();
            boardPanel.setReducedMotion(reducedMotionEnabled);
            updateStatus();
        }
    }

    private void showHome() {
        if (showingGame) {
            autosaveCurrentGameIfSafe();
            activeDailyDateId = null;
            completedAssisted = false;
            assistedSolveActive = false;
        }
        showingGame = false;
        clearMovableHint();
        syncGameTimerState();
        contentLayout.show(contentPanel, HOME_CARD);
        updateHomeSaveSummary();
        statusLabel.setText("Home | New Game, Continue, Daily Calendar, How to Play, Records, Preferences");
    }

    private void showGame() {
        showingGame = true;
        contentLayout.show(contentPanel, GAME_CARD);
        syncGameTimerState();
        updateStatus();
        SwingUtilities.invokeLater(() -> boardPanel.requestFocusInWindow());
    }

    private void updateHomeSaveSummary() {
        if (continueSummaryLabel == null) {
            return;
        }
        SaveManager.SaveMetadata[] saves = SaveManager.getAllSaveMetadata();
        if (saves.length == 0) {
            continueSummaryLabel.setText("No saved games yet.");
            return;
        }
        if (saves.length == 1) {
            SaveManager.SaveMetadata metadata = saves[0];
            continueSummaryLabel.setText("Saved: " + metadata.size + "x" + metadata.size
                    + " · " + difficultyLabel(metadata.difficulty) + " · "
                    + metadata.moves + " moves · " + (metadata.elapsedMs / 1000) + "s");
            return;
        }
        continueSummaryLabel.setText(saves.length + " independent saved games available.");
    }

    private boolean autosaveCurrentGameIfSafe() {
        if (model == null || !DesktopAutosavePolicy.shouldAutosave(
                showingGame, boardPanel != null && boardPanel.isBusy(), solverRunning)) {
            return false;
        }
        return runWithPausedTimer(this::saveCurrentGame);
    }

    private void syncGameTimerState() {
        if (model == null) {
            return;
        }
        boolean shouldRun = DesktopTimerPolicy.shouldRun(
                showingGame,
                windowActive,
                timerPauseDepth > 0,
                solverRunning,
                model.isGameRunning());
        if (shouldRun) {
            model.resumeTimer();
            if (gameTimer != null) {
                gameTimer.start();
            }
        } else {
            model.pauseTimer();
            if (gameTimer != null) {
                gameTimer.stop();
            }
        }
    }

    private void runWithPausedTimer(Runnable action) {
        runWithPausedTimer(() -> {
            action.run();
            return null;
        });
    }

    private <T> T runWithPausedTimer(Supplier<T> action) {
        timerPauseDepth++;
        syncGameTimerState();
        try {
            return action.get();
        } finally {
            timerPauseDepth--;
            syncGameTimerState();
        }
    }

    private void showMessageDialog(Object message, String title, int messageType) {
        runWithPausedTimer(() -> JOptionPane.showMessageDialog(
                this, message, title, messageType));
    }

    private int showConfirmDialog(Object message, String title, int optionType) {
        return runWithPausedTimer(() -> JOptionPane.showConfirmDialog(
                this, message, title, optionType));
    }

    private int showConfirmDialog(Object message, String title, int optionType, int messageType) {
        return runWithPausedTimer(() -> JOptionPane.showConfirmDialog(
                this, message, title, optionType, messageType));
    }

    private int showOptionDialog(Object message, String title, int optionType,
            int messageType, Icon icon, Object[] options, Object initialValue) {
        return runWithPausedTimer(() -> JOptionPane.showOptionDialog(
                this, message, title, optionType, messageType, icon, options, initialValue));
    }

    private void updateStatus() {
        if (showingGame && model.isGameRunning()) {
            long elapsed = model.getElapsedTime() / 1000;
            SaveManager.BestRecord best = SaveManager.getBestRecord(model.getSize(), model.getDifficulty());
            String bestText = best == null ? "Best: --" : "Best: " + best.format();
            String hintText = movableHintActive ? " | Hint: highlighted tiles can slide into the empty cell" : "";
            String motionText = reducedMotionEnabled ? " | Reduced motion" : "";
            String dailyText = activeDailyDateId == null ? "" : " | Daily: " + activeDailyDateId;
            statusLabel.setText(String.format("Moves: %d | Time: %ds | Difficulty: %s | %s%s%s%s",
                    model.getMoveCount(), elapsed, difficultyLabel(model.getDifficulty()),
                    bestText, hintText, motionText, dailyText));
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
        syncGameTimerState();
        if (!completionTracker.claim()) {
            return;
        }
        int size = model.getSize();
        PuzzleDifficulty difficulty = model.getDifficulty();
        boolean assisted = assistedSolveActive;
        SaveManager.recordCompletion(completionTracker.runId(), size, difficulty,
                moves, timeMs, assisted);
        if (activeDailyDateId != null) {
            SaveManager.recordDailyCompletion(activeDailyDateId);
        }
        SaveManager.BestRecord previousBest = SaveManager.getBestRecord(size, difficulty);
        SaveManager.BestRecord candidate = new SaveManager.BestRecord(moves, timeMs);
        boolean newBest = !assisted && (previousBest == null || candidate.isBetterThan(previousBest));
        if (newBest) {
            SaveManager.recordBestIfBetter(size, difficulty, moves, timeMs);
        }
        SaveManager.BestRecord best = SaveManager.getBestRecord(size, difficulty);
        assistedSolveActive = false;
        completedAssisted = assisted;
        pendingResultMessage = DesktopResultContent.resultsMessage(
                size, difficulty, moves, timeMs,
                assisted, newBest, previousBest, best);
        if (activeDailyDateId != null) {
            pendingResultMessage += "\n" + DesktopDailyContent.progressSummary(
                    SaveManager.getDailyProgress(LocalDate.now().toString()));
        }
        String bestText = best == null ? "--" : best.format();
        statusLabel.setText(String.format("Solved! Moves: %d | Time: %ds | Difficulty: %s | Best: %s",
                moves, timeMs / 1000, difficultyLabel(difficulty), bestText));
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
