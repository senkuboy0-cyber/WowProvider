package com.wow

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class WowProvider : MainAPI() {
    override var name = "Wow"
    override var mainUrl = "https://www.wow.xxx"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/latest-updates/" to "Latest",
        "$mainUrl/top-rated/" to "Top Rated",
        "$mainUrl/most-popular/" to "Most Popular",
        "$mainUrl/categories/amateur/" to "Amateur",
        "$mainUrl/categories/anal/" to "Anal",
        "$mainUrl/categories/big-tits/" to "Big Tits",
        "$mainUrl/categories/blowjob/" to "Blowjob",
        "$mainUrl/categories/brunette/" to "Brunette",
        "$mainUrl/categories/blonde/" to "Blonde",
        "$mainUrl/categories/milf/" to "MILF",
        "$mainUrl/categories/teen/" to "Teen"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) {
            val base = request.data.trimEnd('/')
            if (base.contains("/categories/") || base.contains("/tags/")) {
                "$base/page/$page/"
            } else {
                "$base/$page/"
            }
        } else {
            request.data
        }
        
        val doc = app.get(url, headers = headers).document
        val items = doc.select(".thumb").mapNotNull { el ->
            val a = el.closest("a") ?: return@mapNotNull null
            val href = a.attr("href").ifBlank { return@mapNotNull null }
            val title = el.attr("alt").ifBlank { 
                a.attr("title").ifBlank { return@mapNotNull null } 
            }.trim()
            val poster = el.attr("src") ?: return@mapNotNull null
            
            if (title.isBlank() || href.contains("/models/") || href.contains("/channels/") || !href.startsWith("$mainUrl/videos/")) {
                return@mapNotNull null
            }
            
            newMovieSearchResponse(title, href, TvType.Movie) { 
                posterUrl = poster 
            }
        }
        return newHomePageResponse(request.name, items, page < 10)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search/${query.replace(" ", "-")}/relevance/", headers = headers).document
        return doc.select(".thumb").mapNotNull { el ->
            val a = el.closest("a") ?: return@mapNotNull null
            val href = a.attr("href").ifBlank { return@mapNotNull null }
            val title = el.attr("alt").ifBlank { 
                a.attr("title").ifBlank { return@mapNotNull null } 
            }.trim()
            val poster = el.attr("src") ?: return@mapNotNull null
            
            if (title.isBlank() || !href.startsWith("$mainUrl/videos/")) {
                return@mapNotNull null
            }
            
            newMovieSearchResponse(title, href, TvType.Movie) { 
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
            ?: doc.selectFirst(".video-js")?.attr("poster")
            
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
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
        
        // Keep trailing slash - it's needed for 302 redirect to work
        val videoRegex = Regex("""get_file/\d+/[a-f0-9]+/\d+/\d+/[^"'\s]+\.mp4""")
        val matches = videoRegex.findAll(html).map { it.value }.distinct().toList()
        
        if (matches.isEmpty()) {
            return false
        }
        
        val videoHeaders = headers + mapOf("Referer" to mainUrl)
        
        matches.forEach { path ->
            // Keep trailing slash
            val fullUrl = "$mainUrl/$path/"
            val quality = when {
                path.contains("2160m") -> Qualities.P2160.value
                path.contains("1080m") -> Qualities.P1080.value
                path.contains("720m") -> Qualities.P720.value
                path.contains("480m") -> Qualities.P480.value
                else -> Qualities.Unknown.value
            }
            
            callback(
                newExtractorLink(name, "$name ${quality}p", fullUrl, ExtractorLinkType.VIDEO) {
                    this.quality = quality
                    this.headers = videoHeaders
                }
            )
        }
        
        return true
    }
}