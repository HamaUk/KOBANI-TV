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

    val libVLC = remember {
        val args = ArrayList<String>()
        args.add("-vvv")
        args.add("--network-caching=3000") // 3 seconds
        LibVLC(context, args)
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
            mediaPlayer.detachViews()
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
                mediaPlayer.attachViews(this, null, false, false)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
