package com.example.youshouldcheckthis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // ActionBar Customize
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<TextView>(R.id.abs_id).text = "설정"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.none, R.anim.to_right)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        overridePendingTransition(R.anim.none, R.anim.to_right)
    }
}