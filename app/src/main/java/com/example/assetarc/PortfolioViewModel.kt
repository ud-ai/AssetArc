package com.example.assetarc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.assetarc.ErrorHandler
import com.example.assetarc.AppError

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
    private val _portfolio = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val portfolio: StateFlow<List<PortfolioItem>> = _portfolio.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val stockService = IndianStockService()

    fun addAsset(type: AssetType, symbol: String, quantity: Double, context: Context) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val price = when (type) {
                    is AssetType.IndianStock -> {
                        val stockData = stockService.getStockPrice(symbol, context)
                        stockData?.price ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                    is AssetType.Crypto -> {
                        fetchCoinGeckoPrice(symbol) ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                    is AssetType.Stock -> {
                        fetchUSStockPrice(symbol) ?: throw AppError.DataError("Failed to fetch price for $symbol")
                    }
                }

                val name = when (type) {
                    is AssetType.IndianStock -> stockService.getStockName(symbol)
                    is AssetType.Crypto -> getCryptoName(symbol)
                    is AssetType.Stock -> getUSStockName(symbol)
                }

                val newItem = PortfolioItem(
                    type = type,
                    symbol = symbol.uppercase(),
                    name = name,
                    quantity = quantity,
                    price = price
                )

                val currentPortfolio = _portfolio.value.toMutableList()
                val existingIndex = currentPortfolio.indexOfFirst { it.symbol == symbol && it.type == type }
                
                if (existingIndex >= 0) {
                    // Update existing item
                    currentPortfolio[existingIndex] = currentPortfolio[existingIndex].copy(
                        quantity = currentPortfolio[existingIndex].quantity + quantity,
                        price = price,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    // Add new item
                    currentPortfolio.add(newItem)
                }

                _portfolio.value = currentPortfolio
                Log.d("PortfolioViewModel", "Added asset: $symbol with price: $price")
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

    fun removeAsset(symbol: String, type: AssetType) {
        val currentPortfolio = _portfolio.value.toMutableList()
        currentPortfolio.removeAll { it.symbol == symbol && it.type == type }
        _portfolio.value = currentPortfolio
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

    private suspend fun fetchCoinGeckoPrice(symbol: String): Double? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=${symbol.lowercase()}&vs_currencies=usd")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val price = json.getJSONObject(symbol.lowercase()).getDouble("usd")
                    Log.d("PortfolioViewModel", "Fetched crypto price for $symbol: $price")
                    price
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
                val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

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

    private fun getCryptoName(symbol: String): String {
        val cryptoNames = mapOf(
            "BTC" to "Bitcoin",
            "ETH" to "Ethereum",
            "BNB" to "Binance Coin",
            "ADA" to "Cardano",
            "SOL" to "Solana",
            "DOT" to "Polkadot",
            "DOGE" to "Dogecoin",
            "AVAX" to "Avalanche",
            "MATIC" to "Polygon",
            "LINK" to "Chainlink"
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
            "INTC" to "Intel Corp."
        )
        return stockNames[symbol.uppercase()] ?: "$symbol"
    }
} 