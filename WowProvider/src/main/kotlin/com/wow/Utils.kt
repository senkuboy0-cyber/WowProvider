package com.wow

import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink

fun String.regexFindAll(regex: String): List<String> {
    return Regex(regex).findAll(this).map { it.value }.toList()
}

fun List<String>.apmap(block: suspend (String) -> Unit) {
    this.map { asyncIO { block(it) } }
}

suspend fun List<ExtractorLink>.sortByQuality(): List<ExtractorLink> {
    return this.sortedByDescending {
        when {
            it.quality == 2160 -> 4
            it.quality == 1080 -> 3
            it.quality == 720 -> 2
            it.quality == 480 -> 1
            else -> 0
        }
    }
}