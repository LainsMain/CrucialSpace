package com.crucialspace.app.ui.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.repo.CollectionRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionsScreen(onOpenCollection: (String) -> Unit, onOpenSearch: () -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    val ctx = LocalContext.current
    val repo = remember { CollectionRepository(db(ctx)) }
    val collections by repo.observeAll().collectAsState(initial = emptyList())

    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("date-created-desc") } // date-created-desc, date-created-asc, name-asc, name-desc
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Map to store last modified times (most recent memory in each collection)
    var lastModifiedMap by remember { mutableStateOf(mapOf<String, Long>()) }
    LaunchedEffect(collections) {
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, Long>()
            collections.forEach { col ->
                val mems = db(ctx).memoryDao().listMemoriesForCollection(col.id)
                val lastMod = mems.maxOfOrNull { it.createdAt } ?: col.createdAt
                map[col.id] = lastMod
            }
            lastModifiedMap = map
        }
    }
    
    // Filter and sort collections
    val filteredCollections = remember(collections, searchQuery, sortBy, lastModifiedMap) {
        val filtered = if (searchQuery.isBlank()) collections
        else collections.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description?.contains(searchQuery, ignoreCase = true) == true
        }
        
        when (sortBy) {
            "date-created-desc" -> filtered.sortedByDescending { it.createdAt }
            "date-created-asc" -> filtered.sortedBy { it.createdAt }
            "name-asc" -> filtered.sortedBy { it.name.lowercase() }
            "name-desc" -> filtered.sortedByDescending { it.name.lowercase() }
            "last-modified" -> filtered.sortedByDescending { lastModifiedMap[it.id] ?: it.createdAt }
            else -> filtered
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar matching Feed
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Collections", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            if (selectedIds.isNotEmpty()) {
                IconButton(onClick = { showBulkMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = showBulkMenu, onDismissRequest = { showBulkMenu = false }) {
                    DropdownMenuItem(text = { Text("Delete collections") }, onClick = {
                        showBulkMenu = false
                        showDeleteConfirm = true
                    }, leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) })
                }
            }
            // Sort, Search, and Settings
            androidx.compose.material3.Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        androidx.compose.material3.IconButton(onClick = { showSortMenu = true }) {
                            androidx.compose.material3.Icon(Icons.Filled.List, contentDescription = "Sort", tint = Color.White)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = { sortBy = "date-created-desc"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = { sortBy = "date-created-asc"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Last modified") },
                                onClick = { sortBy = "last-modified"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = { sortBy = "name-asc"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = { sortBy = "name-desc"; showSortMenu = false }
                            )
                        }
                    }
                    androidx.compose.material3.IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                        androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Search collections", tint = Color.White)
                    }
                    androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        
        // Search bar (when active)
        if (showSearch) {
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search collections...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {{
                    androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                }} else null
            )
            Spacer(Modifier.height(8.dp))
        }
        // Grid-like 2-column layout similar to Memory grid
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 4.dp, bottom = 80.dp)) {
                items(filteredCollections.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { c ->
                        var previewImages by remember(c.id) { mutableStateOf<List<String>>(emptyList()) }
                        LaunchedEffect(c.id) {
                            withContext(Dispatchers.IO) {
                                val mems = db(ctx).memoryDao().listMemoriesForCollection(c.id)
                                val images = mems.filter { !it.imageUri.isNullOrBlank() }
                                    .sortedByDescending { it.createdAt }
                                    .take(4)
                                    .mapNotNull { it.imageUri }
                                previewImages = images
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            val isSelected = selectedIds.contains(c.id)
                            Surface(onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds = selectedIds.toMutableSet().apply { if (contains(c.id)) remove(c.id) else add(c.id) }
                                } else onOpenCollection(c.id)
                            }, shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, border = if (isSelected) BorderStroke(3.dp, Color(0xFFFFD54F)) else null) {
                                Box(modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            selectedIds = selectedIds.toMutableSet().apply { if (contains(c.id)) remove(c.id) else add(c.id) }
                                        } else onOpenCollection(c.id)
                                    }, onLongClick = {
                                        selectedIds = selectedIds.toMutableSet().apply { add(c.id) }
                                    })
                                ) {
                                    // Smart preview: single image or 4-image collage
                                    if (previewImages.isNotEmpty() && previewImages.size >= 4) {
                                        // 4-image collage (2x2 grid)
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                coil.compose.AsyncImage(
                                                    model = previewImages[0],
                                                    contentDescription = null,
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                coil.compose.AsyncImage(
                                                    model = previewImages[1],
                                                    contentDescription = null,
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                coil.compose.AsyncImage(
                                                    model = previewImages[2],
                                                    contentDescription = null,
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                coil.compose.AsyncImage(
                                                    model = previewImages[3],
                                                    contentDescription = null,
                                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                    } else if (previewImages.isNotEmpty()) {
                                        // Single latest image
                                        coil.compose.AsyncImage(
                                            model = previewImages.first(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        // No images - gradient background
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val g = Brush.linearGradient(listOf(Color(0xFF3A3A46), Color(0xFF2E3A46), Color(0xFF294238), Color(0xFF513A3A)))
                                            drawRect(brush = g, size = this.size)
                                        }
                                    }
                                    // bottom gradient and title
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x88000000), Color(0xCC000000))))
                                    )
                                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(text = c.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            // overlay top fade
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
        }

        FloatingActionButton(onClick = { showNew = true }, containerColor = Color(0xFF1D1D20), contentColor = Color.White, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.Add, contentDescription = "New collection")
        }

    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            confirmButton = {
                Button(onClick = {
                    val nm = newName.trim()
                    if (nm.isNotEmpty()) {
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { repo.create(nm, newDesc.ifBlank { null }) }
                    }
                    showNew = false
                    newName = ""; newDesc = ""
                }) { Text("Create") }
            },
            dismissButton = {
                Button(onClick = { showNew = false }) { Text("Cancel") }
            },
            title = { Text("New Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") })
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description (optional)") })
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                Button(onClick = {
                    val ids = selectedIds.toList()
                    showDeleteConfirm = false
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        val r = CollectionRepository(db(ctx))
                        ids.forEach { r.delete(it) }
                    }
                    selectedIds = emptySet()
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
            title = { Text("Delete collections?") },
            text = { Text("This will delete ${selectedIds.size} collections.") }
        )
    }
}


