package com.example.youshouldcheckthis

 import android.animation.ValueAnimator
 import android.app.ActionBar
 import android.content.Context
 import android.content.Intent
 import android.content.res.ColorStateList
 import android.graphics.Color
 import android.os.Handler
 import android.os.Looper
 import android.os.Message
 import android.os.SystemClock
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
 import java.text.SimpleDateFormat
 import java.time.LocalDateTime
 import java.util.*
 import kotlin.collections.ArrayList
 import kotlin.coroutines.*

class ListViewAdapter : BaseAdapter(){
    public var listViewItemList = ArrayList<ListViewItem>()
    public var isRemoveMode = false
    private lateinit var viewGroupParent:ViewGroup
    public lateinit var rootView:View
    public lateinit var interfaceMainActivity:InterfaceMainActivity
    public lateinit var setting:Setting

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

        //list를 View에 대입
        val stockNameTextView = view!!.findViewById<TextView>(R.id.text_stock_name)
        val stockPriceTextView = view.findViewById<TextView>(R.id.text_stock_price)
        val stockPriceUnitTextView = view.findViewById<TextView>(R.id.text_stock_price_unit)
        val stockRateTextView = view.findViewById<TextView>(R.id.text_stock_rate)
        val stockPriceFluctuationTextView = view.findViewById<TextView>(R.id.text_stock_price_fluctuation)
        val stockUpdatedAt = view.findViewById<TextView>(R.id.text_stock_updatedAt)
        val curlistViewItem = listViewItemList[position]
        stockNameTextView.text = curlistViewItem.stockNameStr
        stockPriceTextView.text = curlistViewItem.stockPriceStr
        stockPriceUnitTextView.text = curlistViewItem.stockPriceUnitStr
        stockRateTextView.text = curlistViewItem.stockRateStr
        stockPriceFluctuationTextView.text = curlistViewItem.stockPriceFluctuationStr
        stockUpdatedAt.text = curlistViewItem.stockUpdatedAt


        //색상 결정
        val stockArrowView = view.findViewById<ImageView>(R.id.stock_arrow)
        if(curlistViewItem.stockPriceFluctuationStr!![0]=='+'){
            stockRateTextView.setTextColor(Color.parseColor("#FF0000"))
            stockPriceFluctuationTextView.setTextColor(Color.parseColor("#FF0000"))
            stockArrowView.background = context.resources.getDrawable(R.drawable.ic_baseline_arrow_drop_up)
        }else{
            stockRateTextView.setTextColor(Color.parseColor("#0000FF"))
            stockPriceFluctuationTextView.setTextColor(Color.parseColor("#0000FF"))
            stockArrowView.background = context.resources.getDrawable(R.drawable.ic_baseline_arrow_drop_down)
        }
        
        //item의 alarmMode true/false -> 알람 아이콘 변경
        val btnStockAlarmView = view.findViewById<ImageButton>(R.id.btn_stock_alarm)
        if(curlistViewItem.stockAlarm){
            btnStockAlarmView.background = context.resources.getDrawable(R.drawable.ic_baseline_alarm_on)
        }else{
            btnStockAlarmView.background = context.resources.getDrawable(R.drawable.ic_baseline_alarm_off)
        }

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

        //Item - Alarm Btn Click
        btnStockAlarmView.setOnClickListener(View.OnClickListener {
            curlistViewItem.stockAlarm = !curlistViewItem.stockAlarm
            this.interfaceMainActivity.refreshStockView(viewGroupParent, listViewItemList, position)
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
        item.stockIncreaseRateLimit = this.setting.increaseRateLimit
        item.stockDecreaseRateLimit = this.setting.decreaseRateLimit
        listViewItemList.add(item)
        this.refreshStockList(listViewItemList.size-1, 0)
        this.interfaceMainActivity.setPreferenceStockList(this.listViewItemList)
    }
    fun refreshAllStockList(isPeriodic:Boolean){
        var i: Int = 0
        for(i in 0 until this.count){
            if(isPeriodic&&(listViewItemList[i].stockPriceStr=="0"||listViewItemList[i].stockRateStr=="0"||listViewItemList[i].stockPriceFluctuationStr=="0")){
                continue
            }
            this.refreshStockList(i, 0)
        }
    }
    fun refreshStockList(index: Int, cntTry: Int){
        if(cntTry in 2..9){
            this.interfaceMainActivity.makeToastText("종목을 불러오는 데 실패하여 재시도합니다.", Toast.LENGTH_SHORT)
        }
        else if (cntTry>=10){
            this.interfaceMainActivity.makeToastText("종목을 불러오는 데 실패하였습니다. 종목명 및 인터넷 연결 유무를 확인해주세요.", Toast.LENGTH_LONG)
            return
        }
        val rThread = Thread(
                Runnable {
                    try {
                        val url = "https://www.google.com/search?q=" + listViewItemList[index].stockNameStr + "%20주가"
                        val doc = Jsoup.connect(url)
                                .timeout(1500)
                                .get()

                        val priceData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(1) > span > span:nth-child(1)").last()
                        val priceUnitData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(1) > span > span:nth-child(2)").last()
                        val rateData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2)> div:nth-child(1) > span:nth-child(2) > span:nth-child(2) > span:nth-child(1)").last()
                        val priceFluctuationData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(2) > span:nth-child(1)").last()
                        val updatedAtData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > div:nth-child(3) > span:nth-child(1) > span:nth-child(2)").last()
                        listViewItemList[index].stockPriceStr = priceData.text()
                        listViewItemList[index].stockPriceUnitStr = priceUnitData.text()
                        listViewItemList[index].stockRateStr = rateData.text().substring(1, rateData.text().length-1)
                        listViewItemList[index].stockPriceFluctuationStr = priceFluctuationData.text()
                        listViewItemList[index].stockUpdatedAt = updatedAtData.text().substring(0, updatedAtData.text().length-8)
                        this.interfaceMainActivity.refreshStockView(viewGroupParent, listViewItemList, index)
                    } catch (e: Exception) { //엘리멘트 로드 실패
                        SystemClock.sleep(1000)
                        this.refreshStockList(index, cntTry+1)
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
            val checkbox = curView.findViewById<CheckBox>(R.id.checkbox)
            checkbox.visibility = View.VISIBLE
            var params = LinearLayout.LayoutParams(checkboxLayout.layoutParams.width, checkboxLayout.layoutParams.height)
            params.weight = 1f
            checkboxLayout.layoutParams = params

            val stockLayout = curView.findViewById<LinearLayout>(R.id.stock_layout)
            params = LinearLayout.LayoutParams(stockLayout.layoutParams.width, stockLayout.layoutParams.height)
            params.weight = 5f
            stockLayout.layoutParams = params
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
            checkbox.visibility = View.INVISIBLE
            checkbox.isChecked = false
            var params = LinearLayout.LayoutParams(checkboxLayout.layoutParams.width, checkboxLayout.layoutParams.height)
            params.weight = 0f
            checkboxLayout.layoutParams = params

            val stockLayout = curView.findViewById<LinearLayout>(R.id.stock_layout)
            params = LinearLayout.LayoutParams(stockLayout.layoutParams.width, stockLayout.layoutParams.height)
            params.weight = 6f
            stockLayout.layoutParams = params
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
