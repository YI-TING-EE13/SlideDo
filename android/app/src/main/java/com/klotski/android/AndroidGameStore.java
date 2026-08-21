package com.klotski.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.klotski.core.DailyChallenge;
import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SharedPreferences-backed Android state store for app-level data.
 * <p>
 * This class owns the Android persistence schema for independent per-size
 * saves, records, completion history, personal statistics, settings,
 * onboarding, and the last selected puzzle size and difficulty. A legacy
 * single-save payload is migrated into its matching size slot without
 * overwriting a newer slot. Puzzle rules and validation still belong to
 * {@link GameModel}; persisted game data is loaded back into the shared model
 * before any gameplay behavior runs.
 * </p>
 */
final class AndroidGameStore {
    static final String PREFS_NAME = "slidedo";

    private static final String KEY_SIZE = "size";
    private static final String KEY_GRID = "grid";
    private static final String KEY_INITIAL_GRID = "initial_grid";
    private static final String KEY_MOVES = "moves";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_SOLVED = "solved";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_ASSISTED = "assisted";
    private static final String KEY_SAVE_PREFIX = "save_";
    private static final String KEY_DAILY_SAVE_PREFIX = "daily_save_";
    private static final String KEY_DAILY_SAVE_DATE = "daily_save_date";
    private static final String KEY_DAILY_COMPLETED_DATES = "daily_completed_dates_v1";
    private static final String KEY_DAILY_LAST_COMPLETED_DATE = "daily_last_completed_date";
    private static final String KEY_DAILY_CURRENT_STREAK = "daily_current_streak";
    private static final String KEY_DAILY_BEST_STREAK = "daily_best_streak";
    private static final String KEY_LAST_SIZE = "last_size";
    private static final String KEY_LAST_DIFFICULTY = "last_difficulty";
    private static final String KEY_BEST_PREFIX = "best_";
    private static final String KEY_STATS_PREFIX = "stats_";
    private static final String KEY_COMPLETION_HISTORY = "completion_history_v1";
    private static final String KEY_PLAYER_COMPLETIONS = "player_completions";
    private static final String KEY_ASSISTED_COMPLETIONS = "assisted_completions";
    private static final String KEY_PLAYER_MOVES = "player_moves";
    private static final String KEY_PLAYER_TIME = "player_time";
    private static final int MAX_COMPLETION_HISTORY = 50;
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    private static final String KEY_REDUCED_MOTION = "reduced_motion";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VISUAL_THEME = "visual_theme";
    static final String KEY_LANGUAGE_TAG = "language_tag";

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

    PuzzleDifficulty getLastDifficulty() {
        return PuzzleDifficulty.fromId(prefs.getString(
                KEY_LAST_DIFFICULTY, PuzzleDifficulty.CLASSIC.getId()));
    }

    void setLastDifficulty(PuzzleDifficulty difficulty) {
        if (difficulty != null) {
            prefs.edit().putString(KEY_LAST_DIFFICULTY, difficulty.getId()).apply();
        }
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

    boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, false);
    }

    void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    AndroidVisualTheme getVisualTheme() {
        return AndroidVisualTheme.fromId(prefs.getString(
                KEY_VISUAL_THEME, AndroidVisualTheme.MIDNIGHT.id));
    }

    void setVisualTheme(AndroidVisualTheme theme) {
        AndroidVisualTheme selected = theme == null ? AndroidVisualTheme.MIDNIGHT : theme;
        prefs.edit().putString(KEY_VISUAL_THEME, selected.id).apply();
    }

    String getLanguageTag() {
        return AndroidAppLocale.normalizeLanguageTag(
                prefs.getString(KEY_LANGUAGE_TAG, AndroidAppLocale.DEFAULT_LANGUAGE_TAG));
    }

    void setLanguageTag(String languageTag) {
        prefs.edit().putString(KEY_LANGUAGE_TAG,
                AndroidAppLocale.normalizeLanguageTag(languageTag)).apply();
    }

    void saveGame(GameModel model, long elapsedMs) {
        saveGame(model, elapsedMs, false);
    }

    void saveGame(GameModel model, long elapsedMs, boolean assisted) {
        if (model == null || !isSupportedSize(model.getSize())) {
            return;
        }
        String prefix = savePrefix(model.getSize());
        SharedPreferences.Editor editor = prefs.edit();
        putSave(editor, prefix, model.getSize(), model.getGridCopy(), model.getInitialGridCopy(),
                model.getMoveCount(), Math.max(0, elapsedMs), System.currentTimeMillis(),
                model.isGameRunning(), model.isSolved(), model.getDifficulty());
        editor.putBoolean(prefix + KEY_ASSISTED, assisted)
                .putInt(KEY_LAST_SIZE, model.getSize()).apply();
    }

    void saveDailyGame(String dateId, GameModel model, long elapsedMs) {
        saveDailyGame(dateId, model, elapsedMs, false);
    }

    void saveDailyGame(String dateId, GameModel model, long elapsedMs, boolean assisted) {
        DailyChallenge challenge = parseDailyChallenge(dateId);
        if (challenge == null || model == null
                || model.getSize() != challenge.getSize()
                || model.getDifficulty() != challenge.getDifficulty()
                || !Arrays.deepEquals(model.getInitialGridCopy(),
                        challenge.createGame().getInitialGridCopy())) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        putSave(editor, KEY_DAILY_SAVE_PREFIX, model.getSize(), model.getGridCopy(),
                model.getInitialGridCopy(), model.getMoveCount(), Math.max(0, elapsedMs),
                System.currentTimeMillis(), model.isGameRunning(), model.isSolved(),
                model.getDifficulty());
        editor.putBoolean(KEY_DAILY_SAVE_PREFIX + KEY_ASSISTED, assisted)
                .putString(KEY_DAILY_SAVE_DATE, challenge.getDateId()).apply();
    }

    SaveManager.SaveData loadDailyGame(String dateId) {
        DailyChallenge challenge = parseDailyChallenge(dateId);
        if (challenge == null
                || !challenge.getDateId().equals(prefs.getString(KEY_DAILY_SAVE_DATE, null))) {
            return null;
        }
        SaveManager.SaveData data = readSavedGame(KEY_DAILY_SAVE_PREFIX, challenge.getSize());
        if (data == null || data.difficulty != challenge.getDifficulty()
                || !Arrays.deepEquals(data.initialGrid,
                        challenge.createGame().getInitialGridCopy())) {
            return null;
        }
        return data;
    }

    SaveMetadata getDailySaveMetadata(String dateId) {
        SaveManager.SaveData data = loadDailyGame(dateId);
        if (data == null) {
            return null;
        }
        return new SaveMetadata(data.updatedAt, data.size, data.moveCount,
                data.elapsedTime, data.active, data.solved, data.difficulty);
    }

    boolean isDailyGameAssisted(String dateId) {
        return loadDailyGame(dateId) != null
                && prefs.getBoolean(KEY_DAILY_SAVE_PREFIX + KEY_ASSISTED, false);
    }

    boolean recordDailyCompletion(String dateId) {
        DailyChallenge challenge = parseDailyChallenge(dateId);
        if (challenge == null) {
            return false;
        }
        String canonicalDateId = challenge.getDateId();
        Set<String> completedDates = new HashSet<>(prefs.getStringSet(
                KEY_DAILY_COMPLETED_DATES, Collections.emptySet()));
        if (!completedDates.add(canonicalDateId)) {
            return false;
        }

        LocalDate completedDate = LocalDate.parse(canonicalDateId);
        String storedLastId = prefs.getString(KEY_DAILY_LAST_COMPLETED_DATE, null);
        LocalDate storedLast = parseLocalDate(storedLastId);
        int currentStreak = Math.max(0, prefs.getInt(KEY_DAILY_CURRENT_STREAK, 0));
        int bestStreak = Math.max(0, prefs.getInt(KEY_DAILY_BEST_STREAK, 0));
        String lastCompletedDateId = storedLastId;
        if (storedLast == null || completedDate.isAfter(storedLast)) {
            currentStreak = storedLast != null && completedDate.equals(storedLast.plusDays(1))
                    ? currentStreak + 1 : 1;
            bestStreak = Math.max(bestStreak, currentStreak);
            lastCompletedDateId = canonicalDateId;
        }

        prefs.edit()
                .putStringSet(KEY_DAILY_COMPLETED_DATES, completedDates)
                .putString(KEY_DAILY_LAST_COMPLETED_DATE, lastCompletedDateId)
                .putInt(KEY_DAILY_CURRENT_STREAK, currentStreak)
                .putInt(KEY_DAILY_BEST_STREAK, bestStreak)
                .commit();
        return true;
    }

    DailyProgress getDailyProgress(String dateId) {
        DailyChallenge challenge = parseDailyChallenge(dateId);
        if (challenge == null) {
            return DailyProgress.EMPTY;
        }
        String canonicalDateId = challenge.getDateId();
        LocalDate today = LocalDate.parse(canonicalDateId);
        Set<String> completedDates = prefs.getStringSet(
                KEY_DAILY_COMPLETED_DATES, Collections.emptySet());
        String lastCompletedDateId = prefs.getString(KEY_DAILY_LAST_COMPLETED_DATE, null);
        LocalDate lastCompleted = parseLocalDate(lastCompletedDateId);
        int storedCurrent = Math.max(0, prefs.getInt(KEY_DAILY_CURRENT_STREAK, 0));
        boolean streakIsCurrent = lastCompleted != null
                && (lastCompleted.equals(today) || lastCompleted.equals(today.minusDays(1)));
        return new DailyProgress(completedDates.contains(canonicalDateId),
                streakIsCurrent ? storedCurrent : 0,
                Math.max(0, prefs.getInt(KEY_DAILY_BEST_STREAK, 0)),
                lastCompleted == null ? null : lastCompleted.toString());
    }

    SaveManager.SaveData loadSavedGame() {
        migrateLegacySaveIfNeeded();
        int preferredSize = getLastSize(4);
        SaveManager.SaveData preferred = readSavedGame(savePrefix(preferredSize), preferredSize);
        if (preferred != null) {
            return preferred;
        }

        SaveMetadata latest = null;
        for (SaveMetadata metadata : getAllSaveMetadata()) {
            if (latest == null || metadata.updatedAt > latest.updatedAt) {
                latest = metadata;
            }
        }
        return latest == null ? null : readSavedGame(savePrefix(latest.size), latest.size);
    }

    SaveManager.SaveData loadSavedGame(int size) {
        if (!isSupportedSize(size)) {
            return null;
        }
        migrateLegacySaveIfNeeded();
        return readSavedGame(savePrefix(size), size);
    }

    boolean isSavedGameAssisted(int size) {
        return loadSavedGame(size) != null
                && prefs.getBoolean(savePrefix(size) + KEY_ASSISTED, false);
    }

    SaveMetadata getSaveMetadata() {
        SaveManager.SaveData data = loadSavedGame();
        if (data == null) {
            return null;
        }
        return new SaveMetadata(data.updatedAt, data.size, data.moveCount,
                data.elapsedTime, data.active, data.solved, data.difficulty);
    }

    SaveMetadata getSaveMetadata(int size) {
        SaveManager.SaveData data = loadSavedGame(size);
        if (data == null) {
            return null;
        }
        return new SaveMetadata(data.updatedAt, data.size, data.moveCount,
                data.elapsedTime, data.active, data.solved, data.difficulty);
    }

    SaveMetadata[] getAllSaveMetadata() {
        migrateLegacySaveIfNeeded();
        List<SaveMetadata> saves = new ArrayList<>();
        for (int size = 3; size <= 5; size++) {
            SaveManager.SaveData data = readSavedGame(savePrefix(size), size);
            if (data != null) {
                saves.add(new SaveMetadata(data.updatedAt, data.size, data.moveCount,
                        data.elapsedTime, data.active, data.solved, data.difficulty));
            }
        }
        return saves.toArray(new SaveMetadata[0]);
    }

    boolean hasSavedGame() {
        return getAllSaveMetadata().length > 0;
    }

    boolean hasSavedGame(int size) {
        return loadSavedGame(size) != null;
    }

    void clearSavedGame() {
        SharedPreferences.Editor editor = prefs.edit();
        removeSave(editor, "");
        for (int size = 3; size <= 5; size++) {
            removeSave(editor, savePrefix(size));
        }
        removeSave(editor, KEY_DAILY_SAVE_PREFIX);
        editor.remove(KEY_DAILY_SAVE_DATE);
        editor.commit();
    }

    Best getBest(int size) {
        return getBest(size, PuzzleDifficulty.CLASSIC);
    }

    Best getBest(int size, PuzzleDifficulty difficulty) {
        String prefix = difficultyBestPrefix(size, difficulty);
        int moves = prefs.getInt(prefix + "_moves", -1);
        long timeMs = prefs.getLong(prefix + "_time", -1);
        if ((moves < 0 || timeMs < 0) && difficulty == PuzzleDifficulty.CLASSIC) {
            // Original Android releases keyed best records by size only.
            moves = prefs.getInt(KEY_BEST_PREFIX + size + "_moves", -1);
            timeMs = prefs.getLong(KEY_BEST_PREFIX + size + "_time", -1);
        }
        if (moves < 0 || timeMs < 0) {
            return null;
        }
        return new Best(moves, timeMs);
    }

    boolean recordBestIfBetter(int size, int moves, long timeMs) {
        return recordBestIfBetter(size, PuzzleDifficulty.CLASSIC, moves, timeMs);
    }

    boolean recordBestIfBetter(int size, PuzzleDifficulty difficulty, int moves, long timeMs) {
        Best best = getBest(size, difficulty);
        if (!isBetterRecord(best, moves, timeMs)) {
            return false;
        }

        String prefix = difficultyBestPrefix(size, difficulty);
        prefs.edit()
                .putInt(prefix + "_moves", moves)
                .putLong(prefix + "_time", timeMs)
                .apply();
        return true;
    }

    void recordCompletion(int size, PuzzleDifficulty difficulty, int moves, long timeMs,
            boolean assisted) {
        recordCompletion(size, difficulty, moves, timeMs, assisted, System.currentTimeMillis());
    }

    void recordCompletion(int size, PuzzleDifficulty difficulty, int moves, long timeMs,
            boolean assisted, long completedAt) {
        if (!isSupportedSize(size) || moves < 0 || timeMs < 0 || completedAt < 0) {
            return;
        }
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        CompletionRecord completion = new CompletionRecord(
                completedAt, size, selected, moves, timeMs, assisted);
        CompletionRecord[] currentHistory = getCompletionHistory();
        StringBuilder encodedHistory = new StringBuilder(encodeCompletion(completion));
        int retained = Math.min(currentHistory.length, MAX_COMPLETION_HISTORY - 1);
        for (int index = 0; index < retained; index++) {
            encodedHistory.append('\n').append(encodeCompletion(currentHistory[index]));
        }

        String statsPrefix = completionStatsPrefix(size, selected);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_COMPLETION_HISTORY, encodedHistory.toString());
        if (assisted) {
            editor.putInt(statsPrefix + KEY_ASSISTED_COMPLETIONS,
                    prefs.getInt(statsPrefix + KEY_ASSISTED_COMPLETIONS, 0) + 1);
        } else {
            editor.putInt(statsPrefix + KEY_PLAYER_COMPLETIONS,
                    prefs.getInt(statsPrefix + KEY_PLAYER_COMPLETIONS, 0) + 1)
                    .putLong(statsPrefix + KEY_PLAYER_MOVES,
                            prefs.getLong(statsPrefix + KEY_PLAYER_MOVES, 0L) + moves)
                    .putLong(statsPrefix + KEY_PLAYER_TIME,
                            prefs.getLong(statsPrefix + KEY_PLAYER_TIME, 0L) + timeMs);
        }
        editor.apply();
    }

    CompletionRecord[] getCompletionHistory() {
        String encoded = prefs.getString(KEY_COMPLETION_HISTORY, "");
        if (encoded == null || encoded.isEmpty()) {
            return new CompletionRecord[0];
        }
        List<CompletionRecord> history = new ArrayList<>();
        for (String line : encoded.split("\\n")) {
            CompletionRecord record = parseCompletion(line);
            if (record != null) {
                history.add(record);
            }
            if (history.size() == MAX_COMPLETION_HISTORY) {
                break;
            }
        }
        return history.toArray(new CompletionRecord[0]);
    }

    CompletionStats getCompletionStats(int size, PuzzleDifficulty difficulty) {
        if (!isSupportedSize(size)) {
            return CompletionStats.EMPTY;
        }
        String prefix = completionStatsPrefix(size, difficulty);
        return new CompletionStats(
                prefs.getInt(prefix + KEY_PLAYER_COMPLETIONS, 0),
                prefs.getInt(prefix + KEY_ASSISTED_COMPLETIONS, 0),
                prefs.getLong(prefix + KEY_PLAYER_MOVES, 0L),
                prefs.getLong(prefix + KEY_PLAYER_TIME, 0L));
    }

    CompletionStats getOverallCompletionStats() {
        CompletionStats total = CompletionStats.EMPTY;
        for (int size = 3; size <= 5; size++) {
            for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
                total = total.plus(getCompletionStats(size, difficulty));
            }
        }
        return total;
    }

    void clearRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_COMPLETION_HISTORY)
                .remove(KEY_DAILY_COMPLETED_DATES)
                .remove(KEY_DAILY_LAST_COMPLETED_DATE)
                .remove(KEY_DAILY_CURRENT_STREAK)
                .remove(KEY_DAILY_BEST_STREAK);
        for (int size = 3; size <= 5; size++) {
            editor.remove(KEY_BEST_PREFIX + size + "_moves");
            editor.remove(KEY_BEST_PREFIX + size + "_time");
            for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
                String prefix = difficultyBestPrefix(size, difficulty);
                editor.remove(prefix + "_moves");
                editor.remove(prefix + "_time");
                String statsPrefix = completionStatsPrefix(size, difficulty);
                editor.remove(statsPrefix + KEY_PLAYER_COMPLETIONS);
                editor.remove(statsPrefix + KEY_ASSISTED_COMPLETIONS);
                editor.remove(statsPrefix + KEY_PLAYER_MOVES);
                editor.remove(statsPrefix + KEY_PLAYER_TIME);
            }
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

    private static String savePrefix(int size) {
        return KEY_SAVE_PREFIX + size + "_";
    }

    private static DailyChallenge parseDailyChallenge(String dateId) {
        try {
            return DailyChallenge.fromDateId(dateId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static LocalDate parseLocalDate(String dateId) {
        try {
            return dateId == null ? null : LocalDate.parse(dateId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private SaveManager.SaveData readSavedGame(String prefix, int expectedSize) {
        if (!prefs.contains(prefix + KEY_GRID)) {
            return null;
        }

        int fallbackSize = isSupportedSize(expectedSize) ? expectedSize : 4;
        int size = prefs.getInt(prefix + KEY_SIZE, fallbackSize);
        if (!isSupportedSize(size)
                || (isSupportedSize(expectedSize) && size != expectedSize)) {
            return null;
        }
        int[][] grid = parseGrid(prefs.getString(prefix + KEY_GRID, ""), size);
        if (grid == null) {
            return null;
        }
        int[][] initialGrid = parseGrid(prefs.getString(prefix + KEY_INITIAL_GRID, ""), size);
        if (initialGrid == null) {
            initialGrid = copyGrid(grid);
        }

        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = initialGrid;
        data.moveCount = prefs.getInt(prefix + KEY_MOVES, 0);
        data.elapsedTime = prefs.getLong(prefix + KEY_ELAPSED, 0);
        data.updatedAt = prefs.getLong(prefix + KEY_UPDATED_AT, 0);
        data.solved = prefs.getBoolean(prefix + KEY_SOLVED, false);
        data.active = prefs.getBoolean(prefix + KEY_ACTIVE, false);
        data.difficulty = PuzzleDifficulty.fromId(prefs.getString(prefix + KEY_DIFFICULTY, null));
        try {
            normalizeStateMetadata(data);
        } catch (RuntimeException exception) {
            return null;
        }
        return data;
    }

    private void migrateLegacySaveIfNeeded() {
        if (!prefs.contains(KEY_GRID)) {
            return;
        }

        SaveManager.SaveData legacy = readSavedGame("", 0);
        if (legacy == null) {
            return;
        }
        String targetPrefix = savePrefix(legacy.size);
        SaveManager.SaveData current = readSavedGame(targetPrefix, legacy.size);
        SharedPreferences.Editor editor = prefs.edit();
        if (current == null || legacy.updatedAt >= current.updatedAt) {
            putSave(editor, targetPrefix, legacy.size, legacy.grid, legacy.initialGrid,
                    legacy.moveCount, legacy.elapsedTime, legacy.updatedAt,
                    legacy.active, legacy.solved, legacy.difficulty);
        }
        removeSave(editor, "");
        editor.commit();
    }

    private static void putSave(SharedPreferences.Editor editor, String prefix, int size,
            int[][] grid, int[][] initialGrid, int moves, long elapsedMs, long updatedAt,
            boolean active, boolean solved, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        editor.putInt(prefix + KEY_SIZE, size)
                .putString(prefix + KEY_GRID, flatten(grid))
                .putString(prefix + KEY_INITIAL_GRID, flatten(initialGrid))
                .putInt(prefix + KEY_MOVES, moves)
                .putLong(prefix + KEY_ELAPSED, Math.max(0, elapsedMs))
                .putLong(prefix + KEY_UPDATED_AT, updatedAt)
                .putBoolean(prefix + KEY_ACTIVE, active)
                .putBoolean(prefix + KEY_SOLVED, solved)
                .putString(prefix + KEY_DIFFICULTY, selected.getId());
    }

    private static void removeSave(SharedPreferences.Editor editor, String prefix) {
        editor.remove(prefix + KEY_SIZE)
                .remove(prefix + KEY_GRID)
                .remove(prefix + KEY_INITIAL_GRID)
                .remove(prefix + KEY_MOVES)
                .remove(prefix + KEY_ELAPSED)
                .remove(prefix + KEY_UPDATED_AT)
                .remove(prefix + KEY_ACTIVE)
                .remove(prefix + KEY_SOLVED)
                .remove(prefix + KEY_DIFFICULTY)
                .remove(prefix + KEY_ASSISTED);
    }

    private static String difficultyBestPrefix(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        return KEY_BEST_PREFIX + size + "_" + selected.getId();
    }

    private static String completionStatsPrefix(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        return KEY_STATS_PREFIX + size + "_" + selected.getId() + "_";
    }

    private static String encodeCompletion(CompletionRecord record) {
        return record.completedAt + "," + record.size + "," + record.difficulty.getId()
                + "," + record.moves + "," + record.timeMs + "," + (record.assisted ? 1 : 0);
    }

    private static CompletionRecord parseCompletion(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String[] fields = encoded.split(",", -1);
        if (fields.length != 6 || !("0".equals(fields[5]) || "1".equals(fields[5]))) {
            return null;
        }
        try {
            long completedAt = Long.parseLong(fields[0]);
            int size = Integer.parseInt(fields[1]);
            PuzzleDifficulty difficulty = PuzzleDifficulty.fromId(fields[2]);
            int moves = Integer.parseInt(fields[3]);
            long timeMs = Long.parseLong(fields[4]);
            if (completedAt < 0 || !isSupportedSize(size) || moves < 0 || timeMs < 0) {
                return null;
            }
            return new CompletionRecord(completedAt, size, difficulty, moves, timeMs,
                    "1".equals(fields[5]));
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private static void normalizeStateMetadata(SaveManager.SaveData data) {
        GameModel metadataModel = new GameModel(data.size);
        metadataModel.loadState(data);
        data.active = metadataModel.isGameRunning();
        data.solved = metadataModel.isSolved();
    }

    /**
     * Lightweight summary used by Continue and release-readiness diagnostics.
     */
    static final class SaveMetadata {
        final long updatedAt;
        final int size;
        final int moves;
        final long elapsedMs;
        final boolean active;
        final boolean solved;
        final PuzzleDifficulty difficulty;

        SaveMetadata(long updatedAt, int size, int moves, long elapsedMs, boolean active, boolean solved,
                PuzzleDifficulty difficulty) {
            this.updatedAt = updatedAt;
            this.size = size;
            this.moves = moves;
            this.elapsedMs = elapsedMs;
            this.active = active;
            this.solved = solved;
            this.difficulty = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        }
    }

    /**
     * Immutable local progress for the daily challenge shown on a selected date.
     */
    static final class DailyProgress {
        private static final DailyProgress EMPTY = new DailyProgress(false, 0, 0, null);

        final boolean completedToday;
        final int currentStreak;
        final int bestStreak;
        final String lastCompletedDateId;

        DailyProgress(boolean completedToday, int currentStreak, int bestStreak,
                String lastCompletedDateId) {
            this.completedToday = completedToday;
            this.currentStreak = Math.max(0, currentStreak);
            this.bestStreak = Math.max(0, bestStreak);
            this.lastCompletedDateId = lastCompletedDateId;
        }
    }

    /**
     * Immutable completed-game entry retained in newest-first order.
     */
    static final class CompletionRecord {
        final long completedAt;
        final int size;
        final PuzzleDifficulty difficulty;
        final int moves;
        final long timeMs;
        final boolean assisted;

        CompletionRecord(long completedAt, int size, PuzzleDifficulty difficulty,
                int moves, long timeMs, boolean assisted) {
            this.completedAt = completedAt;
            this.size = size;
            this.difficulty = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
            this.moves = moves;
            this.timeMs = timeMs;
            this.assisted = assisted;
        }
    }

    /**
     * Lifetime completion counters for one scope or an aggregate of scopes.
     */
    static final class CompletionStats {
        private static final CompletionStats EMPTY = new CompletionStats(0, 0, 0L, 0L);

        final int playerCompletions;
        final int assistedCompletions;
        final long playerMoves;
        final long playerTimeMs;

        CompletionStats(int playerCompletions, int assistedCompletions,
                long playerMoves, long playerTimeMs) {
            this.playerCompletions = Math.max(0, playerCompletions);
            this.assistedCompletions = Math.max(0, assistedCompletions);
            this.playerMoves = Math.max(0L, playerMoves);
            this.playerTimeMs = Math.max(0L, playerTimeMs);
        }

        private CompletionStats plus(CompletionStats other) {
            return new CompletionStats(
                    playerCompletions + other.playerCompletions,
                    assistedCompletions + other.assistedCompletions,
                    playerMoves + other.playerMoves,
                    playerTimeMs + other.playerTimeMs);
        }
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
