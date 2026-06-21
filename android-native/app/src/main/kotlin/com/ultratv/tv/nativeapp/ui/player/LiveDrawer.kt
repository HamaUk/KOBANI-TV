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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import com.ultratv.tv.nativeapp.i18n.LocalStrings
import com.ultratv.tv.nativeapp.ui.common.ChannelLogo
import com.ultratv.tv.nativeapp.ui.theme.UltraFonts
import com.ultratv.tv.nativeapp.ui.theme.UltraTokens
import com.ultratv.tv.nativeapp.ui.theme.ultraCardColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Settings

/**
 * OTT-style channel-zap drawer used by the full-screen player. Slides in from
 * the left while the live stream keeps playing in the background. Layout:
 *
 *   [ Icon Menu (72dp) ][ Channel List (420dp) ][ dim overlay -> dismiss ]
 *
 * D-pad LEFT from the channel list moves focus to the icon menu.
 * D-pad RIGHT from the icon menu moves focus back to the channel list.
 * Pressing BACK dismisses the drawer.
 */
@OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    ExperimentalComposeUiApi::class,
)
@Composable
internal fun LiveDrawer(
    vm: PlayerViewModel,
    onPick: (com.ultratv.tv.nativeapp.data.db.ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
    onSearch: () -> Unit = {},
    onEpg: () -> Unit = {},
    onFav: () -> Unit = {},
    onSleep: () -> Unit = {},
    onTracks: () -> Unit = {},
    onRecord: () -> Unit = {},
    onAspect: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val entries by vm.queue.collectAsState()
    val s = LocalStrings.current
    val t = UltraTokens
    val f = UltraFonts
    BackHandler { onDismiss() }

    // Focus requesters for cross-column D-pad navigation
    val iconMenuFocus = remember { FocusRequester() }
    val channelListFocus = remember { FocusRequester() }

    // Give initial focus to channel list when drawer opens
    LaunchedEffect(Unit) {
        channelListFocus.requestFocus()
    }

    Row(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFA000000), Color(0xF00A0A12))
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
                )
        ) {
            // -- Left vertical icon menu --
            // Uses a regular Column (not LazyColumn) because there are only
            // 8 buttons -- simpler focus handling and they all fit on screen.
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF08080C))
                    .padding(vertical = 40.dp)
                    .focusRequester(iconMenuFocus)
                    .focusProperties {
                        // D-pad RIGHT -> jump to channel list
                        right = channelListFocus
                        // Don't let LEFT escape the drawer
                        left = iconMenuFocus
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DrawerIconButton(Icons.Default.Search, "Search", onClick = onSearch)
                DrawerIconButton(Icons.Default.ViewList, "EPG", onClick = onEpg)
                DrawerIconButton(Icons.Default.Favorite, "Favorites", onClick = onFav)
                DrawerIconButton(Icons.Default.Timer, "Sleep", onClick = onSleep)
                DrawerIconButton(Icons.Default.Audiotrack, "Tracks", onClick = onTracks)
                DrawerIconButton(Icons.Default.FiberManualRecord, "Record", onClick = onRecord, tint = Color.Red)
                DrawerIconButton(Icons.Default.AspectRatio, "Aspect", onClick = onAspect)
                DrawerIconButton(Icons.Default.Settings, "Settings", onClick = onSettings)
            }

            // -- Channel List --
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .padding(horizontal = 24.dp, vertical = 40.dp),
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
                    modifier = Modifier
                        .focusRequester(channelListFocus)
                        .focusProperties {
                            // D-pad LEFT -> jump to icon menu
                            left = iconMenuFocus
                        },
                ) {
                    items(items = entries, key = { entry -> entry.channel.id }) { e ->
                        val idx = entries.indexOf(e)
                        val interaction = remember { MutableInteractionSource() }
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
                                    epgChannelId = e.channel.epgChannelId,
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

        // Right side click-through area to dismiss
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0x66000000))
        )
    }
}

/**
 * Single icon button for the drawer sidebar. Uses TV material IconButton
 * with an interaction source so it gets proper focus highlight on D-pad.
 */
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
private fun DrawerIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (focused) Color.Black else tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
