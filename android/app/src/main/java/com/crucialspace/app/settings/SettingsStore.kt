package com.crucialspace.app.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsStore(private val context: Context) {
	private val prefs by lazy {
		val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
		EncryptedSharedPreferences.create(
			"cs_settings",
			masterKeyAlias,
			context,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		)
	}

	fun getBaseUrl(): String {
		return prefs.getString(KEY_BASE_URL, "http://10.0.2.2:8000/")!!
	}

	fun setBaseUrl(url: String) {
		prefs.edit().putString(KEY_BASE_URL, url).apply()
	}

	fun getSharedSecret(): String? = prefs.getString(KEY_SECRET, null)

	fun setSharedSecret(secret: String) {
		prefs.edit().putString(KEY_SECRET, secret).apply()
	}

	fun getGenerateImages(): Boolean = prefs.getBoolean(KEY_GENERATE_IMAGES, false)

	fun setGenerateImages(enabled: Boolean) {
		prefs.edit().putBoolean(KEY_GENERATE_IMAGES, enabled).apply()
	}

	companion object {
		private const val KEY_BASE_URL = "base_url"
		private const val KEY_SECRET = "secret"
		private const val KEY_GENERATE_IMAGES = "generate_images"
	}
}
