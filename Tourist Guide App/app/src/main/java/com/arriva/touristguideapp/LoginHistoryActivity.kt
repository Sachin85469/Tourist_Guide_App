package com.arriva.touristguideapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginHistoryActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        container = findViewById(R.id.historyContainer)
        tvEmpty = findViewById(R.id.tvHistoryEmpty)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        loadHistory()
    }

    private fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        container.removeAllViews()

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("users")
                    .document(uid)
                    .collection("login_history")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20) // Show up to last 20 logins
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    return@launch
                }

                tvEmpty.visibility = View.GONE
                val inflater = LayoutInflater.from(this@LoginHistoryActivity)

                for (doc in snapshot.documents) {
                    val date = doc.getString("date") ?: "Unknown Date"
                    val time = doc.getString("time") ?: "Unknown Time"
                    val device = doc.getString("device") ?: "Unknown Device"

                    // Inflate card
                    val card = inflater.inflate(R.layout.item_login_history, container, false) as MaterialCardView
                    card.findViewById<TextView>(R.id.tvHistoryDevice).text = device
                    card.findViewById<TextView>(R.id.tvHistoryDateTime).text = "$date at $time"

                    container.addView(card)
                }

            } catch (e: Exception) {
                Toast.makeText(this@LoginHistoryActivity, "Failed to load history: ${e.message}", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Error loading history"
            }
        }
    }
}
