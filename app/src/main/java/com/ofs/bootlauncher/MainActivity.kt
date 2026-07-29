package com.ofs.bootlauncher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ofs.bootlauncher.databinding.MainactivityBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainactivityBinding
    private val deviceAdminReceiver by lazy { ComponentName(this, MyDeviceAdminReceiver::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        updateStatus()

        binding.contentMain.btnSetup.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.contentMain.btnSelectApp.setOnClickListener {
            startActivity(Intent(this, AppSelectorActivity::class.java))
        }
        
        binding.fab.setOnClickListener {
             updateSelectedAppText()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        checkAndRequestPermissions(autoRequest = true)
    }

    private fun checkAndRequestPermissions(autoRequest: Boolean = false) {
        // 1. Device Admin
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isAdminActive(deviceAdminReceiver)) {
            if (autoRequest) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Admin privileges are used to ensure the app can trigger startup events.")
                }
                startActivity(intent)
            }
            return
        }

        // 2. Overlay Permission
        if (!Settings.canDrawOverlays(this)) {
            if (autoRequest) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
            return
        }

        // 3. Battery Optimization
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            if (autoRequest) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
            return
        }

        // 4. Accessibility Service
        if (!isAccessibilityServiceEnabled()) {
            if (autoRequest) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                AlertDialog.Builder(this)
                    .setTitle("Enable Accessibility")
                    .setMessage("Please enable BootLauncher in the Accessibility settings to ensure reliable auto-launch.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            return
        }

        updateStatus()

        if (!autoRequest) {
            // All permissions are already granted (only show if manually triggered)
            AlertDialog.Builder(this)
                .setTitle("Setup Complete")
                .setMessage("All necessary permissions have been granted.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun updateStatus() {
        updateSelectedAppText()
        updatePermissionStatusText()
    }

    private fun updatePermissionStatusText() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        val isAdminActive = dpm.isAdminActive(deviceAdminReceiver)
        val canOverlay = Settings.canDrawOverlays(this)
        val isIgnoringBattery = pm.isIgnoringBatteryOptimizations(packageName)
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()

        val allGranted = isAdminActive && canOverlay && isIgnoringBattery && isAccessibilityEnabled
        
        binding.contentMain.tvPermissionStatus.apply {
            if (allGranted) {
                text = "Permissions: ALL GRANTED"
                setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
            } else {
                val missing = mutableListOf<String>()
                if (!isAdminActive) missing.add("Admin")
                if (!canOverlay) missing.add("Overlay")
                if (!isIgnoringBattery) missing.add("Battery")
                if (!isAccessibilityEnabled) missing.add("Accessibility")
                
                text = "Permissions Missing: ${missing.joinToString(", ")}"
                setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${BootAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled == 1) {
            val settingValue = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return settingValue?.contains(service) == true
        }
        return false
    }


    private fun updateSelectedAppText() {
        val prefs = getSharedPreferences("BootLauncherPrefs", Context.MODE_PRIVATE)
        val packageName = prefs.getString("selected_app", null)
        
        if (packageName != null) {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                binding.contentMain.tvSelectedApp.text = "Selected App: $appLabel"
            } catch (e: Exception) {
                binding.contentMain.tvSelectedApp.text = "Selected App: $packageName"
            }
        } else {
            binding.contentMain.tvSelectedApp.text = "Selected App: None"
        }
    }
}
