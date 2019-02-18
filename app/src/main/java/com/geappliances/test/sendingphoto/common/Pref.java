package com.geappliances.test.sendingphoto.common;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

/**
 * @file Pref.java
 * @date 18/02/2019
 * @brief
 * @copyright GE Appliances, a Haier Company (Confidential). All rights reserved.
 */

public class Pref {
    private static SharedPreferences mPrefs;

    public static void initPrefs(Context context) {
        if (mPrefs == null) {
            String key = context.getPackageName();
            if (key == null) {
                throw new NullPointerException("Prefs key may not be null");
            }
            mPrefs = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        }
    }

    public static SharedPreferences getPreferences() {
        if (mPrefs != null) {
            return mPrefs;
        }
        throw new RuntimeException("Prefs class not correctly instantiated please call Prefs.iniPrefs(context) in the Application class onCreate.");
    }

    public static Map<String, ?> getAll() {
        return getPreferences().getAll();
    }

    public static int getInt(final String key, final int defValue) {
        return getPreferences().getInt(key, defValue);
    }

    public static int getInt(final String key) {
        return getPreferences().getInt(key, -1);
    }

    public static boolean getBoolean(final String key, final boolean defValue) {
        return getPreferences().getBoolean(key, defValue);
    }

    public static boolean getBoolean(final String key) {
        return getPreferences().getBoolean(key, false);
    }

    public static long getLong(final String key, final long defValue) {
        return getPreferences().getLong(key, defValue);
    }

    public static long getLong(final String key) {
        return getPreferences().getLong(key, -1);
    }

    public static float getFloat(final String key, final float defValue) {
        return getPreferences().getFloat(key, defValue);
    }

    public static float getFloat(final String key) {
        return getPreferences().getFloat(key, -1);
    }

    public static String getString(final String key, final String defValue) {
        return getPreferences().getString(key, defValue);
    }

    public static String getString(final String key) {
        return getPreferences().getString(key, null);
    }

    public static Set<String> getStringSet(final String key, final Set<String> defValue) {
        SharedPreferences prefs = getPreferences();
        return prefs.getStringSet(key, defValue);

    }

    public static void putLong(final String key, final long value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putLong(key, value);

        editor.apply();
    }

    public static void putInt(final String key, final int value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void putFloat(final String key, final float value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public static void putBoolean(final String key, final boolean value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void putString(final String key, final String value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void put(final String key, final Object value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof String) {
            editor.putString(key, String.valueOf(value));
        } else if (value instanceof Set<?>) {
            editor.putStringSet(key, (Set<String>) value);
        }
        editor.apply();
    }

    public static void putStringSet(final String key, final Set<String> value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    public static void remove(final String key) {
        SharedPreferences prefs = getPreferences();
        final SharedPreferences.Editor editor = prefs.edit();
        if (prefs.contains(key + "#LENGTH")) {
            int stringSetLength = prefs.getInt(key + "#LENGTH", -1);
            if (stringSetLength >= 0) {
                editor.remove(key + "#LENGTH");
                for (int i = 0; i < stringSetLength; i++) {
                    editor.remove(key + "[" + i + "]");
                }
            }
        }
        editor.remove(key);

        editor.apply();
    }

    public static boolean contains(final String key) {
        return getPreferences().contains(key);
    }

}
