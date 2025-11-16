package com.crucialspace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crucialspace.app.ui.feed.FeedScreen
import com.crucialspace.app.ui.settings.SettingsScreen
import com.crucialspace.app.ui.detail.DetailScreen
import com.crucialspace.app.ui.theme.CrucialTheme
import androidx.navigation.navDeepLink
import com.crucialspace.app.ui.search.SearchScreen
import com.crucialspace.app.ui.collections.CollectionsScreen
import com.crucialspace.app.ui.collections.CollectionDetailScreen
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.crucialspace.app.update.UpdateChecker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
        setContent {
            CrucialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav()
                }
            }
        }
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNav() {
	val navController = rememberNavController()
	val context = LocalContext.current
	val updateAvailable = remember { mutableStateOf<com.crucialspace.app.update.GithubRelease?>(null) }
	val showUpdateDialog = remember { mutableStateOf(false) }
	
	// Check for updates on startup
	LaunchedEffect(Unit) {
		kotlinx.coroutines.withContext(Dispatchers.IO) {
			try {
				val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
				val release = UpdateChecker.checkForUpdate(currentVersion)
				if (release != null) {
					updateAvailable.value = release
					showUpdateDialog.value = true
				}
			} catch (e: Exception) {
				android.util.Log.e("MainActivity", "Failed to check for updates on startup", e)
			}
		}
	}
	
	// Update dialog
	if (showUpdateDialog.value && updateAvailable.value != null) {
		val release = updateAvailable.value!!
		AlertDialog(
			onDismissRequest = { showUpdateDialog.value = false },
			title = { androidx.compose.material3.Text("Update Available") },
			text = {
				androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					androidx.compose.material3.Text("Version ${release.version} is available!")
					androidx.compose.material3.Text(
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
					androidx.compose.material3.Text("Download")
				}
			},
			dismissButton = {
				Button(onClick = { showUpdateDialog.value = false }) {
					androidx.compose.material3.Text("Later")
				}
			}
		)
	}
	
    NavHost(navController, startDestination = "feed") {
        composable(
            route = "feed",
            deepLinks = listOf(navDeepLink { uriPattern = "app://feed" })
        ) {
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
            val retFlow = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("return_to_collections", false)
            val ret by (retFlow ?: MutableStateFlow(false)).collectAsState(initial = false)
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            LaunchedEffect(ret) {
                if (ret) {
                    pagerState.scrollToPage(1)
                    navController.currentBackStackEntry?.savedStateHandle?.set("return_to_collections", false)
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { p ->
                    if (p == 0) {
                        FeedScreen(
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenDetail = { id -> navController.navigate("detail/$id") },
                            onOpenSearch = { navController.navigate("search") },
                            onOpenCollection = { cid -> navController.navigate("collection/$cid") }
                        )
                    } else {
                        CollectionsScreen(
                            onOpenCollection = { id -> navController.navigate("collection/$id") },
                            onOpenSearch = { navController.navigate("search") },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                }
                // removed overlay; detection lives on container pointerInput above
                // bottom dots indicator
                Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
                    listOf(0, 1).forEach { i ->
                        Surface(shape = RoundedCornerShape(50), color = if (pagerState.currentPage == i) Color.White else Color.Gray, tonalElevation = 0.dp) {
                            androidx.compose.foundation.layout.Box(Modifier.size(8.dp)) {}
                        }
                        if (i == 0) Spacer(Modifier.size(8.dp))
                    }
                }
            }
        }
        composable("settings") { SettingsScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() }) }
        composable("search") { SearchScreen(onOpenDetail = { id -> navController.navigate("detail/$id") }, onBack = { navController.popBackStack() }) }
        composable(
            route = "collections",
            deepLinks = listOf(navDeepLink { uriPattern = "app://collections" })
        ) {
            CollectionsScreen(
                onOpenCollection = { id -> navController.navigate("collection/$id") },
                onOpenSearch = { navController.navigate("search") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("collection/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            CollectionDetailScreen(id = id, onBack = {
                navController.previousBackStackEntry?.savedStateHandle?.set("return_to_collections", true)
                navController.popBackStack()
            })
        }
        composable(
            route = "detail/{id}",
            deepLinks = listOf(navDeepLink { uriPattern = "app://detail/{id}" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            DetailScreen(id, onClose = { navController.popBackStack() })
        }

        // Trampoline for shortcuts: app://shortcut/{dest}
        composable(
            route = "shortcut/{dest}",
            deepLinks = listOf(navDeepLink { uriPattern = "app://shortcut/{dest}" })
        ) { backStackEntry ->
            val dest = backStackEntry.arguments?.getString("dest") ?: "feed"
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            androidx.compose.runtime.LaunchedEffect(dest) {
                when (dest) {
                    "enrich" -> {
                        val intent = android.content.Intent(ctx, com.crucialspace.app.share.ShareTargetActivity::class.java)
                        intent.action = android.content.Intent.ACTION_SEND
                        intent.type = "text/plain"
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                        navController.popBackStack()
                    }
                    "collections" -> {
                        navController.popBackStack()
                        navController.navigate("collections")
                    }
                    else -> {
                        navController.popBackStack()
                        // fall back to feed (already the start destination)
                    }
                }
            }
        }
	}
}
