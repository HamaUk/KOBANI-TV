package com.ultratv.tv.nativeapp.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface

import com.ultratv.tv.nativeapp.RemoteLog
import com.ultratv.tv.nativeapp.data.prefs.UserPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun AdvancedVideoPlayer(
    url: String,
    title: String,
    isLive: Boolean,
    playbackPrefs: UserPrefs,
    onBack: () -> Unit,
    onZap: suspend (forward: Boolean) -> Pair<String, String>? = { null },
    resumePositionMs: Long = 0L,
    onProgressUpdate: (position: Long, duration: Long) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentUrl by remember { mutableStateOf(url) }
    var currentTitle by remember { mutableStateOf(title) }
    
    // UI States
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsMenuVisible by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(resumePositionMs) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Player Configuration States
    var aspectMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentQualityPreset by remember { mutableIntStateOf(0) } // 0=Auto, 1=720p, 2=1080p, 3=4K
    
    // Volume OSD
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volumeOsdVisible by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    
    // Reconnect State
    var reconnectAttempts by remember { mutableIntStateOf(0) }
    val maxReconnectAttempts = if (isLive) 8 else 3

    val trackSelector = remember { DefaultTrackSelector(context) }
    
    val player = remember {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(if (isLive) 12L else 20L, TimeUnit.SECONDS)
            .readTimeout(if (isLive) 30L else 90L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
            
        val dataSourceFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (isLive) 3000 else 8000,
                if (isLive) 20_000 else 120_000,
                if (isLive) 1000 else 3000,
                if (isLive) 2000 else 6000
            ).build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(
                if (playbackPrefs.preferSoftwareDecoder) 
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER 
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            )
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(), true
                )
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                playWhenReady = true
            }
    }

    // Hide controls timer
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }
    
    // Hide volume OSD timer
    LaunchedEffect(volumeOsdVisible, currentVolume) {
        if (volumeOsdVisible) {
            delay(2500)
            volumeOsdVisible = false
        }
    }

    // Progress updater
    LaunchedEffect(player) {
        while (true) {
            if (player.playbackState == Player.STATE_READY) {
                currentPosition = player.currentPosition
                duration = player.duration
                if (currentPosition > 0) {
                    onProgressUpdate(currentPosition, duration)
                }
            }
            delay(1000)
        }
    }

    // Prepare and play source
    LaunchedEffect(currentUrl) {
        errorMessage = null
        isBuffering = true
        reconnectAttempts = 0
        player.setMediaItem(MediaItem.fromUri(currentUrl))
        player.prepare()
        if (resumePositionMs > 5000 && !isLive) {
            player.seekTo(resumePositionMs)
        }
        player.play()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    errorMessage = null
                    reconnectAttempts = 0
                }
                if (state == Player.STATE_ENDED && isLive) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: PlaybackException) {
                RemoteLog.error("advanced_player", "Error: ${error.errorCodeName}")
                if (reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++
                    errorMessage = "Reconnecting... ($reconnectAttempts/$maxReconnectAttempts)"
                    scope.launch {
                        delay(if (isLive) 2000L else 4000L)
                        player.setMediaItem(MediaItem.fromUri(currentUrl))
                        player.prepare()
                        player.play()
                    }
                } else {
                    errorMessage = "Playback failed. Please try again."
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                
                controlsVisible = true
                
                when (ev.key) {
                    Key.DirectionUp, Key.ChannelUp -> {
                        if (isLive) {
                            scope.launch {
                                onZap(false)?.let { (newUrl, newTitle) ->
                                    currentUrl = newUrl
                                    currentTitle = newTitle
                                }
                            }
                            true
                        } else false
                    }
                    Key.DirectionDown, Key.ChannelDown -> {
                        if (isLive) {
                            scope.launch {
                                onZap(true)?.let { (newUrl, newTitle) ->
                                    currentUrl = newUrl
                                    currentTitle = newTitle
                                }
                            }
                            true
                        } else false
                    }
                    Key.VolumeUp -> {
                        currentVolume = (currentVolume + 1).coerceAtMost(maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                        volumeOsdVisible = true
                        true
                    }
                    Key.VolumeDown -> {
                        currentVolume = (currentVolume - 1).coerceAtLeast(0)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                        volumeOsdVisible = true
                        true
                    }
                    Key.VolumeMute -> {
                        currentVolume = if (currentVolume > 0) 0 else maxVolume / 2
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                        volumeOsdVisible = true
                        true
                    }
                    Key.MediaPlayPause, Key.DirectionCenter, Key.Enter -> {
                        if (settingsMenuVisible) return@onKeyEvent false
                        if (isPlaying) player.pause() else player.play()
                        true
                    }
                    Key.Back -> {
                        if (settingsMenuVisible) {
                            settingsMenuVisible = false
                            true
                        } else {
                            onBack()
                            true
                        }
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = aspectMode
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Error & Buffering Overlays
        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
        if (errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.ErrorOutline, "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(errorMessage!!, color = Color.White)
            }
        }

        // Volume OSD
        AnimatedVisibility(
            visible = volumeOsdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (currentVolume == 0) Icons.Filled.VolumeOff else if (currentVolume > maxVolume/2) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("${(currentVolume * 100f / maxVolume).roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !settingsMenuVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x66000000))
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(currentTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.weight(1f))

                // Bottom Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(24.dp)
                ) {
                    if (!isLive && duration > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatTime(currentPosition), color = Color.White)
                            Slider(
                                value = currentPosition.toFloat(),
                                valueRange = 0f..duration.toFloat(),
                                onValueChange = { player.seekTo(it.toLong()) },
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )
                            Text(formatTime(duration), color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLive) {
                            IconButton(onClick = { scope.launch { onZap(false)?.let { (u, t) -> currentUrl=u; currentTitle=t } } }) {
                                Icon(Icons.Filled.SkipPrevious, "Previous Channel", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                        
                        IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        if (isLive) {
                            IconButton(onClick = { scope.launch { onZap(true)?.let { (u, t) -> currentUrl=u; currentTitle=t } } }) {
                                Icon(Icons.Filled.SkipNext, "Next Channel", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(32.dp))
                        
                        IconButton(onClick = { settingsMenuVisible = true }) {
                            Icon(Icons.Filled.Settings, "Settings", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
        
        // Settings Menu Dialog
        if (settingsMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB3000000))
                    .clickable { settingsMenuVisible = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.width(400.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(Modifier.padding(16.dp)) {
                        item {
                            Text("Advanced Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        item {
                            SettingsRow("Resize Mode", getResizeLabel(aspectMode)) {
                                aspectMode = nextResizeMode(aspectMode)
                            }
                        }
                        if (!isLive) {
                            item {
                                SettingsRow("Playback Speed", "${playbackSpeed}x") {
                                    playbackSpeed = if (playbackSpeed >= 2.0f) 0.5f else playbackSpeed + 0.25f
                                    player.setPlaybackSpeed(playbackSpeed)
                                }
                            }
                        }
                        item {
                            SettingsRow("Quality", getQualityLabel(currentQualityPreset)) {
                                currentQualityPreset = (currentQualityPreset + 1) % 4
                                applyQualityPreset(currentQualityPreset, trackSelector)
                            }
                        }
                        item {
                            SettingsRow("Picture-in-Picture", "") {
                                settingsMenuVisible = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val act = context as? android.app.Activity
                                    act?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface)
        if (value.isNotEmpty()) {
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}

@OptIn(UnstableApi::class)
private fun nextResizeMode(current: Int): Int = when(current) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

@OptIn(UnstableApi::class)
private fun getResizeLabel(mode: Int): String = when(mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> "Fixed Width"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
    else -> "Fit"
}

private fun getQualityLabel(preset: Int): String = when(preset) {
    1 -> "720p"
    2 -> "1080p"
    3 -> "4K"
    else -> "Auto"
}

@OptIn(UnstableApi::class)
private fun applyQualityPreset(preset: Int, trackSelector: DefaultTrackSelector) {
    val (w, h, br) = when (preset) {
        1 -> Triple(1280, 720, 4_000_000)
        2 -> Triple(1920, 1080, 12_000_000)
        3 -> Triple(3840, 2160, Int.MAX_VALUE)
        else -> Triple(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
    }
    trackSelector.parameters = trackSelector.buildUponParameters()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setMaxVideoSize(w, h)
        .setMaxVideoBitrate(br)
        .build()
}
