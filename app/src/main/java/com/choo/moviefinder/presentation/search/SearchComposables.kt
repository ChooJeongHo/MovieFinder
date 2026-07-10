package com.choo.moviefinder.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.presentation.adapter.MoviePagingAdapter.ViewMode
import java.util.Locale
import kotlinx.coroutines.launch

// XML: TextInputLayout(search_input_layout) + TextInputEditText(et_search)
// Compose: 상태 호이스팅(state hoisting) 패턴 — 이 컴포저블은 텍스트를 직접 들고 있지 않고
//          query/onQueryChange를 호출부(SearchFragment)에서 주입받는다.
//          기존 코드에서 binding.etSearch.setText(...)로 외부에서 값을 바꾸던 지점들
//          (최근 검색어 클릭, 추천 칩 클릭)은 동일하게 query 상태값만 바꿔주면 재구성된다.
@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cd_clear_search),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

// XML: RecyclerView(rv_search_results) + MoviePagingAdapter + GridLayoutManager/LinearLayoutManager
// Compose: collectAsLazyPagingItems()로 얻은 LazyPagingItems를 그대로 LazyVerticalGrid/LazyColumn에 넘긴다.
//          viewMode에 따라 컴포저블 자체를 분기 — PagingDataAdapter.viewMode + notifyDataSetChanged()로
//          뷰타입을 바꾸던 방식보다 훨씬 선언적이다.
// XML: fab_scroll_top의 RecyclerView.addOnScrollListener → RecyclerView가 사라졌으므로
//      LazyGridState/LazyListState.canScrollBackward를 콜백으로 Fragment에 전달하는 방식으로 재구현.
//      onScrollControllerReady는 "맨 위로 스크롤" 동작 자체를 람다로 등록해두는 콜백 브릿지.
@Composable
fun SearchResultsList(
    pagingItems: LazyPagingItems<Movie>,
    viewMode: ViewMode,
    spanCount: Int,
    onMovieClick: (Int) -> Unit,
    onScrollStateChanged: (canScrollBack: Boolean) -> Unit,
    onScrollControllerReady: (scrollToTop: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (viewMode) {
        ViewMode.GRID -> {
            val gridState = rememberLazyGridState()
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                onScrollControllerReady { scope.launch { gridState.animateScrollToItem(0) } }
            }
            LaunchedEffect(gridState) {
                snapshotFlow { gridState.canScrollBackward }.collect(onScrollStateChanged)
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(spanCount),
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    pagingItems[index]?.let { movie ->
                        MoviePosterCard(movie = movie, onClick = { onMovieClick(movie.id) })
                    }
                }
            }
        }

        ViewMode.LIST -> {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                onScrollControllerReady { scope.launch { listState.animateScrollToItem(0) } }
            }
            LaunchedEffect(listState) {
                snapshotFlow { listState.canScrollBackward }.collect(onScrollStateChanged)
            }
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    pagingItems[index]?.let { movie ->
                        MovieListRow(movie = movie, onClick = { onMovieClick(movie.id) })
                    }
                }
            }
        }
    }
}

// XML: item_movie_grid.xml (ImageView + CircularRatingView)
// Compose 단순화: 원형 평점 뷰(CircularRatingView)는 핵심 마이그레이션 범위 밖이라
//                "★ 7.2" 형태의 텍스트로 단순화했다 (추후 별도 컴포저블로 이식 가능).
@Composable
private fun MoviePosterCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = ImageUrlProvider.posterUrl(movie.posterPath),
            contentDescription = movie.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun MovieListRow(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageUrlProvider.posterUrl(movie.posterPath),
            contentDescription = movie.title,
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // CircularRatingView와 동일하게 Locale.US 고정 — 로케일별 소수점 구분자(콤마/점) 차이 방지
                Text(
                    text = "★ " + String.format(Locale.US, "%.1f", movie.voteAverage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
