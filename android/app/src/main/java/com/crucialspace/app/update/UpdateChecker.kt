package com.crucialspace.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class GithubRelease(
    val version: String,
    val name: String,
    val body: String,
    val downloadUrl: String,
    val publishedAt: String
)

object UpdateChecker {
    private const val GITHUB_API = "https://api.github.com/repos/LainsMain/CrucialSpace/releases/latest"
    
    suspend fun checkForUpdate(currentVersion: String): GithubRelease? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(GITHUB_API)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string() ?: return@withContext null
            
            val json = JSONObject(jsonStr)
            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", "")
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")
            
            // Extract version from tag (e.g., "v1.0.1" -> "1.0.1")
            val version = tagName.removePrefix("v")
            
            // Find APK download URL
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            
            if (apkUrl == null) return@withContext null
            
            // Compare versions
            if (isNewerVersion(version, currentVersion)) {
                GithubRelease(version, name, body, apkUrl, publishedAt)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Failed to check for updates", e)
            null
        }
    }
    
    private fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
                val r = remoteParts.getOrNull(i) ?: 0
                val c = currentParts.getOrNull(i) ?: 0
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    fun downloadAndInstallApk(context: Context, downloadUrl: String, version: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Crucial Space Update")
                .setDescription("Downloading version $version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CrucialSpace-$version.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            
            Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Failed to download update", e)
            Toast.makeText(context, "Failed to download update: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

