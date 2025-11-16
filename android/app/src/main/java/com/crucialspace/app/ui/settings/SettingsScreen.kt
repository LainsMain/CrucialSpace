package com.crucialspace.app.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crucialspace.app.settings.SettingsStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.crucialspace.app.permissions.requestIgnoreBatteryOptimizations
import com.crucialspace.app.permissions.requestPostNotificationsIfNeeded
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.pm.ShortcutInfoCompat
import android.content.Intent
import com.crucialspace.app.share.ShareTargetActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.crucialspace.app.update.UpdateChecker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun SettingsScreen(onSaved: () -> Unit, onBack: () -> Unit = {}) {
	val context = LocalContext.current
	val store = remember { SettingsStore(context) }
	val base = remember { mutableStateOf(store.getBaseUrl()) }
	val secret = remember { mutableStateOf(store.getSharedSecret().orEmpty()) }
	val localAi = remember { mutableStateOf(store.isLocalAiEnabled()) }
	val geminiKey = remember { mutableStateOf(store.getGeminiApiKey().orEmpty()) }
	val languagePref = remember { mutableStateOf(store.getLanguagePreference()) }
	val showLangMenu = remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()
	val checkingUpdate = remember { mutableStateOf(false) }
	val updateAvailable = remember { mutableStateOf<com.crucialspace.app.update.GithubRelease?>(null) }
	val showUpdateDialog = remember { mutableStateOf(false) }
	
	val languageOptions = mapOf(
		"auto" to "Auto-detect",
		"en" to "English",
		"es" to "Spanish",
		"fr" to "French",
		"de" to "German",
		"it" to "Italian",
		"pt" to "Portuguese",
		"nl" to "Dutch",
		"pl" to "Polish",
		"ru" to "Russian",
		"ja" to "Japanese",
		"zh" to "Chinese",
		"ko" to "Korean"
	)
    val activity = LocalContext.current as android.app.Activity
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.padding(16.dp).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { androidx.compose.material3.Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        val anim = rememberInfiniteTransition(label = "gold")
        val shift = anim.animateFloat(initialValue = 0f, targetValue = 600f, animationSpec = infiniteRepeatable(animation = tween(2500), repeatMode = RepeatMode.Restart), label = "goldAnim")
        val goldBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFE082), Color(0xFFFFD54F), Color(0xFFFFF59D)),
            start = androidx.compose.ui.geometry.Offset(shift.value, 0f),
            end = androidx.compose.ui.geometry.Offset(shift.value + 300f, 200f)
        )

        // No model selector; always uses gemini-2.5-flash

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(8.dp)) {
            OutlinedTextField(
                value = base.value,
                onValueChange = { base.value = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF1D1D20),
                    unfocusedContainerColor = Color(0xFF1D1D20)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = secret.value,
                onValueChange = { secret.value = it },
                label = { Text("Shared Secret") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF1D1D20),
                    unfocusedContainerColor = Color(0xFF1D1D20)
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Base URL and Shared Secret are only needed when using a backend. If you use Gemini directly, you can leave these blank.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0)
            )
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Use Gemini directly (no backend)")
                androidx.compose.material3.Switch(checked = localAi.value, onCheckedChange = { localAi.value = it })
            }
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(8.dp)) {
            OutlinedTextField(
                value = geminiKey.value,
                onValueChange = { geminiKey.value = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF1D1D20),
                    unfocusedContainerColor = Color(0xFF1D1D20)
                )
            )
            Spacer(Modifier.height(4.dp))
            Text("Model: gemini-2.5-flash", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B0))
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(12.dp)) {
            Text("AI Response Language", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { showLangMenu.value = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(languageOptions[languagePref.value] ?: "Auto-detect")
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showLangMenu.value,
                    onDismissRequest = { showLangMenu.value = false }
                ) {
                    languageOptions.forEach { (code, name) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                languagePref.value = code
                                showLangMenu.value = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "AI will generate summaries, titles, todos, and reminders in the selected language. Auto-detect will use the language from your input.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0B0)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            OutlinedButton(
                onClick = {
                    store.setBaseUrl(base.value)
                    store.setSharedSecret(secret.value)
                    store.setLocalAiEnabled(localAi.value)
                    store.setGeminiApiKey(geminiKey.value)
                    store.setLanguagePreference(languagePref.value)
                    onSaved()
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1D1D20),
                    contentColor = Color.White
                )
            ) { Text("Save") }

            OutlinedButton(
                onClick = { requestPostNotificationsIfNeeded(activity, notifLauncher) },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1D1D20),
                    contentColor = Color.White
                )
            ) { Text("Enable notifications") }
        }

        OutlinedButton(
            onClick = { requestIgnoreBatteryOptimizations(activity) },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF1D1D20),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Allow background (ignore battery optimization)") }

        // Add pinned shortcut for Enrich
        OutlinedButton(
            onClick = {
                val intent = Intent(activity, ShareTargetActivity::class.java).setAction(Intent.ACTION_VIEW)
                val shortcut = ShortcutInfoCompat.Builder(activity, "enrich-shortcut")
                    .setShortLabel("Enrich")
                    .setLongLabel("Open Enrich")
                    .setIcon(IconCompat.createWithResource(activity, com.crucialspace.app.R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build()
                ShortcutManagerCompat.requestPinShortcut(activity, shortcut, null)
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF1D1D20),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add Enrich shortcut to Home") }
        
        // Check for updates
        OutlinedButton(
            onClick = {
                checkingUpdate.value = true
                scope.launch {
                    try {
                        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        val release = UpdateChecker.checkForUpdate(currentVersion)
                        if (release != null) {
                            updateAvailable.value = release
                            showUpdateDialog.value = true
                        } else {
                            android.widget.Toast.makeText(context, "You're on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Failed to check for updates: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    } finally {
                        checkingUpdate.value = false
                    }
                }
            },
            enabled = !checkingUpdate.value,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF1D1D20),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) { 
            if (checkingUpdate.value) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (checkingUpdate.value) "Checking..." else "Check for updates")
        }
        
        // Update dialog
        if (showUpdateDialog.value && updateAvailable.value != null) {
            val release = updateAvailable.value!!
            AlertDialog(
                onDismissRequest = { showUpdateDialog.value = false },
                title = { Text("Update Available") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Version ${release.version} is available!")
                        Text(
                            text = release.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        UpdateChecker.downloadAndInstallApk(context, release.downloadUrl, release.version)
                        showUpdateDialog.value = false
                    }) {
                        Text("Download")
                    }
                },
                dismissButton = {
                    Button(onClick = { showUpdateDialog.value = false }) {
                        Text("Later")
                    }
                }
            )
        }
    }
}
