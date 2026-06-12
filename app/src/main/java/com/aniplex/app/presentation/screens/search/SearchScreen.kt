package com.aniplex.app.presentation.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.*
import coil.compose.AsyncImage
import com.aniplex.app.domain.model.Anime
import com.aniplex.app.presentation.components.AnimeCard
import com.aniplex.app.theme.BackgroundVoid
import com.aniplex.app.theme.CrunchyrollOrange
import com.aniplex.app.theme.NetflixRed
import com.aniplex.app.theme.SurfaceDark
import com.aniplex.app.theme.SurfaceDarkVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var isSearchFieldFocused by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Detect scrolling boundary for infinite paging
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Search Bar Outlined Input with keyboard controller support
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.onQueryChange(it)
                    isSearchFieldFocused = true
                },
                placeholder = { Text("Search anime...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.LightGray
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            val activeFiltersCount = (if (selectedType != null) 1 else 0) +
                                    (if (selectedStatus != null) 1 else 0) +
                                    (if (selectedSort != null) 1 else 0) +
                                    (if (selectedLanguage != null) 1 else 0) +
                                    selectedGenres.size
                            BadgedBox(
                                badge = {
                                    if (activeFiltersCount > 0) {
                                        Badge(containerColor = CrunchyrollOrange, contentColor = Color.White) {
                                            Text(activeFiltersCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (activeFiltersCount > 0) CrunchyrollOrange else Color.LightGray
                                )
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.performSearch()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        isSearchFieldFocused = false
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrunchyrollOrange,
                    unfocusedBorderColor = SurfaceDarkVariant,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        isSearchFieldFocused = state.isFocused
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (recentSearches.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Clear All",
                            color = CrunchyrollOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentSearches) { query ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.onQueryChange(query)
                                        viewModel.performSearch()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = query,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.removeRecentSearch(query) }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Main Search Content Areas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        SearchIdleState(
                            onGenreClick = { genre ->
                                viewModel.toggleGenre(genre)
                                viewModel.performSearch()
                            }
                        )
                    }
                    is SearchUiState.Loading -> {
                        SearchShimmerLoader()
                    }
                    is SearchUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.results) { anime ->
                                AnimeCard(anime = anime, onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.recordSearchQuery(searchQuery)
                                    onAnimeClick(it)
                                })
                            }
                            if (state.hasNextPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = CrunchyrollOrange, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                    is SearchUiState.Empty -> {
                        SearchEmptyState(
                            query = searchQuery,
                            selectedType = selectedType,
                            selectedStatus = selectedStatus,
                            selectedLanguage = selectedLanguage,
                            selectedGenres = selectedGenres,
                            onClearFilters = {
                                viewModel.clearFilters()
                                viewModel.onQueryChange("")
                                viewModel.performSearch()
                            }
                        )
                    }
                    is SearchUiState.Error -> {
                        SearchErrorState(
                            message = state.message,
                            onRetry = { viewModel.performSearch() }
                        )
                    }
                }

                // 3. Debounced Suggestions Dropdown Overlay with elegant glassmorphic dark design
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSearchFieldFocused && suggestions.isNotEmpty() && searchQuery.length >= 2,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.98f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(suggestions) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isSearchFieldFocused = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.recordSearchQuery(searchQuery)
                                            onAnimeClick(item.id)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.poster,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.type} • ${item.duration}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Advanced Filters Modal Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = SurfaceDark,
                contentColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.clearFilters() },
                            colors = ButtonDefaults.textButtonColors(contentColor = CrunchyrollOrange)
                        ) {
                            Text("Reset All", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Type Filter
                        item {
                            FilterSectionTitle("Type")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val types = listOf("tv", "movie", "ova", "ona", "special")
                                types.forEach { type ->
                                    FilterChip(
                                        selected = selectedType == type,
                                        onClick = { viewModel.selectedType.value = if (selectedType == type) null else type },
                                        label = { Text(type.uppercase()) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrunchyrollOrange,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDarkVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        // Status Filter
                        item {
                            FilterSectionTitle("Status")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val statuses = mapOf(
                                    "currently-airing" to "Airing",
                                    "finished-airing" to "Finished",
                                    "not-yet-aired" to "Upcoming"
                                )
                                statuses.forEach { (key, display) ->
                                    FilterChip(
                                        selected = selectedStatus == key,
                                        onClick = { viewModel.selectedStatus.value = if (selectedStatus == key) null else key },
                                        label = { Text(display) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrunchyrollOrange,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDarkVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        // Language / Audio Filter
                        item {
                            FilterSectionTitle("Language")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val langs = mapOf("sub" to "Sub", "dub" to "Dub", "sub-dub" to "Both")
                                langs.forEach { (key, display) ->
                                    FilterChip(
                                        selected = selectedLanguage == key,
                                        onClick = { viewModel.selectedLanguage.value = if (selectedLanguage == key) null else key },
                                        label = { Text(display) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrunchyrollOrange,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDarkVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        // Sort Order
                        item {
                            FilterSectionTitle("Sort Order")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sorts = mapOf(
                                    "default" to "Default",
                                    "recently-added" to "Newest",
                                    "most-popular" to "Popular",
                                    "alphabetical" to "A-Z"
                                )
                                sorts.forEach { (key, display) ->
                                    FilterChip(
                                        selected = selectedSort == key,
                                        onClick = { viewModel.selectedSort.value = if (selectedSort == key) null else key },
                                        label = { Text(display) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrunchyrollOrange,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDarkVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        // Genres (multi-select flow row chips)
                        item {
                            FilterSectionTitle("Genres")
                            val genres = listOf(
                                "action", "adventure", "comedy", "drama", "fantasy",
                                "romance", "sci-fi", "slice-of-life", "supernatural", "thriller"
                            )
                            OptFlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                genres.forEach { genre ->
                                    val isSelected = selectedGenres.contains(genre)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleGenre(genre) },
                                        label = { Text(genre.replace("-", " ").replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrunchyrollOrange,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDarkVariant,
                                            labelColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Apply Button
                    Button(
                        onClick = {
                            showFilterSheet = false
                            viewModel.performSearch()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("APPLY FILTERS", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.LightGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SearchIdleState(onGenreClick: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "idle_glow")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = CrunchyrollOrange.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Discover Anime",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type any keyword or browse popular genres below.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "POPULAR GENRES",
                color = CrunchyrollOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val popularGenres = listOf("action", "comedy", "fantasy", "sci-fi", "romance", "thriller")
            OptFlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                popularGenres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(SurfaceDark, RoundedCornerShape(18.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                            .clickable { onGenreClick(genre) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = genre.replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchEmptyState(
    query: String,
    selectedType: String?,
    selectedStatus: String?,
    selectedLanguage: String?,
    selectedGenres: Set<String>,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = NetflixRed.copy(alpha = 0.8f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Results Found",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            val filterTexts = mutableListOf<String>()
            selectedType?.let { filterTexts.add(it.uppercase()) }
            selectedLanguage?.let { filterTexts.add(it.uppercase()) }
            if (selectedGenres.isNotEmpty()) {
                filterTexts.add("${selectedGenres.size} genres")
            }
            
            val messageText = if (query.isNotBlank()) {
                if (filterTexts.isNotEmpty()) {
                    "No ${filterTexts.joinToString(", ")} found for \"$query\""
                } else {
                    "No anime found for \"$query\""
                }
            } else {
                "No anime matches the selected filters."
            }

            Text(
                text = messageText,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We couldn't find any matching results. Please check your spelling or try resetting filters.",
                fontSize = 12.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Reset Search & Filters", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NetflixRed,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search Request Failed",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchShimmerLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDarkVariant.copy(alpha = alphaAnim))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f * alphaAnim))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.06f * alphaAnim))
                    )
                }
            }
        }
    }
}

// Simple FlowRow helper for items wrap-around
@Composable
fun OptFlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
