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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists desktop game state and best records.
 * <p>
 * New saves use one small JSON slot per supported board size, including
 * completed and redo action histories, so the data remains portable across
 * Java desktop and Android. Replacements are atomic and leave recoverable
 * siblings when the platform cannot complete a write. The loader also accepts
 * the legacy single JSON and serialized {@code klotski_save.dat} files without
 * deleting them or overwriting a newer size slot.
 * </p>
 */
public class SaveManager {
    /** Optional JVM property used by tests and portable desktop packages. */
    public static final String DATA_DIR_PROPERTY = "slidedo.data.dir";

    private static final int CURRENT_SAVE_VERSION = 3;
    private static final int MIN_SUPPORTED_SIZE = 3;
    private static final int MAX_SUPPORTED_SIZE = 5;
    private static final String SAVE_FILE_PREFIX = "klotski_save_";
    private static final String SAVE_FILE_SUFFIX = ".json";
    private static final String SAVE_FILE = "klotski_save.json";
    private static final String LEGACY_SAVE_FILE = "klotski_save.dat";
    private static final String RECORDS_FILE = "klotski_records.json";
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
        if (model == null || !isSupportedSize(model.getSize())) {
            return false;
        }
        return saveGame(model, getSaveFile(model.getSize()));
    }

    /**
     * Persists an active game at a lifecycle boundary without changing the
     * manual Save command's semantics.
     *
     * @param model game model to persist
     * @return {@code true} when the autosave slot was written successfully
     */
    public static boolean autosaveGame(GameModel model) {
        return saveGame(model);
    }

    /**
     * Writes the current game state to a caller-supplied save file.
     *
     * @param model game model to persist
     * @param saveFile target JSON save file
     * @return {@code true} when the save file was written successfully
     */
    static boolean saveGame(GameModel model, File saveFile) {
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
        migrateLegacySaves();
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
        return loadLegacyFallback();
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
        migrateLegacySaves();
        return loadSlot(size);
    }

    /**
     * Returns metadata for every valid normal save, ordered by board size.
     *
     * @return independent save summaries for the Home Continue chooser
     */
    public static SaveMetadata[] getAllSaveMetadata() {
        migrateLegacySaves();
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
     * Loads save data from caller-supplied JSON and legacy fallback files.
     *
     * @param saveFile primary JSON save file
     * @param legacySaveFile legacy serialized fallback file
     * @return parsed save data, or {@code null} when no valid save exists
     */
    static SaveData loadGame(File saveFile, File legacySaveFile) {
        SaveData data = readJsonWithRecovery(saveFile);
        if (data != null) {
            return data;
        }
        return loadLegacyGame(legacySaveFile);
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
        File recordsFile = new File(getDataDirectory(), RECORDS_FILE);
        if (!recordsFile.exists() && new File(RECORDS_FILE).exists()) {
            return recordBest(recordsFile, new File(RECORDS_FILE), size, moves, timeMs);
        }
        return recordBest(recordsFile, size, moves, timeMs);
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
        BestRecord record = getBestRecord(new File(getDataDirectory(), RECORDS_FILE), size);
        return record != null ? record : getBestRecord(new File(RECORDS_FILE), size);
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

    private static void migrateLegacySaves() {
        File dataDirectory = getDataDirectory();
        for (File legacyFile : legacyCandidates(dataDirectory)) {
            SaveData legacy = legacyFile.getName().endsWith(".dat")
                    ? loadLegacyGame(legacyFile)
                    : readJsonWithRecovery(legacyFile);
            if (legacy == null || !isSupportedSize(legacy.size)) {
                continue;
            }

            File target = getSaveFile(legacy.size);
            SaveData current = readJsonWithRecovery(target);
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
                    ? loadLegacyGame(legacyFile)
                    : readJsonWithRecovery(legacyFile);
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
        SaveData data = readJsonWithRecovery(getSaveFile(size));
        if (data != null && data.size == size) {
            return data;
        }
        File rootSlot = new File(saveFileName(size));
        if (!sameFile(rootSlot, getSaveFile(size))) {
            data = readJsonWithRecovery(rootSlot);
        }
        return data != null && data.size == size ? data : null;
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

    private static boolean isSupportedSize(int size) {
        return size >= MIN_SUPPORTED_SIZE && size <= MAX_SUPPORTED_SIZE;
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

        private SaveMetadata(SaveData data) {
            updatedAt = data.updatedAt;
            size = data.size;
            moves = data.moveCount;
            elapsedMs = data.elapsedTime;
            active = data.active;
            solved = data.solved;
            difficulty = data.difficulty == null ? PuzzleDifficulty.CLASSIC : data.difficulty;
        }
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

        /** Scramble-intensity preset, defaulting to Classic for legacy saves. */
        public PuzzleDifficulty difficulty;

        /** Oldest-first completed action history in compact core format. */
        public String actionHistory = "";

        /** Next-redo-first undone action history in compact core format. */
        public String redoHistory = "";
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
