package com.example.moviebuzz;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_BALANCE = "balance"; // Added key for balance

    private static UserSession instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private UserSession(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context.getApplicationContext());
        }
        return instance;
    }

    public void login(String username, String role) {
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }


    public void setBalance(float balance) {
        editor.putFloat(KEY_BALANCE, balance);
        editor.apply();
    }


    public float getBalance() {
        return sharedPreferences.getFloat(KEY_BALANCE, 0.0f);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    public boolean isLoggedIn() {
        return getUsername() != null;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getRole());
    }
}