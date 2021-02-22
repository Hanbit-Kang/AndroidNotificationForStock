package com.example.youshouldcheckthis

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listview: ListView
        val adapter: ListViewAdapter

        adapter = ListViewAdapter()

        listview = findViewById<View>(R.id.stock_list_view) as ListView
        listview.adapter = adapter

        adapter.addItem("NAVER", "399420", "0.52")
        adapter.addItem("LG디스플레이", "24350", "-0.34")
        adapter.addItem("SK하이닉스", "136500", "2.63")
        adapter.addItem("삼성전자", "82200", "-0.48")

        // + Button -> Dialog!
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
            val dialogText = dialogView.findViewById<EditText>(R.id.dialogEditText)
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            builder.setView(dialogView)
                    .setPositiveButton("확인"){ dialog: DialogInterface?, which: Int ->
                        adapter.addItem(dialogText.text.toString(), "0", "0")
                        imm.hideSoftInputFromWindow(dialogText.windowToken, 0)
                    }
                    .setNegativeButton("취소") {dialog: DialogInterface?, which: Int ->
                        imm.hideSoftInputFromWindow(dialogText.windowToken, 0)
                    }
                    .setCancelable(false)
                    .show()
            Handler(Looper.getMainLooper()).postDelayed({
                imm.showSoftInput(dialogText, InputMethodManager.SHOW_FORCED)
            }, 200L)
        }
    }
}