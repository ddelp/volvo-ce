package com.ddelp.volvoce;

import android.content.SharedPreferences;

import com.firebase.client.Firebase;

/**
 * Created by Denny on 5/8/16.
 */
public class VolvoCE extends android.app.Application {
    public static SharedPreferences prefs;
    public static SharedPreferences.Editor prefsEditor;

    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this); //set system-wide context for firebase
    }

}