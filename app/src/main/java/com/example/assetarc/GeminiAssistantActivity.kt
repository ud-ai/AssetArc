package com.example.assetarc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.assetarc.NewsApiService
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import android.view.animation.ScaleAnimation

class GeminiAssistantActivity : AppCompatActivity() {
    private lateinit var tvPortfolioInsights: TextView
    private lateinit var tvNewsSummary: TextView
    private lateinit var etChatInput: EditText
    private lateinit var btnSend: Button
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val geminiApi = GeminiApiService.create()
    private val newsApi = NewsApiService.create()
    private val portfolioViewModel: PortfolioViewModel by viewModels()

    private lateinit var shimmerPortfolio: ShimmerFrameLayout
    private lateinit var shimmerNews: ShimmerFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gemini_assistant)

        tvPortfolioInsights = findViewById(R.id.tvPortfolioInsights)
        tvNewsSummary = findViewById(R.id.tvNewsSummary)
        etChatInput = findViewById(R.id.etChatInput)
        btnSend = findViewById(R.id.btnSend)
        rvChatMessages = findViewById(R.id.rvChatMessages)
        shimmerPortfolio = findViewById(R.id.shimmerPortfolio)
        shimmerNews = findViewById(R.id.shimmerNews)

        // Load portfolio from storage
        portfolioViewModel.loadPortfolio(this)
        // Observe portfolio changes and update insights
        lifecycleScope.launchWhenStarted {
            portfolioViewModel.portfolio.collectLatest {
                loadPortfolioInsightsWithGemini()
            }
        }
        // Improved: Load news summaries using Gemini
        loadNewsSummariesWithGemini()
        // Load chat history from Firestore
        loadChatHistory()

        chatAdapter = ChatAdapter(chatMessages)
        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter
        rvChatMessages.itemAnimator = DefaultItemAnimator()

        btnSend.setOnClickListener {
            // Animate send button
            val scaleAnim = ScaleAnimation(
                1f, 0.85f, 1f, 0.85f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            )
            scaleAnim.duration = 100
            scaleAnim.fillAfter = true
            btnSend.startAnimation(scaleAnim)
            btnSend.postDelayed({
                btnSend.clearAnimation()
            }, 120)
            val userMessage = etChatInput.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessageToChat(userMessage, true, System.currentTimeMillis())
                etChatInput.text.clear()
                btnSend.isEnabled = false
                addMessageToChat("Gemini is typing...", false, System.currentTimeMillis())
                sendMessageToGemini(userMessage)
            }
        }
    }

    /**
     * Improved: Use Gemini to generate a portfolio summary from sample data.
     * Replace samplePortfolio with real data as needed.
     */
    private fun loadPortfolioInsightsWithGemini() {
        shimmerPortfolio.startShimmer()
        shimmerPortfolio.visibility = View.VISIBLE
        tvPortfolioInsights.visibility = View.INVISIBLE
        val realPortfolio = portfolioViewModel.getSummaryString()
        val prompt = "Analyze this portfolio and provide a performance summary and explanation of trends in simple language: $realPortfolio"
        val request = GeminiContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )
        geminiApi.generateContent(request).enqueue(object : Callback<GeminiContentResponse> {
            override fun onResponse(call: Call<GeminiContentResponse>, response: Response<GeminiContentResponse>) {
                val summary = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                tvPortfolioInsights.text = summary ?: "Could not generate insights."
                shimmerPortfolio.stopShimmer()
                shimmerPortfolio.visibility = View.GONE
                tvPortfolioInsights.visibility = View.VISIBLE
            }
            override fun onFailure(call: Call<GeminiContentResponse>, t: Throwable) {
                tvPortfolioInsights.text = "Error loading insights: ${t.localizedMessage}"
                shimmerPortfolio.stopShimmer()
                shimmerPortfolio.visibility = View.GONE
                tvPortfolioInsights.visibility = View.VISIBLE
            }
        })
    }

    /**
     * Improved: Use Gemini to summarize a sample news article.
     * Replace sampleNews with real news data as needed.
     */
    private fun loadNewsSummariesWithGemini() {
        shimmerNews.startShimmer()
        shimmerNews.visibility = View.VISIBLE
        tvNewsSummary.visibility = View.INVISIBLE
        // Fetch real news headlines
        newsApi.getTopHeadlines().enqueue(object : retrofit2.Callback<NewsApiResponse> {
            override fun onResponse(call: retrofit2.Call<NewsApiResponse>, response: retrofit2.Response<NewsApiResponse>) {
                val articles = response.body()?.articles
                if (articles.isNullOrEmpty()) {
                    tvNewsSummary.text = "No news found."
                    shimmerNews.stopShimmer()
                    shimmerNews.visibility = View.GONE
                    tvNewsSummary.visibility = View.VISIBLE
                    return
                }
                val newsString = articles.joinToString(" ") { "${it.title}. ${it.description ?: ""}" }
                val prompt = "Summarize the following news for an investor and highlight any important takeaways: $newsString"
                val request = GeminiContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                geminiApi.generateContent(request).enqueue(object : retrofit2.Callback<GeminiContentResponse> {
                    override fun onResponse(call: retrofit2.Call<GeminiContentResponse>, response: retrofit2.Response<GeminiContentResponse>) {
                        val summary = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        tvNewsSummary.text = summary ?: "Could not summarize news."
                        shimmerNews.stopShimmer()
                        shimmerNews.visibility = View.GONE
                        tvNewsSummary.visibility = View.VISIBLE
                    }
                    override fun onFailure(call: retrofit2.Call<GeminiContentResponse>, t: Throwable) {
                        tvNewsSummary.text = "Error summarizing news: ${t.localizedMessage}"
                        shimmerNews.stopShimmer()
                        shimmerNews.visibility = View.GONE
                        tvNewsSummary.visibility = View.VISIBLE
                    }
                })
            }
            override fun onFailure(call: retrofit2.Call<NewsApiResponse>, t: Throwable) {
                tvNewsSummary.text = "Error loading news: ${t.localizedMessage}"
                shimmerNews.stopShimmer()
                shimmerNews.visibility = View.GONE
                tvNewsSummary.visibility = View.VISIBLE
            }
        })
    }

    private fun loadChatHistory() {
        userId?.let { uid ->
            firestore.collection("users").document(uid).collection("chatHistory")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener { result ->
                    chatMessages.clear()
                    for (doc in result) {
                        val message = doc.getString("message") ?: ""
                        val isUser = message.startsWith("You:")
                        val cleanMsg = message.removePrefix("You: ").removePrefix("Gemini: ")
                        chatMessages.add(ChatMessage(cleanMsg, isUser))
                    }
                    chatAdapter.notifyDataSetChanged()
                    rvChatMessages.scrollToPosition(chatMessages.size - 1)
                }
        }
    }

    private fun addMessageToChat(message: String, isUser: Boolean, timestamp: Long) {
        chatMessages.add(ChatMessage(message, isUser, timestamp))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        rvChatMessages.post {
            rvChatMessages.smoothScrollToPosition(chatMessages.size - 1)
        }
    }

    private fun sendMessageToGemini(message: String) {
        val request = GeminiContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = message))))
        )
        geminiApi.generateContent(request).enqueue(object : Callback<GeminiContentResponse> {
            override fun onResponse(call: Call<GeminiContentResponse>, response: Response<GeminiContentResponse>) {
                removeLastGeminiMessage()
                val geminiText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sorry, I couldn't generate a response."
                addMessageToChat(geminiText, false, System.currentTimeMillis())
                saveMessageToFirestore("Gemini: $geminiText")
                btnSend.isEnabled = true
            }
            override fun onFailure(call: Call<GeminiContentResponse>, t: Throwable) {
                removeLastGeminiMessage()
                addMessageToChat("Error: ${t.localizedMessage}", false, System.currentTimeMillis())
                btnSend.isEnabled = true
            }
        })
    }

    private fun removeLastGeminiMessage() {
        if (chatMessages.isNotEmpty() && !chatMessages.last().isUser && chatMessages.last().text == "Gemini is typing...") {
            chatMessages.removeAt(chatMessages.size - 1)
            chatAdapter.notifyItemRemoved(chatMessages.size)
        }
    }

    private fun saveMessageToFirestore(message: String) {
        userId?.let { uid ->
            val chatRef = firestore.collection("users").document(uid).collection("chatHistory")
            val data = hashMapOf(
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )
            chatRef.add(data)
        }
    }
} 