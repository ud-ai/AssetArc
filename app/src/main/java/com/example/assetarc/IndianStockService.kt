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
    
    companion object {
        private const val TAG = "IndianStockService"
        private const val YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        private const val ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query"
        private const val ALPHA_VANTAGE_API_KEY = "demo" // Free demo key
        
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

    suspend fun getStockPrice(symbol: String, context: Context): StockData? {
        return try {
            Log.d(TAG, "Fetching price for Indian stock: $symbol")
            
            // Try Yahoo Finance first
            val yahooData = getYahooFinanceData(symbol)
            if (yahooData != null) {
                Log.d(TAG, "Successfully fetched data from Yahoo Finance for $symbol")
                return yahooData
            }
            
            // Fallback to Alpha Vantage
            val alphaData = getAlphaVantageData(symbol)
            if (alphaData != null) {
                Log.d(TAG, "Successfully fetched data from Alpha Vantage for $symbol")
                return alphaData
            }
            
            // Return mock data if both APIs fail
            Log.d(TAG, "Both APIs failed, returning mock data for $symbol")
            getMockStockData(symbol)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock price for $symbol", e)
            getMockStockData(symbol)
        }
    }

    private suspend fun getYahooFinanceData(symbol: String): StockData? {
        return withContext(Dispatchers.IO) {
            try {
                // Add .NS suffix for Indian stocks on Yahoo Finance
                val fullSymbol = if (!symbol.endsWith(".NS")) "$symbol.NS" else symbol
                val url = URL("$YAHOO_FINANCE_BASE_URL$fullSymbol?interval=1d&range=1d")
                
                Log.d(TAG, "Yahoo Finance URL: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Yahoo Finance response code: $responseCode")
                
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Yahoo Finance response: ${response.take(500)}...")
                    
                    val json = JSONObject(response)
                    val chart = json.getJSONObject("chart")
                    val result = chart.getJSONArray("result").getJSONObject(0)
                    val meta = result.getJSONObject("meta")
                    val timestamp = result.getJSONArray("timestamp")
                    val indicators = result.getJSONObject("indicators")
                    val quote = indicators.getJSONArray("quote").getJSONObject(0)
                    
                    val currentPrice = meta.getDouble("regularMarketPrice")
                    val previousClose = meta.getDouble("previousClose")
                    val change = currentPrice - previousClose
                    val changePercent = (change / previousClose) * 100
                    
                    val open = quote.getJSONArray("open").getDouble(0)
                    val high = quote.getJSONArray("high").getDouble(0)
                    val low = quote.getJSONArray("low").getDouble(0)
                    val volume = quote.getJSONArray("volume").getLong(0)
                    
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
                    Log.w(TAG, "Yahoo Finance failed with response code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Yahoo Finance", e)
                null
            }
        }
    }

    private suspend fun getAlphaVantageData(symbol: String): StockData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$ALPHA_VANTAGE_BASE_URL?function=GLOBAL_QUOTE&symbol=$symbol.BSE&apikey=$ALPHA_VANTAGE_API_KEY")
                
                Log.d(TAG, "Alpha Vantage URL: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Alpha Vantage response code: $responseCode")
                
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Alpha Vantage response: ${response.take(500)}...")
                    
                    val json = JSONObject(response)
                    val globalQuote = json.getJSONObject("Global Quote")
                    
                    val currentPrice = globalQuote.getString("05. price").toDouble()
                    val change = globalQuote.getString("09. change").toDouble()
                    val changePercent = globalQuote.getString("10. change percent").removeSuffix("%").toDouble()
                    val volume = globalQuote.getString("06. volume").toLong()
                    val previousClose = globalQuote.getString("08. previous close").toDouble()
                    val open = globalQuote.getString("02. open").toDouble()
                    val high = globalQuote.getString("03. high").toDouble()
                    val low = globalQuote.getString("04. low").toDouble()
                    
                    StockData(
                        symbol = symbol.uppercase(),
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
                    Log.w(TAG, "Alpha Vantage failed with response code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Alpha Vantage", e)
                null
            }
        }
    }

    private fun getMockStockData(symbol: String): StockData {
        val mockData = mapOf(
            "RELIANCE" to StockData("RELIANCE", "Reliance Industries Ltd", 2450.75, 45.25, 1.88, 12500000, 1650000.0, 2480.0, 2400.0, 2405.5, 2405.5),
            "TCS" to StockData("TCS", "Tata Consultancy Services Ltd", 3850.25, -25.75, -0.66, 8500000, 1450000.0, 3900.0, 3800.0, 3876.0, 3876.0),
            "HDFCBANK" to StockData("HDFCBANK", "HDFC Bank Ltd", 1650.50, 12.50, 0.76, 9800000, 950000.0, 1665.0, 1635.0, 1638.0, 1638.0),
            "INFY" to StockData("INFY", "Infosys Ltd", 1450.75, 35.25, 2.49, 12000000, 600000.0, 1465.0, 1415.0, 1415.5, 1415.5),
            "ICICIBANK" to StockData("ICICIBANK", "ICICI Bank Ltd", 950.25, -15.75, -1.63, 15000000, 650000.0, 970.0, 940.0, 966.0, 966.0),
            "HINDUNILVR" to StockData("HINDUNILVR", "Hindustan Unilever Ltd", 2450.00, 50.00, 2.08, 3500000, 580000.0, 2460.0, 2400.0, 2400.0, 2400.0),
            "ITC" to StockData("ITC", "ITC Ltd", 450.75, 8.25, 1.86, 25000000, 570000.0, 455.0, 442.5, 442.5, 442.5),
            "SBIN" to StockData("SBIN", "State Bank of India", 650.50, 12.50, 1.96, 20000000, 580000.0, 655.0, 638.0, 638.0, 638.0),
            "BHARTIARTL" to StockData("BHARTIARTL", "Bharti Airtel Ltd", 850.25, -20.75, -2.38, 12000000, 470000.0, 875.0, 845.0, 871.0, 871.0),
            "KOTAKBANK" to StockData("KOTAKBANK", "Kotak Mahindra Bank Ltd", 1850.75, 25.75, 1.41, 4500000, 370000.0, 1865.0, 1825.0, 1825.0, 1825.0),
            "AXISBANK" to StockData("AXISBANK", "Axis Bank Ltd", 950.50, -10.50, -1.09, 12000000, 290000.0, 965.0, 945.0, 961.0, 961.0),
            "ASIANPAINT" to StockData("ASIANPAINT", "Asian Paints Ltd", 3250.25, 75.25, 2.37, 2800000, 310000.0, 3275.0, 3175.0, 3175.0, 3175.0),
            "MARUTI" to StockData("MARUTI", "Maruti Suzuki India Ltd", 9500.75, 150.75, 1.61, 1800000, 290000.0, 9550.0, 9350.0, 9350.0, 9350.0),
            "SUNPHARMA" to StockData("SUNPHARMA", "Sun Pharmaceutical Industries Ltd", 950.25, 20.25, 2.18, 8500000, 230000.0, 960.0, 930.0, 930.0, 930.0),
            "TATAMOTORS" to StockData("TATAMOTORS", "Tata Motors Ltd", 650.50, 25.50, 4.08, 25000000, 210000.0, 660.0, 625.0, 625.0, 625.0)
        )
        
        return mockData[symbol.uppercase()] ?: StockData(
            symbol = symbol.uppercase(),
            name = "$symbol Ltd",
            price = 1000.0,
            change = 10.0,
            changePercent = 1.0,
            volume = 1000000,
            marketCap = 100000.0,
            high = 1010.0,
            low = 990.0,
            open = 990.0,
            previousClose = 990.0
        )
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
        return listOf(
            TrendingStock("TATAMOTORS", "Tata Motors Ltd", 650.50, 25.50, 4.08, 25000000),
            TrendingStock("ASIANPAINT", "Asian Paints Ltd", 3250.25, 75.25, 2.37, 2800000),
            TrendingStock("INFY", "Infosys Ltd", 1450.75, 35.25, 2.49, 12000000),
            TrendingStock("HINDUNILVR", "Hindustan Unilever Ltd", 2450.00, 50.00, 2.08, 3500000),
            TrendingStock("RELIANCE", "Reliance Industries Ltd", 2450.75, 45.25, 1.88, 12500000)
        )
    }

    suspend fun getTopLosers(): List<TrendingStock> {
        return listOf(
            TrendingStock("BHARTIARTL", "Bharti Airtel Ltd", 850.25, -20.75, -2.38, 12000000),
            TrendingStock("AXISBANK", "Axis Bank Ltd", 950.50, -10.50, -1.09, 12000000),
            TrendingStock("ICICIBANK", "ICICI Bank Ltd", 950.25, -15.75, -1.63, 15000000),
            TrendingStock("TCS", "Tata Consultancy Services Ltd", 3850.25, -25.75, -0.66, 8500000),
            TrendingStock("KOTAKBANK", "Kotak Mahindra Bank Ltd", 1850.75, 25.75, 1.41, 4500000)
        )
    }

    suspend fun getMarketOverview(): MarketOverview {
        return MarketOverview(
            sensex = 72500.50,
            nifty = 21850.25,
            sensexChange = 450.75,
            niftyChange = 125.50,
            sensexChangePercent = 0.63,
            niftyChangePercent = 0.58
        )
    }

    suspend fun getTrendingStocks(): List<TrendingStock> {
        return listOf(
            TrendingStock("RELIANCE", "Reliance Industries Ltd", 2450.75, 45.25, 1.88, 12500000),
            TrendingStock("TCS", "Tata Consultancy Services Ltd", 3850.25, -25.75, -0.66, 8500000),
            TrendingStock("HDFCBANK", "HDFC Bank Ltd", 1650.50, 12.50, 0.76, 9800000),
            TrendingStock("INFY", "Infosys Ltd", 1450.75, 35.25, 2.49, 12000000),
            TrendingStock("ICICIBANK", "ICICI Bank Ltd", 950.25, -15.75, -1.63, 15000000),
            TrendingStock("HINDUNILVR", "Hindustan Unilever Ltd", 2450.00, 50.00, 2.08, 3500000),
            TrendingStock("ITC", "ITC Ltd", 450.75, 8.25, 1.86, 25000000),
            TrendingStock("SBIN", "State Bank of India", 650.50, 12.50, 1.96, 20000000)
        )
    }
} 