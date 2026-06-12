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

private val sharedOkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()

@Stable
data class PlayerScreenState(
    val isLandscape: Boolean,
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
    val settingsScrollState: ScrollState
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
    val onShowVersionDialogChange: (Boolean) -> Unit
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
                Log.d("ANIPLEX_PLAYER", "Successfully seeked inside stream LaunchedEffect: $initialProgress")
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
                Log.d("ANIPLEX_PLAYER", "Successfully seeked to async progress: $initialProgress")
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

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            delay(500)
            currentPositionMs = player.currentPosition
            if (player.duration > 0L) {
                durationMs = player.duration
            }
            
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
        settingsScrollState = settingsScrollState
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
        onShowVersionDialogChange = { showVersionDialog = it }
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
                            setStyle(androidx.media3.ui.CaptionStyleCompat(
                                AndroidColor.WHITE,
                                AndroidColor.TRANSPARENT,
                                AndroidColor.TRANSPARENT,
                                androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                AndroidColor.BLACK,
                                null
                            ))
                            setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, state.subtitleSizeSp)
                        }
                    }
                },
                update = { view ->
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

                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val screenWidth = size.width
                                val screenHeight = size.height
                                val startX = down.position.x
                                val startY = down.position.y

                                // Completely ignore touches starting near the screen edges
                                // so they are allowed to propagate to system/back gestures
                                if (startX < edgeThresholdPx || startX > screenWidth - edgeThresholdPx ||
                                    startY < edgeThresholdPx || startY > screenHeight - edgeThresholdPx) {
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                    continue
                                }

                                val halfWidth = screenWidth / 2f
                                isLeftHalf = startX < halfWidth

                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                                if (isLeftHalf) {
                                    val lp = activity?.window?.attributes
                                    val currentBrightness = lp?.screenBrightness ?: -1f
                                    dragStartValue = if (currentBrightness < 0) 0.5f else currentBrightness
                                } else {
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    dragStartValue = currentVol.toFloat() / maxVolume.toFloat()
                                }

                                var currentPointerId = down.id
                                var hasTriggeredDrag = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == currentPointerId }
                                    if (change == null || change.isConsumed) {
                                        break
                                    }
                                    if (change.changedToUp()) {
                                        break
                                    }

                                    val currentX = change.position.x
                                    val currentY = change.position.y

                                    val dragAmountY = change.position.y - change.previousPosition.y
                                    val dragAmountX = change.position.x - change.previousPosition.x

                                    if (!hasTriggeredDrag) {
                                        val diffY = kotlin.math.abs(currentY - startY)
                                        val diffX = kotlin.math.abs(currentX - startX)
                                        if (diffY > 15 && diffY > diffX) {
                                            hasTriggeredDrag = true
                                            callbacks.onGestureChanged(
                                                if (isLeftHalf) "brightness" else "volume",
                                                dragStartValue,
                                                true
                                            )
                                        }
                                    }

                                    if (hasTriggeredDrag) {
                                        change.consume()
                                        val delta = -dragAmountY / screenHeight.toFloat()
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
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                            callbacks.onGestureChanged("volume", newVolumeFraction, true)
                                        }
                                    }
                                }
                            }
                        }
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .border(1.dp, Color(0xFFF5A623), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFF5A623),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = formatTime(pos),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
                            Text(
                                text = formatTime(state.currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )

                            Slider(
                                value = if (state.durationMs > 0) (state.currentPositionMs.toFloat() / state.durationMs.toFloat()) else 0f,
                                onValueChange = { fraction ->
                                    if (exoPlayer != null) {
                                        callbacks.onControlsInteraction()
                                        val targetPos = (fraction * state.durationMs).toLong()
                                        exoPlayer.seekTo(targetPos)
                                        callbacks.onPositionChanged(targetPos)
                                        scrubbingPositionMs = targetPos
                                    }
                                },
                                onValueChangeFinished = {
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
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )

                            Text(
                                text = formatTime(state.durationMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
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
