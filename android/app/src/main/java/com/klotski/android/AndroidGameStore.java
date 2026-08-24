package com.klotski.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.DailyChallenge;
import com.klotski.core.GameModel;
import com.klotski.core.PersonalTrend;
import com.klotski.core.PuzzleIdentity;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.SaveManager;
import com.klotski.core.WeeklyGoalProgress;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SharedPreferences-backed Android state store for app-level data.
 * <p>
 * This class owns the Android persistence schema for independent per-size
 * saves and their action histories, favorite puzzles and their isolated practice progress, records,
 * completion history, personal statistics, trend scope, weekly goal, settings,
 * onboarding, and the last selected puzzle size and difficulty. A legacy
 * single-save payload is migrated into its matching size slot without
 * overwriting a newer slot. Puzzle rules and validation still belong to
 * {@link GameModel}; persisted game data is loaded back into the shared model
 * before any gameplay behavior runs.
 * </p>
 * Synchronous preference commits are deliberate in this store: backup import,
 * reset, save, and Activity recreation flows must know whether durable writes
 * succeeded before reporting success or replacing visible state.
 */
@SuppressLint("ApplySharedPref")
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
    private static final String KEY_ACTION_HISTORY = "action_history";
    private static final String KEY_REDO_HISTORY = "redo_history";
    private static final String KEY_ASSISTED = "assisted";
    private static final String KEY_SAVE_PREFIX = "save_";
    private static final String KEY_DAILY_SAVE_PREFIX = "daily_save_";
    private static final String KEY_DAILY_SAVE_BY_DATE_PREFIX = "daily_save_v2_";
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
    private static final String KEY_WEEKLY_GOAL_TARGET = "weekly_goal_target_v1";
    private static final String KEY_TREND_SIZE = "trend_size_v1";
    private static final String KEY_TREND_DIFFICULTY = "trend_difficulty_v1";
    private static final String KEY_FAVORITE_PUZZLES = "favorite_puzzles_v1";
    private static final String KEY_FAVORITE_RUN_PREFIX = "favorite_run_v1_";
    private static final String KEY_CONTINUOUS_SAVE_PREFIX = "continuous_save_v1_";
    private static final String KEY_CONTINUOUS_TARGET = "challenge_target";
    private static final String KEY_CONTINUOUS_COMPLETED = "challenge_completed";
    private static final String KEY_CONTINUOUS_TOTAL_MOVES = "challenge_total_moves";
    private static final String KEY_CONTINUOUS_TOTAL_TIME = "challenge_total_time";
    private static final String KEY_CONTINUOUS_ASSISTED = "challenge_assisted";
    private static final String KEY_PLAYER_COMPLETIONS = "player_completions";
    private static final String KEY_ASSISTED_COMPLETIONS = "assisted_completions";
    private static final String KEY_PLAYER_MOVES = "player_moves";
    private static final String KEY_PLAYER_TIME = "player_time";
    private static final int MAX_COMPLETION_HISTORY = 50;
    private static final int DEFAULT_WEEKLY_GOAL_TARGET = 5;
    private static final int MAX_FAVORITE_PUZZLES = 50;
    private static final int MAX_FAVORITE_LABEL_LENGTH = 40;
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

    /**
     * Exports every Android save, record, statistic, and setting to a versioned
     * owner-controlled JSON document.
     *
     * @return complete personal-data backup
     */
    String exportPersonalData() {
        return AndroidPersonalDataArchive.encode(prefs.getAll(), System.currentTimeMillis());
    }

    /**
     * Replaces Android personal data with a fully validated backup document.
     * Invalid documents are rejected before existing preferences are changed.
     *
     * @param archive versioned backup JSON
     * @throws IllegalArgumentException when the document is malformed or unsupported
     * @throws IllegalStateException when Android cannot persist the restored state
     */
    void importPersonalData(String archive) {
        Map<String, Object> values = AndroidPersonalDataArchive.decode(archive);
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            putPreference(editor, entry.getKey(), entry.getValue());
        }
        if (!editor.commit()) {
            throw new IllegalStateException("Android could not persist the restored SlideDo data.");
        }
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
                model.isGameRunning(), model.isSolved(), model.getDifficulty(),
                model.getEncodedActionHistory(), model.getEncodedRedoHistory());
        editor.putBoolean(prefix + KEY_ASSISTED, assisted)
                .putInt(KEY_LAST_SIZE, model.getSize()).apply();
    }

    /**
     * Adds or renames one exact starting puzzle in the local favorite library.
     * Saving the same identity never creates a duplicate.
     *
     * @param model puzzle whose immutable starting board should be retained
     * @param label owner-provided local label
     * @param createdAt creation timestamp used for stable newest-first ordering
     * @return persisted favorite, or {@code null} for invalid input
     */
    FavoritePuzzle saveFavorite(GameModel model, String label, long createdAt) {
        String normalizedLabel = normalizeFavoriteLabel(label);
        if (model == null || !isSupportedSize(model.getSize())
                || normalizedLabel == null || createdAt < 0) {
            return null;
        }

        final PuzzleIdentity identity;
        try {
            identity = PuzzleIdentity.from(model);
        } catch (RuntimeException exception) {
            return null;
        }

        FavoritePuzzle[] current = getFavoritePuzzles();
        List<FavoritePuzzle> updated = new ArrayList<>();
        FavoritePuzzle result = null;
        for (FavoritePuzzle favorite : current) {
            if (favorite.id.equals(identity.getId())) {
                result = new FavoritePuzzle(identity, normalizedLabel, favorite.createdAt);
                updated.add(result);
            } else if (updated.size() < MAX_FAVORITE_PUZZLES) {
                updated.add(favorite);
            }
        }
        if (result == null) {
            result = new FavoritePuzzle(identity, normalizedLabel, createdAt);
            updated.add(0, result);
        }
        if (updated.size() > MAX_FAVORITE_PUZZLES) {
            updated = new ArrayList<>(updated.subList(0, MAX_FAVORITE_PUZZLES));
        }
        prefs.edit().putString(KEY_FAVORITE_PUZZLES, encodeFavorites(updated)).apply();
        return result;
    }

    /** @return valid local favorites in newest-first creation order */
    FavoritePuzzle[] getFavoritePuzzles() {
        String encoded = prefs.getString(KEY_FAVORITE_PUZZLES, "");
        if (encoded == null || encoded.isEmpty()) {
            return new FavoritePuzzle[0];
        }
        List<FavoritePuzzle> favorites = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (String row : encoded.split("\\n")) {
            FavoritePuzzle favorite = parseFavorite(row);
            if (favorite != null && seenIds.add(favorite.id)) {
                favorites.add(favorite);
            }
            if (favorites.size() == MAX_FAVORITE_PUZZLES) {
                break;
            }
        }
        return favorites.toArray(new FavoritePuzzle[0]);
    }

    FavoritePuzzle getFavoritePuzzle(String favoriteId) {
        return findFavorite(favoriteId);
    }

    /**
     * Removes one favorite and its isolated practice progress.
     *
     * @param favoriteId stable exact-puzzle identity
     * @return {@code true} when a favorite was removed
     */
    boolean removeFavorite(String favoriteId) {
        if (favoriteId == null || favoriteId.isEmpty()) {
            return false;
        }
        List<FavoritePuzzle> retained = new ArrayList<>();
        boolean removed = false;
        for (FavoritePuzzle favorite : getFavoritePuzzles()) {
            if (favorite.id.equals(favoriteId)) {
                removed = true;
            } else {
                retained.add(favorite);
            }
        }
        if (!removed) {
            return false;
        }
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_FAVORITE_PUZZLES, encodeFavorites(retained));
        removeSave(editor, favoriteRunPrefix(favoriteId));
        return editor.commit();
    }

    /**
     * Saves progress for a favorite practice run without touching normal or
     * daily save slots.
     */
    void saveFavoriteRun(String favoriteId, GameModel model, long elapsedMs) {
        FavoritePuzzle favorite = findFavorite(favoriteId);
        if (favorite == null || model == null) {
            return;
        }
        PuzzleIdentity current;
        try {
            current = PuzzleIdentity.from(model);
        } catch (RuntimeException exception) {
            return;
        }
        if (!favorite.id.equals(current.getId())) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        putSave(editor, favoriteRunPrefix(favorite.id), model.getSize(), model.getGridCopy(),
                model.getInitialGridCopy(), model.getMoveCount(), Math.max(0, elapsedMs),
                System.currentTimeMillis(), model.isGameRunning(), model.isSolved(),
                model.getDifficulty(), model.getEncodedActionHistory(),
                model.getEncodedRedoHistory());
        editor.apply();
    }

    /** @return isolated favorite practice progress, or {@code null} when invalid */
    SaveManager.SaveData loadFavoriteRun(String favoriteId) {
        FavoritePuzzle favorite = findFavorite(favoriteId);
        if (favorite == null) {
            return null;
        }
        SaveManager.SaveData data = readSavedGame(
                favoriteRunPrefix(favorite.id), favorite.size);
        if (data == null || data.difficulty != favorite.difficulty
                || !Arrays.deepEquals(data.initialGrid, favorite.initialGrid)) {
            return null;
        }
        try {
            new PuzzleIdentity(data.size, data.difficulty, data.grid);
        } catch (RuntimeException exception) {
            return null;
        }
        return data;
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
        migrateLegacyDailySaveIfNeeded(challenge);
        String prefix = dailySavePrefix(challenge.getDateId());
        SharedPreferences.Editor editor = prefs.edit();
        putSave(editor, prefix, model.getSize(), model.getGridCopy(),
                model.getInitialGridCopy(), model.getMoveCount(), Math.max(0, elapsedMs),
                System.currentTimeMillis(), model.isGameRunning(), model.isSolved(),
                model.getDifficulty(), model.getEncodedActionHistory(),
                model.getEncodedRedoHistory());
        editor.putBoolean(prefix + KEY_ASSISTED, assisted).apply();
    }

    SaveManager.SaveData loadDailyGame(String dateId) {
        DailyChallenge challenge = parseDailyChallenge(dateId);
        if (challenge == null) {
            return null;
        }
        migrateLegacyDailySaveIfNeeded(challenge);
        SaveManager.SaveData data = readSavedGame(
                dailySavePrefix(challenge.getDateId()), challenge.getSize());
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
        DailyChallenge challenge = parseDailyChallenge(dateId);
        return challenge != null && loadDailyGame(dateId) != null
                && prefs.getBoolean(dailySavePrefix(challenge.getDateId()) + KEY_ASSISTED, false);
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

    void saveContinuousGame(GameModel model, long elapsedMs, boolean assisted,
            ContinuousChallenge challenge) {
        if (model == null || challenge == null || !isSupportedSize(model.getSize())) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        putSave(editor, KEY_CONTINUOUS_SAVE_PREFIX, model.getSize(), model.getGridCopy(),
                model.getInitialGridCopy(), model.getMoveCount(), Math.max(0, elapsedMs),
                System.currentTimeMillis(), model.isGameRunning(), model.isSolved(),
                model.getDifficulty(), model.getEncodedActionHistory(),
                model.getEncodedRedoHistory());
        editor.putBoolean(KEY_CONTINUOUS_SAVE_PREFIX + KEY_ASSISTED, assisted)
                .putInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TARGET,
                        challenge.getTargetPuzzles())
                .putInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_COMPLETED,
                        challenge.getCompletedPuzzles())
                .putInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_MOVES,
                        challenge.getTotalMoves())
                .putLong(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_TIME,
                        challenge.getTotalTimeMs())
                .putInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_ASSISTED,
                        challenge.getAssistedPuzzles())
                .apply();
    }

    ContinuousGame loadContinuousGame() {
        SaveManager.SaveData game = readSavedGame(KEY_CONTINUOUS_SAVE_PREFIX, 0);
        if (game == null) {
            return null;
        }
        try {
            ContinuousChallenge challenge = ContinuousChallenge.restore(
                    prefs.getInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TARGET, 0),
                    prefs.getInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_COMPLETED, -1),
                    prefs.getInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_MOVES, -1),
                    prefs.getLong(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_TIME, -1L),
                    prefs.getInt(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_ASSISTED, -1));
            return new ContinuousGame(game, challenge,
                    prefs.getBoolean(KEY_CONTINUOUS_SAVE_PREFIX + KEY_ASSISTED, false));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    void clearContinuousGame() {
        SharedPreferences.Editor editor = prefs.edit();
        removeContinuousGame(editor);
        editor.commit();
    }

    void clearSavedGame() {
        SharedPreferences.Editor editor = prefs.edit();
        removeSave(editor, "");
        for (int size = 3; size <= 5; size++) {
            removeSave(editor, savePrefix(size));
        }
        removeContinuousGame(editor);
        removeSave(editor, KEY_DAILY_SAVE_PREFIX);
        editor.remove(KEY_DAILY_SAVE_DATE);
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_DAILY_SAVE_BY_DATE_PREFIX)) {
                editor.remove(key);
            } else if (key.startsWith(KEY_FAVORITE_RUN_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.commit();
    }

    private static void removeContinuousGame(SharedPreferences.Editor editor) {
        removeSave(editor, KEY_CONTINUOUS_SAVE_PREFIX);
        editor.remove(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TARGET)
                .remove(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_COMPLETED)
                .remove(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_MOVES)
                .remove(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_TOTAL_TIME)
                .remove(KEY_CONTINUOUS_SAVE_PREFIX + KEY_CONTINUOUS_ASSISTED);
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

    PersonalTrend getPersonalTrend(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null
                ? PuzzleDifficulty.CLASSIC : difficulty;
        List<PersonalTrend.Sample> samples = new ArrayList<>();
        for (CompletionRecord record : getCompletionHistory()) {
            if (!record.assisted && record.size == size && record.difficulty == selected) {
                samples.add(new PersonalTrend.Sample(record.moves, record.timeMs));
            }
        }
        return PersonalTrend.summarize(samples);
    }

    int getWeeklyGoalTarget() {
        int target = prefs.getInt(KEY_WEEKLY_GOAL_TARGET, DEFAULT_WEEKLY_GOAL_TARGET);
        return WeeklyGoalProgress.isValidTarget(target)
                ? target : DEFAULT_WEEKLY_GOAL_TARGET;
    }

    void setWeeklyGoalTarget(int target) {
        if (WeeklyGoalProgress.isValidTarget(target)) {
            prefs.edit().putInt(KEY_WEEKLY_GOAL_TARGET, target).apply();
        }
    }

    int getTrendSize() {
        return getSupportedInt(KEY_TREND_SIZE, getLastSize(4));
    }

    void setTrendSize(int size) {
        if (isSupportedSize(size)) {
            prefs.edit().putInt(KEY_TREND_SIZE, size).apply();
        }
    }

    PuzzleDifficulty getTrendDifficulty() {
        return PuzzleDifficulty.fromId(prefs.getString(
                KEY_TREND_DIFFICULTY, getLastDifficulty().getId()));
    }

    void setTrendDifficulty(PuzzleDifficulty difficulty) {
        if (difficulty != null) {
            prefs.edit().putString(KEY_TREND_DIFFICULTY, difficulty.getId()).apply();
        }
    }

    WeeklyGoalProgress getWeeklyGoalProgress(LocalDate today, ZoneId zoneId) {
        List<LocalDate> playerCompletionDates = new ArrayList<>();
        for (CompletionRecord record : getCompletionHistory()) {
            if (!record.assisted) {
                playerCompletionDates.add(Instant.ofEpochMilli(record.completedAt)
                        .atZone(zoneId).toLocalDate());
            }
        }
        return WeeklyGoalProgress.calculate(
                today, getWeeklyGoalTarget(), playerCompletionDates);
    }

    private int getSupportedInt(String key, int fallback) {
        int value = prefs.getInt(key, fallback);
        return isSupportedSize(value) ? value : fallback;
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
        data.actionHistory = prefs.getString(prefix + KEY_ACTION_HISTORY, "");
        data.redoHistory = prefs.getString(prefix + KEY_REDO_HISTORY, "");
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
                    legacy.active, legacy.solved, legacy.difficulty,
                    legacy.actionHistory, legacy.redoHistory);
        }
        removeSave(editor, "");
        editor.commit();
    }

    private void migrateLegacyDailySaveIfNeeded(DailyChallenge challenge) {
        if (challenge == null
                || !challenge.getDateId().equals(prefs.getString(KEY_DAILY_SAVE_DATE, null))) {
            return;
        }
        SaveManager.SaveData legacy = readSavedGame(KEY_DAILY_SAVE_PREFIX, challenge.getSize());
        if (legacy == null || legacy.difficulty != challenge.getDifficulty()
                || !Arrays.deepEquals(legacy.initialGrid,
                        challenge.createGame().getInitialGridCopy())) {
            return;
        }

        String targetPrefix = dailySavePrefix(challenge.getDateId());
        SaveManager.SaveData current = readSavedGame(targetPrefix, challenge.getSize());
        boolean legacyAssisted = prefs.getBoolean(KEY_DAILY_SAVE_PREFIX + KEY_ASSISTED, false);
        SharedPreferences.Editor editor = prefs.edit();
        if (current == null || legacy.updatedAt >= current.updatedAt) {
            putSave(editor, targetPrefix, legacy.size, legacy.grid, legacy.initialGrid,
                    legacy.moveCount, legacy.elapsedTime, legacy.updatedAt,
                    legacy.active, legacy.solved, legacy.difficulty,
                    legacy.actionHistory, legacy.redoHistory);
            editor.putBoolean(targetPrefix + KEY_ASSISTED, legacyAssisted);
        }
        removeSave(editor, KEY_DAILY_SAVE_PREFIX);
        editor.remove(KEY_DAILY_SAVE_DATE);
        editor.commit();
    }

    private static void putSave(SharedPreferences.Editor editor, String prefix, int size,
            int[][] grid, int[][] initialGrid, int moves, long elapsedMs, long updatedAt,
            boolean active, boolean solved, PuzzleDifficulty difficulty,
            String actionHistory, String redoHistory) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        editor.putInt(prefix + KEY_SIZE, size)
                .putString(prefix + KEY_GRID, flatten(grid))
                .putString(prefix + KEY_INITIAL_GRID, flatten(initialGrid))
                .putInt(prefix + KEY_MOVES, moves)
                .putLong(prefix + KEY_ELAPSED, Math.max(0, elapsedMs))
                .putLong(prefix + KEY_UPDATED_AT, updatedAt)
                .putBoolean(prefix + KEY_ACTIVE, active)
                .putBoolean(prefix + KEY_SOLVED, solved)
                .putString(prefix + KEY_DIFFICULTY, selected.getId())
                .putString(prefix + KEY_ACTION_HISTORY,
                        actionHistory == null ? "" : actionHistory)
                .putString(prefix + KEY_REDO_HISTORY,
                        redoHistory == null ? "" : redoHistory);
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
                .remove(prefix + KEY_ACTION_HISTORY)
                .remove(prefix + KEY_REDO_HISTORY)
                .remove(prefix + KEY_ASSISTED);
    }

    @SuppressWarnings("unchecked")
    private static void putPreference(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String stringValue) {
            editor.putString(key, stringValue);
        } else if (value instanceof Integer integerValue) {
            editor.putInt(key, integerValue);
        } else if (value instanceof Long longValue) {
            editor.putLong(key, longValue);
        } else if (value instanceof Float floatValue) {
            editor.putFloat(key, floatValue);
        } else if (value instanceof Boolean booleanValue) {
            editor.putBoolean(key, booleanValue);
        } else if (value instanceof Set<?>) {
            editor.putStringSet(key, new HashSet<>((Set<String>) value));
        } else {
            throw new IllegalArgumentException("Unsupported restored preference type.");
        }
    }

    private static String difficultyBestPrefix(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        return KEY_BEST_PREFIX + size + "_" + selected.getId();
    }

    private static String dailySavePrefix(String dateId) {
        return KEY_DAILY_SAVE_BY_DATE_PREFIX + dateId + "_";
    }

    private static String favoriteRunPrefix(String favoriteId) {
        return KEY_FAVORITE_RUN_PREFIX + favoriteId + "_";
    }

    private static String completionStatsPrefix(int size, PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
        return KEY_STATS_PREFIX + size + "_" + selected.getId() + "_";
    }

    private static String encodeCompletion(CompletionRecord record) {
        return record.completedAt + "," + record.size + "," + record.difficulty.getId()
                + "," + record.moves + "," + record.timeMs + "," + (record.assisted ? 1 : 0);
    }

    private FavoritePuzzle findFavorite(String favoriteId) {
        if (favoriteId != null) {
            for (FavoritePuzzle favorite : getFavoritePuzzles()) {
                if (favorite.id.equals(favoriteId)) {
                    return favorite;
                }
            }
        }
        return null;
    }

    private static String encodeFavorites(List<FavoritePuzzle> favorites) {
        StringBuilder encoded = new StringBuilder();
        for (FavoritePuzzle favorite : favorites) {
            if (encoded.length() > 0) {
                encoded.append('\n');
            }
            String label = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    favorite.label.getBytes(StandardCharsets.UTF_8));
            encoded.append(favorite.id).append('|')
                    .append(favorite.createdAt).append('|')
                    .append(favorite.size).append('|')
                    .append(favorite.difficulty.getId()).append('|')
                    .append(label).append('|')
                    .append(flatten(favorite.initialGrid));
        }
        return encoded.toString();
    }

    private static FavoritePuzzle parseFavorite(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != 6 || fields[0].length() != 64) {
            return null;
        }
        try {
            long createdAt = Long.parseLong(fields[1]);
            int size = Integer.parseInt(fields[2]);
            PuzzleDifficulty difficulty = strictDifficulty(fields[3]);
            String label = new String(Base64.getUrlDecoder().decode(fields[4]),
                    StandardCharsets.UTF_8);
            int[][] grid = parseGrid(fields[5], size);
            if (createdAt < 0 || !isSupportedSize(size) || difficulty == null
                    || grid == null || normalizeFavoriteLabel(label) == null) {
                return null;
            }
            PuzzleIdentity identity = new PuzzleIdentity(size, difficulty, grid);
            if (!identity.getId().equals(fields[0])) {
                return null;
            }
            return new FavoritePuzzle(identity, normalizeFavoriteLabel(label), createdAt);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String normalizeFavoriteLabel(String label) {
        if (label == null) {
            return null;
        }
        String normalized = label.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_FAVORITE_LABEL_LENGTH) {
            normalized = normalized.substring(0, MAX_FAVORITE_LABEL_LENGTH).trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static PuzzleDifficulty strictDifficulty(String id) {
        if (id != null) {
            for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
                if (difficulty.getId().equals(id)) {
                    return difficulty;
                }
            }
        }
        return null;
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
     * Restored continuous-challenge aggregate and its isolated current puzzle.
     */
    static final class ContinuousGame {
        final SaveManager.SaveData game;
        final ContinuousChallenge challenge;
        final boolean assisted;

        ContinuousGame(SaveManager.SaveData game, ContinuousChallenge challenge,
                boolean assisted) {
            this.game = game;
            this.challenge = challenge;
            this.assisted = assisted;
        }
    }

    /**
     * Immutable owner-labeled entry in the local favorite puzzle library.
     */
    static final class FavoritePuzzle {
        final String id;
        final String label;
        final long createdAt;
        final int size;
        final PuzzleDifficulty difficulty;
        private final int[][] initialGrid;

        FavoritePuzzle(PuzzleIdentity identity, String label, long createdAt) {
            this.id = identity.getId();
            this.label = label;
            this.createdAt = createdAt;
            this.size = identity.getSize();
            this.difficulty = identity.getDifficulty();
            this.initialGrid = identity.getInitialGridCopy();
        }

        int[][] getInitialGridCopy() {
            return copyGrid(initialGrid);
        }

        GameModel createGame() {
            return new PuzzleIdentity(size, difficulty, initialGrid).createGame();
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
