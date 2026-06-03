package com.arriva.touristguideapp.sos.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.BaseActivity
import com.arriva.touristguideapp.R
import com.arriva.touristguideapp.sos.manager.SOSPreferences
import com.arriva.touristguideapp.sos.model.SOSEvent
import com.google.android.material.appbar.MaterialToolbar

class SOSHistoryActivity : BaseActivity() {
    private lateinit var preferences: SOSPreferences
    private lateinit var adapter: SOSHistoryAdapter
    private var eventsList = listOf<SOSEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_history)

        preferences = SOSPreferences(this)

        // Setup Toolbar
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        // Fetch events history
        eventsList = preferences.sosEvents

        val layoutEmpty = findViewById<View>(R.id.layout_empty_history)
        if (eventsList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
        }

        // Setup RecyclerView
        val rvHistory = findViewById<RecyclerView>(R.id.rv_sos_history)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = SOSHistoryAdapter(eventsList)
        rvHistory.adapter = adapter
    }
}
