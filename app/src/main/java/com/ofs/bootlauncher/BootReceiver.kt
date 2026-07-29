package com.ofs.bootlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            Log.d("BootReceiver", "Boot completed detected")
            
            val prefs = context.getSharedPreferences("BootLauncherPrefs", Context.MODE_PRIVATE)
            val packageName = prefs.getString("selected_app", null)
            
            if (packageName != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Log.d("BootReceiver", "Launching $packageName")
                }
            }
        }
    }
}