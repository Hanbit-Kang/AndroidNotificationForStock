package com.example.youshouldcheckthis

import android.Manifest
import android.Manifest.permission.*
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.work.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import com.google.gson.internal.GsonBuildConfig
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

public interface InterfaceMainActivityForAdapter{
    fun refreshStockView(viewGroupParent: ViewGroup, listViewItemList: ArrayList<ListViewItem>, index: Int)
    fun makeToastText(text: String, lengthToast:Int)
    fun setPreferenceStockList(list: ArrayList<ListViewItem>)
    fun getPreferenceStockList():ArrayList<ListViewItem>?
}

class MainActivity : AppCompatActivity() {
    private var mToast:Toast? = null
    private var setting = Setting()
    private lateinit var adapter:ListViewAdapter
    private val multiplePermissionsCode = 100
    private val requiredPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.USE_FULL_SCREEN_INTENT,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_NETWORK_STATE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissionAndRequest()
        enqueuePeriodicCheckWorker()

        // ActionBar Customize
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)
        findViewById<TextView>(R.id.abs_id).text = "관심 종목"

        //ListView
        val listview: ListView = findViewById<View>(R.id.stock_list_view) as ListView
        adapter = ListViewAdapter()
        adapter.interfaceMainActivityForAdapter = object: InterfaceMainActivityForAdapter{
            override fun refreshStockView(viewGroupParent: ViewGroup, listViewItemList: ArrayList<ListViewItem>, index: Int) {
                runOnUiThread{
                    adapter.notifyDataSetChanged()
                    this.setPreferenceStockList(adapter.listViewItemList)
                }
            }
            override fun makeToastText(text: String, lengthToast:Int){
                runOnUiThread{
                    mToast?.cancel()
                    mToast = Toast.makeText(baseContext, text, lengthToast)
                    mToast?.show()
                }
            }
            override fun setPreferenceStockList(list: ArrayList<ListViewItem>){
                val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
                val gson = GsonBuilder().create()
                val json = gson.toJson(list)
                prefStock.edit().putString("pref_stock_list", json).apply()
            }
            override fun getPreferenceStockList():ArrayList<ListViewItem>?{
                val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
                val gson = GsonBuilder().create()
                val json = prefStock.getString("pref_stock_list", null) ?: return null
                val type = object: TypeToken<ArrayList<ListViewItem>>(){}.type
                return gson.fromJson(json, type)
            }
        }
        adapter.rootView = findViewById<View>(android.R.id.content).rootView
        listview.adapter = adapter

        //Load Stocks From Preference To Adapter
        val tmp = adapter.interfaceMainActivityForAdapter.getPreferenceStockList()
        if(tmp!=null) {
            adapter.listViewItemList = tmp
            adapter.refreshAllStockList(false)
        }

        //종목 없을 때 보이는 메시지 세팅
        setMessageNoList()

        // + / x Btn
        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            if(adapter.isRemoveMode){ // x Btn -> Cancel
                adapter.isRemoveMode= false
                adapter.setCheckBoxInvisible()
            }else{ // + Btn -> Add
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
                val dialogText = dialogView.findViewById<EditText>(R.id.dialogEditText)
                val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                builder.setView(dialogView)
                        .setNegativeButton("취소") {dialog: DialogInterface?, which: Int ->
                            imm.hideSoftInputFromWindow(dialogText.windowToken, 0)
                        }
                        .setPositiveButton("확인"){ dialog: DialogInterface?, which: Int ->
                            adapter.addItem(dialogText.text.toString())
                            imm.hideSoftInputFromWindow(dialogText.windowToken, 0)
                            setMessageNoList()
                        }
                        .setCancelable(false)
                        .show()
                Handler(Looper.getMainLooper()).postDelayed({
                    imm.showSoftInput(dialogText, InputMethodManager.SHOW_FORCED)
                }, 200L)
            }
        }

        //Remove Btn
        findViewById<FloatingActionButton>(R.id.fab_remove).setOnClickListener {
            var todo = arrayListOf<Int>()
            var i:Int= 0
            for(i in listview.count-1 downTo 0){
                val curCheckBox = listview.getChildAt(i).findViewById<CheckBox>(R.id.stock_checkbox)
                if(curCheckBox.isChecked){
                    todo.add(i)
                }
            }
            adapter.setCheckBoxInvisible()
            adapter.isRemoveMode = false
            for(i in todo){
                adapter.removeItem(i)
            }
            adapter.interfaceMainActivityForAdapter.setPreferenceStockList(adapter.listViewItemList)
            runOnUiThread{
                adapter.notifyDataSetChanged()
            }
            setMessageNoList()
        }

        //Periodic Refresh
        val pThread = Thread( //Error app이 실행된 상태에서 alarm으로 MainActivity 진입 시 Thread 중복
                Runnable{
                    try{
                        while(true){
                            //Checking Service에서 리스트를 갱신할 수도 있음
                            val tmp = adapter.interfaceMainActivityForAdapter.getPreferenceStockList()
                            if(tmp!=null) {
                                adapter.listViewItemList = tmp
                            }
                            adapter.refreshAllStockList(true)
                            Log.i("MainActivity","PeriodicRefreshThread")
                            Thread.sleep(60000)
                        }
                    }catch(e:Exception){
                        e.printStackTrace()
                    }
                }
        )
        pThread.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            multiplePermissionsCode -> {
                if(grantResults.isNotEmpty()){
                    for((i, permission) in permissions.withIndex()){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.e("MainActivity", "권한 획득 실패")
                        }
                    }
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_setting, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.settingItem) {
            val settingActivity = SettingActivity()
            val intent = Intent(this, settingActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.to_left, R.anim.none)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if(adapter.isRemoveMode){
            adapter.isRemoveMode= false
            adapter.setCheckBoxInvisible()
            return
        }
        super.onBackPressed()
    }

    fun checkPermissionAndRequest(){
        var rejectedPermissionList = ArrayList<String>()
        for(permission in requiredPermissions){
            if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){
                rejectedPermissionList.add(permission)
            }
        }

        if(rejectedPermissionList.isNotEmpty()){
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array), multiplePermissionsCode)
        }
    }

    fun setMessageNoList(){
        val messageNoListLayout = findViewById<LinearLayout>(R.id.message_no_list_layout)
        if(adapter.count == 0){
            messageNoListLayout.visibility = View.VISIBLE
        }else{
            messageNoListLayout.visibility = View.INVISIBLE
        }
    }

    fun enqueuePeriodicCheckWorker(){
        val checkWorkerBuilder = PeriodicWorkRequest.Builder(CheckWorker::class.java, 16, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                        .build()
                )
        val periodicWorkRequest = checkWorkerBuilder.addTag("CheckWorker")
                .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("CheckWorker", ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest)
        Log.e("MainActivity", "Enqueued Worker")
    }
}