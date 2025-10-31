package com.crucialspace.app.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import android.media.MediaRecorder
import android.media.AudioRecord
import android.media.AudioFormat
import android.os.SystemClock
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
// removed invalid import
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.CameraAlt
import com.crucialspace.app.data.local.MemoryEntity
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.repo.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import com.crucialspace.app.ui.theme.CrucialTheme

class ShareTargetActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val action = intent?.action
		val type = intent?.type
		val sharedImage: Uri? = if (Intent.ACTION_SEND == action && type?.startsWith("image/") == true) {
			intent.getParcelableExtra(Intent.EXTRA_STREAM)
		} else null
		val sharedText: String? = intent.getStringExtra(Intent.EXTRA_TEXT)

        setContent {
            CrucialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var note by remember { mutableStateOf(sharedText.orEmpty()) }
                    var isRecording by remember { mutableStateOf(false) }
                    var audioRecord: AudioRecord? = null
                    var recordingJob: kotlinx.coroutines.Job? = null
                    var recordedFile by remember { mutableStateOf<File?>(null) }
                    val samples = remember { mutableStateListOf<Float>() }
                    var elapsedMs by remember { mutableIntStateOf(0) }
                    val uiScope = rememberCoroutineScope()

                    fun startRecording() {
                        try {
							if (ContextCompat.checkSelfPermission(this@ShareTargetActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this@ShareTargetActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 2001)
                                return
                            }
                            // Clean previous
                            recordedFile?.let { runCatching { it.delete() } }
                            val dir = File(filesDir, "audio").apply { mkdirs() }
                            val outFile = File(dir, "rec-${UUID.randomUUID()}.wav")
                            recordedFile = outFile

                            val sampleRate = 44100
                            val channelConfig = AudioFormat.CHANNEL_IN_MONO
                            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)
                            val rec = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBuf)
                            audioRecord = rec

                            // Start background recording loop
                            isRecording = true
                            samples.clear()
                            elapsedMs = 0
                            recordingJob = uiScope.launch(Dispatchers.IO) {
                                val out = java.io.RandomAccessFile(outFile, "rw")
                                try {
                                    // Placeholder WAV header (44 bytes)
                                    out.setLength(0L)
                                    out.write(ByteArray(44))
                                    val buffer = ByteArray(minBuf)
                                    var totalBytes = 0
                                    var lastUi = SystemClock.uptimeMillis()
                                    rec.startRecording()
                                    val startMs = SystemClock.uptimeMillis()
                                    while (isRecording) {
                                        val read = rec.read(buffer, 0, buffer.size)
                                        if (read > 0) {
                                            out.write(buffer, 0, read)
                                            totalBytes += read
                                            // Compute peak amplitude for this chunk
                                            var peak = 0
                                            var i = 0
                                            while (i + 1 < read) {
                                                val lo = buffer[i].toInt() and 0xFF
                                                val hi = buffer[i + 1].toInt()
                                                val sample = (hi shl 8) or lo
                                                val abs = kotlin.math.abs(sample)
                                                if (abs > peak) peak = abs
                                                i += 2
                                            }
                                            val now = SystemClock.uptimeMillis()
                                            if (now - lastUi >= 50) {
                                                val norm = (peak / 32767f).coerceIn(0f, 1f)
                                                withContext(Dispatchers.Main) {
                                                    if (samples.size > 48) samples.removeAt(0)
                                                    samples.add(norm.coerceAtLeast(0.1f))
                                                    elapsedMs = (now - startMs).toInt()
                                                }
                                                lastUi = now
                                            }
                                        } else {
                                            // small backoff
                                            kotlinx.coroutines.delay(10)
                                        }
                                    }
                                    // Finalize WAV header with totalBytes
                                    writeWavHeader(out, totalBytes, sampleRate, 1, 16)
                                } finally {
                                    try { out.close() } catch (_: Exception) {}
                                }
                            }
                        } catch (_: Exception) { isRecording = false }
                    }

                    fun stopRecording() {
                        isRecording = false
                        try { audioRecord?.stop() } catch (_: Exception) {}
                        try { audioRecord?.release() } catch (_: Exception) {}
                        audioRecord = null
                        // Do not cancel the IO job so it can finalize WAV header safely
                        recordingJob = null
                    }

                    fun trashRecording() {
                        val job = recordingJob
                        val file = recordedFile
                        stopRecording()
                        job?.invokeOnCompletion {
                            runCatching { file?.delete() }
                        }
                        recordedFile = null
                        samples.clear()
                        elapsedMs = 0
                    }

                    var previewImage by remember { mutableStateOf<Uri?>(null) }
                    var capturedImage by remember { mutableStateOf<Uri?>(null) }
                    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

                    fun createImageUri(): Uri? {
                        return try {
                            val dir = File(filesDir, "images").apply { mkdirs() }
                            val file = File(dir, "cap-${UUID.randomUUID()}.jpg")
                            FileProvider.getUriForFile(this@ShareTargetActivity, "${packageName}.fileprovider", file)
                        } catch (_: Throwable) { null }
                    }

                    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                        val uri = pendingCaptureUri
                        if (ok && uri != null) {
                            capturedImage = uri
                            previewImage = uri
                        }
                        pendingCaptureUri = null
                    }

                    // Try to fetch preview image when URL changes and no local/shared image is present
                    var lastFetchedUrl by remember { mutableStateOf("") }
                    val currentUrl = remember(note) { Regex("https?://\\S+").find(note)?.value.orEmpty() }
                    LaunchedEffect(currentUrl, sharedImage, capturedImage) {
                        if (sharedImage == null && capturedImage == null) {
                            if (currentUrl.isBlank()) {
                                previewImage = null
                                lastFetchedUrl = ""
                            } else if (currentUrl != lastFetchedUrl && (previewImage == null || lastFetchedUrl.isBlank())) {
                                try {
                                    val fileUri = withContext(Dispatchers.IO) { fetchOgImageToCache(this@ShareTargetActivity, currentUrl) }
                                    if (fileUri != null) {
                                        previewImage = fileUri
                                        lastFetchedUrl = currentUrl
                                    }
                                } catch (_: Exception) { /* keep previous preview if any */ }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Enrich", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(this@ShareTargetActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(this@ShareTargetActivity, arrayOf(Manifest.permission.CAMERA), 2002)
                                    return@IconButton
                                }
                                val uri = createImageUri()
                                if (uri != null) {
                                    pendingCaptureUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            }) {
                                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Camera")
                            }
                        }
					Spacer(Modifier.height(8.dp))

					// Animated golden outline brush (reused for image border and input bar)
					val anim = rememberInfiniteTransition(label = "gold")
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

                    val imageToShow = sharedImage ?: previewImage
                    if (imageToShow != null) {
                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(this@ShareTargetActivity)
                                .data(imageToShow)
                                .crossfade(true)
                                .build()
                        )
                        val intrinsic = painter.intrinsicSize
                        val aspect = if (intrinsic.isSpecified && intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.height > 0f) (intrinsic.width / intrinsic.height) else 1f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    } else {
                        // No preview image: keep the input bar pinned to bottom
                        Spacer(Modifier.weight(1f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, goldBrush, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF1D1D20))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(onClick = { if (isRecording) trashRecording() else startRecording() }) {
                                androidx.compose.material3.Surface(shape = RoundedCornerShape(50), color = Color(0xFF2A2A2F)) {
                                    Icon(imageVector = if (isRecording) Icons.Filled.Delete else Icons.Filled.Mic, contentDescription = if (isRecording) "Discard recording" else "Record", tint = Color.White, modifier = Modifier.size(36.dp).padding(8.dp))
                                }
                            }
                            if (isRecording) {
								Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
									Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        val barW = (w / (samples.size.coerceAtLeast(1))).coerceAtLeast(3f)
                                        samples.forEachIndexed { i, v ->
                                            val bh = (h * v.coerceAtLeast(0.1f))
                                            drawRect(
                                                color = Color(0xFFFFD54F),
                                                topLeft = androidx.compose.ui.geometry.Offset(i * barW, (h - bh) / 2f),
                                                size = androidx.compose.ui.geometry.Size(barW * 0.6f, bh)
                                            )
                                        }
                                    }
                                    val secs = elapsedMs / 1000
                                    Text(String.format("%d:%02d", secs / 60, secs % 60), color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (note.isBlank()) {
                                        Text("Add a quick note…", color = Color.LightGray)
                                    }
                                    BasicTextField(
                                        value = note,
                                        onValueChange = { note = it },
                                        singleLine = false,
                                        minLines = 1,
                                        maxLines = 6,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 20.dp, max = 160.dp)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                if (isRecording) stopRecording()
                                // Save to DB and enqueue worker
                                CoroutineScope(Dispatchers.IO).launch {
                                    // If no image, try to grab preview from first URL
                                    var localImageUri: Uri? = sharedImage?.let { copyToInternal(it) } ?: capturedImage
                                    if (localImageUri == null) {
                                        val url = Regex("https?://\\S+").find(note)?.value
                                        if (!url.isNullOrBlank()) {
                                            localImageUri = fetchOgImageToFiles(this@ShareTargetActivity, url)
                                        }
                                    }
                                    val appDb = db(applicationContext)
                                    val repo = MemoryRepository(appDb)
                                    val entity = MemoryEntity(
                                        imageUri = localImageUri?.toString(),
                                        noteText = note.ifBlank { null },
                                        audioUri = recordedFile?.let { Uri.fromFile(it).toString() },
                                    )
                                    repo.saveAndQueue(entity, applicationContext)
                                }
                                finish()
                            }) {
                                androidx.compose.material3.Surface(shape = RoundedCornerShape(50), color = Color(0xFF2A2A2F)) {
                                    Icon(imageVector = Icons.Filled.Send, contentDescription = "Send", tint = Color(0xFFFFD54F), modifier = Modifier.size(36.dp).padding(8.dp))
                                }
                            }
                        }
                    }
                }
			}
		}
	}
}

private fun writeWavHeader(out: java.io.RandomAccessFile, totalAudioBytes: Int, sampleRate: Int, channels: Int, bitsPerSample: Int) {
    // RIFF header with PCM format
    fun writeIntLE(v: Int) { out.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())) }
    fun writeShortLE(v: Int) { out.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())) }

    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val subchunk2Size = totalAudioBytes
    val chunkSize = 36 + subchunk2Size

    out.seek(0)
    out.write("RIFF".toByteArray())
    writeIntLE(chunkSize)
    out.write("WAVE".toByteArray())
    out.write("fmt ".toByteArray())
    writeIntLE(16) // Subchunk1Size for PCM
    writeShortLE(1) // AudioFormat = 1 (PCM)
    writeShortLE(channels)
    writeIntLE(sampleRate)
    writeIntLE(byteRate)
    writeShortLE(blockAlign)
    writeShortLE(bitsPerSample)
    out.write("data".toByteArray())
    writeIntLE(subchunk2Size)
}

private fun ShareTargetActivity.copyToInternal(uri: Uri): Uri? {
    return try {
        val dir = File(filesDir, "images").apply { mkdirs() }
        val ext = when (contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            else -> ".bin"
        }
        val file = File(dir, UUID.randomUUID().toString() + ext)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { out ->
                input.copyTo(out)
            }
        }
        Uri.fromFile(file)
    } catch (t: Throwable) {
        null
    }
}

// Robustly fetch and store the best preview image for a given page URL into cacheDir.
private fun fetchOgImageToCache(context: android.content.Context, pageUrl: String): Uri? {
    return try {
        val client = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .readTimeout(java.time.Duration.ofSeconds(20))
            .build()

        val req = okhttp3.Request.Builder()
            .url(pageUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.8")
            .build()
        client.newCall(req).execute().use { resp ->
            val html = resp.body?.string() ?: return null
            val imgUrl = findBestImageUrl(html, pageUrl) ?: return null
            val imgReq = okhttp3.Request.Builder()
                .url(imgUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
                .build()
            client.newCall(imgReq).execute().use { imgResp ->
                val bytes = imgResp.body?.bytes() ?: return null
                val dir = File(context.cacheDir, "images").apply { mkdirs() }
                val ext = guessImageExt(imgResp.header("Content-Type"))
                val file = File(dir, "url-prev-${UUID.randomUUID()}$ext")
                FileOutputStream(file).use { it.write(bytes) }
                return Uri.fromFile(file)
            }
        }
    } catch (_: Exception) {
        null
    }
}

// Same as above but stores in filesDir for persistence until upload.
private fun fetchOgImageToFiles(activity: ShareTargetActivity, pageUrl: String): Uri? {
    return try {
        val client = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .readTimeout(java.time.Duration.ofSeconds(20))
            .build()

        val req = okhttp3.Request.Builder()
            .url(pageUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.8")
            .build()
        client.newCall(req).execute().use { resp ->
            val html = resp.body?.string() ?: return null
            val imgUrl = findBestImageUrl(html, pageUrl) ?: return null
            val imgReq = okhttp3.Request.Builder()
                .url(imgUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
                .build()
            client.newCall(imgReq).execute().use { imgResp ->
                val bytes = imgResp.body?.bytes() ?: return null
                val dir = File(activity.filesDir, "images").apply { mkdirs() }
                val ext = guessImageExt(imgResp.header("Content-Type"))
                val file = File(dir, "url-${UUID.randomUUID()}$ext")
                FileOutputStream(file).use { it.write(bytes) }
                return Uri.fromFile(file)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun guessImageExt(contentType: String?): String {
    val ct = contentType?.lowercase() ?: return ".jpg"
    return when {
        ct.contains("png") -> ".png"
        ct.contains("webp") -> ".webp"
        ct.contains("gif") -> ".gif"
        else -> ".jpg"
    }
}

// Parse HTML for common image hints and resolve against the page URL.
private fun findBestImageUrl(html: String, pageUrl: String): String? {
    fun resolve(href: String): String {
        val trimmed = href.trim()
        if (trimmed.startsWith("//")) {
            val scheme = try { java.net.URI(pageUrl).scheme ?: "https" } catch (_: Exception) { "https" }
            return "$scheme:$trimmed"
        }
        return try { java.net.URI(pageUrl).resolve(trimmed).toString() } catch (_: Exception) { trimmed }
    }

    val patterns = listOf(
        // og:image:secure_url
        "<meta[^>]*property=['\"]og:image:secure_url['\"][^>]*content=['\"]([^'\"]+)['\"][^>]*>" to 1,
        "<meta[^>]*content=['\"]([^'\"]+)['\"][^>]*property=['\"]og:image:secure_url['\"][^>]*>" to 1,
        // og:image:url
        "<meta[^>]*property=['\"]og:image:url['\"][^>]*content=['\"]([^'\"]+)['\"][^>]*>" to 1,
        "<meta[^>]*content=['\"]([^'\"]+)['\"][^>]*property=['\"]og:image:url['\"][^>]*>" to 1,
        // og:image
        "<meta[^>]*property=['\"]og:image['\"][^>]*content=['\"]([^'\"]+)['\"][^>]*>" to 1,
        "<meta[^>]*content=['\"]([^'\"]+)['\"][^>]*property=['\"]og:image['\"][^>]*>" to 1,
        // twitter:image
        "<meta[^>]*name=['\"]twitter:image['\"][^>]*content=['\"]([^'\"]+)['\"][^>]*>" to 1,
        "<meta[^>]*name=['\"]twitter:image:src['\"][^>]*content=['\"]([^'\"]+)['\"][^>]*>" to 1,
        // link rel=image_src
        "<link[^>]*rel=['\"]image_src['\"][^>]*href=['\"]([^'\"]+)['\"][^>]*>" to 1,
        "<link[^>]*href=['\"]([^'\"]+)['\"][^>]*rel=['\"]image_src['\"][^>]*>" to 1
    )
    for ((p, idx) in patterns) {
        val r = Regex(p, RegexOption.IGNORE_CASE)
        val m = r.find(html)
        if (m != null) {
            val href = m.groupValues.getOrNull(idx)
            if (!href.isNullOrBlank()) return resolve(href)
        }
    }

    // Amazon-specific fallbacks
    val hostLower = try { java.net.URI(pageUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }
    if (hostLower.contains("amazon.") || hostLower.contains("amzn.")) {
        // 1) landingImage with data-old-hires
        run {
            val r = Regex("<img[^>]*id=['\"]landingImage['\"][^>]*data-old-hires=['\"]([^'\"]+)['\"][^>]*>", RegexOption.IGNORE_CASE)
            val m = r.find(html)
            val href = m?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) return resolve(href)
        }
        // 2) data-a-dynamic-image JSON containing URL keys
        run {
            val r = Regex("data-a-dynamic-image=['\"](\\{.*?\\})['\"]", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val m = r.find(html)
            val jsonStr = m?.groupValues?.getOrNull(1)?.replace("&quot;", "\"")
            if (!jsonStr.isNullOrBlank()) {
                try {
                    val obj = org.json.JSONObject(jsonStr)
                    val it = obj.keys()
                    if (it.hasNext()) return resolve(it.next())
                } catch (_: Exception) {}
            }
        }
        // 3) JSON hints (hiRes / large / mainUrl)
        run {
            val r = Regex("\\\"hiRes\\\"\\s*:\\s*\\\"(https?:[^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
            val m = r.find(html)
            val href = m?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) return resolve(href)
        }
        run {
            val r = Regex("\\\"large\\\"\\s*:\\s*\\\"(https?:[^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
            val m = r.find(html)
            val href = m?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) return resolve(href)
        }
        run {
            val r = Regex("\\\"mainUrl\\\"\\s*:\\s*\\\"(https?:[^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
            val m = r.find(html)
            val href = m?.groupValues?.getOrNull(1)
            if (!href.isNullOrBlank()) return resolve(href)
        }
    }
    // Generic fallbacks: srcset (pick largest), or first <img src>
    run {
        val r = Regex("<img[^>]*srcset=['\"]([^'\"]+)['\"][^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val m = r.find(html)
        val srcset = m?.groupValues?.getOrNull(1)
        if (!srcset.isNullOrBlank()) {
            val best = srcset.split(',').map { it.trim() }.maxByOrNull { seg ->
                val w = Regex("(\\d+)w").find(seg)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                w
            }
            val url = best?.split(' ')?.firstOrNull()
            if (!url.isNullOrBlank()) return resolve(url)
        }
    }
    run {
        val r = Regex("<img[^>]*src=['\"]([^'\"]+)['\"][^>]*>", RegexOption.IGNORE_CASE)
        val m = r.find(html)
        val href = m?.groupValues?.getOrNull(1)
        if (!href.isNullOrBlank()) return resolve(href)
    }
    return null
}
