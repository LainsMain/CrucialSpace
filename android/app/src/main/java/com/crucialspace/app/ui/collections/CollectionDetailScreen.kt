package com.crucialspace.app.ui.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crucialspace.app.data.local.CollectionEntity
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.repo.CollectionRepository
import com.crucialspace.app.data.repo.MemoryRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.crucialspace.app.ui.feed.MemoryCard
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

@Composable
fun CollectionDetailScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { CollectionRepository(db(ctx)) }
    val memRepo = remember { MemoryRepository(db(ctx)) }
    var coll by remember { mutableStateOf<CollectionEntity?>(null) }
    var memories by remember { mutableStateOf(emptyList<com.crucialspace.app.data.local.MemoryEntity>()) }

    LaunchedEffect(id) {
        coll = withContext(Dispatchers.IO) { repo.listAll().firstOrNull { it.id == id } }
        memories = withContext(Dispatchers.IO) { repo.listMemories(id) }
    }

    val c = coll
    if (c == null) { Text("Loading...", modifier = Modifier.padding(16.dp)); return }

    var showPicker by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showCollectionMenu by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteCollectionConfirm by remember { mutableStateOf(false) }
    var showEditCollection by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var allMemories by remember { mutableStateOf(emptyList<com.crucialspace.app.data.local.MemoryEntity>()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { androidx.compose.material3.Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text(c.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (selectedIds.isNotEmpty()) {
                IconButton(onClick = { showBulkMenu = true }) { androidx.compose.material3.Icon(Icons.Filled.MoreVert, contentDescription = "Actions") }
                androidx.compose.material3.DropdownMenu(expanded = showBulkMenu, onDismissRequest = { showBulkMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Remove from collection") }, onClick = { showBulkMenu = false; showRemoveConfirm = true })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Delete memories") }, onClick = { showBulkMenu = false; showDeleteConfirm = true }, leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) })
                }
            } else {
                IconButton(onClick = { showCollectionMenu = true }) { androidx.compose.material3.Icon(Icons.Filled.MoreVert, contentDescription = "Collection actions") }
                androidx.compose.material3.DropdownMenu(expanded = showCollectionMenu, onDismissRequest = { showCollectionMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Add memories") }, onClick = {
                        showCollectionMenu = false
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { allMemories = memRepo.listAll() }
                        showPicker = true
                    })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Edit collection") }, onClick = {
                        showCollectionMenu = false
                        editName = c.name
                        editDesc = c.description ?: ""
                        showEditCollection = true
                    })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Delete collection") }, onClick = {
                        showCollectionMenu = false
                        showDeleteCollectionConfirm = true
                    }, leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) })
                }
            }
        }
        if (!c.description.isNullOrBlank()) Text(c.description!!, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        // Simple column grid (2 cols)
        val rows = memories.chunked(2)
        rows.forEach { row ->
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { m ->
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        val isSel = selectedIds.contains(m.id)
                        MemoryCard(item = m, isSelected = isSel, onClick = {
                            if (selectedIds.isNotEmpty()) {
                                selectedIds = selectedIds.toMutableSet().apply { if (contains(m.id)) remove(m.id) else add(m.id) }
                            } else openDetail(ctx, m.id)
                        }, onLongPress = { selectedIds = selectedIds.toMutableSet().apply { add(m.id) } })
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (showPicker) {
        val selected = remember { mutableStateOf(memories.map { it.id }.toSet()) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                Button(onClick = {
                    val chosen = selected.value
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        // add chosen that are not present
                        chosen.forEach { mid -> repo.addMemoryToCollection(id, mid) }
                        // remove those no longer selected
                        val current = repo.listMemories(id).map { it.id }.toSet()
                        current.filter { it !in chosen }.forEach { mid -> repo.removeMemoryFromCollection(id, mid) }
                        val updated = repo.listMemories(id)
                        withContext(Dispatchers.Main) { memories = updated }
                    }
                    showPicker = false
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { showPicker = false }) { Text("Cancel") } },
            title = { Text("Add memories to collection") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allMemories, key = { it.id }) { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = selected.value.contains(m.id), onCheckedChange = { ch ->
                                selected.value = selected.value.toMutableSet().apply { if (ch) add(m.id) else remove(m.id) }
                            })
                            Text(m.aiTitle ?: m.noteText ?: formatCreatedAt(m.createdAt))
                        }
                    }
                }
            }
        )
    }

    if (showEditCollection) {
        AlertDialog(
            onDismissRequest = { showEditCollection = false },
            confirmButton = {
                Button(onClick = {
                    val newName = editName.trim()
                    val newDesc = editDesc.trim().ifBlank { null }
                    showEditCollection = false
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        repo.rename(id, newName, newDesc, colorHex = coll?.colorHex)
                        val updated = repo.listAll().firstOrNull { it.id == id }
                        withContext(Dispatchers.Main) { coll = updated }
                    }
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { showEditCollection = false }) { Text("Cancel") } },
            title = { Text("Edit collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") })
                }
            }
        )
    }

    if (showDeleteCollectionConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteCollectionConfirm = false },
            confirmButton = {
                Button(onClick = {
                    showDeleteCollectionConfirm = false
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        repo.delete(id)
                        withContext(Dispatchers.Main) { onBack() }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { showDeleteCollectionConfirm = false }) { Text("Cancel") } },
            title = { Text("Delete collection?") },
            text = { Text("This will delete the collection and keep its memories.") }
        )
    }

    if (showRemoveConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            confirmButton = {
                Button(onClick = {
                    val ids = selectedIds.toList()
                    showRemoveConfirm = false
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        ids.forEach { mid -> repo.removeMemoryFromCollection(id, mid) }
                        val updated = repo.listMemories(id)
                        withContext(Dispatchers.Main) { memories = updated; selectedIds = emptySet() }
                    }
                }) { Text("Remove") }
            },
            dismissButton = { Button(onClick = { showRemoveConfirm = false }) { Text("Cancel") } },
            title = { Text("Remove from collection?") },
            text = { Text("This will remove ${selectedIds.size} memories from this collection.") }
        )
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                Button(onClick = {
                    val ids = selectedIds.toList()
                    showDeleteConfirm = false
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        val memRepo2 = com.crucialspace.app.data.repo.MemoryRepository(db(ctx))
                        memRepo2.deleteMany(ids, ctx)
                        val updated = repo.listMemories(id)
                        withContext(Dispatchers.Main) { memories = updated; selectedIds = emptySet() }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
            title = { Text("Delete memories?") },
            text = { Text("This will permanently delete ${selectedIds.size} memories.") }
        )
    }
}

private fun formatCreatedAt(epochMillis: Long): String {
    return runCatching {
        val zoned = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
        val d = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        val t = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        "${t.format(zoned)} ${d.format(zoned)}"
    }.getOrElse { "" }
}


