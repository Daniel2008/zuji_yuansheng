package com.damors.zuji.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.damors.zuji.model.User;
import com.google.gson.Gson;

public class UserManager {
    private static final String PREF_NAME = "user_pref";
    private static final String KEY_USER = "user_info";
    private static final String KEY_TOKEN = "user_token";

    private static UserManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    private User currentUser;
    private String token;

    private UserManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadUserData();
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager(context);
                }
            }
        }
    }

    public static UserManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserManager must be initialized first");
        }
        return instance;
    }

    private void loadUserData() {
        String userJson = preferences.getString(KEY_USER, null);
        if (!TextUtils.isEmpty(userJson)) {
            currentUser = gson.fromJson(userJson, User.class);
        }
        token = preferences.getString(KEY_TOKEN, null);
    }

    public void saveUserAndToken(User user, String token) {
        this.currentUser = user;
        this.token = token;

        SharedPreferences.Editor editor = preferences.edit();
        if (user != null) {
            editor.putString(KEY_USER, gson.toJson(user));
        } else {
            editor.remove(KEY_USER);
        }

        if (token != null) {
            editor.putString(KEY_TOKEN, token);
        } else {
            editor.remove(KEY_TOKEN);
        }

        editor.apply();
    }

    public void logout() {
        currentUser = null;
        token = null;
        preferences.edit().clear().apply();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return currentUser != null && !TextUtils.isEmpty(token);
    }
}