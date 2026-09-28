package com.klotski.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.klotski.core.ContinuousChallenge;
import com.klotski.core.GameModel;
import com.klotski.core.PuzzleDifficulty;
import com.klotski.core.PuzzleSolvability;
import com.klotski.core.RelayChallengeSpec;
import com.klotski.core.RelayCodeCodec;
import com.klotski.core.SaveManager;
import com.klotski.core.WeeklyGoalProgress;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versioned JSON codec for owner-controlled Android personal-data backups.
 *
 * <p>The codec accepts only SharedPreferences-compatible scalar and string-set
 * values. Decoding validates the entire document before callers replace any
 * stored state. This is a local-device handoff format, not a Desktop archive
 * or a cloud-synchronization protocol.</p>
 */
final class AndroidPersonalDataArchive {
    static final int FORMAT_VERSION = 1;
    static final int MAX_ARCHIVE_CHARS = 1_000_000;

    private static final String FORMAT_NAME = "slidedo-personal-data";
    private static final int MAX_ENTRIES = 1_000;
    private static final int MAX_KEY_CHARS = 256;
    private static final Pattern SIZED_SAVE_FIELD = Pattern.compile(
            "^(save_([345])_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted)$");
    private static final Pattern DAILY_SAVE_FIELD = Pattern.compile(
            "^(daily_save_v2_(\\d{4}-\\d{2}-\\d{2})_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted)$");
    private static final Pattern FAVORITE_RUN_FIELD = Pattern.compile(
            "^(favorite_run_v1_([0-9a-f]{64})_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted)$");
    private static final Pattern LEGACY_DAILY_SAVE_FIELD = Pattern.compile(
            "^(daily_save_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted)$");
    private static final Pattern CONTINUOUS_SAVE_FIELD = Pattern.compile(
            "^(continuous_save_v1_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted|challenge_target|challenge_completed|challenge_total_moves|challenge_total_time|challenge_assisted)$");
    private static final Pattern RELAY_SAVE_FIELD = Pattern.compile(
            "^(relay_save_v1_)(size|grid|initial_grid|moves|elapsed|updated_at|active|solved|difficulty|action_history|redo_history|assisted|code)$");
    private static final Pattern BEST_FIELD = Pattern.compile(
            "^best_([345])_(?:(relaxed|classic|challenge)_)?(moves|time)$");
    private static final Pattern STATS_FIELD = Pattern.compile(
            "^stats_([345])_(relaxed|classic|challenge)_(player_completions|assisted_completions|player_moves|player_time)$");

    private AndroidPersonalDataArchive() {
    }

    static String encode(Map<String, ?> values, long createdAt) {
        try {
            JSONObject root = new JSONObject();
            root.put("format", FORMAT_NAME);
            root.put("version", FORMAT_VERSION);
            root.put("createdAt", Math.max(0L, createdAt));
            JSONArray entries = new JSONArray();
            for (Map.Entry<String, ?> entry : new TreeMap<>(values).entrySet()) {
                entries.put(encodeEntry(entry.getKey(), entry.getValue()));
            }
            root.put("entries", entries);
            String encoded = root.toString();
            if (encoded.length() > MAX_ARCHIVE_CHARS) {
                throw invalidArchive("Backup exceeds the supported size.");
            }
            return encoded;
        } catch (JSONException exception) {
            throw invalidArchive("Backup could not be encoded.", exception);
        }
    }

    static Map<String, Object> decode(String archive) {
        if (archive == null || archive.isBlank() || archive.length() > MAX_ARCHIVE_CHARS) {
            throw invalidArchive("Backup is empty or exceeds the supported size.");
        }
        try {
            JSONObject root = new JSONObject(archive);
            if (!FORMAT_NAME.equals(root.optString("format"))) {
                throw invalidArchive("Backup format is not recognized.");
            }
            if (root.optInt("version", -1) != FORMAT_VERSION) {
                throw invalidArchive("Backup version is not supported.");
            }
            JSONArray entries = root.getJSONArray("entries");
            if (entries.length() > MAX_ENTRIES) {
                throw invalidArchive("Backup contains too many entries.");
            }

            Map<String, Object> values = new LinkedHashMap<>();
            for (int index = 0; index < entries.length(); index++) {
                JSONObject entry = entries.getJSONObject(index);
                String key = entry.getString("key");
                if (key.isBlank() || key.length() > MAX_KEY_CHARS || values.containsKey(key)) {
                    throw invalidArchive("Backup contains an invalid or duplicate key.");
                }
                values.put(key, decodeValue(entry));
            }
            validatePreferenceValues(values);
            return values;
        } catch (JSONException exception) {
            throw invalidArchive("Backup JSON is malformed.", exception);
        }
    }

    /**
     * Checks the known SharedPreferences schema before the caller can replace
     * current data. Unknown keys retain the original archive compatibility
     * policy and remain opaque SharedPreferences values.
     */
    private static void validatePreferenceValues(Map<String, Object> values) {
        Map<String, Map<String, Object>> saveGroups = new HashMap<>();
        Map<String, Integer> expectedSizes = new HashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Class<?> expectedType = expectedPreferenceType(key);
            if (expectedType != null && !expectedType.isInstance(value)) {
                throw invalidArchive("Backup value type does not match preference " + key + ".");
            }
            validatePreferenceValue(key, value);

            SaveKey saveKey = findSaveKey(key);
            if (saveKey != null) {
                saveGroups.computeIfAbsent(saveKey.prefix, ignored -> new HashMap<>())
                        .put(saveKey.field, value);
                if (saveKey.expectedSize != 0) {
                    expectedSizes.put(saveKey.prefix, saveKey.expectedSize);
                }
            }
        }
        for (Map.Entry<String, Map<String, Object>> entry : saveGroups.entrySet()) {
            validateSaveGroup(entry.getKey(), entry.getValue(),
                    expectedSizes.getOrDefault(entry.getKey(), 0));
        }
        validateDailyProgress(values);
        validateCompletionStats(values);
        validateContinuousProgress(saveGroups.get("continuous_save_v1_"));
        validateRelaySave(saveGroups.get("relay_save_v1_"));
    }

    private static Class<?> expectedPreferenceType(String key) {
        SaveKey saveKey = findSaveKey(key);
        if (saveKey != null) {
            return saveFieldType(saveKey.field);
        }
        Matcher best = BEST_FIELD.matcher(key);
        if (best.matches()) {
            return "moves".equals(best.group(3)) ? Integer.class : Long.class;
        }
        Matcher stats = STATS_FIELD.matcher(key);
        if (stats.matches()) {
            return stats.group(3).endsWith("completions") ? Integer.class : Long.class;
        }
        return switch (key) {
            case "daily_save_date", "daily_last_completed_date", "last_difficulty",
                    "trend_difficulty_v1", "favorite_puzzles_v1", "completion_history_v1",
                    "visual_theme", "language_tag" -> String.class;
            case "daily_completed_dates_v1" -> Set.class;
            case "daily_current_streak", "daily_best_streak", "last_size",
                    "trend_size_v1", "weekly_goal_target_v1" -> Integer.class;
            case "onboarding_seen", "haptic_enabled", "reduced_motion",
                    "sound_enabled" -> Boolean.class;
            default -> null;
        };
    }

    private static SaveKey findSaveKey(String key) {
        if (isSaveField(key)) {
            return new SaveKey("", key, 0);
        }
        Matcher matcher = SIZED_SAVE_FIELD.matcher(key);
        if (matcher.matches()) {
            return new SaveKey(matcher.group(1), matcher.group(3),
                    Integer.parseInt(matcher.group(2)));
        }
        matcher = DAILY_SAVE_FIELD.matcher(key);
        if (matcher.matches()) {
            requireDate(matcher.group(2), key);
            return new SaveKey(matcher.group(1), matcher.group(3), 4);
        }
        matcher = FAVORITE_RUN_FIELD.matcher(key);
        if (matcher.matches()) {
            return new SaveKey(matcher.group(1), matcher.group(3), 0);
        }
        matcher = LEGACY_DAILY_SAVE_FIELD.matcher(key);
        if (matcher.matches()) {
            return new SaveKey(matcher.group(1), matcher.group(2), 4);
        }
        matcher = CONTINUOUS_SAVE_FIELD.matcher(key);
        if (matcher.matches()) {
            return new SaveKey(matcher.group(1), matcher.group(2), 0);
        }
        matcher = RELAY_SAVE_FIELD.matcher(key);
        if (matcher.matches()) {
            return new SaveKey(matcher.group(1), matcher.group(2), 0);
        }
        return null;
    }

    private static boolean isSaveField(String key) {
        return switch (key) {
            case "size", "grid", "initial_grid", "moves", "elapsed", "updated_at",
                    "active", "solved", "difficulty", "action_history", "redo_history",
                    "assisted" -> true;
            default -> false;
        };
    }

    private static Class<?> saveFieldType(String field) {
        return switch (field) {
            case "size", "moves", "challenge_target", "challenge_completed",
                    "challenge_total_moves", "challenge_assisted" -> Integer.class;
            case "elapsed", "updated_at", "challenge_total_time" -> Long.class;
            case "active", "solved", "assisted" -> Boolean.class;
            default -> String.class;
        };
    }

    private static void validatePreferenceValue(String key, Object value) {
        SaveKey saveKey = findSaveKey(key);
        if (saveKey != null) {
            switch (saveKey.field) {
                case "size" -> {
                    int size = (Integer) value;
                    if (!isSupportedSize(size)
                            || (saveKey.expectedSize != 0 && size != saveKey.expectedSize)) {
                        throw invalidArchive("Backup contains an unsupported board size.");
                    }
                }
                case "moves", "elapsed", "updated_at", "challenge_total_moves",
                        "challenge_total_time" -> requireNonNegative((Number) value, key);
                case "difficulty" -> requireDifficulty((String) value, key);
                case "challenge_target" -> {
                    if (!ContinuousChallenge.isSupportedTarget((Integer) value)) {
                        throw invalidArchive("Backup contains an unsupported challenge target.");
                    }
                }
                case "challenge_completed", "challenge_assisted" -> {
                    if ((Integer) value < 0) {
                        throw invalidArchive("Backup contains a negative challenge count.");
                    }
                }
                default -> { }
            }
            return;
        }

        Matcher best = BEST_FIELD.matcher(key);
        if (best.matches()) {
            requireNonNegative((Number) value, key);
            return;
        }
        Matcher stats = STATS_FIELD.matcher(key);
        if (stats.matches()) {
            requireNonNegative((Number) value, key);
            return;
        }
        switch (key) {
            case "last_size", "trend_size_v1" -> {
                if (!isSupportedSize((Integer) value)) {
                    throw invalidArchive("Backup contains an unsupported board size.");
                }
            }
            case "weekly_goal_target_v1" -> {
                if (!WeeklyGoalProgress.isValidTarget((Integer) value)) {
                    throw invalidArchive("Backup contains an unsupported weekly goal.");
                }
            }
            case "last_difficulty", "trend_difficulty_v1" ->
                    requireDifficulty((String) value, key);
            case "daily_save_date", "daily_last_completed_date" ->
                    requireDate((String) value, key);
            case "visual_theme" -> {
                if (!("midnight".equals(value) || "ocean".equals(value))) {
                    throw invalidArchive("Backup contains an unsupported visual theme.");
                }
            }
            case "language_tag" -> {
                String tag = (String) value;
                if (!("en".equalsIgnoreCase(tag) || "zh-TW".equalsIgnoreCase(tag)
                        || "ja-JP".equalsIgnoreCase(tag))) {
                    throw invalidArchive("Backup contains an unsupported language.");
                }
            }
            case "daily_current_streak", "daily_best_streak" ->
                    requireNonNegative((Number) value, key);
            case "favorite_puzzles_v1" -> {
                if (!AndroidGameStore.isValidFavoriteArchiveValue((String) value)) {
                    throw invalidArchive("Backup contains invalid favorite puzzle data.");
                }
            }
            case "completion_history_v1" -> validateCompletionHistory((String) value);
            case "daily_completed_dates_v1" -> {
                for (Object date : (Set<?>) value) {
                    requireDate((String) date, key);
                }
            }
            default -> { }
        }
    }

    private static void validateSaveGroup(String prefix, Map<String, Object> values,
            int expectedSize) {
        Object sizeValue = values.get("size");
        int size = sizeValue instanceof Integer storedSize ? storedSize
                : expectedSize != 0 ? expectedSize : 4;
        if (expectedSize != 0 && size != expectedSize) {
            throw invalidArchive("Backup save size does not match its key namespace.");
        }
        if (values.containsKey("grid") && !values.containsKey("initial_grid")) {
            throw invalidArchive("Backup saved game is missing its initial board.");
        }
        int[][] grid = validateGrid(values.get("grid"), size, prefix + "grid");
        int[][] initialGrid = validateGrid(values.get("initial_grid"), size,
                prefix + "initial_grid");
        if ((grid != null && !PuzzleSolvability.isSolvable(grid, size))
                || (initialGrid != null && !PuzzleSolvability.isSolvable(initialGrid, size))) {
            throw invalidArchive("Backup contains an unreachable saved board.");
        }
        validateActionHistory(prefix, values, size, grid, initialGrid);
    }

    private static void validateRelaySave(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        String[] required = {"size", "grid", "initial_grid", "moves", "elapsed",
                "updated_at", "active", "solved", "difficulty", "action_history",
                "redo_history", "assisted", "code"};
        for (String field : required) {
            if (!values.containsKey(field)) {
                throw invalidArchive("Backup Relay save is incomplete.");
            }
        }
        int size = (Integer) values.get("size");
        int[][] initialGrid = validateGrid(values.get("initial_grid"), size,
                "relay initial_grid");
        RelayChallengeSpec spec;
        try {
            spec = RelayCodeCodec.decode((String) values.get("code"));
        } catch (RuntimeException exception) {
            throw invalidArchive("Backup Relay code is invalid.", exception);
        }
        PuzzleDifficulty difficulty = PuzzleDifficulty.fromId(
                (String) values.get("difficulty"));
        if (spec.getSize() != size || spec.getDifficulty() != difficulty
                || !java.util.Arrays.deepEquals(spec.getInitialGridCopy(), initialGrid)) {
            throw invalidArchive("Backup Relay save does not match its challenge code.");
        }
    }

    private static void validateActionHistory(String prefix, Map<String, Object> values,
            int size, int[][] grid, int[][] initialGrid) {
        String actionHistory = (String) values.getOrDefault("action_history", "");
        String redoHistory = (String) values.getOrDefault("redo_history", "");
        if (actionHistory.isEmpty() && redoHistory.isEmpty()) {
            return;
        }
        if (grid == null || initialGrid == null) {
            throw invalidArchive("Backup action history is missing its board state.");
        }
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.size = size;
        data.grid = grid;
        data.initialGrid = initialGrid;
        data.moveCount = (Integer) values.getOrDefault("moves", 0);
        data.elapsedTime = (Long) values.getOrDefault("elapsed", 0L);
        data.updatedAt = (Long) values.getOrDefault("updated_at", 0L);
        data.active = (Boolean) values.getOrDefault("active", false);
        data.solved = (Boolean) values.getOrDefault("solved", false);
        data.difficulty = PuzzleDifficulty.fromId((String) values.getOrDefault(
                "difficulty", PuzzleDifficulty.CLASSIC.getId()));
        data.actionHistory = actionHistory;
        data.redoHistory = redoHistory;
        GameModel restored = new GameModel(size);
        try {
            restored.loadState(data);
        } catch (RuntimeException exception) {
            throw invalidArchive("Backup action history is invalid.", exception);
        }
        if (!actionHistory.equals(restored.getEncodedActionHistory())
                || !redoHistory.equals(restored.getEncodedRedoHistory())) {
            throw invalidArchive("Backup action history does not match its saved board.");
        }
    }

    private static int[][] validateGrid(Object encoded, int size, String key) {
        if (encoded == null) {
            return null;
        }
        if (!isSupportedSize(size)) {
            throw invalidArchive("Backup grid has no supported board size.");
        }
        String[] tokens = ((String) encoded).split(",", -1);
        if (tokens.length != size * size) {
            throw invalidArchive("Backup grid dimensions do not match " + key + ".");
        }
        int[][] grid = new int[size][size];
        boolean[] seen = new boolean[tokens.length];
        try {
            for (int index = 0; index < tokens.length; index++) {
                int tile = Integer.parseInt(tokens[index]);
                if (tile < 0 || tile >= seen.length || seen[tile]) {
                    throw invalidArchive("Backup grid must contain each tile exactly once.");
                }
                seen[tile] = true;
                grid[index / size][index % size] = tile;
            }
        } catch (NumberFormatException exception) {
            throw invalidArchive("Backup grid contains an invalid tile.", exception);
        }
        return grid;
    }

    private static void validateDailyProgress(Map<String, Object> values) {
        int current = (Integer) values.getOrDefault("daily_current_streak", 0);
        int best = (Integer) values.getOrDefault("daily_best_streak", 0);
        if (current > best || current == Integer.MAX_VALUE) {
            throw invalidArchive("Backup daily streak values are invalid or out of range.");
        }
    }

    private static void validateCompletionStats(Map<String, Object> values) {
        int playerCompletions = 0;
        int assistedCompletions = 0;
        long playerMoves = 0L;
        long playerTime = 0L;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Matcher stats = STATS_FIELD.matcher(entry.getKey());
            if (!stats.matches()) {
                continue;
            }
            String field = stats.group(3);
            Number value = (Number) entry.getValue();
            try {
                switch (field) {
                    case "player_completions" -> {
                        if (value.intValue() == Integer.MAX_VALUE) {
                            throw invalidArchive(
                                    "Backup player completion counter cannot be incremented.");
                        }
                        playerCompletions = Math.addExact(playerCompletions, value.intValue());
                    }
                    case "assisted_completions" -> {
                        if (value.intValue() == Integer.MAX_VALUE) {
                            throw invalidArchive(
                                    "Backup assisted completion counter cannot be incremented.");
                        }
                        assistedCompletions = Math.addExact(
                                assistedCompletions, value.intValue());
                    }
                    case "player_moves" -> playerMoves = Math.addExact(
                            playerMoves, value.longValue());
                    case "player_time" -> playerTime = Math.addExact(
                            playerTime, value.longValue());
                    default -> throw new IllegalStateException("Unknown completion statistic.");
                }
            } catch (ArithmeticException exception) {
                throw invalidArchive(
                        "Backup aggregate completion statistics exceed supported numeric ranges.",
                        exception);
            }
        }
        if (playerCompletions == Integer.MAX_VALUE
                || assistedCompletions == Integer.MAX_VALUE) {
            throw invalidArchive(
                    "Backup aggregate completion counters cannot be incremented.");
        }
    }

    private static void validateContinuousProgress(Map<String, Object> values) {
        if (values == null) {
            return;
        }
        String[] aggregateFields = {"challenge_target", "challenge_completed",
                "challenge_total_moves", "challenge_total_time", "challenge_assisted"};
        boolean hasAggregate = false;
        for (String field : aggregateFields) {
            hasAggregate |= values.containsKey(field);
        }
        if (!hasAggregate) {
            return;
        }
        for (String field : aggregateFields) {
            if (!values.containsKey(field)) {
                throw invalidArchive("Backup continuous challenge data is incomplete.");
            }
        }
        try {
            ContinuousChallenge.restore((Integer) values.get("challenge_target"),
                    (Integer) values.get("challenge_completed"),
                    (Integer) values.get("challenge_total_moves"),
                    (Long) values.get("challenge_total_time"),
                    (Integer) values.get("challenge_assisted"));
        } catch (RuntimeException exception) {
            throw invalidArchive("Backup continuous challenge data is invalid.", exception);
        }
    }

    private static void validateCompletionHistory(String encoded) {
        if (encoded.isEmpty()) {
            return;
        }
        String[] rows = encoded.split("\\n", -1);
        if (rows.length > 50) {
            throw invalidArchive("Backup completion history exceeds its supported size.");
        }
        for (String row : rows) {
            String[] fields = row.split(",", -1);
            if (fields.length != 6 || !("0".equals(fields[5]) || "1".equals(fields[5]))) {
                throw invalidArchive("Backup completion history contains an invalid row.");
            }
            try {
                long completedAt = Long.parseLong(fields[0]);
                int size = Integer.parseInt(fields[1]);
                int moves = Integer.parseInt(fields[3]);
                long timeMs = Long.parseLong(fields[4]);
                if (completedAt < 0 || !isSupportedSize(size) || moves < 0 || timeMs < 0) {
                    throw invalidArchive("Backup completion history contains invalid metrics.");
                }
                requireDifficulty(fields[2], "completion_history_v1");
            } catch (NumberFormatException exception) {
                throw invalidArchive("Backup completion history contains invalid metrics.", exception);
            }
        }
    }

    private static void requireDifficulty(String id, String key) {
        for (PuzzleDifficulty difficulty : PuzzleDifficulty.values()) {
            if (difficulty.getId().equals(id)) {
                return;
            }
        }
        throw invalidArchive("Backup contains an unsupported difficulty for " + key + ".");
    }

    private static void requireDate(String date, String key) {
        try {
            if (!LocalDate.parse(date).toString().equals(date)) {
                throw invalidArchive("Backup contains an invalid date for " + key + ".");
            }
        } catch (RuntimeException exception) {
            throw invalidArchive("Backup contains an invalid date for " + key + ".", exception);
        }
    }

    private static void requireNonNegative(Number value, String key) {
        if (value.longValue() < 0) {
            throw invalidArchive("Backup contains a negative value for " + key + ".");
        }
    }

    private static boolean isSupportedSize(int size) {
        return size >= 3 && size <= 5;
    }

    private static final class SaveKey {
        final String prefix;
        final String field;
        final int expectedSize;

        SaveKey(String prefix, String field, int expectedSize) {
            this.prefix = prefix;
            this.field = field;
            this.expectedSize = expectedSize;
        }
    }

    private static JSONObject encodeEntry(String key, Object value) throws JSONException {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_CHARS) {
            throw invalidArchive("Preference key is not valid for backup.");
        }
        JSONObject entry = new JSONObject();
        entry.put("key", key);
        if (value instanceof String stringValue) {
            entry.put("type", "string");
            entry.put("value", stringValue);
        } else if (value instanceof Integer integerValue) {
            entry.put("type", "int");
            entry.put("value", integerValue);
        } else if (value instanceof Long longValue) {
            entry.put("type", "long");
            entry.put("value", longValue);
        } else if (value instanceof Float floatValue) {
            entry.put("type", "float");
            entry.put("value", Float.toString(floatValue));
        } else if (value instanceof Boolean booleanValue) {
            entry.put("type", "boolean");
            entry.put("value", booleanValue);
        } else if (value instanceof Set<?> setValue) {
            entry.put("type", "string-set");
            JSONArray strings = new JSONArray();
            List<String> sorted = new ArrayList<>();
            for (Object item : setValue) {
                if (!(item instanceof String stringItem)) {
                    throw invalidArchive("Backup contains a non-string set value.");
                }
                sorted.add(stringItem);
            }
            sorted.sort(String::compareTo);
            for (String item : sorted) {
                strings.put(item);
            }
            entry.put("value", strings);
        } else {
            throw invalidArchive("Backup contains an unsupported preference type.");
        }
        return entry;
    }

    private static Object decodeValue(JSONObject entry) throws JSONException {
        String type = entry.getString("type");
        Object rawValue = entry.get("value");
        return switch (type) {
            case "string" -> requireJsonType(rawValue, String.class, "string");
            case "int" -> decodeInteger(rawValue);
            case "long" -> decodeLong(rawValue);
            case "float" -> parseFloat((String) requireJsonType(
                    rawValue, String.class, "float"));
            case "boolean" -> requireJsonType(rawValue, Boolean.class, "boolean");
            case "string-set" -> decodeStringSet((JSONArray) requireJsonType(
                    rawValue, JSONArray.class, "string-set"));
            default -> throw invalidArchive("Backup contains an unsupported value type.");
        };
    }

    private static Object requireJsonType(Object value, Class<?> type, String label) {
        if (!type.isInstance(value)) {
            throw invalidArchive("Backup contains an invalid " + label + " value.");
        }
        return value;
    }

    private static Integer decodeInteger(Object value) {
        long parsed = exactInteger(value, "int");
        if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
            throw invalidArchive("Backup contains an out-of-range int value.");
        }
        return (int) parsed;
    }

    private static Long decodeLong(Object value) {
        return exactInteger(value, "long");
    }

    private static long exactInteger(Object value, String label) {
        if (!(value instanceof Number number)
                || !Double.isFinite(number.doubleValue())
                || number.doubleValue() != number.longValue()) {
            throw invalidArchive("Backup contains an invalid " + label + " value.");
        }
        return number.longValue();
    }

    private static Float parseFloat(String value) {
        try {
            float parsed = Float.parseFloat(value);
            if (!Float.isFinite(parsed)) {
                throw invalidArchive("Backup contains a non-finite float.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalidArchive("Backup contains an invalid float.", exception);
        }
    }

    private static Set<String> decodeStringSet(JSONArray values) throws JSONException {
        Set<String> result = new HashSet<>();
        for (int index = 0; index < values.length(); index++) {
            String value = (String) requireJsonType(values.get(index), String.class,
                    "string-set item");
            if (!result.add(value)) {
                throw invalidArchive("Backup contains a duplicate string-set value.");
            }
        }
        return result;
    }

    private static IllegalArgumentException invalidArchive(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalidArchive(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }
}
