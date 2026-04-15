package com.wow

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class WowProvider : MainAPI() {
    override var name = "Wow"
    override var mainUrl = "https://www.wow.xxx"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Others)

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/latest-updates/" to "Latest",
        "$mainUrl/top-rated/" to "Top Rated",
        "$mainUrl/most-popular/" to "Most Popular"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) {
            val base = request.data.trimEnd('/')
            "$base/$page/"
        } else {
            request.data
        }
        
        val doc = app.get(url, headers = headers).document
        val items = doc.select("div.video-item, div.thumb").mapNotNull { element ->
            val a = element.selectFirst("a[href]") ?: return@mapNotNull null
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = a.attr("title").ifBlank { a.selectFirst("span.title")?.text() }?.trim() ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("data-src")?.ifBlank { it.attr("src") } ?: return@mapNotNull null
            
            if (title.isBlank() || href.contains("/models/") || href.contains("/channels/")) return@mapNotNull null
            
            val type = if (title.contains("episode", true) || title.contains("series", true)) TvType.TvSeries else TvType.Movie
            
            newMovieSearchResponse(title, href, type) { 
                posterUrl = poster 
            }
        }
        return newHomePageResponse(request.name, items, page < 10)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search/${query.replace(" ", "-")}/relevance/", headers = headers).document
        return doc.select("div.video-item, div.thumb").mapNotNull { element ->
            val a = element.selectFirst("a[href]") ?: return@mapNotNull null
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = a.attr("title").ifBlank { a.selectFirst("span.title")?.text() }?.trim() ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("data-src")?.ifBlank { it.attr("src") } ?: return@mapNotNull null
            
            if (title.isBlank()) return@mapNotNull null
            
            val type = if (title.contains("episode", true) || title.contains("series", true)) TvType.TvSeries else TvType.Movie
            
            newMovieSearchResponse(title, href, type) { 
                posterUrl = poster 
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = headers).document
        
        val title = doc.selectFirst("h1.page-title, h1.title")?.text()?.trim() 
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
            ?: url.substringAfterLast("/").replace("-", " ")
            
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content") 
            ?: doc.selectFirst(".video-poster img")?.attr("src")
            
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content") 
            ?: doc.selectFirst(".video-info, .description")?.text()
            
        val tags = doc.select(".tags a, .video-tags a").map { it.text() }.take(10)
        
        val isSeries = title.contains("episode", true) || title.contains("series", true)

        return if (isSeries) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, listOf(Episode(url))) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = headers).document
        val html = doc.toString()
        
        val videoRegex = Regex("""get_file/\d+/[a-f0-9]+/\d+/\d+/[^"'\s]+\.mp4""")
        val matches = videoRegex.findAll(html).map { it.value }.distinct().toList()
        
        if (matches.isEmpty()) {
            return false
        }
        
        matches.forEach { path ->
            val fullUrl = "$mainUrl/$path"
            val quality = when {
                path.contains("2160m") -> 2160
                path.contains("1080m") -> 1080
                path.contains("720m") -> 720
                path.contains("480m") -> 480
                else -> 360
            }
            
            callback(
                ExtractorLink(
                    name,
                    "$name ${quality}p",
                    fullUrl,
                    mainUrl,
                    quality
                )
            )
        }
        
        return true
    }
}