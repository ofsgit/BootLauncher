package com.ofs.bootlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selector)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Select App to Launch"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)

        recyclerView.adapter = AppAdapter(apps) { selectedApp ->
            getSharedPreferences("BootLauncherPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("selected_app", selectedApp.activityInfo.packageName)
                .apply()
            finish()
        }
    }

    data class AppInfo(val name: String, val icon: Drawable, val resolveInfo: ResolveInfo)

    inner class AppAdapter(
        private val apps: List<ResolveInfo>,
        private val onAppSelected: (ResolveInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        private val pm = packageManager

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_app_icon)
            val name: TextView = view.findViewById(R.id.tv_app_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.name.text = app.loadLabel(pm).toString()
            holder.icon.setImageDrawable(app.loadIcon(pm))
            holder.itemView.setOnClickListener { onAppSelected(app) }
        }

        override fun getItemCount() = apps.size
    }
}
