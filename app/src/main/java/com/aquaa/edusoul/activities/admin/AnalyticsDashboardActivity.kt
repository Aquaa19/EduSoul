// File: main/java/com/aquaa/edusoul/activities/admin/AnalyticsDashboardActivity.kt
package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity

class AnalyticsDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics_dashboard)

        val toolbar: Toolbar = findViewById(R.id.toolbarAnalyticsDashboard)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Report Analytics"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}