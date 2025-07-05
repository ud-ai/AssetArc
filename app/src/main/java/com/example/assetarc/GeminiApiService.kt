package com.example.assetarc

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Protocol
import com.google.gson.annotations.SerializedName
import com.example.assetarc.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit

// Gemini API request/response data classes

data class GeminiContentRequest(
    @SerializedName("contents") val contents: List<Content>
)

data class Content(
    @SerializedName("parts") val parts: List<Part>
)

data class Part(
    @SerializedName("text") val text: String
)

data class GeminiContentResponse(
    @SerializedName("candidates") val candidates: List<Candidate>?
)

data class Candidate(
    @SerializedName("content") val content: Content?
)

interface GeminiApiService {
    @POST("v1/models/gemini-2.5-pro:generateContent")
    fun generateContent(@Body request: GeminiContentRequest): Call<GeminiContentResponse>

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        private const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB cache
        private const val MAX_IDLE_CONNECTIONS = 5
        private const val KEEP_ALIVE_DURATION = 5L // 5 minutes

        fun create(): GeminiApiService {
            val apiKey = com.example.assetarc.BuildConfig.GEMINI_API_KEY
            
            // Create cache directory
            val cacheDir = File(System.getProperty("java.io.tmpdir"), "gemini_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val client = OkHttpClient.Builder()
                // Optimized timeouts
                .connectTimeout(15, TimeUnit.SECONDS) // Reduced from 60s
                .readTimeout(30, TimeUnit.SECONDS)    // Reduced from 60s
                .writeTimeout(15, TimeUnit.SECONDS)   // Reduced from 60s
                
                // Connection pooling for better performance
                .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES))
                
                // Enable HTTP/2 for better performance
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                
                // Add caching
                .cache(Cache(cacheDir, CACHE_SIZE))
                
                // Add compression
                .addInterceptor { chain ->
                    val original = chain.request()
                    val compressedRequest = original.newBuilder()
                        .header("Accept-Encoding", "gzip, deflate")
                        .build()
                    chain.proceed(compressedRequest)
                }
                
                // Add API key interceptor
                .addInterceptor { chain ->
                    val original = chain.request()
                    val url = original.url.newBuilder()
                        .addQueryParameter("key", apiKey)
                        .build()
                    val request = original.newBuilder().url(url).build()
                    chain.proceed(request)
                }
                
                // Add retry interceptor for failed requests
                .addInterceptor { chain ->
                    val request = chain.request()
                    var response: Response? = null
                    var exception: Exception? = null
                    
                    // Try up to 3 times
                    for (attempt in 1..3) {
                        try {
                            response = chain.proceed(request)
                            if (response.isSuccessful) {
                                return@addInterceptor response
                            }
                            response.close()
                        } catch (e: Exception) {
                            exception = e
                            if (attempt == 3) {
                                throw e
                            }
                            // Wait before retry (exponential backoff)
                            Thread.sleep(attempt * 1000L)
                        }
                    }
                    
                    response ?: throw exception ?: RuntimeException("Request failed after 3 attempts")
                }
                
                .build()
                
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(GeminiApiService::class.java)
        }
    }
} 