package com.aquaa.edusoul.activities.admin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import java.util.ArrayList

class CustomizeDashboardActivity : BaseActivity() {

    private lateinit var recyclerViewDashboardCards: RecyclerView
    private lateinit var textViewNoCustomizationOptions: TextView
    private lateinit var cardAdapter: AdminDashboardCardsAdapter
    private val dashboardCards = ArrayList<DashboardCard>()

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "AdminDashboardPrefs"
        const val KEY_CARD_VISIBILITY_PREFIX = "card_visibility_"

        // Map of card IDs to their default visibility state (true for visible) and name
        val ALL_DASHBOARD_CARDS = mapOf(
            R.id.cardManageUsers to "Manage Users",
            R.id.cardManageStudents to "Manage Students",
            R.id.cardManageSubjects to "Manage Subjects",
            R.id.cardManageBatches to "Manage Batches",
            R.id.cardManageFeeStructures to "Manage Fee Structures",
            R.id.cardManageAnnouncements to "Announcements",
            R.id.cardManageSchedule to "Class Schedule",
            R.id.cardManageExams to "Exam Management",
            R.id.cardStatusReports to "Status Reports",
            R.id.cardSystemConfig to "Backup & Restore"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarCustomizeDashboard)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        recyclerViewDashboardCards = findViewById(R.id.recyclerViewDashboardCards)
        textViewNoCustomizationOptions = findViewById(R.id.textViewNoCustomizationOptions)

        setupRecyclerView() // Call setup, but it will no longer control visibility of no-data message.
        loadCardVisibilityState() // This function will now control the visibility.
    }

    private fun setupRecyclerView() {
        cardAdapter = AdminDashboardCardsAdapter(dashboardCards) { cardId, isChecked ->
            // Save state immediately when checkbox is toggled
            saveCardVisibilityState(cardId, isChecked)
            setResult(RESULT_OK) // Indicate that preferences might have changed
        }
        recyclerViewDashboardCards.layoutManager = LinearLayoutManager(this)
        recyclerViewDashboardCards.adapter = cardAdapter
        // REMOVED: The if/else block that managed visibility based on dashboardCards.isEmpty()
        // This logic is now moved to loadCardVisibilityState()
    }

    private fun loadCardVisibilityState() {
        dashboardCards.clear()
        for ((id, name) in ALL_DASHBOARD_CARDS) {
            val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + id, true) // Default to true
            dashboardCards.add(DashboardCard(id, name, isVisible))
        }
        cardAdapter.notifyDataSetChanged()

        // NEW: Control visibility here after data is loaded and adapter notified
        if (dashboardCards.isEmpty()) {
            textViewNoCustomizationOptions.visibility = View.VISIBLE
            recyclerViewDashboardCards.visibility = View.GONE
        } else {
            textViewNoCustomizationOptions.visibility = View.GONE
            recyclerViewDashboardCards.visibility = View.VISIBLE
        }
    }

    private fun saveCardVisibilityState(cardId: Int, isVisible: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_CARD_VISIBILITY_PREFIX + cardId, isVisible)
            .apply()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Go back and trigger AdminDashboardActivity's onResume
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Data class to represent a dashboard card
    data class DashboardCard(val id: Int, val name: String, var isVisible: Boolean)

    // RecyclerView Adapter
    class AdminDashboardCardsAdapter(
        private val cards: ArrayList<DashboardCard>,
        private val listener: (cardId: Int, isChecked: Boolean) -> Unit
    ) : RecyclerView.Adapter<AdminDashboardCardsAdapter.CardViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dashboard_card_customize, parent, false)
            return CardViewHolder(view)
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = cards[position]
            holder.textViewCardName.text = card.name
            holder.checkBoxCardVisibility.setOnCheckedChangeListener(null) // Remove listener to prevent recursion
            holder.checkBoxCardVisibility.isChecked = card.isVisible
            holder.checkBoxCardVisibility.setOnCheckedChangeListener { _, isChecked ->
                card.isVisible = isChecked // Update model
                listener.invoke(card.id, isChecked) // Notify activity
            }
        }

        override fun getItemCount(): Int = cards.size

        class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textViewCardName: TextView = itemView.findViewById(R.id.textViewCardName)
            val checkBoxCardVisibility: CheckBox = itemView.findViewById(R.id.checkBoxCardVisibility)
        }
    }
}