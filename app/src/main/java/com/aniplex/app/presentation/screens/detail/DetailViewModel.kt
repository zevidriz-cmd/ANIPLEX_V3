package com.aniplex.app.presentation.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniplex.app.data.local.preferences.ProfileManager
import com.aniplex.app.domain.model.AnimeDetail
import com.aniplex.app.domain.model.Character
import com.aniplex.app.domain.model.Episode
import com.aniplex.app.domain.model.HistoryItem
import com.aniplex.app.domain.model.Result
import com.aniplex.app.domain.model.Season
import com.aniplex.app.domain.repository.AnimeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface DetailState<out T> {
    data object Loading : DetailState<Nothing>
    data class Success<out T>(val data: T) : DetailState<T>
    data class Error(val message: String) : DetailState<Nothing>
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _detailState = MutableStateFlow<DetailState<AnimeDetail>>(DetailState.Loading)
    val detailState: StateFlow<DetailState<AnimeDetail>> = _detailState.asStateFlow()

    private val _episodesState = MutableStateFlow<DetailState<List<Episode>>>(DetailState.Loading)
    val episodesState: StateFlow<DetailState<List<Episode>>> = _episodesState.asStateFlow()

    private val _charactersState = MutableStateFlow<DetailState<List<Character>>>(DetailState.Loading)
    val charactersState: StateFlow<DetailState<List<Character>>> = _charactersState.asStateFlow()

    private val _isWatchlisted = MutableStateFlow(false)
    val isWatchlisted: StateFlow<Boolean> = _isWatchlisted.asStateFlow()

    private val _watchHistory = MutableStateFlow<HistoryItem?>(null)
    val watchHistory: StateFlow<HistoryItem?> = _watchHistory.asStateFlow()

    private val _userRating = MutableStateFlow(0)
    val userRating: StateFlow<Int> = _userRating.asStateFlow()

    private val _seasonsState = MutableStateFlow<DetailState<List<Season>>>(DetailState.Loading)
    val seasonsState: StateFlow<DetailState<List<Season>>> = _seasonsState.asStateFlow()

    private val _resolvedAnikotoId = MutableStateFlow<String?>(null)
    val resolvedAnikotoId: StateFlow<String?> = _resolvedAnikotoId.asStateFlow()

    private val _resolutionError = MutableStateFlow<String?>(null)
    val resolutionError: StateFlow<String?> = _resolutionError.asStateFlow()

    fun loadAnimeData(animeId: String, forceRefresh: Boolean = false) {
        if (animeId.startsWith("mal-")) {
            val malId = animeId.substringAfter("mal-")
            resolveMALAndNavigate(malId)
            return
        }
        _detailState.value = DetailState.Loading
        _episodesState.value = DetailState.Loading
        _charactersState.value = DetailState.Loading

        viewModelScope.launch {
            // Load Anime Details
            repository.getAnimeDetail(animeId, forceRefresh).collect { result ->
                when (result) {
                    is Result.Loading -> _detailState.value = DetailState.Loading
                    is Result.Success -> {
                        _detailState.value = DetailState.Success(result.data)
                        // Load seasons when detail is available
                        if (result.data.malId.isNotBlank()) {
                            val currentSeasons = (_seasonsState.value as? DetailState.Success)?.data ?: emptyList()
                            val alreadyHasSeason = currentSeasons.any { it.malId == result.data.malId }
                            if (!alreadyHasSeason) {
                                loadSeasons(result.data.malId)
                            }
                        } else {
                            _seasonsState.value = DetailState.Success(emptyList())
                        }
                    }
                    is Result.Error -> _detailState.value = DetailState.Error(result.message)
                }
            }
        }

        viewModelScope.launch {
            // Load Episodes
            repository.getEpisodes(animeId, forceRefresh).collect { result ->
                when (result) {
                    is Result.Loading -> _episodesState.value = DetailState.Loading
                    is Result.Success -> _episodesState.value = DetailState.Success(result.data)
                    is Result.Error -> _episodesState.value = DetailState.Error(result.message)
                }
            }
        }

        viewModelScope.launch {
            // Load Characters
            repository.getCharacters(animeId).collect { result ->
                when (result) {
                    is Result.Loading -> _charactersState.value = DetailState.Loading
                    is Result.Success -> _charactersState.value = DetailState.Success(result.data)
                    is Result.Error -> _charactersState.value = DetailState.Error(result.message)
                }
            }
        }

        // Query Firestore status
        val userId = auth.currentUser?.uid
        val profileId = profileManager.activeProfile.value?.id
        if (userId != null) {
            viewModelScope.launch {
                try {
                    // 1. Check watchlist
                    val docRef = if (profileId != null) {
                        firestore.collection("users").document(userId)
                            .collection("profiles").document(profileId)
                            .collection("watchlist").document(animeId)
                    } else {
                        firestore.collection("users").document(userId)
                            .collection("watchlist").document(animeId)
                    }
                    val watchDoc = docRef.get().await()
                    _isWatchlisted.value = watchDoc.exists()
                } catch (e: Exception) {
                    _isWatchlisted.value = false
                }
            }

            viewModelScope.launch {
                try {
                    // 2. Check history
                    val docRef = if (profileId != null) {
                        firestore.collection("users").document(userId)
                            .collection("profiles").document(profileId)
                            .collection("history").document(animeId)
                    } else {
                        firestore.collection("users").document(userId)
                            .collection("history").document(animeId)
                    }
                    val histDoc = docRef.get().await()
                    if (histDoc.exists()) {
                        _watchHistory.value = HistoryItem(
                            animeId = animeId,
                            animeTitle = histDoc.getString("animeTitle") ?: "",
                            poster = histDoc.getString("poster") ?: "",
                            episodeId = histDoc.getString("episodeId") ?: "",
                            episodeNumber = histDoc.getLong("episodeNumber")?.toInt() ?: 1,
                            episodeTitle = histDoc.getString("episodeTitle") ?: "",
                            progressPosition = histDoc.getLong("progressPosition") ?: 0L,
                            totalDuration = histDoc.getLong("totalDuration") ?: 0L,
                            updatedAt = histDoc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } else {
                        _watchHistory.value = null
                    }
                } catch (e: Exception) {
                    _watchHistory.value = null
                }
            }

            viewModelScope.launch {
                try {
                    // 3. Check rating
                    val docRef = if (profileId != null) {
                        firestore.collection("users").document(userId)
                            .collection("profiles").document(profileId)
                            .collection("ratings").document(animeId)
                    } else {
                        firestore.collection("users").document(userId)
                            .collection("ratings").document(animeId)
                    }
                    val rateDoc = docRef.get().await()
                    _userRating.value = rateDoc.getLong("rating")?.toInt() ?: 0
                } catch (e: Exception) {
                    _userRating.value = 0
                }
            }
        }
    }

    fun toggleWatchlist(animeDetail: AnimeDetail) {
        val userId = auth.currentUser?.uid ?: return
        val profileId = profileManager.activeProfile.value?.id
        viewModelScope.launch {
            val docRef = if (profileId != null) {
                firestore.collection("users").document(userId)
                    .collection("profiles").document(profileId)
                    .collection("watchlist").document(animeDetail.id)
            } else {
                firestore.collection("users").document(userId)
                    .collection("watchlist").document(animeDetail.id)
            }
            if (_isWatchlisted.value) {
                try {
                    docRef.delete().await()
                    _isWatchlisted.value = false
                } catch (e: Exception) {
                    // Handle failure
                }
            } else {
                val data = hashMapOf(
                    "id" to animeDetail.id,
                    "name" to animeDetail.name,
                    "poster" to animeDetail.poster,
                    "addedAt" to System.currentTimeMillis()
                )
                try {
                    docRef.set(data).await()
                    _isWatchlisted.value = true
                } catch (e: Exception) {
                    // Handle failure
                }
            }
        }
    }

    fun setRating(animeId: String, rating: Int) {
        val userId = auth.currentUser?.uid ?: return
        val profileId = profileManager.activeProfile.value?.id
        viewModelScope.launch {
            val docRef = if (profileId != null) {
                firestore.collection("users").document(userId)
                    .collection("profiles").document(profileId)
                    .collection("ratings").document(animeId)
            } else {
                firestore.collection("users").document(userId)
                    .collection("ratings").document(animeId)
            }
            try {
                if (rating == 0) {
                    docRef.delete().await()
                    _userRating.value = 0
                } else {
                    val data = hashMapOf(
                        "rating" to rating,
                        "ratedAt" to System.currentTimeMillis()
                    )
                    docRef.set(data).await()
                    _userRating.value = rating
                }
            } catch (e: Exception) {
                // Handle failure
            }
        }
    }

    private fun loadSeasons(malId: String) {
        viewModelScope.launch {
            repository.getSeasons(malId).collect { result ->
                when (result) {
                    is Result.Loading -> _seasonsState.value = DetailState.Loading
                    is Result.Success -> _seasonsState.value = DetailState.Success(result.data)
                    is Result.Error -> _seasonsState.value = DetailState.Success(emptyList())
                }
            }
        }
    }

    fun resolveMALAndNavigate(malId: String) {
        _resolvedAnikotoId.value = null
        _resolutionError.value = null
        viewModelScope.launch {
            repository.resolveMAL(malId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.data != null) {
                            _resolvedAnikotoId.value = result.data
                        } else {
                            _resolutionError.value = "This season is not available on Megaplay yet."
                        }
                    }
                    is Result.Error -> {
                        _resolutionError.value = result.message ?: "Failed to resolve season."
                    }
                    else -> _resolvedAnikotoId.value = null
                }
            }
        }
    }

    fun clearResolvedId() {
        _resolvedAnikotoId.value = null
    }

    fun clearResolutionError() {
        _resolutionError.value = null
    }

    fun markAsWatched(animeId: String, title: String, poster: String) {
        val userId = auth.currentUser?.uid ?: return
        val profileId = profileManager.activeProfile.value?.id
        viewModelScope.launch {
            try {
                val docRef = if (profileId != null) {
                    firestore.collection("users").document(userId)
                        .collection("profiles").document(profileId)
                        .collection("history").document(animeId)
                } else {
                    firestore.collection("users").document(userId)
                        .collection("history").document(animeId)
                }
                val data = hashMapOf(
                    "animeId" to animeId,
                    "animeTitle" to title,
                    "poster" to poster,
                    "episodeId" to "",
                    "episodeNumber" to 1,
                    "episodeTitle" to "Finished Watching",
                    "progressPosition" to 100L,
                    "totalDuration" to 100L,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
                _watchHistory.value = HistoryItem(
                    animeId = animeId,
                    animeTitle = title,
                    poster = poster,
                    episodeId = "",
                    episodeNumber = 1,
                    episodeTitle = "Finished Watching",
                    progressPosition = 100L,
                    totalDuration = 100L,
                    updatedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Squelch
            }
        }
    }

    fun removeFromHistory(animeId: String) {
        val userId = auth.currentUser?.uid ?: return
        val profileId = profileManager.activeProfile.value?.id
        viewModelScope.launch {
            try {
                val docRef = if (profileId != null) {
                    firestore.collection("users").document(userId)
                        .collection("profiles").document(profileId)
                        .collection("history").document(animeId)
                } else {
                    firestore.collection("users").document(userId)
                        .collection("history").document(animeId)
                }
                docRef.delete().await()
                _watchHistory.value = null
            } catch (e: Exception) {
                // Squelch
            }
        }
    }
}
