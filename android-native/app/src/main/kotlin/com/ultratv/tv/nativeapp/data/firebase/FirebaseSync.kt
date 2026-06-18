package com.ultratv.tv.nativeapp.data.firebase

import com.google.firebase.database.FirebaseDatabase
import com.ultratv.tv.nativeapp.data.db.CategoryDao
import com.ultratv.tv.nativeapp.data.db.CategoryEntity
import com.ultratv.tv.nativeapp.data.db.ChannelDao
import com.ultratv.tv.nativeapp.data.db.ChannelEntity
import com.ultratv.tv.nativeapp.data.db.MovieDao
import com.ultratv.tv.nativeapp.data.db.MovieEntity
import com.ultratv.tv.nativeapp.data.db.ProviderDao
import com.ultratv.tv.nativeapp.data.db.ProviderEntity
import com.ultratv.tv.nativeapp.data.db.SeriesDao
import com.ultratv.tv.nativeapp.data.db.SeriesEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSync @Inject constructor(
    private val providerDao: ProviderDao,
    private val channelDao: ChannelDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val categoryDao: CategoryDao,
) {
    suspend fun syncPlaylist(): Int {
        val ref = FirebaseDatabase.getInstance().getReference("sync/global/managedPlaylist")
        val snapshot = ref.get().await()
        if (!snapshot.exists()) return 0

        // Ensure FIREBASE provider exists
        var p = providerDao.findByIdentity("FIREBASE", "firebase", "firebase")
        val pid = if (p == null) {
            providerDao.upsert(
                ProviderEntity(
                    name = "KOBANI 4K",
                    kind = "FIREBASE",
                    baseUrl = "firebase",
                    username = "firebase",
                    password = "",
                    active = true
                )
            )
        } else {
            p.id
        }

        // Set as active
        providerDao.deactivateAll()
        providerDao.activate(pid)

        val channels = mutableListOf<ChannelEntity>()
        val movies = mutableListOf<MovieEntity>()
        val series = mutableListOf<SeriesEntity>()
        val categories = mutableListOf<CategoryEntity>()
        
        val categoryMap = mutableMapOf<String, String>() // name -> categoryId

        fun getCategoryId(name: String, kind: String): String {
            val key = "${kind}_$name"
            if (!categoryMap.containsKey(key)) {
                val catId = "cat_${categoryMap.size + 1}"
                categoryMap[key] = catId
                categories.add(CategoryEntity(
                    providerId = pid,
                    kind = kind,
                    remoteId = catId,
                    name = name,
                    locked = false
                ))
            }
            return categoryMap[key]!!
        }

        var order = 0

        // snapshot can be List or Map
        val children = snapshot.children
        children.forEach { child ->
            val name = child.child("name").value as? String ?: return@forEach
            val url = child.child("url").value as? String ?: return@forEach
            val type = child.child("type").value as? String ?: "live"
            val group = child.child("group").value as? String ?: child.child("category").value as? String ?: "General"
            val logo = child.child("logo").value as? String ?: child.child("icon_url").value as? String

            order++

            if (type == "movie") {
                val catId = getCategoryId(group, "MOVIE")
                movies.add(MovieEntity(
                    providerId = pid,
                    remoteId = "m_$order",
                    name = name,
                    poster = logo,
                    categoryId = catId,
                    streamUrl = url,
                    container = null,
                    year = null,
                    rating = null,
                    plot = null
                ))
            } else if (type == "series") {
                val catId = getCategoryId(group, "SERIES")
                series.add(SeriesEntity(
                    providerId = pid,
                    remoteId = "s_$order",
                    name = name,
                    poster = logo,
                    categoryId = catId,
                    year = null,
                    rating = null,
                    plot = null
                ))
            } else {
                val catId = getCategoryId(group, "LIVE")
                channels.add(ChannelEntity(
                    providerId = pid,
                    remoteId = "c_$order",
                    name = name,
                    logo = logo,
                    categoryId = catId,
                    streamUrl = url,
                    epgChannelId = null,
                    catchupSource = null,
                    catchupDays = 0,
                    userPosition = order
                ))
            }
        }

        categoryDao.deleteForProviderKind(pid, "LIVE")
        categoryDao.deleteForProviderKind(pid, "MOVIE")
        categoryDao.deleteForProviderKind(pid, "SERIES")
        categoryDao.upsertAll(categories)

        channelDao.deleteForProvider(pid)
        // chunk insert
        channels.chunked(500).forEach { channelDao.upsertAll(it) }

        movieDao.deleteForProvider(pid)
        movies.chunked(500).forEach { movieDao.upsertAll(it) }

        seriesDao.deleteForProvider(pid)
        series.chunked(500).forEach { seriesDao.upsertAll(it) }

        return channels.size + movies.size + series.size
    }
}
