package com.joshua_afk.usagetracker.collector

import android.content.Context
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.prefs.SettingsStore

/** Wires the collector to the real event source, database and settings. */
fun createUsageCollector(context: Context): UsageCollector {
    val app = context.applicationContext
    return UsageCollector(
        source = UsageEventReader(app),
        labelFor = AppLabelResolver(app)::label,
        db = AppDatabase.get(app),
        settings = SettingsStore(app),
    )
}
