package com.example.assetarc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.assetarc.ErrorHandler
import com.example.assetarc.AppError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Calendar

data class PortfolioItem(
    val type: AssetType,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val price: Double,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

sealed class AssetType {
    object Stock : AssetType()
    object Crypto : AssetType()
    object IndianStock : AssetType()
}

data class PortfolioSummary(
    val totalValue: Double,
    val totalChange: Double,
    val totalChangePercent: Double,
    val assetCount: Int
)

class PortfolioViewModel : ViewModel() {
    companion object {
        @Volatile
        private var INSTANCE: PortfolioViewModel? = null
        
        fun getInstance(): PortfolioViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PortfolioViewModel().also { INSTANCE = it }
            }
        }
    }

    private val _portfolio = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val portfolio: StateFlow<List<PortfolioItem>> = _portfolio.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val stockService = IndianStockService()
    private val gson = Gson()

    private val _isRealTimeUpdatesEnabled = MutableStateFlow(false)
    val isRealTimeUpdatesEnabled: StateFlow<Boolean> = _isRealTimeUpdatesEnabled.asStateFlow()

    private var priceUpdateJob: Job? = null

    // Load portfolio from SharedPreferences on initialization
    fun loadPortfolio(context: Context) {
        val prefs = context.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)
        val portfolioJson = prefs.getString("portfolio", "[]")
        try {
            val type = object : TypeToken<List<PortfolioItem>>() {}.type
            val savedPortfolio = gson.fromJson<List<PortfolioItem>>(portfolioJson, type) ?: emptyList()
            _portfolio.value = savedPortfolio
            Log.d("PortfolioViewModel", "Loaded portfolio with ${savedPortfolio.size} items")
        } catch (e: Exception) {
            Log.e("PortfolioViewModel", "Error loading portfolio", e)
            _portfolio.value = emptyList()
        }
    }

    // Save portfolio to SharedPreferences
    private fun savePortfolio(context: Context) {
        val prefs = context.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)
        val portfolioJson = gson.toJson(_portfolio.value)
        prefs.edit().putString("portfolio", portfolioJson).apply()
        Log.d("PortfolioViewModel", "Saved portfolio with ${_portfolio.value.size} items")
    }

    fun addAsset(type: AssetType, symbol: String, quantity: Double, context: Context) {
        Log.d("PortfolioViewModel", "Starting addAsset: type=$type, symbol=$symbol, quantity=$quantity")
        Log.d("PortfolioViewModel", "Current portfolio size before adding: ${_portfolio.value.size}")
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                Log.d("PortfolioViewModel", "Fetching price for $symbol...")
                val price = when (type) {
                    is AssetType.IndianStock -> {
                        val stockData = stockService.getStockPrice(symbol, context)
                        Log.d("PortfolioViewModel", "Indian stock data: $stockData")
                        stockData?.price ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                    is AssetType.Crypto -> {
                        val cryptoPrice = fetchCoinGeckoPrice(symbol)
                        Log.d("PortfolioViewModel", "Crypto price for $symbol: $cryptoPrice")
                        cryptoPrice ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                    is AssetType.Stock -> {
                        val stockPrice = fetchUSStockPrice(symbol)
                        Log.d("PortfolioViewModel", "US stock price for $symbol: $stockPrice")
                        stockPrice ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                }

                Log.d("PortfolioViewModel", "Price fetched: $price")

                val name = when (type) {
                    is AssetType.IndianStock -> stockService.getStockName(symbol)
                    is AssetType.Crypto -> getCryptoName(symbol)
                    is AssetType.Stock -> getUSStockName(symbol)
                }

                Log.d("PortfolioViewModel", "Name resolved: $name")

                val newItem = PortfolioItem(
                    type = type,
                    symbol = symbol,
                    name = name,
                    quantity = quantity,
                    price = price
                )

                Log.d("PortfolioViewModel", "Created new item: $newItem")

                val currentPortfolio = _portfolio.value.toMutableList()
                Log.d("PortfolioViewModel", "Current portfolio size: ${currentPortfolio.size}")
                
                val existingIndex = currentPortfolio.indexOfFirst { it.symbol == symbol && it.type == type }
                Log.d("PortfolioViewModel", "Existing index: $existingIndex")
                
                if (existingIndex >= 0) {
                    // Update existing item
                    Log.d("PortfolioViewModel", "Updating existing item at index $existingIndex")
                    currentPortfolio[existingIndex] = currentPortfolio[existingIndex].copy(
                        quantity = currentPortfolio[existingIndex].quantity + quantity,
                        price = price,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    // Add new item
                    Log.d("PortfolioViewModel", "Adding new item to portfolio")
                    currentPortfolio.add(newItem)
                }

                Log.d("PortfolioViewModel", "Final portfolio size: ${currentPortfolio.size}")
                _portfolio.value = currentPortfolio
                savePortfolio(context) // Save to persistent storage
                Log.d("PortfolioViewModel", "Successfully added asset: $symbol with price: $price")
                Log.d("PortfolioViewModel", "Portfolio after save: ${_portfolio.value}")
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error adding asset: ${e.message}", e)
                val appError = when (e) {
                    is AppError -> e
                    else -> AppError.UnknownError("Failed to add $symbol: ${e.message}", e)
                }
                _error.value = ErrorHandler.getErrorMessage(appError)
                ErrorHandler.handleError(e, context, false)
            } finally {
                _loading.value = false
            }
        }
    }

    fun removeAsset(symbol: String, type: AssetType, context: Context) {
        val currentPortfolio = _portfolio.value.toMutableList()
        currentPortfolio.removeAll { it.symbol == symbol && it.type == type }
        _portfolio.value = currentPortfolio
        savePortfolio(context) // Save to persistent storage
    }

    fun updatePrices(context: Context) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val updatedPortfolio = _portfolio.value.map { item ->
                    try {
                        val newPrice = when (item.type) {
                            is AssetType.IndianStock -> {
                                val stockData = stockService.getStockPrice(item.symbol, context)
                                stockData?.price ?: item.price
                            }
                            is AssetType.Crypto -> {
                                fetchCoinGeckoPrice(item.symbol) ?: item.price
                            }
                            is AssetType.Stock -> {
                                fetchUSStockPrice(item.symbol) ?: item.price
                            }
                        }

                        val change = newPrice - item.price
                        val changePercent = if (item.price > 0) (change / item.price) * 100 else 0.0

                        item.copy(
                            price = newPrice,
                            change = change,
                            changePercent = changePercent,
                            lastUpdated = System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("PortfolioViewModel", "Error updating price for ${item.symbol}: ${e.message}")
                        ErrorHandler.handleError(e, context, false)
                        item
                    }
                }

                _portfolio.value = updatedPortfolio
                savePortfolio(context) // Save updated prices
                Log.d("PortfolioViewModel", "Updated prices for ${updatedPortfolio.size} assets")
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error updating prices: ${e.message}", e)
                val appError = when (e) {
                    is AppError -> e
                    else -> AppError.UnknownError("Failed to update prices: ${e.message}", e)
                }
                _error.value = ErrorHandler.getErrorMessage(appError)
                ErrorHandler.handleError(e, context, false)
            } finally {
                _loading.value = false
            }
        }
    }

    fun getPortfolioSummary(): PortfolioSummary {
        val items = _portfolio.value
        val totalValue = items.sumOf { it.quantity * it.price }
        val totalChange = items.sumOf { it.quantity * it.change }
        val totalChangePercent = if (totalValue > 0) (totalChange / totalValue) * 100 else 0.0

        return PortfolioSummary(
            totalValue = totalValue,
            totalChange = totalChange,
            totalChangePercent = totalChangePercent,
            assetCount = items.size
        )
    }

    fun getAssetsByType(type: AssetType): List<PortfolioItem> {
        return _portfolio.value.filter { it.type == type }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun fetchCoinGeckoPrice(symbol: String): Double? {
        return withContext(Dispatchers.IO) {
            try {
                // Map symbols to CoinGecko IDs
                val coinGeckoIds = mapOf(
                    "BTC" to "bitcoin",
                    "ETH" to "ethereum",
                    "BNB" to "binancecoin",
                    "ADA" to "cardano",
                    "SOL" to "solana",
                    "XRP" to "ripple",
                    "DOT" to "polkadot",
                    "DOGE" to "dogecoin",
                    "AVAX" to "avalanche-2",
                    "MATIC" to "matic-network",
                    "LINK" to "chainlink",
                    "UNI" to "uniswap",
                    "LTC" to "litecoin",
                    "BCH" to "bitcoin-cash",
                    "XLM" to "stellar",
                    "ATOM" to "cosmos",
                    "FTT" to "ftx-token",
                    "NEAR" to "near",
                    "ALGO" to "algorand",
                    "VET" to "vechain"
                )
                
                val coinId = coinGeckoIds[symbol.uppercase()] ?: symbol.lowercase()
                val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=$coinId&vs_currencies=usd&include_24hr_change=true")
                
                Log.d("PortfolioViewModel", "Fetching crypto price for $symbol using ID: $coinId")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "AssetArc/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("PortfolioViewModel", "CoinGecko response: ${response.take(200)}...")
                    
                    val json = JSONObject(response)
                    if (json.has(coinId)) {
                        val price = json.getJSONObject(coinId).getDouble("usd")
                        Log.d("PortfolioViewModel", "Fetched crypto price for $symbol: $price")
                        price
                    } else {
                        Log.w("PortfolioViewModel", "CoinGecko response doesn't contain $coinId")
                        null
                    }
                } else {
                    Log.w("PortfolioViewModel", "CoinGecko API failed with response code: ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error fetching crypto price for $symbol", e)
                null
            }
        }
    }

    private suspend fun fetchUSStockPrice(symbol: String): Double? {
        return withContext(Dispatchers.IO) {
            try {
                // Using Yahoo Finance API for US stocks
                val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1m&range=1d")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val chart = json.getJSONObject("chart")
                    val result = chart.getJSONArray("result").getJSONObject(0)
                    val meta = result.getJSONObject("meta")
                    val price = meta.getDouble("regularMarketPrice")
                    Log.d("PortfolioViewModel", "Fetched US stock price for $symbol: $price")
                    price
                } else {
                    Log.w("PortfolioViewModel", "Yahoo Finance API failed with response code: ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e("PortfolioViewModel", "Error fetching US stock price for $symbol", e)
                null
            }
        }
    }

    fun getCryptoName(symbol: String): String {
        val cryptoNames = mapOf(
            "BTC" to "Bitcoin",
            "ETH" to "Ethereum",
            "BNB" to "Binance Coin",
            "ADA" to "Cardano",
            "SOL" to "Solana",
            "XRP" to "Ripple",
            "DOT" to "Polkadot",
            "DOGE" to "Dogecoin",
            "AVAX" to "Avalanche",
            "MATIC" to "Polygon",
            "LINK" to "Chainlink",
            "UNI" to "Uniswap",
            "LTC" to "Litecoin",
            "BCH" to "Bitcoin Cash",
            "XLM" to "Stellar",
            "ATOM" to "Cosmos",
            "FTT" to "FTX Token",
            "NEAR" to "NEAR Protocol",
            "ALGO" to "Algorand",
            "VET" to "VeChain"
        )
        return cryptoNames[symbol.uppercase()] ?: "$symbol"
    }

    private fun getUSStockName(symbol: String): String {
        val stockNames = mapOf(
            "AAPL" to "Apple Inc.",
            "GOOGL" to "Alphabet Inc.",
            "MSFT" to "Microsoft Corp.",
            "AMZN" to "Amazon.com Inc.",
            "TSLA" to "Tesla Inc.",
            "META" to "Meta Platforms Inc.",
            "NFLX" to "Netflix Inc.",
            "NVDA" to "NVIDIA Corp.",
            "AMD" to "Advanced Micro Devices Inc.",
            "INTC" to "Intel Corp.",
            "ORCL" to "Oracle Corp.",
            "IBM" to "International Business Machines Corp.",
            "CSCO" to "Cisco Systems Inc.",
            "QCOM" to "Qualcomm Inc.",
            "TXN" to "Texas Instruments Inc.",
            "AVGO" to "Broadcom Inc.",
            "MU" to "Micron Technology Inc.",
            "ADBE" to "Adobe Inc.",
            "CRM" to "Salesforce Inc.",
            "PYPL" to "PayPal Holdings Inc."
        )
        return stockNames[symbol.uppercase()] ?: "$symbol"
    }

    fun startRealTimeUpdates(context: Context) {
        if (_isRealTimeUpdatesEnabled.value) return
        
        _isRealTimeUpdatesEnabled.value = true
        priceUpdateJob = viewModelScope.launch {
            while (_isRealTimeUpdatesEnabled.value) {
                try {
                    if (isMarketOpen()) {
                        Log.d("PortfolioViewModel", "Updating real-time prices...")
                        updatePrices(context)
                    } else {
                        Log.d("PortfolioViewModel", "Market is closed, skipping price update")
                    }
                } catch (e: Exception) {
                    Log.e("PortfolioViewModel", "Error in real-time price update", e)
                }
                
                // Wait 5 seconds before next update
                delay(5000)
            }
        }
        Log.d("PortfolioViewModel", "Started real-time price updates")
    }

    fun stopRealTimeUpdates() {
        _isRealTimeUpdatesEnabled.value = false
        priceUpdateJob?.cancel()
        priceUpdateJob = null
        Log.d("PortfolioViewModel", "Stopped real-time price updates")
    }

    private fun isMarketOpen(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentTime = hour * 60 + minute

        // Check if it's a weekday (Monday = 2, Sunday = 1)
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            return false
        }

        // Indian Market Hours: 9:15 AM to 3:30 PM IST (3:45 AM to 10:00 AM UTC)
        val indianMarketOpen = 3 * 60 + 45  // 3:45 AM UTC
        val indianMarketClose = 10 * 60      // 10:00 AM UTC

        // US Market Hours: 9:30 AM to 4:00 PM EST (2:30 PM to 9:00 PM UTC)
        val usMarketOpen = 14 * 60 + 30      // 2:30 PM UTC
        val usMarketClose = 21 * 60          // 9:00 PM UTC

        // Crypto markets are always open
        val isCryptoMarketOpen = true

        // Check if any market is open
        val isIndianMarketOpen = currentTime >= indianMarketOpen && currentTime <= indianMarketClose
        val isUSMarketOpen = currentTime >= usMarketOpen && currentTime <= usMarketClose

        Log.d("PortfolioViewModel", "Market status - Indian: $isIndianMarketOpen, US: $isUSMarketOpen, Crypto: $isCryptoMarketOpen")
        
        return isIndianMarketOpen || isUSMarketOpen || isCryptoMarketOpen
    }

    // Returns a summary string of the current portfolio for Gemini prompts
    fun getSummaryString(): String {
        return _portfolio.value.joinToString(", ") {
            "${it.name} (${it.symbol}): ${it.quantity} units at ₹${it.price} (" +
            (if (it.changePercent >= 0) "+" else "") + "${"%.2f".format(it.changePercent)}%)"
        }
    }
} 