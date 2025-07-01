package com.example.assetarc

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class StockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val marketCap: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val previousClose: Double
)

data class MarketOverview(
    val sensex: Double,
    val nifty: Double,
    val sensexChange: Double,
    val niftyChange: Double,
    val sensexChangePercent: Double,
    val niftyChangePercent: Double
)

data class TrendingStock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long
)

class IndianStockService {
    
    // In-memory cache for historical prices: key = "symbol-days"
    private val historicalCache = mutableMapOf<String, List<Pair<Long, Double>>>()
    
    companion object {
        private const val TAG = "IndianStockService"
        private const val YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        private const val ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query"
        private const val ALPHA_VANTAGE_API_KEY = "YS4SA0VVGZ8ZQW6MR"
        
        // Popular Indian stocks with their symbols
        val INDIAN_STOCKS = mapOf(
            "RELIANCE" to "Reliance Industries",
            "TCS" to "Tata Consultancy Services", 
            "HDFCBANK" to "HDFC Bank",
            "INFY" to "Infosys",
            "ICICIBANK" to "ICICI Bank",
            "HINDUNILVR" to "Hindustan Unilever",
            "ITC" to "ITC Limited",
            "SBIN" to "State Bank of India",
            "BHARTIARTL" to "Bharti Airtel",
            "KOTAKBANK" to "Kotak Mahindra Bank",
            "AXISBANK" to "Axis Bank",
            "ASIANPAINT" to "Asian Paints",
            "MARUTI" to "Maruti Suzuki",
            "HCLTECH" to "HCL Technologies",
            "SUNPHARMA" to "Sun Pharmaceutical",
            "WIPRO" to "Wipro",
            "ULTRACEMCO" to "UltraTech Cement",
            "TITAN" to "Titan Company",
            "BAJFINANCE" to "Bajaj Finance",
            "NESTLEIND" to "Nestle India",
            "POWERGRID" to "Power Grid Corporation",
            "NTPC" to "NTPC Limited",
            "TECHM" to "Tech Mahindra",
            "BAJAJFINSV" to "Bajaj Finserv",
            "ADANIENT" to "Adani Enterprises",
            "JSWSTEEL" to "JSW Steel",
            "ONGC" to "Oil & Natural Gas Corporation",
            "COALINDIA" to "Coal India",
            "TATAMOTORS" to "Tata Motors",
            "SHREECEM" to "Shree Cement",
            "DRREDDY" to "Dr Reddy's Laboratories",
            "BRITANNIA" to "Britannia Industries",
            "CIPLA" to "Cipla",
            "EICHERMOT" to "Eicher Motors",
            "HEROMOTOCO" to "Hero MotoCorp",
            "DIVISLAB" to "Divi's Laboratories",
            "GRASIM" to "Grasim Industries",
            "HINDALCO" to "Hindalco Industries",
            "VEDL" to "Vedanta Limited",
            "TATASTEEL" to "Tata Steel",
            "BPCL" to "Bharat Petroleum",
            "IOC" to "Indian Oil Corporation",
            "UPL" to "UPL Limited",
            "SBILIFE" to "SBI Life Insurance",
            "HDFCLIFE" to "HDFC Life Insurance",
            "TATACONSUM" to "Tata Consumer Products",
            "BAJAJ-AUTO" to "Bajaj Auto",
            "INDUSINDBK" to "IndusInd Bank",
            "LT" to "Larsen & Toubro",
            "MM" to "Mahindra & Mahindra",
            "LIC" to "Life Insurance Corporation of India",
            "ADANIPORTS" to "Adani Ports & SEZ",
            "ADANIPOWER" to "Adani Power",
            "ADANITRANS" to "Adani Transmission",
            "ADANIGREEN" to "Adani Green Energy",
            "ADANITOTAL" to "Adani Total Gas",
            "ADANIWILMAR" to "Adani Wilmar",
            "HDFC" to "Housing Development Finance Corporation",
            "HDFCAMC" to "HDFC Asset Management Company",
            "HDFCBANK" to "HDFC Bank",
            "HINDCOPPER" to "Hindustan Copper",
            "HINDPETRO" to "Hindustan Petroleum",
            "HINDZINC" to "Hindustan Zinc",
            "IDEA" to "Vodafone Idea",
            "INDIGO" to "InterGlobe Aviation",
            "IRCTC" to "Indian Railway Catering & Tourism",
            "JINDALSTEL" to "Jindal Steel & Power",
            "JSWENERGY" to "JSW Energy",
            "KOTAKBANK" to "Kotak Mahindra Bank",
            "LUPIN" to "Lupin",
            "M&M" to "Mahindra & Mahindra",
            "MARICO" to "Marico",
            "MCDOWELL-N" to "United Spirits",
            "MUTHOOTFIN" to "Muthoot Finance",
            "NESTLEIND" to "Nestle India",
            "NMDC" to "NMDC",
            "PEL" to "Piramal Enterprises",
            "PERSISTENT" to "Persistent Systems",
            "PIDILITIND" to "Pidilite Industries",
            "PNB" to "Punjab National Bank",
            "POWERGRID" to "Power Grid Corporation",
            "PVR" to "PVR Cinemas",
            "RBLBANK" to "RBL Bank",
            "SAIL" to "Steel Authority of India",
            "SIEMENS" to "Siemens",
            "TATACOMM" to "Tata Communications",
            "TATAPOWER" to "Tata Power",
            "TECHM" to "Tech Mahindra",
            "TORNTPHARM" to "Torrent Pharmaceuticals",
            "ULTRACEMCO" to "UltraTech Cement",
            "VEDL" to "Vedanta",
            "VOLTAS" to "Voltas",
            "WIPRO" to "Wipro",
            "ZEEL" to "Zee Entertainment Enterprises"
        )
    }

    suspend fun getStockPrice(symbol: String, context: Context?): StockData? {
        return try {
            Log.d(TAG, "Fetching price for Indian stock: $symbol")
            // Try Yahoo Finance first
            val yahooData = getYahooFinanceData(symbol)
            if (yahooData != null) {
                Log.d(TAG, "Successfully fetched data from Yahoo Finance for $symbol")
                return yahooData
            }
            // Try Alpha Vantage with .BSE and .NSE
            val bseData = getAlphaVantageData(symbol, exchange = "BSE")
            if (bseData != null) {
                Log.d(TAG, "Successfully fetched data from Alpha Vantage for $symbol.BSE")
                return bseData
            }
            val nseData = getAlphaVantageData(symbol, exchange = "NSE")
            if (nseData != null) {
                Log.d(TAG, "Successfully fetched data from Alpha Vantage for $symbol.NSE")
                return nseData
            }
            Log.e(TAG, "Both APIs failed, returning null for $symbol")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock price for $symbol", e)
            null
        }
    }

    private suspend fun getYahooFinanceData(symbol: String): StockData? {
        return withContext(Dispatchers.IO) {
            try {
                // Add .NS suffix for Indian stocks on Yahoo Finance
                val fullSymbol = if (!symbol.endsWith(".NS")) "$symbol.NS" else symbol
                val url = URL("$YAHOO_FINANCE_BASE_URL$fullSymbol?interval=1d&range=1d")
                Log.d(TAG, "Yahoo Finance URL: $url for symbol: $fullSymbol")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                Log.d(TAG, "Yahoo Finance response code: $responseCode for symbol: $fullSymbol")
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Yahoo Finance response for $fullSymbol: ${response.take(500)}...")
                    val json = JSONObject(response)
                    val chart = json.getJSONObject("chart")
                    val result = chart.getJSONArray("result").getJSONObject(0)
                    val meta = result.getJSONObject("meta")
                    val indicators = result.getJSONObject("indicators")
                    val quote = indicators.getJSONArray("quote").getJSONObject(0)
                    val currentPrice = meta.optDouble("regularMarketPrice", 0.0)
                    val previousClose: Double
                    if (meta.has("previousClose")) {
                        previousClose = meta.optDouble("previousClose", 0.0)
                    } else {
                        Log.w(TAG, "Yahoo Finance response missing 'previousClose' for $fullSymbol. Using 0.0 as default.")
                        previousClose = 0.0
                    }
                    val change = currentPrice - previousClose
                    val changePercent = if (previousClose != 0.0) (change / previousClose) * 100 else 0.0
                    val open = quote.getJSONArray("open").optDouble(0, 0.0)
                    val high = quote.getJSONArray("high").optDouble(0, 0.0)
                    val low = quote.getJSONArray("low").optDouble(0, 0.0)
                    val volume = quote.getJSONArray("volume").optLong(0, 0L)
                    StockData(
                        symbol = symbol.uppercase(),
                        name = getStockName(symbol),
                        price = currentPrice,
                        change = change,
                        changePercent = changePercent,
                        volume = volume,
                        marketCap = 0.0, // Not available in basic API
                        high = high,
                        low = low,
                        open = open,
                        previousClose = previousClose
                    )
                } else {
                    Log.w(TAG, "Yahoo Finance failed with response code: $responseCode for $fullSymbol")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Yahoo Finance for $symbol", e)
                null
            }
        }
    }

    private suspend fun getAlphaVantageData(symbol: String, exchange: String = "BSE"): StockData? {
        return withContext(Dispatchers.IO) {
            try {
                val fullSymbol = if (exchange == "BSE" || exchange == "NSE") "$symbol.$exchange" else symbol
                val url = URL("$ALPHA_VANTAGE_BASE_URL?function=GLOBAL_QUOTE&symbol=$fullSymbol&apikey=$ALPHA_VANTAGE_API_KEY")
                Log.d(TAG, "Alpha Vantage URL: $url for symbol: $fullSymbol")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                Log.d(TAG, "Alpha Vantage response code: $responseCode for symbol: $fullSymbol")
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Alpha Vantage response for $fullSymbol: ${response.take(500)}...")
                    val json = JSONObject(response)
                    if (!json.has("Global Quote") || json.getJSONObject("Global Quote").length() == 0) {
                        Log.w(TAG, "Alpha Vantage returned empty Global Quote for $fullSymbol")
                        return@withContext null
                    }
                    val globalQuote = json.getJSONObject("Global Quote")
                    val currentPrice = globalQuote.getString("05. price").toDoubleOrNull() ?: return@withContext null
                    val change = globalQuote.getString("09. change").toDoubleOrNull() ?: 0.0
                    val changePercent = globalQuote.getString("10. change percent").removeSuffix("%").toDoubleOrNull() ?: 0.0
                    val volume = globalQuote.optString("06. volume").toLongOrNull() ?: 0L
                    val previousClose = globalQuote.optString("08. previous close").toDoubleOrNull() ?: 0.0
                    val open = globalQuote.optString("02. open").toDoubleOrNull() ?: 0.0
                    val high = globalQuote.optString("03. high").toDoubleOrNull() ?: 0.0
                    val low = globalQuote.optString("04. low").toDoubleOrNull() ?: 0.0
                    StockData(
                        symbol = fullSymbol.uppercase(),
                        name = getStockName(symbol),
                        price = currentPrice,
                        change = change,
                        changePercent = changePercent,
                        volume = volume,
                        marketCap = 0.0,
                        high = high,
                        low = low,
                        open = open,
                        previousClose = previousClose
                    )
                } else {
                    Log.w(TAG, "Alpha Vantage failed with response code: $responseCode for $fullSymbol")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Alpha Vantage for $symbol.$exchange", e)
                null
            }
        }
    }

    fun getStockName(symbol: String): String {
        return INDIAN_STOCKS[symbol.uppercase()] ?: symbol.uppercase()
    }

    suspend fun searchStocks(query: String): List<String> {
        val allStocks = listOf(
            "RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "HINDUNILVR", "ITC", "SBIN", "BHARTIARTL",
            "KOTAKBANK", "AXISBANK", "ASIANPAINT", "MARUTI", "SUNPHARMA", "TATAMOTORS", "WIPRO", "ULTRACEMCO",
            "TITAN", "BAJFINANCE", "NESTLEIND", "POWERGRID", "BAJAJFINSV", "HCLTECH", "ADANIENT", "JSWSTEEL",
            "ONGC", "COALINDIA", "TECHM", "TATACONSUM", "SHREECEM", "DIVISLAB", "BRITANNIA", "EICHERMOT",
            "HEROMOTOCO", "CIPLA", "DRREDDY", "GRASIM", "HINDALCO", "VEDL", "TATASTEEL", "BPCL", "IOC",
            "M&M", "UPL", "BAJAJ-AUTO", "SHRIRAMFIN", "INDUSINDBK", "TATAPOWER", "APOLLOHOSP", "BAJAJFINSV"
        )
        
        return allStocks.filter { it.contains(query, ignoreCase = true) }
    }

    suspend fun getTopGainers(): List<TrendingStock> {
        return try {
            val gainers = mutableListOf<TrendingStock>()
            
            // Fetch real-time data for top Indian stocks
            val topStocks = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "HINDUNILVR", "ITC", "SBIN", "BHARTIARTL", "KOTAKBANK")
            
            topStocks.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        gainers.add(TrendingStock(
                            symbol = stockData.symbol,
                            name = stockData.name,
                            price = stockData.price,
                            change = stockData.change,
                            changePercent = stockData.changePercent,
                            volume = stockData.volume
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching data for $symbol", e)
                }
            }
            
            // Sort by change percentage and return top 5
            gainers.sortedByDescending { it.changePercent }.take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top gainers", e)
            emptyList()
        }
    }

    suspend fun getTopLosers(): List<TrendingStock> {
        return try {
            val losers = mutableListOf<TrendingStock>()
            
            // Fetch real-time data for top Indian stocks
            val topStocks = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "HINDUNILVR", "ITC", "SBIN", "BHARTIARTL", "KOTAKBANK")
            
            topStocks.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        losers.add(TrendingStock(
                            symbol = stockData.symbol,
                            name = stockData.name,
                            price = stockData.price,
                            change = stockData.change,
                            changePercent = stockData.changePercent,
                            volume = stockData.volume
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching data for $symbol", e)
                }
            }
            
            // Sort by change percentage (ascending for losers) and return top 5
            losers.sortedBy { it.changePercent }.take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top losers", e)
            emptyList()
        }
    }

    suspend fun getMarketOverview(): MarketOverview? {
        return try {
            // Fetch real-time data for Sensex and Nifty
            val sensexData = getStockPrice("^BSESN", null) // Sensex symbol
            val niftyData = getStockPrice("^NSEI", null)   // Nifty symbol
            if (sensexData == null || niftyData == null) return null
            MarketOverview(
                sensex = sensexData.price,
                nifty = niftyData.price,
                sensexChange = sensexData.change,
                niftyChange = niftyData.change,
                sensexChangePercent = sensexData.changePercent,
                niftyChangePercent = niftyData.changePercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching market overview", e)
            null
        }
    }

    suspend fun getTrendingStocks(): List<TrendingStock> {
        return try {
            val trending = mutableListOf<TrendingStock>()
            
            // Fetch real-time data for trending Indian stocks
            val trendingSymbols = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "HINDUNILVR", "ITC", "SBIN")
            
            trendingSymbols.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        trending.add(TrendingStock(
                            symbol = stockData.symbol,
                            name = stockData.name,
                            price = stockData.price,
                            change = stockData.change,
                            changePercent = stockData.changePercent,
                            volume = stockData.volume
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching data for $symbol", e)
                }
            }
            
            // Return all fetched stocks
            trending
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trending stocks", e)
            emptyList()
        }
    }

    // Fetch historical daily close prices for the last [days] days
    suspend fun getHistoricalPrices(symbol: String, days: Int): List<Pair<Long, Double>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Long, Double>>()
        try {
            val fullSymbol = if (!symbol.endsWith(".NS")) "$symbol.NS" else symbol
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$fullSymbol?interval=1d&range=${days}d")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val chart = json.getJSONObject("chart")
                val chartResult = chart.getJSONArray("result").getJSONObject(0)
                val timestamps = chartResult.getJSONArray("timestamp")
                val closePrices = chartResult.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close")
                for (i in 0 until timestamps.length()) {
                    val ts = timestamps.getLong(i)
                    val price = closePrices.optDouble(i, Double.NaN)
                    if (!price.isNaN()) {
                        result.add(ts to price)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching historical prices for $symbol", e)
        }
        result
    }
} 