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

    /** Active Favorite Practice namespace, or {@code null} outside favorites. */
    private String activeFavoriteId;

    /** Active Continuous Challenge aggregate, or {@code null} outside continuous mode. */
    private ContinuousChallenge activeContinuousChallenge;

    /** Fixed size/difficulty scope retained for the active Continuous session. */
    private int continuousSize;
    /** Difficulty fixed for the active Continuous Challenge session. */
    private PuzzleDifficulty continuousDifficulty;

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

    /** Desktop sound-feedback preference. */
    private boolean soundEnabled;

    /** Prevents an active session from recreating a save immediately after reset. */
    private boolean savedGamesReset;

    /** Persisted desktop palette. */
    private DesktopTheme desktopTheme;

    /** Persisted desktop locale. */
    private DesktopLocale desktopLocale;

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
        desktopTheme = DesktopTheme.fromId(SaveManager.getDesktopTheme());
        desktopLocale = DesktopLocale.fromTag(SaveManager.getDesktopLanguageTag());
        reducedMotionEnabled = SaveManager.isReducedMotionEnabled();
        soundEnabled = SaveManager.isSoundEnabled();

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
        boardPanel.setTheme(desktopTheme);
        boardPanel.setReducedMotion(reducedMotionEnabled);
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
        if (!SaveManager.isOnboardingSeen()) {
            SwingUtilities.invokeLater(this::showOnboardingDialog);
        }
    }

    private String text(String key) {
        return desktopLocale.text(key);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Game Menu
        JMenu gameMenu = new JMenu(text("game"));

        JMenuItem newGame3 = new JMenuItem(text("new3"));
        newGame3.addActionListener(e -> startNewGame(3));
        gameMenu.add(newGame3);

        JMenuItem newGame4 = new JMenuItem(text("new4"));
        newGame4.addActionListener(e -> startNewGame(4));
        gameMenu.add(newGame4);

        JMenuItem newGame5 = new JMenuItem(text("new5"));
        newGame5.addActionListener(e -> startNewGame(5));
        gameMenu.add(newGame5);

        gameMenu.addSeparator();

        JMenuItem restartItem = new JMenuItem(text("restart"));
        restartItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        restartItem.addActionListener(e -> restartCurrentGame());
        gameMenu.add(restartItem);

        JMenuItem undoItem = new JMenuItem(text("undo"));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undoMove());
        gameMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem(text("redo"));
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> redoMove());
        gameMenu.add(redoItem);

        JMenuItem moveHistoryItem = new JMenuItem(text("history"));
        moveHistoryItem.addActionListener(e -> showMoveHistoryDialog());
        gameMenu.add(moveHistoryItem);

        gameMenu.addSeparator();

        JMenuItem saveItem = new JMenuItem(text("save"));
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

        JMenuItem loadItem = new JMenuItem(text("load"));
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(e -> loadGame());
        gameMenu.add(loadItem);

        JMenuItem recordsItem = new JMenuItem(text("records"));
        recordsItem.addActionListener(e -> showRecordsDialog());
        gameMenu.add(recordsItem);

        JMenuItem dailyItem = new JMenuItem(text("daily"));
        dailyItem.addActionListener(e -> showDailyCalendarDialog());
        gameMenu.add(dailyItem);

        JMenuItem favoritesItem = new JMenuItem(text("favorites"));
        favoritesItem.addActionListener(e -> showFavoritesDialog());
        gameMenu.add(favoritesItem);

        JMenuItem trendsItem = new JMenuItem(text("trends"));
        trendsItem.addActionListener(e -> showTrendsDialog());
        gameMenu.add(trendsItem);

        JMenuItem continuousItem = new JMenuItem(text("continuous"));
        continuousItem.addActionListener(e -> showContinuousDialog());
        gameMenu.add(continuousItem);

        JMenuItem quickReminderItem = new JMenuItem(text("quickReminder"));
        quickReminderItem.addActionListener(e -> showQuickReminderDialog());
        gameMenu.add(quickReminderItem);

        JMenuItem preferencesItem = new JMenuItem(text("preferences"));
        preferencesItem.addActionListener(e -> showPreferencesDialog());
        gameMenu.add(preferencesItem);

        gameMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem(text("exit"));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> {
            autosaveCurrentGameIfSafe();
            System.exit(0);
        });
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);

        // Assist Menu
        JMenu assistMenu = new JMenu(text("assist"));

        JMenuItem showMovableItem = new JMenuItem(text("showMovable"));
        showMovableItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
        showMovableItem.addActionListener(e -> showMovableTiles());
        assistMenu.add(showMovableItem);

        menuBar.add(assistMenu);

        // Solver Menu
        JMenu solverMenu = new JMenu(text("solver"));

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
        JMenu helpMenu = new JMenu(text("help"));

        JMenuItem howToPlayItem = new JMenuItem(text("howToPlay"));
        howToPlayItem.addActionListener(e -> showHelpDialog(text("howToPlay"), DesktopHelpContent.howToPlay(desktopLocale)));
        helpMenu.add(howToPlayItem);

        JMenuItem practiceTutorialItem = new JMenuItem(text("practiceTutorial"));
        practiceTutorialItem.addActionListener(e -> showPracticeTutorialDialog());
        helpMenu.add(practiceTutorialItem);

        JMenuItem beginnerGuideItem = new JMenuItem(text("beginnerGuide"));
        beginnerGuideItem.addActionListener(e -> showOnboardingDialog());
        helpMenu.add(beginnerGuideItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
        panel.setBackground(desktopTheme.getHomeBackground());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("SlideDo", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        title.setForeground(desktopTheme.getHomeTitle());
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(title, gbc);

        JLabel subtitle = new JLabel(text("firstRunSubtitle"),
                SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(desktopTheme.getHomeSecondary());
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

        panel.add(createHomeButton(text("continueLoad"), this::loadGame), nextHomeRow(gbc));
        continueSummaryLabel = new JLabel("", SwingConstants.CENTER);
        continueSummaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        continueSummaryLabel.setForeground(desktopTheme.getHomeSecondary());
        panel.add(continueSummaryLabel, nextHomeRow(gbc));
        panel.add(createHomeButton(text("daily"), this::showDailyCalendarDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("favorites"), this::showFavoritesDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("trends"), this::showTrendsDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("continuous"), this::showContinuousDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("howToPlay"), () -> showHelpDialog(text("howToPlay"), DesktopHelpContent.howToPlay(desktopLocale))),
                nextHomeRow(gbc));
        panel.add(createHomeButton(text("practiceTutorial"), this::showPracticeTutorialDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("beginnerGuide"), this::showOnboardingDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("records"), this::showRecordsDialog), nextHomeRow(gbc));
        panel.add(createHomeButton(text("preferences"), this::showPreferencesDialog), nextHomeRow(gbc));

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
        savedGamesReset = false;
        clearMovableHint();
        model.removeObserver(this);
        model = DesktopGameFactory.create(size, difficulty);
        model.addObserver(this);
        boardPanel.setModel(model);

        activeDailyDateId = null;
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        continuousDifficulty = null;
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
        savedGamesReset = false;
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
            savedGamesReset = false;
            clearMovableHint();
            if (model.getSize() != data.size) {
                model.removeObserver(this);
                model = new GameModel(data.size);
                model.addObserver(this);
                boardPanel.setModel(model);
            }
            model.loadState(data);
            activeDailyDateId = null;
            activeFavoriteId = null;
            activeContinuousChallenge = null;
            continuousDifficulty = null;
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
        if (model == null || savedGamesReset) {
            return false;
        }
        if (activeContinuousChallenge != null) {
            boolean assisted = assistedSolveActive || (model.isSolved() && completedAssisted);
            return SaveManager.saveContinuousGame(model, activeContinuousChallenge, assisted);
        }
        if (activeFavoriteId != null) {
            boolean assisted = assistedSolveActive || (model.isSolved() && completedAssisted);
            return SaveManager.saveFavoriteRun(activeFavoriteId, model, assisted);
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
        savedGamesReset = false;
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
        activeFavoriteId = null;
        activeContinuousChallenge = null;
        continuousDifficulty = null;
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

    private void showQuickReminderDialog() {
        showHelpDialog(text("quickReminder"), DesktopHelpContent.quickReminder(desktopLocale));
    }

    /** Shows the first-run learning path and remembers completion separately from saves. */
    private void showOnboardingDialog() {
        List<DesktopLearningContent.OnboardingPage> pages =
                DesktopLearningContent.onboardingPages(desktopLocale);
        JDialog dialog = new JDialog(this, text("beginnerGuide"), true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setFont(new Font("SansSerif", Font.PLAIN, 15));
        body.setRows(5);
        body.setColumns(34);
        JLabel progress = new JLabel("", SwingConstants.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton back = new JButton(text("back"));
        JButton next = new JButton(text("next"));
        JButton skip = new JButton(text("skip"));
        JButton practice = new JButton(text("practiceTutorial"));
        JButton start = new JButton(text("start"));
        footer.add(back);
        footer.add(next);
        footer.add(skip);
        footer.add(practice);
        footer.add(start);
        content.add(title, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.add(progress, BorderLayout.NORTH);
        bottom.add(footer, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);
        dialog.setContentPane(content);

        int[] index = {0};
        Runnable refresh = () -> {
            DesktopLearningContent.OnboardingPage page = pages.get(index[0]);
            title.setText(page.getTitle());
            body.setText(page.getBody());
            progress.setText((index[0] + 1) + " / " + pages.size());
            back.setEnabled(index[0] > 0);
            next.setEnabled(index[0] < pages.size() - 1);
        };
        back.addActionListener(event -> {
            if (index[0] > 0) {
                index[0]--;
                refresh.run();
            }
        });
        next.addActionListener(event -> {
            if (index[0] < pages.size() - 1) {
                index[0]++;
                refresh.run();
            }
        });
        skip.addActionListener(event -> {
            SaveManager.markOnboardingSeen();
            dialog.dispose();
        });
        practice.addActionListener(event -> {
            SaveManager.markOnboardingSeen();
            dialog.dispose();
            SwingUtilities.invokeLater(this::showPracticeTutorialDialog);
        });
        start.addActionListener(event -> {
            SaveManager.markOnboardingSeen();
            dialog.dispose();
            startNewGame(3, PuzzleDifficulty.CLASSIC);
        });
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                SaveManager.markOnboardingSeen();
            }
        });
        refresh.run();
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        runWithPausedTimer(() -> dialog.setVisible(true));
    }

    /** Opens an isolated interactive two-step practice board without recording a game. */
    private void showPracticeTutorialDialog() {
        GameModel tutorialModel = new GameModel(3);
        tutorialModel.loadState(new int[][] {{1, 2, 3}, {4, 0, 6}, {7, 5, 8}}, 0);
        DesktopTutorialProgress progress = new DesktopTutorialProgress();
        BoardPanel tutorialBoard = new BoardPanel(tutorialModel);
        tutorialBoard.setTheme(desktopTheme);
        tutorialBoard.setReducedMotion(reducedMotionEnabled);
        tutorialBoard.setPreferredSize(new Dimension(420, 420));
        tutorialBoard.setWinDialogHandler((parent, moves, timeMs) -> { });

        JDialog dialog = new JDialog(this, text("practiceTutorial"), true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel instruction = new JLabel("", SwingConstants.CENTER);
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JLabel state = new JLabel("", SwingConstants.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JButton reset = new JButton(text("resetLesson"));
        JButton start = new JButton(text("startTutorialPuzzle"));
        JButton close = new JButton(text("close"));
        controls.add(reset);
        controls.add(start);
        controls.add(close);
        panel.add(instruction, BorderLayout.NORTH);
        panel.add(tutorialBoard, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(state, BorderLayout.NORTH);
        south.add(controls, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        dialog.setContentPane(panel);

        Runnable refresh = () -> {
            instruction.setText(DesktopLearningContent.practiceTutorial(desktopLocale)
                    .split("\\n", 2)[0]);
            String step = switch (progress.getStep()) {
                case FIRST_MOVE -> "1 / 2: make an adjacent move";
                case WHOLE_LINE -> "2 / 2: try a farther aligned tile";
                case COMPLETE -> "Complete: you can start a normal puzzle.";
            };
            state.setText(step);
        };
        tutorialModel.addObserver(new GameObserver() {
            @Override
            public void onGridChanged() {
                refresh.run();
            }

            @Override
            public void onMove(Direction direction) {
                progress.observe(tutorialModel);
                refresh.run();
            }

            @Override
            public void onGameWon(int moves, long timeMs) {
                refresh.run();
            }
        });
        reset.addActionListener(event -> {
            tutorialModel.restartCurrentGame();
            progress.reset();
            refresh.run();
        });
        start.addActionListener(event -> {
            dialog.dispose();
            startNewGame(3, PuzzleDifficulty.CLASSIC);
        });
        close.addActionListener(event -> dialog.dispose());
        refresh.run();
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        runWithPausedTimer(() -> dialog.setVisible(true));
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

    private void showFavoritesDialog() {
        if (solverRunning) {
            return;
        }
        SaveManager.FavoritePuzzle[] favorites = SaveManager.getFavoritePuzzles();
        boolean canSaveCurrent = showingGame && activeFavoriteId == null
                && activeContinuousChallenge == null && model != null;
        Object[] options = new Object[favorites.length + (canSaveCurrent ? 1 : 0)];
        int offset = canSaveCurrent ? 1 : 0;
        if (canSaveCurrent) {
            options[0] = "Save current puzzle as Favorite";
        }
        for (int index = 0; index < favorites.length; index++) {
            options[index + offset] = DesktopFavoriteContent.optionLabel(favorites[index]);
        }
        if (options.length == 0) {
            showMessageDialog("No favorites saved yet. Start a normal puzzle to save one.\n\n"
                    + DesktopFavoriteContent.practiceSummary(), "Favorites",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = showOptionDialog(DesktopFavoriteContent.practiceSummary(), "Favorites",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                options, options[0]);
        if (canSaveCurrent && choice == 0) {
            saveCurrentAsFavorite();
        } else if (choice >= offset && choice < options.length) {
            showFavoriteActions(favorites[choice - offset]);
        }
    }

    private void saveCurrentAsFavorite() {
        String defaultLabel = model == null ? "Favorite" : model.getSize() + "x"
                + model.getSize() + " " + difficultyLabel(model.getDifficulty());
        String label = runWithPausedTimer(() -> JOptionPane.showInputDialog(this,
                "Name this exact starting puzzle (up to 40 characters).",
                defaultLabel));
        if (label == null) {
            return;
        }
        SaveManager.FavoritePuzzle saved = SaveManager.saveFavorite(model, label);
        showMessageDialog(saved == null ? "Favorite could not be saved."
                        : "Favorite saved: " + saved.label,
                "Favorites", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFavoriteActions(SaveManager.FavoritePuzzle favorite) {
        Object[] options = {"Replay Favorite", "Rename", "Delete", "Cancel"};
        int choice = showOptionDialog(DesktopFavoriteContent.optionLabel(favorite) + "\n\n"
                        + DesktopFavoriteContent.practiceSummary(), "Favorite",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                options, options[0]);
        if (choice == 0) {
            startFavoritePractice(favorite);
        } else if (choice == 1) {
            renameFavorite(favorite);
        } else if (choice == 2) {
            int confirm = showConfirmDialog("Delete this favorite and its practice save?",
                    "Delete Favorite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                SaveManager.removeFavorite(favorite.id);
            }
        }
    }

    private void renameFavorite(SaveManager.FavoritePuzzle favorite) {
        String label = runWithPausedTimer(() -> JOptionPane.showInputDialog(this,
                "New favorite label (up to 40 characters).", favorite.label));
        if (label != null && !SaveManager.renameFavorite(favorite.id, label)) {
            showMessageDialog("Favorite label was not changed.", "Favorites",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void startFavoritePractice(SaveManager.FavoritePuzzle favorite) {
        if (solverRunning || favorite == null) {
            return;
        }
        autosaveCurrentGameIfSafe();
        savedGamesReset = false;
        clearMovableHint();
        model.removeObserver(this);
        model = favorite.createGame();
        model.addObserver(this);
        boardPanel.setModel(model);
        activeDailyDateId = null;
        activeFavoriteId = favorite.id;
        activeContinuousChallenge = null;
        continuousDifficulty = null;
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        showGame();
    }

    private void showTrendsDialog() {
        if (solverRunning) {
            return;
        }
        int size = SaveManager.getTrendSize();
        PuzzleDifficulty difficulty = SaveManager.getTrendDifficulty();
        Object[] options = {"Change Scope", "Set Weekly Goal", "Close"};
        int choice = showOptionDialog(
                DesktopTrendContent.summary(size, difficulty,
                        SaveManager.getPersonalTrend(size, difficulty),
                        SaveManager.getWeeklyGoalProgress(LocalDate.now(),
                                java.time.ZoneId.systemDefault(), size, difficulty)),
                "Trends / Weekly Goal", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            showTrendScopeDialog();
        } else if (choice == 1) {
            showWeeklyGoalDialog();
        }
    }

    private void showTrendScopeDialog() {
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        Object[] options = new Object[9];
        int selected = 0;
        for (int size = 3; size <= 5; size++) {
            for (int index = 0; index < difficulties.length; index++) {
                options[(size - 3) * difficulties.length + index] =
                        DesktopTrendContent.scopeLabel(size, difficulties[index]);
                if (size == SaveManager.getTrendSize()
                        && difficulties[index] == SaveManager.getTrendDifficulty()) {
                    selected = (size - 3) * difficulties.length + index;
                }
            }
        }
        int choice = showOptionDialog("Choose the size and difficulty used by Trends and Weekly Goal.",
                "Trend Scope", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[selected]);
        if (choice >= 0) {
            SaveManager.setTrendSize(3 + choice / difficulties.length);
            SaveManager.setTrendDifficulty(difficulties[choice % difficulties.length]);
            showTrendsDialog();
        }
    }

    private void showWeeklyGoalDialog() {
        String value = runWithPausedTimer(() -> JOptionPane.showInputDialog(this,
                "Weekly player-completion target (1-50).",
                String.valueOf(SaveManager.getWeeklyGoalTarget())));
        if (value == null) {
            return;
        }
        try {
            if (!SaveManager.setWeeklyGoalTarget(Integer.parseInt(value.trim()))) {
                throw new NumberFormatException();
            }
            showTrendsDialog();
        } catch (NumberFormatException exception) {
            showMessageDialog("Weekly goal must be a whole number from 1 through 50.",
                    "Weekly Goal", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showContinuousDialog() {
        if (solverRunning) {
            return;
        }
        SaveManager.ContinuousGame saved = SaveManager.loadContinuousGame();
        if (saved != null && saved.challenge.isComplete()) {
            SaveManager.clearContinuousGame();
            saved = null;
        }
        boolean resumable = saved != null;
        int[] targets = {3, 5, 10};
        Object[] options = new Object[targets.length + (resumable ? 2 : 0)];
        int offset = 0;
        if (resumable) {
            options[0] = DesktopContinuousContent.optionLabel(saved.challenge, saved.size,
                    saved.difficulty);
            options[1] = "End saved challenge";
            offset = 2;
        }
        for (int index = 0; index < targets.length; index++) {
            options[index + offset] = "Start " + targets[index] + " puzzles";
        }
        int choice = showOptionDialog("One fixed size/difficulty scope; progress is isolated from normal, "
                        + "Daily, and Favorite Practice saves.", "Continuous Challenge",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);
        if (choice < 0) {
            return;
        }
        if (resumable && choice == 0) {
            resumeContinuousChallenge();
        } else if (resumable && choice == 1) {
            endContinuousChallenge();
        } else {
            chooseContinuousScope(targets[choice - offset]);
        }
    }

    private void chooseContinuousScope(int target) {
        String[] options = new String[9];
        PuzzleDifficulty[] difficulties = PuzzleDifficulty.values();
        int selected = 0;
        for (int size = 3; size <= 5; size++) {
            for (int index = 0; index < difficulties.length; index++) {
                options[(size - 3) * difficulties.length + index] =
                        DesktopTrendContent.scopeLabel(size, difficulties[index]);
                if (size == (model == null ? 4 : model.getSize())
                        && difficulties[index] == (model == null
                                ? PuzzleDifficulty.CLASSIC : model.getDifficulty())) {
                    selected = (size - 3) * difficulties.length + index;
                }
            }
        }
        int choice = showOptionDialog("Choose the fixed scope for this challenge.",
                "Continuous Scope", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[selected]);
        if (choice >= 0) {
            startContinuousChallenge(3 + choice / difficulties.length,
                    difficulties[choice % difficulties.length], target);
        }
    }

    private void startContinuousChallenge(int size, PuzzleDifficulty difficulty, int target) {
        if (solverRunning || !ContinuousChallenge.isSupportedTarget(target)) {
            return;
        }
        autosaveCurrentGameIfSafe();
        savedGamesReset = false;
        SaveManager.clearContinuousGame();
        activeContinuousChallenge = ContinuousChallenge.start(target);
        continuousSize = size;
        continuousDifficulty = difficulty;
        clearMovableHint();
        model.removeObserver(this);
        model = DesktopGameFactory.create(size, difficulty);
        model.addObserver(this);
        boardPanel.setModel(model);
        activeDailyDateId = null;
        activeFavoriteId = null;
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        saveCurrentGame();
        showGame();
    }

    private void resumeContinuousChallenge() {
        SaveManager.ContinuousGame saved = SaveManager.loadContinuousGame();
        if (saved == null || saved.challenge.isComplete()) {
            SaveManager.clearContinuousGame();
            return;
        }
        savedGamesReset = false;
        clearMovableHint();
        model.removeObserver(this);
        model = new GameModel(saved.game.size);
        model.loadState(saved.game);
        model.addObserver(this);
        boardPanel.setModel(model);
        activeContinuousChallenge = saved.challenge;
        continuousSize = saved.size;
        continuousDifficulty = saved.difficulty;
        activeDailyDateId = null;
        activeFavoriteId = null;
        assistedSolveActive = saved.assisted;
        completedAssisted = saved.assisted;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        if (model.isSolved()) {
            startNextContinuousPuzzle();
        } else {
            showGame();
        }
    }

    private void startNextContinuousPuzzle() {
        if (solverRunning || activeContinuousChallenge == null
                || activeContinuousChallenge.isComplete()) {
            return;
        }
        savedGamesReset = false;
        clearMovableHint();
        model.removeObserver(this);
        model = DesktopGameFactory.create(continuousSize, continuousDifficulty);
        model.addObserver(this);
        boardPanel.setModel(model);
        activeDailyDateId = null;
        activeFavoriteId = null;
        assistedSolveActive = false;
        completedAssisted = false;
        completionTracker.reset();
        pendingResultMessage = null;
        solverRunning = false;
        saveCurrentGame();
        showGame();
    }

    private void endContinuousChallenge() {
        SaveManager.clearContinuousGame();
        activeContinuousChallenge = null;
        continuousDifficulty = null;
        completedAssisted = false;
        assistedSolveActive = false;
        if (showingGame) {
            showHome();
        }
    }

    private void showResultsDialog(int moves, long timeMs) {
        if (activeDailyDateId != null && model != null && model.isSolved()) {
            saveCurrentGame();
        }
        if (activeFavoriteId != null || activeContinuousChallenge != null) {
            if (activeFavoriteId != null) {
                saveCurrentGame();
                String message = pendingResultMessage == null
                        ? DesktopResultContent.favoritePracticeMessage(
                                model.getSize(), model.getDifficulty(), moves, timeMs)
                        : pendingResultMessage;
                pendingResultMessage = null;
                Object[] options = {"Replay Favorite", "Favorites", "Home"};
                int choice = showOptionDialog(message, "Favorite Practice",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                        null, options, options[0]);
                if (choice == 0) {
                    replayCurrentPuzzle();
                } else if (choice == 1) {
                    showFavoritesDialog();
                } else if (choice == 2) {
                    showHome();
                }
                return;
            }
            showContinuousResultsDialog(moves, timeMs);
            return;
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

    private void showContinuousResultsDialog(int moves, long timeMs) {
        if (model != null && model.isSolved()) {
            saveCurrentGame();
        }
        String message = DesktopContinuousContent.result(activeContinuousChallenge,
                continuousSize, continuousDifficulty) + "\n\nLast puzzle: "
                + DesktopResultContent.formatMoves(moves) + " · " + (timeMs / 1000) + "s";
        Object[] options = activeContinuousChallenge.isComplete()
                ? new Object[] {"End Challenge", "Home"}
                : new Object[] {"Next Puzzle", "End Challenge", "Home"};
        int choice = showOptionDialog(message, "Continuous Results",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
        if (!activeContinuousChallenge.isComplete() && choice == 0) {
            startNextContinuousPuzzle();
        } else if (choice == (activeContinuousChallenge.isComplete() ? 0 : 1)) {
            endContinuousChallenge();
        } else if (choice >= 0) {
            showHome();
        }
    }

    private void showPreferencesDialog() {
        JCheckBox reducedMotionBox = new JCheckBox(text("reduceMotion"), reducedMotionEnabled);
        JCheckBox soundBox = new JCheckBox(text("sound"), soundEnabled);
        JComboBox<String> languageBox = new JComboBox<>(DesktopLocale.supportedTags());
        languageBox.setSelectedItem(desktopLocale.getTag());
        JComboBox<String> themeBox = new JComboBox<>(new String[] {"midnight", "ocean"});
        themeBox.setSelectedItem(desktopTheme.getId());

        JPanel choices = new JPanel(new GridLayout(0, 2, 8, 8));
        choices.add(new JLabel(text("language")));
        choices.add(languageBox);
        choices.add(new JLabel(text("theme")));
        choices.add(themeBox);
        choices.add(reducedMotionBox);
        choices.add(soundBox);

        JButton resetSaved = new JButton(text("resetSaved"));
        resetSaved.addActionListener(event -> {
            int answer = showConfirmDialog(text("resetSavedConfirm"), text("resetSaved"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                boolean cleared = SaveManager.clearSavedGames();
                savedGamesReset = true;
                showMessageDialog(cleared ? "Saved-game domains cleared." : "Some saved-game files could not be cleared.",
                        text("resetSaved"), cleared ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                updateHomeSaveSummary();
            }
        });
        JButton resetRecords = new JButton(text("resetRecords"));
        resetRecords.addActionListener(event -> {
            int answer = showConfirmDialog(text("resetRecordsConfirm"), text("resetRecords"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                boolean cleared = SaveManager.clearRecords();
                showMessageDialog(cleared ? "Records and statistics cleared." : "Some record files could not be cleared.",
                        text("resetRecords"), cleared ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel resets = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        resets.add(resetSaved);
        resets.add(resetRecords);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel(DesktopHomeContent.preferencesDescription()), BorderLayout.NORTH);
        panel.add(choices, BorderLayout.CENTER);
        panel.add(resets, BorderLayout.SOUTH);

        int result = showConfirmDialog(panel, text("preferences"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String selectedLanguage = (String) languageBox.getSelectedItem();
            String selectedTheme = (String) themeBox.getSelectedItem();
            boolean changed = !desktopLocale.getTag().equals(selectedLanguage)
                    || !desktopTheme.getId().equals(selectedTheme);
            SaveManager.setReducedMotionEnabled(reducedMotionBox.isSelected());
            SaveManager.setSoundEnabled(soundBox.isSelected());
            SaveManager.setDesktopLanguageTag(selectedLanguage);
            SaveManager.setDesktopTheme(selectedTheme);
            reducedMotionEnabled = reducedMotionBox.isSelected();
            soundEnabled = soundBox.isSelected();
            desktopLocale = DesktopLocale.fromTag(selectedLanguage);
            desktopTheme = DesktopTheme.fromId(selectedTheme);
            boardPanel.setReducedMotion(reducedMotionEnabled);
            boardPanel.setTheme(desktopTheme);
            if (changed) {
                rebuildLocalizedWindow();
            } else {
                updateStatus();
            }
        }
    }

    private void rebuildLocalizedWindow() {
        boolean wasShowingGame = showingGame;
        if (wasShowingGame) {
            autosaveCurrentGameIfSafe();
        }
        remove(contentPanel);
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.add(createHomePanel(), HOME_CARD);
        contentPanel.add(boardPanel, GAME_CARD);
        add(contentPanel, BorderLayout.CENTER);
        setupMenu();
        if (wasShowingGame) {
            showGame();
        } else {
            showHome();
        }
        revalidate();
        repaint();
    }

    private void showHome() {
        if (showingGame) {
            autosaveCurrentGameIfSafe();
            activeDailyDateId = null;
            activeFavoriteId = null;
            activeContinuousChallenge = null;
            continuousDifficulty = null;
            completedAssisted = false;
            assistedSolveActive = false;
        }
        showingGame = false;
        clearMovableHint();
        syncGameTimerState();
        contentLayout.show(contentPanel, HOME_CARD);
        updateHomeSaveSummary();
        statusLabel.setText(text("homeSummary"));
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
            continueSummaryLabel.setText(text("noSaves"));
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
            String favoriteText = activeFavoriteId == null ? "" : " | Favorite Practice";
            String continuousText = activeContinuousChallenge == null ? ""
                    : " | " + DesktopContinuousContent.status(activeContinuousChallenge);
            statusLabel.setText(String.format("Moves: %d | Time: %ds | Difficulty: %s | %s%s%s%s%s%s",
                    model.getMoveCount(), elapsed, difficultyLabel(model.getDifficulty()),
                    bestText, hintText, motionText, dailyText, favoriteText, continuousText));
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
        DesktopSoundFeedback.playMove(soundEnabled);
        updateStatus();
    }

    @Override
    public void onGameWon(int moves, long timeMs) {
        syncGameTimerState();
        DesktopSoundFeedback.playWin(soundEnabled);
        if (!completionTracker.claim()) {
            return;
        }
        int size = model.getSize();
        PuzzleDifficulty difficulty = model.getDifficulty();
        boolean assisted = assistedSolveActive;
        if (activeFavoriteId != null) {
            assistedSolveActive = false;
            completedAssisted = assisted;
            pendingResultMessage = DesktopResultContent.favoritePracticeMessage(
                    size, difficulty, moves, timeMs);
            saveCurrentGame();
            statusLabel.setText(String.format("Favorite solved! Moves: %d | Time: %ds | Difficulty: %s",
                    moves, timeMs / 1000, difficultyLabel(difficulty)));
            return;
        }
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
        if (activeContinuousChallenge != null) {
            activeContinuousChallenge = activeContinuousChallenge.completePuzzle(
                    moves, timeMs, assisted);
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
        if (activeContinuousChallenge != null) {
            saveCurrentGame();
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
