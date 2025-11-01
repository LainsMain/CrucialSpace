package com.crucialspace.app.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Switch

@Composable
fun SettingsScreen(onSaved: () -> Unit, onBack: () -> Unit = {}) {
	val context = LocalContext.current
	val store = remember { SettingsStore(context) }
	val base = remember { mutableStateOf(store.getBaseUrl()) }
	val secret = remember { mutableStateOf(store.getSharedSecret().orEmpty()) }
	val generateImages = remember { mutableStateOf(store.getGenerateImages()) }
    val activity = LocalContext.current as android.app.Activity
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(8.dp)) {
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
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, goldBrush, RoundedCornerShape(24.dp))
            .padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Generate images for memories", color = Color.White)
                Switch(
                    checked = generateImages.value,
                    onCheckedChange = { generateImages.value = it }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            OutlinedButton(
                onClick = {
                    store.setBaseUrl(base.value)
                    store.setSharedSecret(secret.value)
                    store.setGenerateImages(generateImages.value)
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
    }
}
