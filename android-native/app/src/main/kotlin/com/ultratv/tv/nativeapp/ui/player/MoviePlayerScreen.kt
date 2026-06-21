package com.ultratv.tv.nativeapp.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ultratv.tv.nativeapp.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

@androidx.media3.common.util.UnstableApi
@Composable
fun MoviePlayerScreen(url: String, title: String, onBack: () -> Unit, vm: MoviePlayerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    BackHandler { onBack() }
    
    var currentUrl by remember { mutableStateOf(url) }
    var tracksOpen by remember { mutableStateOf(false) }
    var displayMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectMode.Fit) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    val S = LocalStrings.current

    val loadedPrefs by androidx.compose.runtime.produceState<com.ultratv.tv.nativeapp.data.prefs.UserPrefs?>(
        initialValue = null,
    ) {
        value = vm.playbackPrefs()
    }
    val playbackPrefs = loadedPrefs ?: run {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val player = remember {
        val bufMs = (playbackPrefs.bufferSeconds * 1000).coerceAtLeast(15_000)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs                       = */ 15_000,
                /* maxBufferMs                       = */ bufMs,
                /* bufferForPlaybackMs               = */ 500,
                /* bufferForPlaybackAfterRebufferMs  = */ 1_500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        val defaultFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(defaultFactory)
            
        val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (playbackPrefs.preferSoftwareDecoder)
                    androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                else
                    androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            )
            setEnableDecoderFallback(true)
        }
        
        ExoPlayer.Builder(context, renderers)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            playWhenReady = true
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    com.ultratv.tv.nativeapp.RemoteLog.error(
                        "movie_player",
                        "code=${error.errorCodeName} ${error.message ?: ""}",
                    )
                }
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (!playbackPrefs.autoFrameRate) return
                    val act = (context as? android.app.Activity) ?: return
                    val fmt = currentTracks.groups
                        .firstOrNull { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
                        ?.let { g -> (0 until g.length).firstOrNull { g.isTrackSelected(it) }?.let { g.getTrackFormat(it) } }
                    val fps = fmt?.frameRate ?: return
                    if (fps <= 0f) return
                    val display = act.windowManager.defaultDisplay ?: return
                    val target = display.supportedModes.minByOrNull { m ->
                        val multiple = (m.refreshRate / fps).coerceAtLeast(1f)
                        kotlin.math.abs(m.refreshRate - fps * kotlin.math.round(multiple))
                    } ?: return
                    val lp = act.window.attributes
                    if (lp.preferredDisplayModeId != target.modeId) {
                        lp.preferredDisplayModeId = target.modeId
                        act.window.attributes = lp
                    }
                }
            })
            videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    var statsOpen by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(StreamStats()) }
    LaunchedEffect(statsOpen) {
        if (!statsOpen) return@LaunchedEffect
        while (true) {
            stats = StreamStats.read(player)
            delay(1_000)
        }
    }

    var sleepDeadlineMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sleepDeadlineMs) {
        if (sleepDeadlineMs <= 0L) return@LaunchedEffect
        while (System.currentTimeMillis() < sleepDeadlineMs) delay(5_000)
        player.pause()
        com.ultratv.tv.nativeapp.ui.common.Toaster.show(S.sleepReached)
        onBack()
    }
    
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank()) {
            player.setMediaItem(MediaItem.fromUri(currentUrl))
            player.prepare()
            val resume = vm.prepareResume()
            if (resume > 5_000) {
                player.seekTo(resume)
            }
            player.play()
        }
    }
    
    LaunchedEffect(playbackSpeed) {
        player.playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed)
    }

    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (player.duration > 0) {
                vm.recordProgress(player.currentPosition, player.duration)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.recordProgress(player.currentPosition, player.duration.coerceAtLeast(0))
            player.release()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    controllerShowTimeoutMs = 3000
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { v -> v.resizeMode = aspectMode.resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        val activity = remember(context) { context as? Activity }
        val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
        var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
        var brightness by remember {
            mutableFloatStateOf(
                activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
            )
        }
        var gestureLabel by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(gestureLabel) {
            if (gestureLabel != null) { delay(900); gestureLabel = null }
        }
        
        var volAccum by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier.align(Alignment.CenterEnd).width(120.dp).fillMaxHeight()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { volAccum = 0f },
                    ) { _, dragAmount ->
                        volAccum += -dragAmount
                        val step = (volAccum / 60f).toInt()
                        if (step != 0) {
                            volume = (volume + step).coerceIn(0, maxVol)
                            volAccum -= step * 60f
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                            gestureLabel = "🔊 ${(volume * 100 / maxVol)}%"
                        }
                    }
                },
        )
        
        var brAccum by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier.align(Alignment.CenterStart).width(120.dp).fillMaxHeight()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { brAccum = 0f },
                    ) { _, dragAmount ->
                        brAccum += -dragAmount
                        val deltaPct = (brAccum / 8f).toInt()
                        if (deltaPct != 0) {
                            brightness = (brightness + deltaPct / 100f).coerceIn(0.05f, 1f)
                            brAccum -= deltaPct * 8f
                            activity?.window?.let { w ->
                                val attrs = w.attributes as WindowManager.LayoutParams
                                attrs.screenBrightness = brightness
                                w.attributes = attrs
                            }
                            gestureLabel = "☀ ${(brightness * 100).toInt()}%"
                        }
                    }
                },
        )
        
        gestureLabel?.let { lbl ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(lbl, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        FlowRow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(max = 760.dp)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { sleepMenu = !sleepMenu }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep", modifier = Modifier.size(16.dp))
                    Text(
                        if (sleepDeadlineMs > 0L) {
                            val mins = ((sleepDeadlineMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L)
                            "${mins}min"
                        } else S.sleepLabel
                    )
                }
            }

            Button(onClick = { tracksOpen = true }) { 
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Quality/Tracks", modifier = Modifier.size(16.dp))
                    Text(S.playerTracks) 
                }
            }
            
            Button(onClick = { displayMenu = !displayMenu }) { 
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", modifier = Modifier.size(16.dp))
                    Text(S.playerDisplay) 
                }
            }
            
            val castInited = remember {
                runCatching { com.google.android.gms.cast.framework.CastContext.getSharedInstance(context) }.isSuccess
            }
            if (castInited) {
                AndroidView(
                    factory = { ctx ->
                        androidx.mediarouter.app.MediaRouteButton(ctx).also { btn ->
                            runCatching {
                                com.google.android.gms.cast.framework.CastButtonFactory
                                    .setUpMediaRouteButton(ctx.applicationContext, btn)
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            
            Button(onClick = { statsOpen = !statsOpen }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Stats", modifier = Modifier.size(16.dp))
                    Text(S.playerStats)
                }
            }
            
            Button(onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "video/*")
                        putExtra("title", title)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(intent, S.recordingsOpenWith))
                }
            }) { 
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "External Player", modifier = Modifier.size(16.dp))
                    Text(S.playerExternal) 
                }
            }
        }

        if (sleepMenu) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp, top = 80.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                    .padding(10.dp),
            ) {
                SleepOption(S.sleepMin15) { sleepDeadlineMs = System.currentTimeMillis() + 15 * 60_000; sleepMenu = false }
                SleepOption(S.sleepMin30) { sleepDeadlineMs = System.currentTimeMillis() + 30 * 60_000; sleepMenu = false }
                SleepOption(S.sleep1h) { sleepDeadlineMs = System.currentTimeMillis() + 60 * 60_000; sleepMenu = false }
                SleepOption(S.sleep2h) { sleepDeadlineMs = System.currentTimeMillis() + 120 * 60_000; sleepMenu = false }
                if (sleepDeadlineMs > 0L) {
                    SleepOption(S.sleepCancel) { sleepDeadlineMs = 0L; sleepMenu = false }
                }
            }
        }
        
        if (displayMenu) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp, top = 80.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(S.playerAspect, color = Color(0xFF66B3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                AspectMode.entries.forEach { mode ->
                    Button(
                        onClick = { aspectMode = mode; displayMenu = false },
                        colors = if (mode == aspectMode) androidx.tv.material3.ButtonDefaults.colors()
                        else androidx.tv.material3.ButtonDefaults.colors(containerColor = androidx.tv.material3.MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text(mode.label, fontSize = 12.sp) }
                }
                Text(S.playerSpeed, color = Color(0xFF66B3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { sp ->
                    Button(
                        onClick = { playbackSpeed = sp; displayMenu = false },
                        colors = if (sp == playbackSpeed) androidx.tv.material3.ButtonDefaults.colors()
                        else androidx.tv.material3.ButtonDefaults.colors(containerColor = androidx.tv.material3.MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text("${sp}x", fontSize = 12.sp) }
                }
            }
        }
        
        if (tracksOpen) {
            TracksDialog(player = player, onDismiss = { tracksOpen = false })
        }
        
        if (statsOpen) {
            val T = com.ultratv.tv.nativeapp.ui.theme.UltraTokens
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 24.dp, start = 24.dp)
                    .width(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xB3000000))
                    .border(1.dp, T.Line2, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "STREAM STATS",
                        color = T.Fg3,
                        fontSize = 10.sp,
                        letterSpacing = 2.3.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    androidx.tv.material3.IconButton(
                        onClick = { statsOpen = false },
                        modifier = Modifier.size(24.dp),
                    ) { Text("×", fontSize = 18.sp, color = T.Fg) }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                StatRow("Resolution", stats.resolution)
                StatRow("Video Codec", stats.videoCodec)
                StatRow("Audio Codec", stats.audioCodec)
                StatRow("Framerate", stats.frameRate)
                StatRow("Bitrate", stats.videoBitrate)
                StatRow("Drops", stats.droppedFrames)
                StatRow("Channels", stats.audioChannels)
            }
        }
    }
}
