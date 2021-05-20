package com.myex.youshouldcheckthis

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.gesture.Gesture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

public interface InterfaceMainActivityForAdapter{
    fun refreshStockView(viewGroupParent: ViewGroup, listViewItemList: ArrayList<ListViewItem>, index: Int)
    fun makeToastText(text: String, lengthToast:Int)
    fun setPreferenceStockList(list: ArrayList<ListViewItem>)
    fun getPreferenceStockList():ArrayList<ListViewItem>?
}

class MainActivity : AppCompatActivity() {
    private var mToast:Toast? = null
    private var isPThreadRunning = true
    private lateinit var adapter: CustomAdapter
    private val multiplePermissionsCode = 100
    private val requiredPermissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.USE_FULL_SCREEN_INTENT,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.RECEIVE_BOOT_COMPLETED
    )

    private lateinit var fabDrag: View
    private lateinit var fabSwipe: View
    private lateinit var fabCancel: View

    private lateinit var listViewItemList:ArrayList<ListViewItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissionAndRequest()

        //Background Service For Alarm!
        startService(Intent(this, CheckingService::class.java))

        // ActionBar Customize
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)
        findViewById<TextView>(R.id.abs_id).text = "관심 종목"

        //get fab buttons
        fabDrag = findViewById<FloatingActionButton>(R.id.fab_drag)
        fabSwipe = findViewById<FloatingActionButton>(R.id.fab_swipe)
        fabCancel = findViewById<FloatingActionButton>(R.id.fab_cancel)

        //ListView
        val recyclerView: androidx.recyclerview.widget.RecyclerView = findViewById<View>(R.id.stock_recycler_view) as androidx.recyclerview.widget.RecyclerView
        listViewItemList = ArrayList<ListViewItem>()
        adapter = CustomAdapter(listViewItemList)
        adapter.interfaceMainActivityForAdapter = object: InterfaceMainActivityForAdapter{
            override fun refreshStockView(viewGroupParent: ViewGroup, listViewItemList: ArrayList<ListViewItem>, index: Int) {
                runOnUiThread{
                    adapter.notifyDataSetChanged()
                    this.setPreferenceStockList(adapter.dataSet)
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
        recyclerView.adapter = adapter

        //Load Stocks From Preference To Adapter
        val tmp = adapter.interfaceMainActivityForAdapter.getPreferenceStockList()
        if(tmp!=null) {
            adapter.dataSet = tmp
            adapter.refreshAllStockList(false)
        }

        //종목 없을 때 보이는 메시지 세팅
        setMessageNoList()

        // + Btn
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)
        fabAdd.setOnClickListener {
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

        // Edit 3 Buttons
        fabDrag.setOnClickListener{
            adapter.setDragMode()
            adapter.setEditButtonsColor(fabDrag, fabSwipe)
        }
        fabSwipe.setOnClickListener{
            adapter.setSwipeMode()
            adapter.setEditButtonsColor(fabDrag, fabSwipe)
        }
        fabCancel.setOnClickListener{
            adapter.setEditMode(false, fabDrag, fabSwipe, fabCancel)

            adapter.interfaceMainActivityForAdapter.setPreferenceStockList(adapter.dataSet)
            runOnUiThread{
                adapter.notifyDataSetChanged()
            }
        }

        //Swipe To Remove
        val itemTouchHelper = ItemTouchHelper(this.mIth)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        //Periodic Refresh
        isPThreadRunning = true
        val pThread = Thread( //Error app이 실행된 상태에서 alarm으로 MainActivity 진입 시 Thread 중복
            Runnable{
                try{
                    while(isPThreadRunning){
                        //Checking Service에서 리스트를 갱신할 수도 있음
                        val tmp = adapter.interfaceMainActivityForAdapter.getPreferenceStockList()
                        if(tmp!=null) {
                            adapter.dataSet = tmp
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
        pThread?.start()
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
        when(item.itemId){
            R.id.settingItem -> {
                val settingActivity = SettingActivity()
                val intent = Intent(this, settingActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.to_left, R.anim.none)
            }
            R.id.editItem -> {
                if(findViewById<FloatingActionButton>(R.id.fab_drag).isVisible){
                    //편집모드 OFF
                    adapter.setEditMode(false, fabDrag, fabSwipe, fabCancel)

                    adapter.interfaceMainActivityForAdapter.setPreferenceStockList(adapter.dataSet)
                    runOnUiThread{
                        adapter.notifyDataSetChanged()
                    }
                }else{
                    //편집모드 ON
                    adapter.setEditMode(true, fabDrag, fabSwipe, fabCancel)
                    adapter.setDragMode()
                    adapter.setEditButtonsColor(fabDrag, fabSwipe)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        isPThreadRunning = false
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, Restarter::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if(adapter.isDragMode || adapter.isSwipeMode){
            adapter.setEditMode(false, fabDrag, fabSwipe, fabCancel)

            adapter.interfaceMainActivityForAdapter.setPreferenceStockList(adapter.dataSet)
            runOnUiThread{
                adapter.notifyDataSetChanged()
            }
        }else{
            super.onBackPressed()
        }
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
        if(adapter.itemCount == 0){
            messageNoListLayout.visibility = View.VISIBLE
        }else{
            messageNoListLayout.visibility = View.INVISIBLE
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int){
        var items = adapter.dataSet
        val fromItem = items[fromPosition]

        items.removeAt(fromPosition)
        items.add(toPosition, fromItem)

        adapter.notifyItemMoved(fromPosition, toPosition)
    }

    val mIth: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun isLongPressDragEnabled(): Boolean {
                return adapter.isDragMode
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return adapter.isSwipeMode
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                swapItems(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                adapter.removeItem(position)
                adapter.interfaceMainActivityForAdapter.setPreferenceStockList(adapter.dataSet)
                runOnUiThread{
                    adapter.notifyDataSetChanged()
                }

                setMessageNoList()
            }
        }
}