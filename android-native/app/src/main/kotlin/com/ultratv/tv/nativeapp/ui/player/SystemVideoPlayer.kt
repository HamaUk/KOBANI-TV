package com.ultratv.tv.nativeapp.ui.player

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SystemVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnErrorListener { _, what, extra ->
                    com.ultratv.tv.nativeapp.RemoteLog.error("system_player", "Error: what=$what extra=$extra")
                    true
                }
            }
        },
        update = { view ->
            if (url.isNotEmpty()) {
                view.setVideoURI(Uri.parse(url))
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
