package com.crucialspace.app.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.crucialspace.app.data.local.MemoryEntity
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.remote.AiReminder
import com.crucialspace.app.data.repo.MemoryRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.OffsetDateTime
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(id: String, onClose: () -> Unit) {
	val context = LocalContext.current
	val state = remember { mutableStateOf<MemoryEntity?>(null) }
	LaunchedEffect(id) {
		state.value = withContext(Dispatchers.IO) {
			MemoryRepository(db(context)).getById(id)
		}
	}
	val item = state.value
	if (item == null) {
		Text("Loading...", modifier = Modifier.padding(16.dp))
		return
	}
	val moshi = remember { Moshi.Builder().build() }
	val todosType = remember { Types.newParameterizedType(List::class.java, String::class.java) }
	val remindersType = remember { Types.newParameterizedType(List::class.java, AiReminder::class.java) }
    val todos = remember(item.aiTodosJson) { item.aiTodosJson?.let { moshi.adapter<List<String>>(todosType).fromJson(it) } ?: emptyList() }
    val urls = remember(item.aiUrlsJson) { item.aiUrlsJson?.let { moshi.adapter<List<String>>(todosType).fromJson(it) } ?: emptyList() }
    var doneSet by remember(item.aiTodosDoneJson) {
        mutableStateOf(item.aiTodosDoneJson?.let { moshi.adapter<Set<String>>(Types.newParameterizedType(Set::class.java, String::class.java)).fromJson(it) } ?: emptySet())
    }
    var reminders by remember(item.aiRemindersJson) {
        mutableStateOf(item.aiRemindersJson?.let { moshi.adapter<List<AiReminder>>(remindersType).fromJson(it) } ?: emptyList())
    }
    var showImage by remember { mutableStateOf(false) }
    var showReminderOptions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var editingText by remember { mutableStateOf("") }
    var editingDateTime by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showCollectionsPicker by remember { mutableStateOf(false) }
    // shared editing flags to allow outside taps to dismiss editing
    var editingSummary by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    var editingTodos by remember { mutableStateOf(false) }
    val rootFocusManager = LocalFocusManager.current
    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(editingSummary, editingNote, editingTodos) {
            if (editingSummary || editingNote || editingTodos) {
                detectTapGestures(onTap = {
                    rootFocusManager.clearFocus()
                    editingSummary = false
                    editingNote = false
                    editingTodos = false
                })
            }
        }
    ) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top app bar with back, centered-ish smaller title, and overflow menu
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text(text = item.aiTitle ?: item.noteText ?: "", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { showOverflow = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                    DropdownMenuItem(text = { Text("Add to collections") }, onClick = {
                        showOverflow = false
                        // open picker dialog
                        showCollectionsPicker = true
                    })
                    DropdownMenuItem(text = { Text("Delete memory") }, onClick = { showOverflow = false; confirmDelete = true }, leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (!item.imageUri.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showImage = true }
            )
        } else {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF434159),
                        Color(0xFF2F3D4F),
                        Color(0xFF2A3A35),
                        Color(0xFF4E3B3B)
                    )
                )
                drawRect(brush = gradient, size = this.size)
            }
        }
        // Created-at timestamp under image
        Text(
            text = formatCreatedAt(item.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        // Collections chips (basic read-only)
        run {
            val dao = db(context).memoryDao()
            var chips by remember { mutableStateOf(emptyList<com.crucialspace.app.data.local.CollectionEntity>()) }
            LaunchedEffect(item.id) {
                chips = withContext(Dispatchers.IO) { dao.listCollectionsForMemory(item.id) }
            }
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    chips.forEach { c ->
                        androidx.compose.material3.Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
                            Text(c.name, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
        // Fullscreen image viewer with zoom/pan and scrim, dismiss on back or outside tap
        BackHandler(showImage) { if (showImage) showImage = false }
        if (showImage && !item.imageUri.isNullOrBlank()) {
            Dialog(onDismissRequest = { showImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                // Gesture state
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                    val factor = newScale / scale
                    scale = newScale
                    offsetX = (offsetX + panChange.x).coerceIn(-2000f, 2000f)
                    offsetY = (offsetY + panChange.y).coerceIn(-2000f, 2000f)
                }
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color(0xCC000000))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showImage = false }, onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f } else { scale = 2f }
                        })
                    }
                ) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .transformable(transformState)
                    )
                }
            }
        }
        // Title over image
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) { /* already displayed above */ }
        // Editable AI summary (tap to edit inline)
        run {
            val isEditing = editingSummary
            var text by remember(item.aiSummary) { mutableStateOf(item.aiSummary ?: "") }
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            var hadFocus by remember { mutableStateOf(false) }
            if (!item.aiSummary.isNullOrBlank() || isEditing) {
                androidx.compose.material3.Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D20))) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .let { m -> if (!isEditing) m.clickable { editingSummary = true } else m },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isEditing) {
                            BackHandler(enabled = true) {
                                focusManager.clearFocus()
                                editingSummary = false
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { f ->
                                        if (f.isFocused) hadFocus = true
                                        if (!f.isFocused && hadFocus) {
                                            // persist and exit edit mode
                                            val newVal = text.trim().ifBlank { null }
                                            if (newVal != item.aiSummary) {
                                                val dao = db(context).memoryDao()
                                                scope.launch { withContext(Dispatchers.IO) { dao.updateAiSummary(item.id, newVal) }; state.value = state.value?.copy(aiSummary = newVal) }
                                            }
                                            editingSummary = false
                                            hadFocus = false
                                        }
                                    },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                            LaunchedEffect(isEditing) { if (isEditing) focusRequester.requestFocus() }
                        } else {
                            MarkdownText(item.aiSummary ?: "")
                        }
                    }
                }
            }
        }
        // THEN: Voice note or plain text note under the AI text
        if (!item.audioUri.isNullOrBlank()) {
            AudioSection(audioUri = item.audioUri!!, transcript = item.noteText)
        } else {
            // Editable Note
            run {
                val isEditing = editingNote
                var note by remember(item.noteText) { mutableStateOf(item.noteText ?: "") }
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                var hadFocus by remember { mutableStateOf(false) }
                SectionCard(title = "Note") {
                    if (isEditing || !item.noteText.isNullOrBlank()) {
                        if (isEditing) {
                            BackHandler(enabled = true) {
                                focusManager.clearFocus()
                                editingNote = false
                            }
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { f ->
                                        if (f.isFocused) hadFocus = true
                                        if (!f.isFocused && hadFocus) {
                                            val newVal = note.trim().ifBlank { null }
                                            if (newVal != item.noteText) {
                                                val dao = db(context).memoryDao()
                                                scope.launch { withContext(Dispatchers.IO) { dao.updateNoteText(item.id, newVal) }; state.value = state.value?.copy(noteText = newVal) }
                                            }
                                            editingNote = false
                                            hadFocus = false
                                        }
                                    },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                            LaunchedEffect(isEditing) { if (isEditing) focusRequester.requestFocus() }
                        } else {
                            Text(item.noteText ?: "", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clickable { editingNote = true })
                        }
                    } else {
                        Text("", modifier = Modifier.clickable { editingNote = true })
                    }
                }
            }
        }
        if (showCollectionsPicker) {
            val dao = db(context).memoryDao()
            var all by remember { mutableStateOf(emptyList<com.crucialspace.app.data.local.CollectionEntity>()) }
            var selected by remember { mutableStateOf(emptySet<String>()) }
            LaunchedEffect(item.id) {
                withContext(Dispatchers.IO) {
                    all = dao.listCollections()
                    val existing = dao.listCollectionsForMemory(item.id)
                    selected = existing.map { it.id }.toSet()
                }
            }
            AlertDialog(
                onDismissRequest = { showCollectionsPicker = false },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = {
                        // apply selections
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            val repo = com.crucialspace.app.data.repo.CollectionRepository(db(context))
                            // add missing
                            selected.forEach { cid -> repo.addMemoryToCollection(cid, item.id) }
                            // remove deselected
                            val existing = dao.listCollectionsForMemory(item.id).map { it.id }.toSet()
                            existing.filter { it !in selected }.forEach { cid -> repo.removeMemoryFromCollection(cid, item.id) }
                        }
                        showCollectionsPicker = false
                    }) { Text("Save") }
                },
                dismissButton = { androidx.compose.material3.Button(onClick = { showCollectionsPicker = false }) { Text("Cancel") } },
                title = { Text("Add to collections") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(all, key = { it.id }) { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = selected.contains(c.id), onCheckedChange = { ch ->
                                    selected = selected.toMutableSet().apply { if (ch) add(c.id) else remove(c.id) }
                                })
                                Text(c.name)
                            }
                        }
                    }
                }
            )
        }
        run {
            val isEditing = editingTodos
            val adapter = remember { moshi.adapter<List<String>>(todosType) }
            var text by remember(item.aiTodosJson, isEditing) {
                mutableStateOf(
                    if (!isEditing) "" else (todos.joinToString("\n"))
                )
            }
            SectionCard(title = "To-Dos") {
                if (isEditing) {
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current
                    var hadFocus by remember { mutableStateOf(false) }
                    BackHandler(enabled = true) {
                        focusManager.clearFocus()
                        editingTodos = false
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { f ->
                                if (f.isFocused) hadFocus = true
                                if (!f.isFocused && hadFocus) {
                                    val newList = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                                    val newJson = adapter.toJson(newList)
                                    val newDone = doneSet.intersect(newList.toSet())
                                    val doneJson = moshi.adapter<Set<String>>(Types.newParameterizedType(Set::class.java, String::class.java)).toJson(newDone)
                                    val dao = db(context).memoryDao()
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            dao.updateTodosJson(item.id, newJson)
                                            dao.updateTodosDone(item.id, doneJson)
                                        }
                                        state.value = state.value?.copy(aiTodosJson = newJson, aiTodosDoneJson = doneJson)
                                    }
                                    editingTodos = false
                                    hadFocus = false
                                }
                            },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    LaunchedEffect(isEditing) { if (isEditing) focusRequester.requestFocus() }
                } else if (todos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable { text = todos.joinToString("\n"); editingTodos = true }) {
                        todos.forEach { t ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val checked = doneSet.contains(t)
                                Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                    val new = if (isChecked) doneSet + t else doneSet - t
                                    doneSet = new
                                    val json = moshi.adapter<Set<String>>(Types.newParameterizedType(Set::class.java, String::class.java)).toJson(new)
                                    val dao = db(context).memoryDao()
                                    scope.launch { withContext(Dispatchers.IO) { dao.updateTodosDone(item.id, json) } }
                                })
                                Text(t, color = if (checked) Color.LightGray else MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }
        if (reminders.isNotEmpty()) {
            SectionCard(title = "Reminders") {
                reminders.forEachIndexed { idx, r ->
                    ReminderRow(
                        text = r.event,
                        time = r.datetime,
                        onLongPress = { editingIndex = idx; showReminderOptions = true; editingText = r.event; editingDateTime = r.datetime }
                    )
                }
            }
        }
        if (urls.isNotEmpty()) {
            SectionCard(title = "Links") {
                urls.forEach { u ->
                    Text(u, color = Color(0xFF5ED1C1), modifier = Modifier.clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(u))
                        context.startActivity(intent)
                    })
                }
            }
        }
        if (showReminderOptions && editingIndex >= 0) {
            AlertDialog(
                onDismissRequest = { showReminderOptions = false },
                confirmButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showReminderOptions = false; showEditDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color.White),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color.White
                        )
                    ) { Text("Edit") }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showReminderOptions = false
                            scope.launch {
                                val new = reminders.toMutableList().also { it.removeAt(editingIndex) }
                                val newJson = moshi.adapter<List<AiReminder>>(remindersType).toJson(new)
                                withContext(Dispatchers.IO) { db(context).memoryDao().updateReminders(item.id, newJson) }
                                com.crucialspace.app.work.ReminderWorker.cancel(context, "reminder-${item.id}-${editingIndex}")
                                reminders = new
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFFFF4D4D)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color(0xFFFF4D4D)
                        )
                    ) { Text("Delete") }
                },
                title = { Text("Reminder") },
                text = { Text("Edit or delete this reminder?") }
            )
        }
        if (showEditDialog && editingIndex >= 0) {
            // Initialize pickers based on existing datetime (UTC) or now
            val init = runCatching { OffsetDateTime.parse(editingDateTime) }.getOrNull()
            var localDate = remember(editingDateTime) { (init?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDate()) ?: LocalDate.now() }
            var localTime = remember(editingDateTime) { (init?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalTime()?.withSecond(0)?.withNano(0)) ?: LocalTime.now().withSecond(0).withNano(0) }

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                confirmButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                        val zoned = localDate.atTime(localTime).atZone(ZoneId.systemDefault())
                        val utcIso = zoned.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString()
                        val new = reminders.toMutableList().also { it[editingIndex] = AiReminder(editingText, utcIso) }
                        val newJson = moshi.adapter<List<AiReminder>>(remindersType).toJson(new)
                        scope.launch {
                            withContext(Dispatchers.IO) { db(context).memoryDao().updateReminders(item.id, newJson) }
                            com.crucialspace.app.work.ReminderWorker.cancel(context, "reminder-${item.id}-${editingIndex}")
                            com.crucialspace.app.work.ReminderWorker.schedule(
                                context,
                                uniqueName = "reminder-${item.id}-${editingIndex}",
                                whenIso = utcIso,
                                title = editingText,
                                text = item.aiTitle ?: item.noteText ?: "",
                                memoryId = item.id,
                                reminderIndex = editingIndex
                            )
                            reminders = new
                            showEditDialog = false
                        }
                    },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color.White),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color.White
                        )
                    ) { Text("Save") }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showEditDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color.White),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color.White
                        )
                    ) { Text("Cancel") }
                },
                title = { Text("Edit Reminder") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editingText, onValueChange = { editingText = it }, label = { Text("Title") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.OutlinedButton(onClick = {
                                DatePickerDialog(context, { _, y, m, d ->
                                    localDate = LocalDate.of(y, m + 1, d)
                                }, localDate.year, localDate.monthValue - 1, localDate.dayOfMonth).show()
                            },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, Color.White),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1D1D20),
                                    contentColor = Color.White
                                )
                            ) { Text(localDate.toString()) }
                            androidx.compose.material3.OutlinedButton(onClick = {
                                TimePickerDialog(context, { _, h, min ->
                                    localTime = LocalTime.of(h, min)
                                }, localTime.hour, localTime.minute, true).show()
                            },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, Color.White),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1D1D20),
                                    contentColor = Color.White
                                )
                            ) { Text(localTime.toString()) }
                        }
                    }
                }
            )
        }
		Spacer(Modifier.height(8.dp))
    val repo = remember { MemoryRepository(db(context)) }
    if (item.status == "ERROR") {
        Button(onClick = {
            scope.launch {
                withContext(Dispatchers.IO) { repo.retry(item.id, context) }
            }
        }) { Text("Retry") }
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            // Center both buttons in one row
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            confirmDelete = false
                            scope.launch {
                                withContext(Dispatchers.IO) { repo.delete(item.id, context) }
                                onClose()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFFFF4D4D)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color(0xFFFF4D4D)
                        )
                    ) { Text("Delete") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirmDelete = false },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color.White),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1D1D20),
                            contentColor = Color.White
                        )
                    ) { Text("Cancel") }
                }
            },
            dismissButton = {},
            title = { Text("Delete memory?") },
            text = { Text("This action cannot be undone.") }
        )
    }
    // Bottom trash removed; actions moved to top-right menu

        // moved voice note above
	}
    // overlay removed
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D20))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AudioSection(audioUri: String, transcript: String?) {
    var isPlaying by remember { mutableStateOf(false) }
    var showTranscript by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val player = remember(audioUri) { MediaPlayer.create(context, Uri.parse(audioUri)) }
    val visualizer = remember(player) {
        try {
            val sessionId = player?.audioSessionId ?: 0
            if (sessionId != 0) Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                enabled = false
            } else null
        } catch (_: Throwable) { null }
    }
    var positionMs by remember { mutableStateOf(0) }
    val durationMs = remember(player) { player?.duration ?: 0 }
    val samples = remember { androidx.compose.runtime.mutableStateListOf<Float>() }
    androidx.compose.runtime.DisposableEffect(player) {
        onDispose {
            try { visualizer?.release() } catch (_: Exception) {}
            try { player?.release() } catch (_: Exception) {}
        }
    }
    SectionCard(title = "Voice Note") {
        Row(modifier = Modifier.fillMaxWidth().clickable { showTranscript = !showTranscript }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = {
                if (player != null) {
                    if (player.isPlaying) {
                        player.pause(); isPlaying = false
                        try { visualizer?.enabled = false } catch (_: Exception) {}
                    } else {
                        player.start(); isPlaying = true
                        try { visualizer?.enabled = true } catch (_: Exception) {}
                    }
                }
            }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                if (isPlaying) {
                    // waveform from actual audio via Visualizer; fallback to timer if unavailable
                    LaunchedEffect(player, isPlaying) {
                        val viz = visualizer
                        if (viz != null) {
                            val buf = ByteArray(viz.captureSize)
                            try {
                                while (isPlaying && player != null && player.isPlaying) {
                                    positionMs = player.currentPosition
                                    val ok = runCatching { viz.getWaveForm(buf) }.getOrDefault(Visualizer.ERROR)
                                    if (ok == Visualizer.SUCCESS) {
                                        val bars = 48
                                        val step = kotlin.math.max(1, buf.size / bars)
                                        val newSamples = ArrayList<Float>(bars)
                                        var i = 0
                                        while (i < buf.size && newSamples.size < bars) {
                                            var maxV = 0f
                                            var k = 0
                                            while (k < step && (i + k) < buf.size) {
                                                val v = ((buf[i + k].toInt() and 0xFF) / 255f)
                                                if (v > maxV) maxV = v
                                                k++
                                            }
                                            newSamples.add(maxV.coerceIn(0.1f, 1f))
                                            i += step
                                        }
                                        samples.clear()
                                        samples.addAll(newSamples)
                                    }
                                    kotlinx.coroutines.delay(50)
                                }
                            } finally {
                                runCatching { viz.enabled = false }
                            }
                        } else {
                            while (isPlaying && player != null && player.isPlaying) {
                                positionMs = player.currentPosition
                                kotlinx.coroutines.delay(100)
                            }
                        }
                    }
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        val sz = this.size
                        val w = sz.width
                        val h = sz.height
                        val barW = (w / (samples.size.coerceAtLeast(1))).coerceAtLeast(3f)
                        samples.forEachIndexed { i, v ->
                            val bh = (h * (0.1f + 0.9f * v))
                            drawRect(
                                color = Color(0xFFFFD54F),
                                topLeft = androidx.compose.ui.geometry.Offset(i * barW, (h - bh) / 2f),
                                size = androidx.compose.ui.geometry.Size(barW * 0.6f, bh)
                            )
                        }
                    }
                }
                val cur = positionMs / 1000
                val tot = durationMs / 1000
                Text(String.format("%d:%02d / %d:%02d", cur / 60, cur % 60, tot / 60, tot % 60))
            }
        }
        if (showTranscript && !transcript.isNullOrBlank()) {
            Text(transcript)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(text: String, time: String?, onLongPress: () -> Unit) {
    androidx.compose.material3.Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Surface(shape = RoundedCornerShape(50), border = BorderStroke(2.dp, Color.Gray)) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.size(16.dp)) {}
            }
            Text(text = formatShortTime(time), style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            Text(text = text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, modifier = Modifier.weight(1f))
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

private fun formatCreatedAt(epochMillis: Long): String {
    return runCatching {
        val zoned = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
        val d = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        val t = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        "${t.format(zoned)} ${d.format(zoned)}"
    }.getOrElse { "" }
}

@Composable
private fun MarkdownText(text: String) {
    val lines = remember(text) { text.split('\n') }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.startsWith("### ") -> Text(
                    buildBoldInline(line.removePrefix("### ")), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold
                )
                line.startsWith("## ") -> Text(
                    buildBoldInline(line.removePrefix("## ")), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold
                )
                line.startsWith("# ") -> Text(
                    buildBoldInline(line.removePrefix("# ")), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold
                )
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = Color.White)
                        Text(buildBoldInline(line.drop(2)), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(buildBoldInline(line), style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun buildBoldInline(text: String): AnnotatedString {
    // very small **bold** parser
    return remember(text) {
        val builder = buildAnnotatedString {
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
        builder
    }
}
