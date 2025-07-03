package com.example.assetarc

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import com.example.assetarc.BuildConfig

// Data classes for NewsAPI response

data class NewsApiResponse(
    @SerializedName("articles") val articles: List<NewsArticle>
)

data class NewsArticle(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?
)

interface NewsApiService {
    @GET("v2/top-headlines")
    fun getTopHeadlines(
        @Query("country") country: String = "in",
        @Query("apiKey") apiKey: String = com.example.assetarc.BuildConfig.NEWS_API_KEY
    ): Call<NewsApiResponse>

    companion object {
        private const val BASE_URL = "https://newsapi.org/"
        fun create(): NewsApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(NewsApiService::class.java)
        }
    }
} 