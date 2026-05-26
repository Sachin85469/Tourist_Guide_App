package com.arriva.touristguideapp;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class LanguageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RadioGroup rgLanguage = findViewById(R.id.rgLanguage);
        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEnglish) {
                Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbHindi) {
                Toast.makeText(this, "Language set to Hindi", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.rbMarathi) {
                Toast.makeText(this, "Language set to Marathi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}