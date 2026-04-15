package com.wow

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WowPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WowProvider())
    }
}
