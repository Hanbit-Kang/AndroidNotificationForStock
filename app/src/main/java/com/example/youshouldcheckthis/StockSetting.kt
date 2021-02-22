package com.example.youshouldcheckthis

import android.widget.TextView
import java.text.DecimalFormat

class StockSetting {
    fun convertStockPrice(price: String?): String? {
        val dec = DecimalFormat("#,###")
        return dec.format(Integer.parseInt(price))+'원'
    }

    fun convertStockRate(rate: String?): String? {
        var ret: String? = ""
        if (rate?.toDouble()!!>=0){
            ret+='+'
        }
        ret+=rate + "%"
        return ret
    }
}