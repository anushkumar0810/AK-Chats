package com.anush_projects.akchats.Application;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.anush_projects.akchats.utils.ThemeHelper;

public class AKProjectsApplications  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPreferences.getString("themePref", ThemeHelper.DEFAULT_MODE);
        ThemeHelper.applyTheme(themePref);
    }
}
