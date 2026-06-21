package com.ultratv.tv.nativeapp.nav

object Routes {
    const val HOME = "home"
    const val LIVE = "live"
    const val MOVIES = "movies"
    const val MOVIE_DETAIL = "movies/{id}"
    const val SERIES = "series"
    const val SERIES_DETAIL = "series/{id}"
    const val SEARCH = "search"
    const val GUIDE = "guide"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    // Player accepts a stream URL (URL-encoded) + title.
    const val PLAYER = "player?url={url}&title={title}"
    const val MOVIE_PLAYER = "movie_player?url={url}&title={title}"

    fun movieDetail(id: Long) = "movies/$id"
    fun seriesDetail(id: Long) = "series/$id"
    fun player(url: String, title: String): String {
        val u = android.util.Base64.encodeToString(url.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val t = android.util.Base64.encodeToString(title.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        return "player?url=$u&title=$t"
    }
    fun moviePlayer(url: String, title: String): String {
        val u = android.util.Base64.encodeToString(url.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val t = android.util.Base64.encodeToString(title.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        return "movie_player?url=$u&title=$t"
    }
}
