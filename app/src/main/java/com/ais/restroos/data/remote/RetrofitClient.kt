package com.ais.restroos.data.remote

import com.ais.restroos.BuildConfig
import com.ais.restroos.core.auth.AuthEventManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val DEFAULT_PRODUCTION_BASE_URL = "https://restro.trakiyo.in/api/"

    private var currentBaseUrl: String = DEFAULT_PRODUCTION_BASE_URL
    @Volatile
    private var authToken: String? = null
    @Volatile
    private var cachedApiService: RestroApiService? = null

    fun updateBaseUrl(url: String) {
        if (url.isNotBlank()) {
            val formatted = if (url.endsWith("/")) url else "$url/"
            if (formatted != currentBaseUrl) {
                currentBaseUrl = formatted
                cachedApiService = null // Invalidate cached instance
            }
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun updateAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        val token = authToken
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        requestBuilder.header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // Handle HTTP 401 Unauthorized or 403 Forbidden
        if (response.code == 401 || response.code == 403) {
            // Exclude public endpoints like login and register from triggering session expiry
            val path = original.url.encodedPath
            if (!path.contains("/auth/login") && !path.contains("/auth/register") && !path.contains("/health")) {
                // Only notify session expired if we actually sent a token that the server rejected!
                if (!token.isNullOrBlank()) {
                    AuthEventManager.notifySessionExpired("Session expired. Please log in again to continue.")
                }
            }
        }

        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: RestroApiService
        get() {
            return cachedApiService ?: synchronized(this) {
                cachedApiService ?: Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(RestroApiService::class.java).also {
                        cachedApiService = it
                    }
            }
        }
}
