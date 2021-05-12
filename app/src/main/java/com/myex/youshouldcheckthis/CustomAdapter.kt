package com.myex.youshouldcheckthis

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.jsoup.Jsoup

class CustomAdapter(public var dataSet: ArrayList<ListViewItem>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    public var isRemoveMode = false
    private lateinit var viewGroupParent: ViewGroup
    public lateinit var rootView: View
    public lateinit var interfaceMainActivityForAdapter:InterfaceMainActivityForAdapter

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val textStockNameView: TextView
        val textStockUpdatedAtView: TextView
        val textStockPriceView: TextView
        val textStockPriceUnitView: TextView
        val textStockPriceFlutuation: TextView
        val textStockRate: TextView

        init{
            textStockNameView = view.findViewById(R.id.text_stock_name)
            textStockUpdatedAtView = view.findViewById(R.id.text_stock_updatedAt)
            textStockPriceView = view.findViewById(R.id.text_stock_price)
            textStockPriceUnitView = view.findViewById(R.id.text_stock_price_unit)
            textStockPriceFlutuation = view.findViewById(R.id.text_stock_price_fluctuation)
            textStockRate = view.findViewById(R.id.text_stock_rate)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item, parent, false)

        viewGroupParent = parent

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemView:View = holder.itemView

        //텍스트에 값 대입
        holder.textStockNameView.text = dataSet[position].stockNameStr
        holder.textStockUpdatedAtView.text = dataSet[position].stockUpdatedAt
        holder.textStockPriceView.text = dataSet[position].stockPriceStr
        holder.textStockPriceUnitView.text = dataSet[position].stockPriceUnitStr
        holder.textStockPriceFlutuation.text = dataSet[position].stockPriceFluctuationStr
        holder.textStockRate.text = dataSet[position].stockRateStr
        
        //색상 결정
        val stockArrowView = itemView.findViewById<ImageView>(R.id.stock_arrow)
        if(dataSet[position].stockPriceFluctuationStr!![0]=='+'){
            holder.textStockRate.setTextColor(Color.parseColor("#FF0000"))
            holder.textStockPriceFlutuation.setTextColor(Color.parseColor("#FF0000"))
            stockArrowView.background = itemView.context.resources.getDrawable(R.drawable.ic_baseline_arrow_drop_up)
        }else{
            holder.textStockRate.setTextColor(Color.parseColor("#0000FF"))
            holder.textStockPriceFlutuation.setTextColor(Color.parseColor("#0000FF"))
            stockArrowView.background = itemView.context.resources.getDrawable(R.drawable.ic_baseline_arrow_drop_down)
        }

        //알람 온오프 아이콘 세팅
        val btnStockAlarmView = itemView.findViewById<ImageButton>(R.id.btn_stock_alarm)
        if(dataSet[position].stockAlarm){
            btnStockAlarmView.background = itemView.context.resources.getDrawable(R.drawable.ic_baseline_alarm_on)
        }else{
            btnStockAlarmView.background = itemView.context.resources.getDrawable(R.drawable.ic_baseline_alarm_off)
        }

        //제거 모드 ON/OFF에 따라서 체크박스 설정
        if(isRemoveMode) setCheckBoxVisible(itemView)
        else setCheckBoxInvisible(itemView)

        //제거 모드
        val recyclerviewItem = itemView.findViewById<LinearLayout>(R.id.recyclerview_item)
        recyclerviewItem.setOnLongClickListener(View.OnLongClickListener {
            this.setRemoveModeOn()
            val checkbox = itemView.findViewById<CheckBox>(R.id.stock_checkbox)
            checkbox.isChecked = true
            true
        })

        //제거 모드에서 아이템 클릭 시 체크
        recyclerviewItem.setOnClickListener(View.OnClickListener { 
            val checkbox = itemView.findViewById<CheckBox>(R.id.stock_checkbox)
            if(checkbox.visibility==View.VISIBLE){
                checkbox.isChecked = checkbox.isChecked==false
            }
            true
        })
        
        //알림 온오프
        btnStockAlarmView.setOnClickListener(View.OnClickListener {
            dataSet[position].stockAlarm = !dataSet[position].stockAlarm
            this.interfaceMainActivityForAdapter.refreshStockView(viewGroupParent, dataSet, position)
            this.interfaceMainActivityForAdapter.setPreferenceStockList(dataSet)
            true
        })

        //마지막 인덱스 밑에 여백 추가
        if(position==dataSet.lastIndex){
            var params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 150
            holder.itemView.layoutParams = params
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
    
    fun addItem(stockName: String){
        val item = ListViewItem()
        item.stockNameStr = stockName
        item.stockPriceStr = "0"
        item.stockRateStr = "0"
        item.stockPriceFluctuationStr = "0"
        dataSet.add(item)
        this.refreshStockList(dataSet.size-1, 0)
        this.interfaceMainActivityForAdapter.setPreferenceStockList(dataSet)
    }

    fun removeItem(index:Int){
        dataSet.removeAt(index)
    }

    fun refreshAllStockList(isPeriodic:Boolean){
        var i: Int = 0
        for(i in 0 until this.itemCount){
            if(isPeriodic&&(dataSet[i].stockPriceStr=="0"||dataSet[i].stockRateStr=="0"||dataSet[i].stockPriceFluctuationStr=="0")){
                continue
            }
            this.refreshStockList(i, 0)
        }
    }

    fun refreshStockList(index: Int, cntTry: Int){
        if(cntTry==3){
            this.interfaceMainActivityForAdapter.makeToastText("종목을 불러오는 데 실패하여 재시도합니다.", Toast.LENGTH_SHORT)
        }
        else if (cntTry>=10){
            this.interfaceMainActivityForAdapter.makeToastText("종목을 불러오는 데 실패하였습니다. 종목명 및 인터넷 연결 유무를 확인해주세요.", Toast.LENGTH_LONG)
            return
        }
        val rThread = Thread(
            Runnable {
                try {
                    val url = "https://www.google.com/search?q=" + dataSet[index].stockNameStr + "%20주가"
                    val doc = Jsoup.connect(url)
                        .timeout(1500)
                        .get()

                    val priceData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(1) > span > span:nth-child(1)").last()
                    val priceUnitData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(1) > span > span:nth-child(2)").last()
                    val rateData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2)> div:nth-child(1) > span:nth-child(2) > span:nth-child(2) > span:nth-child(1)").last()
                    val priceFluctuationData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(2) > span:nth-child(1)").last()
                    val updatedAtData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > div:nth-child(3) > span:nth-child(1) > span:nth-child(2)").last()
                    dataSet[index].stockPriceStr = priceData.text()
                    dataSet[index].stockPriceUnitStr = priceUnitData.text()
                    dataSet[index].stockRateStr = rateData.text().substring(1, rateData.text().length-1)
                    dataSet[index].stockPriceFluctuationStr = priceFluctuationData.text()
                    dataSet[index].stockUpdatedAt = updatedAtData.text().substring(0, updatedAtData.text().length-8)
                    this.interfaceMainActivityForAdapter.refreshStockView(viewGroupParent, dataSet, index)
                } catch (e: Exception) { //엘리멘트 로드 실패
                    SystemClock.sleep(1000)
                    this.refreshStockList(index, cntTry+1)
                }
            }
        )
        rThread.start()
    }

    fun setRemoveModeOn(){
        this.isRemoveMode = true

        var i: Int
        for(i in 0 until this.viewGroupParent.childCount){
            setCheckBoxVisible(this.viewGroupParent.getChildAt(i))
        }

        var fabRemove = this.rootView.findViewById<FloatingActionButton>(R.id.fab_remove)
        fabRemove.visibility = View.VISIBLE
        val animationTranslateUp: TranslateAnimation = TranslateAnimation(
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

    fun setRemoveModeOff(){
        this.isRemoveMode = false

        var i: Int
        for(i in 0 until this.viewGroupParent.childCount){
            setCheckBoxInvisible(this.viewGroupParent.getChildAt(i))
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

    fun setCheckBoxVisible(itemView: View){
        val checkboxLayout = itemView.findViewById<LinearLayout>(R.id.checkbox_layout)
        val checkbox = itemView.findViewById<CheckBox>(R.id.stock_checkbox)
        var params = LinearLayout.LayoutParams(checkboxLayout.layoutParams.width, checkboxLayout.layoutParams.height)

        checkbox.visibility = View.VISIBLE
        params.weight = 1f
        checkboxLayout.layoutParams = params

        val stockLayout = itemView.findViewById<LinearLayout>(R.id.stock_layout)
        params = LinearLayout.LayoutParams(stockLayout.layoutParams.width, stockLayout.layoutParams.height)
        params.weight = 5f
        stockLayout.layoutParams = params
    }

    fun setCheckBoxInvisible(itemView: View){
        val checkboxLayout = itemView.findViewById<LinearLayout>(R.id.checkbox_layout)
        val checkbox = itemView.findViewById<CheckBox>(R.id.stock_checkbox)
        var params = LinearLayout.LayoutParams(checkboxLayout.layoutParams.width, checkboxLayout.layoutParams.height)

        checkbox.visibility = View.INVISIBLE
        checkbox.isChecked = false
        params.weight = 0f
        checkboxLayout.layoutParams = params

        val stockLayout = itemView.findViewById<LinearLayout>(R.id.stock_layout)
        params = LinearLayout.LayoutParams(stockLayout.layoutParams.width, stockLayout.layoutParams.height)
        params.weight = 6f
        stockLayout.layoutParams = params
    }

    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }
}