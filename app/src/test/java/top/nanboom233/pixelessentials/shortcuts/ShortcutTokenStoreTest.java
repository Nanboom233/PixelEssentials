package top.nanboom233.pixelessentials.shortcuts;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class ShortcutTokenStoreTest {
    @Test
    public void getOrCreateToken_persistsAndReusesToken() {
        FakeSharedPreferences sharedPreferences = new FakeSharedPreferences();
        ShortcutTokenStore store = new ShortcutTokenStore(null, sharedPreferences);

        String first = store.getOrCreateToken();
        String second = store.getOrCreateToken();

        assertNotNull(first);
        assertEquals(first, second);
        assertEquals(first, store.peekToken());
    }

    private static final class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return Collections.unmodifiableMap(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof Set ? (Set<String>) value : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private final class FakeEditor implements Editor {
            @Override
            public Editor putString(String key, String value) {
                values.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                FakeSharedPreferences.this.values.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                values.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                values.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                values.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                values.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                values.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                values.clear();
                return this;
            }

            @Override
            public boolean commit() {
                return true;
            }

            @Override
            public void apply() {
            }
        }
    }
}
