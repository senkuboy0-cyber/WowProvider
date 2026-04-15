package com.wow

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class WowProvider : MainAPI() {
    override var name = "Wow"
    override var mainUrl = "https://www.wow.xxx"
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val document = Jsoup.connect("$mainUrl/search/$query/").get()
        return document.select(".video-item").mapNotNull { element ->
            val title = element.selectFirst(".video-title")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src") ?: return@mapNotNull null
            SearchResponse(
                title,
                href,
                this.name,
                TvType.Movie,
                posterUrl,
                null,
                null
            )
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = Jsoup.connect(url).get()
        val title = document.selectFirst(".video-title")?.text() ?: return null
        val posterUrl = document.selectFirst(".video-poster")?.attr("src") ?: return null
        val description = document.selectFirst(".video-description")?.text() ?: ""
        val tags = document.select(".video-tags a").map { it.text() }
        val year = document.selectFirst(".video-date")?.text()?.substringAfterLast(",")?.trim()?.toIntOrNull()

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            this.posterUrl = posterUrl
            this.year = year
            this.plot = description
            this.tags = tags
            this.recommendations = getRecommendations(url)
        }
    }

    private suspend fun getRecommendations(url: String): List<SearchResponse> {
        val document = Jsoup.connect(url).get()
        return document.select(".recommended-video-item").mapNotNull { element ->
            val title = element.selectFirst(".video-title")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src") ?: return@mapNotNull null
            SearchResponse(
                title,
                href,
                this.name,
                TvType.Movie,
                posterUrl,
                null,
                null
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = Jsoup.connect(data).get()
        val videoUrls = document.toString().regexFindAll("get_file[^\\s\"']+\\.mp4").map { url ->
            fixUrl(url)
        }

        videoUrls.apmap { url ->
            callback(
                ExtractorLink(
                    this.name,
                    this.name,
                    url,
                    "",
                    getQualityFromName(url)
                )
            )
        }

        return true
    }

    private fun getQualityFromName(url: String): Int {
        return when {
            "2160m" in url -> 2160
            "1080m" in url -> 1080
            "720m" in url -> 720
            "480m" in url -> 480
            else -> 360
        }
    }
}