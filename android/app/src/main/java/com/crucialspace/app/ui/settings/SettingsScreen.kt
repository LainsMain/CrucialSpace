package com.crucialspace.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.crucialspace.app.settings.SettingsStore
import com.crucialspace.app.permissions.requestIgnoreBatteryOptimizations
import com.crucialspace.app.permissions.requestPostNotificationsIfNeeded
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.pm.ShortcutInfoCompat
import android.content.Intent
import com.crucialspace.app.share.ShareTargetActivity
import kotlinx.coroutines.launch
import com.crucialspace.app.update.UpdateChecker
import com.crucialspace.app.ui.components.*

/**
 * Material 3 Expressive Settings Screen
 * 
 * Complete redesign with pill buttons, squiggly dividers, and gold accents
 */
@Composable
fun SettingsScreen(onSaved: () -> Unit, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val geminiKey = remember { mutableStateOf(store.getGeminiApiKey().orEmpty()) }
    val languagePref = remember { mutableStateOf(store.getLanguagePreference()) }
    val showLangMenu = remember { mutableStateOf(false) }
    val themePref = remember { mutableStateOf(store.getThemePreference()) }
    val showThemeMenu = remember { mutableStateOf(false) }
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with bold typography
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconPillButton(
                onClick = onBack,
                icon = Icons.Filled.ArrowBack,
                size = 48.dp
            )
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge // Bold serif
            )
        }
        
        // Gold squiggly divider
        GoldWaveDivider()
        
        // API Key Section
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldSparkle()
                    Text(
                        "Gemini API Key",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                    
                    OutlinedTextField(
                        value = geminiKey.value,
                        onValueChange = { geminiKey.value = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge, // Pill-shaped
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        trailingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null)
                        }
                    )
                    
                Text(
                    "Get your API key at: aistudio.google.com/apikey",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Language Section
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "AI Response Language",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Box {
                    PillButton(
                        onClick = { showLangMenu.value = true },
                        text = languageOptions[languagePref.value] ?: "Auto-detect",
                        icon = Icons.Filled.Language,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    androidx.compose.material3.DropdownMenu(
                        expanded = showLangMenu.value,
                        onDismissRequest = { showLangMenu.value = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp
                    ) {
                        languageOptions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    languagePref.value = code
                                    showLangMenu.value = false
                                }
                            )
                        }
                    }
                }
                
                Text(
                    "AI will generate content in the selected language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Theme Section
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Box {
                    PillButton(
                        onClick = { showThemeMenu.value = true },
                        text = when (themePref.value) {
                            "dynamic" -> "Material You"
                            "purple" -> "Purple Expressive"
                            "blue" -> "Ocean Blue"
                            "green" -> "Forest Green"
                            "orange" -> "Sunset Orange"
                            "red" -> "Cherry Red"
                            "lavender" -> "Lavender Dream"
                            "midnight" -> "Midnight"
                            else -> "Purple Expressive"
                        },
                        icon = Icons.Filled.Palette,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    androidx.compose.material3.DropdownMenu(
                        expanded = showThemeMenu.value,
                        onDismissRequest = { showThemeMenu.value = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp
                    ) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            DropdownMenuItem(
                                text = { Text("Material You (Dynamic)") },
                                onClick = {
                                    themePref.value = "dynamic"
                                    showThemeMenu.value = false
                                    store.setThemePreference("dynamic")
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                            Divider()
                        }
                        listOf(
                            "purple" to "Purple Expressive",
                            "blue" to "Ocean Blue",
                            "green" to "Forest Green",
                            "orange" to "Sunset Orange",
                            "red" to "Cherry Red",
                            "lavender" to "Lavender Dream",
                            "midnight" to "Midnight"
                        ).forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    themePref.value = code
                                    showThemeMenu.value = false
                                    store.setThemePreference(code)
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                        }
                    }
                }
                
                Text(
                    "Choose colors that match your style",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Squiggly divider
        SquigglyWaveform(
            modifier = Modifier.fillMaxWidth(),
            style = SquigglyStyle.SECTION_DIVIDER,
            color = MaterialTheme.colorScheme.outline,
            animated = true,
            height = 12.dp
        )
        
        // Actions Section
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleLarge
        )
        
        // Save Button - Primary action
        PillButton(
            onClick = {
                store.setGeminiApiKey(geminiKey.value)
                store.setLanguagePreference(languagePref.value)
                store.setThemePreference(themePref.value)
                onSaved()
            },
            text = "Save Changes",
            icon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth(),
            height = 60.dp
        )
        
        // Secondary actions
        OutlinedPillButton(
            onClick = { requestPostNotificationsIfNeeded(activity, notifLauncher) },
            text = "Enable Notifications",
            icon = Icons.Filled.Notifications,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedPillButton(
            onClick = { requestIgnoreBatteryOptimizations(activity) },
            text = "Allow Background",
            icon = Icons.Filled.BatteryChargingFull,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedPillButton(
            onClick = {
                val intent = Intent(context, ShareTargetActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                }
                val shortcut = ShortcutInfoCompat.Builder(context, "enrich_shortcut")
                    .setShortLabel("Enrich")
                    .setLongLabel("Open Enrich")
                    .setIcon(IconCompat.createWithResource(activity, com.crucialspace.app.R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build()
                ShortcutManagerCompat.requestPinShortcut(activity, shortcut, null)
            },
            text = "Add Shortcut to Home",
            icon = Icons.Filled.AddToHomeScreen,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedPillButton(
            onClick = {
                checkingUpdate.value = true
                scope.launch {
                    try {
                        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        val release = UpdateChecker.checkForUpdate(currentVersion)
                        updateAvailable.value = release
                        if (release != null) {
                            showUpdateDialog.value = true
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "You're on the latest version!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "Failed to check for updates: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        checkingUpdate.value = false
                    }
                }
            },
            text = if (checkingUpdate.value) "Checking..." else "Check for Updates",
            icon = Icons.Filled.SystemUpdate,
            enabled = !checkingUpdate.value,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(32.dp))
    }
    
    // Update dialog
    if (showUpdateDialog.value && updateAvailable.value != null) {
        val release = updateAvailable.value!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog.value = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Update Available", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version ${release.version} is available!")
                    Text(
                        text = release.body,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                PillButton(
                    onClick = {
                        showUpdateDialog.value = false
                        scope.launch {
                            try {
                                UpdateChecker.downloadAndInstallApk(context, release.downloadUrl, release.version)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to download update",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    text = "Update"
                )
            },
            dismissButton = {
                OutlinedPillButton(
                    onClick = { showUpdateDialog.value = false },
                    text = "Later"
                )
            }
        )
    }
}

