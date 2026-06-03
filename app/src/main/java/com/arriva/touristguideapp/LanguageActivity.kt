package com.arriva.touristguideapp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.util.*

class LanguageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val rgLanguages = findViewById<RadioGroup>(R.id.rgLanguages)
        val currentLang = getSharedPreferences("settings", Context.MODE_PRIVATE).getString("language", "en")
        
        when(currentLang) {
            "en" -> rgLanguages.check(R.id.rbEnglish)
            "hi" -> rgLanguages.check(R.id.rbHindi)
            "mr" -> rgLanguages.check(R.id.rbMarathi)
            "kn" -> rgLanguages.check(R.id.rbKannada)
        }

        findViewById<View>(R.id.btnApplyLanguage).setOnClickListener {
            val selectedId = rgLanguages.checkedRadioButtonId
            val langCode = when(selectedId) {
                R.id.rbEnglish -> "en"
                R.id.rbHindi -> "hi"
                R.id.rbMarathi -> "mr"
                R.id.rbKannada -> "kn"
                else -> "en"
            }

            setLocale(langCode)
        }
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        
        getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("language", langCode).apply()
        
        // This won't apply instantly to all activities without restart or recreation
        // For production "react-i18next" style behavior, we'd need a base activity that handles this
        Toast.makeText(this, "Language changed. Please restart the app to see full changes.", Toast.LENGTH_LONG).show()
        finish()
    }
}
