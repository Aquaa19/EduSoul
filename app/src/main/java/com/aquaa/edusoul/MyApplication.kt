package com.aquaa.edusoul

import android.app.Application
import com.google.firebase.FirebaseApp
import android.content.pm.ApplicationInfo

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}