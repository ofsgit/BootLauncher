package com.ofs.bootlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class BootAccessibilityService : AccessibilityService() {

    private var hasLaunchedThisSession = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BootAccessibility", "Service Connected")
        checkAndLaunch()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We no longer trigger on window changes to avoid "forcing" the app on top.
        // The service connection itself is a reliable signal for boot-time start.
    }

    override fun onInterrupt() {}

    private fun checkAndLaunch() {
        if (hasLaunchedThisSession) return

        val prefs = getSharedPreferences("BootLauncherPrefs", Context.MODE_PRIVATE)
        val packageName = prefs.getString("selected_app", null)
        
        if (packageName != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(launchIntent)
                    hasLaunchedThisSession = true
                    Log.d("BootAccessibility", "Launching $packageName")
                } catch (e: Exception) {
                    Log.e("BootAccessibility", "Failed to launch", e)
                }
            }
        }
    }
}
