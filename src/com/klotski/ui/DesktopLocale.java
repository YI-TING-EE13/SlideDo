package com.klotski.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/**
 * Desktop locale catalog for the controls and learning copy needed to finish
 * a normal play session.  It intentionally uses plain Java maps instead of
 * Android resources so the packaged Swing app remains self-contained. Locale
 * instances are immutable; unsupported tags fall back to English, while
 * missing non-English keys also fall back per message so a new UI key cannot
 * make the desktop shell unreadable.
 */
public final class DesktopLocale {
    private static final String[] SUPPORTED_TAGS = {"en", "zh-TW", "ja-JP"};
    private static final String[] REQUIRED_KEYS = {
            "history", "historyNoMoves", "historyCounts", "historyAllUndone",
            "historyLatest50", "historyAction", "historyWholeLine",
            "direction.up", "direction.down", "direction.left", "direction.right",
            "strategicHint", "strategicHintUnavailable", "strategicHintStatus",
            "solverTools", "solverBfs", "solverAStar", "solverIdaStar", "solverCancel",
            "solverWarningBfs", "solverWarningAStar", "solverWarningIdaStar", "warning",
            "solverFound", "solverNoSolution", "solverCancelled", "solverError",
            "assistedRun", "assistedResult", "playerBestProtected",
            "favoriteSolved", "favoriteIsolation", "favoritePracticeSummary",
            "dailyCompleted", "dailyInProgress", "dailyReady", "dailyMissed", "dailyFuture",
            "dailyPuzzle", "dailyProgress",
            "continuousResume", "continuousStatus", "continuousResult", "continuousComplete", "continuousRetained",
            "trendTitle", "trendScope", "trendGoal", "trendReached", "trendWeek", "trendNoData", "trendExclusion", "trendRecent", "trendNeedData", "trendPrevious", "trendMoves", "trendTime", "trendImproving", "trendDeclining", "trendSteady", "trendNotEnough",
            "recordsPlayer", "recordsAssisted", "recordsFootnote", "recordsPlayerOnly", "recordsAssistedNote",
            "recordsScopeRecord", "preferencesDescription",
            "recordPlayerBest", "recordFirst", "recordFirstSize", "recordNewBest", "recordBestRemains", "moveSingular", "movePlural",
            "difficultyRelaxed", "difficultyClassic", "difficultyChallenge",
            "game", "assist", "solver", "help", "new3", "new4", "new5", "restart", "undo", "redo",
            "save", "load", "exit", "showMovable", "howToPlay", "beginnerGuide", "practiceTutorial",
            "continueLoad", "reduceMotion", "sound", "language", "theme", "close", "back", "next",
            "previous", "skip", "start", "resetLesson", "startTutorialPuzzle", "resetSavedConfirm",
            "resetRecordsConfirm", "homeSummary", "noSaves", "firstRunSubtitle",
            "records", "daily", "favorites", "trends", "continuous",
            "preferences", "resetSaved", "resetRecords", "quickReminder",
            "cellEmptyName", "cellTileName", "cellEmptyDescription",
            "cellMovableDescription", "cellNotAlignedDescription",
            "boardAccessibleName", "boardAccessibleDescription", "winTitle", "winMessage",
            "windowTitle", "statusInitial", "statusAccessibleDescription",
            "statusAccessibleName", "statusSeparator", "menuBarAccessibleName", "homeAccessibleName",
            "menuGameDescription", "menuAssistDescription", "menuSolverDescription",
            "menuHelpDescription", "homeAccessibleDescription", "homeScrollAccessibleDescription",
            "buttonActivate", "startOrLoadFirst", "saveSuccess", "saveFailure", "noSaveFile",
            "savedGameChooserPrompt", "savedGamesTitle", "savedGameSummary", "savedGamesAvailable",
            "gameLoaded", "difficultyChooserPrompt", "difficultyTitle", "scrambleMoves",
            "dailyCalendarSubtitle", "dailyLegend", "weekdaySun", "weekdayMon", "weekdayTue",
            "weekdayWed", "weekdayThu", "weekdayFri", "weekdaySat", "beginnerGuideTextName",
            "learningPageDescription", "learningPreviousDescription", "learningNextDescription",
            "learningSkipDescription", "learningPracticeDescription", "learningStartDescription",
            "practiceBoardName", "tutorialResetDescription", "tutorialStartDescription",
            "tutorialCloseDescription", "tutorialFirstMove", "tutorialWholeLine", "tutorialComplete",
            "learningGoalTitle", "learningGoalBody", "learningMoveTitle", "learningMoveBody",
            "learningWholeLineTitle", "learningWholeLineBody", "learningStartTitle", "learningStartBody",
            "howToPlayTitle", "howToPlayGoal", "howToPlayGoalBody", "howToPlayMoves",
            "howToPlayMovesBody", "howToPlayWholeLine", "howToPlayWholeLineBody", "howToPlayAssist",
            "howToPlayAssistBody", "practiceTitle", "practiceStepOne", "practiceStepTwo",
            "practiceStepThree", "practiceFootnote", "reminderTitle", "reminderEmpty",
            "reminderAligned", "reminderWholeLine", "reminderSolver", "dailyDayAccessibility",
            "recordFormat",
            "saveCurrentFavorite", "noFavorites", "favoriteNamePrompt", "favoriteDefaultLabel",
            "favoriteSaveFailure", "favoriteSaveSuccess", "favoriteReplay", "favoriteRename",
            "favoriteDelete", "favoriteCancel", "favoriteDeletePrompt", "favoriteDeleteTitle",
            "favoriteRenamePrompt", "favoriteRenameFailure", "trendChangeScope", "trendSetGoal",
            "trendClose", "trendScopePrompt", "trendScopeTitle", "weeklyGoalPrompt",
            "weeklyGoalTitle", "weeklyGoalInvalid", "continuousEndSaved", "continuousStartPuzzles",
            "continuousSetupDescription", "continuousTitle", "continuousScopePrompt",
            "continuousScopeTitle", "lastPuzzle", "nextPuzzle", "endChallenge", "home",
            "replayPuzzle", "newSize", "favoriteResultsTitle", "resultsTitle", "continuousResultsTitle",
            "chooseLanguageDescription", "chooseThemeDescription", "reducedMotionDescription",
            "soundDescription", "resetSavedDescription", "resetRecordsDescription",
            "preferencesAccessibleDescription", "savedGamesCleared", "savedGamesClearFailure",
            "recordsCleared", "recordsClearFailure", "backupExport", "backupRestore",
            "backupExportDescription", "backupRestoreDescription", "backupFileFilter", "backupExported",
            "backupRestored", "backupExportFailure", "backupRestoreFailure", "backupInvalid",
            "backupRestoreConfirm", "backupBusy", "backupRestoreCleanupWarning",
            "backupRestoreRecoveryRequired", "recoveryRequiredStatus",
            "statusRunning", "statusBest", "statusReducedMotion",
            "statusDaily", "statusFavorite", "statusSolved", "statusFavoriteSolved", "bestNone",
            "lastPuzzleSummary", "puzzleSolved", "puzzleTitle", "difficultyLabel", "timeLabel",
            "noRecord", "favoritePuzzleTitle", "favoriteResultIsolation", "homeIndependentSaves"
    };
    private static final Map<String, Map<String, String>> STRINGS = createStrings();

    private final String tag;

    private DesktopLocale(String tag) {
        this.tag = tag;
    }

    /**
     * Returns the stable language tag persisted for this locale.
     *
     * @return stable language tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns a defensive copy of supported desktop language tags.
     *
     * @return supported language tags
     */
    public static String[] supportedTags() {
        return SUPPORTED_TAGS.clone();
    }

    /**
     * Resolves a stable language tag, defaulting unknown values to English.
     *
     * @param tag BCP-47-like desktop tag
     * @return locale object
     */
    public static DesktopLocale fromTag(String tag) {
        if (tag != null) {
            for (String supported : SUPPORTED_TAGS) {
                if (supported.equalsIgnoreCase(tag.trim())) {
                    return new DesktopLocale(supported);
                }
            }
        }
        return new DesktopLocale("en");
    }

    /**
     * Looks up a critical control or message.
     *
     * @param key stable English key
     * @return translated text, or the key when a non-critical future key is missing
     */
    public String text(String key) {
        Map<String, String> values = STRINGS.get(tag);
        String value = values == null ? null : values.get(key);
        if (value != null) {
            return value;
        }
        Map<String, String> english = STRINGS.get("en");
        value = english == null ? null : english.get(key);
        return value == null ? key : value;
    }

    /**
     * Formats a localized message using the catalog's stable placeholders.
     * Missing locale entries fall back to English before formatting.
     *
     * @param key message key
     * @param arguments format arguments
     * @return formatted player-facing message
     */
    public String format(String key, Object... arguments) {
        return String.format(Locale.ROOT, text(key), arguments);
    }

    /**
     * Returns the material keys required by the completed desktop flows.
     *
     * @return defensive copy of required keys
     */
    public static String[] requiredKeys() {
        return REQUIRED_KEYS.clone();
    }

    /**
     * Checks the selected locale itself rather than accepting an English
     * fallback. This makes localization coverage tests identify the exact key.
     *
     * @param key material key
     * @return whether this locale has an explicit value for the key
     */
    public boolean hasTranslation(String key) {
        Map<String, String> values = STRINGS.get(tag);
        return values != null && values.containsKey(key);
    }

    /**
     * Checks whether a tag is one of the supported desktop locales.
     *
     * @param tag candidate language tag
     * @return whether the tag is supported
     */
    public static boolean isSupported(String tag) {
        if (tag == null) {
            return false;
        }
        for (String supported : SUPPORTED_TAGS) {
            if (supported.equalsIgnoreCase(tag.trim())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Map<String, String>> createStrings() {
        Map<String, Map<String, String>> all = new LinkedHashMap<>();
        all.put("en", english());
        all.put("zh-TW", traditionalChinese());
        all.put("ja-JP", japanese());
        return Collections.unmodifiableMap(all);
    }

    private static Map<String, String> english() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("game", "Game");
        values.put("assist", "Assist");
        values.put("solver", "Solver");
        values.put("help", "Help");
        values.put("new3", "New 3x3");
        values.put("new4", "New 4x4");
        values.put("new5", "New 5x5");
        values.put("restart", "Restart This Puzzle");
        values.put("undo", "Undo");
        values.put("redo", "Redo");
        values.put("history", "Move History");
        values.put("save", "Save Game");
        values.put("load", "Load Game");
        values.put("records", "Records");
        values.put("daily", "Daily Calendar");
        values.put("favorites", "Favorites");
        values.put("trends", "Trends / Weekly Goal");
        values.put("continuous", "Continuous Challenge");
        values.put("preferences", "Preferences");
        values.put("quickReminder", "Quick Reminder");
        values.put("exit", "Exit");
        values.put("showMovable", "Show Movable Tiles");
        values.put("howToPlay", "How to Play");
        values.put("beginnerGuide", "Beginner Guide");
        values.put("practiceTutorial", "Practice Tutorial");
        values.put("continueLoad", "Continue / Load");
        values.put("resetSaved", "Reset Saved Games");
        values.put("resetRecords", "Reset Records & Statistics");
        values.put("reduceMotion", "Reduce motion");
        values.put("sound", "Sound feedback");
        values.put("theme", "Theme");
        values.put("language", "Language");
        values.put("close", "Close");
        values.put("back", "Back");
        values.put("next", "Next");
        values.put("previous", "Previous");
        values.put("skip", "Skip");
        values.put("start", "Start 3x3");
        values.put("resetLesson", "Reset Lesson");
        values.put("startTutorialPuzzle", "Start 3x3 Puzzle");
        values.put("resetSavedConfirm", "Clear normal, Daily, Favorite Practice, and Continuous saves?\nFavorite labels, records, statistics, Daily history, and preferences stay.");
        values.put("resetRecordsConfirm", "Clear best records, completion history/statistics, and Daily streak state?\nActive Continuous Challenge and preferences stay.");
        values.put("homeSummary", "Home | New Game, Continue, Daily, Favorites, Trends, Continuous, Records");
        values.put("noSaves", "No saved games yet.");
        values.put("firstRunSubtitle", "Learn the rules, try one guided move, then start playing.");
        values.put("historyNoMoves", "No moves yet. Your current run history will appear here.");
        values.put("historyCounts", "%d completed actions · %d available to redo");
        values.put("historyAllUndone", "All completed actions are currently undone.");
        values.put("historyLatest50", "Showing the latest 50 actions.");
        values.put("historyAction", "%d. Empty %s");
        values.put("historyWholeLine", "× %d (one move)");
        values.put("direction.up", "up"); values.put("direction.down", "down");
        values.put("direction.left", "left"); values.put("direction.right", "right");
        values.put("strategicHint", "Strategic Hint");
        values.put("strategicHintUnavailable", "No strategic hint is available for this position.");
        values.put("strategicHintStatus", "Hint: try tile %d (empty cell moves %s)");
        values.put("solverTools", "Solver Tools");
        values.put("warning", "Warning");
        values.put("solverBfs", "Solve with BFS (best for 3x3)");
        values.put("solverAStar", "Solve with A* (best for 4x4+)");
        values.put("solverIdaStar", "Solve with IDA* (mobile-friendly core)");
        values.put("solverCancel", "Cancel Solver");
        values.put("solverWarningBfs", "BFS on 4x4 or larger may use substantial memory or time. Continue?");
        values.put("solverWarningAStar", "A* on 5x5 can be very slow or memory-heavy. Continue?");
        values.put("solverWarningIdaStar", "IDA* on 5x5 can take a long time. Continue?");
        values.put("solverFound", "Solution found: %d moves.\nAnimate it now?");
        values.put("solverNoSolution", "No solution found or the search timed out.");
        values.put("solverCancelled", "Solver cancelled. The current puzzle was not changed.");
        values.put("solverError", "Solver could not complete. You can keep playing or try a smaller board.");
        values.put("assistedRun", "Assisted run");
        values.put("assistedResult", "Solved with assist.");
        values.put("playerBestProtected", "Assist result not saved as a player best.");
        values.put("favoriteSolved", "Favorite Practice solved.");
        values.put("favoriteIsolation", "Practice result not saved to records, history, statistics, or Daily streaks.");
        values.put("favoritePracticeSummary", "Favorite Practice is isolated: saves, records, completion history, lifetime statistics, and Daily streaks are not changed.");
        values.put("dailyCompleted", "Completed"); values.put("dailyInProgress", "In progress"); values.put("dailyReady", "Ready"); values.put("dailyMissed", "Missed"); values.put("dailyFuture", "Future date"); values.put("dailyPuzzle", "Daily 4x4 Classic puzzle."); values.put("dailyProgress", "Daily streak: %d · Best: %d");
        values.put("continuousResume", "Resume %d/%d · %dx%d · %s"); values.put("continuousStatus", "Continuous: %d/%d puzzles · %d moves · %ds · Assisted: %d"); values.put("continuousResult", "Continuous Challenge\n\n%s\nScope: %dx%d · %s\n\n%s"); values.put("continuousComplete", "Challenge complete."); values.put("continuousRetained", "The solved puzzle is retained for resume or Next Puzzle.");
        values.put("trendTitle", "Trends and Weekly Goal"); values.put("trendScope", "Scope: %dx%d · %s"); values.put("trendGoal", "Weekly goal: %d/%d"); values.put("trendReached", "reached"); values.put("trendWeek", "Week: %s to %s"); values.put("trendNoData", "No player completions in this scope yet."); values.put("trendExclusion", "Assisted and Favorite Practice results are excluded."); values.put("trendRecent", "Recent player solves: %d · average %d moves, %ds"); values.put("trendNeedData", "Comparison: need at least six player solves."); values.put("trendPrevious", "Previous window: %d moves, %ds"); values.put("trendMoves", "Moves: %s (%d%%)"); values.put("trendTime", "Time: %s (%d%%)"); values.put("trendImproving", "improving"); values.put("trendDeclining", "declining"); values.put("trendSteady", "steady"); values.put("trendNotEnough", "not enough data");
        values.put("recordsPlayer", "Player solves: %d"); values.put("recordsAssisted", "Assisted: %d"); values.put("recordsFootnote", "Player records use size and difficulty scopes; fewer moves rank first; ties use faster time.\nAssisted completions remain in history/statistics but never replace player best records.");
        values.put("recordsPlayerOnly", "Player solves only. Fewer moves rank first; ties use faster time."); values.put("recordsAssistedNote", "Solver-assisted completions do not replace player best records."); values.put("recordsScopeRecord", "%dx%d: %s");
        values.put("preferencesDescription", "Desktop preferences affect only presentation. Puzzle rules and records remain unchanged.");
        values.put("recordPlayerBest", "Assist result not saved. Player best: %s"); values.put("recordFirst", "First player record for this size and difficulty."); values.put("recordFirstSize", "First player record for this size."); values.put("recordNewBest", "New best. Previous best: %s"); values.put("recordBestRemains", "Best remains: %s"); values.put("moveSingular", "%d move"); values.put("movePlural", "%d moves");
        values.put("difficultyRelaxed", "Relaxed"); values.put("difficultyClassic", "Classic");
        values.put("difficultyChallenge", "Challenge");
        values.put("cellEmptyName", "Empty cell, row %d, column %d");
        values.put("cellTileName", "Tile %d, row %d, column %d");
        values.put("cellEmptyDescription", "Empty cell. Select another cell or use the arrow keys to move the empty cell.");
        values.put("cellMovableDescription", "Movable tile. Press Space or Enter to slide this aligned tile.");
        values.put("cellNotAlignedDescription", "Tile is not aligned with the empty cell right now.");
        values.put("boardAccessibleName", "Puzzle board");
        values.put("boardAccessibleDescription", "Keyboard-accessible sliding puzzle board. Tab between cells and use arrow keys to move the empty cell.");
        values.put("winTitle", "Winner!");
        values.put("winMessage", "Congratulations! You won in %d moves.\nTime: %ds");
        values.put("windowTitle", "Number Klotski - Java Edition");
        values.put("statusInitial", "Moves: 0 | Time: 0s");
        values.put("statusAccessibleDescription", "Current moves, elapsed time, difficulty, and record status.");
        values.put("statusAccessibleName", "Game status");
        values.put("statusSeparator", " | ");
        values.put("menuBarAccessibleName", "SlideDo menu bar");
        values.put("homeAccessibleName", "SlideDo Home");
        values.put("menuGameDescription", "Open game creation, save, reset, and navigation commands.");
        values.put("menuAssistDescription", "Open presentation-only assistance commands.");
        values.put("menuSolverDescription", "Open solver commands.");
        values.put("menuHelpDescription", "Open learning and help commands.");
        values.put("homeAccessibleDescription", "Choose a puzzle size, continue a saved game, open learning, or open settings.");
        values.put("homeScrollAccessibleDescription", "Scrollable Home actions for puzzle modes, learning, records, and preferences.");
        values.put("buttonActivate", "Activate %s.");
        values.put("startOrLoadFirst", "Start or load a game first.");
        values.put("saveSuccess", "Game saved.");
        values.put("saveFailure", "Could not save game.");
        values.put("noSaveFile", "No save file found.");
        values.put("savedGameChooserPrompt", "Choose a saved game to continue.");
        values.put("savedGamesTitle", "Saved Games");
        values.put("savedGameSummary", "Saved: %dx%d · %s · %d moves · %ds");
        values.put("savedGamesAvailable", "%d independent saved games available.");
        values.put("gameLoaded", "Game loaded!");
        values.put("difficultyChooserPrompt", "Choose a difficulty for the %dx%d puzzle.");
        values.put("difficultyTitle", "Difficulty");
        values.put("scrambleMoves", "%s — %d scramble moves");
        values.put("dailyCalendarSubtitle", "%dx%d · %s · choose today or replay an earlier offline puzzle.");
        values.put("dailyLegend", "+ Completed   ~ In progress   ! Missed   · Future");
        values.put("weekdaySun", "Sun"); values.put("weekdayMon", "Mon"); values.put("weekdayTue", "Tue");
        values.put("weekdayWed", "Wed"); values.put("weekdayThu", "Thu"); values.put("weekdayFri", "Fri"); values.put("weekdaySat", "Sat");
        values.put("beginnerGuideTextName", "Beginner guide text");
        values.put("learningPageDescription", "Current learning page.");
        values.put("learningPreviousDescription", "Show the previous learning page.");
        values.put("learningNextDescription", "Show the next learning page.");
        values.put("learningSkipDescription", "Close the guide and mark onboarding as seen.");
        values.put("learningPracticeDescription", "Open the isolated practice tutorial.");
        values.put("learningStartDescription", "Start a normal 3x3 puzzle.");
        values.put("practiceBoardName", "Practice tutorial board");
        values.put("tutorialResetDescription", "Restart the isolated tutorial board.");
        values.put("tutorialStartDescription", "Leave the tutorial and start a normal 3x3 puzzle.");
        values.put("tutorialCloseDescription", "Close the practice tutorial.");
        values.put("tutorialFirstMove", "1 / 2: make an adjacent move");
        values.put("tutorialWholeLine", "2 / 2: try a farther aligned tile");
        values.put("tutorialComplete", "Complete: you can start a normal puzzle.");
        values.put("learningGoalTitle", "Goal");
        values.put("learningGoalBody", "Arrange the numbers in order and leave the empty cell in the bottom-right corner.");
        values.put("learningMoveTitle", "Move");
        values.put("learningMoveBody", "Click or swipe a tile in the same row or column as the empty cell.");
        values.put("learningWholeLineTitle", "Whole-line slides");
        values.put("learningWholeLineBody", "Choosing a farther aligned tile moves the whole line as one move and one undo.");
        values.put("learningStartTitle", "Start playing");
        values.put("learningStartBody", "Hints and the solver are for learning; only player solves update best records.");
        values.put("howToPlayTitle", "How to Play"); values.put("howToPlayGoal", "Goal");
        values.put("howToPlayGoalBody", "Arrange the numbers in row-major order with the empty cell at the end.");
        values.put("howToPlayMoves", "Moves"); values.put("howToPlayMovesBody", "Click or swipe a tile in the same row or column as the empty cell.");
        values.put("howToPlayWholeLine", "Whole-line slides"); values.put("howToPlayWholeLineBody", "A whole-line slide moves every tile between that tile and the empty cell and counts as one move.");
        values.put("howToPlayAssist", "Assist"); values.put("howToPlayAssistBody", "Use Show Movable Tiles to highlight tiles that can slide now. Solver-assisted completions do not replace player best records.");
        values.put("practiceTitle", "Practice Tutorial"); values.put("practiceStepOne", "1. Find a tile in the same row or column as the empty cell.");
        values.put("practiceStepTwo", "2. Click an adjacent tile to make your first move."); values.put("practiceStepThree", "3. Try a farther aligned tile to see a whole-line slide.");
        values.put("practiceFootnote", "This interactive practice mirrors the Android guided first puzzle and uses the shared GameModel.");
        values.put("reminderTitle", "Quick Reminder"); values.put("reminderEmpty", "• The empty cell is the destination.");
        values.put("reminderAligned", "• Any aligned tile can slide into it."); values.put("reminderWholeLine", "• A whole-line slide counts as one move and one undo.");
        values.put("reminderSolver", "• Solver-assisted wins do not update player best records.");
        values.put("dailyDayAccessibility", "%s. %s. %s");
        values.put("recordFormat", "%d moves, %ds");
        values.put("saveCurrentFavorite", "Save current puzzle as Favorite");
        values.put("noFavorites", "No favorites saved yet. Start a normal puzzle to save one.\n\n%s");
        values.put("favoriteNamePrompt", "Name this exact starting puzzle (up to 40 characters).");
        values.put("favoriteDefaultLabel", "Favorite");
        values.put("favoriteSaveFailure", "Favorite could not be saved.");
        values.put("favoriteSaveSuccess", "Favorite saved: %s");
        values.put("favoriteReplay", "Replay Favorite");
        values.put("favoriteRename", "Rename");
        values.put("favoriteDelete", "Delete");
        values.put("favoriteCancel", "Cancel");
        values.put("favoriteDeletePrompt", "Delete this favorite and its practice save?");
        values.put("favoriteDeleteTitle", "Delete Favorite");
        values.put("favoriteRenamePrompt", "New favorite label (up to 40 characters).");
        values.put("favoriteRenameFailure", "Favorite label was not changed.");
        values.put("trendChangeScope", "Change Scope");
        values.put("trendSetGoal", "Set Weekly Goal");
        values.put("trendClose", "Close");
        values.put("trendScopePrompt", "Choose the size and difficulty used by Trends and Weekly Goal.");
        values.put("trendScopeTitle", "Trend Scope");
        values.put("weeklyGoalPrompt", "Weekly player-completion target (1-50).");
        values.put("weeklyGoalTitle", "Weekly Goal");
        values.put("weeklyGoalInvalid", "Weekly goal must be a whole number from 1 through 50.");
        values.put("continuousEndSaved", "End saved challenge");
        values.put("continuousStartPuzzles", "Start %d puzzles");
        values.put("continuousSetupDescription", "One fixed size/difficulty scope; progress is isolated from normal, Daily, and Favorite Practice saves.");
        values.put("continuousTitle", "Continuous Challenge");
        values.put("continuousScopePrompt", "Choose the fixed scope for this challenge.");
        values.put("continuousScopeTitle", "Continuous Scope");
        values.put("lastPuzzle", "Last puzzle");
        values.put("nextPuzzle", "Next Puzzle");
        values.put("endChallenge", "End Challenge");
        values.put("home", "Home");
        values.put("replayPuzzle", "Replay Puzzle");
        values.put("newSize", "New Size");
        values.put("favoriteResultsTitle", "Favorite Practice");
        values.put("resultsTitle", "Results");
        values.put("continuousResultsTitle", "Continuous Results");
        values.put("chooseLanguageDescription", "Choose the desktop language.");
        values.put("chooseThemeDescription", "Choose the desktop color theme.");
        values.put("reducedMotionDescription", "Disable board transition animation without changing puzzle rules.");
        values.put("soundDescription", "Enable or disable desktop move and completion feedback.");
        values.put("resetSavedDescription", "Delete saved-game domains after explicit confirmation.");
        values.put("resetRecordsDescription", "Delete records, completion statistics, and daily streak state after explicit confirmation.");
        values.put("preferencesAccessibleDescription", "Choose language, theme, sound, reduced motion, or reset one persisted domain.");
        values.put("backupExport", "Export Personal Data");
        values.put("backupRestore", "Restore Personal Data");
        values.put("backupExportDescription", "Write a versioned local backup of all Desktop personal data.");
        values.put("backupRestoreDescription", "Replace all managed Desktop personal data from a validated local backup.");
        values.put("backupFileFilter", "SlideDo JSON backup (*.json)");
        values.put("backupExported", "Personal data backup exported.");
        values.put("backupRestored", "Personal data restored. The current Desktop session was reloaded.");
        values.put("backupExportFailure", "Personal data backup could not be exported.");
        values.put("backupRestoreFailure", "Personal data could not be restored; the previous state was retained.");
        values.put("backupInvalid", "The selected backup is invalid or unsupported. No data was changed.");
        values.put("backupRestoreConfirm", "Restore this backup as a full replacement? Managed Desktop saves, records, history, settings, Daily, Favorites, and Continuous data will be replaced.");
        values.put("backupBusy", "Wait for the current board animation or solver operation to finish before using backup.");
        values.put("backupRestoreCleanupWarning", "Personal data restore succeeded, but cleanup was incomplete. Retained recovery data: %s. You may remove that recovery directory after inspecting it.");
        values.put("backupRestoreRecoveryRequired", "Personal-data restore could not be completed safely. Automatic rollback was incomplete. Recovery data was retained at: %s\nPrevious snapshot: %s\nFurther automatic saves are disabled until restart or recovery.");
        values.put("recoveryRequiredStatus", "Personal-data recovery is required. Normal gameplay and automatic saves are disabled.");
        values.put("savedGamesCleared", "Saved-game domains cleared.");
        values.put("savedGamesClearFailure", "Some saved-game files could not be cleared.");
        values.put("recordsCleared", "Records and statistics cleared.");
        values.put("recordsClearFailure", "Some record files could not be cleared.");
        values.put("statusRunning", "Moves: %d | Time: %ds | Difficulty: %s | %s%s%s%s%s%s%s");
        values.put("statusBest", "Best: %s");
        values.put("statusReducedMotion", " | Reduced motion");
        values.put("statusDaily", " | Daily: %s");
        values.put("statusFavorite", " | Favorite Practice");
        values.put("statusSolved", "Solved! Moves: %d | Time: %ds | Difficulty: %s | Best: %s");
        values.put("statusFavoriteSolved", "Favorite solved! Moves: %d | Time: %ds | Difficulty: %s");
        values.put("bestNone", "--");
        values.put("lastPuzzleSummary", "Last puzzle: %s · %ds");
        values.put("puzzleSolved", "Puzzle solved.");
        values.put("puzzleTitle", "%dx%d Puzzle");
        values.put("difficultyLabel", "Difficulty: %s");
        values.put("timeLabel", "%s   Time: %ds");
        values.put("noRecord", "No record yet");
        values.put("favoritePuzzleTitle", "%dx%d Puzzle");
        values.put("favoriteResultIsolation", "Practice result not saved to records, history, statistics, or Daily streaks.");
        values.put("homeIndependentSaves", "%d independent saved games available.");
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, String> traditionalChinese() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("game", "遊戲"); values.put("assist", "輔助"); values.put("solver", "求解器"); values.put("help", "說明");
        values.put("new3", "新建 3x3"); values.put("new4", "新建 4x4"); values.put("new5", "新建 5x5");
        values.put("restart", "重新開始本題"); values.put("undo", "復原"); values.put("redo", "重做"); values.put("history", "移動記錄");
        values.put("save", "儲存遊戲"); values.put("load", "載入遊戲"); values.put("records", "紀錄"); values.put("daily", "每日挑戰日曆");
        values.put("favorites", "收藏題目"); values.put("trends", "趨勢／每週目標"); values.put("continuous", "連續挑戰"); values.put("preferences", "偏好設定");
        values.put("quickReminder", "快速提醒"); values.put("exit", "離開"); values.put("showMovable", "顯示可移動方塊");
        values.put("howToPlay", "玩法說明"); values.put("beginnerGuide", "新手指南"); values.put("practiceTutorial", "練習教學"); values.put("continueLoad", "繼續／載入");
        values.put("resetSaved", "重置已儲存遊戲"); values.put("resetRecords", "重置紀錄與統計"); values.put("reduceMotion", "減少動態效果"); values.put("sound", "音效提示");
        values.put("theme", "主題"); values.put("language", "語言"); values.put("close", "關閉"); values.put("back", "返回"); values.put("next", "下一步"); values.put("previous", "上一步"); values.put("skip", "略過"); values.put("start", "開始 3x3");
        values.put("resetLesson", "重置課程"); values.put("startTutorialPuzzle", "開始 3x3 題目"); values.put("resetSavedConfirm", "要清除一般、每日、收藏練習與連續挑戰的儲存嗎？\n收藏標籤、紀錄、統計、每日歷史與偏好設定會保留。"); values.put("resetRecordsConfirm", "要清除最佳紀錄、完成歷史／統計與每日連勝狀態嗎？\n進行中的連續挑戰與偏好設定會保留。"); values.put("homeSummary", "首頁｜新遊戲、繼續、每日、收藏、趨勢、連續、紀錄"); values.put("noSaves", "目前沒有已儲存的遊戲。"); values.put("firstRunSubtitle", "先學會規則、完成引導移動，再開始遊玩。");
        values.put("historyNoMoves", "目前還沒有移動，這一局的記錄會顯示在這裡。");
        values.put("historyCounts", "已完成 %d 個動作 · 可重做 %d 個動作");
        values.put("historyAllUndone", "所有已完成動作目前都已復原。");
        values.put("historyLatest50", "顯示最近 50 個動作。");
        values.put("historyAction", "%d. 空格向%s"); values.put("historyWholeLine", "× %d（一次移動）");
        values.put("direction.up", "上"); values.put("direction.down", "下"); values.put("direction.left", "左"); values.put("direction.right", "右");
        values.put("strategicHint", "策略提示"); values.put("strategicHintUnavailable", "目前局面沒有可用的策略提示。");
        values.put("strategicHintStatus", "提示：嘗試方塊 %d（空格向%s移動）"); values.put("solverTools", "求解器工具");
        values.put("solverTools", "求解器工具"); values.put("solverBfs", "使用 BFS 求解（適合 3x3）"); values.put("solverAStar", "使用 A* 求解（適合 4x4 以上）"); values.put("solverIdaStar", "使用 IDA* 求解（適合行動核心）"); values.put("solverCancel", "取消求解器");
        values.put("warning", "警告");
        values.put("solverWarningBfs", "4x4 以上使用 BFS 可能需要大量記憶體或時間。要繼續嗎？"); values.put("solverWarningAStar", "5x5 使用 A* 可能非常慢或耗用大量記憶體。要繼續嗎？"); values.put("solverWarningIdaStar", "5x5 使用 IDA* 可能需要很久。要繼續嗎？");
        values.put("solverFound", "找到解答：%d 步。\n要現在播放嗎？"); values.put("solverNoSolution", "找不到解答或搜尋逾時。"); values.put("solverCancelled", "已取消求解器，目前題目沒有變更。"); values.put("solverError", "求解器無法完成。你可以繼續遊玩或改用較小棋盤。");
        values.put("assistedRun", "輔助遊戲"); values.put("assistedResult", "使用輔助完成。"); values.put("playerBestProtected", "輔助結果不會寫入玩家最佳紀錄。");
        values.put("favoriteSolved", "收藏練習完成。"); values.put("favoriteIsolation", "練習結果不會寫入紀錄、歷史、統計或每日連勝。");
        values.put("favoritePracticeSummary", "收藏練習彼此隔離：不會變更儲存、紀錄、完成歷史、終身統計或每日連勝。");
        values.put("dailyCompleted", "已完成"); values.put("dailyInProgress", "進行中"); values.put("dailyReady", "可開始"); values.put("dailyMissed", "錯過"); values.put("dailyFuture", "未來日期"); values.put("dailyPuzzle", "每日 4x4 經典題目。"); values.put("dailyProgress", "每日連勝：%d · 最佳：%d");
        values.put("continuousResume", "繼續 %d/%d · %dx%d · %s"); values.put("continuousStatus", "連續挑戰：%d/%d 題 · %d 步 · %d 秒 · 輔助：%d"); values.put("continuousResult", "連續挑戰\n\n%s\n範圍：%dx%d · %s\n\n%s"); values.put("continuousComplete", "挑戰完成。"); values.put("continuousRetained", "已完成題目會保留，方便繼續或開始下一題。");
        values.put("trendTitle", "趨勢與每週目標"); values.put("trendScope", "範圍：%dx%d · %s"); values.put("trendGoal", "每週目標：%d/%d"); values.put("trendReached", "已達成"); values.put("trendWeek", "週期：%s 至 %s"); values.put("trendNoData", "此範圍尚無玩家完成紀錄。"); values.put("trendExclusion", "輔助與收藏練習結果不列入。"); values.put("trendRecent", "最近玩家完成：%d · 平均 %d 步、%d 秒"); values.put("trendNeedData", "比較：至少需要六次玩家完成。"); values.put("trendPrevious", "前一區間：%d 步、%d 秒"); values.put("trendMoves", "步數：%s（%d%%）"); values.put("trendTime", "時間：%s（%d%%）"); values.put("trendImproving", "改善"); values.put("trendDeclining", "變差"); values.put("trendSteady", "持平"); values.put("trendNotEnough", "資料不足");
        values.put("recordsPlayer", "玩家完成：%d"); values.put("recordsAssisted", "輔助：%d"); values.put("recordsFootnote", "玩家紀錄依棋盤大小與難度分開；步數越少越好，同步數以時間較快者優先。\n輔助完成仍保留在歷史／統計，但不會取代玩家最佳紀錄。");
        values.put("recordsPlayerOnly", "僅玩家完成。步數越少越好；同歩數以時間較快者優先。"); values.put("recordsAssistedNote", "求解器完成不會取代玩家最佳紀錄。"); values.put("recordsScopeRecord", "%dx%d：%s"); values.put("preferencesDescription", "桌面偏好只影響呈現。拼圖規則與紀錄保持不變。");
        values.put("recordPlayerBest", "輔助結果不儲存。玩家最佳：%s"); values.put("recordFirst", "此大小與難度的第一筆玩家紀錄。"); values.put("recordFirstSize", "此大小的第一筆玩家紀錄。"); values.put("recordNewBest", "新最佳。之前最佳：%s"); values.put("recordBestRemains", "最佳維持：%s"); values.put("moveSingular", "%d 步"); values.put("movePlural", "%d 步");
        values.put("difficultyRelaxed", "輕鬆"); values.put("difficultyClassic", "經典"); values.put("difficultyChallenge", "挑戰");
        values.put("cellEmptyName", "空格，第 %d 列，第 %d 欄"); values.put("cellTileName", "方塊 %d，第 %d 列，第 %d 欄"); values.put("cellEmptyDescription", "空格。選擇其他方塊，或使用方向鍵移動空格。"); values.put("cellMovableDescription", "可移動方塊。按空白鍵或 Enter 將對齊方塊滑入。"); values.put("cellNotAlignedDescription", "此方塊目前未與空格對齊。"); values.put("boardAccessibleName", "拼圖棋盤"); values.put("boardAccessibleDescription", "可用鍵盤操作的滑塊拼圖棋盤。使用 Tab 在方塊間移動，方向鍵移動空格。");
        values.put("winTitle", "完成！"); values.put("winMessage", "恭喜！你用了 %d 步完成。\n時間：%d 秒");
        values.put("windowTitle", "數字華容道 - Java 版");
        values.put("statusInitial", "步數：0｜時間：0 秒");
        values.put("statusAccessibleDescription", "目前步數、經過時間、難度與紀錄狀態。");
        values.put("statusAccessibleName", "遊戲狀態"); values.put("statusSeparator", "｜"); values.put("menuBarAccessibleName", "SlideDo 選單列"); values.put("homeAccessibleName", "SlideDo 首頁");
        values.put("menuGameDescription", "開啟建立遊戲、儲存、重置與導覽指令。");
        values.put("menuAssistDescription", "開啟只影響呈現的輔助指令。");
        values.put("menuSolverDescription", "開啟求解器指令。");
        values.put("menuHelpDescription", "開啟學習與說明指令。");
        values.put("homeAccessibleDescription", "選擇拼圖大小、繼續已儲存遊戲、開啟學習或設定。");
        values.put("homeScrollAccessibleDescription", "可捲動的首頁動作，包含遊戲模式、學習、紀錄與偏好。");
        values.put("buttonActivate", "啟用%s。");
        values.put("startOrLoadFirst", "請先開始或載入遊戲。");
        values.put("saveSuccess", "遊戲已儲存。");
        values.put("saveFailure", "無法儲存遊戲。");
        values.put("noSaveFile", "找不到儲存檔。");
        values.put("savedGameChooserPrompt", "選擇要繼續的已儲存遊戲。");
        values.put("savedGamesTitle", "已儲存遊戲");
        values.put("savedGameSummary", "已儲存：%dx%d · %s · %d 步 · %d 秒");
        values.put("savedGamesAvailable", "有 %d 個獨立的已儲存遊戲可用。");
        values.put("gameLoaded", "遊戲已載入！");
        values.put("difficultyChooserPrompt", "選擇 %dx%d 拼圖的難度。");
        values.put("difficultyTitle", "難度");
        values.put("scrambleMoves", "%s — %d 次打亂移動");
        values.put("dailyCalendarSubtitle", "%dx%d · %s · 選擇今天或重玩較早的離線題目。");
        values.put("dailyLegend", "+ 已完成   ~ 進行中   ! 錯過   · 未來");
        values.put("weekdaySun", "週日"); values.put("weekdayMon", "週一"); values.put("weekdayTue", "週二");
        values.put("weekdayWed", "週三"); values.put("weekdayThu", "週四"); values.put("weekdayFri", "週五"); values.put("weekdaySat", "週六");
        values.put("beginnerGuideTextName", "新手指南文字");
        values.put("learningPageDescription", "目前學習頁面。");
        values.put("learningPreviousDescription", "顯示上一個學習頁面。");
        values.put("learningNextDescription", "顯示下一個學習頁面。");
        values.put("learningSkipDescription", "關閉指南並標記已看過導覽。");
        values.put("learningPracticeDescription", "開啟隔離的練習教學。");
        values.put("learningStartDescription", "開始一般 3x3 拼圖。");
        values.put("practiceBoardName", "練習教學棋盤");
        values.put("tutorialResetDescription", "重新開始隔離的教學棋盤。");
        values.put("tutorialStartDescription", "離開教學並開始一般 3x3 拼圖。");
        values.put("tutorialCloseDescription", "關閉練習教學。");
        values.put("tutorialFirstMove", "1 / 2：完成相鄰移動");
        values.put("tutorialWholeLine", "2 / 2：嘗試較遠的對齊方塊");
        values.put("tutorialComplete", "完成：你可以開始一般拼圖。");
        values.put("learningGoalTitle", "目標"); values.put("learningGoalBody", "依序排列數字，空格必須在右下角。");
        values.put("learningMoveTitle", "移動"); values.put("learningMoveBody", "點擊或滑動與空格同列或同欄的方塊。");
        values.put("learningWholeLineTitle", "整列滑動"); values.put("learningWholeLineBody", "點擊較遠的對齊方塊，整列會算作一次移動，也只佔一次復原。");
        values.put("learningStartTitle", "開始遊玩"); values.put("learningStartBody", "提示與求解器適合學習；只有玩家完成會更新最佳紀錄。");
        values.put("howToPlayTitle", "玩法說明"); values.put("howToPlayGoal", "目標"); values.put("howToPlayGoalBody", "依序排列數字，空格在右下角。");
        values.put("howToPlayMoves", "移動"); values.put("howToPlayMovesBody", "點擊或滑動與空格同列或同欄的方塊。");
        values.put("howToPlayWholeLine", "整列滑動"); values.put("howToPlayWholeLineBody", "整列滑動算一次移動，符合 Android 規則。");
        values.put("howToPlayAssist", "輔助"); values.put("howToPlayAssistBody", "使用「顯示可移動方塊」查看目前可移動的方塊。求解器完成不會取代玩家最佳紀錄。");
        values.put("practiceTitle", "練習教學"); values.put("practiceStepOne", "1. 找到與空格同列或同欄的方塊。"); values.put("practiceStepTwo", "2. 點擊相鄰方塊完成第一步。");
        values.put("practiceStepThree", "3. 再點擊較遠的對齊方塊，觀察整列滑動。"); values.put("practiceFootnote", "這個互動練習對齊 Android 引導式第一題，使用共用 GameModel 規則。");
        values.put("reminderTitle", "快速提醒"); values.put("reminderEmpty", "• 空格是移動目標。"); values.put("reminderAligned", "• 同列或同欄的方塊都可滑入空格。");
        values.put("reminderWholeLine", "• 整列滑動算一次移動與一次復原。"); values.put("reminderSolver", "• 求解器完成不會更新玩家最佳紀錄。");
        values.put("dailyDayAccessibility", "%s。%s。%s"); values.put("recordFormat", "%d 步，%d 秒");
        values.put("saveCurrentFavorite", "將目前題目儲存為收藏");
        values.put("noFavorites", "尚未儲存收藏題目。請先開始一般拼圖。\n\n%s");
        values.put("favoriteNamePrompt", "為這個精確起始題目命名（最多 40 個字元）。");
        values.put("favoriteDefaultLabel", "收藏");
        values.put("favoriteSaveFailure", "無法儲存收藏題目。");
        values.put("favoriteSaveSuccess", "收藏已儲存：%s");
        values.put("favoriteReplay", "重玩收藏"); values.put("favoriteRename", "重新命名"); values.put("favoriteDelete", "刪除"); values.put("favoriteCancel", "取消");
        values.put("favoriteDeletePrompt", "要刪除此收藏及其練習存檔嗎？"); values.put("favoriteDeleteTitle", "刪除收藏");
        values.put("favoriteRenamePrompt", "新的收藏名稱（最多 40 個字元）。"); values.put("favoriteRenameFailure", "收藏名稱未變更。");
        values.put("trendChangeScope", "變更範圍"); values.put("trendSetGoal", "設定每週目標"); values.put("trendClose", "關閉");
        values.put("trendScopePrompt", "選擇趨勢與每週目標使用的大小與難度。"); values.put("trendScopeTitle", "趨勢範圍");
        values.put("weeklyGoalPrompt", "每週玩家完成目標（1-50）。"); values.put("weeklyGoalTitle", "每週目標"); values.put("weeklyGoalInvalid", "每週目標必須是 1 到 50 的整數。");
        values.put("continuousEndSaved", "結束已儲存挑戰"); values.put("continuousStartPuzzles", "開始 %d 題");
        values.put("continuousSetupDescription", "固定大小／難度範圍；進度與一般、每日及收藏練習存檔隔離。");
        values.put("continuousTitle", "連續挑戰"); values.put("continuousScopePrompt", "選擇此挑戰的固定範圍。"); values.put("continuousScopeTitle", "連續挑戰範圍");
        values.put("lastPuzzle", "最後一題"); values.put("nextPuzzle", "下一題"); values.put("endChallenge", "結束挑戰"); values.put("home", "首頁"); values.put("replayPuzzle", "重玩題目"); values.put("newSize", "新大小");
        values.put("favoriteResultsTitle", "收藏練習"); values.put("resultsTitle", "結果"); values.put("continuousResultsTitle", "連續挑戰結果");
        values.put("chooseLanguageDescription", "選擇桌面語言。"); values.put("chooseThemeDescription", "選擇桌面色彩主題。");
        values.put("backupExport", "匯出個人資料"); values.put("backupRestore", "還原個人資料");
        values.put("backupExportDescription", "將所有桌面個人資料寫入版本化的本機備份。");
        values.put("backupRestoreDescription", "使用已驗證的本機備份完整取代桌面管理資料。");
        values.put("backupFileFilter", "SlideDo JSON 備份 (*.json)");
        values.put("backupExported", "個人資料備份已匯出。"); values.put("backupRestored", "個人資料已還原。桌面目前工作階段已重新載入。");
        values.put("backupExportFailure", "無法匯出個人資料備份。"); values.put("backupRestoreFailure", "無法還原個人資料；已保留原本狀態。");
        values.put("backupInvalid", "選取的備份無效或不受支援，資料未變更。");
        values.put("backupRestoreConfirm", "要以完整取代方式還原此備份嗎？桌面管理的存檔、紀錄、歷史、設定、每日、收藏與連續挑戰資料都會被取代。");
        values.put("backupBusy", "請先等待目前棋盤動畫或求解器操作完成，再使用備份功能。");
        values.put("backupRestoreCleanupWarning", "個人資料已成功還原，但清理未完成。保留的復原資料：%s。檢查後即可移除此復原目錄。");
        values.put("backupRestoreRecoveryRequired", "個人資料無法安全完成還原。自動回復未完成，已保留復原資料：%s\n還原前快照：%s\n重新啟動或完成復原前，將停用後續自動儲存。");
        values.put("recoveryRequiredStatus", "需要復原個人資料。一般遊玩與自動儲存已停用。");
        values.put("reducedMotionDescription", "停用棋盤轉場動畫，但不改變拼圖規則。"); values.put("soundDescription", "啟用或停用桌面移動與完成提示音。");
        values.put("resetSavedDescription", "在明確確認後刪除已儲存遊戲範圍。"); values.put("resetRecordsDescription", "在明確確認後刪除紀錄、完成統計與每日連勝狀態。");
        values.put("preferencesAccessibleDescription", "選擇語言、主題、音效、減少動態效果，或重置一個儲存範圍。");
        values.put("savedGamesCleared", "已清除已儲存遊戲範圍。"); values.put("savedGamesClearFailure", "部分遊戲存檔無法清除。");
        values.put("recordsCleared", "紀錄與統計已清除。"); values.put("recordsClearFailure", "部分紀錄檔案無法清除。");
        values.put("statusRunning", "步數：%d｜時間：%d 秒｜難度：%s｜%s%s%s%s%s%s%s"); values.put("statusBest", "最佳：%s");
        values.put("statusReducedMotion", "｜減少動態效果"); values.put("statusDaily", "｜每日：%s"); values.put("statusFavorite", "｜收藏練習");
        values.put("statusSolved", "完成！步數：%d｜時間：%d 秒｜難度：%s｜最佳：%s"); values.put("statusFavoriteSolved", "收藏完成！步數：%d｜時間：%d 秒｜難度：%s");
        values.put("bestNone", "--"); values.put("lastPuzzleSummary", "最後一題：%s · %d 秒"); values.put("puzzleSolved", "拼圖完成。");
        values.put("puzzleTitle", "%dx%d 拼圖"); values.put("difficultyLabel", "難度：%s"); values.put("timeLabel", "%s   時間：%d 秒"); values.put("noRecord", "尚無紀錄");
        values.put("favoritePuzzleTitle", "%dx%d 拼圖"); values.put("favoriteResultIsolation", "練習結果不會寫入紀錄、歷史、統計或每日連勝。"); values.put("homeIndependentSaves", "有 %d 個獨立的已儲存遊戲可用。");
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, String> japanese() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("game", "ゲーム"); values.put("assist", "アシスト"); values.put("solver", "ソルバー"); values.put("help", "ヘルプ");
        values.put("new3", "3x3を開始"); values.put("new4", "4x4を開始"); values.put("new5", "5x5を開始"); values.put("restart", "このパズルを再開");
        values.put("undo", "元に戻す"); values.put("redo", "やり直す"); values.put("history", "手順履歴"); values.put("save", "ゲームを保存"); values.put("load", "ゲームを読み込む");
        values.put("records", "記録"); values.put("daily", "デイリーカレンダー"); values.put("favorites", "お気に入り"); values.put("trends", "傾向／週間目標"); values.put("continuous", "連続チャレンジ"); values.put("preferences", "設定"); values.put("quickReminder", "クイックリマインダー"); values.put("exit", "終了");
        values.put("showMovable", "動かせるタイルを表示"); values.put("howToPlay", "遊び方"); values.put("beginnerGuide", "初心者ガイド"); values.put("practiceTutorial", "練習チュートリアル"); values.put("continueLoad", "続き／読み込み");
        values.put("resetSaved", "保存ゲームをリセット"); values.put("resetRecords", "記録と統計をリセット"); values.put("reduceMotion", "動きを減らす"); values.put("sound", "サウンド通知"); values.put("theme", "テーマ"); values.put("language", "言語");
        values.put("close", "閉じる"); values.put("back", "戻る"); values.put("next", "次へ"); values.put("previous", "前へ"); values.put("skip", "スキップ"); values.put("start", "3x3を開始"); values.put("resetLesson", "レッスンをリセット"); values.put("startTutorialPuzzle", "3x3パズルを開始"); values.put("resetSavedConfirm", "通常、デイリー、お気に入り練習、連続チャレンジの保存を消去しますか？\nお気に入り名、記録、統計、デイリー履歴、設定は保持されます。"); values.put("resetRecordsConfirm", "ベスト記録、完了履歴／統計、デイリー連勝を消去しますか？\n進行中の連続チャレンジと設定は保持されます。"); values.put("homeSummary", "ホーム｜新規、続き、デイリー、お気に入り、傾向、連続、記録"); values.put("noSaves", "保存されたゲームはありません。"); values.put("firstRunSubtitle", "ルールを学び、ガイド付きの一手を試してから始めましょう。");
        values.put("solverTools", "ソルバーツール"); values.put("historyNoMoves", "まだ手順はありません。現在の履歴がここに表示されます。"); values.put("historyCounts", "完了した操作 %d · やり直し可能 %d"); values.put("historyAllUndone", "完了した操作はすべて元に戻されています。"); values.put("historyLatest50", "最新 50 件を表示しています。"); values.put("historyAction", "%d. 空白を%sへ"); values.put("historyWholeLine", "× %d（1手）");
        values.put("warning", "警告");
        values.put("direction.up", "上"); values.put("direction.down", "下"); values.put("direction.left", "左"); values.put("direction.right", "右"); values.put("strategicHint", "戦略ヒント"); values.put("strategicHintUnavailable", "この局面には戦略ヒントがありません。"); values.put("strategicHintStatus", "ヒント：タイル %d を試す（空白が%sへ移動）"); values.put("solverTools", "ソルバーツール"); values.put("solverBfs", "BFSで解く（3x3向け）"); values.put("solverAStar", "A*で解く（4x4以上向け）"); values.put("solverIdaStar", "IDA*で解く（モバイル向けコア）"); values.put("solverCancel", "ソルバーをキャンセル");
        values.put("solverWarningBfs", "4x4以上のBFSは大量のメモリや時間を使う場合があります。続行しますか？"); values.put("solverWarningAStar", "5x5のA*は非常に遅いか、大量のメモリを使う場合があります。続行しますか？"); values.put("solverWarningIdaStar", "5x5のIDA*は長時間かかる場合があります。続行しますか？"); values.put("solverFound", "解答が見つかりました：%d手。\n今すぐ再生しますか？"); values.put("solverNoSolution", "解答が見つからないか、検索がタイムアウトしました。"); values.put("solverCancelled", "ソルバーをキャンセルしました。現在のパズルは変更されていません。"); values.put("solverError", "ソルバーを完了できませんでした。続けるか、小さい盤面を試してください。");
        values.put("assistedRun", "アシストプレイ"); values.put("assistedResult", "アシストでクリア。"); values.put("playerBestProtected", "アシスト結果はプレイヤー記録に保存されません。"); values.put("difficultyRelaxed", "イージー"); values.put("difficultyClassic", "クラシック"); values.put("difficultyChallenge", "チャレンジ");
        values.put("favoriteSolved", "お気に入り練習をクリア。"); values.put("favoriteIsolation", "練習結果は記録、履歴、統計、デイリー連勝に保存されません。");
        values.put("favoritePracticeSummary", "お気に入り練習は独立しています。保存、記録、完了履歴、統計、デイリー連勝は変更されません。");
        values.put("dailyCompleted", "完了"); values.put("dailyInProgress", "進行中"); values.put("dailyReady", "開始可能"); values.put("dailyMissed", "未達成"); values.put("dailyFuture", "未来の日付"); values.put("dailyPuzzle", "デイリー4x4クラシックパズル。"); values.put("dailyProgress", "デイリー連勝：%d · ベスト：%d");
        values.put("continuousResume", "再開 %d/%d · %dx%d · %s"); values.put("continuousStatus", "連続：%d/%d パズル · %d手 · %d秒 · アシスト：%d"); values.put("continuousResult", "連続チャレンジ\n\n%s\n範囲：%dx%d · %s\n\n%s"); values.put("continuousComplete", "チャレンジ完了。"); values.put("continuousRetained", "クリアしたパズルは再開または次のパズル用に保持されます。");
        values.put("trendTitle", "傾向と週間目標"); values.put("trendScope", "範囲：%dx%d · %s"); values.put("trendGoal", "週間目標：%d/%d"); values.put("trendReached", "達成"); values.put("trendWeek", "週：%s から %s"); values.put("trendNoData", "この範囲にはプレイヤーの完了記録がありません。"); values.put("trendExclusion", "アシストとお気に入り練習は除外されます。"); values.put("trendRecent", "最近のクリア：%d · 平均 %d手、%d秒"); values.put("trendNeedData", "比較には少なくとも6回のプレイヤークリアが必要です。"); values.put("trendPrevious", "前の期間：%d手、%d秒"); values.put("trendMoves", "手数：%s（%d%%）"); values.put("trendTime", "時間：%s（%d%%）"); values.put("trendImproving", "改善"); values.put("trendDeclining", "悪化"); values.put("trendSteady", "安定"); values.put("trendNotEnough", "データ不足");
        values.put("recordsPlayer", "プレイヤークリア：%d"); values.put("recordsAssisted", "アシスト：%d"); values.put("recordsFootnote", "プレイヤー記録はサイズと難易度ごとに管理し、手数が少ない順、同数なら時間が短い順です。\nアシスト完了は履歴／統計に残りますが、プレイヤー記録を置き換えません。");
        values.put("recordsPlayerOnly", "プレイヤークリアのみ。手数が少ない順、同数なら時間が短い順です。"); values.put("recordsAssistedNote", "ソルバーのクリアはプレイヤー記録を置き換えません。"); values.put("recordsScopeRecord", "%dx%d：%s"); values.put("preferencesDescription", "デスクトップ設定は表示だけに影響します。パズルのルールと記録は変わりません。");
        values.put("recordPlayerBest", "アシスト結果は保存されません。プレイヤー記録：%s"); values.put("recordFirst", "このサイズと難易度の最初のプレイヤー記録です。"); values.put("recordFirstSize", "このサイズの最初のプレイヤー記録です。"); values.put("recordNewBest", "新記録。以前の記録：%s"); values.put("recordBestRemains", "記録：%s"); values.put("moveSingular", "%d手"); values.put("movePlural", "%d手");
        values.put("cellEmptyName", "空白、%d行 %d列"); values.put("cellTileName", "タイル %d、%d行 %d列"); values.put("cellEmptyDescription", "空白です。別のタイルを選ぶか、矢印キーで空白を動かします。"); values.put("cellMovableDescription", "移動可能なタイルです。SpaceまたはEnterで揃ったタイルを動かします。"); values.put("cellNotAlignedDescription", "このタイルは現在空白と揃っていません。"); values.put("boardAccessibleName", "パズル盤面"); values.put("boardAccessibleDescription", "キーボードで操作できるスライドパズル盤面。Tabでセルを移動し、矢印キーで空白を動かします。");
        values.put("winTitle", "クリア！"); values.put("winMessage", "おめでとうございます！%d手でクリアしました。\n時間：%d秒");
        values.put("learningGoalTitle", "目標"); values.put("learningGoalBody", "数字を順番に並べ、空きマスを右下に置きます。");
        values.put("learningMoveTitle", "移動"); values.put("learningMoveBody", "空きマスと同じ行または列のタイルをクリック／スワイプします。");
        values.put("learningWholeLineTitle", "列の移動"); values.put("learningWholeLineBody", "遠いタイルを選ぶと一列が一手として動き、元に戻すのも一回です。");
        values.put("learningStartTitle", "開始"); values.put("learningStartBody", "ヒントとソルバーは学習用です。プレイヤーのクリアだけが記録を更新します。");
        values.put("howToPlayTitle", "遊び方"); values.put("howToPlayGoal", "目標"); values.put("howToPlayGoalBody", "数字を行優先で並べ、空きマスを最後にします。");
        values.put("howToPlayMoves", "移動"); values.put("howToPlayMovesBody", "空きマスと同じ行または列のタイルをクリック／スワイプします。");
        values.put("howToPlayWholeLine", "列全体の移動"); values.put("howToPlayWholeLineBody", "列全体の移動は一手として数えます。");
        values.put("howToPlayAssist", "アシスト"); values.put("howToPlayAssistBody", "「動かせるタイルを表示」で現在の候補を確認できます。ソルバーのクリアはプレイヤー記録を更新しません。");
        values.put("practiceTitle", "練習チュートリアル"); values.put("practiceStepOne", "1. 空きマスと同じ行または列のタイルを探します。"); values.put("practiceStepTwo", "2. 隣のタイルをクリックして最初の一手を試します。");
        values.put("practiceStepThree", "3. 遠いタイルを選び、列の移動を確認します。"); values.put("practiceFootnote", "Android のガイド付き最初のパズルと同じ GameModel ルールです。");
        values.put("reminderTitle", "クイックリマインダー"); values.put("reminderEmpty", "• 空きマスが移動先です。"); values.put("reminderAligned", "• 同じ行または列のタイルを動かせます。");
        values.put("reminderWholeLine", "• 列全体の移動は一手／一回の元に戻すです。"); values.put("reminderSolver", "• ソルバーのクリアはプレイヤー記録を更新しません。");
        values.put("dailyDayAccessibility", "%s。%s。%s"); values.put("recordFormat", "%d手、%d秒");
        values.put("windowTitle", "数字スライド - Java版");
        values.put("statusInitial", "手数：0 | 時間：0秒");
        values.put("statusAccessibleDescription", "現在の手数、経過時間、難易度、記録状態。");
        values.put("statusAccessibleName", "ゲーム状態"); values.put("statusSeparator", " | "); values.put("menuBarAccessibleName", "SlideDo メニューバー"); values.put("homeAccessibleName", "SlideDo ホーム");
        values.put("menuGameDescription", "ゲーム作成、保存、リセット、移動のコマンドを開きます。");
        values.put("menuAssistDescription", "表示だけに影響するアシストコマンドを開きます。");
        values.put("menuSolverDescription", "ソルバーコマンドを開きます。");
        values.put("menuHelpDescription", "学習とヘルプのコマンドを開きます。");
        values.put("homeAccessibleDescription", "サイズを選び、保存ゲームを続け、学習や設定を開きます。");
        values.put("homeScrollAccessibleDescription", "ゲームモード、学習、記録、設定のホーム操作をスクロールします。");
        values.put("buttonActivate", "%sを実行。");
        values.put("startOrLoadFirst", "先にゲームを開始または読み込んでください。");
        values.put("saveSuccess", "ゲームを保存しました。"); values.put("saveFailure", "ゲームを保存できませんでした。"); values.put("noSaveFile", "保存ゲームが見つかりません。");
        values.put("savedGameChooserPrompt", "続ける保存ゲームを選択してください。"); values.put("savedGamesTitle", "保存ゲーム");
        values.put("savedGameSummary", "保存：%dx%d · %s · %d手 · %d秒"); values.put("savedGamesAvailable", "独立した保存ゲームが%d件あります。"); values.put("gameLoaded", "ゲームを読み込みました！");
        values.put("difficultyChooserPrompt", "%dx%dパズルの難易度を選択してください。"); values.put("difficultyTitle", "難易度"); values.put("scrambleMoves", "%s — シャッフル%d手");
        values.put("dailyCalendarSubtitle", "%dx%d · %s · 今日または過去のオフラインパズルを選びます。"); values.put("dailyLegend", "+ 完了   ~ 進行中   ! 未達成   · 未来");
        values.put("weekdaySun", "日"); values.put("weekdayMon", "月"); values.put("weekdayTue", "火"); values.put("weekdayWed", "水"); values.put("weekdayThu", "木"); values.put("weekdayFri", "金"); values.put("weekdaySat", "土");
        values.put("beginnerGuideTextName", "初心者ガイド本文"); values.put("learningPageDescription", "現在の学習ページ。"); values.put("learningPreviousDescription", "前の学習ページを表示。"); values.put("learningNextDescription", "次の学習ページを表示。");
        values.put("learningSkipDescription", "ガイドを閉じて導入済みにする。"); values.put("learningPracticeDescription", "独立した練習チュートリアルを開く。"); values.put("learningStartDescription", "通常の3x3パズルを開始。"); values.put("practiceBoardName", "練習チュートリアル盤面");
        values.put("tutorialResetDescription", "独立したチュートリアル盤面を再開。"); values.put("tutorialStartDescription", "チュートリアルを終了して通常の3x3を開始。"); values.put("tutorialCloseDescription", "練習チュートリアルを閉じる。");
        values.put("tutorialFirstMove", "1 / 2：隣のタイルを動かす"); values.put("tutorialWholeLine", "2 / 2：遠い整列タイルを試す"); values.put("tutorialComplete", "完了：通常のパズルを開始できます。");
        values.put("saveCurrentFavorite", "現在のパズルをお気に入りに保存"); values.put("noFavorites", "お気に入りはまだありません。通常のパズルを開始して保存してください。\n\n%s"); values.put("favoriteNamePrompt", "この開始パズルに名前を付けます（40文字まで）。"); values.put("favoriteDefaultLabel", "お気に入り");
        values.put("favoriteSaveFailure", "お気に入りを保存できませんでした。"); values.put("favoriteSaveSuccess", "お気に入りを保存しました：%s"); values.put("favoriteReplay", "お気に入りを再生"); values.put("favoriteRename", "名前変更"); values.put("favoriteDelete", "削除"); values.put("favoriteCancel", "キャンセル");
        values.put("favoriteDeletePrompt", "このお気に入りと練習セーブを削除しますか？"); values.put("favoriteDeleteTitle", "お気に入りを削除"); values.put("favoriteRenamePrompt", "新しいお気に入り名（40文字まで）。"); values.put("favoriteRenameFailure", "お気に入り名は変更されませんでした。");
        values.put("trendChangeScope", "範囲を変更"); values.put("trendSetGoal", "週間目標を設定"); values.put("trendClose", "閉じる"); values.put("trendScopePrompt", "傾向と週間目標に使うサイズと難易度を選択。"); values.put("trendScopeTitle", "傾向の範囲");
        values.put("weeklyGoalPrompt", "週間プレイヤークリア目標（1〜50）。"); values.put("weeklyGoalTitle", "週間目標"); values.put("weeklyGoalInvalid", "週間目標は1から50までの整数です。");
        values.put("continuousEndSaved", "保存したチャレンジを終了"); values.put("continuousStartPuzzles", "%d問を開始"); values.put("continuousSetupDescription", "固定サイズ／難易度の範囲。通常、デイリー、お気に入り練習とは別に進行します。"); values.put("continuousTitle", "連続チャレンジ"); values.put("continuousScopePrompt", "このチャレンジの固定範囲を選択。"); values.put("continuousScopeTitle", "連続チャレンジの範囲");
        values.put("lastPuzzle", "最後のパズル"); values.put("nextPuzzle", "次のパズル"); values.put("endChallenge", "チャレンジを終了"); values.put("home", "ホーム"); values.put("replayPuzzle", "パズルを再生"); values.put("newSize", "新しいサイズ");
        values.put("favoriteResultsTitle", "お気に入り練習"); values.put("resultsTitle", "結果"); values.put("continuousResultsTitle", "連続チャレンジの結果"); values.put("chooseLanguageDescription", "デスクトップ言語を選択。"); values.put("chooseThemeDescription", "デスクトップのカラーテーマを選択。");
        values.put("backupExport", "個人データを書き出す"); values.put("backupRestore", "個人データを復元");
        values.put("backupExportDescription", "すべてのデスクトップ個人データをバージョン付きのローカルバックアップに保存します。");
        values.put("backupRestoreDescription", "検証済みバックアップで管理対象のデスクトップ個人データを完全に置き換えます。");
        values.put("backupFileFilter", "SlideDo JSON バックアップ (*.json)");
        values.put("backupExported", "個人データのバックアップを書き出しました。"); values.put("backupRestored", "個人データを復元しました。現在のデスクトップセッションを再読み込みしました。");
        values.put("backupExportFailure", "個人データのバックアップを書き出せませんでした。"); values.put("backupRestoreFailure", "個人データを復元できませんでした。以前の状態を保持しました。");
        values.put("backupInvalid", "選択したバックアップは無効または未対応です。データは変更されていません。");
        values.put("backupRestoreConfirm", "このバックアップを完全置換として復元しますか？管理対象の保存、記録、履歴、設定、デイリー、お気に入り、連続チャレンジが置き換えられます。");
        values.put("backupBusy", "バックアップを使う前に、盤面のアニメーションまたはソルバー処理が終わるまで待ってください。");
        values.put("backupRestoreCleanupWarning", "個人データの復元は成功しましたが、クリーンアップが未完了です。保持された復旧データ：%s。確認後にこの復旧フォルダーを削除できます。");
        values.put("backupRestoreRecoveryRequired", "個人データを安全に復元できませんでした。自動ロールバックが未完了のため、復旧データを保持しています：%s\n復元前のスナップショット：%s\n再起動または復旧が完了するまで自動保存を無効にします。");
        values.put("recoveryRequiredStatus", "個人データの復旧が必要です。通常のプレイと自動保存は無効です。");
        values.put("reducedMotionDescription", "パズルのルールを変えずに盤面の遷移アニメーションを無効化。"); values.put("soundDescription", "デスクトップの移動・完了サウンドを有効／無効化。"); values.put("resetSavedDescription", "確認後に保存ゲームの範囲を削除。"); values.put("resetRecordsDescription", "確認後に記録、完了統計、デイリー連勝を削除。"); values.put("preferencesAccessibleDescription", "言語、テーマ、サウンド、動きの軽減、保存範囲のリセットを選択。");
        values.put("savedGamesCleared", "保存ゲームの範囲を削除しました。"); values.put("savedGamesClearFailure", "一部の保存ゲームを削除できませんでした。"); values.put("recordsCleared", "記録と統計を削除しました。"); values.put("recordsClearFailure", "一部の記録ファイルを削除できませんでした。");
        values.put("statusRunning", "手数：%d | 時間：%d秒 | 難易度：%s | %s%s%s%s%s%s%s"); values.put("statusBest", "記録：%s"); values.put("statusReducedMotion", " | 動きを減らす"); values.put("statusDaily", " | デイリー：%s"); values.put("statusFavorite", " | お気に入り練習");
        values.put("statusSolved", "クリア！手数：%d | 時間：%d秒 | 難易度：%s | 記録：%s"); values.put("statusFavoriteSolved", "お気に入りをクリア！手数：%d | 時間：%d秒 | 難易度：%s"); values.put("bestNone", "--"); values.put("lastPuzzleSummary", "最後のパズル：%s · %d秒"); values.put("puzzleSolved", "パズルをクリア。");
        values.put("puzzleTitle", "%dx%d パズル"); values.put("difficultyLabel", "難易度：%s"); values.put("timeLabel", "%s   時間：%d秒"); values.put("noRecord", "記録なし"); values.put("favoritePuzzleTitle", "%dx%d パズル"); values.put("favoriteResultIsolation", "練習結果は記録、履歴、統計、デイリー連勝に保存されません。"); values.put("homeIndependentSaves", "独立した保存ゲームが%d件あります。");
        return Collections.unmodifiableMap(values);
    }
}
