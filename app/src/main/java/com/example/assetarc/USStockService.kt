package com.example.assetarc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class USStockData(
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

data class USMarketOverview(
    val sp500: Double,
    val nasdaq: Double,
    val dowJones: Double,
    val sp500Change: Double,
    val nasdaqChange: Double,
    val dowJonesChange: Double,
    val sp500ChangePercent: Double,
    val nasdaqChangePercent: Double,
    val dowJonesChangePercent: Double
)

data class USTrendingStock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long
)

class USStockService {
    
    companion object {
        private const val TAG = "USStockService"
        private const val YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        
        // Popular US stocks with their symbols
        val US_STOCKS = mapOf(
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
            "PYPL" to "PayPal Holdings Inc.",
            "JPM" to "JPMorgan Chase & Co.",
            "BAC" to "Bank of America Corp.",
            "WMT" to "Walmart Inc.",
            "JNJ" to "Johnson & Johnson",
            "PG" to "Procter & Gamble Co.",
            "UNH" to "UnitedHealth Group Inc.",
            "HD" to "Home Depot Inc.",
            "DIS" to "Walt Disney Co.",
            "V" to "Visa Inc.",
            "MA" to "Mastercard Inc.",
            "ABT" to "Abbott Laboratories",
            "KO" to "Coca-Cola Co.",
            "PEP" to "PepsiCo Inc.",
            "TMO" to "Thermo Fisher Scientific Inc.",
            "COST" to "Costco Wholesale Corp.",
            "ACN" to "Accenture PLC",
            "DHR" to "Danaher Corp.",
            "NEE" to "NextEra Energy Inc.",
            "LLY" to "Eli Lilly and Co.",
            "VZ" to "Verizon Communications Inc.",
            "CMCSA" to "Comcast Corp.",
            "ADP" to "Automatic Data Processing Inc.",
            "PM" to "Philip Morris International Inc.",
            "RTX" to "Raytheon Technologies Corp.",
            "T" to "AT&T Inc.",
            "UPS" to "United Parcel Service Inc.",
            "QCOM" to "Qualcomm Inc.",
            "INTU" to "Intuit Inc.",
            "HON" to "Honeywell International Inc.",
            "LOW" to "Lowe's Companies Inc.",
            "SPGI" to "S&P Global Inc.",
            "ISRG" to "Intuitive Surgical Inc.",
            "GILD" to "Gilead Sciences Inc.",
            "AMGN" to "Amgen Inc.",
            "TXN" to "Texas Instruments Inc.",
            "BKNG" to "Booking Holdings Inc.",
            "ADI" to "Analog Devices Inc.",
            "MDLZ" to "Mondelez International Inc.",
            "REGN" to "Regeneron Pharmaceuticals Inc.",
            "VRTX" to "Vertex Pharmaceuticals Inc.",
            "KLAC" to "KLA Corp.",
            "PANW" to "Palo Alto Networks Inc.",
            "CHTR" to "Charter Communications Inc.",
            "MAR" to "Marriott International Inc.",
            "ORLY" to "O'Reilly Automotive Inc.",
            "MNST" to "Monster Beverage Corp.",
            "KDP" to "Keurig Dr Pepper Inc.",
            "SNPS" to "Synopsys Inc.",
            "CDNS" to "Cadence Design Systems Inc.",
            "MELI" to "MercadoLibre Inc.",
            "ASML" to "ASML Holding NV",
            "JD" to "JD.com Inc.",
            "PDD" to "Pinduoduo Inc.",
            "BIDU" to "Baidu Inc.",
            "NIO" to "NIO Inc.",
            "XPEV" to "XPeng Inc.",
            "LI" to "Li Auto Inc.",
            "BABA" to "Alibaba Group Holding Ltd.",
            "TCEHY" to "Tencent Holdings Ltd.",
            "NTES" to "NetEase Inc.",
            "BILI" to "Bilibili Inc.",
            "DIDI" to "DiDi Global Inc.",
            "XOM" to "Exxon Mobil Corp.",
            "CVX" to "Chevron Corp.",
            "COP" to "ConocoPhillips",
            "EOG" to "EOG Resources Inc.",
            "SLB" to "Schlumberger Ltd.",
            "HAL" to "Halliburton Co.",
            "BKR" to "Baker Hughes Co.",
            "MPC" to "Marathon Petroleum Corp.",
            "PSX" to "Phillips 66",
            "VLO" to "Valero Energy Corp.",
            "KMI" to "Kinder Morgan Inc.",
            "OKE" to "Oneok Inc.",
            "WMB" to "Williams Companies Inc.",
            "ET" to "Energy Transfer LP",
            "ENB" to "Enbridge Inc.",
            "TRP" to "TC Energy Corp.",
            "PPL" to "PPL Corp.",
            "DUK" to "Duke Energy Corp.",
            "SO" to "Southern Co.",
            "D" to "Dominion Energy Inc.",
            "AEP" to "American Electric Power Co.",
            "XEL" to "Xcel Energy Inc.",
            "SRE" to "Sempra Energy",
            "WEC" to "WEC Energy Group Inc.",
            "DTE" to "DTE Energy Co.",
            "ED" to "Consolidated Edison Inc.",
            "EIX" to "Edison International",
            "PCG" to "PG&E Corp.",
            "AEE" to "Ameren Corp.",
            "CMS" to "CMS Energy Corp.",
            "CNP" to "CenterPoint Energy Inc.",
            "NRG" to "NRG Energy Inc.",
            "EXC" to "Exelon Corp.",
            "FE" to "FirstEnergy Corp.",
            "AES" to "AES Corp."
        )
    }

    suspend fun getStockPrice(symbol: String, context: Context?): USStockData? {
        return try {
            Log.d(TAG, "Fetching price for US stock: $symbol")
            
            val stockData = getYahooFinanceData(symbol)
            if (stockData != null) {
                Log.d(TAG, "Successfully fetched data from Yahoo Finance for $symbol")
                return stockData
            }
            
            // Return mock data if API fails
            Log.d(TAG, "API failed, returning mock data for $symbol")
            getMockStockData(symbol)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stock price for $symbol", e)
            getMockStockData(symbol)
        }
    }

    private suspend fun getYahooFinanceData(symbol: String): USStockData? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$YAHOO_FINANCE_BASE_URL$symbol?interval=1d&range=1d")
                
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
                    
                    USStockData(
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

    private fun getMockStockData(symbol: String): USStockData {
        val mockData = mapOf(
            "AAPL" to USStockData("AAPL", "Apple Inc.", 180.0, 2.5, 1.4, 85000000, 2800000000000.0, 182.0, 177.5, 177.5, 177.5),
            "GOOGL" to USStockData("GOOGL", "Alphabet Inc.", 140.0, -1.2, -0.85, 25000000, 1800000000000.0, 142.0, 138.0, 141.2, 141.2),
            "MSFT" to USStockData("MSFT", "Microsoft Corp.", 350.0, 5.8, 1.68, 35000000, 2600000000000.0, 352.0, 344.2, 344.2, 344.2),
            "AMZN" to USStockData("AMZN", "Amazon.com Inc.", 130.0, 3.2, 2.52, 45000000, 1400000000000.0, 132.0, 126.8, 126.8, 126.8),
            "TSLA" to USStockData("TSLA", "Tesla Inc.", 240.0, -8.5, -3.42, 120000000, 760000000000.0, 250.0, 235.0, 248.5, 248.5),
            "META" to USStockData("META", "Meta Platforms Inc.", 320.0, 12.5, 4.06, 25000000, 820000000000.0, 322.0, 307.5, 307.5, 307.5),
            "NFLX" to USStockData("NFLX", "Netflix Inc.", 450.0, 15.2, 3.49, 8000000, 200000000000.0, 452.0, 434.8, 434.8, 434.8),
            "NVDA" to USStockData("NVDA", "NVIDIA Corp.", 480.0, 25.8, 5.68, 60000000, 1200000000000.0, 485.0, 454.2, 454.2, 454.2),
            "AMD" to USStockData("AMD", "Advanced Micro Devices Inc.", 120.0, 4.2, 3.62, 80000000, 190000000000.0, 122.0, 115.8, 115.8, 115.8),
            "INTC" to USStockData("INTC", "Intel Corp.", 45.0, -1.8, -3.85, 45000000, 190000000000.0, 47.0, 44.0, 46.8, 46.8)
        )
        
        return mockData[symbol.uppercase()] ?: USStockData(
            symbol = symbol.uppercase(),
            name = "$symbol Inc.",
            price = 100.0,
            change = 1.0,
            changePercent = 1.0,
            volume = 1000000,
            marketCap = 100000000.0,
            high = 101.0,
            low = 99.0,
            open = 99.0,
            previousClose = 99.0
        )
    }

    fun getStockName(symbol: String): String {
        return US_STOCKS[symbol.uppercase()] ?: symbol.uppercase()
    }

    suspend fun searchStocks(query: String): List<String> {
        val allStocks = US_STOCKS.keys.toList()
        return allStocks.filter { it.contains(query, ignoreCase = true) }
    }

    suspend fun getTopGainers(): List<USTrendingStock> {
        return try {
            val gainers = mutableListOf<USTrendingStock>()
            
            // Fetch real-time data for top US stocks
            val topStocks = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NFLX", "NVDA", "AMD", "INTC")
            
            topStocks.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        gainers.add(USTrendingStock(
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
            // Fallback to mock data
            listOf(
                USTrendingStock("NVDA", "NVIDIA Corp.", 480.0, 25.8, 5.68, 60000000),
                USTrendingStock("META", "Meta Platforms Inc.", 320.0, 12.5, 4.06, 25000000),
                USTrendingStock("NFLX", "Netflix Inc.", 450.0, 15.2, 3.49, 8000000),
                USTrendingStock("AMD", "Advanced Micro Devices Inc.", 120.0, 4.2, 3.62, 80000000),
                USTrendingStock("MSFT", "Microsoft Corp.", 350.0, 5.8, 1.68, 35000000)
            )
        }
    }

    suspend fun getTopLosers(): List<USTrendingStock> {
        return try {
            val losers = mutableListOf<USTrendingStock>()
            
            // Fetch real-time data for top US stocks
            val topStocks = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NFLX", "NVDA", "AMD", "INTC")
            
            topStocks.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        losers.add(USTrendingStock(
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
            // Fallback to mock data
            listOf(
                USTrendingStock("TSLA", "Tesla Inc.", 240.0, -8.5, -3.42, 120000000),
                USTrendingStock("INTC", "Intel Corp.", 45.0, -1.8, -3.85, 45000000),
                USTrendingStock("GOOGL", "Alphabet Inc.", 140.0, -1.2, -0.85, 25000000),
                USTrendingStock("AAPL", "Apple Inc.", 180.0, 2.5, 1.4, 85000000),
                USTrendingStock("AMZN", "Amazon.com Inc.", 130.0, 3.2, 2.52, 45000000)
            )
        }
    }

    suspend fun getMarketOverview(): USMarketOverview {
        return try {
            // Fetch real-time data for major indices
            val sp500Data = getStockPrice("^GSPC", null) // S&P 500
            val nasdaqData = getStockPrice("^IXIC", null) // NASDAQ
            val dowData = getStockPrice("^DJI", null)     // Dow Jones
            
            val sp500 = sp500Data?.price ?: 4500.0
            val nasdaq = nasdaqData?.price ?: 14000.0
            val dowJones = dowData?.price ?: 35000.0
            val sp500Change = sp500Data?.change ?: 25.0
            val nasdaqChange = nasdaqData?.change ?: 75.0
            val dowJonesChange = dowData?.change ?: 150.0
            val sp500ChangePercent = sp500Data?.changePercent ?: 0.56
            val nasdaqChangePercent = nasdaqData?.changePercent ?: 0.54
            val dowJonesChangePercent = dowData?.changePercent ?: 0.43
            
            USMarketOverview(
                sp500 = sp500,
                nasdaq = nasdaq,
                dowJones = dowJones,
                sp500Change = sp500Change,
                nasdaqChange = nasdaqChange,
                dowJonesChange = dowJonesChange,
                sp500ChangePercent = sp500ChangePercent,
                nasdaqChangePercent = nasdaqChangePercent,
                dowJonesChangePercent = dowJonesChangePercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching market overview", e)
            // Fallback to mock data
            USMarketOverview(
                sp500 = 4500.0,
                nasdaq = 14000.0,
                dowJones = 35000.0,
                sp500Change = 25.0,
                nasdaqChange = 75.0,
                dowJonesChange = 150.0,
                sp500ChangePercent = 0.56,
                nasdaqChangePercent = 0.54,
                dowJonesChangePercent = 0.43
            )
        }
    }

    suspend fun getTrendingStocks(): List<USTrendingStock> {
        return try {
            val trending = mutableListOf<USTrendingStock>()
            
            // Fetch real-time data for trending US stocks
            val trendingSymbols = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NFLX", "NVDA")
            
            trendingSymbols.forEach { symbol ->
                try {
                    val stockData = getStockPrice(symbol, null)
                    if (stockData != null) {
                        trending.add(USTrendingStock(
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
            // Fallback to mock data
            listOf(
                USTrendingStock("AAPL", "Apple Inc.", 180.0, 2.5, 1.4, 85000000),
                USTrendingStock("GOOGL", "Alphabet Inc.", 140.0, -1.2, -0.85, 25000000),
                USTrendingStock("MSFT", "Microsoft Corp.", 350.0, 5.8, 1.68, 35000000),
                USTrendingStock("AMZN", "Amazon.com Inc.", 130.0, 3.2, 2.52, 45000000),
                USTrendingStock("TSLA", "Tesla Inc.", 240.0, -8.5, -3.42, 120000000),
                USTrendingStock("META", "Meta Platforms Inc.", 320.0, 12.5, 4.06, 25000000),
                USTrendingStock("NFLX", "Netflix Inc.", 450.0, 15.2, 3.49, 8000000),
                USTrendingStock("NVDA", "NVIDIA Corp.", 480.0, 25.8, 5.68, 60000000)
            )
        }
    }
} 