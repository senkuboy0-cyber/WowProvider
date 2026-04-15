cloudstream {
    setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/senkuboy0-cyber/WowProvider")
    version = 1
    description = "Wow.xxx - Free Porn Videos"
    authors = listOf("senkuboy0-cyber")
    language = "en"
    tvTypes = listOf("Movie", "TvSeries", "Others")
}

android {
    namespace = "com.wow"
}