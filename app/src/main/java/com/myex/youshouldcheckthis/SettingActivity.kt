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
    val settingTypeForId:HashMap<String, String> = hashMapOf(
            "setting_increase_alarm" to "CheckBox",
            "setting_decrease_alarm" to "CheckBox",
            "setting_increase_rate_limit" to "EditText",
            "setting_decrease_rate_limit" to "EditText"
    )
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
            if(isVaildSetting()){
                this.saveAllSettingToPreference()
                Toast.makeText(this,"설정을 저장하였습니다.", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"알람 발생 조건이 비어있거나, 잘못된 형태입니다.", Toast.LENGTH_SHORT).show()
            }
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

    fun isVaildSetting():Boolean{
        try{
            if(!findViewById<EditText>(R.id.setting_increase_rate_limit).text.toString().toFloat().isNaN()
                    &&!findViewById<EditText>(R.id.setting_decrease_rate_limit).text.toString().toFloat().isNaN()){
                return true
            }
        }catch(e:Exception){//toFloat() Format err
            return false
        }
        return false
    }
    fun saveAllSettingToPreference(){
        for((k, v) in settingTypeForId){
            setPreferenceSetting(k, v)
        }
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
        for((k, v) in settingTypeForId){
            loadSettingFromPreference(k, v)
        }
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