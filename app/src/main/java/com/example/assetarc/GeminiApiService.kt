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
import com.google.gson.annotations.SerializedName
import com.example.assetarc.BuildConfig

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

        fun create(): GeminiApiService {
            val apiKey = com.example.assetarc.BuildConfig.GEMINI_API_KEY
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val url = original.url.newBuilder()
                        .addQueryParameter("key", apiKey)
                        .build()
                    val request = original.newBuilder().url(url).build()
                    chain.proceed(request)
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