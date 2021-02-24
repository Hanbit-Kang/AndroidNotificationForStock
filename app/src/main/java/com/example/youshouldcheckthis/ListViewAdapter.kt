package com.example.youshouldcheckthis

 import android.animation.ValueAnimator
 import android.content.Context
 import android.graphics.Color
 import android.os.Handler
 import android.os.Looper
 import android.os.Message
 import android.text.Layout
 import android.util.Log
 import android.util.TypedValue
 import android.view.*
 import android.view.animation.Animation
 import android.view.animation.RotateAnimation
 import android.view.animation.TranslateAnimation
 import android.view.inputmethod.InputMethodManager
 import android.widget.*
 import androidx.core.content.ContextCompat
 import com.google.android.material.floatingactionbutton.FloatingActionButton
 import org.jsoup.Jsoup


class ListViewAdapter : BaseAdapter(){
    private var listViewItemList = ArrayList<ListViewItem>()
    public var isRemoveMode = false
    private lateinit var viewGroupParent:ViewGroup
    public lateinit var rootView:View

    override fun getCount(): Int{
        return listViewItemList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val context = parent.context
        this.viewGroupParent = parent

        if (view==null){
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.listview_item, parent, false)
        }

        val stockNameTextView = view!!.findViewById<TextView>(R.id.text_stock_name)
        val stockPriceTextView = view.findViewById<TextView>(R.id.text_stock_price)
        val stockRateTextView = view.findViewById<TextView>(R.id.text_stock_rate)

        val listViewItem = listViewItemList[position]

        //각 값들 대입, 형식 변환, 색상 결정
        val stocksetting: StockSetting = StockSetting()

        stockNameTextView.text = listViewItem.stockNameStr
        stockPriceTextView.text = listViewItem.stockPriceStr
        stockRateTextView.text = listViewItem.stockRateStr
/*
        if(listViewItem.stockRateStr?.toDouble()!!>=0){
            stockRateTextView.setTextColor(Color.parseColor("#FF0000"))
        }else{
            stockRateTextView.setTextColor(Color.parseColor("#0000FF"))
        }*/

        //LongClick -> edit mode
        val listviewItem = view.findViewById<LinearLayout>(R.id.listview_item)
        listviewItem.setOnLongClickListener(View.OnLongClickListener{
            this.setCheckBoxVisible()
            this.isRemoveMode = true
            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
            checkbox.isChecked = true
            true
        })

        //Item click(not only checkbox) -> checked
        listviewItem.setOnClickListener(View.OnClickListener{
            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
            if(checkbox.visibility==View.VISIBLE){
                checkbox.isChecked = checkbox.isChecked==false
            }
            true
        })
        return view
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): ListViewItem {
        return listViewItemList[position]
    }

    fun addItem(stockName: String){
        val item = ListViewItem()
        item.stockNameStr = stockName
        item.stockPriceStr = "0"
        item.stockRateStr = "0"
        listViewItemList.add(item)
        this.refreshStock(listViewItemList.size-1)
    }
    fun refreshStock(index: Int){
        val rThread = Thread(
                Runnable {
                    try {
                        val url = "https://www.google.com/search?q=" + listViewItemList[index].stockNameStr + "&tbm=fin"
                        val doc = Jsoup.connect(url).timeout(1000 * 10).get()
                        val priceData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > span:nth-child(1) > span > span:nth-child(1)")
                        val rateData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > span:nth-child(2) > span:nth-child(2) > span:nth-child(1)")
                        Log.e("MainActivity", priceData.size.toString())
                        // TODO: 2021-02-24 쓰레드 안은 동기식인데 왜 select가 끝나지 않았는데 .. 머 암튼 검색이 되다가 안 되다라 반반
                        listViewItemList[index].stockPriceStr = priceData.text().toString()
                        listViewItemList[index].stockRateStr = rateData.text().toString()
                        this.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        )
        rThread.start()
    }
    fun removeItem(index:Int){
        listViewItemList.removeAt(index)
    }

    fun setCheckBoxVisible(){
        var i:Int= 0
        for(i in 0 until this.count){
            val curView:View = this.viewGroupParent.getChildAt(i)
            val checkboxLayout = curView.findViewById<LinearLayout>(R.id.checkbox_layout)
            checkboxLayout.visibility = View.VISIBLE
        }

        var fabRemove = this.rootView.findViewById<FloatingActionButton>(R.id.fab_remove)
        fabRemove.visibility = View.VISIBLE
        val animationTranslateUp:TranslateAnimation = TranslateAnimation(
                0f,
                0f,
                dpToPx(viewGroupParent.context, 64f).toFloat(),
                0f
        )
        animationTranslateUp.duration = 200
        animationTranslateUp.fillAfter = true
        fabRemove.startAnimation(animationTranslateUp)

        var fabAdd = this.rootView.findViewById<FloatingActionButton>(R.id.fab_add)
        val animationRotate45Degree: RotateAnimation = RotateAnimation(
                0f,
                45f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        )
        animationRotate45Degree.duration = 200
        animationRotate45Degree.fillAfter = true
        fabAdd.startAnimation(animationRotate45Degree)
    }

    fun setCheckBoxInvisible(){
        var i:Int= 0
        for(i in 0 until this.count){
            val curView:View = this.viewGroupParent.getChildAt(i)
            val checkboxLayout = curView.findViewById<LinearLayout>(R.id.checkbox_layout)
            val checkbox = curView.findViewById<CheckBox>(R.id.checkbox)
            checkboxLayout.visibility = View.INVISIBLE
            checkbox.isChecked = false
        }

        var fabRemove = this.rootView.findViewById<FloatingActionButton>(R.id.fab_remove)
        val animationTranslateDown:TranslateAnimation = TranslateAnimation(
                0f,
                0f,
                0f,
                dpToPx(viewGroupParent.context, 64f).toFloat()
        )
        animationTranslateDown.duration = 200
        animationTranslateDown.fillAfter = true
        fabRemove.startAnimation(animationTranslateDown)
        Handler(Looper.getMainLooper()).postDelayed({
            fabRemove.visibility = View.INVISIBLE
        }, 250L)

        var fabAdd = this.rootView.findViewById<FloatingActionButton>(R.id.fab_add)
        val animationRotate45Degree: RotateAnimation = RotateAnimation(
                45f,
                0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        )
        animationRotate45Degree.duration = 200
        animationRotate45Degree.fillAfter = true
        fabAdd.startAnimation(animationRotate45Degree)
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }
}
