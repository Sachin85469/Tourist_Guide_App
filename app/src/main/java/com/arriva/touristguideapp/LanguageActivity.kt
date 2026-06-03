package com.arriva.touristguideapp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import java.util.*

class LanguageActivity : BaseActivity() {

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
        }

        findViewById<View>(R.id.btnApplyLanguage).setOnClickListener {
            val selectedId = rgLanguages.checkedRadioButtonId
            val langCode = when(selectedId) {
                R.id.rbEnglish -> "en"
                R.id.rbHindi -> "hi"
                R.id.rbMarathi -> "mr"
                else -> "en"
            }

            setLocale(langCode)
        }
    }

    private fun setLocale(langCode: String) {
        // Save choice
        getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("language", langCode)
            .apply()

        // Apply immediately to current context
        LocaleHelper.setLocale(this, langCode)
        
        // Show Toast (in selected language)
        val successMsg = when(langCode) {
            "hi" -> "भाषा सफलतापूर्वक बदली गई"
            "mr" -> "भाषा यशस्वीरित्या बदलली गेली"
            else -> "Language changed successfully"
        }
        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show()

        // Recreate current activity to apply change instantly to the current screen
        recreate()
    }
}
