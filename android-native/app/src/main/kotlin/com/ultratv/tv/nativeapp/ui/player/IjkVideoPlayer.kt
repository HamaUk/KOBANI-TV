package com.ultratv.tv.nativeapp.ui.player

import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import tv.danmaku.ijk.media.player.IjkMediaPlayer

@Composable
fun IjkVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize IjkMediaPlayer natively.
    // It's recommended to load native libs explicitly, though modern wrappers do it automatically.
    // IjkMediaPlayer.loadLibrariesOnce(null) 
    // IjkMediaPlayer.native_profileBegin("libijkplayer.so")

    val ijkPlayer = remember {
        IjkMediaPlayer().apply {
            // Set hardware decoding preference (mediacodec)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
            // Drop frames when CPU is too slow
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", if (playWhenReady) 1 else 0)
            // Reconnect options for unstable IPTV
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 20000000)
        }
    }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            ijkPlayer.reset()
            ijkPlayer.setDataSource(context, Uri.parse(url))
            ijkPlayer.prepareAsync()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (ijkPlayer.isPlaying) ijkPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (playWhenReady) ijkPlayer.start()
                Lifecycle.Event.ON_DESTROY -> {
                    ijkPlayer.stop()
                    ijkPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ijkPlayer.stop()
            ijkPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { ctx ->
            val container = FrameLayout(ctx)
            container.setBackgroundColor(android.graphics.Color.BLACK)
            
            val surfaceView = SurfaceView(ctx)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
            
            surfaceView.layoutParams = lp
            container.addView(surfaceView)

            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    ijkPlayer.setDisplay(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    // Not strictly necessary for basic playback
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    ijkPlayer.setDisplay(null)
                }
            })
            container
        }
    )
}
