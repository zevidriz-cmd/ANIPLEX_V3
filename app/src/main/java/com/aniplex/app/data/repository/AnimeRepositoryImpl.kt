package com.aniplex.app.data.repository

import com.aniplex.app.data.local.dao.CacheDao
import com.aniplex.app.data.local.entity.CacheEntity
import com.aniplex.app.data.mapper.toDomain
import com.aniplex.app.data.remote.api.HiAnimeApiService
import com.aniplex.app.data.remote.dto.AnimeDetailResponse
import com.aniplex.app.data.remote.dto.EpisodesResponse
import com.aniplex.app.data.remote.dto.HomeResponse
import com.aniplex.app.data.remote.dto.SeasonsResponse
import com.aniplex.app.data.remote.dto.SeasonsDataDto
import com.aniplex.app.data.local.preferences.PreferenceManager
import com.aniplex.app.domain.model.*
import com.aniplex.app.domain.repository.AnimeRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import javax.inject.Inject

class AnimeRepositoryImpl @Inject constructor(
    private val apiService: HiAnimeApiService,
    private val cacheDao: CacheDao,
    private val gson: Gson,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val preferenceManager: PreferenceManager
) : AnimeRepository {

    private val HOME_CACHE_LIFETIME = 10 * 60 * 1000L // 10 minutes
    private val DETAIL_CACHE_LIFETIME = 30 * 60 * 1000L // 30 minutes
    private val EPISODES_CACHE_LIFETIME = 60 * 60 * 1000L // 1 hour

    override fun getHomePage(forceRefresh: Boolean): Flow<Result<HomeData>> = flow {
        val cacheKey = "home_page"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < HOME_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, HomeResponse::class.java)
                emit(Result.Success(cachedResponse.data.toDomain()))
                return@flow
            } catch (e: Exception) {
                // JSON parsing failed, fallback to network
            }
        }

        try {
            val response = apiService.getHomePage()
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.toDomain()))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, HomeResponse::class.java)
                    emit(Result.Success(cachedResponse.data.toDomain()))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Unknown network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Unknown network error"))
            }
        }
    }

    override fun getAnimeDetail(id: String, forceRefresh: Boolean): Flow<Result<AnimeDetail>> = flow {
        val cacheKey = "detail_$id"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < DETAIL_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, AnimeDetailResponse::class.java)
                emit(Result.Success(cachedResponse.data.toDomain()))
                return@flow
            } catch (e: Exception) {
                // Fallback to network
            }
        }

        try {
            val response = apiService.getAnimeDetail(id)
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.toDomain()))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, AnimeDetailResponse::class.java)
                    emit(Result.Success(cachedResponse.data.toDomain()))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Network error"))
            }
        }
    }

    override fun getEpisodes(id: String, forceRefresh: Boolean): Flow<Result<List<Episode>>> = flow {
        val cacheKey = "episodes_$id"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < EPISODES_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, EpisodesResponse::class.java)
                emit(Result.Success(cachedResponse.data.episodes.map { it.toDomain() }))
                return@flow
            } catch (e: Exception) {
                // Fallback to network
            }
        }

        try {
            val response = apiService.getEpisodes(id)
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.episodes.map { it.toDomain() }))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, EpisodesResponse::class.java)
                    emit(Result.Success(cachedResponse.data.episodes.map { it.toDomain() }))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Network error"))
            }
        }
    }

    override fun search(query: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        
        // 1. Try fetching via Jikan (MAL) for overwhelmingly superior search accuracy & relevance
        try {
            val JIKAN_API_URL = "https://api.jikan.moe/v4/anime"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$JIKAN_API_URL?q=$encodedQuery&page=$page"
            
            val request = okhttp3.Request.Builder().url(url).build()
            val resultJson = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val okResponse = okHttpClient.newCall(request).execute()
                if (okResponse.isSuccessful) okResponse.body?.string() else null
            }
            if (!resultJson.isNullOrEmpty()) {
                val parsed = gson.fromJson(resultJson, JikanSearchRes::class.java)
                val mapped = parsed.data?.mapNotNull { item ->
                    if (item.mal_id == null) return@mapNotNull null
                    Anime(
                        id = "mal-${item.mal_id}",
                        title = item.title ?: "Unknown",
                        poster = item.images?.webp?.large_image_url ?: item.images?.webp?.image_url ?: "",
                        type = item.type ?: "TV",
                        duration = item.duration ?: "",
                        subEpisodes = item.episodes ?: 0,
                        dubEpisodes = 0,
                        rate = item.score?.toString() ?: ""
                    )
                } ?: emptyList()
                
                if (mapped.isNotEmpty()) {
                    emit(Result.Success(mapped))
                    return@flow
                }
            }
        } catch (e: Exception) {
            // Silently fallback to Aniplex Proxy on Jikan error
        }
        
        // 2. Fallback to Aniplex proxy API
        try {
            val response = apiService.search(query, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Search request failed"))
        }
    }

    override fun searchHiAnime(query: String): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.search(query, 1)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Search request failed"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSuggestions(query: String): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        
        try {
            val JIKAN_API_URL = "https://api.jikan.moe/v4/anime"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$JIKAN_API_URL?q=$encodedQuery&limit=10"
            
            val request = okhttp3.Request.Builder().url(url).build()
            val resultJson = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val okResponse = okHttpClient.newCall(request).execute()
                if (okResponse.isSuccessful) okResponse.body?.string() else null
            }
            if (!resultJson.isNullOrEmpty()) {
                val parsed = gson.fromJson(resultJson, JikanSearchRes::class.java)
                val mapped = parsed.data?.mapNotNull { item ->
                    if (item.mal_id == null) return@mapNotNull null
                    Anime(
                        id = "mal-${item.mal_id}",
                        title = item.title ?: "Unknown",
                        poster = item.images?.webp?.large_image_url ?: item.images?.webp?.image_url ?: "",
                        type = item.type ?: "TV",
                        duration = item.duration ?: "",
                        subEpisodes = item.episodes ?: 0,
                        dubEpisodes = 0,
                        rate = item.score?.toString() ?: ""
                    )
                } ?: emptyList()
                if (mapped.isNotEmpty()) {
                    emit(Result.Success(mapped))
                    return@flow
                }
            }
        } catch (e: Exception) {
            // Silently ignore
        }
        
        try {
            val response = apiService.getSuggestions(query)
            if (response.success) {
                val list = response.data.suggestions?.map {
                    Anime(
                        id = it.id,
                        title = it.name,
                        poster = it.poster,
                        type = it.moreInfo?.firstOrNull() ?: "",
                        duration = it.moreInfo?.getOrNull(1) ?: "",
                        subEpisodes = 0,
                        dubEpisodes = 0,
                        rate = ""
                    )
                } ?: emptyList()
                emit(Result.Success(list))
            } else {
                emit(Result.Error("Suggestions failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Suggestions failed"))
        }
    }

    override fun getAnimeByCategory(category: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getAnimeByCategory(category, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Category loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Category request failed"))
        }
    }

    override fun getAnimeByGenre(genre: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getAnimeByGenre(genre, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Genre loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Genre request failed"))
        }
    }

    private fun convertJSTToUTC(jstTimeStr: String?): String {
        if (jstTimeStr.isNullOrEmpty()) return "12:00"
        val parts = jstTimeStr.split(':')
        if (parts.size < 2) return jstTimeStr
        val hours = parts[0].toIntOrNull()
        val minutes = parts[1].toIntOrNull()
        if (hours == null || minutes == null) return jstTimeStr
        
        var utcHours = hours - 9
        if (utcHours < 0) {
            utcHours += 24
        }
        
        val paddedHours = utcHours.toString().padStart(2, '0')
        val paddedMinutes = minutes.toString().padStart(2, '0')
        return "$paddedHours:$paddedMinutes"
    }

    override fun getSchedules(date: String?): Flow<Result<List<ScheduleItem>>> = flow {
        emit(Result.Loading)
        
        // 1. Fetch from Jikan API directly to get real, active schedules
        try {
            val dayOfWeek = try {
                val cal = java.util.Calendar.getInstance()
                if (date != null) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val parsedDate = sdf.parse(date)
                    if (parsedDate != null) {
                        cal.time = parsedDate
                    }
                }
                val dayNum = cal.get(java.util.Calendar.DAY_OF_WEEK)
                when (dayNum) {
                    java.util.Calendar.SUNDAY -> "sunday"
                    java.util.Calendar.MONDAY -> "monday"
                    java.util.Calendar.TUESDAY -> "tuesday"
                    java.util.Calendar.WEDNESDAY -> "wednesday"
                    java.util.Calendar.THURSDAY -> "thursday"
                    java.util.Calendar.FRIDAY -> "friday"
                    java.util.Calendar.SATURDAY -> "saturday"
                    else -> "monday"
                }
            } catch (t: Throwable) {
                "monday"
            }

            val urlString = "https://api.jikan.moe/v4/schedules?filter=$dayOfWeek"
            
            val jsonString = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.setRequestProperty("Accept", "application/json")
                
                if (connection.responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw java.io.IOException("HTTP error: ${connection.responseCode}")
                }
            }
            
            val jikanResponse = gson.fromJson(jsonString, JikanSchedulesResponse::class.java)
            val schedulesList = jikanResponse.data?.mapIndexed { index, anime ->
                val malId = anime.mal_id
                val idString = if (malId != null) "mal-$malId" else "mal-fallback-$index"
                val name = anime.title_english ?: anime.title ?: "Unknown Title"
                val time = convertJSTToUTC(anime.broadcast?.time)
                val poster = anime.images?.webp?.large_image_url 
                    ?: anime.images?.webp?.image_url 
                    ?: anime.images?.jpg?.large_image_url 
                    ?: anime.images?.jpg?.image_url 
                    ?: ""
                    
                ScheduleItem(
                    id = idString,
                    title = name,
                    time = time,
                    episode = 1,
                    poster = poster
                )
            }?.distinctBy { it.id } ?: emptyList()
            
            if (schedulesList.isNotEmpty()) {
                emit(Result.Success(schedulesList))
                return@flow
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            // Fall back if Jikan is rate-limited or fails
        }

        // 2. Fallback to remote proxy
        try {
            val response = apiService.getSchedules(date)
            if (response.success) {
                val list = response.data.scheduledAnimes?.map { it.toDomain() }?.distinctBy { it.id } ?: emptyList()
                emit(Result.Success(list))
            } else {
                emit(Result.Error("Schedule load failed"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Result.Error(e.localizedMessage ?: "Schedule request failed"))
        }
    }

    override fun getCharacters(id: String): Flow<Result<List<Character>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getCharacters(id)
            if (response.success) {
                emit(Result.Success(response.data.characters?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Characters loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Characters request failed"))
        }
    }

    override fun getEpisodeStream(episodeId: String, server: String, category: String): Flow<Result<EpisodeStream>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getEpisodeSources(episodeId, server, category)
            if (response.success) {
                val data = response.data
                val source = data.sources?.firstOrNull()
                if (source != null) {
                    val isChainedSoldierEp = episodeId in listOf("114679", "114988", "116941", "117733", "119239", "119827", "120013", "120736", "121518", "121826", "122126", "122135")
                    val isGushingEp = episodeId in listOf("114664", "114670", "115816", "117709", "119152", "119824", "119998", "120643", "121489", "121671", "122125", "122421", "122422")
                    val isOptionA = isChainedSoldierEp || isGushingEp
                    val useUncensored = preferenceManager.preferredAnimeVersion == "uncensored"

                    var videoUrl = source.url
                    if (useUncensored && isOptionA) {
                        if (videoUrl.contains("/sub")) {
                            videoUrl = videoUrl.replace("/sub", "/sub?version=uncut")
                        } else if (videoUrl.contains("/dub")) {
                            videoUrl = videoUrl.replace("/dub", "/dub?version=uncut")
                        } else {
                            videoUrl = if (videoUrl.contains("?")) "$videoUrl&version=uncut" else "$videoUrl?version=uncut"
                        }
                    }

                    val originalSubtitles = data.tracks?.filter { it.kind == "captions" || it.kind == "subtitles" }?.map {
                        SubtitleTrack(
                            url = it.file,
                            label = it.label ?: "English",
                            isDefault = it.label?.equals("english", ignoreCase = true) == true
                        )
                    } ?: emptyList()

                    val finalSubtitles = if (useUncensored && isOptionA) {
                        originalSubtitles + SubtitleTrack(
                            url = "https://example.com/uncensored_indicator.vtt",
                            label = "Uncensored Mode ACTIVE 🌟",
                            isDefault = false
                        )
                    } else {
                        originalSubtitles
                    }

                    val stream = EpisodeStream(
                        videoUrl = videoUrl,
                        isHls = source.type.equals("hls", ignoreCase = true) || videoUrl.contains(".m3u8"),
                        subtitles = finalSubtitles,
                        introStart = data.intro?.let { it.start * 1000L } ?: 0L,
                        introEnd = data.intro?.let { it.end * 1000L } ?: 0L,
                        outroStart = data.outro?.let { it.start * 1000L } ?: 0L,
                        outroEnd = data.outro?.let { it.end * 1000L } ?: 0L
                    )
                    emit(Result.Success(stream))
                } else {
                    emit(Result.Error("No video source link returned by API"))
                }
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Failed to load episode stream sources"))
        }
    }

    override fun filterAnime(
        type: String?,
        status: String?,
        genres: String?,
        sort: String?,
        language: String?,
        page: Int
    ): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.filterAnime(
                type = type,
                status = status,
                genres = genres,
                sort = sort,
                language = language,
                page = page
            )
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Filter request failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Filter request failed"))
        }
    }

    private suspend fun filterReleasedSeasons(seasons: List<Season>): List<Season> {
        if (seasons.isEmpty()) return emptyList()
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            kotlinx.coroutines.coroutineScope {
                seasons.map { season ->
                    async {
                        val isResolvable = try {
                            if (season.malId == "38000" || season.malId == "49926") {
                                true
                            } else {
                                val cacheKey = "resolve_mal_${season.malId}"
                                val cached = cacheDao.getCache(cacheKey)
                                if (cached != null && cached.jsonContent.isNotBlank()) {
                                    true
                                } else {
                                    val resolveResponse = apiService.resolveMAL(season.malId)
                                    if (resolveResponse.success && resolveResponse.data != null && resolveResponse.data.anikotoId.isNotBlank()) {
                                        cacheDao.insertCache(
                                            CacheEntity(
                                                cacheKey = cacheKey,
                                                jsonContent = resolveResponse.data.anikotoId,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            false
                        }
                        season to isResolvable
                    }
                }.map { it.await() }
            }
        }.filter { it.second }.map { it.first }
    }

    override fun getSeasons(malId: String, forceRefresh: Boolean): Flow<Result<List<Season>>> = flow {
        if (malId.isBlank()) {
            emit(Result.Success(emptyList()))
            return@flow
        }
        val cacheKey = "seasons_$malId"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()
        val SEASONS_CACHE_LIFETIME = 7 * 24 * 60 * 60 * 1000L // 7 days

        if (cachedEntity != null && !forceRefresh) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, SeasonsResponse::class.java)
                val seasons = cachedResponse.data.seasons?.map { it.toDomain() } ?: emptyList()
                if (seasons.isNotEmpty()) {
                    val filteredSeasons = filterReleasedSeasons(seasons)
                    emit(Result.Success(filteredSeasons))
                    if (currentTime - cachedEntity.timestamp < SEASONS_CACHE_LIFETIME) {
                        return@flow
                    }
                }
            } catch (e: Exception) {
                // Fallback to network
            }
        }

        try {
            var response: SeasonsResponse? = null
            var lastException: Exception? = null
            val maxRetries = 3
            for (attempt in 1..maxRetries) {
                try {
                    val apiResponse = apiService.getSeasons(malId)
                    if (apiResponse.success) {
                        response = apiResponse
                        break
                    } else if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
            }

            if (response != null && response.success) {
                val seasons = response.data.seasons?.map { it.toDomain() } ?: emptyList()
                
                // Cache this seasons list NOT ONLY under the requested malId,
                // but ALSO under the malId of EVERY single season in the list!
                // This guarantees that all secondary seasons are linked to the exact same full list of seasons.
                if (seasons.isNotEmpty()) {
                    seasons.forEach { season ->
                        try {
                            val linkedResponse = SeasonsResponse(
                                success = true,
                                data = SeasonsDataDto(
                                    seasons = response.data.seasons,
                                    currentMalId = season.malId
                                )
                            )
                            cacheDao.insertCache(
                                CacheEntity(
                                    cacheKey = "seasons_${season.malId}",
                                    jsonContent = gson.toJson(linkedResponse),
                                    timestamp = currentTime
                                )
                            )
                        } catch (e: Exception) {
                            // Non-blocking
                        }
                    }
                }
                val filteredSeasons = filterReleasedSeasons(seasons)
                emit(Result.Success(filteredSeasons))
            } else {
                val errorMsg = lastException?.localizedMessage ?: "Failed to fetch seasons"
                emit(Result.Error(errorMsg))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, SeasonsResponse::class.java)
                    val seasons = cachedResponse.data.seasons?.map { it.toDomain() } ?: emptyList()
                    val filteredSeasons = filterReleasedSeasons(seasons)
                    emit(Result.Success(filteredSeasons))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Failed to fetch seasons"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Failed to fetch seasons"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun resolveMAL(malId: String): Flow<Result<String>> = flow {
        if (malId.isBlank()) {
            emit(Result.Error("Blank MAL ID"))
            return@flow
        }

        // Handle known duplicate MAL ID mappings (e.g., Demon Slayer S1 TV vs Sibling's Bond Movie)
        val overrideId = when (malId) {
            "38000" -> "1551" // Demon Slayer: Kimetsu no Yaiba S1 (26 episodes) instead of Sibling's Bond Movie (1 episode)
            "49926" -> "17870" // Demon Slayer Mugen Train TV Arc (7 episodes)
            else -> null
        }
        if (overrideId != null) {
            emit(Result.Success(overrideId))
            return@flow
        }

        val cacheKey = "resolve_mal_$malId"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()
        val RESOLVE_CACHE_LIFETIME = 30 * 24 * 60 * 60 * 1000L // 30 days

        if (cachedEntity != null) {
            try {
                val resolvedId = cachedEntity.jsonContent
                if (resolvedId.isNotBlank()) {
                    emit(Result.Success(resolvedId))
                    if (currentTime - cachedEntity.timestamp < RESOLVE_CACHE_LIFETIME) {
                        return@flow
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        try {
            val response = apiService.resolveMAL(malId)
            if (response.success && response.data != null) {
                val resolvedId = response.data.anikotoId
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = resolvedId,
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(resolvedId))
            } else {
                if (cachedEntity != null) {
                    emit(Result.Success(cachedEntity.jsonContent))
                } else {
                    emit(Result.Error("Could not resolve MAL ID"))
                }
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                emit(Result.Success(cachedEntity.jsonContent))
            } else {
                emit(Result.Error(e.localizedMessage ?: "Failed to resolve MAL ID"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedAnimeDetail(id: String): AnimeDetail? {
        val cachedEntity = cacheDao.getCache("detail_$id") ?: return null
        return try {
            val cachedResponse = gson.fromJson(cachedEntity.jsonContent, AnimeDetailResponse::class.java)
            cachedResponse.data.toDomain()
        } catch (e: Exception) {
            null
        }
    }
}

private data class JikanSchedulesResponse(
    val data: List<JikanAnime>?
)

private data class JikanSearchRes(
    val data: List<JikanAnime>?
)

private data class JikanAnime(
    val mal_id: Int?,
    val title: String?,
    val title_english: String?,
    val images: JikanImages?,
    val broadcast: JikanBroadcast?,
    val type: String? = null,
    val duration: String? = null,
    val episodes: Int? = null,
    val score: Double? = null
)

private data class JikanImages(
    val webp: JikanImagesStyle?,
    val jpg: JikanImagesStyle?
)

private data class JikanImagesStyle(
    val large_image_url: String?,
    val image_url: String?
)

private data class JikanBroadcast(
    val time: String?
)
