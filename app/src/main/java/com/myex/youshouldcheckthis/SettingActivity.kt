package com.myex.youshouldcheckthis

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // ActionBar Customize
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<TextView>(R.id.abs_id).text = "설정"

        //Load settings
        this.loadAllSettingFromPreference()

        //fab Save
        findViewById<FloatingActionButton>(R.id.fab_save).setOnClickListener{
            this.saveAllSettingToPreference()
            Toast.makeText(this,"설정을 저장하였습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.none, R.anim.to_right)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, Restarter::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.none, R.anim.to_right)
    }
    fun saveAllSettingToPreference(){
        setPreferenceSetting("setting_increase_alarm", "CheckBox")
        setPreferenceSetting("setting_decrease_alarm", "CheckBox")
        setPreferenceSetting("setting_increase_rate_limit", "EditText")
        setPreferenceSetting("setting_decrease_rate_limit", "EditText")
    }
    fun setPreferenceSetting(strId:String, strType: String){
        if(strType=="CheckBox"){
            val elem = findViewById<CheckBox>(resources.getIdentifier(strId, "id", packageName))
            val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
            prefStock.edit().putString("pref_$strId", elem.isChecked.toString()).apply()
        }else if(strType=="EditText"){
            val elem = findViewById<EditText>(resources.getIdentifier(strId, "id", packageName))
            val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
            prefStock.edit().putString("pref_$strId", elem.text.toString()).apply()
        }
    }
    fun loadAllSettingFromPreference(){
        loadSettingFromPreference("setting_increase_alarm", "CheckBox")
        loadSettingFromPreference("setting_decrease_alarm", "CheckBox")
        loadSettingFromPreference("setting_increase_rate_limit", "EditText")
        loadSettingFromPreference("setting_decrease_rate_limit", "EditText")
    }
    fun loadSettingFromPreference(strId:String, strType: String){
        if(strType=="CheckBox"){
            val elem = findViewById<CheckBox>(resources.getIdentifier(strId, "id", packageName))
            val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
            elem.isChecked = prefStock.getString("pref_$strId", null).toBoolean()
        }else if(strType=="EditText"){
            val elem = findViewById<EditText>(resources.getIdentifier(strId, "id", packageName))
            val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
            elem.setText(prefStock.getString("pref_$strId", null))
        }
    }
}