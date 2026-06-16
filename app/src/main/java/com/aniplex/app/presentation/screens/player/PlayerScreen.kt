package com.aniplex.app.presentation.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner as LifecycleOwnerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.aniplex.app.data.download.DownloadManager
import com.aniplex.app.data.download.DownloadStatus
import com.aniplex.app.theme.CrunchyrollOrange
import com.aniplex.app.BuildConfig
import com.aniplex.app.theme.SurfaceDark
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.graphics.Color as AndroidColor
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.compose.material.ripple.rememberRipple
import com.aniplex.app.domain.model.Episode
import com.aniplex.app.domain.model.AnimeDetail
import com.aniplex.app.domain.model.SkipTimes

private val sharedOkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()

@Stable
data class PlayerScreenState(
    val isLandscape: Boolean,
    val skipTimes: SkipTimes,
    val showSettings: Boolean,
    val episodes: List<Episode>,
    val uiState: PlayerUiState,
    val animeDetail: AnimeDetail?,
    val currentEpisode: Episode?,
    val likeCount: Int,
    val isLiked: Boolean,
    val dislikeCount: Int,
    val isDisliked: Boolean,
    val activeCategory: String,
    val playbackSpeed: Float,
    val subtitlesEnabled: Boolean,
    val qualityOption: String,
    val subtitleSizeSp: Float,
    val capturedStreamUrl: String?,
    val capturedSubtitles: List<SniffedSubtitle>,
    val currentSubtitleSelection: String,
    val durationMs: Long,
    val currentPositionMs: Long,
    val isBuffering: Boolean,
    val isPlaying: Boolean,
    val extractionState: ExtractionState,
    val exoPlayerRef: ExoPlayer?,
    val webViewRef: WebView?,
    val localResumePlayback: Boolean,
    val retryCount: Int,
    val timeoutKey: Int,
    val extractedUserAgent: String?,
    val extractedCookies: String?,
    val playbackPosition: Long,
    val downloadTaskStatus: DownloadStatus?,
    val taskProgress: Float,
    val initialProgress: Long,
    val autoplayNextEpisode: Boolean,
    val playbackError: String?,
    val retryPlaybackKey: Int,
    val isControlsVisible: Boolean,
    val controlsInteractionKey: Int,
    val gestureIndicatorType: String?,
    val gestureIndicatorValue: Float,
    val showGestureIndicator: Boolean,
    val showAudioDialog: Boolean,
    val showSubtitlesDialog: Boolean,
    val showQualityDialog: Boolean,
    val showSpeedDialog: Boolean,
    val showSubtitleSizeDialog: Boolean,
    val showVersionDialog: Boolean,
    val preferredAnimeVersion: String,
    val metadataScrollState: ScrollState,
    val settingsScrollState: ScrollState,
    val subtitleStyle: String,
    val showSubtitleStyleDialog: Boolean,
    val showEpisodesSelector: Boolean
)

data class PlayerCallbacks(
    val onBackClick: () -> Unit,
    val onAnimeClick: () -> Unit,
    val toggleLike: () -> Unit,
    val toggleDislike: () -> Unit,
    val onDownloadClick: () -> Unit,
    val onNextEpisodeClick: (() -> Unit)?,
    val onSettingsClick: () -> Unit,
    val onSettingsBackClick: () -> Unit,
    val onAutoplayChange: (Boolean) -> Unit,
    val onAudioChange: (String) -> Unit,
    val onSubtitleSelectionChange: (String) -> Unit,
    val onQualityChange: (String) -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onSubtitleSizeChange: (Float) -> Unit,
    val onRetryClick: () -> Unit,
    val onStreamUrlCaptured: (String?) -> Unit,
    val onUserAgentExtracted: (String?) -> Unit,
    val onCookiesExtracted: (String?) -> Unit,
    val onExtractionStateChanged: (ExtractionState) -> Unit,
    val onDurationChanged: (Long) -> Unit,
    val onPositionChanged: (Long) -> Unit,
    val onBufferingChanged: (Boolean) -> Unit,
    val onPlayingChanged: (Boolean) -> Unit,
    val onWebViewRefChanged: (WebView?) -> Unit,
    val onExoPlayerRefChanged: (ExoPlayer?) -> Unit,
    val onRetryCountChanged: (Int) -> Unit,
    val onTimeoutKeyChanged: (Int) -> Unit,
    val onPlaybackPositionChanged: (Long) -> Unit,
    val onSaveProgress: (progress: Long, duration: Long) -> Unit,
    val onPlaybackErrorChanged: (String?) -> Unit,
    val onRetryPlaybackKeyChanged: (Int) -> Unit,
    val onControlsVisibilityToggle: () -> Unit,
    val onControlsInteraction: () -> Unit,
    val onGestureChanged: (type: String?, value: Float, show: Boolean) -> Unit,
    val onShowAudioDialogChange: (Boolean) -> Unit,
    val onShowSubtitlesDialogChange: (Boolean) -> Unit,
    val onShowQualityDialogChange: (Boolean) -> Unit,
    val onShowSpeedDialogChange: (Boolean) -> Unit,
    val onShowSubtitleSizeDialogChange: (Boolean) -> Unit,
    val onVersionChange: (String) -> Unit,
    val onShowVersionDialogChange: (Boolean) -> Unit,
    val onSubtitleStyleChange: (String) -> Unit,
    val onShowSubtitleStyleDialogChange: (Boolean) -> Unit,
    val onShowEpisodesSelectorChange: (Boolean) -> Unit,
    val onEpisodeSelect: (Episode) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    animeId: String,
    animeTitle: String,
    episodeNumber: Int,
    category: String,
    resumePlayback: Boolean = false,
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var isLandscape by remember(configuration.orientation) {
        mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }
    var showSettings by remember { mutableStateOf(false) }

    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val animeDetail by viewModel.animeDetail.collectAsStateWithLifecycle()
    val currentEpisode by viewModel.currentEpisode.collectAsStateWithLifecycle()
    val skipTimes by viewModel.skipTimes.collectAsStateWithLifecycle()
    
    val likeCount by viewModel.likeCount.collectAsStateWithLifecycle()
    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
    val dislikeCount by viewModel.dislikeCount.collectAsStateWithLifecycle()
    val isDisliked by viewModel.isDisliked.collectAsStateWithLifecycle()

    val (activeCategory, setActiveCategory) = remember { mutableStateOf(viewModel.defaultAudioCategory) }
    
    // Playback Settings States
    var playbackSpeed by remember { mutableStateOf(viewModel.playbackSpeed) }
    var subtitlesEnabled by remember { mutableStateOf(viewModel.subtitlesEnabled) }
    var qualityOption by remember { mutableStateOf(viewModel.preferredQuality) }
    var subtitleSizeSp by remember { mutableStateOf(18f) } // Default to 18sp

    // Lifted playback and sniffer states
    var capturedStreamUrl by remember { mutableStateOf<String?>(null) }
    val capturedSubtitles = remember { mutableStateListOf<SniffedSubtitle>() }
    var currentSubtitleSelection by remember { mutableStateOf("en") }
    var durationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var extractionState by remember { mutableStateOf(ExtractionState.EXTRACTING) }
    
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var localResumePlayback by remember { mutableStateOf(resumePlayback) }
    
    var retryCount by remember { mutableStateOf(0) }
    var timeoutKey by remember { mutableStateOf(0) }
    
    var extractedUserAgent by remember { mutableStateOf<String?>(null) }
    var extractedCookies by remember { mutableStateOf<String?>(null) }
    var playbackPosition by remember { mutableStateOf(0L) }

    // Additional player states lifted from ExoVideoPlayer
    var retryPlaybackKey by remember { mutableStateOf(0) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var lastSavedProgressTime by remember { mutableStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(false) }
    var controlsInteractionKey by remember { mutableStateOf(0) }
    var gestureIndicatorType by remember { mutableStateOf<String?>(null) }
    var gestureIndicatorValue by remember { mutableStateOf(0f) }
    var showGestureIndicator by remember { mutableStateOf(false) }

    // Playback settings dialogs visibility states
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var preferredAnimeVersion by remember { mutableStateOf(viewModel.preferredAnimeVersion) }
    var subtitleStyle by remember { mutableStateOf("classic_outline") }
    var showSubtitleStyleDialog by remember { mutableStateOf(false) }
    var showEpisodesSelector by remember { mutableStateOf(false) }

    // Scroll states
    val metadataScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    val webView = remember(episodeId, activeCategory) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    val urlLower = url.lowercase()
                    val cleanUrl = url.split("?")[0].lowercase()

                    // Subtitle sniffing
                    val subtitleMime = when {
                        cleanUrl.endsWith(".vtt") -> androidx.media3.common.MimeTypes.TEXT_VTT
                        cleanUrl.endsWith(".srt") -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                        cleanUrl.endsWith(".ass") || cleanUrl.endsWith(".ssa") -> "text/x-ass"
                        else -> null
                    }

                    if (subtitleMime != null) {
                        val langCode = when {
                            urlLower.contains("eng") || urlLower.contains("english") || urlLower.contains("-en") || urlLower.contains("_en") -> "en"
                            urlLower.contains("ara") || urlLower.contains("-ar") -> "ar"
                            urlLower.contains("spa") || urlLower.contains("-es") -> "es"
                            urlLower.contains("fre") || urlLower.contains("-fr") -> "fr"
                            else -> "und"
                        }
                        Handler(Looper.getMainLooper()).post {
                            if (capturedSubtitles.none { it.url == url }) {
                                capturedSubtitles.add(SniffedSubtitle(url, subtitleMime, langCode))
                                Log.d("ANIPLEX_SUBS", "Collected Subtitle: $langCode | $url")
                            }
                        }
                    }

                    // Sniff for video URL (hls or mp4 fallback)
                    val isVideo = cleanUrl.contains(".m3u8") || cleanUrl.endsWith(".m3u8") || cleanUrl.contains(".mp4") || cleanUrl.endsWith(".mp4")
                    if (isVideo) {
                        if (capturedStreamUrl == null) {
                            DownloadManager.setStreamUrl(episodeId, url)
                            Handler(Looper.getMainLooper()).post {
                                val ua = view?.settings?.userAgentString ?: "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                                extractedUserAgent = ua
                                capturedStreamUrl = url
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    fun resetPlayerState(newEpisodeEmbedUrl: String, isNewEpisode: Boolean = true) {
        val player = exoPlayerRef
        if (player != null) {
            val pos = player.currentPosition
            val dur = player.duration
            if (pos > 0 && dur > 0) {
                val ep = currentEpisode ?: episodes.find { it.id == episodeId }
                viewModel.saveProgress(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    episodeId = ep?.id ?: episodeId,
                    episodeNumber = ep?.number ?: episodeNumber,
                    episodeTitle = ep?.title ?: "Episode ${ep?.number ?: episodeNumber}",
                    progress = pos,
                    duration = dur
                )
            }
        }

        capturedStreamUrl = null
        capturedSubtitles.clear()
        currentSubtitleSelection = "en"
        durationMs = 0L
        currentPositionMs = 0L
        isBuffering = true
        isPlaying = false
        extractionState = ExtractionState.EXTRACTING
        if (isNewEpisode) {
            playbackPosition = 0L
        }
        retryCount = 0
        timeoutKey++
        
        retryPlaybackKey = 0
        playbackError = null
        isControlsVisible = false
        showGestureIndicator = false
        
        exoPlayerRef?.stop()
        exoPlayerRef?.clearMediaItems()
        
        webView.apply {
            stopLoading()
            clearHistory()
            onResume()
            loadUrl(newEpisodeEmbedUrl)
        }
    }

    LaunchedEffect(webView) {
        webViewRef = webView
    }

    LaunchedEffect(uiState) {
        val state = uiState
        when (state) {
            is PlayerUiState.Success -> {
                capturedStreamUrl = state.stream.videoUrl
                extractionState = ExtractionState.READY
            }
            is PlayerUiState.WebViewFallback -> {
                if (extractionState != ExtractionState.READY) {
                    extractionState = ExtractionState.EXTRACTING
                }
            }
            else -> {
                extractionState = ExtractionState.EXTRACTING
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    playbackSpeed = viewModel.playbackSpeed
                    subtitlesEnabled = viewModel.subtitlesEnabled
                    qualityOption = viewModel.preferredQuality
                    setActiveCategory(viewModel.defaultAudioCategory)
                    if (localResumePlayback) exoPlayerRef?.play()
                    webViewRef?.onResume()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayerRef?.pause()
                    webViewRef?.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayerRef?.stop()
                    exoPlayerRef?.release()
                    webViewRef?.destroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Fullscreen behavior on landscape
    DisposableEffect(isLandscape) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            if (isLandscape) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Reset orientation to portrait on leaving player screen
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(episodeId, activeCategory) {
        val initialEmbedUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
            "https://animeplay.cfd/stream/s-2/$episodeId/dub"
        } else {
            "https://animeplay.cfd/stream/s-2/$episodeId"
        }
        localResumePlayback = resumePlayback
        resetPlayerState(initialEmbedUrl)
        viewModel.initialize(animeId, episodeId, activeCategory)
    }

    // Timeout sniffer LaunchedEffect
    LaunchedEffect(uiState, timeoutKey, extractionState) {
        val curUiState = uiState
        if (curUiState is PlayerUiState.WebViewFallback && extractionState == ExtractionState.EXTRACTING) {
            delay(25000) // Increase timeout to 25s
            if (capturedStreamUrl == null) {
                if (retryCount < 2) {
                    retryCount++
                    timeoutKey++
                    val retryUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                        "https://animeplay.cfd/stream/s-2/$episodeId/dub"
                    } else {
                        curUiState.embedUrl
                    }
                    webView.loadUrl(retryUrl)
                } else {
                    extractionState = ExtractionState.ERROR
                }
            }
        }
    }

    LaunchedEffect(capturedStreamUrl) {
        if (capturedStreamUrl != null) {
            // Step 1: Immediately silence all video/audio elements
            webView.evaluateJavascript("""
                (function() {
                    document.querySelectorAll('video, audio').forEach(function(el) {
                        el.pause();
                        el.muted = true;
                        el.src = '';
                    });
                    var frames = document.querySelectorAll('iframe');
                    frames.forEach(function(f) {
                        try { f.src = 'about:blank'; } catch(e) {}
                    });
                })();
            """, null)
            // Step 2: Navigate away from the embed page
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.onPause()
        }
    }

    // Cookies extraction LaunchedEffect
    LaunchedEffect(capturedStreamUrl, extractionState) {
        val stream = capturedStreamUrl
        if (stream != null && extractionState == ExtractionState.EXTRACTING) {
            delay(1000)
            
            val cookieManager = android.webkit.CookieManager.getInstance()
            val curUiState = uiState
            val embedUrl = (curUiState as? PlayerUiState.WebViewFallback)?.embedUrl ?: ""
            
            val cookiesEmbed = if (embedUrl.isNotEmpty()) cookieManager.getCookie(embedUrl) ?: "" else ""
            val cookiesMegaplay1 = cookieManager.getCookie("https://megaplay.buzz") ?: ""
            val cookiesMegaplay2 = cookieManager.getCookie("https://megaplay.buzz/") ?: ""
            val cookiesAnimeplay1 = cookieManager.getCookie("https://animeplay.cfd") ?: ""
            val cookiesAnimeplay2 = cookieManager.getCookie("https://animeplay.cfd/") ?: ""
            
            val allCookies = listOf(cookiesMegaplay1, cookiesMegaplay2, cookiesAnimeplay1, cookiesAnimeplay2, cookiesEmbed)
                .flatMap { it.split("; ") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("; ")

            Log.d("ANIPLEX_COOKIES", "Cookies captured: $allCookies")
            extractedCookies = allCookies

            delay(2000)
            extractionState = ExtractionState.READY
        }
    }

    // WebView url auto load check
    LaunchedEffect(uiState, webView) {
        val curUiState = uiState
        if (curUiState is PlayerUiState.WebViewFallback) {
            val embedUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                "https://animeplay.cfd/stream/s-2/$episodeId/dub"
            } else {
                curUiState.embedUrl
            }
            if (webView.url != embedUrl) {
                webView.loadUrl(embedUrl)
            }
        }
    }

    val downloads by DownloadManager.downloads.collectAsStateWithLifecycle()
    val downloadTask = downloads.find { it.episodeId == (currentEpisode?.id ?: episodeId) }
    val taskStatus = downloadTask?.status?.collectAsStateWithLifecycle()?.value
    val taskProgress = downloadTask?.progress?.collectAsStateWithLifecycle()?.value ?: 0f
    
    val initialProgress by viewModel.initialProgress.collectAsStateWithLifecycle()

    val exoPlayer = remember {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory).build()
    }

    LaunchedEffect(exoPlayer) {
        exoPlayerRef = exoPlayer
    }
    var hasSeekedToInitialProgress by remember(episodeId) { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isPlaying = exoPlayer.playWhenReady && playbackState == Player.STATE_READY
                if (playbackState == Player.STATE_ENDED) {
                    val currentEpNum = currentEpisode?.number ?: episodeNumber
                    val nextEp = episodes.find { it.number == currentEpNum + 1 }
                    if (viewModel.autoplayNextEpisode && nextEp != null) {
                        val nextUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                            "https://animeplay.cfd/stream/s-2/${nextEp.id}/dub"
                        } else {
                            "https://animeplay.cfd/stream/s-2/${nextEp.id}"
                        }
                        localResumePlayback = false
                        resetPlayerState(nextUrl)
                        viewModel.initialize(animeId, nextEp.id, activeCategory)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlayerError(error: PlaybackException) {
                val isSourceError = error.errorCode in 2000..2999
                if (isSourceError) {
                    if (retryCount < 2) {
                        retryCount++
                        capturedStreamUrl = null
                        extractedUserAgent = null
                        extractedCookies = null
                        capturedSubtitles.clear()
                        extractionState = ExtractionState.EXTRACTING
                        timeoutKey++
                        val retryUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                            "https://animeplay.cfd/stream/s-2/$episodeId/dub"
                        } else {
                            (uiState as? PlayerUiState.WebViewFallback)?.embedUrl ?: "https://animeplay.cfd/stream/s-2/$episodeId"
                        }
                        webView.loadUrl(retryUrl)
                    } else {
                        extractionState = ExtractionState.ERROR
                    }
                } else {
                    playbackError = error.localizedMessage ?: "Playback error"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            
            // Save progress of the last active episode
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 0 && dur > 0) {
                val ep = currentEpisode ?: episodes.find { it.id == episodeId }
                viewModel.saveProgress(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    episodeId = ep?.id ?: episodeId,
                    episodeNumber = ep?.number ?: episodeNumber,
                    episodeTitle = ep?.title ?: "Episode ${ep?.number ?: episodeNumber}",
                    progress = pos,
                    duration = dur
                )
            }
            
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(capturedStreamUrl, retryPlaybackKey) {
        val stream = capturedStreamUrl
        if (stream != null) {
            val userAgentHeader = extractedUserAgent ?: "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
            val requestProperties = mapOf(
                "Referer" to "https://megaplay.buzz/",
                "Origin" to "https://megaplay.buzz",
                "Cookie" to (extractedCookies ?: ""),
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9"
            )

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgentHeader)
                .setDefaultRequestProperties(requestProperties)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(25000)
                .setReadTimeoutMs(25000)

            val subtitleConfigs = capturedSubtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(sub.mime)
                    .setLanguage(sub.langCode)
                    .setLabel(if (sub.langCode == "en") "English" else "Language: ${sub.langCode}")
                    .setSelectionFlags(if (sub.langCode == "en") C.SELECTION_FLAG_DEFAULT else 0)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .build()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(stream)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)

            // Apply Subtitle Defaults (Keep trackSelectionParameters logic)
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, currentSubtitleSelection == "off")
                .setPreferredTextLanguage(if (currentSubtitleSelection == "off") "en" else currentSubtitleSelection)
                .build()

            if (!hasSeekedToInitialProgress && localResumePlayback && initialProgress > 0L) {
                exoPlayer.seekTo(initialProgress)
                hasSeekedToInitialProgress = true
                Log.d("ANIPLEX_PLAYER", "Successfully seeked inside stream LaunchedEffect to saved progress: $initialProgress")
            }

            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // Only trigger this effect when the initialProgress actually arrives from Firestore or the player ref changes
    LaunchedEffect(initialProgress, exoPlayerRef, capturedStreamUrl) {
        if (!hasSeekedToInitialProgress && localResumePlayback && initialProgress > 0L && capturedStreamUrl != null) {
            val player = exoPlayerRef
            if (player != null) {
                player.seekTo(initialProgress)
                hasSeekedToInitialProgress = true
                Log.d("ANIPLEX_PLAYER", "Successfully seeked to async saved progress: $initialProgress")
            }
        }
    }

    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(currentSubtitleSelection, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val disabled = currentSubtitleSelection == "off"
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, disabled)
        if (!disabled) {
            builder.setPreferredTextLanguage(currentSubtitleSelection)
        }
        player.trackSelectionParameters = builder.build()
    }

    LaunchedEffect(qualityOption, activeCategory, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val builder = player.trackSelectionParameters.buildUpon()
        if (qualityOption.contains("1080")) {
            builder.setMaxVideoSize(1920, 1080)
        } else if (qualityOption.contains("720")) {
            builder.setMaxVideoSize(1280, 720)
        } else if (qualityOption.contains("480")) {
            builder.setMaxVideoSize(854, 480)
        } else if (qualityOption.contains("360")) {
            builder.setMaxVideoSize(640, 360)
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        }
        builder.setPreferredAudioLanguage(if (activeCategory == "dub") "en" else "ja")
        player.trackSelectionParameters = builder.build()
    }

    LaunchedEffect(exoPlayer, skipTimes) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            delay(500)
            currentPositionMs = player.currentPosition
            if (player.duration > 0L) {
                durationMs = player.duration
            }
            
            // Auto skipping is disabled per user request so videos always start at 0:00 and manual skip buttons can be clicked instead.
            
            val now = System.currentTimeMillis()
            if (now - lastSavedProgressTime >= 30000L && player.isPlaying) {
                lastSavedProgressTime = now
                val ep = currentEpisode ?: episodes.find { it.id == episodeId }
                viewModel.saveProgress(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    episodeId = ep?.id ?: episodeId,
                    episodeNumber = ep?.number ?: episodeNumber,
                    episodeTitle = ep?.title ?: "Episode ${ep?.number ?: episodeNumber}",
                    progress = player.currentPosition,
                    duration = player.duration
                )
            }
        }
    }

    LaunchedEffect(isControlsVisible, isPlaying, controlsInteractionKey) {
        if (isControlsVisible && isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    LaunchedEffect(showGestureIndicator, gestureIndicatorType, gestureIndicatorValue) {
        if (showGestureIndicator) {
            delay(1000)
            showGestureIndicator = false
        }
    }

    BackHandler(enabled = isLandscape || showSettings) {
        if (showSettings) {
            showSettings = false
        } else if (isLandscape) {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val window = activity?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            isLandscape = false
        }
    }

    val localFile = remember(currentEpisode?.id ?: episodeId) { 
        DownloadManager.getDownloadedFile(context, currentEpisode?.id ?: episodeId) 
    }
    val effectiveUiState = remember(uiState, localFile) {
        if (localFile != null && localFile.exists()) {
            PlayerUiState.Success(
                com.aniplex.app.domain.model.EpisodeStream(
                    videoUrl = "file://${localFile.absolutePath}",
                    isHls = localFile.name.endsWith(".m3u8")
                )
            )
        } else {
            uiState
        }
    }

    val currentEpNum = currentEpisode?.number ?: episodeNumber
    val nextEp = episodes.find { it.number == currentEpNum + 1 }
    val onNextEpisodeClick = nextEp?.let { ep ->
        {
            val nextUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                "https://animeplay.cfd/stream/s-2/${ep.id}/dub"
            } else {
                "https://animeplay.cfd/stream/s-2/${ep.id}"
            }
            localResumePlayback = false
            resetPlayerState(nextUrl)
            viewModel.initialize(animeId, ep.id, activeCategory)
        }
    }

    val state = PlayerScreenState(
        isLandscape = isLandscape,
        skipTimes = skipTimes,
        showSettings = showSettings,
        episodes = episodes,
        uiState = effectiveUiState,
        animeDetail = animeDetail,
        currentEpisode = currentEpisode,
        likeCount = likeCount,
        isLiked = isLiked,
        dislikeCount = dislikeCount,
        isDisliked = isDisliked,
        activeCategory = activeCategory,
        playbackSpeed = playbackSpeed,
        subtitlesEnabled = subtitlesEnabled,
        qualityOption = qualityOption,
        subtitleSizeSp = subtitleSizeSp,
        capturedStreamUrl = capturedStreamUrl,
        capturedSubtitles = capturedSubtitles,
        currentSubtitleSelection = currentSubtitleSelection,
        durationMs = durationMs,
        currentPositionMs = currentPositionMs,
        isBuffering = isBuffering,
        isPlaying = isPlaying,
        extractionState = extractionState,
        exoPlayerRef = exoPlayer,
        webViewRef = webView,
        localResumePlayback = localResumePlayback,
        retryCount = retryCount,
        timeoutKey = timeoutKey,
        extractedUserAgent = extractedUserAgent,
        extractedCookies = extractedCookies,
        playbackPosition = playbackPosition,
        downloadTaskStatus = taskStatus,
        taskProgress = taskProgress,
        initialProgress = initialProgress,
        autoplayNextEpisode = viewModel.autoplayNextEpisode,
        playbackError = playbackError,
        retryPlaybackKey = retryPlaybackKey,
        isControlsVisible = isControlsVisible,
        controlsInteractionKey = controlsInteractionKey,
        gestureIndicatorType = gestureIndicatorType,
        gestureIndicatorValue = gestureIndicatorValue,
        showGestureIndicator = showGestureIndicator,
        showAudioDialog = showAudioDialog,
        showSubtitlesDialog = showSubtitlesDialog,
        showQualityDialog = showQualityDialog,
        showSpeedDialog = showSpeedDialog,
        showSubtitleSizeDialog = showSubtitleSizeDialog,
        showVersionDialog = showVersionDialog,
        preferredAnimeVersion = preferredAnimeVersion,
        metadataScrollState = metadataScrollState,
        settingsScrollState = settingsScrollState,
        subtitleStyle = subtitleStyle,
        showSubtitleStyleDialog = showSubtitleStyleDialog,
        showEpisodesSelector = showEpisodesSelector
    )

    val callbacks = PlayerCallbacks(
        onBackClick = onBackClick,
        onAnimeClick = { onAnimeClick(animeId) },
        toggleLike = { viewModel.toggleLike() },
        toggleDislike = { viewModel.toggleDislike() },
        onDownloadClick = {
            val epId = currentEpisode?.id ?: episodeId
            val epTitle = currentEpisode?.title ?: "Episode $currentEpNum"
            val poster = animeDetail?.poster ?: ""
            when (taskStatus) {
                null -> {
                    val streamUrl = DownloadManager.getStreamUrl(epId)
                    if (streamUrl == null) {
                        Toast.makeText(context, "Waiting for stream URL... Try again in a moment.", Toast.LENGTH_SHORT).show()
                    } else {
                        DownloadManager.startDownload(
                            context = context,
                            episodeId = epId,
                            animeId = animeId,
                            animeTitle = animeTitle,
                            episodeNumber = currentEpNum,
                            episodeTitle = epTitle,
                            posterUrl = poster,
                            videoUrl = streamUrl
                        )
                        Toast.makeText(context, "Download started for Episode $currentEpNum", Toast.LENGTH_SHORT).show()
                    }
                }
                DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                    DownloadManager.pauseDownload(context, epId)
                }
                DownloadStatus.PAUSED -> {
                    val streamUrl = DownloadManager.getStreamUrl(epId)
                    DownloadManager.resumeDownload(context, epId, streamUrl)
                }
                DownloadStatus.COMPLETED -> {
                    Toast.makeText(context, "Episode $currentEpNum is already downloaded!", Toast.LENGTH_SHORT).show()
                }
                DownloadStatus.FAILED -> {
                    val streamUrl = DownloadManager.getStreamUrl(epId)
                    if (streamUrl == null) {
                        Toast.makeText(context, "Waiting for stream URL... Try again in a moment.", Toast.LENGTH_SHORT).show()
                    } else {
                        DownloadManager.startDownload(
                            context = context,
                            episodeId = epId,
                            animeId = animeId,
                            animeTitle = animeTitle,
                            episodeNumber = currentEpNum,
                            episodeTitle = epTitle,
                            posterUrl = poster,
                            videoUrl = streamUrl
                        )
                    }
                }
            }
        },
        onNextEpisodeClick = onNextEpisodeClick,
        onSettingsClick = { showSettings = true },
        onSettingsBackClick = { showSettings = false },
        onAutoplayChange = { viewModel.autoplayNextEpisode = it },
        onAudioChange = {
            viewModel.defaultAudioCategory = it
            setActiveCategory(it)
        },
        onSubtitleSelectionChange = { currentSubtitleSelection = it },
        onQualityChange = {
            viewModel.preferredQuality = it
            qualityOption = it
        },
        onSpeedChange = {
            viewModel.playbackSpeed = it
            playbackSpeed = it
        },
        onSubtitleSizeChange = { subtitleSizeSp = it },
        onRetryClick = {
            val retryUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                "https://animeplay.cfd/stream/s-2/${currentEpisode?.id ?: episodeId}/dub"
            } else {
                (effectiveUiState as? PlayerUiState.WebViewFallback)?.embedUrl ?: "https://animeplay.cfd/stream/s-2/${currentEpisode?.id ?: episodeId}"
            }
            resetPlayerState(retryUrl, isNewEpisode = false)
        },
        onStreamUrlCaptured = { capturedStreamUrl = it },
        onUserAgentExtracted = { extractedUserAgent = it },
        onCookiesExtracted = { extractedCookies = it },
        onExtractionStateChanged = { extractionState = it },
        onDurationChanged = { durationMs = it },
        onPositionChanged = { currentPositionMs = it },
        onBufferingChanged = { isBuffering = it },
        onPlayingChanged = { isPlaying = it },
        onWebViewRefChanged = { webViewRef = it },
        onExoPlayerRefChanged = { exoPlayerRef = it },
        onRetryCountChanged = { retryCount = it },
        onTimeoutKeyChanged = { timeoutKey = it },
        onPlaybackPositionChanged = { playbackPosition = it },
        onSaveProgress = { pos, dur ->
            val ep = currentEpisode ?: episodes.find { it.id == episodeId }
            viewModel.saveProgress(
                animeId = animeId,
                animeTitle = animeTitle,
                episodeId = ep?.id ?: episodeId,
                episodeNumber = ep?.number ?: episodeNumber,
                episodeTitle = ep?.title ?: "Episode ${ep?.number ?: episodeNumber}",
                progress = pos,
                duration = dur
            )
        },
        onPlaybackErrorChanged = { playbackError = it },
        onRetryPlaybackKeyChanged = { retryPlaybackKey = it },
        onControlsVisibilityToggle = {
            isControlsVisible = !isControlsVisible
            controlsInteractionKey++
        },
        onControlsInteraction = { controlsInteractionKey++ },
        onGestureChanged = { type, value, show ->
            gestureIndicatorType = type
            gestureIndicatorValue = value
            showGestureIndicator = show
        },
        onShowAudioDialogChange = { showAudioDialog = it },
        onShowSubtitlesDialogChange = { showSubtitlesDialog = it },
        onShowQualityDialogChange = { showQualityDialog = it },
        onShowSpeedDialogChange = { showSpeedDialog = it },
        onShowSubtitleSizeDialogChange = { showSubtitleSizeDialog = it },
        onVersionChange = { targetVersion ->
            viewModel.setPreferredAnimeVersion(targetVersion) { msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                preferredAnimeVersion = viewModel.preferredAnimeVersion
            }
        },
        onShowVersionDialogChange = { showVersionDialog = it },
        onSubtitleStyleChange = { subtitleStyle = it },
        onShowSubtitleStyleDialogChange = { showSubtitleStyleDialog = it },
        onShowEpisodesSelectorChange = { showEpisodesSelector = it },
        onEpisodeSelect = { clickedEp ->
            val nextUrl = if (activeCategory.equals("dub", ignoreCase = true)) {
                "https://animeplay.cfd/stream/s-2/${clickedEp.id}/dub"
            } else {
                "https://animeplay.cfd/stream/s-2/${clickedEp.id}"
            }
            localResumePlayback = false
            resetPlayerState(nextUrl)
            viewModel.initialize(animeId, clickedEp.id, activeCategory)
        }
    )

    PlayerScreenContent(
        state = state,
        callbacks = callbacks,
        modifier = modifier
    )
}

@Composable
private fun PlayerScreenContent(
    state: PlayerScreenState,
    callbacks: PlayerCallbacks,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerContainer(
                state = state,
                callbacks = callbacks,
                modifier = if (state.isLandscape) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }
            )
            
            if (!state.isLandscape) {
                // Metadata scrollable section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                        .verticalScroll(state.metadataScrollState)
                ) {
                    val animeTitle = state.animeDetail?.name ?: ""
                    val currentEpNum = state.currentEpisode?.number ?: 1
                    
                    Text(
                        text = animeTitle,
                        color = CrunchyrollOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { callbacks.onAnimeClick() }
                            .padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "E$currentEpNum - ${state.currentEpisode?.title ?: "Episode $currentEpNum"}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tags
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "16+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (state.activeCategory.equals("dub", true)) "Dubbed" else "Subtitled", color = Color.LightGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            // Like
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { callbacks.toggleLike() }) {
                                Icon(
                                    imageVector = if (state.isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                    contentDescription = "Like",
                                    tint = if (state.isLiked) CrunchyrollOrange else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatCount(state.likeCount),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Dislike
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { callbacks.toggleDislike() }) {
                                Icon(
                                    imageVector = if (state.isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                    contentDescription = "Dislike",
                                    tint = if (state.isDisliked) CrunchyrollOrange else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatCount(state.dislikeCount),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = callbacks.onDownloadClick) {
                                when (state.downloadTaskStatus) {
                                    null -> {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                                    }
                                    DownloadStatus.QUEUED -> {
                                        CircularProgressIndicator(
                                            color = CrunchyrollOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    DownloadStatus.DOWNLOADING -> {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                progress = { state.taskProgress },
                                                color = CrunchyrollOrange,
                                                trackColor = Color.DarkGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    DownloadStatus.PAUSED -> {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                progress = { state.taskProgress },
                                                color = Color.Gray,
                                                trackColor = Color.DarkGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Resume",
                                                tint = CrunchyrollOrange,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    DownloadStatus.COMPLETED -> {
                                        Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = Color.Green)
                                    }
                                    DownloadStatus.FAILED -> {
                                        Icon(Icons.Default.Error, contentDescription = "Failed", tint = Color.Red)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text(
                        text = state.animeDetail?.description ?: "No description available.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Content Advisory
                    Text(text = "Content Advisory", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "16+", color = Color.Gray, fontSize = 14.sp)
                    Text(text = "Profanity, Suggestive Dialogue", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Next Episode
                    Text(text = "Next Episode", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val nextEp = state.episodes.find { it.number == currentEpNum + 1 }
                    if (nextEp != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    callbacks.onNextEpisodeClick?.invoke()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier
                                .width(160.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))) {
                                AsyncImage(
                                    model = state.animeDetail?.poster,
                                    contentDescription = nextEp.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${nextEp.number}. ${nextEp.title}",
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                Toast.makeText(context, "Download started for Episode ${nextEp.number}", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download Next Episode", tint = Color.White)
                            }
                        }
                    } else {
                        Text("No further episodes.", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "All Episodes",
                        color = CrunchyrollOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { callbacks.onBackClick() }
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = state.showSettings,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            PlaybackSettingsOverlay(
                state = state,
                callbacks = callbacks
            )
        }

        AnimatedVisibility(
            visible = state.showEpisodesSelector,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            InPlayerEpisodeSelectorOverlay(
                state = state,
                callbacks = callbacks
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}

data class SniffedSubtitle(val url: String, val mime: String, val langCode: String)

enum class ExtractionState {
    EXTRACTING,
    READY,
    ERROR
}

@Composable
fun PlayerContainer(
    state: PlayerScreenState,
    callbacks: PlayerCallbacks,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black)) {
        // Hidden WebView (always present, never disposed)
        AndroidView(
            factory = { state.webViewRef!! },
            modifier = Modifier.size(1.dp)
        )

        Crossfade(targetState = state.uiState, label = "PlayerStateTransition") { uiState ->
            when (uiState) {
                is PlayerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CrunchyrollOrange)
                    }
                }
                is PlayerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Failed to load stream: ${uiState.message}", color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = callbacks.onBackClick, colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)) {
                                Text("Go Back", color = Color.White)
                            }
                        }
                    }
                }
                is PlayerUiState.Success, is PlayerUiState.WebViewFallback -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state.extractionState) {
                            ExtractionState.EXTRACTING -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = CrunchyrollOrange)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Loading stream...",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            ExtractionState.ERROR -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Extraction failed. HLS stream not found.",
                                            color = Color.Red,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = callbacks.onRetryClick,
                                            colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)
                                        ) {
                                            Text("Retry", color = Color.White)
                                        }
                                    }
                                }
                            }
                            ExtractionState.READY -> {
                                if (state.capturedStreamUrl != null) {
                                    ExoVideoPlayer(
                                        state = state,
                                        callbacks = callbacks,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoVideoPlayer(
    state: PlayerScreenState,
    callbacks: PlayerCallbacks,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var scrubbingPositionMs by remember { mutableStateOf<Long?>(null) }
    var showChaptersSelector by remember { mutableStateOf(false) }
    val exoPlayer = state.exoPlayerRef

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.playbackError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Playback Error: ${state.playbackError}", color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            callbacks.onPlaybackErrorChanged(null)
                            callbacks.onRetryPlaybackKeyChanged(state.retryPlaybackKey + 1)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        visibility = android.view.View.VISIBLE
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        player?.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(pState: Int) {
                                if (pState == Player.STATE_READY) {
                                    this@apply.invalidate()
                                    this@apply.requestLayout()
                                }
                            }
                        })

                        subtitleView?.visibility = android.view.View.VISIBLE
                        subtitleView?.apply {
                            val styleCompat = when (state.subtitleStyle) {
                                "yellow" -> androidx.media3.ui.CaptionStyleCompat(
                                    AndroidColor.YELLOW,
                                    AndroidColor.TRANSPARENT,
                                    AndroidColor.TRANSPARENT,
                                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                    AndroidColor.BLACK,
                                    null
                                )
                                "cyan" -> androidx.media3.ui.CaptionStyleCompat(
                                    AndroidColor.CYAN,
                                    AndroidColor.TRANSPARENT,
                                    AndroidColor.TRANSPARENT,
                                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    AndroidColor.BLACK,
                                    null
                                )
                                "classic_outline" -> androidx.media3.ui.CaptionStyleCompat(
                                    AndroidColor.WHITE,
                                    AndroidColor.TRANSPARENT,
                                    AndroidColor.TRANSPARENT,
                                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    AndroidColor.BLACK,
                                    null
                                )
                                "caption_box" -> androidx.media3.ui.CaptionStyleCompat(
                                    AndroidColor.WHITE,
                                    AndroidColor.parseColor("#99000000"),
                                    AndroidColor.TRANSPARENT,
                                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE,
                                    AndroidColor.BLACK,
                                    null
                                )
                                else -> androidx.media3.ui.CaptionStyleCompat(
                                    AndroidColor.WHITE,
                                    AndroidColor.TRANSPARENT,
                                    AndroidColor.TRANSPARENT,
                                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                    AndroidColor.BLACK,
                                    null
                                )
                            }
                            setStyle(styleCompat)
                            setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, state.subtitleSizeSp)
                        }
                    }
                },
                update = { view ->
                    val styleCompat = when (state.subtitleStyle) {
                        "yellow" -> androidx.media3.ui.CaptionStyleCompat(
                            AndroidColor.YELLOW,
                            AndroidColor.TRANSPARENT,
                            AndroidColor.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            AndroidColor.BLACK,
                            null
                        )
                        "cyan" -> androidx.media3.ui.CaptionStyleCompat(
                            AndroidColor.CYAN,
                            AndroidColor.TRANSPARENT,
                            AndroidColor.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            AndroidColor.BLACK,
                            null
                        )
                        "classic_outline" -> androidx.media3.ui.CaptionStyleCompat(
                            AndroidColor.WHITE,
                            AndroidColor.TRANSPARENT,
                            AndroidColor.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            AndroidColor.BLACK,
                            null
                        )
                        "caption_box" -> androidx.media3.ui.CaptionStyleCompat(
                            AndroidColor.WHITE,
                            AndroidColor.parseColor("#99000000"),
                            AndroidColor.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE,
                            AndroidColor.BLACK,
                            null
                        )
                        else -> androidx.media3.ui.CaptionStyleCompat(
                            AndroidColor.WHITE,
                            AndroidColor.TRANSPARENT,
                            AndroidColor.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            AndroidColor.BLACK,
                            null
                        )
                    }
                    view.subtitleView?.setStyle(styleCompat)
                    view.subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, state.subtitleSizeSp)
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                callbacks.onControlsVisibilityToggle()
                            },
                            onDoubleTap = { offset ->
                                if (exoPlayer != null) {
                                    val halfWidth = size.width / 2
                                    if (offset.x < halfWidth) {
                                        val targetPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                        exoPlayer.seekTo(targetPos)
                                        callbacks.onPositionChanged(targetPos)
                                    } else {
                                        val targetPos = (exoPlayer.currentPosition + 10000).coerceAtMost(state.durationMs)
                                        exoPlayer.seekTo(targetPos)
                                        callbacks.onPositionChanged(targetPos)
                                    }
                                    callbacks.onControlsInteraction()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        val edgeThresholdPx = 50.dp.toPx()
                        var dragStartValue = 0f
                        var isLeftHalf = false
                        var isDragActiveByAgent = false
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                val screenWidth = size.width
                                val screenHeight = size.height
                                val startX = offset.x
                                val startY = offset.y

                                // Completely ignore touches starting near the screen edges
                                if (startX < edgeThresholdPx || startX > screenWidth - edgeThresholdPx ||
                                    startY < edgeThresholdPx || startY > screenHeight - edgeThresholdPx) {
                                    isDragActiveByAgent = false
                                    return@detectVerticalDragGestures
                                }

                                isDragActiveByAgent = true
                                val halfWidth = screenWidth / 2f
                                isLeftHalf = startX < halfWidth

                                if (isLeftHalf) {
                                    val lp = activity?.window?.attributes
                                    val currentBrightness = lp?.screenBrightness ?: -1f
                                    dragStartValue = if (currentBrightness < 0) 0.5f else currentBrightness
                                } else {
                                    val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                    dragStartValue = currentVol.toFloat() / maxVolume.toFloat()
                                }

                                callbacks.onGestureChanged(
                                    if (isLeftHalf) "brightness" else "volume",
                                    dragStartValue,
                                    true
                                )
                            },
                            onDragEnd = {
                                isDragActiveByAgent = false
                                callbacks.onGestureChanged(null, 0f, false)
                            },
                            onDragCancel = {
                                isDragActiveByAgent = false
                                callbacks.onGestureChanged(null, 0f, false)
                            },
                            onVerticalDrag = { change, dragAmountY ->
                                if (!isDragActiveByAgent) return@detectVerticalDragGestures

                                val screenHeight = size.height
                                val delta = -dragAmountY / screenHeight.toFloat()

                                change.consume()

                                if (isLeftHalf) {
                                    val newBrightness = (dragStartValue + delta).coerceIn(0.01f, 1f)
                                    dragStartValue = newBrightness
                                    activity?.runOnUiThread {
                                        val lp = activity.window.attributes
                                        lp.screenBrightness = newBrightness
                                        activity.window.attributes = lp
                                    }
                                    callbacks.onGestureChanged("brightness", newBrightness, true)
                                } else {
                                    val newVolumeFraction = (dragStartValue + delta).coerceIn(0f, 1f)
                                    dragStartValue = newVolumeFraction
                                    val targetVolume = (newVolumeFraction * maxVolume).toInt()
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                    callbacks.onGestureChanged("volume", newVolumeFraction, true)
                                }
                            }
                        )
                    }
            )

            AnimatedVisibility(
                visible = state.isControlsVisible,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isLandscape) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                            ) {
                                val animeTitle = state.animeDetail?.name ?: ""
                                val currentEpNum = state.currentEpisode?.number ?: 1
                                Text(
                                    text = animeTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable { callbacks.onAnimeClick() }
                                        .padding(vertical = 4.dp)
                                )
                                Text(
                                    text = state.currentEpisode?.title ?: "Episode $currentEpNum",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            IconButton(
                                onClick = callbacks.onBackClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Sub/Dub Quick Toggle Button
                            Button(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    val nextCat = if (state.activeCategory.equals("dub", ignoreCase = true)) "sub" else "dub"
                                    callbacks.onAudioChange(nextCat)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    contentColor = CrunchyrollOrange
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrunchyrollOrange),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = if (state.activeCategory.equals("dub", ignoreCase = true)) "DUB" else "SUB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // In-Player Episode Selector Trigger
                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    callbacks.onShowEpisodesSelectorChange(true)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Select Episode",
                                    tint = CrunchyrollOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (callbacks.onNextEpisodeClick != null) {
                                IconButton(
                                    onClick = {
                                        callbacks.onControlsInteraction()
                                        callbacks.onNextEpisodeClick.invoke()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = "Cast",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = callbacks.onSettingsClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    if (state.isLandscape) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (!state.isBuffering && exoPlayer != null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    val targetPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(targetPos)
                                    callbacks.onPositionChanged(targetPos)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    exoPlayer.playWhenReady = !exoPlayer.isPlaying
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    val targetPos = (exoPlayer.currentPosition + 10000).coerceAtMost(state.durationMs)
                                    exoPlayer.seekTo(targetPos)
                                    callbacks.onPositionChanged(targetPos)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = scrubbingPositionMs != null,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(150)) { it / 2 },
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(150)) { it / 2 },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            scrubbingPositionMs?.let { pos ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    ScrubbingThumbnail(
                                        videoUrl = state.capturedStreamUrl,
                                        positionMs = pos,
                                        durationMs = state.durationMs,
                                        fallbackImageUrl = state.animeDetail?.poster,
                                        modifier = Modifier.border(1.5.dp, Color(0xFFF5A623), RoundedCornerShape(8.dp))
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.85f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = Color(0xFFF5A623),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = formatTime(pos),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayPositionMs = scrubbingPositionMs ?: state.currentPositionMs

                            Text(
                                text = formatTime(displayPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Slider(
                                    value = if (state.durationMs > 0) (displayPositionMs.toFloat() / state.durationMs.toFloat()) else 0f,
                                    onValueChange = { fraction ->
                                        if (state.durationMs > 0) {
                                            callbacks.onControlsInteraction()
                                            val targetPos = (fraction * state.durationMs).toLong()
                                            scrubbingPositionMs = targetPos
                                        }
                                    },
                                    onValueChangeFinished = {
                                        val finalPos = scrubbingPositionMs
                                        if (finalPos != null && exoPlayer != null) {
                                            exoPlayer.seekTo(finalPos)
                                            callbacks.onPositionChanged(finalPos)
                                        }
                                        scrubbingPositionMs = null
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFF5A623),
                                        activeTrackColor = Color(0xFFF5A623),
                                        inactiveTrackColor = Color(0xFF4D4D4D)
                                    ),
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(Color(0xFFF5A623), CircleShape)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Segment markers ticks overlay
                                if (state.durationMs > 0) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                                        val width = maxWidth
                                        if (state.skipTimes.introStart > 0) {
                                            val introFraction = state.skipTimes.introStart.toFloat() / state.durationMs.toFloat()
                                            if (introFraction in 0f..1f) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = width * introFraction - 3.dp)
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                        .border(1.dp, CrunchyrollOrange, CircleShape)
                                                        .align(Alignment.CenterStart)
                                                )
                                            }
                                        }
                                        if (state.skipTimes.introEnd > 0) {
                                            val introEndFraction = state.skipTimes.introEnd.toFloat() / state.durationMs.toFloat()
                                            if (introEndFraction in 0f..1f) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = width * introEndFraction - 3.dp)
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                        .border(1.dp, CrunchyrollOrange, CircleShape)
                                                        .align(Alignment.CenterStart)
                                                )
                                            }
                                        }
                                        if (state.skipTimes.outroStart > 0) {
                                            val outroFraction = state.skipTimes.outroStart.toFloat() / state.durationMs.toFloat()
                                            if (outroFraction in 0f..1f) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = width * outroFraction - 3.dp)
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                        .border(1.dp, CrunchyrollOrange, CircleShape)
                                                        .align(Alignment.CenterStart)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = formatTime(state.durationMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Chapters trigger button
                            IconButton(
                                onClick = {
                                    callbacks.onControlsInteraction()
                                    showChaptersSelector = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Chapters",
                                    tint = CrunchyrollOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.isBuffering) {
                CircularProgressIndicator(
                    color = CrunchyrollOrange,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            if (state.showGestureIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.gestureIndicatorType == "brightness") Icons.Default.WbSunny else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        LinearProgressIndicator(
                            progress = { state.gestureIndicatorValue },
                            color = CrunchyrollOrange,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
            
            // Skip Intro / Skip Outro Floating Button Overlay
            val hasIntroNow = state.skipTimes.isDuringIntro(state.currentPositionMs)
            val hasOutroNow = state.skipTimes.isDuringOutro(state.currentPositionMs)
            
            if (hasIntroNow || hasOutroNow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 24.dp)
                ) {
                    Button(
                        onClick = {
                            if (exoPlayer != null) {
                                val targetPos = if (hasIntroNow) state.skipTimes.introEnd else state.skipTimes.outroEnd
                                exoPlayer.seekTo(targetPos)
                                callbacks.onPositionChanged(targetPos)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrunchyrollOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Skip Scene",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasIntroNow) "Skip Intro" else "Skip Outro",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showChaptersSelector) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable { showChaptersSelector = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF141414),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(if (state.isLandscape) 0.5f else 0.85f)
                            .fillMaxHeight(0.7f)
                            .clickable(enabled = false) {}
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episode Chapters",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showChaptersSelector = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            HorizontalDivider(color = Color(0xFF222222), modifier = Modifier.padding(vertical = 8.dp))
                            
                            val chapters = remember(state.skipTimes, state.durationMs) {
                                val list = mutableListOf<SegmentChapter>()
                                list.add(SegmentChapter("Hook / Prologue", 0L))
                                
                                if (state.skipTimes.introStart > 0 && state.skipTimes.introEnd > state.skipTimes.introStart) {
                                    list.add(SegmentChapter("Opening Song (Intro)", state.skipTimes.introStart))
                                    list.add(SegmentChapter("Canon Episode Content", state.skipTimes.introEnd))
                                } else {
                                    list.add(SegmentChapter("Episode Intro", 90000L))
                                    list.add(SegmentChapter("Main Story Content", 180000L))
                                }
                                
                                if (state.durationMs > 720000L) {
                                    list.add(SegmentChapter("Midpoint Eyecatch", 720000L))
                                }
                                
                                if (state.skipTimes.outroStart > 0 && state.skipTimes.outroEnd > state.skipTimes.outroStart) {
                                    list.add(SegmentChapter("Ending Credits", state.skipTimes.outroStart))
                                    list.add(SegmentChapter("Next Episode Preview", state.skipTimes.outroEnd))
                                } else if (state.durationMs > 1300000L) {
                                    list.add(SegmentChapter("Ending Theme Credits", 1290000L))
                                    list.add(SegmentChapter("Next Episode Teaser", 1380000L))
                                }
                                
                                list.filter { it.startMs < state.durationMs }
                            }
                            
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(chapters) { chap ->
                                    val isActive = exoPlayer?.currentPosition?.let { currentPos ->
                                        val index = chapters.indexOf(chap)
                                        val nextStart = if (index < chapters.size - 1) chapters[index + 1].startMs else state.durationMs
                                        currentPos >= chap.startMs && currentPos < nextStart
                                    } ?: false
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isActive) Color(0xFF2E1A0C) else Color(0xFF1D1D1D),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isActive) CrunchyrollOrange else Color(0xFF2C2C2C),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                exoPlayer?.seekTo(chap.startMs)
                                                callbacks.onPositionChanged(chap.startMs)
                                                showChaptersSelector = false
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isActive) Icons.Default.PlayCircle else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isActive) CrunchyrollOrange else Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chap.title,
                                                color = if (isActive) CrunchyrollOrange else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = formatTime(chap.startMs),
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun ScrubbingThumbnail(
    videoUrl: String?,
    positionMs: Long,
    durationMs: Long,
    fallbackImageUrl: String?,
    modifier: Modifier = Modifier
) {
    // Keep a cache of already loaded bitmaps to make dragging extremely smooth and efficient
    val bitmapCache = remember { LruCache<Long, Bitmap>(100) }

    // Round position to nearest 1000ms (1 second) to maximize cache hits
    val roundedPositionS = positionMs / 1000

    var thumbnailBitmap by remember(videoUrl, positionMs) { 
        mutableStateOf<Bitmap?>(bitmapCache.get(roundedPositionS)) 
    }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var retriever by remember(videoUrl) { mutableStateOf<MediaMetadataRetriever?>(null) }

    LaunchedEffect(videoUrl) {
        if (!videoUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val r = MediaMetadataRetriever().apply {
                        setDataSource(videoUrl, HashMap<String, String>())
                    }
                    withContext(Dispatchers.Main) {
                        retriever = r
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else {
            retriever = null
        }
    }

    DisposableEffect(retriever) {
        onDispose {
            val r = retriever
            if (r != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        r.release()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    LaunchedEffect(videoUrl, roundedPositionS, retriever) {
        val currentRetriever = retriever ?: return@LaunchedEffect

        val cached = bitmapCache.get(roundedPositionS)
        if (cached != null) {
            thumbnailBitmap = cached
            return@LaunchedEffect
        }

        isLoading = true
        // Debounce dragging requests by 60ms so fast dragging doesn't choke the thread/network
        delay(60)

        withContext(Dispatchers.IO) {
            try {
                val timeUs = roundedPositionS * 1000 * 1000
                val frame = currentRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val scaled = Bitmap.createScaledBitmap(frame, 240, 135, true)
                    bitmapCache.put(roundedPositionS, scaled)
                    withContext(Dispatchers.Main) {
                        thumbnailBitmap = scaled
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .width(160.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = "Preview Frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (!fallbackImageUrl.isNullOrEmpty()) {
                val progressFraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                AsyncImage(
                    model = fallbackImageUrl,
                    contentDescription = "Preview Fallback",
                    contentScale = ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(
                        horizontalBias = (progressFraction * 2f - 1f).coerceIn(-1f, 1f),
                        verticalBias = 0f
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1F1F1F))
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFF5A623),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsOverlay(
    state: PlayerScreenState,
    callbacks: PlayerCallbacks,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Playback Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = callbacks.onSettingsBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0A0A0A))
                .verticalScroll(state.settingsScrollState)
        ) {
            // Autoplay Row
            SettingsToggleRow(
                label = "Autoplay",
                checked = state.autoplayNextEpisode,
                onCheckedChange = callbacks.onAutoplayChange
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Audio Row
            SettingsClickableRow(
                label = "Audio",
                value = if (state.activeCategory == "dub") "English (Dub)" else "Japanese (Sub)",
                onClick = { callbacks.onShowAudioDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Subtitles Row
            SettingsClickableRow(
                label = "Subtitles/CC",
                value = when (state.currentSubtitleSelection) {
                    "off" -> "Off"
                    "en" -> "English"
                    "ar" -> "Arabic"
                    "es" -> "Spanish"
                    "fr" -> "French"
                    else -> "Track: ${state.currentSubtitleSelection}"
                },
                onClick = { callbacks.onShowSubtitlesDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Subtitle Size Row
            SettingsClickableRow(
                label = "Subtitle Size",
                value = when (state.subtitleSizeSp) {
                    14f -> "Small"
                    18f -> "Normal"
                    22f -> "Large"
                    26f -> "Extra Large"
                    else -> "Normal"
                },
                onClick = { callbacks.onShowSubtitleSizeDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Subtitle Style Row
            SettingsClickableRow(
                label = "Subtitle Style",
                value = when (state.subtitleStyle) {
                    "classic_outline" -> "White with Outline"
                    "yellow" -> "Crunchyroll Yellow"
                    "cyan" -> "Neon Cyan Glow"
                    "caption_box" -> "Black Background Box"
                    else -> "White Drop Shadow"
                },
                onClick = { callbacks.onShowSubtitleStyleDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Quality Row
            SettingsClickableRow(
                label = "Quality",
                value = state.qualityOption,
                onClick = { callbacks.onShowQualityDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Playback Speed Row
            SettingsClickableRow(
                label = "Playback Speed",
                value = if (state.playbackSpeed == 1.0f) "Normal" else "${state.playbackSpeed}x",
                onClick = { callbacks.onShowSpeedDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Anime Version Row (Censored/Uncensored)
            SettingsClickableRow(
                label = "Release Version",
                value = if (state.preferredAnimeVersion == "uncensored") "Uncut (Uncensored)" else "TV (Censored)",
                onClick = { callbacks.onShowVersionDialogChange(true) }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Report a Problem Row
            SettingsClickableRow(
                label = "Report a Problem",
                value = "",
                onClick = {
                    Toast.makeText(context, "Problem reported. Thank you!", Toast.LENGTH_SHORT).show()
                }
            )
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
        }

        // Dialogs
        if (state.showVersionDialog) {
            SettingsDialog(
                title = "Select Anime Version",
                onDismiss = { callbacks.onShowVersionDialogChange(false) }
            ) {
                val options = listOf("uncensored" to "Uncut (Uncensored)", "censored" to "TV Broadcast (Censored)")
                LazyColumn {
                    items(options) { (key, label) ->
                        SettingsDialogRow(
                            label = label,
                            selected = state.preferredAnimeVersion == key,
                            onClick = {
                                callbacks.onVersionChange(key)
                                Toast.makeText(context, "Preference set to: $label", Toast.LENGTH_SHORT).show()
                                callbacks.onShowVersionDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showAudioDialog) {
            SettingsDialog(
                title = "Select Audio Language",
                onDismiss = { callbacks.onShowAudioDialogChange(false) }
            ) {
                val options = listOf("sub" to "Japanese (Sub)", "dub" to "English (Dub)")
                LazyColumn {
                    items(options) { (key, label) ->
                        SettingsDialogRow(
                            label = label,
                            selected = state.activeCategory == key,
                            onClick = {
                                callbacks.onAudioChange(key)
                                callbacks.onShowAudioDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showSubtitlesDialog) {
            SettingsDialog(
                title = "Subtitles",
                onDismiss = { callbacks.onShowSubtitlesDialogChange(false) }
            ) {
                LazyColumn {
                    items(state.capturedSubtitles) { sub ->
                        val label = when (sub.langCode) {
                            "en" -> "English"
                            "ar" -> "Arabic"
                            "es" -> "Spanish"
                            "fr" -> "French"
                            else -> "Track: ${sub.langCode}"
                        }
                        SettingsDialogRow(
                            label = label,
                            selected = state.currentSubtitleSelection == sub.langCode,
                            onClick = {
                                callbacks.onSubtitleSelectionChange(sub.langCode)
                                callbacks.onShowSubtitlesDialogChange(false)
                            }
                        )
                    }
                    item {
                        SettingsDialogRow(
                            label = "Off",
                            selected = state.currentSubtitleSelection == "off",
                            onClick = {
                                callbacks.onSubtitleSelectionChange("off")
                                callbacks.onShowSubtitlesDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showQualityDialog) {
            SettingsDialog(
                title = "Select Video Quality",
                onDismiss = { callbacks.onShowQualityDialogChange(false) }
            ) {
                val options = listOf("Auto", "1080p", "720p", "480p")
                LazyColumn {
                    items(options) { label ->
                        SettingsDialogRow(
                            label = label,
                            selected = state.qualityOption == label,
                            onClick = {
                                callbacks.onQualityChange(label)
                                callbacks.onShowQualityDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showSpeedDialog) {
            SettingsDialog(
                title = "Select Playback Speed",
                onDismiss = { callbacks.onShowSpeedDialogChange(false) }
            ) {
                val options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                LazyColumn {
                    items(options) { value ->
                        SettingsDialogRow(
                            label = if (value == 1.0f) "Normal (1.0x)" else "${value}x",
                            selected = state.playbackSpeed == value,
                            onClick = {
                                callbacks.onSpeedChange(value)
                                callbacks.onShowSpeedDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showSubtitleSizeDialog) {
            SettingsDialog(
                title = "Select Subtitle Size",
                onDismiss = { callbacks.onShowSubtitleSizeDialogChange(false) }
            ) {
                val options = listOf(
                    14f to "Small",
                    18f to "Normal",
                    22f to "Large",
                    26f to "Extra Large"
                )
                LazyColumn {
                    items(options) { (value, label) ->
                        SettingsDialogRow(
                            label = label,
                            selected = state.subtitleSizeSp == value,
                            onClick = {
                                callbacks.onSubtitleSizeChange(value)
                                callbacks.onShowSubtitleSizeDialogChange(false)
                            }
                        )
                    }
                }
            }
        }

        if (state.showSubtitleStyleDialog) {
            SettingsDialog(
                title = "Select Subtitle Style",
                onDismiss = { callbacks.onShowSubtitleStyleDialogChange(false) }
            ) {
                val styles = listOf(
                    "classic_outline" to "White with Outline",
                    "yellow" to "Crunchyroll Yellow",
                    "cyan" to "Neon Cyan Glow",
                    "caption_box" to "Black Background Box",
                    "default" to "White Drop Shadow"
                )
                LazyColumn {
                    items(styles) { (key, label) ->
                        SettingsDialogRow(
                            label = label,
                            selected = state.subtitleStyle == key,
                            onClick = {
                                callbacks.onSubtitleStyleChange(key)
                                callbacks.onShowSubtitleStyleDialogChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFF5A623),
                checkedTrackColor = Color(0xFFF5A623).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value.isNotEmpty()) {
                Text(text = value, color = Color(0xFFB3B3B3), fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                content()
            }
        },
        containerColor = Color(0xFF1F1F1F),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFF5A623))
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun SettingsDialogRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFF5A623),
                unselectedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun InPlayerEpisodeSelectorOverlay(
    state: PlayerScreenState,
    callbacks: PlayerCallbacks,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = { callbacks.onShowEpisodesSelectorChange(false) })
    ) {
        Surface(
            color = Color(0xFF141414),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2C)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth(if (state.isLandscape) 0.5f else 1f)
                .fillMaxHeight(if (state.isLandscape) 1f else 0.65f)
                .align(if (state.isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episodes (${state.episodes.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { callbacks.onShowEpisodesSelectorChange(false) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                HorizontalDivider(color = Color(0xFF222222))
                
                if (state.episodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No episodes available", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.episodes) { ep ->
                            val isCurrent = state.currentEpisode?.id == ep.id
                            Surface(
                                color = if (isCurrent) Color(0xFF2E1A0C) else Color(0xFF1D1D1D),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) CrunchyrollOrange else Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        callbacks.onShowEpisodesSelectorChange(false)
                                        callbacks.onEpisodeSelect(ep)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isCurrent) CrunchyrollOrange else Color(0xFF2C2C2C),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ep.number.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title.ifEmpty { "Episode ${ep.number}" },
                                            color = if (isCurrent) CrunchyrollOrange else Color.White,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (ep.isFiller) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF8B0000), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "FILLER",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Playing",
                                            tint = CrunchyrollOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SegmentChapter(val title: String, val startMs: Long)
