package com.crucialspace.app.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.local.MemoryEntity
import com.crucialspace.app.data.repo.MemoryRepository
import com.crucialspace.app.data.remote.ApiService
import com.crucialspace.app.data.remote.EmbedRequest
import com.crucialspace.app.data.ai.GeminiClient
import com.crucialspace.app.settings.SettingsStore
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.crucialspace.app.ui.components.IconPillButton

@Composable
fun SearchScreen(onOpenDetail: (String) -> Unit, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val gemini = remember { GeminiClient(context) }
    val repo = remember { MemoryRepository(db(context)) }
    val allItems by repo.observeAll().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<MemoryEntity>() }

    // Gold animated border like other inputs
    val anim = rememberInfiniteTransition(label = "gold")
    val shift by anim.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Restart
        ), label = "goldAnim"
    )
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFE082), Color(0xFFFFD54F), Color(0xFFFFF59D)),
        start = androidx.compose.ui.geometry.Offset(shift, 0f),
        end = androidx.compose.ui.geometry.Offset(shift + 300f, 200f)
    )

    LaunchedEffect(query, allItems) {
        if (query.isBlank()) {
            results.clear()
            results.addAll(allItems)
        } else {
            val q = query
            delay(200)
            try {
                val emb = withContext(Dispatchers.IO) {
                    gemini.embed(q)
                }
                val scored = allItems.mapNotNull { m ->
                    val vec = parseEmbedding(m.embeddingJson)
                    if (vec == null || vec.isEmpty()) null else m to cosine(emb, vec)
                }.sortedByDescending { it.second }.map { it.first }
                results.clear(); results.addAll(scored)
            } catch (_: Throwable) {
                results.clear(); results.addAll(allItems)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconPillButton(
                    onClick = onBack,
                    icon = Icons.Filled.ArrowBack,
                    size = 48.dp
                )
                Text("Search", style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Type to search…") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
            Spacer(Modifier.height(12.dp))

            // Scrollable 2-column grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { m ->
                    SearchResultCard(item = m, onClick = { onOpenDetail(m.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: MemoryEntity, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    androidx.compose.material3.Surface(onClick = onClick, shape = shape, tonalElevation = 2.dp) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(0.dp)) {
            if (!item.imageUri.isNullOrBlank()) {
                AsyncImage(model = item.imageUri, contentDescription = null, modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val g = Brush.linearGradient(listOf(Color(0xFF2B2B33), Color(0xFF1F2933), Color(0xFF1B2A24), Color(0xFF3A2626)))
                    drawRect(brush = g, size = this.size)
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(64.dp).background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x99000000), Color(0xE6000000)))))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(text = item.aiTitle ?: item.noteText ?: "", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.imageUri.isNullOrBlank() && !item.aiSummary.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = item.aiSummary!!, color = Color.White.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun parseEmbedding(json: String?): List<Double>? {
    if (json.isNullOrBlank()) return null
    return runCatching {
        val trimmed = json.trim()
        val inner = trimmed.removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) emptyList() else inner.split(',').map { it.trim().toDouble() }
    }.getOrNull()
}

private fun cosine(a: List<Double>, b: List<Double>): Double {
    if (a.isEmpty() || b.isEmpty()) return -1.0
    val n = kotlin.math.min(a.size, b.size)
    var dot = 0.0
    var an = 0.0
    var bn = 0.0
    for (i in 0 until n) {
        val x = a[i]
        val y = b[i]
        dot += x * y
        an += x * x
        bn += y * y
    }
    val denom = kotlin.math.sqrt(an) * kotlin.math.sqrt(bn)
    return if (denom == 0.0) -1.0 else dot / denom
}


