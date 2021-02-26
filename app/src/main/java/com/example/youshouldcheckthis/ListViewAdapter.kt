package com.example.youshouldcheckthis

 import android.animation.ValueAnimator
 import android.content.Context
 import android.content.Intent
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
import kotlin.coroutines.*

class ListViewAdapter : BaseAdapter(){
    private var listViewItemList = ArrayList<ListViewItem>()
    public var isRemoveMode = false
    private lateinit var viewGroupParent:ViewGroup
    public lateinit var rootView:View
    public lateinit var interfaceMainActivity:InterfaceMainActivity

    override fun getCount(): Int{
        return listViewItemList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        Log.e("MainActivity", "VIEW"+position.toString())
        var view = convertView
        val context = parent.context
        this.viewGroupParent = parent
        if (view==null){
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.listview_item, parent, false)
        }
        //list를 View에 대입
        val stockNameTextView = view!!.findViewById<TextView>(R.id.text_stock_name)
        val stockPriceTextView = view.findViewById<TextView>(R.id.text_stock_price)
        val stockRateTextView = view.findViewById<TextView>(R.id.text_stock_rate)
        val curlistViewItem = listViewItemList[position]
        stockNameTextView.text = curlistViewItem.stockNameStr
        stockPriceTextView.text = curlistViewItem.stockPriceStr
        stockRateTextView.text = curlistViewItem.stockRateStr


        //색상 결정
/*
        if(curlistViewItem.stockRateStr?.toDouble()!!>=0){
            stockRateTextView.setTextColor(Color.parseColor("#FF0000"))
        }else{
            stockRateTextView.setTextColor(Color.parseColor("#0000FF"))
        }*/

        //LongClick -> edit mode
        val listviewItem = view!!.findViewById<LinearLayout>(R.id.listview_item)
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
        item.stockPriceFluctuationStr = "0"
        listViewItemList.add(item)
        this.refreshStockList(listViewItemList.size-1)
    }
    fun refreshStockList(index: Int){ //TODO: 오류 수정 구간
        val rThread = Thread(
                Runnable {
                    try {
                        Log.e("START", index.toString())
                        val url = "https://www.google.com/search?q=" + listViewItemList[index].stockNameStr + "&tbm=fin"
                        val doc = Jsoup.connect(url)
                                .timeout(1500)
                                .get()
                        val priceData = doc.select("body > c-wiz > div > div:nth-child(3) > main > div:nth-child(2) > c-wiz > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(1) > div > span > div > div").last()
                        //val elemBeforeRateData = doc.select("body > c-wiz > div > div:nth-child(3) > main > div:nth-child(2) > c-wiz > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(2) > div > span:nth-child(1) > div > div > span")
                        //Log.e("selected", index.toString()+elemBeforeRateData)
                        //val rateData = elemBeforeRateData.last().nextElementSibling()
                        Log.e("nextelemt", index.toString())
                        val priceFluctuationData = doc.select("body> c-wiz > div > div:nth-child(3) > main > div:nth-child(2) > c-wiz > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(1) > div:nth-child(1) > div > div:nth-child(2) > div > span:nth-child(1) > div > div").last()
                        listViewItemList[index].stockPriceStr = priceData.text()
                        //listViewItemList[index].stockRateStr = rateData.html()
                        //listViewItemList[index].stockPriceFluctuationStr = priceFluctuationData.text()
                        //Log.e("END", index.toString()+"["+rateData.text()+"]")
                        this.interfaceMainActivity.refreshStockView(viewGroupParent, listViewItemList, index)
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
