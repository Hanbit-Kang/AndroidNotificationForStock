package com.example.youshouldcheckthis

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ActionBar Customize
        supportActionBar?.title = "관심 종목"

        //ListView
        val listview: ListView = findViewById<View>(R.id.stock_list_view) as ListView
        val adapter: ListViewAdapter = ListViewAdapter()
        adapter.rootView = findViewById<View>(android.R.id.content).rootView
        listview.adapter = adapter

        //Default
        adapter.addItem("NAVER")
        adapter.addItem("LG디스플레이")
        adapter.addItem("SK하이닉스")
        adapter.addItem("삼성전자")

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
            var i:Int= 0
            for(i in listview.count-1 downTo 0){
                val curCheckBox = listview.getChildAt(i).findViewById<CheckBox>(R.id.checkbox)
                if(curCheckBox.isChecked){
                    adapter.removeItem(i)
                }
            }
            adapter.isRemoveMode = false
            adapter.setCheckBoxInvisible()
            adapter.notifyDataSetChanged()
        }
    }
}