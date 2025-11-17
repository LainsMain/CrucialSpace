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
		val v = prefs.getString(KEY_BASE_URL, null)
		return if (v.isNullOrBlank()) "http://10.0.2.2:8000/" else v
	}

	fun setBaseUrl(url: String) {
		prefs.edit().putString(KEY_BASE_URL, url).apply()
	}

	fun getSharedSecret(): String? = prefs.getString(KEY_SECRET, null)

	fun setSharedSecret(secret: String) {
		prefs.edit().putString(KEY_SECRET, secret).apply()
	}

	fun isLocalAiEnabled(): Boolean {
		return prefs.getBoolean(KEY_LOCAL_AI, false)
	}

	fun setLocalAiEnabled(enabled: Boolean) {
		prefs.edit().putBoolean(KEY_LOCAL_AI, enabled).apply()
	}

	fun getGeminiApiKey(): String? {
		return prefs.getString(KEY_GEMINI_API_KEY, null)
	}

	fun setGeminiApiKey(key: String) {
		prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
	}

	fun getGeminiModel(): String {
		return prefs.getString(KEY_GEMINI_MODEL, "gemini-2.5-flash")!!
	}

	fun setGeminiModel(model: String) {
		prefs.edit().putString(KEY_GEMINI_MODEL, model).apply()
	}

	fun getEmbeddingModel(): String {
		return prefs.getString(KEY_EMBEDDING_MODEL, "text-embedding-004")!!
	}

	fun setEmbeddingModel(model: String) {
		prefs.edit().putString(KEY_EMBEDDING_MODEL, model).apply()
	}

	fun getLanguagePreference(): String {
		return prefs.getString(KEY_LANGUAGE_PREF, "auto")!!
	}

	fun setLanguagePreference(language: String) {
		prefs.edit().putString(KEY_LANGUAGE_PREF, language).apply()
	}

	fun getThemePreference(): String {
		return prefs.getString(KEY_THEME_PREF, "purple")!!
	}

	fun setThemePreference(theme: String) {
		prefs.edit().putString(KEY_THEME_PREF, theme).apply()
	}

	fun getCollectionsSortPreference(): String {
		return prefs.getString(KEY_COLLECTIONS_SORT, "date-created-desc")!!
	}

	fun setCollectionsSortPreference(sort: String) {
		prefs.edit().putString(KEY_COLLECTIONS_SORT, sort).apply()
	}

	companion object {
		private const val KEY_BASE_URL = "base_url"
		private const val KEY_SECRET = "secret"
		private const val KEY_LOCAL_AI = "local_ai"
		private const val KEY_GEMINI_API_KEY = "gemini_api_key"
		private const val KEY_GEMINI_MODEL = "gemini_model"
		private const val KEY_EMBEDDING_MODEL = "embedding_model"
		private const val KEY_LANGUAGE_PREF = "language_preference"
		private const val KEY_THEME_PREF = "theme_preference"
		private const val KEY_COLLECTIONS_SORT = "collections_sort_preference"
	}
}
