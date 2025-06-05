package com.example.serveradminapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

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
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        if (context instanceof Activity) {
            ((Activity) context).getApplicationContext().getResources()
                    .updateConfiguration(config, res.getDisplayMetrics());
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, lang).apply();
    }
}
