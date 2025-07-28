package com.example.driverdrowsinessdetectionsystem;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferences {
    private static final String PREF_NAME = "my_preferences";
    private static final String FIRST_TIME_KEY = "first_time";
    private static final String IS_LOGGED_IN = "is_logged_in";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_NAME = "user_name";
    private static final String PROFILE_IMAGE_PATH = "profile_image_path";

    public static boolean isFirst(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isFirstTime = preferences.getBoolean(FIRST_TIME_KEY, true);
        if (isFirstTime) {
            preferences.edit().putBoolean(FIRST_TIME_KEY, false).apply();
        }
        return isFirstTime;
    }

    public static void setLoggedIn(Context context, boolean isLoggedIn, String email, String name) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(IS_LOGGED_IN, isLoggedIn);
        editor.putString(USER_EMAIL, email);
        editor.putString(USER_NAME, name);
        editor.apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(IS_LOGGED_IN, false);
    }

    public static String getLoggedInEmail(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(USER_EMAIL, "");
    }

    public static String getLoggedInName(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(USER_NAME, "");
    }

    public static void clearSession(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(IS_LOGGED_IN)
                .remove(USER_EMAIL)
                .remove(USER_NAME)
                .remove(PROFILE_IMAGE_PATH) // Also clear profile image path on logout
                .apply();
    }

    // New methods for profile image persistence
    public static void setProfileImagePath(Context context, String imagePath) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(PROFILE_IMAGE_PATH, imagePath).apply();
    }

    public static String getProfileImagePath(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(PROFILE_IMAGE_PATH, null);
    }

    public static void clearProfileImagePath(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(PROFILE_IMAGE_PATH).apply();
    }
}