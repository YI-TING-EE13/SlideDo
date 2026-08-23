package com.klotski.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Versioned JSON codec for owner-controlled Android personal-data backups.
 *
 * <p>The codec accepts only SharedPreferences-compatible scalar and string-set
 * values. Decoding validates the entire document before callers replace any
 * stored state.</p>
 */
final class AndroidPersonalDataArchive {
    static final int FORMAT_VERSION = 1;
    static final int MAX_ARCHIVE_CHARS = 1_000_000;

    private static final String FORMAT_NAME = "slidedo-personal-data";
    private static final int MAX_ENTRIES = 1_000;
    private static final int MAX_KEY_CHARS = 256;

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
            return values;
        } catch (JSONException exception) {
            throw invalidArchive("Backup JSON is malformed.", exception);
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
        return switch (entry.getString("type")) {
            case "string" -> entry.getString("value");
            case "int" -> entry.getInt("value");
            case "long" -> entry.getLong("value");
            case "float" -> parseFloat(entry.getString("value"));
            case "boolean" -> entry.getBoolean("value");
            case "string-set" -> decodeStringSet(entry.getJSONArray("value"));
            default -> throw invalidArchive("Backup contains an unsupported value type.");
        };
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
            if (!result.add(values.getString(index))) {
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
