package com.aquaa.edusoul.activities.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.activities.MainActivity
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemConfigActivity : BaseActivity() {

    private val TAG = "SystemConfigActivity"

    // Removed: private lateinit var buttonBackupData: Button
    // Removed: private lateinit var buttonRestoreData: Button
    // Removed: private lateinit var roomBackupRestoreManager: RoomBackupRestoreManager

    // Removed: private lateinit var createBackupFileLauncher: ActivityResultLauncher<String>
    // Removed: private lateinit var selectRestoreFileLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSystemConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "System Configuration"

        // Removed: Initialization of RoomBackupRestoreManager
        // val appDatabase = AppDatabase.getDatabase(applicationContext)
        // roomBackupRestoreManager = RoomBackupRestoreManager(...)

        // Removed: Button declarations and their click listeners for backup/restore
        // buttonBackupData = findViewById(R.id.buttonBackupData)
        // buttonRestoreData = findViewById(R.id.buttonRestoreData)
        // buttonBackupData.setOnClickListener { initiateBackup() }
        // buttonRestoreData.setOnClickListener { initiateRestore() }

        // Removed: ActivityResultLauncher registrations for backup/restore
        // createBackupFileLauncher = registerForActivityResult(...)
        // selectRestoreFileLauncher = registerForActivityResult(...)

        // The migration button and logic are removed as AppDatabase and DAOs are deleted.
        // The local database migration is no longer possible from this point.
    }

    // Removed: addMigrationButton() function as it depended on performMigration()
    // Removed: performMigration() function as it depended on AppDatabase and DAOs

    // Removed: initiateBackup()
    // Removed: performBackupToJson(uri: Uri)
    // Removed: initiateRestore()
    // Removed: performRestoreFromJson(uri: Uri)

    private fun restartApplication() {
        val mStartActivity = Intent(this, MainActivity::class.java)
        val mPendingIntentId = 123456
        val mPendingIntent = android.app.PendingIntent.getActivity(
            this, mPendingIntentId, mStartActivity,
            android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val mgr = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        mgr.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
        System.exit(0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}