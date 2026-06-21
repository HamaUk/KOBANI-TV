package com.ultratv.tv.nativeapp.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun VlcVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val libVLC = remember {
        val args = ArrayList<String>()
        args.add("-vvv")
        args.add("--network-caching=3000") // 3 seconds
        LibVLC(appContext, args)
    }

    val mediaPlayer = remember { MediaPlayer(libVLC) }

    DisposableEffect(url) {
        if (url.isNotEmpty()) {
            val media = Media(libVLC, Uri.parse(url))
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        }
        onDispose {
            mediaPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
            libVLC.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            VLCVideoLayout(ctx).apply {
                addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) {
                        mediaPlayer.attachViews(this@apply, null, false, false)
                    }
                    override fun onViewDetachedFromWindow(v: android.view.View) {
                        mediaPlayer.detachViews()
                    }
                })
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
