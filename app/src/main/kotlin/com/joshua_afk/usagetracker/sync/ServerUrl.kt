package com.joshua_afk.usagetracker.sync

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** True for a full http(s) URL such as http://192.168.1.20:3000/api/usage. */
fun isValidServerUrl(url: String): Boolean = url.trim().toHttpUrlOrNull() != null
