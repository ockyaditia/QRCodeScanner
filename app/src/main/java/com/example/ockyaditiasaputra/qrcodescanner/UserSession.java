package com.example.ockyaditiasaputra.qrcodescanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Created by Ocky Aditia Saputra on 09/10/2015.
 */
public class UserSession {

    SharedPreferences pref;
    Editor editor;
    Context _context;

    private static final String IS_USER_LOGIN = "IsUserLoggedIn";
    public static final String KEY_NAME = "Username";
    public static final String KEY_PASSWORD = "Password";

    public UserSession(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences("usersession_qrcodescanner", Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createUserLoginSession(String name, String password) {
        editor.putBoolean(IS_USER_LOGIN, true);

        editor.putString(KEY_NAME, name);

        editor.putString(KEY_PASSWORD, password);

        editor.commit();
    }

    public boolean checkLogin() {
        if (!this.isUserLoggedIn()) {
            return false;
        } else {
            return true;
        }
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

    public boolean isUserLoggedIn() {
        return pref.getBoolean(IS_USER_LOGIN, false);
    }
}
