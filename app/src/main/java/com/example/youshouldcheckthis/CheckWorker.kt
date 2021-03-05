package com.example.youshouldcheckthis

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.lang.Exception


class CheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    val channel_name: String = "주식 변동"
    val CHANNEL_ID: String = "MY_CH"
    var notificationId: Int = 1002

    var isRunning = true
    var setting = Setting()
    private var listViewItemList:ArrayList<ListViewItem>? = null
    override suspend fun doWork(): Result {
        Log.i("CheckWorker", "makeCheckingCaroutine")
        isRunning = true
        while(isRunning){
            Log.i("CheckWorker","PeriodicRefreshCaroutine")
            loadAllSettingFromPreference()
            val tmp = getPreferenceStockList()
            if(tmp!=null&&((setting.decreaseAlarm == true &&setting.decreaseRateLimit?.isNaN()==false)||(setting.increaseAlarm == true &&setting.increaseRateLimit?.isNaN()==false))){
                listViewItemList = tmp!!
                var i:Int? = null
                for(i in 0 until listViewItemList!!.size){
                    if(i<listViewItemList!!.size&&listViewItemList!![i].stockAlarm){
                        refreshStockListOnBackground(i, 0)
                        delay(1000)
                    }
                }
                delay(60000)
            }
        }
        return Result.success()
    }

    fun killCheckingCaroutine(){
        isRunning=false
    }
    fun loadAllSettingFromPreference(){
        this.setting.increaseAlarm = getPreferenceSetting("setting_increase_alarm", "CheckBox").toBoolean()
        this.setting.decreaseAlarm = getPreferenceSetting("setting_decrease_alarm", "CheckBox").toBoolean()
        this.setting.increaseRateLimit = getPreferenceSetting("setting_increase_rate_limit", "EditText")?.toFloat()
        this.setting.decreaseRateLimit = getPreferenceSetting("setting_decrease_rate_limit", "EditText")?.toFloat()
    }
    fun getPreferenceSetting(strId:String, strType: String):String?{
        val prefStock: SharedPreferences = applicationContext.getSharedPreferences("pref_$strId", Context.MODE_PRIVATE)
        return prefStock.getString("pref_$strId", null)
    }
    fun getPreferenceStockList():ArrayList<ListViewItem>?{
        val prefStock: SharedPreferences = applicationContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
        val gson = GsonBuilder().create()
        val json = prefStock.getString("pref_stock_list", null) ?: return null
        val type = object: TypeToken<ArrayList<ListViewItem>>(){}.type
        return gson.fromJson(json, type)
    }
    fun refreshStockListOnBackground(index: Int, cntTry: Int){
        if(cntTry>=10){
            return
        }
        try{
            val url = "https://www.google.com/search?q=" + listViewItemList!![index].stockNameStr + "%20주가"
            val doc = Jsoup.connect(url)
                .timeout(1500)
                .get()

            val updatedAtDataText = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > div:nth-child(3) > span:nth-child(1) > span:nth-child(2)").last().text()
            val indexDate = updatedAtDataText.indexOf("일") //한국 아닌 곳에서 구글 실행 시 Date 포맷이 다름!!오류
            val dateString = updatedAtDataText.substring(0, indexDate)

            if(dateString!=listViewItemList!![index].recentAlarmDateStr){ //최근 알람 울린 게 오늘이면 안 됨
                val priceFluctuationData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2) > div:nth-child(1) > span:nth-child(2) > span:nth-child(1)").last()
                val rateData = doc.select("#knowledge-finance-wholepage__entity-summary > div > g-card-section > div > g-card-section > div:nth-child(2)> div:nth-child(1) > span:nth-child(2) > span:nth-child(2) > span:nth-child(1)").last()

                if(this.setting.increaseAlarm==true&&priceFluctuationData.text()[0]=='+'){
                    if(rateData.text().substring(1, rateData.text().length-2).toFloat()>this.setting.increaseRateLimit!!){
                        listViewItemList!![index].recentAlarmDateStr = dateString
                        this.setPreferenceStockList(listViewItemList!!)
                        this.createStockNotification("종목 알림", "["+listViewItemList!![index].stockNameStr+"] 종목 "+priceFluctuationData.text()[0]+rateData.text().substring(1, rateData.text().length-1)+" 변동")
                    }
                }else if(this.setting.decreaseAlarm==true&&priceFluctuationData.text()[0]=='−'){
                    if(rateData.text().substring(1, rateData.text().length-2).toFloat()>this.setting.decreaseRateLimit!!){
                        listViewItemList!![index].recentAlarmDateStr = dateString
                        this.setPreferenceStockList(listViewItemList!!)
                        this.createStockNotification("종목 알림", "["+listViewItemList!![index].stockNameStr+"] 종목 "+priceFluctuationData.text()[0]+rateData.text().substring(1, rateData.text().length-1)+" 변동")
                    }
                }
            }
        }catch(e:Exception){
            this.refreshStockListOnBackground(index, cntTry+1)
        }
    }

    fun setPreferenceStockList(list: ArrayList<ListViewItem>){
        val prefStock: SharedPreferences = applicationContext.getSharedPreferences("pref_stock_list", Context.MODE_PRIVATE)
        val gson = GsonBuilder().create()
        val json = gson.toJson(list)
        prefStock.edit().putString("pref_stock_list", json).apply()
    }
    fun createStockNotification(title:String, text:String){
        val sIntent = Intent(applicationContext, MainActivity::class.java)
        val sPpendingIntent = PendingIntent.getActivity(applicationContext, notificationId, sIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        var builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
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
            val descriptionText = "1번 채널입니다."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, channel_name, importance).apply{
                description = descriptionText
            }

            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            val notificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.notify(notificationId, builder.build())
        }else{
            val notificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        }
    }
}

