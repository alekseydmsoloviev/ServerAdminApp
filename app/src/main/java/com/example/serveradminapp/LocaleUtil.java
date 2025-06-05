package com.example.serveradminapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Intent;

import java.util.Locale;

public class LocaleUtil {
    private static final String PREFS = "app_prefs";
    private static final String KEY_LANG = "lang";

    public static void apply(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());
        setLocale(activity, lang);
    }

    public static void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        if (!lang.equals(config.locale.getLanguage())) {
            config = new Configuration(config);
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, lang).apply();
    }

    public static Context attach(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = context.getResources().getConfiguration();
        if (!lang.equals(config.locale.getLanguage())) {
            Configuration newConfig = new Configuration(config);
            newConfig.setLocale(locale);
            context = context.createConfigurationContext(newConfig);
        }
        return context;
    }

    /** Restart the given activity to apply configuration changes safely.
     *  Any active metrics WebSocket is closed so it can be recreated. */
    public static void restart(Activity activity) {
        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);
    }
}
