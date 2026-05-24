package com.klotski.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists desktop game state and best records.
 * <p>
 * New saves use a small JSON format so the data remains portable across Java
 * desktop and Android. The loader also accepts the legacy serialized
 * {@code klotski_save.dat} file to avoid breaking older local saves.
 * </p>
 */
public class SaveManager {
    private static final String SAVE_FILE = "klotski_save.json";
    private static final String LEGACY_SAVE_FILE = "klotski_save.dat";
    private static final String RECORDS_FILE = "klotski_records.json";

    private SaveManager() {
    }

    /**
     * Writes the current game state to the default desktop save file.
     *
     * @param model game model to persist
     * @return {@code true} when the save file was written successfully
     */
    public static boolean saveGame(GameModel model) {
        return saveGame(model, new File(SAVE_FILE));
    }

    /**
     * Writes the current game state to a caller-supplied save file.
     *
     * @param model game model to persist
     * @param saveFile target JSON save file
     * @return {@code true} when the save file was written successfully
     */
    static boolean saveGame(GameModel model, File saveFile) {
        SaveData data = new SaveData();
        data.grid = model.getGridCopy();
        data.initialGrid = model.getInitialGridCopy();
        data.size = model.getSize();
        data.moveCount = model.getMoveCount();
        data.startTime = model.getStartTime();
        data.elapsedTime = model.getElapsedTime();

        try {
            writeText(saveFile, toJson(data));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the default desktop save file.
     *
     * @return parsed save data, or {@code null} when no valid save exists
     */
    public static SaveData loadGame() {
        return loadGame(new File(SAVE_FILE), new File(LEGACY_SAVE_FILE));
    }

    /**
     * Loads save data from caller-supplied JSON and legacy fallback files.
     *
     * @param saveFile primary JSON save file
     * @param legacySaveFile legacy serialized fallback file
     * @return parsed save data, or {@code null} when no valid save exists
     */
    static SaveData loadGame(File saveFile, File legacySaveFile) {
        if (saveFile.exists()) {
            try {
                return fromJson(readText(saveFile));
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            }
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

    private static void writeText(File file, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
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
        return recordBest(new File(RECORDS_FILE), size, moves, timeMs);
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
        Map<Integer, BestRecord> records = loadRecords(recordsFile);
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
        return getBestRecord(new File(RECORDS_FILE), size);
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
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            SaveData data = (SaveData) ois.readObject();
            if (data.initialGrid == null) {
                data.initialGrid = data.grid;
            }
            return data;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String toJson(SaveData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"size\": ").append(data.size).append(",\n");
        sb.append("  \"moveCount\": ").append(data.moveCount).append(",\n");
        sb.append("  \"elapsedTime\": ").append(data.elapsedTime).append(",\n");
        sb.append("  \"grid\": ").append(gridToJson(data.grid)).append(",\n");
        sb.append("  \"initialGrid\": ").append(gridToJson(data.initialGrid)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static SaveData fromJson(String json) {
        SaveData data = new SaveData();
        data.size = intField(json, "size");
        data.moveCount = intField(json, "moveCount");
        data.elapsedTime = longField(json, "elapsedTime");
        data.grid = gridField(json, "grid", data.size);
        data.initialGrid = gridField(json, "initialGrid", data.size);
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

    private static int[][] gridField(String json, String key, int size) {
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
            writeText(file, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
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
