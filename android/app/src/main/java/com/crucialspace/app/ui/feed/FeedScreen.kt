package com.crucialspace.app.ui.feed

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.crucialspace.app.ui.components.PillButton
import com.crucialspace.app.ui.components.OutlinedPillButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.crucialspace.app.data.local.MemoryEntity
import com.crucialspace.app.data.local.CollectionEntity
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.repo.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.crucialspace.app.data.remote.AiReminder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Send
import android.content.Intent
import com.crucialspace.app.share.ShareTargetActivity
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.media.MediaPlayer
import android.net.Uri
 

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(onOpenSettings: () -> Unit, onOpenDetail: (String) -> Unit, onOpenSearch: () -> Unit, onOpenCollection: (String) -> Unit) {
	val context = getContext()
    val repo = MemoryRepository(db(context))
    val items by repo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val moshi = Moshi.Builder().build()
    val remindersType = Types.newParameterizedType(List::class.java, AiReminder::class.java)
    val remindersAdapter = moshi.adapter<List<AiReminder>>(remindersType)

    val selectedIds = remember { mutableStateOf(setOf<String>()) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showAddToCollections by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Space", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            if (selectedIds.value.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showBulkMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Actions") }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showBulkMenu,
                        onDismissRequest = { showBulkMenu = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp
                    ) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Add to collections") }, onClick = { showBulkMenu = false; showAddToCollections = true })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Delete memories") }, onClick = { showBulkMenu = false; showDeleteConfirm = true }, leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) })
                    }
                }
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Content + FAB layered in a Box so the FAB doesn't consume layout space
        val contextObj = getContext()
        Box(modifier = Modifier.fillMaxSize()) {
            // Compute content data once
            val latest = items.firstOrNull { it.status == "SYNCED" } ?: items.firstOrNull()
            val upcomingPairs = items.flatMap { e ->
                val json = e.aiRemindersJson
                val list = if (json.isNullOrBlank()) emptyList() else runCatching { remindersAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
                list.filter { it.event.isNotBlank() }.map { e to it }
            }

            // Collection spotlight selection (recent activity)
            var spotlight by remember { mutableStateOf<CollectionSpotlight?>(null) }
            LaunchedEffect(items) {
                withContext(Dispatchers.IO) {
                    val dao = db(context).memoryDao()
                    val cols = dao.listCollections()
                    var best: CollectionSpotlight? = null
                    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                    cols.forEach { c ->
                        val mems = dao.listMemoriesForCollection(c.id)
                        if (mems.size >= 4) {
                            val recent = mems.count { it.createdAt >= weekAgo }
                            val total = mems.size
                            val score = recent * 3 + total
                            val cand = CollectionSpotlight(c, mems, recent, total, score)
                            if (best == null || score > best!!.score) best = cand
                        }
                    }
                    spotlight = best
                }
            }

            // Make the entire content scroll, not the grid
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                // small spacer so content tucks under overlay fade
                Spacer(Modifier.height(4.dp))
            // Smart hero carousel (Soon Reminder -> Latest -> [Spotlight] -> Throwback)
            run {
                val heroes = remember(items, upcomingPairs, spotlight) {
                    val list = mutableListOf<Hero>()
                    val now = java.time.Instant.now()
                    val soonLimit = now.plus(java.time.Duration.ofHours(3))
                    val soon = upcomingPairs
                        .mapIndexed { idx, pair -> idx to pair }
                        .filter { (_, pair) ->
                            val t = runCatching { java.time.Instant.parse(pair.second.datetime) }.getOrNull()
                            t != null && t.isAfter(now) && t.isBefore(soonLimit)
                        }
                        .minByOrNull { (_, pair) -> runCatching { java.time.Instant.parse(pair.second.datetime).toEpochMilli() }.getOrDefault(Long.MAX_VALUE) }
                        ?.second
                    if (soon != null) list.add(Hero.ReminderHero(soon.first, soon.second))

                    latest?.let { list.add(Hero.MemoryHero(it)) }

                    // optional collection spotlight
                    spotlight?.let { list.add(Hero.CollectionHero(it.collection, it.memories)) }

                    val target = java.time.Instant.now().minus(java.time.Duration.ofDays(30))
                    val tol = java.time.Duration.ofDays(3).toMillis()
                    val throwback = items.minByOrNull { kotlin.math.abs(it.createdAt - target.toEpochMilli()) }
                        ?.takeIf { kotlin.math.abs(it.createdAt - target.toEpochMilli()) <= tol }
                    throwback?.let { list.add(Hero.MemoryHero(it)) }
                    list
                }
                if (heroes.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { heroes.size })
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(220.dp)) { pg ->
                        when (val h = heroes[pg]) {
                            is Hero.ReminderHero -> ReminderHeroCard(h, onOpenDetail = onOpenDetail)
                            is Hero.MemoryHero -> MemoryHeroCard(h, onOpenDetail = onOpenDetail)
                            is Hero.CollectionHero -> CollectionHeroCard(h, onOpenDetail = onOpenDetail, onOpenCollection = onOpenCollection)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(heroes.size) { i ->
                            Surface(shape = RoundedCornerShape(50), color = if (pagerState.currentPage == i) Color.White else Color.Gray, tonalElevation = 0.dp) {
                                Box(Modifier.size(8.dp)) {}
                            }
                            if (i != heroes.lastIndex) Spacer(Modifier.width(8.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Upcoming reminders
            if (upcomingPairs.isNotEmpty()) {
                Text("Upcoming", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingPairs.take(8).forEachIndexed { index, (entity, r) ->
                        UpcomingRowItem(
                            text = r.event,
                            time = r.datetime,
                            onClick = { onOpenDetail(entity.id) },
                            onToggle = { /* todo: persist todo state if mapped */ }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
                // Memory grid (non-lazy), grouped by local day
            Text("Memory", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val groups = items.groupBy { dayKey(it.createdAt) }
            groups.toSortedMap(compareByDescending { it }).forEach { (day, list) ->
                Text(formatDay(day), style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    list.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { m ->
                                val isSelected = selectedIds.value.contains(m.id)
                                Box(modifier = Modifier.weight(1f)) {
                                    MemoryCard(
                                        item = m,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (selectedIds.value.isNotEmpty()) {
                                                selectedIds.value = selectedIds.value.toMutableSet().apply { if (contains(m.id)) remove(m.id) else add(m.id) }
                                            } else onOpenDetail(m.id)
                                        },
                                        onLongPress = { selectedIds.value = selectedIds.value.toMutableSet().apply { add(m.id) } }
                                    )
                                }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(72.dp))
            }

            // Top overlay fade (thin and subtle) — must be sibling in Box scope
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Quick Enrich button (FAB)
            FloatingActionButton(
                onClick = {
                    val intent = Intent(contextObj, ShareTargetActivity::class.java)
                    intent.action = Intent.ACTION_SEND
                    intent.type = "text/plain"
                    contextObj.startActivity(intent)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enrich")
            }
        }
        }

        if (showDeleteConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                // Center both buttons in one row
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                        OutlinedPillButton(
                            onClick = {
                                showDeleteConfirm = false
                                val ids = selectedIds.value.toList()
                                val ctx = context
                                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { repo.deleteMany(ids, ctx) }
                                selectedIds.value = emptySet()
                            },
                            text = "Delete"
                        )
                        OutlinedPillButton(
                            onClick = { showDeleteConfirm = false },
                            text = "Cancel"
                        )
                    }
                },
                dismissButton = {},
                title = { Text("Delete selected?") },
                text = { Text("This will delete ${selectedIds.value.size} memories.") }
            )
        }

        if (showAddToCollections) {
            val dao = db(context).memoryDao()
            var all by remember { mutableStateOf(emptyList<com.crucialspace.app.data.local.CollectionEntity>()) }
            var selectedColls by remember { mutableStateOf(emptySet<String>()) }
            LaunchedEffect(selectedIds.value) {
                withContext(Dispatchers.IO) { all = dao.listCollections() }
                selectedColls = emptySet()
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddToCollections = false },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                confirmButton = {
                    PillButton(
                        onClick = {
                            val memIds = selectedIds.value.toList()
                            showAddToCollections = false
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                val collRepo = com.crucialspace.app.data.repo.CollectionRepository(db(context))
                                memIds.forEach { mid -> selectedColls.forEach { cid -> collRepo.addMemoryToCollection(cid, mid) } }
                            }
                            selectedIds.value = emptySet()
                        },
                        text = "Save"
                    )
                },
                dismissButton = { 
                    OutlinedPillButton(
                        onClick = { showAddToCollections = false },
                        text = "Cancel"
                    ) 
                },
                title = { Text("Add to collections") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(all, key = { it.id }) { c ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selectedColls.contains(c.id), onCheckedChange = { ch ->
                                    selectedColls = selectedColls.toMutableSet().apply { if (ch) add(c.id) else remove(c.id) }
                                })
                                Text(c.name)
                            }
                        }
                    }
                }
            )
        }
	}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoryCard(item: MemoryEntity, isSelected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit) {
    val shape = MaterialTheme.shapes.large // 24dp - Expressive!
    // Animated gold border when selected
    val border = if (isSelected) {
        val anim = rememberInfiniteTransition(label = "goldSelect")
        val shift by anim.animateFloat(
            initialValue = 0f,
            targetValue = 600f,
            animationSpec = infiniteRepeatable(animation = tween(2500), repeatMode = RepeatMode.Restart),
            label = "goldAnim"
        )
        val goldBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFE082), Color(0xFFFFD54F), Color(0xFFFFF59D)),
            start = androidx.compose.ui.geometry.Offset(shift, 0f),
            end = androidx.compose.ui.geometry.Offset(shift + 300f, 200f)
        )
        BorderStroke(3.dp, goldBrush)
    } else null
    Surface(onClick = onClick, shape = shape, tonalElevation = 2.dp, border = border) {
        Box(modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
        ) {
            if (!item.imageUri.isNullOrBlank()) {
                AsyncImage(model = item.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // bottom gradient and title for image cards
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x88000000), Color(0xCC000000))))
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!item.audioUri.isNullOrBlank()) {
                            Icon(
                                Icons.Filled.Mic, 
                                contentDescription = "Audio", 
                                tint = Color(0xFFFFD54F), 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = item.aiTitle ?: item.noteText ?: "",
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (!item.aiSummary.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        MarkdownCompactText(
                            text = item.aiSummary!!,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // Text/voice-only card: lighter gradient background and top-aligned content
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val g = Brush.linearGradient(listOf(Color(0xFF3A3A46), Color(0xFF2E3A46), Color(0xFF294238), Color(0xFF513A3A)))
                    drawRect(brush = g, size = this.size)
                }
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    Text(
                        text = item.aiTitle ?: item.noteText ?: "",
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        if (!item.audioUri.isNullOrBlank()) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        }
                        MarkdownCompactText(
                            text = item.aiSummary ?: item.noteText ?: "",
                            color = Color.White.copy(alpha = 0.95f),
                            maxLines = 6,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
			if (item.status == "PENDING") PendingGlowOverlay()
		}
	}
}

@Composable
private fun HeroCard(item: MemoryEntity, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(
        onClick = onClick,
        shape = shape,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(shape)) {
            // subtle multicolor background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF434159),
                                Color(0xFF2F3D4F),
                                Color(0xFF2A3A35),
                                Color(0xFF4E3B3B)
                            )
                        )
                    )
                    .blur(24.dp)
            )
            Column(modifier = Modifier.matchParentSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                MarkdownCompactText(
                    text = item.aiSummary ?: item.noteText ?: "Saved item",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 3
                )
                Text(
                    text = when (item.status) {
                        "PENDING" -> "Processing…"
                        "ERROR" -> "Tap to retry"
                        else -> "View details"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (item.status == "PENDING") PendingGlowOverlay()
        }
    }
}

// --- Smart Hero models & cards ---
private sealed class Hero {
    data class ReminderHero(val memory: MemoryEntity, val reminder: AiReminder): Hero()
    data class MemoryHero(val memory: MemoryEntity): Hero()
    data class CollectionHero(val collection: CollectionEntity, val memories: List<MemoryEntity>): Hero()
}

@Composable
private fun ReminderHeroCard(h: Hero.ReminderHero, onOpenDetail: (String) -> Unit) {
    val shape = MaterialTheme.shapes.large
    val ctx = getContext()
    Surface(
        onClick = { onOpenDetail(h.memory.id) },
        shape = shape,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(shape)) {
            if (!h.memory.imageUri.isNullOrBlank()) {
                AsyncImage(model = h.memory.imageUri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF434159),
                                    Color(0xFF2F3D4F),
                                    Color(0xFF2A3A35),
                                    Color(0xFF4E3B3B)
                                )
                            )
                        )
                        .blur(24.dp)
                )
            }
            // bottom scrim (higher for readability)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xAA000000), Color(0xCC000000))))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = h.reminder.event, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = formatShortTime(h.reminder.datetime), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = {
                        // Done: remove this reminder
                        val moshi = Moshi.Builder().build()
                        val t = Types.newParameterizedType(List::class.java, AiReminder::class.java)
                        val ad = moshi.adapter<List<AiReminder>>(t)
                        val list = (h.memory.aiRemindersJson?.let { runCatching { ad.fromJson(it) }.getOrNull() } ?: emptyList()).toMutableList()
                        val index = list.indexOfFirst { it.event == h.reminder.event && it.datetime == h.reminder.datetime }
                        if (index >= 0) list.removeAt(index)
                        val newJson = ad.toJson(list)
                        // best-effort cancel worker
                        runCatching { com.crucialspace.app.work.ReminderWorker.cancel(ctx, "reminder-${h.memory.id}-${index}") }
                        // persist in background
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { db(ctx).memoryDao().updateReminders(h.memory.id, newJson) }
                    }, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) { Text("Done") }
                    androidx.compose.material3.OutlinedButton(onClick = {
                        // Snooze 1h: shift datetime
                        val moshi = Moshi.Builder().build()
                        val t = Types.newParameterizedType(List::class.java, AiReminder::class.java)
                        val ad = moshi.adapter<List<AiReminder>>(t)
                        val list = (h.memory.aiRemindersJson?.let { runCatching { ad.fromJson(it) }.getOrNull() } ?: emptyList()).toMutableList()
                        val index = list.indexOfFirst { it.event == h.reminder.event && it.datetime == h.reminder.datetime }
                        if (index >= 0) {
                            val cur = runCatching { java.time.OffsetDateTime.parse(list[index].datetime) }.getOrNull()
                            val next = (cur ?: java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)).plusHours(1)
                            list[index] = list[index].copy(datetime = next.toString())
                            val newJson = ad.toJson(list)
                            // persist and reschedule
                            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                                db(ctx).memoryDao().updateReminders(h.memory.id, newJson)
                                runCatching {
                                    com.crucialspace.app.work.ReminderWorker.cancel(ctx, "reminder-${h.memory.id}-${index}")
                                    com.crucialspace.app.work.ReminderWorker.schedule(
                                        ctx,
                                        uniqueName = "reminder-${h.memory.id}-${index}",
                                        whenIso = next.toString(),
                                        title = h.reminder.event,
                                        text = h.memory.aiTitle ?: h.memory.noteText ?: "",
                                        memoryId = h.memory.id,
                                        reminderIndex = index
                                    )
                                }
                            }
                        }
                    }, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) { Text("Snooze 1h") }
                }
            }
        }
    }
}

private data class CollectionSpotlight(
    val collection: CollectionEntity,
    val memories: List<MemoryEntity>,
    val recentAdds: Int,
    val total: Int,
    val score: Int
)

@Composable
private fun CollectionHeroCard(h: Hero.CollectionHero, onOpenDetail: (String) -> Unit, onOpenCollection: (String) -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(onClick = { onOpenCollection(h.collection.id) }, shape = shape, tonalElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(shape)) {
            // Mosaic background (up to 4 thumbnails)
            Box(modifier = Modifier.matchParentSize()) {
                val imgs = h.memories.filter { !it.imageUri.isNullOrBlank() }.take(4)
                if (imgs.isNotEmpty()) {
                    Row(Modifier.matchParentSize()) {
                        Column(Modifier.weight(1f)) {
                            val a = imgs.getOrNull(0)
                            val b = imgs.getOrNull(1)
                            if (a != null) AsyncImage(model = a.imageUri, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                            else MosaicPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                            if (b != null) AsyncImage(model = b.imageUri, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                            else MosaicPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                        Column(Modifier.weight(1f)) {
                            val c = imgs.getOrNull(2)
                            val d = imgs.getOrNull(3)
                            if (c != null) AsyncImage(model = c.imageUri, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                            else MosaicPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                            if (d != null) AsyncImage(model = d.imageUri, contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                            else MosaicPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                } else {
                    MosaicPlaceholder(modifier = Modifier.matchParentSize())
                }
            }
            // bottom scrim (higher for readability)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xAA000000), Color(0xCC000000))))
            )
            val recentAdds = h.memories.count { it.createdAt >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = h.collection.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = if (recentAdds > 0) "$recentAdds added this week • ${h.memories.size} total" else "${h.memories.size} memories", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = { onOpenCollection(h.collection.id) }, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) { Text("View") }
                    val pool = h.memories
                    if (pool.isNotEmpty()) {
                        androidx.compose.material3.OutlinedButton(onClick = {
                            val day = java.time.LocalDate.now().dayOfYear
                            val pick = pool[day % pool.size]
                            onOpenDetail(pick.id)
                        }, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) { Text("Random") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MosaicPlaceholder(modifier: Modifier) {
    Box(modifier = modifier.background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF434159),
                Color(0xFF2F3D4F),
                Color(0xFF2A3A35),
                Color(0xFF4E3B3B)
            )
        )
    ))
}

@Composable
private fun MemoryHeroCard(h: Hero.MemoryHero, onOpenDetail: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val context = getContext()
    var player by remember(h.memory.audioUri) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(h.memory.audioUri) {
        onDispose { try { player?.release() } catch (_: Exception) {} }
    }
    Surface(onClick = { onOpenDetail(h.memory.id) }, shape = shape, tonalElevation = 4.dp) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(shape)) {
            if (!h.memory.imageUri.isNullOrBlank()) {
                AsyncImage(model = h.memory.imageUri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF434159),
                                    Color(0xFF2F3D4F),
                                    Color(0xFF2A3A35),
                                    Color(0xFF4E3B3B)
                                )
                            )
                        )
                        .blur(24.dp)
                )
            }
            // bottom scrim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xAA000000), Color(0xCC000000))))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = h.memory.aiTitle ?: h.memory.noteText ?: "", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                if (!h.memory.aiSummary.isNullOrBlank()) {
                    MarkdownCompactText(
                        text = h.memory.aiSummary!!,
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 4,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!h.memory.audioUri.isNullOrBlank()) {
                        androidx.compose.material3.OutlinedButton(onClick = {
                            if (player == null) player = MediaPlayer.create(context, Uri.parse(h.memory.audioUri))
                            player?.let {
                                if (it.isPlaying) { it.pause(); isPlaying = false } else { it.start(); isPlaying = true }
                            }
                        }, shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }
                    val urlsType = remember { Types.newParameterizedType(List::class.java, String::class.java) }
                    val urls = remember(h.memory.aiUrlsJson) { h.memory.aiUrlsJson?.let { Moshi.Builder().build().adapter<List<String>>(urlsType).fromJson(it) } ?: emptyList() }
                    urls.take(2).forEach { u ->
                        androidx.compose.material3.OutlinedButton(onClick = {
                            runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(u))) }
                        }, shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp, Color.White), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33000000), contentColor = Color.White)) {
                            Text("Link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownCompactText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            val lines = text.split('\n')
            lines.forEachIndexed { idx, raw ->
                var line = raw.trimEnd()
                // strip atx headings
                if (line.startsWith("### ")) line = line.removePrefix("### ")
                else if (line.startsWith("## ")) line = line.removePrefix("## ")
                else if (line.startsWith("# ")) line = line.removePrefix("# ")
                // bullets
                if (line.startsWith("- ") || line.startsWith("* ") || line.matches(Regex("^\\d+\\. .*"))) {
                    append("• ")
                    line = line.replace(Regex("^(?:- |\\* |\\d+\\. )"), "")
                }
                append(buildBoldInline(line))
                if (idx != lines.lastIndex) append('\n')
            }
        }
    }
    Text(annotated, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

// outline variants removed per request

private fun buildBoldInline(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        var bold = false
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                bold = !bold
                i += 2
                continue
            }
            val start = i
            while (i < text.length && !(i + 1 < text.length && text[i] == '*' && text[i + 1] == '*')) {
                i++
            }
            val chunk = text.substring(start, i)
            if (bold) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(chunk) }
            } else append(chunk)
        }
    }
}

@Composable
private fun PendingGlowOverlay() {
	val shimmerColors = listOf(
		Color.Transparent,
		Color.White.copy(alpha = 0.35f),
		Color.Transparent
	)
	val transition = rememberInfiniteTransition(label = "glow")
	val progress = transition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Restart
		), label = "glowAnim"
	)
	Canvas(modifier = Modifier.fillMaxSize()) {
		val w = size.width
		val band = w * 0.5f // width of the bright band
		val startX = -band
		val endX = w + band
		val x = startX + (endX - startX) * progress.value
		val brush = Brush.linearGradient(
			colors = shimmerColors,
			start = androidx.compose.ui.geometry.Offset(x - band, 0f),
			end = androidx.compose.ui.geometry.Offset(x + band, 0f)
		)
		drawRect(brush = brush, size = this.size)
	}
}

@Composable
private fun UpcomingRow(reminders: List<AiReminder>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { r -> ReminderChip(text = r.event) }
    }
}

@Composable
private fun RowScope.ReminderChip(text: String) {
    Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UpcomingRowItem(text: String, time: String?, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), tonalElevation = 0.dp, color = Color.Transparent, border = BorderStroke(2.dp, Color.Gray)) {
                Box(modifier = Modifier.size(16.dp)) {}
            }
            Text(text = formatShortTime(time), style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            Text(text = text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(text = "›", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatShortTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val dt = java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        val date = dt.toLocalDate()
        val time = dt.toLocalTime().withSecond(0).withNano(0)
        val d = java.time.format.DateTimeFormatter.ofPattern("dd MMM")
        val t = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        "${t.format(time)} ${d.format(date)}"
    }.getOrElse { "" }
}

@Composable
private fun getContext(): Context = androidx.compose.ui.platform.LocalContext.current

private fun dayKey(epochMillis: Long): Long {
    val zdt = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
    val start = zdt.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())
    return start.toInstant().toEpochMilli()
}

private fun formatDay(dayEpoch: Long): String {
    val z = java.time.Instant.ofEpochMilli(dayEpoch).atZone(java.time.ZoneId.systemDefault())
    val d = java.time.format.DateTimeFormatter.ofPattern("dd MMM")
    return d.format(z)
}
