package com.example.youshouldcheckthis

 import android.content.Context
 import android.graphics.Color
 import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
 import androidx.core.content.ContextCompat

class ListViewAdapter : BaseAdapter(){
    private var listViewItemList = ArrayList<ListViewItem>()

    override fun getCount(): Int{
        return listViewItemList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val context = parent.context

        if (view==null){
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.listview_item, parent, false)
        }

        val stockNameTextView = view!!.findViewById<TextView>(R.id.text_stock_name)
        val stockPriceTextView = view.findViewById<TextView>(R.id.text_stock_price)
        val stockRateTextView = view.findViewById<TextView>(R.id.text_stock_rate)

        val listViewItem = listViewItemList[position]

        val stocksetting: StockSetting = StockSetting()
        stockNameTextView.text = listViewItem.stockNameStr
        stockPriceTextView.text = stocksetting.convertStockPrice(listViewItem.stockPriceStr)
        stockRateTextView.text = stocksetting.convertStockRate(listViewItem.stockRateStr)
        if(listViewItem.stockRateStr?.toDouble()!!>=0){
            stockRateTextView.setTextColor(Color.parseColor("#FF0000"))
        }else{
            stockRateTextView.setTextColor(Color.parseColor("#0000FF"))
        }

        return view
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any {
        return listViewItemList[position]
    }

    fun addItem(stockName: String, stockPrice: String, stockRate: String){
        val item = ListViewItem()

        item.stockNameStr = stockName
        item.stockPriceStr = stockPrice
        item.stockRateStr = stockRate

        listViewItemList.add(item)
    }
}
