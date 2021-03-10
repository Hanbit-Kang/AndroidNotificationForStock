package com.myex.youshouldcheckthis

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.lang.Exception
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList

class CheckingService : Service() {
    val channel_name: String = "주식 변동"
    val CHANNEL_ID: String = "MY_CH"
    var notificationId: Int = 1002

    var isRunning = true
    var setting = Setting()
    var listViewItemList:ArrayList<ListViewItem>? = null

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID, channel_name, NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build()
            startForeground(1, notification)
        }
        makeCheckingCaroutine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    fun makeCheckingCaroutine(){
        Log.i("CheckingService", "makeCheckingCaroutine")
        isRunning = true
        GlobalScope.launch(Dispatchers.IO){
            while(isRunning){
                Log.i("CheckingService","PeriodicRefreshCaroutine")
                loadAllSettingFromPreference()
                val tmp = getPreferenceStockList()
                if(tmp!=null&&((setting.decreaseAlarm == true &&setting.decreaseRateLimit?.isNaN()==false)||(setting.increaseAlarm == true &&setting.increaseRateLimit?.isNaN()==false))&&(tmp.size>=1)){
                    listViewItemList = tmp!!
                    var i:Int? = null
                    for(i in 0 until listViewItemList!!.size){
                        if(i<listViewItemList!!.size&&listViewItemList!![i].stockAlarm){
                            refreshStockListOnBackground(i, 0)
                            delay(1000)
                        }
                    }
                    delay(6*60000)//TODO
                }else{
                    killCheckingCaroutine()
                }
            }
        }
    }
    fun killCheckingCaroutine(){
        isRunning=false
    }

    fun loadAllSettingFromPreference(){
        this.setting.increaseAlarm = getPreferenceSetting("setting_increase_alarm", "CheckBox").toBoolean()
        this.setting.decreaseAlarm = getPreferenceSetting("setting_decrease_alarm", "CheckBox").toBoolean()
        this.setting.increaseRateLimit = getPreferenceSetting("setting_increase_rate_limit", "EditText")?.toFloat()
        this.setting.decreaseRateLimit = getPreferenceSetting("setting_decrease_rate_limit", "EditText")?.toFloat()
        this.setting.repeatHour = getPreferenceSetting("setting_repeat_hour", "EditText")?.toInt()
        this.setting.repeatMinute = getPreferenceSetting("setting_repeat_minute", "EditText")?.toInt()
    }
    fun getPreferenceSetting(strId:String, strType: String):String?{
        val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
        return prefStock.getString("pref_$strId", null)
    }
    fun getPreferenceStockList():ArrayList<ListViewItem>?{
        val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
        val gson = GsonBuilder().create()
        val json = prefStock.getString("pref_stock_list", null) ?: return null
        val type = object: TypeToken<ArrayList<ListViewItem>>(){}.type
        return gson.fromJson(json, type)
    }

    fun refreshStockListOnBackground(index: Int, cntTry: Int){
        if(cntTry>=10){
            return
        }
        val rThread = Thread(
                Runnable {
                    try{
                        val url = "https://www.google.com/search?q=" + listViewItemList!![index].stockNameStr + "%20주가"
                        val doc = Jsoup.connect(url)
                                .timeout(1500)
                                .get()

                        val timeNow = Date(System.currentTimeMillis())
                        if(listViewItemList!![index].recentAlarmDateTime==null||isAvailableStock(timeNow, listViewItemList!![index].recentAlarmDateTime!!)){ //연산 함수 만들기
                            val priceFluctuationData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(2) > span:nth-child(1)").last()
                            val rateData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2)> div:nth-child(1) > span:nth-child(2) > span:nth-child(2) > span:nth-child(1)").last()

                            if(this.setting.increaseAlarm==true&&priceFluctuationData.text()[0]=='+'){
                                if(rateData.text().substring(1, rateData.text().length-2).toFloat()>this.setting.increaseRateLimit!!){
                                    listViewItemList!![index].recentAlarmDateTime = timeNow
                                    this.setPreferenceStockList(listViewItemList!!)
                                    this.createStockNotification("종목 알림", "["+listViewItemList!![index].stockNameStr+"] 종목 "+priceFluctuationData.text()[0]+rateData.text().substring(1, rateData.text().length-1)+" 변동")
                                }
                            }else if(this.setting.decreaseAlarm==true&&priceFluctuationData.text()[0]=='−'){
                                if(rateData.text().substring(1, rateData.text().length-2).toFloat()>this.setting.decreaseRateLimit!!){
                                    listViewItemList!![index].recentAlarmDateTime = timeNow
                                    this.setPreferenceStockList(listViewItemList!!)
                                    this.createStockNotification("종목 알림", "["+listViewItemList!![index].stockNameStr+"] 종목 "+priceFluctuationData.text()[0]+rateData.text().substring(1, rateData.text().length-1)+" 변동")
                                }
                            }
                        }
                    }catch(e:Exception){
                        this.refreshStockListOnBackground(index, cntTry+1)
                    }
                }
        )
        rThread.start()
        rThread.join()
    }
    fun setPreferenceStockList(list: ArrayList<ListViewItem>){
        val prefStock: SharedPreferences = baseContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
        val gson = GsonBuilder().create()
        val json = gson.toJson(list)
        prefStock.edit().putString("pref_stock_list", json).apply()
    }
    fun createStockNotification(title:String, text:String){
        val sIntent = Intent(baseContext, MainActivity::class.java)
        val sPpendingIntent = PendingIntent.getActivity(baseContext, notificationId, sIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setFullScreenIntent(sPpendingIntent, true)
        this.createNotificationChannel(builder, notificationId)
        notificationId+=1
    }
    fun createNotificationChannel(builder: NotificationCompat.Builder, notificationId: Int){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val descriptionText = "지정한 조건에 만족하였을 때 발생시키는 알람입니다."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, channel_name, importance).apply{
                description = descriptionText
                setShowBadge(false)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.notify(notificationId, builder.build())
        }else{
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        }
    }

    fun isAvailableStock(timeNow:Date, recentAlarmDateTime: Date):Boolean{
        val calendar = Calendar.getInstance()
        calendar.time = recentAlarmDateTime
        setting.repeatHour?.let { calendar.add(Calendar.HOUR, it) }
        setting.repeatMinute?.let { calendar.add(Calendar.MINUTE, it) }
        val combinedTime:Date = calendar.time

        return combinedTime.before(timeNow)
    }
}