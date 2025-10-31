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
            // Keep Search and Gear
            androidx.compose.material3.Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = onOpenSearch) {
                        androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
                    }
                    androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // Grid-like 2-column layout similar to Memory grid
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                items(collections.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { c ->
                        var thumb by remember(c.id) { mutableStateOf<String?>(null) }
                        LaunchedEffect(c.id) {
                            withContext(Dispatchers.IO) {
                                val mems = db(ctx).memoryDao().listMemoriesForCollection(c.id)
                                val img = mems.firstOrNull { !it.imageUri.isNullOrBlank() }?.imageUri
                                thumb = img
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
                                    if (!thumb.isNullOrBlank()) {
                                        coil.compose.AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    } else {
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


