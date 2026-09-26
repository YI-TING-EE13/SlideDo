package com.klotski.android;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Deep copy of SharedPreferences values for recovery after a failed commit.
 *
 * <p>SharedPreferences commits update their in-process view before reporting
 * whether the disk write succeeded. Restoring a snapshot therefore recovers
 * values visible to the current process. A false rollback commit still leaves
 * durable recovery unconfirmed.</p>
 */
final class AndroidSharedPreferencesSnapshot {
    private final Map<String, Object> values;
    private final Set<String> keys;

    private AndroidSharedPreferencesSnapshot(Map<String, Object> values, Set<String> keys) {
        this.values = values;
        this.keys = keys;
    }

    static AndroidSharedPreferencesSnapshot captureAll(SharedPreferences preferences) {
        return new AndroidSharedPreferencesSnapshot(copyValues(preferences.getAll()), null);
    }

    static AndroidSharedPreferencesSnapshot captureKeys(
            SharedPreferences preferences, Set<String> keys) {
        Map<String, ?> current = preferences.getAll();
        Map<String, Object> selected = new HashMap<>();
        for (String key : keys) {
            if (current.containsKey(key)) {
                selected.put(key, copyValue(current.get(key)));
            }
        }
        return new AndroidSharedPreferencesSnapshot(selected, new HashSet<>(keys));
    }

    boolean restore(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        if (keys == null) {
            editor.clear();
        } else {
            for (String key : keys) {
                editor.remove(key);
            }
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            putPreference(editor, entry.getKey(), entry.getValue());
        }
        boolean persisted = editor.commit();
        return persisted && matches(preferences);
    }

    boolean matches(SharedPreferences preferences) {
        Map<String, ?> current = preferences.getAll();
        if (keys == null) {
            return copyValues(current).equals(values);
        }
        for (String key : keys) {
            if (values.containsKey(key)) {
                if (!values.get(key).equals(copyValue(current.get(key)))) {
                    return false;
                }
            } else if (current.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> copyValues(Map<String, ?> source) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            copy.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return copy;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Set<?> values) {
            Set<String> copy = new HashSet<>();
            for (Object item : values) {
                if (!(item instanceof String stringValue)) {
                    throw new IllegalArgumentException(
                            "SharedPreferences string set contains a non-string value.");
                }
                copy.add(stringValue);
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static void putPreference(SharedPreferences.Editor editor, String key, Object value) {
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
            throw new IllegalArgumentException("Unsupported SharedPreferences value type.");
        }
    }
}
