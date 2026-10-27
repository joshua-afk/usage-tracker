package com.joshua_afk.usagetracker

import android.app.Application
import com.joshua_afk.usagetracker.work.Scheduler

class UsageTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scheduler.scheduleCollection(this)
    }
}
