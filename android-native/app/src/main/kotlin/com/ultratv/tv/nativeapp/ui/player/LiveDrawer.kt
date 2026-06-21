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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val initialIdx = remember(entries) { entries.indexOfFirst { it.isCurrent }.coerceAtLeast(0) }
    val currentFocus = remember { FocusRequester() }
    var focusSet by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(entries) {
        if (!focusSet && entries.isNotEmpty()) {
            if (initialIdx > 0) {
                listState.scrollToItem(initialIdx)
            }
            // Let the UI settle before requesting focus
            kotlinx.coroutines.delay(50)
            runCatching { currentFocus.requestFocus() }
            focusSet = true
        }
    }

    Row(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color(0xE60F0E17)) // ZinaTV Dark Navy semi-transparent
        ) {
            // -- Channel List (Left Side in ZinaTV) --
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "All Channels",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = entries, key = { entry -> entry.channel.id }) { e ->
                        val idx = entries.indexOf(e)
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        val highlight = e.isCurrent || focused

                        Card(
                            onClick = { onPick(e.channel) },
                            modifier = if (e.isCurrent) Modifier.focusRequester(currentFocus) else Modifier,
                            interactionSource = interaction,
                            shape = CardDefaults.shape(RoundedCornerShape(4.dp)),
                            colors = ultraCardColors(
                                containerColor = if (e.isCurrent) t.Accent else Color.Transparent,
                                focusedContainerColor = t.Accent,
                                focusedContentColor = Color.White,
                            ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    (idx + 1).toString(),
                                    color = if (highlight) Color.White else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(32.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        e.channel.name,
                                        color = if (highlight) Color.White else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                    )
                                }
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Favorite",
                                    tint = if (highlight) Color.White else Color.White.copy(alpha=0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0B0A12)) // Slightly darker for sidebar
                    .padding(vertical = 24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DrawerIconButton(Icons.Default.Search, "Search", onClick = onSearch)
                DrawerIconButton(Icons.Default.ViewList, "EPG", onClick = onEpg)
                DrawerIconButton(Icons.Default.Favorite, "Add to Fav", onClick = onFav)
                DrawerIconButton(Icons.Default.Timer, "Time Shift", onClick = onSleep)
                DrawerIconButton(Icons.Default.Audiotrack, "Tracks", onClick = onTracks)
                DrawerIconButton(Icons.Default.FiberManualRecord, "Record", onClick = onRecord, tint = Color.Red)
                DrawerIconButton(Icons.Default.AspectRatio, "Aspect", onClick = onAspect)
                DrawerIconButton(Icons.Default.Settings, "Settings", onClick = onSettings)
            }
        }

        // Right side click-through area to dismiss
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0x33000000))
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
