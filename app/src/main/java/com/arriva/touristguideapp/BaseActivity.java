package com.arriva.touristguideapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private String currentLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", "en");
        Context context = LocaleHelper.setLocale(newBase, langCode);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        currentLanguage = prefs.getString("language", "en");
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "en");
        if (!lang.equals(currentLanguage)) {
            recreate();
        }
    }
}
