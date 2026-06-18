package com.ultratv.tv.nativeapp.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.ultratv.tv.nativeapp.i18n.LocalStrings
import com.ultratv.tv.nativeapp.ui.common.ChannelLogo
import com.ultratv.tv.nativeapp.ui.theme.UltraFonts
import com.ultratv.tv.nativeapp.ui.theme.UltraTokens
import com.ultratv.tv.nativeapp.ui.theme.ultraCardColors

/**
 * OTT-style channel-zap drawer used by the full-screen player. Slides in from
 * the right while the live stream keeps playing in the background. Each row
 * carries the channel position, logo, name and now/next programmes; the
 * currently-playing channel is highlighted with an EN COURS pill. Picking a
 * row zaps without leaving the player.
 *
 * Extracted from PlayerScreen for readability; visibility is `internal` so
 * PlayerScreen can still reach it without going public.
 */
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
internal fun LiveDrawer(
    vm: PlayerViewModel,
    onPick: (com.ultratv.tv.nativeapp.data.db.ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries by vm.queue.collectAsState()
    val s = LocalStrings.current
    val t = UltraTokens
    val f = UltraFonts
    BackHandler { onDismiss() }
    Row(Modifier.fillMaxSize()) {
        // Click-through area on the left so OK / BACK reach us first.
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0x66000000))
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(480.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xF00A0A12), Color(0xFA000000))
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
                )
                .padding(horizontal = 32.dp, vertical = 40.dp),
        ) {
            Text(
                s.liveZappingEyebrow,
                color = t.Accent,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                s.liveAllChannels,
                color = Color.White,
                fontFamily = f.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = entries, key = { entry -> entry.channel.id }) { e ->
                    val idx = entries.indexOf(e)
                    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    val highlight = e.isCurrent || focused

                    Card(
                        onClick = { onPick(e.channel) },
                        interactionSource = interaction,
                        shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
                        colors = ultraCardColors(
                            containerColor = if (e.isCurrent) t.AccentSoft else Color(0x1AFFFFFF),
                            focusedContainerColor = t.Accent,
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "%02d".format(idx + 1),
                                color = if (highlight) Color.White else t.Fg4,
                                fontSize = 14.sp,
                                fontFamily = f.Mono,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(36.dp),
                            )
                            ChannelLogo(
                                name = e.channel.name,
                                logoUrl = e.channel.logo,
                                short = null,
                                hueSeed = e.channel.name.hashCode(),
                                hd = null,
                                size = 44.dp,
                                showBadge = false,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        e.channel.name,
                                        color = if (highlight) Color.White else t.Fg2,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    if (e.isCurrent) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (focused) Color.White else t.Accent)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                s.liveOnAirPill,
                                                color = if (focused) t.Accent else Color.White,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                if (e.now != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        e.now.title,
                                        color = if (highlight) Color.White else t.Fg3,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                }
                                if (e.next != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${s.liveThen} ${e.next.title}",
                                        color = if (highlight) Color.White.copy(alpha = 0.7f) else t.Fg4,
                                        fontSize = 11.sp,
                                        maxLines = 1,
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
