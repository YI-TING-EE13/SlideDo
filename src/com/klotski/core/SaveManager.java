package com.klotski.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists desktop game state and best records.
 * <p>
 * New saves use one small JSON slot per supported board size, including
 * completed and redo action histories, so the data remains portable across
 * Java desktop and Android. Scoped best records and bounded completion
 * history/statistics use additive files and durable completion ids. Replacements
 * are atomic and leave recoverable siblings when the platform cannot complete a
 * write. The loader also accepts the legacy single JSON and serialized
 * {@code klotski_save.dat} files without deleting them or overwriting a newer
 * size slot.
 * </p>
 */
public class SaveManager {
    /** Optional JVM property used by tests and portable desktop packages. */
    public static final String DATA_DIR_PROPERTY = "slidedo.data.dir";

    private static final int CURRENT_SAVE_VERSION = 4;
    private static final int MIN_SUPPORTED_SIZE = 3;
    private static final int MAX_SUPPORTED_SIZE = 5;
    private static final String SAVE_FILE_PREFIX = "klotski_save_";
    private static final String SAVE_FILE_SUFFIX = ".json";
    private static final String SAVE_FILE = "klotski_save.json";
    private static final String LEGACY_SAVE_FILE = "klotski_save.dat";
    private static final String RECORDS_FILE = "klotski_records.json";
    private static final String SCOPED_RECORDS_FILE = "klotski_records_v2.json";
    private static final String RECORDS_RESET_FILE = "klotski_records_reset.marker";
    private static final String SAVED_GAMES_RESET_FILE = "klotski_saved_games_reset.marker";
    private static final String STATISTICS_FILE = "klotski_statistics.json";
    private static final String DAILY_SAVE_PREFIX = "klotski_daily_";
    private static final String DAILY_SAVE_SUFFIX = ".json";
    private static final String DAILY_ASSISTED_SUFFIX = ".assisted";
    private static final String DAILY_PROGRESS_FILE = "klotski_daily_progress.json";
    private static final String FAVORITES_FILE = "klotski_favorites.json";
    private static final String FAVORITE_RUN_PREFIX = "klotski_favorite_";
    private static final String FAVORITE_RUN_SUFFIX = ".json";
    private static final String FAVORITE_ASSISTED_SUFFIX = ".assisted";
    private static final String PERSONAL_PREFERENCES_FILE = "klotski_personal_preferences.json";
    private static final String CONTINUOUS_META_FILE = "klotski_continuous_meta.json";
    private static final String CONTINUOUS_CURRENT_FILE = "klotski_continuous_current.json";
    private static final String CONTINUOUS_ASSISTED_SUFFIX = ".assisted";
    private static final int MAX_COMPLETION_HISTORY = 50;
    private static final int MAX_FAVORITE_PUZZLES = 50;
    private static final int MAX_FAVORITE_LABEL_LENGTH = 40;
    private static final int DEFAULT_WEEKLY_GOAL_TARGET = 5;
    private static final String ATOMIC_TEMP_SUFFIX = ".tmp";
    private static final String ATOMIC_BACKUP_SUFFIX = ".bak";

    private SaveManager() {
    }

    /**
     * Writes the current game state to the independent slot for its board size.
     *
     * @param model game model to persist
     * @return {@code true} when the save file was written successfully
     */
    public static boolean saveGame(GameModel model) {
        return saveGame(model, false);
    }

    /**
     * Writes a normal save while retaining whether the current run has used
     * solver or strategic assistance.
     *
     * <p>The assistance bit is additive. Older unsolved normal saves without
     * provenance remain eligible; solved pre-v4 normal saves fail closed as
     * assisted because the historical Results autosave could not prove who
     * solved them. Isolated Daily, Favorite Practice, and Continuous stores
     * retain their own sidecar/meta markers.</p>
     *
     * @param model game model to persist
     * @param assisted whether this run is no longer eligible for a player best
     * <p>A durable {@code klotski_saved_games_reset.marker}, created by an
     * explicit reset or a full archive restore without imported legacy source,
     * is intentionally retained after later canonical saves. This prevents a
     * project-root legacy fallback from reappearing while normal slots remain
     * independently usable.</p>
     *
     * @return {@code true} when the save file was written successfully
     */
    public static boolean saveGame(GameModel model, boolean assisted) {
        if (model == null || !isSupportedSize(model.getSize())) {
            return false;
        }
        boolean saved = saveGame(model, getSaveFile(model.getSize()), assisted);
        return saved;
    }

    /**
     * Persists an active game at a lifecycle boundary without changing the
     * manual Save command's semantics.
     *
     * @param model game model to persist
     * @return {@code true} when the autosave slot was written successfully
     */
    public static boolean autosaveGame(GameModel model) {
        return saveGame(model, false);
    }

    /**
     * Persists an active normal run at a lifecycle boundary.
     *
     * @param model game model to persist
     * @param assisted whether this run has used solver or strategic assistance
     * @return {@code true} when the save file was written successfully
     */
    public static boolean autosaveGame(GameModel model, boolean assisted) {
        return saveGame(model, assisted);
    }

    /**
     * Writes the current game state to a caller-supplied save file.
     *
     * @param model game model to persist
     * @param saveFile target JSON save file
     * @return {@code true} when the save file was written successfully
     */
    static boolean saveGame(GameModel model, File saveFile) {
        return saveGame(model, saveFile, false);
    }

    static boolean saveGame(GameModel model, File saveFile, boolean assisted) {
        if (model == null || saveFile == null) {
            return false;
        }
        SaveData data = new SaveData();
        data.grid = model.getGridCopy();
        data.initialGrid = model.getInitialGridCopy();
        data.size = model.getSize();
        data.moveCount = model.getMoveCount();
        data.startTime = model.getStartTime();
        data.elapsedTime = model.getElapsedTime();
        data.updatedAt = System.currentTimeMillis();
        data.active = model.isGameRunning();
        data.solved = model.isSolved();
        data.assisted = assisted;
        data.assistedMetadataPresent = true;
        data.difficulty = model.getDifficulty();
        data.actionHistory = model.getEncodedActionHistory();
        data.redoHistory = model.getEncodedRedoHistory();

        try {
            writeTextAtomic(saveFile, toJson(data));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Loads the newest valid independent desktop save.
     *
     * @return parsed save data, or {@code null} when no valid save exists
     */
    public static SaveData loadGame() {
        if (!isSavedGamesReset()) {
            migrateLegacySaves();
        }
        SaveData newest = null;
        for (int size = MIN_SUPPORTED_SIZE; size <= MAX_SUPPORTED_SIZE; size++) {
            SaveData candidate = loadSlot(size);
            if (candidate != null && (newest == null || candidate.updatedAt > newest.updatedAt)) {
                newest = candidate;
            }
        }
        if (newest != null) {
            return newest;
        }
        return isSavedGamesReset() ? null : loadLegacyFallback();
    }

    /**
     * Loads the independent save slot for one supported board size.
     *
     * @param size supported square board size
     * @return parsed save data, or {@code null} when the slot is absent/invalid
     */
    public static SaveData loadGame(int size) {
        if (!isSupportedSize(size)) {
            return null;
        }
        if (!isSavedGamesReset()) {
            migrateLegacySaves();
        }
        return loadSlot(size);
    }

    /**
     * Returns metadata for every valid normal save, ordered by board size.
     *
     * @return independent save summaries for the Home Continue chooser
     */
    public static SaveMetadata[] getAllSaveMetadata() {
        if (!isSavedGamesReset()) {
            migrateLegacySaves();
        }
        List<SaveMetadata> metadata = new ArrayList<>();
        for (int size = MIN_SUPPORTED_SIZE; size <= MAX_SUPPORTED_SIZE; size++) {
            SaveData data = loadSlot(size);
            if (data != null) {
                metadata.add(new SaveMetadata(data));
            }
        }
        return metadata.toArray(new SaveMetadata[0]);
    }

    /**
     * Indicates whether at least one valid normal save exists.
     *
     * @return {@code true} when a Continue choice is available
     */
    public static boolean hasSavedGame() {
        return getAllSaveMetadata().length > 0;
    }

    /**
     * Reports whether a date can be opened in the offline daily calendar.
     *
     * @param dateId ISO-8601 date identity
     * @param today caller-selected local date boundary
     * @return {@code true} for a valid date that is not in the future
     */
    public static boolean isDailyDatePlayable(String dateId, LocalDate today) {
        LocalDate date = parseDailyDate(dateId);
        return date != null && today != null && !date.isAfter(today);
    }

    /**
     * Writes an isolated daily save for one dated 4x4 Classic challenge.
     *
     * @param dateId ISO-8601 date identity
     * @param model daily game model to persist
     * @param assisted whether solver or strategic assistance is active
     * @return {@code true} when the daily save and assistance marker are written
     */
    public static synchronized boolean saveDailyGame(String dateId, GameModel model, boolean assisted) {
        LocalDate date = parseDailyDate(dateId);
        if (date == null || date.isAfter(LocalDate.now()) || model == null) {
            return false;
        }
        DailyChallenge challenge = DailyChallenge.forDate(date);
        if (!matchesDailyIdentity(model.getSize(), model.getDifficulty(), model.getInitialGridCopy(), challenge)) {
            return false;
        }
        File saveFile = getDailySaveFile(date);
        if (!saveGame(model, saveFile, assisted)) {
            return false;
        }
        try {
            writeTextAtomic(getDailyAssistedFile(date), Boolean.toString(assisted));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Loads one isolated daily save after validating its exact dated identity.
     * Historical dates remain loadable; future dates are rejected.
     *
     * @param dateId ISO-8601 date identity
     * @return validated daily save data, or {@code null}
     */
    public static synchronized SaveData loadDailyGame(String dateId) {
        LocalDate date = parseDailyDate(dateId);
        if (date == null || date.isAfter(LocalDate.now())) {
            return null;
        }
        DailyChallenge challenge = DailyChallenge.forDate(date);
        SaveData data = readJsonWithRecovery(getDailySaveFile(date));
        if (data == null || !matchesDailyIdentity(data.size, data.difficulty, data.initialGrid, challenge)) {
            return null;
        }
        return data;
    }

    /**
     * Reads metadata for one dated daily save.
     *
     * @param dateId ISO-8601 date identity
     * @return metadata, or {@code null} when no valid daily save exists
     */
    public static SaveMetadata getDailySaveMetadata(String dateId) {
        SaveData data = loadDailyGame(dateId);
        return data == null ? null : new SaveMetadata(data);
    }

    /**
     * Reports whether the saved daily run used assistance.
     *
     * @param dateId ISO-8601 date identity
     * @return {@code true} only when the validated save has an assistance marker
     */
    public static boolean isDailyGameAssisted(String dateId) {
        LocalDate date = parseDailyDate(dateId);
        SaveData data = date == null ? null : loadDailyGame(dateId);
        if (data == null) {
            return false;
        }
        if (data.assisted) {
            return true;
        }
        File marker = getDailyAssistedFile(date);
        if (!marker.exists()) {
            return false;
        }
        try {
            return Boolean.parseBoolean(readText(marker).trim());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Records a daily completion using the host's local date as the boundary.
     *
     * @param dateId completed daily date
     * @return {@code true} when this date was newly recorded
     */
    public static boolean recordDailyCompletion(String dateId) {
        return recordDailyCompletion(dateId, LocalDate.now());
    }

    /**
     * Testable daily completion overload with an explicit local-date boundary.
     * Historical completion never moves the latest-date streak backward.
     *
     * @param dateId completed daily date
     * @param today caller-selected local date boundary
     * @return {@code true} when this date was newly recorded
     */
    public static synchronized boolean recordDailyCompletion(String dateId, LocalDate today) {
        LocalDate date = parseDailyDate(dateId);
        if (date == null || today == null || date.isAfter(today)) {
            return false;
        }
        DailyProgressState state = loadDailyProgressState();
        String canonicalDateId = date.toString();
        if (!state.completedDates.add(canonicalDateId)) {
            return false;
        }

        if (state.lastCompletedDate == null || date.isAfter(state.lastCompletedDate)) {
            state.currentStreak = state.lastCompletedDate != null
                    && date.equals(state.lastCompletedDate.plusDays(1))
                    ? state.currentStreak + 1 : 1;
            state.bestStreak = Math.max(state.bestStreak, state.currentStreak);
            state.lastCompletedDate = date;
        }
        return saveDailyProgressState(state);
    }

    /**
     * Returns completion and streak state for a selected calendar date.
     *
     * @param dateId selected ISO-8601 date
     * @return immutable progress, or zero state for an invalid date
     */
    public static synchronized DailyProgress getDailyProgress(String dateId) {
        LocalDate date = parseDailyDate(dateId);
        if (date == null) {
            return DailyProgress.empty();
        }
        DailyProgressState state = loadDailyProgressState();
        boolean current = state.lastCompletedDate != null
                && (state.lastCompletedDate.equals(date)
                        || state.lastCompletedDate.equals(date.minusDays(1)));
        return new DailyProgress(state.completedDates.contains(date.toString()),
                current ? state.currentStreak : 0, state.bestStreak,
                state.lastCompletedDate == null ? null : state.lastCompletedDate.toString());
    }

    /**
     * Returns the dates recorded as completed, in ascending order.
     *
     * @return defensive set of ISO date identities
     */
    public static synchronized Set<String> getDailyCompletedDates() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(loadDailyProgressState().completedDates));
    }

    /**
     * Saves one exact starting puzzle in the local favorite library.
     * <p>
     * Favorite identity is supplied by {@link PuzzleIdentity}; saving the same
     * identity updates its label instead of creating a duplicate. The favorite
     * list has its own bounded file and never changes a normal or Daily slot.
     * </p>
     *
     * @param model puzzle whose immutable starting board should be retained
     * @param label owner-provided local label
     * @return persisted favorite, or {@code null} when input is invalid or the
     *         library file cannot be written
     */
    public static FavoritePuzzle saveFavorite(GameModel model, String label) {
        return saveFavorite(model, label, System.currentTimeMillis());
    }

    /**
     * Testable favorite-save overload with an explicit creation timestamp.
     *
     * @param model puzzle whose immutable starting board should be retained
     * @param label owner-provided local label
     * @param createdAt timestamp used for newest-first ordering
     * @return persisted favorite, or {@code null} for invalid input
     */
    public static synchronized FavoritePuzzle saveFavorite(GameModel model, String label,
            long createdAt) {
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

        List<FavoritePuzzle> current = new ArrayList<>(getFavoritePuzzlesList());
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
        List<String> evictedIds = new ArrayList<>();
        while (updated.size() > MAX_FAVORITE_PUZZLES) {
            FavoritePuzzle removed = updated.remove(updated.size() - 1);
            evictedIds.add(removed.id);
        }
        if (!saveFavorites(updated)) {
            return null;
        }
        for (String evictedId : evictedIds) {
            deleteFavoriteRun(evictedId);
        }
        return result;
    }

    /**
     * Returns valid favorite entries in newest-first library order.
     *
     * @return defensive array of exact puzzle identities and labels
     */
    public static synchronized FavoritePuzzle[] getFavoritePuzzles() {
        return getFavoritePuzzlesList().toArray(new FavoritePuzzle[0]);
    }

    /**
     * Finds one favorite by its stable exact-puzzle identity.
     *
     * @param favoriteId lowercase SHA-256 identity
     * @return favorite entry, or {@code null}
     */
    public static synchronized FavoritePuzzle getFavoritePuzzle(String favoriteId) {
        if (favoriteId == null) {
            return null;
        }
        for (FavoritePuzzle favorite : getFavoritePuzzlesList()) {
            if (favorite.id.equals(favoriteId)) {
                return favorite;
            }
        }
        return null;
    }

    /**
     * Renames a favorite without changing its identity or practice save.
     *
     * @param favoriteId favorite identity
     * @param label new owner-provided label
     * @return {@code true} when the renamed library was persisted
     */
    public static synchronized boolean renameFavorite(String favoriteId, String label) {
        FavoritePuzzle favorite = getFavoritePuzzle(favoriteId);
        if (favorite == null) {
            return false;
        }
        return saveFavorite(favorite.createGame(), label, favorite.createdAt) != null;
    }

    /**
     * Removes a favorite and its isolated practice progress.
     *
     * @param favoriteId favorite identity
     * @return {@code true} when an entry was removed and the library persisted
     */
    public static synchronized boolean removeFavorite(String favoriteId) {
        if (getFavoritePuzzle(favoriteId) == null) {
            return false;
        }
        List<FavoritePuzzle> retained = new ArrayList<>();
        for (FavoritePuzzle favorite : getFavoritePuzzlesList()) {
            if (!favorite.id.equals(favoriteId)) {
                retained.add(favorite);
            }
        }
        if (!saveFavorites(retained)) {
            return false;
        }
        deleteFavoriteRun(favoriteId);
        return true;
    }

    /**
     * Persists a favorite-practice board in its own namespace.
     *
     * @param favoriteId exact favorite identity
     * @param model current practice board
     * @param assisted whether assistance is active for this practice run
     * @return {@code true} when both the board and marker were written
     */
    public static synchronized boolean saveFavoriteRun(String favoriteId, GameModel model,
            boolean assisted) {
        FavoritePuzzle favorite = getFavoritePuzzle(favoriteId);
        if (favorite == null || model == null) {
            return false;
        }
        try {
            if (!favorite.id.equals(PuzzleIdentity.from(model).getId())) {
                return false;
            }
        } catch (RuntimeException exception) {
            return false;
        }
        File saveFile = getFavoriteRunFile(favorite.id);
        if (!saveGame(model, saveFile, assisted)) {
            return false;
        }
        try {
            writeTextAtomic(getFavoriteAssistedFile(favorite.id), Boolean.toString(assisted));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Loads validated favorite-practice progress without touching other modes.
     *
     * @param favoriteId exact favorite identity
     * @return isolated practice save, or {@code null}
     */
    public static synchronized SaveData loadFavoriteRun(String favoriteId) {
        FavoritePuzzle favorite = getFavoritePuzzle(favoriteId);
        if (favorite == null) {
            return null;
        }
        SaveData data = readJsonWithRecovery(getFavoriteRunFile(favorite.id));
        if (data == null || data.difficulty != favorite.difficulty
                || !Arrays.deepEquals(data.initialGrid, favorite.initialGrid)) {
            return null;
        }
        return data;
    }

    /**
     * Reports whether a validated favorite-practice save was assisted.
     *
     * @param favoriteId exact favorite identity
     * @return assistance marker value, or {@code false} when absent/invalid
     */
    public static synchronized boolean isFavoriteRunAssisted(String favoriteId) {
        SaveData data = loadFavoriteRun(favoriteId);
        if (data == null) {
            return false;
        }
        if (data.assisted) {
            return true;
        }
        File marker = getFavoriteAssistedFile(favoriteId);
        if (!marker.exists()) {
            return false;
        }
        try {
            return Boolean.parseBoolean(readText(marker).trim());
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Persists one isolated Continuous Challenge current board and aggregate.
     *
     * @param model current fixed-scope puzzle
     * @param challenge session aggregate
     * @param assisted whether the current puzzle used assistance
     * @return {@code true} when current board and aggregate were written
     */
    public static synchronized boolean saveContinuousGame(GameModel model,
            ContinuousChallenge challenge, boolean assisted) {
        if (model == null || challenge == null || !isSupportedSize(model.getSize())
                || model.getDifficulty() == null) {
            return false;
        }
        if (!saveGame(model, getContinuousCurrentFile(), assisted)) {
            return false;
        }
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"version\": 1,\n")
                .append("  \"size\": ").append(model.getSize()).append(",\n")
                .append("  \"difficulty\": ").append(jsonString(model.getDifficulty().getId())).append(",\n")
                .append("  \"targetPuzzles\": ").append(challenge.getTargetPuzzles()).append(",\n")
                .append("  \"completedPuzzles\": ").append(challenge.getCompletedPuzzles()).append(",\n")
                .append("  \"totalMoves\": ").append(challenge.getTotalMoves()).append(",\n")
                .append("  \"totalTimeMs\": ").append(challenge.getTotalTimeMs()).append(",\n")
                .append("  \"assistedPuzzles\": ").append(challenge.getAssistedPuzzles()).append(",\n")
                .append("  \"assistedCurrent\": ").append(assisted).append(",\n")
                .append("  \"updatedAt\": ").append(System.currentTimeMillis()).append("\n}\n");
        try {
            writeTextAtomic(getContinuousMetaFile(), json.toString());
            writeTextAtomic(getContinuousAssistedFile(), Boolean.toString(assisted));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Loads the isolated Continuous Challenge state after validating its
     * aggregate and current-board scope.
     *
     * @return restored session, or {@code null} when either file is invalid
     */
    public static synchronized ContinuousGame loadContinuousGame() {
        File metaFile = getContinuousMetaFile();
        if (!metaFile.exists()) {
            return null;
        }
        try {
            String json = readText(metaFile);
            int size = intField(json, "size");
            PuzzleDifficulty difficulty = PuzzleDifficulty.fromId(
                    optionalStringField(json, "difficulty", PuzzleDifficulty.CLASSIC.getId()));
            ContinuousChallenge challenge = ContinuousChallenge.restore(
                    intField(json, "targetPuzzles"), intField(json, "completedPuzzles"),
                    intField(json, "totalMoves"), longField(json, "totalTimeMs"),
                    intField(json, "assistedPuzzles"));
            SaveData game = readJsonWithRecovery(getContinuousCurrentFile());
            if (game == null || game.size != size || game.difficulty != difficulty) {
                return null;
            }
            boolean assisted = optionalBooleanField(json, "assistedCurrent", false)
                    || game.assisted;
            return new ContinuousGame(game, challenge, size, difficulty, assisted);
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return null;
        }
    }

    /**
     * Ends a Continuous Challenge without touching normal, Daily, Favorite,
     * records, or completion-history files.
     *
     * @return {@code true} when the continuous files were removed
     */
    public static synchronized boolean clearContinuousGame() {
        boolean success = deleteAtomicFile(getContinuousMetaFile());
        success &= deleteAtomicFile(getContinuousCurrentFile());
        success &= deleteFile(getContinuousAssistedFile());
        return success;
    }

    /**
     * Returns the persisted weekly goal target, defaulting to five completions.
     *
     * @return weekly player-completion target
     */
    public static synchronized int getWeeklyGoalTarget() {
        PersonalPreferences preferences = loadPersonalPreferences();
        return WeeklyGoalProgress.isValidTarget(preferences.weeklyGoalTarget)
                ? preferences.weeklyGoalTarget : DEFAULT_WEEKLY_GOAL_TARGET;
    }

    /**
     * Stores a valid weekly player-completion target.
     *
     * @param target target from one through fifty
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setWeeklyGoalTarget(int target) {
        if (!WeeklyGoalProgress.isValidTarget(target)) {
            return false;
        }
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.weeklyGoalTarget = target;
        return savePersonalPreferences(preferences);
    }

    /**
     * Reads the persisted Desktop reduced-motion presentation preference.
     *
     * @return whether reduced motion is enabled
     */
    public static synchronized boolean isReducedMotionEnabled() {
        return loadPersonalPreferences().reducedMotion;
    }

    /**
     * Persists the Desktop reduced-motion presentation preference.
     *
     * @param enabled whether animations should snap
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setReducedMotionEnabled(boolean enabled) {
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.reducedMotion = enabled;
        return savePersonalPreferences(preferences);
    }

    /**
     * Reads the persisted optional Desktop sound-feedback preference.
     *
     * @return whether sound feedback is enabled
     */
    public static synchronized boolean isSoundEnabled() {
        return loadPersonalPreferences().soundEnabled;
    }

    /**
     * Persists the optional Desktop sound-feedback preference.
     *
     * @param enabled whether local move/win beeps are enabled
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setSoundEnabled(boolean enabled) {
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.soundEnabled = enabled;
        return savePersonalPreferences(preferences);
    }

    /**
     * Reads the stable Desktop theme id, currently {@code midnight} or {@code ocean}.
     *
     * @return persisted theme id
     */
    public static synchronized String getDesktopTheme() {
        PersonalPreferences preferences = loadPersonalPreferences();
        String normalized = normalizeDesktopTheme(preferences.theme);
        return normalized == null ? "midnight" : normalized;
    }

    /**
     * Persists a supported Desktop theme id.
     *
     * @param theme stable theme id
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setDesktopTheme(String theme) {
        String normalized = normalizeDesktopTheme(theme);
        if (normalized == null) {
            return false;
        }
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.theme = normalized;
        return savePersonalPreferences(preferences);
    }

    /**
     * Reads the normalized Desktop language tag, defaulting to English.
     *
     * @return persisted language tag
     */
    public static synchronized String getDesktopLanguageTag() {
        String normalized = normalizeDesktopLanguage(loadPersonalPreferences().languageTag);
        return normalized == null ? "en" : normalized;
    }

    /**
     * Persists one of the supported Desktop language tags.
     *
     * @param languageTag {@code en}, {@code zh-TW}, or {@code ja-JP}
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setDesktopLanguageTag(String languageTag) {
        String normalized = normalizeDesktopLanguage(languageTag);
        if (normalized == null) {
            return false;
        }
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.languageTag = normalized;
        return savePersonalPreferences(preferences);
    }

    /**
     * Reads whether first-run Desktop onboarding has been completed.
     *
     * @return whether onboarding was seen
     */
    public static synchronized boolean isOnboardingSeen() {
        return loadPersonalPreferences().onboardingSeen;
    }

    /**
     * Marks the first-run Desktop onboarding as completed.
     *
     * @return whether the preference was written
     */
    public static synchronized boolean markOnboardingSeen() {
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.onboardingSeen = true;
        return savePersonalPreferences(preferences);
    }

    /**
     * Clears normal, dated Daily, Favorite Practice, and Continuous save data.
     * Favorite labels, records, statistics, Daily completion history, and
     * preferences remain intact.
     *
     * @return {@code true} when all targeted files were removed
     */
    public static synchronized boolean clearSavedGames() {
        boolean success = true;
        File dataDirectory = getDataDirectory();
        for (int size = MIN_SUPPORTED_SIZE; size <= MAX_SUPPORTED_SIZE; size++) {
            success &= deleteAtomicFile(new File(dataDirectory, saveFileName(size)));
        }
        success &= deleteAtomicFile(new File(dataDirectory, SAVE_FILE));
        success &= deleteAtomicFile(new File(dataDirectory, LEGACY_SAVE_FILE));
        File[] files = dataDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if ((name.startsWith(DAILY_SAVE_PREFIX)
                        && !name.startsWith(DAILY_PROGRESS_FILE))
                        || name.startsWith(FAVORITE_RUN_PREFIX)) {
                    success &= deleteFile(file);
                }
            }
        }
        success &= clearContinuousGame();
        try {
            writeTextAtomic(new File(dataDirectory, SAVED_GAMES_RESET_FILE), "reset\n");
            return success;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Clears best records, completion history/statistics, and Daily streak
     * state while preserving active Continuous Challenge files and settings.
     * Legacy size-only record source files remain untouched but are masked by a
     * reset marker so an explicit reset cannot resurrect their values.
     *
     * @return {@code true} when reset state was persisted
     */
    public static synchronized boolean clearRecords() {
        boolean success = deleteFile(new File(getDataDirectory(), SCOPED_RECORDS_FILE));
        success &= deleteFile(new File(getDataDirectory(), STATISTICS_FILE));
        success &= deleteFile(new File(getDataDirectory(), DAILY_PROGRESS_FILE));
        try {
            writeTextAtomic(new File(getDataDirectory(), RECORDS_RESET_FILE), "reset\n");
            return success;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * Returns the selected trends/weekly-goal board size, defaulting to 4.
     *
     * @return selected supported board size
     */
    public static synchronized int getTrendSize() {
        PersonalPreferences preferences = loadPersonalPreferences();
        return isSupportedSize(preferences.trendSize) ? preferences.trendSize : 4;
    }

    /**
     * Stores the selected trends/weekly-goal board size.
     *
     * @param size supported square board size
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setTrendSize(int size) {
        if (!isSupportedSize(size)) {
            return false;
        }
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.trendSize = size;
        return savePersonalPreferences(preferences);
    }

    /**
     * Returns the selected trends/weekly-goal difficulty, defaulting to Classic.
     *
     * @return selected difficulty
     */
    public static synchronized PuzzleDifficulty getTrendDifficulty() {
        PersonalPreferences preferences = loadPersonalPreferences();
        return normalizeDifficulty(preferences.trendDifficulty);
    }

    /**
     * Stores the selected trends/weekly-goal difficulty.
     *
     * @param difficulty selected difficulty
     * @return {@code true} when the preference was written
     */
    public static synchronized boolean setTrendDifficulty(PuzzleDifficulty difficulty) {
        if (difficulty == null) {
            return false;
        }
        PersonalPreferences preferences = loadPersonalPreferences();
        preferences.trendDifficulty = difficulty;
        return savePersonalPreferences(preferences);
    }

    /**
     * Computes player-only trends for one exact size/difficulty scope.
     *
     * @param size board size
     * @param difficulty difficulty scope
     * @return shared-core trend summary
     */
    public static synchronized PersonalTrend getPersonalTrend(int size,
            PuzzleDifficulty difficulty) {
        PuzzleDifficulty selected = normalizeDifficulty(difficulty);
        List<PersonalTrend.Sample> samples = new ArrayList<>();
        for (CompletionRecord record : getCompletionHistory()) {
            if (!record.assisted && record.size == size && record.difficulty == selected) {
                samples.add(new PersonalTrend.Sample(record.moves, record.timeMs));
            }
        }
        return PersonalTrend.summarize(samples);
    }

    /**
     * Computes the selected-scope weekly goal using local completion dates.
     * Assisted and Favorite Practice results are absent from completion history.
     *
     * @param today local date boundary
     * @param zoneId local date zone
     * @return selected-scope weekly progress
     */
    public static synchronized WeeklyGoalProgress getWeeklyGoalProgress(LocalDate today,
            ZoneId zoneId) {
        return getWeeklyGoalProgress(today, zoneId, getTrendSize(), getTrendDifficulty());
    }

    /**
     * Computes weekly progress for an explicit size/difficulty scope.
     *
     * @param today local date boundary
     * @param zoneId local date zone
     * @param size board size
     * @param difficulty difficulty scope
     * @return selected-scope weekly progress
     */
    public static synchronized WeeklyGoalProgress getWeeklyGoalProgress(LocalDate today,
            ZoneId zoneId, int size, PuzzleDifficulty difficulty) {
        ZoneId selectedZone = zoneId == null ? ZoneId.systemDefault() : zoneId;
        PuzzleDifficulty selected = normalizeDifficulty(difficulty);
        List<LocalDate> completionDates = new ArrayList<>();
        for (CompletionRecord record : getCompletionHistory()) {
            if (!record.assisted && record.size == size && record.difficulty == selected) {
                completionDates.add(Instant.ofEpochMilli(record.completedAt)
                        .atZone(selectedZone).toLocalDate());
            }
        }
        return WeeklyGoalProgress.calculate(today, getWeeklyGoalTarget(), completionDates);
    }

    private static List<FavoritePuzzle> getFavoritePuzzlesList() {
        File file = new File(getDataDirectory(), FAVORITES_FILE);
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                return parseFavorites(readText(candidate));
            } catch (IOException | RuntimeException ignored) {
                // Try the recoverable sibling.
            }
        }
        return new ArrayList<>();
    }

    private static boolean saveFavorites(List<FavoritePuzzle> favorites) {
        try {
            writeTextAtomic(new File(getDataDirectory(), FAVORITES_FILE), favoritesToJson(favorites));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static List<FavoritePuzzle> parseFavorites(String json) {
        List<FavoritePuzzle> favorites = new ArrayList<>();
        if (json == null || !json.trim().startsWith("[") || !json.trim().endsWith("]")) {
            throw new IllegalArgumentException("Invalid favorites file");
        }
        Matcher entries = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(json);
        Set<String> seenIds = new LinkedHashSet<>();
        while (entries.find() && favorites.size() < MAX_FAVORITE_PUZZLES) {
            String value = entries.group(1);
            String id = optionalStringField(value, "id", "");
            String labelEncoded = optionalStringField(value, "label", "");
            String difficultyId = optionalStringField(value, "difficulty",
                    PuzzleDifficulty.CLASSIC.getId());
            int size = (int) optionalLongField(value, "size", 0);
            long createdAt = optionalLongField(value, "createdAt", -1);
            String flattened = optionalStringField(value, "initialGrid", "");
            if (!isValidFavoriteId(id) || !isSupportedSize(size) || createdAt < 0
                    || !seenIds.add(id)) {
                continue;
            }
            try {
                PuzzleDifficulty difficulty = PuzzleDifficulty.fromId(difficultyId);
                String label = new String(Base64.getUrlDecoder().decode(labelEncoded),
                        StandardCharsets.UTF_8);
                label = normalizeFavoriteLabel(label);
                int[][] grid = parseFlattenedGrid(flattened, size);
                PuzzleIdentity identity = new PuzzleIdentity(size, difficulty, grid);
                if (!id.equals(identity.getId()) || label == null) {
                    continue;
                }
                favorites.add(new FavoritePuzzle(identity, label, createdAt));
            } catch (IllegalArgumentException exception) {
                // Ignore one corrupt row while preserving the remaining library.
            }
        }
        return favorites;
    }

    private static String favoritesToJson(List<FavoritePuzzle> favorites) {
        StringBuilder json = new StringBuilder("[\n");
        for (int index = 0; index < favorites.size(); index++) {
            if (index > 0) {
                json.append(",\n");
            }
            FavoritePuzzle favorite = favorites.get(index);
            String label = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    favorite.label.getBytes(StandardCharsets.UTF_8));
            json.append("  {\"id\": ").append(jsonString(favorite.id))
                    .append(", \"createdAt\": ").append(favorite.createdAt)
                    .append(", \"size\": ").append(favorite.size)
                    .append(", \"difficulty\": ").append(jsonString(favorite.difficulty.getId()))
                    .append(", \"label\": ").append(jsonString(label))
                    .append(", \"initialGrid\": ").append(jsonString(flattenGrid(favorite.initialGrid)))
                    .append("}");
        }
        return json.append("\n]\n").toString();
    }

    private static String normalizeFavoriteLabel(String label) {
        if (label == null) {
            return null;
        }
        String normalized = label.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_FAVORITE_LABEL_LENGTH) {
            return null;
        }
        return normalized;
    }

    private static String flattenGrid(int[][] grid) {
        StringBuilder value = new StringBuilder();
        for (int[] row : grid) {
            for (int tile : row) {
                if (value.length() > 0) {
                    value.append(',');
                }
                value.append(tile);
            }
        }
        return value.toString();
    }

    private static int[][] parseFlattenedGrid(String value, int size) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing favorite grid");
        }
        String[] values = value.split(",", -1);
        if (values.length != size * size) {
            throw new IllegalArgumentException("Invalid favorite grid length");
        }
        int[][] grid = new int[size][size];
        int index = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                grid[row][col] = Integer.parseInt(values[index++]);
            }
        }
        return grid;
    }

    private static boolean isValidFavoriteId(String favoriteId) {
        return favoriteId != null && favoriteId.matches("[0-9a-f]{64}");
    }

    private static File getFavoriteRunFile(String favoriteId) {
        return new File(getDataDirectory(), FAVORITE_RUN_PREFIX + favoriteId + FAVORITE_RUN_SUFFIX);
    }

    private static File getFavoriteAssistedFile(String favoriteId) {
        return new File(getDataDirectory(), FAVORITE_RUN_PREFIX + favoriteId
                + FAVORITE_ASSISTED_SUFFIX);
    }

    private static void deleteFavoriteRun(String favoriteId) {
        if (!isValidFavoriteId(favoriteId)) {
            return;
        }
        try {
            Files.deleteIfExists(getFavoriteRunFile(favoriteId).toPath());
            Files.deleteIfExists(getFavoriteAssistedFile(favoriteId).toPath());
        } catch (IOException ignored) {
            // Removing a stale practice file must not invalidate the library.
        }
    }

    private static PersonalPreferences loadPersonalPreferences() {
        File file = new File(getDataDirectory(), PERSONAL_PREFERENCES_FILE);
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                return personalPreferencesFromJson(readText(candidate));
            } catch (IOException | RuntimeException ignored) {
                // Try the recoverable sibling.
            }
        }
        return new PersonalPreferences();
    }

    private static boolean savePersonalPreferences(PersonalPreferences preferences) {
        try {
            writeTextAtomic(new File(getDataDirectory(), PERSONAL_PREFERENCES_FILE),
                    personalPreferencesToJson(preferences));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String personalPreferencesToJson(PersonalPreferences preferences) {
        return "{\n"
                + "  \"weeklyGoalTarget\": " + preferences.weeklyGoalTarget + ",\n"
                + "  \"trendSize\": " + preferences.trendSize + ",\n"
                + "  \"trendDifficulty\": " + jsonString(preferences.trendDifficulty.getId()) + ",\n"
                + "  \"reducedMotion\": " + preferences.reducedMotion + ",\n"
                + "  \"soundEnabled\": " + preferences.soundEnabled + ",\n"
                + "  \"theme\": " + jsonString(preferences.theme) + ",\n"
                + "  \"languageTag\": " + jsonString(preferences.languageTag) + ",\n"
                + "  \"onboardingSeen\": " + preferences.onboardingSeen + "\n"
                + "}\n";
    }

    private static PersonalPreferences personalPreferencesFromJson(String json) {
        PersonalPreferences preferences = new PersonalPreferences();
        preferences.weeklyGoalTarget = (int) optionalLongField(json,
                "weeklyGoalTarget", DEFAULT_WEEKLY_GOAL_TARGET);
        preferences.trendSize = (int) optionalLongField(json, "trendSize", 4);
        preferences.trendDifficulty = PuzzleDifficulty.fromId(optionalStringField(json,
                "trendDifficulty", PuzzleDifficulty.CLASSIC.getId()));
        preferences.reducedMotion = optionalBooleanField(json, "reducedMotion", false);
        preferences.soundEnabled = optionalBooleanField(json, "soundEnabled", false);
        preferences.theme = optionalStringField(json, "theme", "midnight");
        preferences.languageTag = optionalStringField(json, "languageTag", "en");
        preferences.onboardingSeen = optionalBooleanField(json, "onboardingSeen", false);
        return preferences;
    }

    private static File getContinuousMetaFile() {
        return new File(getDataDirectory(), CONTINUOUS_META_FILE);
    }

    private static File getContinuousCurrentFile() {
        return new File(getDataDirectory(), CONTINUOUS_CURRENT_FILE);
    }

    private static File getContinuousAssistedFile() {
        return new File(getDataDirectory(), CONTINUOUS_CURRENT_FILE + CONTINUOUS_ASSISTED_SUFFIX);
    }

    /**
     * Loads save data from caller-supplied JSON and legacy fallback files.
     *
     * @param saveFile primary JSON save file
     * @param legacySaveFile legacy serialized fallback file
     * @return parsed save data, or {@code null} when no valid save exists
     */
    static SaveData loadGame(File saveFile, File legacySaveFile) {
        SaveData data = readNormalJsonWithRecovery(saveFile);
        if (data != null) {
            return data;
        }
        return loadNormalLegacyGame(legacySaveFile);
    }

    /**
     * Validates a Desktop personal-data directory without migrating or
     * rewriting any source file. The archive service uses this against an
     * isolated candidate directory before replacement, and against a stable
     * source before export. The JVM data-directory property is scoped to this
     * synchronized call so the existing public loaders validate every mode
     * through their normal semantic paths.
     *
     * @param directory candidate Desktop data directory
     * <p>Presence relationships are part of the contract: Continuous metadata
     * and current state must appear together, and an assistance sidecar cannot
     * exist without both owners. The archive resolver presents logical
     * canonical candidates here, so callers can qualify recovery siblings
     * without mutating the source directory.</p>
     *
     * @return whether every present managed file is structurally and
     *         semantically valid
     */
    static synchronized boolean validatePersonalDataDirectory(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return directory != null && !directory.exists();
        }
        String previous = System.getProperty(DATA_DIR_PROPERTY);
        System.setProperty(DATA_DIR_PROPERTY, directory.getAbsolutePath());
        try {
            File[] files = directory.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                String name = file.getName();
                if (DesktopPersonalDataArchive.isManagedName(name)
                        && !name.endsWith(ATOMIC_TEMP_SUFFIX)
                        && !name.endsWith(ATOMIC_BACKUP_SUFFIX)
                        && (!file.isFile() || file.length() > DesktopPersonalDataArchive.MAX_ENTRY_BYTES)) {
                    return false;
                }
            }

            for (int size = MIN_SUPPORTED_SIZE; size <= MAX_SUPPORTED_SIZE; size++) {
                File slot = new File(directory, saveFileName(size));
                if (slot.exists() && !isValidSaveJsonFile(slot, false)) {
                    return false;
                }
            }
            File legacyJson = new File(directory, SAVE_FILE);
            if (legacyJson.exists() && !isValidSaveJsonFile(legacyJson, true)) {
                return false;
            }
            File legacySerialized = new File(directory, LEGACY_SAVE_FILE);
            if (legacySerialized.exists() && loadLegacyGame(legacySerialized) == null) {
                return false;
            }

            File scopedRecords = new File(directory, SCOPED_RECORDS_FILE);
            if (scopedRecords.exists() && !isValidScopedRecordsFile(scopedRecords)) {
                return false;
            }
            File legacyRecords = new File(directory, RECORDS_FILE);
            if (legacyRecords.exists() && !isValidLegacyRecordsFile(legacyRecords)) {
                return false;
            }
            File statistics = new File(directory, STATISTICS_FILE);
            if (statistics.exists() && !isValidCompletionStoreFile(statistics)) {
                return false;
            }
            if (!validateResetMarker(new File(directory, RECORDS_RESET_FILE))
                    || !validateResetMarker(new File(directory, SAVED_GAMES_RESET_FILE))) {
                return false;
            }

            File dailyProgress = new File(directory, DAILY_PROGRESS_FILE);
            if (dailyProgress.exists()) {
                try {
                    String json = readText(dailyProgress);
                    if (!hasField(json, "completedDates") || !hasField(json, "currentStreak")
                            || !hasField(json, "bestStreak")) {
                        return false;
                    }
                    DailyProgressState state = dailyProgressFromJson(json);
                    for (String dateId : state.completedDates) {
                        if (parseDailyDate(dateId) == null) {
                            return false;
                        }
                    }
                } catch (IOException | RuntimeException exception) {
                    return false;
                }
            }
            if (!validateDailyFiles(directory)) {
                return false;
            }

            File favorites = new File(directory, FAVORITES_FILE);
            if (favorites.exists() && !isValidFavoritesFile(favorites)) {
                return false;
            }
            if (!validateFavoriteRunFiles(directory)) {
                return false;
            }

            File preferences = new File(directory, PERSONAL_PREFERENCES_FILE);
            if (preferences.exists() && !isValidPreferencesFile(preferences)) {
                return false;
            }
            File continuousMeta = new File(directory, CONTINUOUS_META_FILE);
            File continuousCurrent = new File(directory, CONTINUOUS_CURRENT_FILE);
            File continuousAssisted = new File(directory,
                    CONTINUOUS_CURRENT_FILE + CONTINUOUS_ASSISTED_SUFFIX);
            if (continuousMeta.exists() != continuousCurrent.exists()) {
                return false;
            }
            if (continuousMeta.exists() && loadContinuousGame() == null) {
                return false;
            }
            if (continuousAssisted.exists()
                    && (!continuousCurrent.exists() || !continuousMeta.exists()
                            || !isBooleanMarker(continuousAssisted))) {
                return false;
            }
            return true;
        } finally {
            if (previous == null) {
                System.clearProperty(DATA_DIR_PROPERTY);
            } else {
                System.setProperty(DATA_DIR_PROPERTY, previous);
            }
        }
    }

    private static boolean isValidSaveJsonFile(File file, boolean legacy) {
        try {
            String json = readText(file);
            if (json == null || !json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                return false;
            }
            SaveData data = normalizeSaveData(fromJson(json));
            if (data == null || !isSupportedSize(data.size)) {
                return false;
            }
            long version = optionalLongField(json, "version", -1);
            if (!legacy && (version < 1 || version > CURRENT_SAVE_VERSION)) {
                return false;
            }
            if (!legacy && version >= CURRENT_SAVE_VERSION && !hasBooleanField(json, "assisted")) {
                return false;
            }
            if (!legacy && version >= CURRENT_SAVE_VERSION
                    && (!hasField(json, "actionHistory") || !hasField(json, "redoHistory"))) {
                return false;
            }
            if (!legacy && hasField(json, "difficulty")
                    && !isDifficultyId(optionalStringField(json, "difficulty", null))) {
                return false;
            }
            GameModel reconstructed = new GameModel(data.size);
            reconstructed.loadState(data);
            if (!legacy && version >= CURRENT_SAVE_VERSION
                    && (!data.actionHistory.equals(reconstructed.getEncodedActionHistory())
                            || !data.redoHistory.equals(reconstructed.getEncodedRedoHistory()))) {
                return false;
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean isValidScopedRecordsFile(File file) {
        try {
            return validateRecordJson(readText(file), true);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean isValidLegacyRecordsFile(File file) {
        try {
            return validateRecordJson(readText(file), false);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean validateRecordJson(String json, boolean scoped) {
        if (json == null) {
            return false;
        }
        String compact = json.replaceAll("\\s+", "");
        if (compact.equals("{}")) {
            return true;
        }
        String entryKey = scoped ? "[345]:(?:relaxed|classic|challenge)" : "[345]";
        String entryPattern = "\\\"(" + entryKey + ")\\\":\\{\\\"moves\\\":(\\d+),\\\"timeMs\\\":(\\d+)\\}";
        Matcher whole = Pattern.compile("^\\{(" + entryPattern + "(?:," + entryPattern + ")*)\\}$")
                .matcher(compact);
        if (!whole.matches()) {
            return false;
        }
        Set<String> keys = new java.util.HashSet<>();
        Matcher entries = Pattern.compile(entryPattern).matcher(whole.group(1));
        int count = 0;
        while (entries.find()) {
            if (!keys.add(entries.group(1))) {
                return false;
            }
            Integer.parseInt(entries.group(2));
            Long.parseLong(entries.group(3));
            count++;
        }
        return count > 0;
    }

    private static boolean isValidCompletionStoreFile(File file) {
        try {
            String json = readText(file);
            if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                return false;
            }
            if (optionalLongField(json, "version", -1) != 1
                    || !hasField(json, "recordedIds") || !hasField(json, "history")
                    || !hasField(json, "stats")) {
                return false;
            }
            completionStoreFromJson(json);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean validateResetMarker(File file) {
        if (!file.exists()) {
            return true;
        }
        return isBooleanOrResetMarker(file, "reset");
    }

    private static boolean isBooleanOrResetMarker(File file, String resetValue) {
        try {
            String value = readText(file).trim();
            return resetValue.equals(value);
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean isBooleanMarker(File file) {
        try {
            String value = readText(file).trim();
            return "true".equals(value) || "false".equals(value);
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean validateDailyFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith(DAILY_SAVE_PREFIX) || name.equals(DAILY_PROGRESS_FILE)
                    || name.endsWith(DAILY_ASSISTED_SUFFIX)) {
                if (name.startsWith(DAILY_SAVE_PREFIX) && name.endsWith(DAILY_ASSISTED_SUFFIX)
                        && (!new File(directory, name.substring(0, name.length()
                                - DAILY_ASSISTED_SUFFIX.length()) + DAILY_SAVE_SUFFIX).exists()
                                || !isBooleanMarker(file))) {
                    return false;
                }
                continue;
            }
            if (!name.endsWith(DAILY_SAVE_SUFFIX)) {
                return false;
            }
            String dateId = name.substring(DAILY_SAVE_PREFIX.length(),
                    name.length() - DAILY_SAVE_SUFFIX.length());
            LocalDate date;
            try {
                date = LocalDate.parse(dateId);
            } catch (DateTimeParseException exception) {
                return false;
            }
            if (date.isAfter(LocalDate.now())) {
                return false;
            }
            try {
                SaveData data = normalizeSaveData(fromJson(readText(file)));
                DailyChallenge challenge = DailyChallenge.forDate(date);
                if (data == null || !matchesDailyIdentity(data.size, data.difficulty,
                        data.initialGrid, challenge)) {
                    return false;
                }
            } catch (IOException | RuntimeException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidFavoritesFile(File file) {
        try {
            String json = readText(file);
            if (!json.trim().startsWith("[") || !json.trim().endsWith("]")) {
                return false;
            }
            int objects = 0;
            Matcher matcher = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(json);
            while (matcher.find()) {
                objects++;
            }
            return parseFavorites(json).size() == objects;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean validateFavoriteRunFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith(FAVORITE_RUN_PREFIX)) {
                continue;
            }
            if (name.endsWith(FAVORITE_ASSISTED_SUFFIX)) {
                String id = name.substring(FAVORITE_RUN_PREFIX.length(),
                        name.length() - FAVORITE_ASSISTED_SUFFIX.length());
                if (!isValidFavoriteId(id)
                        || !new File(directory, FAVORITE_RUN_PREFIX + id + FAVORITE_RUN_SUFFIX).exists()
                        || !isBooleanMarker(file)) {
                    return false;
                }
                continue;
            }
            if (!name.endsWith(FAVORITE_RUN_SUFFIX)) {
                return false;
            }
            String id = name.substring(FAVORITE_RUN_PREFIX.length(),
                    name.length() - FAVORITE_RUN_SUFFIX.length());
            if (!isValidFavoriteId(id)) {
                return false;
            }
            try {
                FavoritePuzzle favorite = getFavoritePuzzle(id);
                SaveData data = normalizeSaveData(fromJson(readText(file)));
                if (favorite == null || data == null || data.difficulty != favorite.difficulty
                        || !Arrays.deepEquals(data.initialGrid, favorite.initialGrid)) {
                    return false;
                }
            } catch (IOException | RuntimeException exception) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPreferencesFile(File file) {
        try {
            String json = readText(file);
            if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                return false;
            }
            String[] required = {"weeklyGoalTarget", "trendSize", "trendDifficulty",
                    "reducedMotion", "soundEnabled", "theme", "languageTag", "onboardingSeen"};
            for (String key : required) {
                if (!hasField(json, key)) {
                    return false;
                }
            }
            PersonalPreferences preferences = personalPreferencesFromJson(json);
            return WeeklyGoalProgress.isValidTarget(preferences.weeklyGoalTarget)
                    && isSupportedSize(preferences.trendSize)
                    && preferences.trendDifficulty != null
                    && normalizeDesktopTheme(preferences.theme) != null
                    && normalizeDesktopLanguage(preferences.languageTag) != null;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static String readText(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int total = 0;
            while (total < bytes.length) {
                int read = fis.read(bytes, total, bytes.length - total);
                if (read < 0) {
                    break;
                }
                total += read;
            }
            return new String(bytes, 0, total, StandardCharsets.UTF_8);
        }
    }

    /**
     * Records a completed game's score if it beats the previous local record.
     *
     * @param size puzzle size, such as 3, 4, or 5
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @return the best record after comparing the submitted result
     */
    public static BestRecord recordBest(int size, int moves, long timeMs) {
        return recordBest(size, PuzzleDifficulty.CLASSIC, moves, timeMs);
    }

    /**
     * Records a player score in the size-plus-difficulty namespace.
     * <p>
     * A legacy size-only record is read as the Classic record until a better
     * Classic result is written to the additive v2 file. The legacy source is
     * never rewritten or deleted.
     * </p>
     *
     * @param size puzzle size, such as 3, 4, or 5
     * @param difficulty stable difficulty scope, defaulting to Classic when null
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @return the best record after comparing the submitted result
     */
    public static synchronized BestRecord recordBest(int size, PuzzleDifficulty difficulty,
            int moves, long timeMs) {
        if (!isSupportedSize(size)) {
            return null;
        }
        PuzzleDifficulty selected = normalizeDifficulty(difficulty);
        String key = recordScopeKey(size, selected);
        File target = new File(getDataDirectory(), SCOPED_RECORDS_FILE);
        Map<String, BestRecord> records = loadScopedRecordsForWrite(target);
        BestRecord current = records.get(key);
        if (current == null && selected == PuzzleDifficulty.CLASSIC) {
            current = findLegacyRecord(size);
        }
        BestRecord candidate = new BestRecord(moves, timeMs);
        if (current == null || candidate.isBetterThan(current)) {
            records.put(key, candidate);
            saveScopedRecords(target, records);
            return candidate;
        }
        return current;
    }

    /**
     * Records a player score only when it improves the scoped record.
     *
     * @param size puzzle size
     * @param difficulty difficulty scope
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @return {@code true} when the submitted score is a new scoped best
     */
    public static synchronized boolean recordBestIfBetter(int size, PuzzleDifficulty difficulty,
            int moves, long timeMs) {
        BestRecord current = getBestRecord(size, difficulty);
        BestRecord candidate = new BestRecord(moves, timeMs);
        if (current != null && !candidate.isBetterThan(current)) {
            return false;
        }
        recordBest(size, difficulty, moves, timeMs);
        return true;
    }

    /**
     * Records a completed score in a caller-supplied records file.
     *
     * @param recordsFile JSON records file to read and update
     * @param size puzzle size, such as 3, 4, or 5
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @return the best record after comparing the submitted result
     */
    static BestRecord recordBest(File recordsFile, int size, int moves, long timeMs) {
        return recordBest(recordsFile, recordsFile, size, moves, timeMs);
    }

    private static BestRecord recordBest(File recordsFile, File sourceRecordsFile, int size, int moves, long timeMs) {
        Map<Integer, BestRecord> records = loadRecords(sourceRecordsFile);
        BestRecord current = records.get(size);
        BestRecord candidate = new BestRecord(moves, timeMs);

        if (current == null || candidate.isBetterThan(current)) {
            records.put(size, candidate);
            saveRecords(recordsFile, records);
            return candidate;
        }

        return current;
    }

    /**
     * Reads the best local record for a puzzle size.
     *
     * @param size puzzle size
     * @return the best record, or {@code null} if none has been saved
     */
    public static BestRecord getBestRecord(int size) {
        return getBestRecord(size, PuzzleDifficulty.CLASSIC);
    }

    /**
     * Reads the best local player record for one size and difficulty.
     *
     * @param size puzzle size
     * @param difficulty difficulty scope, defaulting to Classic when null
     * @return the best record, or {@code null} when no result exists
     */
    public static synchronized BestRecord getBestRecord(int size, PuzzleDifficulty difficulty) {
        if (!isSupportedSize(size)) {
            return null;
        }
        PuzzleDifficulty selected = normalizeDifficulty(difficulty);
        BestRecord record = findScopedRecord(size, selected);
        if (record == null && selected == PuzzleDifficulty.CLASSIC) {
            return findLegacyRecord(size);
        }
        return record;
    }

    /**
     * Reads the best local record for a puzzle size from a caller-supplied file.
     *
     * @param recordsFile JSON records file to read
     * @param size puzzle size
     * @return the best record, or {@code null} if none has been saved
     */
    static BestRecord getBestRecord(File recordsFile, int size) {
        return loadRecords(recordsFile).get(size);
    }

    private static PuzzleDifficulty normalizeDifficulty(PuzzleDifficulty difficulty) {
        return difficulty == null ? PuzzleDifficulty.CLASSIC : difficulty;
    }

    private static String recordScopeKey(int size, PuzzleDifficulty difficulty) {
        return size + ":" + normalizeDifficulty(difficulty).getId();
    }

    private static BestRecord findScopedRecord(int size, PuzzleDifficulty difficulty) {
        File dataFile = new File(getDataDirectory(), SCOPED_RECORDS_FILE);
        BestRecord record = loadScopedRecords(dataFile).get(recordScopeKey(size, difficulty));
        if (record != null) {
            return record;
        }
        if (isRecordsReset()) {
            return null;
        }
        File rootFile = new File(SCOPED_RECORDS_FILE);
        if (!sameFile(dataFile, rootFile)) {
            return loadScopedRecords(rootFile).get(recordScopeKey(size, difficulty));
        }
        return null;
    }

    private static BestRecord findLegacyRecord(int size) {
        if (isRecordsReset()) {
            return null;
        }
        BestRecord record = getBestRecord(new File(getDataDirectory(), RECORDS_FILE), size);
        if (record != null) {
            return record;
        }
        File rootFile = new File(RECORDS_FILE);
        if (!sameFile(rootFile, new File(getDataDirectory(), RECORDS_FILE))) {
            return getBestRecord(rootFile, size);
        }
        return null;
    }

    private static boolean isRecordsReset() {
        return new File(getDataDirectory(), RECORDS_RESET_FILE).isFile();
    }

    private static boolean isSavedGamesReset() {
        return new File(getDataDirectory(), SAVED_GAMES_RESET_FILE).isFile();
    }

    private static Map<String, BestRecord> loadScopedRecordsForWrite(File target) {
        if (target.exists()) {
            return loadScopedRecords(target);
        }
        File rootFile = new File(SCOPED_RECORDS_FILE);
        if (!sameFile(target, rootFile) && rootFile.exists()) {
            return loadScopedRecords(rootFile);
        }
        return new HashMap<>();
    }

    private static Map<String, BestRecord> loadScopedRecords(File file) {
        Map<String, BestRecord> records = new HashMap<>();
        if (file == null) {
            return records;
        }
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                String json = readText(candidate);
                String trimmed = json.trim();
                if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                    continue;
                }
                Matcher matcher = Pattern.compile(
                        "\\\"(\\d+:(?:relaxed|classic|challenge))\\\"\\s*:\\s*\\{\\s*"
                                + "\\\"moves\\\"\\s*:\\s*(\\d+)\\s*,\\s*"
                                + "\\\"timeMs\\\"\\s*:\\s*(\\d+)\\s*\\}").matcher(json);
                while (matcher.find()) {
                    records.put(matcher.group(1), new BestRecord(
                            Integer.parseInt(matcher.group(2)), Long.parseLong(matcher.group(3))));
                }
                return records;
            } catch (IOException | RuntimeException ignored) {
                // Try the recoverable sibling.
            }
        }
        return records;
    }

    private static void saveScopedRecords(File file, Map<String, BestRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int index = 0;
        for (Map.Entry<String, BestRecord> entry : new TreeMap<>(records).entrySet()) {
            if (index++ > 0) {
                sb.append(",\n");
            }
            BestRecord record = entry.getValue();
            sb.append("  \"").append(entry.getKey()).append("\": {");
            sb.append("\"moves\": ").append(record.moves).append(", ");
            sb.append("\"timeMs\": ").append(record.timeMs).append("}");
        }
        sb.append("\n}\n");
        try {
            ensureParentDirectory(file);
            writeTextAtomic(file, sb.toString());
        } catch (IOException ignored) {
            // Records remain unchanged when an atomic replacement fails.
        }
    }

    /**
     * Records one player or solver-assisted completion exactly once.
     *
     * @param completionId stable id for the current completed run
     * @param size puzzle size
     * @param difficulty difficulty scope
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @param assisted whether solver or strategic assistance produced the win
     * @return {@code true} when a new completion sample was persisted
     */
    public static boolean recordCompletion(String completionId, int size, PuzzleDifficulty difficulty,
            int moves, long timeMs, boolean assisted) {
        return recordCompletion(completionId, size, difficulty, moves, timeMs, assisted,
                System.currentTimeMillis());
    }

    /**
     * Testable completion-recording overload with an explicit completion time.
     *
     * @param completionId stable id for the current completed run
     * @param size puzzle size
     * @param difficulty difficulty scope
     * @param moves final move count
     * @param timeMs elapsed time in milliseconds
     * @param assisted whether solver or strategic assistance produced the win
     * @param completedAt completion timestamp in milliseconds since epoch
     * @return {@code true} when a new completion sample was persisted
     */
    public static synchronized boolean recordCompletion(String completionId, int size,
            PuzzleDifficulty difficulty, int moves, long timeMs, boolean assisted, long completedAt) {
        if (completionId == null || completionId.isBlank() || !isSupportedSize(size)
                || moves < 0 || timeMs < 0 || completedAt < 0) {
            return false;
        }
        CompletionStore store = loadCompletionStore();
        if (store.recordedIds.contains(completionId)) {
            return false;
        }

        PuzzleDifficulty selected = normalizeDifficulty(difficulty);
        CompletionRecord record = new CompletionRecord(completionId, completedAt, size,
                selected, moves, timeMs, assisted);
        store.recordedIds.add(completionId);
        store.history.add(0, record);
        while (store.history.size() > MAX_COMPLETION_HISTORY) {
            store.history.remove(store.history.size() - 1);
        }
        String key = recordScopeKey(size, selected);
        CompletionStatsMutable stats = store.stats.get(key);
        if (stats == null) {
            stats = new CompletionStatsMutable();
            store.stats.put(key, stats);
        }
        if (assisted) {
            stats.assistedCompletions++;
        } else {
            stats.playerCompletions++;
            stats.playerMoves += moves;
            stats.playerTimeMs += timeMs;
        }
        return saveCompletionStore(store);
    }

    /**
     * Returns newest-first local completion samples, retaining at most 50.
     *
     * @return a defensive array of completion records
     */
    public static synchronized CompletionRecord[] getCompletionHistory() {
        CompletionStore store = loadCompletionStore();
        return store.history.toArray(new CompletionRecord[0]);
    }

    /**
     * Returns lifetime completion totals for one size and difficulty.
     *
     * @param size puzzle size
     * @param difficulty difficulty scope
     * @return immutable scoped totals, or zero totals when none exist
     */
    public static synchronized CompletionStats getCompletionStats(int size, PuzzleDifficulty difficulty) {
        if (!isSupportedSize(size)) {
            return CompletionStats.empty();
        }
        CompletionStatsMutable stats = loadCompletionStore().stats.get(recordScopeKey(size, difficulty));
        return stats == null ? CompletionStats.empty() : stats.toImmutable();
    }

    /**
     * Returns lifetime totals across all size and difficulty scopes.
     *
     * @return immutable aggregate totals
     */
    public static synchronized CompletionStats getOverallCompletionStats() {
        CompletionStatsMutable total = new CompletionStatsMutable();
        for (CompletionStatsMutable stats : loadCompletionStore().stats.values()) {
            total.add(stats);
        }
        return total.toImmutable();
    }

    private static CompletionStore loadCompletionStore() {
        File file = new File(getDataDirectory(), STATISTICS_FILE);
        File rootFile = new File(STATISTICS_FILE);
        CompletionStore store = loadCompletionStore(file);
        if (store.hasData() || sameFile(file, rootFile)) {
            return store;
        }
        return loadCompletionStore(rootFile);
    }

    private static CompletionStore loadCompletionStore(File file) {
        CompletionStore empty = new CompletionStore();
        if (file == null) {
            return empty;
        }
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                return completionStoreFromJson(readText(candidate));
            } catch (IOException | RuntimeException ignored) {
                // Try the recoverable sibling.
            }
        }
        return empty;
    }

    private static boolean saveCompletionStore(CompletionStore store) {
        try {
            File file = new File(getDataDirectory(), STATISTICS_FILE);
            ensureParentDirectory(file);
            writeTextAtomic(file, completionStoreToJson(store));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static SaveData loadLegacyGame(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object value = ois.readObject();
            if (!(value instanceof SaveData)) {
                return null;
            }
            SaveData data = (SaveData) value;
            if (data.initialGrid == null) {
                data.initialGrid = data.grid;
            }
            return normalizeSaveData(data);
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private static SaveData loadNormalLegacyGame(File file) {
        return applyLegacyNormalAssistancePolicy(loadLegacyGame(file));
    }

    private static void migrateLegacySaves() {
        if (isSavedGamesReset()) {
            return;
        }
        File dataDirectory = getDataDirectory();
        for (File legacyFile : legacyCandidates(dataDirectory)) {
            SaveData legacy = legacyFile.getName().endsWith(".dat")
                    ? loadNormalLegacyGame(legacyFile)
                    : readNormalJsonWithRecovery(legacyFile);
            if (legacy == null || !isSupportedSize(legacy.size)) {
                continue;
            }

            File target = getSaveFile(legacy.size);
            SaveData current = readNormalJsonWithRecovery(target);
            if (current != null && current.updatedAt >= legacy.updatedAt) {
                continue;
            }
            try {
                writeTextAtomic(target, toJson(legacy));
            } catch (IOException ignored) {
                // The original legacy file remains untouched for a later retry.
            }
        }
    }

    private static SaveData loadLegacyFallback() {
        File dataDirectory = getDataDirectory();
        for (File legacyFile : legacyCandidates(dataDirectory)) {
            SaveData data = legacyFile.getName().endsWith(".dat")
                    ? loadNormalLegacyGame(legacyFile)
                    : readNormalJsonWithRecovery(legacyFile);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private static List<File> legacyCandidates(File dataDirectory) {
        List<File> candidates = new ArrayList<>();
        addUnique(candidates, new File(dataDirectory, SAVE_FILE));
        addUnique(candidates, new File(dataDirectory, LEGACY_SAVE_FILE));
        addUnique(candidates, new File(SAVE_FILE));
        addUnique(candidates, new File(LEGACY_SAVE_FILE));
        return candidates;
    }

    private static void addUnique(List<File> files, File candidate) {
        try {
            String path = candidate.getCanonicalPath();
            for (File existing : files) {
                if (existing.getCanonicalPath().equals(path)) {
                    return;
                }
            }
        } catch (IOException ignored) {
            for (File existing : files) {
                if (existing.equals(candidate)) {
                    return;
                }
            }
        }
        files.add(candidate);
    }

    private static SaveData loadSlot(int size) {
        SaveData data = readNormalJsonWithRecovery(getSaveFile(size));
        if (data != null && data.size == size) {
            return data;
        }
        if (isSavedGamesReset()) {
            return null;
        }
        File rootSlot = new File(saveFileName(size));
        if (!sameFile(rootSlot, getSaveFile(size))) {
            data = readNormalJsonWithRecovery(rootSlot);
        }
        return data != null && data.size == size ? data : null;
    }

    /**
     * Applies the conservative normal-save migration rule. Before schema v4,
     * a solved Desktop normal save could be written after solver playback
     * without carrying the in-memory assisted bit. Such a payload cannot be
     * proven player-eligible, so the loader fails closed by treating it as
     * assisted. Unsolved legacy normal saves remain unassisted because the
     * pre-v4 autosave gate rejected solver-owned or busy board sessions.
     */
    private static SaveData applyLegacyNormalAssistancePolicy(SaveData data) {
        if (data != null && !data.assistedMetadataPresent && data.solved) {
            data.assisted = true;
        }
        return data;
    }

    private static SaveData readNormalJsonWithRecovery(File file) {
        return applyLegacyNormalAssistancePolicy(readJsonWithRecovery(file));
    }

    private static SaveData readJsonWithRecovery(File file) {
        if (file == null) {
            return null;
        }
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                SaveData data = normalizeSaveData(fromJson(readText(candidate)));
                if (data != null) {
                    return data;
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
                // A partial/current file can fall back to its recoverable sibling.
            }
        }
        return null;
    }

    private static boolean sameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException ignored) {
            return first.equals(second);
        }
    }

    private static String saveFileName(int size) {
        return SAVE_FILE_PREFIX + size + SAVE_FILE_SUFFIX;
    }

    private static File getSaveFile(int size) {
        return new File(getDataDirectory(), saveFileName(size));
    }

    private static LocalDate parseDailyDate(String dateId) {
        if (dateId == null || dateId.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateId);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static File getDailySaveFile(LocalDate date) {
        return new File(getDataDirectory(), DAILY_SAVE_PREFIX + date + DAILY_SAVE_SUFFIX);
    }

    private static File getDailyAssistedFile(LocalDate date) {
        return new File(getDataDirectory(), DAILY_SAVE_PREFIX + date + DAILY_ASSISTED_SUFFIX);
    }

    private static boolean matchesDailyIdentity(int size, PuzzleDifficulty difficulty,
            int[][] initialGrid, DailyChallenge challenge) {
        if (challenge == null || size != challenge.getSize()
                || normalizeDifficulty(difficulty) != challenge.getDifficulty()
                || initialGrid == null) {
            return false;
        }
        return Arrays.deepEquals(initialGrid, challenge.createGame().getInitialGridCopy());
    }

    private static DailyProgressState loadDailyProgressState() {
        File file = new File(getDataDirectory(), DAILY_PROGRESS_FILE);
        File[] candidates = {
                file,
                new File(file.getPath() + ATOMIC_TEMP_SUFFIX),
                new File(file.getPath() + ATOMIC_BACKUP_SUFFIX)
        };
        for (File candidate : candidates) {
            if (!candidate.exists() || candidate.isDirectory()) {
                continue;
            }
            try {
                return dailyProgressFromJson(readText(candidate));
            } catch (IOException | RuntimeException ignored) {
                // Try the recoverable sibling.
            }
        }
        return new DailyProgressState();
    }

    private static boolean saveDailyProgressState(DailyProgressState state) {
        File file = new File(getDataDirectory(), DAILY_PROGRESS_FILE);
        try {
            ensureParentDirectory(file);
            writeTextAtomic(file, dailyProgressToJson(state));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String dailyProgressToJson(DailyProgressState state) {
        StringBuilder json = new StringBuilder("{\n  \"completedDates\": [");
        int index = 0;
        for (String date : state.completedDates) {
            if (index++ > 0) {
                json.append(", ");
            }
            json.append(jsonString(date));
        }
        json.append("],\n  \"lastCompletedDate\": ")
                .append(state.lastCompletedDate == null ? "null" : jsonString(state.lastCompletedDate.toString()))
                .append(",\n  \"currentStreak\": ").append(state.currentStreak)
                .append(",\n  \"bestStreak\": ").append(state.bestStreak)
                .append("\n}\n");
        return json.toString();
    }

    private static DailyProgressState dailyProgressFromJson(String json) {
        if (json == null || !json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            throw new IllegalArgumentException("Invalid daily progress");
        }
        DailyProgressState state = new DailyProgressState();
        Matcher dates = Pattern.compile("\\\"completedDates\\\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL).matcher(json);
        if (dates.find()) {
            Matcher date = Pattern.compile("\\\"(\\d{4}-\\d{2}-\\d{2})\\\"").matcher(dates.group(1));
            while (date.find()) {
                if (parseDailyDate(date.group(1)) != null) {
                    state.completedDates.add(date.group(1));
                }
            }
        }
        Matcher last = Pattern.compile("\\\"lastCompletedDate\\\"\\s*:\\s*\\\"(\\d{4}-\\d{2}-\\d{2})\\\"")
                .matcher(json);
        if (last.find()) {
            state.lastCompletedDate = parseDailyDate(last.group(1));
        }
        state.currentStreak = Math.max(0, (int) optionalLongField(json, "currentStreak", 0));
        state.bestStreak = Math.max(0, (int) optionalLongField(json, "bestStreak", 0));
        return state;
    }

    private static String toJson(SaveData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": ").append(CURRENT_SAVE_VERSION).append(",\n");
        sb.append("  \"size\": ").append(data.size).append(",\n");
        sb.append("  \"moveCount\": ").append(data.moveCount).append(",\n");
        sb.append("  \"elapsedTime\": ").append(data.elapsedTime).append(",\n");
        sb.append("  \"updatedAt\": ").append(data.updatedAt).append(",\n");
        sb.append("  \"active\": ").append(data.active).append(",\n");
        sb.append("  \"solved\": ").append(data.solved).append(",\n");
        sb.append("  \"assisted\": ").append(data.assisted).append(",\n");
        sb.append("  \"difficulty\": \"").append(data.difficulty.getId()).append("\",\n");
        sb.append("  \"grid\": ").append(gridToJson(data.grid)).append(",\n");
        sb.append("  \"initialGrid\": ").append(gridToJson(data.initialGrid)).append(",\n");
        sb.append("  \"actionHistory\": \"").append(data.actionHistory).append("\",\n");
        sb.append("  \"redoHistory\": \"").append(data.redoHistory).append("\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static SaveData fromJson(String json) {
        SaveData data = new SaveData();
        long version = optionalLongField(json, "version", 1);
        if (version > CURRENT_SAVE_VERSION) {
            throw new IllegalArgumentException("Unsupported save version: " + version);
        }
        data.size = intField(json, "size");
        data.moveCount = (int) optionalLongField(json, "moveCount", 0);
        data.elapsedTime = optionalLongField(json, "elapsedTime", 0);
        data.updatedAt = optionalLongField(json, "updatedAt", 0);
        data.active = optionalBooleanField(json, "active", false);
        data.solved = optionalBooleanField(json, "solved", false);
        data.assisted = optionalBooleanField(json, "assisted", false);
        data.assistedMetadataPresent = hasBooleanField(json, "assisted");
        data.difficulty = PuzzleDifficulty.fromId(optionalStringField(json, "difficulty", null));
        data.grid = gridField(json, "grid", data.size);
        data.initialGrid = optionalGridField(json, "initialGrid", data.size);
        data.actionHistory = optionalStringField(json, "actionHistory", "");
        data.redoHistory = optionalStringField(json, "redoHistory", "");
        return data;
    }

    private static SaveData normalizeSaveData(SaveData data) {
        if (data == null || data.grid == null) {
            return null;
        }
        if (data.size <= 0) {
            data.size = data.grid.length;
        }
        if (!isValidGrid(data.grid, data.size)) {
            return null;
        }
        if (data.initialGrid == null || !isValidGrid(data.initialGrid, data.size)) {
            data.initialGrid = data.grid;
        }
        if (data.difficulty == null) {
            data.difficulty = PuzzleDifficulty.CLASSIC;
        }
        if (data.actionHistory == null) {
            data.actionHistory = "";
        }
        if (data.redoHistory == null) {
            data.redoHistory = "";
        }
        boolean solvedGrid = isSolvedGrid(data.grid);
        data.solved = data.solved || solvedGrid;
        data.active = !data.solved && (data.active || data.updatedAt == 0);
        if (data.updatedAt < 0) {
            data.updatedAt = 0;
        }
        if (data.moveCount < 0) {
            data.moveCount = 0;
        }
        if (data.elapsedTime < 0) {
            data.elapsedTime = 0;
        }
        return data;
    }

    private static String gridToJson(int[][] grid) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int r = 0; r < grid.length; r++) {
            if (r > 0) {
                sb.append(", ");
            }
            sb.append("[");
            for (int c = 0; c < grid[r].length; c++) {
                if (c > 0) {
                    sb.append(", ");
                }
                sb.append(grid[r][c]);
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private static int intField(String json, String key) {
        return (int) longField(json, key);
    }

    private static long longField(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing JSON field: " + key);
        }
        return Long.parseLong(matcher.group(1));
    }

    private static long optionalLongField(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : fallback;
    }

    private static boolean optionalBooleanField(String json, String key, boolean fallback) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : fallback;
    }

    private static boolean hasBooleanField(String json, String key) {
        return Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(json).find();
    }

    private static boolean hasField(String json, String key) {
        return Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:").matcher(json).find();
    }

    private static boolean isDifficultyId(String value) {
        return PuzzleDifficulty.RELAXED.getId().equalsIgnoreCase(value)
                || PuzzleDifficulty.CLASSIC.getId().equalsIgnoreCase(value)
                || PuzzleDifficulty.CHALLENGE.getId().equalsIgnoreCase(value);
    }

    private static String optionalStringField(String json, String key, String fallback) {
        Matcher matcher = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static int[][] gridField(String json, String key, int size) {
        if (!isSupportedSize(size)) {
            throw new IllegalArgumentException("Unsupported saved board size: " + size);
        }
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing JSON grid: " + key);
        }

        int start = json.indexOf('[', keyIndex);
        int end = findMatchingBracket(json, start);
        Matcher matcher = Pattern.compile("-?\\d+").matcher(json.substring(start, end + 1));
        int[][] grid = new int[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!matcher.find()) {
                    throw new IllegalArgumentException("Grid has too few values: " + key);
                }
                grid[r][c] = Integer.parseInt(matcher.group());
            }
        }
        return grid;
    }

    private static int[][] optionalGridField(String json, String key, int size) {
        if (!json.contains("\"" + key + "\"")) {
            return null;
        }
        return gridField(json, key, size);
    }

    private static int findMatchingBracket(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Unclosed JSON array");
    }

    private static Map<Integer, BestRecord> loadRecords(File file) {
        Map<Integer, BestRecord> records = new HashMap<>();
        if (!file.exists()) {
            return records;
        }

        try {
            String json = readText(file);
            Matcher matcher = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{\\s*\"moves\"\\s*:\\s*(\\d+)\\s*,\\s*\"timeMs\"\\s*:\\s*(\\d+)\\s*\\}").matcher(json);
            while (matcher.find()) {
                int size = Integer.parseInt(matcher.group(1));
                int moves = Integer.parseInt(matcher.group(2));
                long timeMs = Long.parseLong(matcher.group(3));
                records.put(size, new BestRecord(moves, timeMs));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static void saveRecords(File file, Map<Integer, BestRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int index = 0;
        for (Map.Entry<Integer, BestRecord> entry : records.entrySet()) {
            if (index++ > 0) {
                sb.append(",\n");
            }
            BestRecord record = entry.getValue();
            sb.append("  \"").append(entry.getKey()).append("\": {");
            sb.append("\"moves\": ").append(record.moves).append(", ");
            sb.append("\"timeMs\": ").append(record.timeMs).append("}");
        }
        sb.append("\n}\n");

        try {
            ensureParentDirectory(file);
            writeTextAtomic(file, sb.toString());
        } catch (IOException e) {
            // Records remain unchanged when an atomic replacement fails.
        }
    }

    private static String completionStoreToJson(CompletionStore store) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"version\": 1,\n  \"recordedIds\": [");
        for (int index = 0; index < store.recordedIds.size(); index++) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(jsonString(store.recordedIds.get(index)));
        }
        sb.append("],\n  \"history\": [\n");
        for (int index = 0; index < store.history.size(); index++) {
            if (index > 0) {
                sb.append(",\n");
            }
            CompletionRecord record = store.history.get(index);
            sb.append("    {\"id\": ").append(jsonString(record.id))
                    .append(", \"completedAt\": ").append(record.completedAt)
                    .append(", \"size\": ").append(record.size)
                    .append(", \"difficulty\": ").append(jsonString(record.difficulty.getId()))
                    .append(", \"moves\": ").append(record.moves)
                    .append(", \"timeMs\": ").append(record.timeMs)
                    .append(", \"assisted\": ").append(record.assisted).append("}");
        }
        sb.append("\n  ],\n  \"stats\": {\n");
        int index = 0;
        for (Map.Entry<String, CompletionStatsMutable> entry : new TreeMap<>(store.stats).entrySet()) {
            if (index++ > 0) {
                sb.append(",\n");
            }
            CompletionStatsMutable stats = entry.getValue();
            sb.append("    ").append(jsonString(entry.getKey())).append(": {\"playerCompletions\": ")
                    .append(stats.playerCompletions).append(", \"assistedCompletions\": ")
                    .append(stats.assistedCompletions).append(", \"playerMoves\": ")
                    .append(stats.playerMoves).append(", \"playerTimeMs\": ")
                    .append(stats.playerTimeMs).append("}");
        }
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    private static CompletionStore completionStoreFromJson(String json) {
        if (json == null || !json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            throw new IllegalArgumentException("Invalid completion store");
        }
        CompletionStore store = new CompletionStore();
        Matcher idsSection = Pattern.compile("\\\"recordedIds\\\"\\s*:\\s*\\[(.*?)\\]",
                Pattern.DOTALL).matcher(json);
        if (idsSection.find()) {
            Matcher id = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(idsSection.group(1));
            while (id.find()) {
                store.recordedIds.add(id.group(1));
            }
        }

        Matcher recordMatcher = Pattern.compile(
                "\\{\\s*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,"
                        + "\\s*\\\"completedAt\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"size\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"difficulty\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,"
                        + "\\s*\\\"moves\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"timeMs\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"assisted\\\"\\s*:\\s*(true|false)\\s*\\}").matcher(json);
        while (recordMatcher.find()) {
            int size = Integer.parseInt(recordMatcher.group(3));
            if (!isSupportedSize(size)) {
                continue;
            }
            CompletionRecord record = new CompletionRecord(recordMatcher.group(1),
                    Long.parseLong(recordMatcher.group(2)), size,
                    PuzzleDifficulty.fromId(recordMatcher.group(4)),
                    Integer.parseInt(recordMatcher.group(5)),
                    Long.parseLong(recordMatcher.group(6)),
                    Boolean.parseBoolean(recordMatcher.group(7)));
            store.history.add(record);
            if (!store.recordedIds.contains(record.id)) {
                store.recordedIds.add(record.id);
            }
        }

        Matcher statsMatcher = Pattern.compile(
                "\\\"(\\d+:(?:relaxed|classic|challenge))\\\"\\s*:\\s*\\{"
                        + "\\s*\\\"playerCompletions\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"assistedCompletions\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"playerMoves\\\"\\s*:\\s*(\\d+)\\s*,"
                        + "\\s*\\\"playerTimeMs\\\"\\s*:\\s*(\\d+)\\s*\\}").matcher(json);
        while (statsMatcher.find()) {
            CompletionStatsMutable stats = new CompletionStatsMutable();
            stats.playerCompletions = Integer.parseInt(statsMatcher.group(2));
            stats.assistedCompletions = Integer.parseInt(statsMatcher.group(3));
            stats.playerMoves = Long.parseLong(statsMatcher.group(4));
            stats.playerTimeMs = Long.parseLong(statsMatcher.group(5));
            store.stats.put(statsMatcher.group(1), stats);
        }
        if (store.stats.isEmpty() && !store.history.isEmpty()) {
            for (CompletionRecord record : store.history) {
                CompletionStatsMutable stats = store.stats.computeIfAbsent(
                        recordScopeKey(record.size, record.difficulty), key -> new CompletionStatsMutable());
                if (record.assisted) {
                    stats.assistedCompletions++;
                } else {
                    stats.playerCompletions++;
                    stats.playerMoves += record.moves;
                    stats.playerTimeMs += record.timeMs;
                }
            }
        }
        return store;
    }

    private static String jsonString(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void ensureParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
    }

    private static void writeTextAtomic(File file, String text) throws IOException {
        ensureParentDirectory(file);
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ATOMIC_TEMP_SUFFIX);
        Path backup = target.resolveSibling(file.getName() + ATOMIC_BACKUP_SUFFIX);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                output.write(text.getBytes(StandardCharsets.UTF_8));
                output.flush();
                output.getFD().sync();
            }
            if (Files.exists(target) && !Files.isDirectory(target)) {
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Returns the desktop user-data directory used for default saves and records.
     *
     * @return directory for desktop user data
     */
    public static File getDataDirectory() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return new File(override);
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return new File(appData, "SlideDo");
        }

        return new File(System.getProperty("user.home"), ".slidedo");
    }

    private static boolean isSolvedGrid(int[][] grid) {
        int size = grid.length;
        int value = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (r == size - 1 && c == size - 1) {
                    if (grid[r][c] != 0) {
                        return false;
                    }
                } else if (grid[r][c] != value++) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isValidGrid(int[][] grid, int size) {
        if (size < 2 || grid == null || grid.length != size) {
            return false;
        }
        boolean[] seen = new boolean[size * size];
        for (int row = 0; row < size; row++) {
            if (grid[row] == null || grid[row].length != size) {
                return false;
            }
            for (int col = 0; col < size; col++) {
                int value = grid[row][col];
                if (value < 0 || value >= seen.length || seen[value]) {
                    return false;
                }
                seen[value] = true;
            }
        }
        return true;
    }

    private static int[][] copyGrid(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }

    private static boolean isSupportedSize(int size) {
        return size >= MIN_SUPPORTED_SIZE && size <= MAX_SUPPORTED_SIZE;
    }

    private static String normalizeDesktopTheme(String theme) {
        if (theme == null) {
            return null;
        }
        String normalized = theme.trim().toLowerCase(Locale.ROOT);
        return "midnight".equals(normalized) || "ocean".equals(normalized)
                ? normalized : null;
    }

    private static String normalizeDesktopLanguage(String languageTag) {
        if (languageTag == null) {
            return null;
        }
        String normalized = languageTag.trim();
        if ("en".equalsIgnoreCase(normalized)) {
            return "en";
        }
        if ("zh-TW".equalsIgnoreCase(normalized)) {
            return "zh-TW";
        }
        if ("ja-JP".equalsIgnoreCase(normalized)) {
            return "ja-JP";
        }
        return null;
    }

    private static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        try {
            return Files.deleteIfExists(file.toPath());
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean deleteAtomicFile(File file) {
        boolean success = deleteFile(file);
        if (file != null) {
            success &= deleteFile(new File(file.getPath() + ATOMIC_TEMP_SUFFIX));
            success &= deleteFile(new File(file.getPath() + ATOMIC_BACKUP_SUFFIX));
        }
        return success;
    }

    /**
     * Immutable summary of one independent normal save slot.
     */
    public static final class SaveMetadata {
        /** Last durable write time in milliseconds since the epoch. */
        public final long updatedAt;

        /** Board width and height. */
        public final int size;

        /** Counted actions in the saved run. */
        public final int moves;

        /** Active-play elapsed milliseconds. */
        public final long elapsedMs;

        /** Whether the saved puzzle can still be played. */
        public final boolean active;

        /** Whether the saved puzzle has been solved. */
        public final boolean solved;

        /** Scramble preset retained by the saved puzzle. */
        public final PuzzleDifficulty difficulty;

        /** Whether the saved run is ineligible for a player best. */
        public final boolean assisted;

        private SaveMetadata(SaveData data) {
            updatedAt = data.updatedAt;
            size = data.size;
            moves = data.moveCount;
            elapsedMs = data.elapsedTime;
            active = data.active;
            solved = data.solved;
            difficulty = data.difficulty == null ? PuzzleDifficulty.CLASSIC : data.difficulty;
            assisted = data.assisted;
        }
    }

    private static final class DailyProgressState {
        private final Set<String> completedDates = new java.util.TreeSet<>();
        private LocalDate lastCompletedDate;
        private int currentStreak;
        private int bestStreak;
    }

    /**
     * Immutable daily completion and streak state for a selected date.
     */
    public static final class DailyProgress {
        /** Whether the selected daily date has a recorded completion. */
        public final boolean completed;

        /** Current streak when the latest completion is current for this date. */
        public final int currentStreak;

        /** Highest recorded consecutive completion streak. */
        public final int bestStreak;

        /** Latest completion date, or {@code null} when none exists. */
        public final String lastCompletedDateId;

        /**
         * Creates immutable progress for a selected date.
         *
         * @param completed whether the date is complete
         * @param currentStreak current streak value
         * @param bestStreak highest streak value
         * @param lastCompletedDateId latest completion date or {@code null}
         */
        public DailyProgress(boolean completed, int currentStreak, int bestStreak,
                String lastCompletedDateId) {
            this.completed = completed;
            this.currentStreak = Math.max(0, currentStreak);
            this.bestStreak = Math.max(0, bestStreak);
            this.lastCompletedDateId = lastCompletedDateId;
        }

        private static DailyProgress empty() {
            return new DailyProgress(false, 0, 0, null);
        }
    }

    /**
     * Restored Continuous Challenge aggregate and its isolated current puzzle.
     */
    public static final class ContinuousGame {
        /** Current puzzle save data. */
        public final SaveData game;

        /** Aggregate progress for the fixed-scope session. */
        public final ContinuousChallenge challenge;

        /** Board size fixed for the session. */
        public final int size;

        /** Difficulty fixed for the session. */
        public final PuzzleDifficulty difficulty;

        /** Whether the current puzzle has an active assistance marker. */
        public final boolean assisted;

        private ContinuousGame(SaveData game, ContinuousChallenge challenge,
                int size, PuzzleDifficulty difficulty, boolean assisted) {
            this.game = game;
            this.challenge = challenge;
            this.size = size;
            this.difficulty = difficulty;
            this.assisted = assisted;
        }
    }

    /**
     * Immutable owner-labeled entry in the local exact-puzzle favorite library.
     */
    public static final class FavoritePuzzle {
        /** Stable SHA-256 identity of size, difficulty, and initial grid. */
        public final String id;

        /** Owner-provided display label. */
        public final String label;

        /** Creation timestamp retained when a favorite is renamed. */
        public final long createdAt;

        /** Board size in the identity. */
        public final int size;

        /** Difficulty in the identity. */
        public final PuzzleDifficulty difficulty;

        private final int[][] initialGrid;

        private FavoritePuzzle(PuzzleIdentity identity, String label, long createdAt) {
            this.id = identity.getId();
            this.label = label;
            this.createdAt = createdAt;
            this.size = identity.getSize();
            this.difficulty = identity.getDifficulty();
            this.initialGrid = identity.getInitialGridCopy();
        }

        /**
         * Returns a defensive copy of the exact favorite starting grid.
         *
         * @return copied initial grid
         */
        public int[][] getInitialGridCopy() {
            return copyGrid(initialGrid);
        }

        /**
         * Creates a fresh zero-move model for isolated favorite practice.
         *
         * @return new model at the exact favorite starting board
         */
        public GameModel createGame() {
            return new PuzzleIdentity(size, difficulty, initialGrid).createGame();
        }
    }

    private static final class PersonalPreferences {
        private int weeklyGoalTarget = DEFAULT_WEEKLY_GOAL_TARGET;
        private int trendSize = 4;
        private PuzzleDifficulty trendDifficulty = PuzzleDifficulty.CLASSIC;
        private boolean reducedMotion;
        private boolean soundEnabled;
        private String theme = "midnight";
        private String languageTag = "en";
        private boolean onboardingSeen;
    }

    /**
     * Serializable save payload shared by the JSON and legacy save paths.
     */
    public static class SaveData implements Serializable {
        private static final long serialVersionUID = 2L;

        /**
         * Creates an empty save payload.
         */
        public SaveData() {
        }

        /** Current board values. */
        public int[][] grid;

        /** Board values at the start of the current puzzle, used by restart. */
        public int[][] initialGrid;

        /** Square board size. */
        public int size;

        /** Counted user moves. */
        public int moveCount;

        /** Original timer start timestamp retained for legacy save compatibility. */
        public long startTime;

        /** Elapsed play time in milliseconds. */
        public long elapsedTime;

        /** Last save/update timestamp in milliseconds since epoch. */
        public long updatedAt;

        /** Whether the persisted puzzle is still active. */
        public boolean active;

        /** Whether the persisted puzzle is solved. */
        public boolean solved;

        /**
         * Whether solver or strategic assistance was used in this run.
         * Missing values in unsolved legacy normal saves default to
         * {@code false}; solved legacy normal saves without provenance fail
         * closed as assisted by the normal-save loader.
         */
        public boolean assisted;

        /**
         * Whether the source JSON explicitly carried the additive assisted
         * field. This is transient provenance used only during migration.
         */
        public transient boolean assistedMetadataPresent;

        /** Scramble-intensity preset, defaulting to Classic for legacy saves. */
        public PuzzleDifficulty difficulty;

        /** Oldest-first completed action history in compact core format. */
        public String actionHistory = "";

        /** Next-redo-first undone action history in compact core format. */
        public String redoHistory = "";
    }

    private static final class CompletionStore {
        private final List<CompletionRecord> history = new ArrayList<>();
        private final Map<String, CompletionStatsMutable> stats = new HashMap<>();
        private final List<String> recordedIds = new ArrayList<>();

        private boolean hasData() {
            return !history.isEmpty() || !stats.isEmpty() || !recordedIds.isEmpty();
        }
    }

    private static final class CompletionStatsMutable {
        private int playerCompletions;
        private int assistedCompletions;
        private long playerMoves;
        private long playerTimeMs;

        private void add(CompletionStatsMutable other) {
            playerCompletions += other.playerCompletions;
            assistedCompletions += other.assistedCompletions;
            playerMoves += other.playerMoves;
            playerTimeMs += other.playerTimeMs;
        }

        private CompletionStats toImmutable() {
            return new CompletionStats(playerCompletions, assistedCompletions,
                    playerMoves, playerTimeMs);
        }
    }

    /**
     * Immutable completion-history sample retained by the Desktop personal store.
     */
    public static final class CompletionRecord {
        /** Unique id used to make repeated win callbacks idempotent. */
        public final String id;

        /** Completion timestamp in milliseconds since the epoch. */
        public final long completedAt;

        /** Board size of the completed puzzle. */
        public final int size;

        /** Difficulty scope of the completed puzzle. */
        public final PuzzleDifficulty difficulty;

        /** Final move count. */
        public final int moves;

        /** Active elapsed milliseconds. */
        public final long timeMs;

        /** Whether the result used solver or strategic assistance. */
        public final boolean assisted;

        private CompletionRecord(String id, long completedAt, int size,
                PuzzleDifficulty difficulty, int moves, long timeMs, boolean assisted) {
            this.id = id;
            this.completedAt = completedAt;
            this.size = size;
            this.difficulty = normalizeDifficulty(difficulty);
            this.moves = moves;
            this.timeMs = timeMs;
            this.assisted = assisted;
        }
    }

    /**
     * Immutable lifetime completion totals for one scope or the aggregate store.
     */
    public static final class CompletionStats {
        /** Number of eligible player completions. */
        public final int playerCompletions;

        /** Number of solver/strategic-assisted completions. */
        public final int assistedCompletions;

        /** Sum of moves across eligible player completions. */
        public final long playerMoves;

        /** Sum of elapsed milliseconds across eligible player completions. */
        public final long playerTimeMs;

        /**
         * Creates immutable totals for a scope or aggregate view.
         *
         * @param playerCompletions eligible player completion count
         * @param assistedCompletions assisted completion count
         * @param playerMoves sum of moves for player completions
         * @param playerTimeMs sum of elapsed milliseconds for player completions
         */
        public CompletionStats(int playerCompletions, int assistedCompletions,
                long playerMoves, long playerTimeMs) {
            this.playerCompletions = playerCompletions;
            this.assistedCompletions = assistedCompletions;
            this.playerMoves = playerMoves;
            this.playerTimeMs = playerTimeMs;
        }

        private static CompletionStats empty() {
            return new CompletionStats(0, 0, 0, 0);
        }
    }

    /**
     * Immutable best-record value object.
     */
    public static class BestRecord {
        /** Lowest move count for this record. */
        public final int moves;

        /** Fastest elapsed time for this move count, in milliseconds. */
        public final long timeMs;

        /**
         * Creates a best-record value.
         *
         * @param moves number of moves used to solve the puzzle
         * @param timeMs elapsed time in milliseconds
         */
        public BestRecord(int moves, long timeMs) {
            this.moves = moves;
            this.timeMs = timeMs;
        }

        /**
         * Compares records by move count first, then elapsed time.
         *
         * @param other existing record to compare against
         * @return {@code true} when this record is better
         */
        public boolean isBetterThan(BestRecord other) {
            return moves < other.moves || (moves == other.moves && timeMs < other.timeMs);
        }

        /**
         * Formats the record for compact UI display.
         *
         * @return text such as {@code "42 moves, 80s"}
         */
        public String format() {
            return moves + " moves, " + (timeMs / 1000) + "s";
        }
    }
}
