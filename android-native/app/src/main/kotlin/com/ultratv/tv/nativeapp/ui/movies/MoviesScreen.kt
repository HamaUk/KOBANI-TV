package com.ultratv.tv.nativeapp.ui.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ultratv.tv.nativeapp.ui.common.CategoryChips
import com.ultratv.tv.nativeapp.ui.common.ContentRail
import com.ultratv.tv.nativeapp.ui.common.HeroBanner
import com.ultratv.tv.nativeapp.ui.common.PosterCard

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(onOpen: (Long) -> Unit, vm: MoviesViewModel = hiltViewModel()) {
    val sel by vm.selectedCategory.collectAsState()
    val cats by vm.categories.collectAsState()
    val rails by vm.rails.collectAsState()
    val featured by vm.featured.collectAsState()
    val flatMovies by vm.movies.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    val S = com.ultratv.tv.nativeapp.i18n.LocalStrings.current
    val paged = vm.pagedMovies.collectAsLazyPagingItems()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = com.ultratv.tv.nativeapp.ui.theme.UltraTokens.EdgeGutter),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        S.moviesTitle,
                        fontFamily = com.ultratv.tv.nativeapp.ui.theme.UltraFonts.Serif,
                        fontSize = 36.sp,
                        letterSpacing = (-1).sp,
                        color = com.ultratv.tv.nativeapp.ui.theme.UltraTokens.Fg,
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.padding(bottom = 20.dp)) {
                        CategoryChips(categories = cats, selected = sel, onSelect = vm::selectCategory)
                    }
                    Text(
                        "${paged.itemCount} titles loaded${if (paged.loadState.append is androidx.paging.LoadState.Loading) "…" else ""}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            items(
                count = paged.itemCount,
                key = { idx -> paged.peek(idx)?.id ?: idx },
            ) { idx ->
                val m = paged[idx] ?: return@items
                PosterCard(title = m.name, poster = m.poster, subtitle = m.year?.toString()) { onOpen(m.id) }
            }
        }
    }
}
