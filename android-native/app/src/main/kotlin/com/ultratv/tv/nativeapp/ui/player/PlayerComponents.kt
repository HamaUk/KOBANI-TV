package com.ultratv.tv.nativeapp.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ultratv.tv.nativeapp.i18n.LocalStrings

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TracksDialog(player: ExoPlayer, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    val T = com.ultratv.tv.nativeapp.ui.theme.UltraTokens
    val tracks = player.currentTracks
    Box(
        Modifier
            .fillMaxHeight()
            .width(340.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            .background(Color(0xE6000000))
            .border(1.dp, T.Line2, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                S.playerTracks,
                color = T.Fg3,
                fontSize = 12.sp,
                letterSpacing = 2.3.sp,
                fontWeight = FontWeight.Bold,
            )
            // Audio tracks
            val audioGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
            Text(S.playerAudioTemplate.format(audioGroups.sumOf { it.length }), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            audioGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    val label = listOfNotNull(
                        fmt.label,
                        fmt.language,
                        fmt.sampleMimeType?.removePrefix("audio/"),
                        fmt.channelCount.takeIf { it > 0 }?.let { "${it}ch" },
                    ).joinToString(" · ").ifBlank { "Track ${i + 1}" }
                    Button(
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                .build()
                            onDismiss()
                        },
                        colors = if (group.isTrackSelected(i)) ButtonDefaults.colors()
                        else ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
            // Subtitle tracks
            val subGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
            Text(S.playerSubtitlesTemplate.format(subGroups.sumOf { it.length }), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Button(
                onClick = {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                        .build()
                    onDismiss()
                },
                colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) { Text(S.playerOff, fontSize = 13.sp) }
            subGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    val label = listOfNotNull(fmt.label, fmt.language).joinToString(" · ").ifBlank { "Subtitle ${i + 1}" }
                    Button(
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                .build()
                            onDismiss()
                        },
                        colors = if (group.isTrackSelected(i)) ButtonDefaults.colors()
                        else ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            ) { Text(S.close) }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, modifier = Modifier.width(90.dp))
        Text(value, color = Color.White, fontSize = 11.sp)
    }
}

/** PlayerView.resizeMode mapping. RESIZE_MODE_* values are ints exposed by
 *  androidx.media3.ui.AspectRatioFrameLayout. */
enum class AspectMode(val label: String, val resizeMode: Int) {
    Fit("Fit", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Fill("Fill", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL),
    Zoom("Zoom", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FixedWidth("16:9", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FixedHeight("4:3", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
}

data class StreamStats(
    val resolution: String = "—",
    val videoCodec: String = "—",
    val frameRate: String = "—",
    val videoBitrate: String = "—",
    val audioCodec: String = "—",
    val audioChannels: String = "—",
    val bufferedAhead: String = "—",
    val droppedFrames: String = "—",
) {
    companion object {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        fun read(player: ExoPlayer): StreamStats {
            val v = player.videoFormat
            val a = player.audioFormat
            val bufferedMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0)
            return StreamStats(
                resolution = v?.let { "${it.width}×${it.height}" } ?: "—",
                videoCodec = v?.sampleMimeType?.removePrefix("video/") ?: "—",
                frameRate = v?.frameRate?.takeIf { it > 0 }?.let { "%.1f fps".format(it) } ?: "—",
                videoBitrate = v?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "—",
                audioCodec = a?.sampleMimeType?.removePrefix("audio/") ?: "—",
                audioChannels = a?.channelCount?.toString() ?: "—",
                bufferedAhead = "${bufferedMs / 1000}s",
                droppedFrames = "n/a",
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SleepOption(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
    ) { Text(label, fontSize = 13.sp) }
}
