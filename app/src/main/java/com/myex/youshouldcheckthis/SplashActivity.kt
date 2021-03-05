package com.myex.youshouldcheckthis

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler

class SplashActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val handler = Handler()
        handler.postDelayed({
        startActivity(Intent(application, MainActivity::class.java))
            finish()
        }, 1500)
    }

    override fun onBackPressed() {
        //You Can't !
    }
}