// File: main/java/com/aquaa/edusoul/activities/admin/StatusReportsDashboardActivity.kt
package com.aquaa.edusoul.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.google.android.material.card.MaterialCardView

/*
 * StatusReportsDashboardActivity: Main dashboard for various administrative reports.
 * Migrated to Kotlin.
 */
class StatusReportsDashboardActivity : BaseActivity(), View.OnClickListener {

    private lateinit var cardReportAttendance: MaterialCardView
    private lateinit var cardReportResults: MaterialCardView
    private lateinit var cardReportSyllabusStatus: MaterialCardView
    private lateinit var cardReportFeesPayment: MaterialCardView
    // REMOVED: private lateinit var cardReportAnalytics: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_reports_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarStatusReportsDashboard)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        cardReportAttendance = findViewById(R.id.cardReportAttendance)
        cardReportResults = findViewById(R.id.cardReportResults)
        cardReportSyllabusStatus = findViewById(R.id.cardReportSyllabusStatus)
        cardReportFeesPayment = findViewById(R.id.cardReportFeesPayment)
        // REMOVED: cardReportAnalytics = findViewById(R.id.cardReportAnalytics)

        cardReportAttendance.setOnClickListener(this)
        cardReportResults.setOnClickListener(this)
        cardReportSyllabusStatus.setOnClickListener(this)
        cardReportFeesPayment.setOnClickListener(this)
        // REMOVED: cardReportAnalytics.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val intent: Intent? = when (v.id) {
            R.id.cardReportAttendance -> Intent(this, AttendanceReportActivity::class.java)
            R.id.cardReportResults -> Intent(this, ViewResultsActivity::class.java)
            R.id.cardReportSyllabusStatus -> Intent(this, SyllabusStatusActivity::class.java)
            R.id.cardReportFeesPayment -> Intent(this, FeesPaymentStatusActivity::class.java)
            // REMOVED: R.id.cardReportAnalytics -> Intent(this, AnalyticsDashboardActivity::class.java)
            else -> null
        }

        intent?.let {
            startActivity(it)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}