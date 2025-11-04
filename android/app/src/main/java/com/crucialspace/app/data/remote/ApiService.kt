package com.crucialspace.app.data.remote

import android.content.Context
import com.crucialspace.app.settings.SettingsStore
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body
import java.io.File

interface ApiService {
	@Multipart
	@POST("process")
	suspend fun process(
    @Part image: MultipartBody.Part?,
		@Part("note_text") note: RequestBody?,
    @Part audio: MultipartBody.Part?,
    @Part("now_utc") nowUtc: RequestBody?,
    @Part("existing_collections") existingCollections: RequestBody?
	): AiResult

  @Multipart
  @POST("process")
  suspend fun processRaw(
    @Part image: MultipartBody.Part?,
    @Part("note_text") note: RequestBody?,
    @Part audio: MultipartBody.Part?,
    @Part("now_utc") nowUtc: RequestBody?,
    @Part("existing_collections") existingCollections: RequestBody?
  ): Response<okhttp3.ResponseBody>

  @POST("embed")
  suspend fun embed(@Body req: EmbedRequest): EmbeddingResponse

	companion object {
		fun create(context: Context): ApiService {
			val settings = SettingsStore(context)
			var baseUrl = settings.getBaseUrl()
			if (!baseUrl.endsWith("/")) baseUrl += "/"

			val secret = settings.getSharedSecret()
			val headerInterceptor = Interceptor { chain ->
				val req = chain.request().newBuilder()
				if (!secret.isNullOrBlank()) req.addHeader("X-CS-Secret", secret)
				chain.proceed(req.build())
			}
      val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
      val client = OkHttpClient.Builder()
				.addInterceptor(headerInterceptor)
				.addInterceptor(logging)
        .retryOnConnectionFailure(true)
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(60))
        .writeTimeout(java.time.Duration.ofSeconds(60))
        .callTimeout(java.time.Duration.ofSeconds(120))
        .pingInterval(java.time.Duration.ofSeconds(30))
				.build()

			val moshi = Moshi.Builder().build()
			return Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(client)
				.addConverterFactory(MoshiConverterFactory.create(moshi))
				.build()
				.create(ApiService::class.java)
		}
	}
}

fun partFromFile(name: String, file: File, contentType: String?): MultipartBody.Part {
	val body = file.asRequestBody((contentType ?: "application/octet-stream").toMediaTypeOrNull())
	return MultipartBody.Part.createFormData(name, file.name, body)
}

fun textPart(value: String?): RequestBody? = value?.toRequestBody("text/plain".toMediaTypeOrNull())
