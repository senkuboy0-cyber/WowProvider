package com.wow

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginContext

@CloudstreamPlugin
class WowPlugin: CloudstreamPlugin() {
    override fun load(context: PluginContext) {
        context.registerProvider(WowProvider())
    }
}