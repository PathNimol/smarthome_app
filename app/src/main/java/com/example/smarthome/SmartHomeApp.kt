package com.example.smarthome

import android.app.Application
import com.example.smarthome.service.SmartHomeMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartHomeApp : Application(){
    override fun onCreate() {
        super.onCreate()
        // Register the notification channel once at app start
        SmartHomeMessagingService.createChannel(this)
    }
}