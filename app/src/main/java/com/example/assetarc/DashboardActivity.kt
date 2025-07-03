package com.example.assetarc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assetarc.ui.theme.AssetArcTheme
import androidx.compose.foundation.Canvas
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Add

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssetArcTheme {
                DashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var marketOverview by remember { mutableStateOf<MarketOverview?>(null) }
    var trendingAssets by remember { mutableStateOf<List<AssetData>>(emptyList()) }
    var watchlist by remember { mutableStateOf<List<AssetData>>(emptyList()) }
    var topGainers by remember { mutableStateOf<List<StockData>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val viewModel = remember { PortfolioViewModel.getInstance() }
    val portfolio by viewModel.portfolio.collectAsState()
    val loadingPortfolio by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRealTimeUpdatesEnabled by viewModel.isRealTimeUpdatesEnabled.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val stockService = remember { IndianStockService() }
    val usStockService = remember { USStockService() }
    val portfolioViewModel = remember { PortfolioViewModel.getInstance() }

    // Start real-time updates
    LaunchedEffect(Unit) {
        viewModel.loadPortfolio(context)
        viewModel.startRealTimeUpdates(context) // Start real-time updates
    }
    
    // Stop real-time updates when the screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRealTimeUpdates()
        }
    }

    // Load real-time market data
    LaunchedEffect(refreshTrigger) {
        try {
            loading = true
            
            // Load Indian market overview
            marketOverview = stockService.getMarketOverview()
            
            // Load trending assets (mix of Indian stocks, US stocks, and crypto)
            val trendingList = mutableListOf<AssetData>()
            
            // Add Indian stocks
            val indianTrending = stockService.getTrendingStocks().take(3)
            if (indianTrending.isEmpty()) {
                trendingList.add(AssetData(
                    symbol = "N/A",
                    name = "Live data unavailable",
                    price = 0.0,
                    change = 0.0,
                    changePercent = 0.0,
                    type = "indian_stock"
                ))
            } else {
                indianTrending.forEach { stock ->
                    trendingList.add(AssetData(
                        symbol = stock.symbol,
                        name = stock.name,
                        price = stock.price,
                        change = stock.change,
                        changePercent = stock.changePercent,
                        type = "indian_stock"
                    ))
                }
            }
            
            // Add US stocks
            val usTrending = usStockService.getTrendingStocks().take(2)
            usTrending.forEach { stock ->
                trendingList.add(AssetData(
                    symbol = stock.symbol,
                    name = stock.name,
                    price = stock.price,
                    change = stock.change,
                    changePercent = stock.changePercent,
                    type = "us_stock"
                ))
            }
            
            trendingAssets = trendingList
            
            // Load top gainers
            topGainers = stockService.getTopGainers().map { trendingStock ->
                StockData(
                    symbol = trendingStock.symbol,
                    name = trendingStock.name,
                    price = trendingStock.price,
                    change = trendingStock.change,
                    changePercent = trendingStock.changePercent,
                    volume = trendingStock.volume,
                    marketCap = 0.0,
                    high = trendingStock.price + 10,
                    low = trendingStock.price - 10,
                    open = trendingStock.price - trendingStock.change,
                    previousClose = trendingStock.price - trendingStock.change
                )
            }
            
            loading = false
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error loading market data", e)
            loading = false
        }
    }

    // Function to refresh all data
    fun refreshAllData() {
        viewModel.updatePrices(context)
        refreshTrigger++ // Trigger refresh
    }

    // Search functionality
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            searchResults = performGlobalSearch(searchQuery)
            showSearchResults = searchResults.isNotEmpty()
        } else {
            searchResults = emptyList()
            showSearchResults = false
        }
    }

    // Auto-refresh every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15000)
            refreshAllData()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Global Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search stocks, crypto, or assets...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF9CA3AF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF9CA3AF)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    )
                    
                    // Search Results Dropdown
                    if (showSearchResults && searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
                        ) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(searchResults) { result ->
                                    var isLoading by remember { mutableStateOf(false) }
                                    SearchResultItem(
                                        result = result,
                                        onClick = {
                                            val ctx = context
                                            when (result.type) {
                                                SearchResultType.INDIAN_STOCK -> {
                                                    isLoading = true
                                                    val symbol = result.symbol
                                                    coroutineScope.launch {
                                                        try {
                                                            val stockData = stockService.getStockPrice(symbol, ctx)
                                                            isLoading = false
                                                            if (stockData != null) {
                                                                val intent = Intent(ctx, AssetDetailActivity::class.java).apply {
                                                                    putExtra("symbol", stockData.symbol)
                                                                    putExtra("name", stockData.name)
                                                                    putExtra("price", stockData.price)
                                                                    putExtra("change", stockData.change)
                                                                    putExtra("changePercent", stockData.changePercent)
                                                                    putExtra("type", "indian_stock")
                                                                }
                                                                ctx.startActivity(intent)
                                                            } else {
                                                                Toast.makeText(ctx, "Failed to fetch stock data", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            isLoading = false
                                                            Toast.makeText(ctx, "Error fetching stock data", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                SearchResultType.US_STOCK -> {
                                                    isLoading = true
                                                    val symbol = result.symbol
                                                    coroutineScope.launch {
                                                        try {
                                                            val stockData = usStockService.getStockPrice(symbol, ctx)
                                                            isLoading = false
                                                            if (stockData != null) {
                                                                val intent = Intent(ctx, AssetDetailActivity::class.java).apply {
                                                                    putExtra("symbol", stockData.symbol)
                                                                    putExtra("name", stockData.name)
                                                                    putExtra("price", stockData.price)
                                                                    putExtra("change", stockData.change)
                                                                    putExtra("changePercent", stockData.changePercent)
                                                                    putExtra("type", "us_stock")
                                                                }
                                                                ctx.startActivity(intent)
                                                            } else {
                                                                Toast.makeText(ctx, "Failed to fetch US stock data", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            isLoading = false
                                                            Toast.makeText(ctx, "Error fetching US stock data", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                SearchResultType.CRYPTO -> {
                                                    isLoading = true
                                                    val symbol = result.symbol
                                                    coroutineScope.launch {
                                                        try {
                                                            val price = portfolioViewModel.fetchCoinGeckoPrice(symbol)
                                                            isLoading = false
                                                            if (price != null) {
                                                                val name = portfolioViewModel.getCryptoName(symbol)
                                                                val intent = Intent(ctx, AssetDetailActivity::class.java).apply {
                                                                    putExtra("symbol", symbol)
                                                                    putExtra("name", name)
                                                                    putExtra("price", price)
                                                                    putExtra("change", 0.0) // You can fetch 24h change if needed
                                                                    putExtra("changePercent", 0.0)
                                                                    putExtra("type", "crypto")
                                                                }
                                                                ctx.startActivity(intent)
                                                            } else {
                                                                Toast.makeText(ctx, "Failed to fetch crypto price", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            isLoading = false
                                                            Toast.makeText(ctx, "Error fetching crypto price", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                            searchQuery = ""
                                            showSearchResults = false
                                        }
                                    )
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    if (result != searchResults.last()) {
                                        Divider(color = Color(0xFF4B5563), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "AssetArc",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Your Investment Dashboard",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                        if (isRealTimeUpdatesEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = Color(0xFF10B981),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    "LIVE",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Manual refresh button
                        IconButton(
                            onClick = {
                                refreshAllData()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Prices",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Portfolio Summary
                    val summary = viewModel.getPortfolioSummary()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Portfolio Value",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                            Text(
                                "₹${String.format("%.2f", summary.totalValue)}",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Today's Change",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                            Text(
                                "${String.format("%.2f", summary.totalChangePercent)}%",
                                color = if (summary.totalChangePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Overview", "Portfolio", "Watchlist").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Card(
                        modifier = Modifier
                            .clickable {
                                try {
                                    selectedTab = index
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1F2937)
                        )
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            when (selectedTab) {
                0 -> OverviewTab(viewModel, topGainers, trendingAssets)
                1 -> PortfolioTab(viewModel)
                2 -> WatchlistTab(viewModel)
            }
        }
        
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
            ) {
                Text(errorMessage)
            }
        }

        // Floating Action Button for Chat Assistant
        FloatingActionButton(
            onClick = {
                context.startActivity(Intent(context, GeminiAssistantActivity::class.java))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF3B82F6)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Open Chat Assistant",
                tint = Color.White
            )
        }
    }
}

@Composable
fun OverviewTab(
    viewModel: PortfolioViewModel,
    topGainers: List<StockData>,
    trendingAssets: List<AssetData>
) {
    val context = LocalContext.current
    val portfolio by viewModel.portfolio.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            // Market Overview
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Market Overview",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MarketOverviewItem("NIFTY 50", "₹19,850.25", "+125.50", "+0.64%", true)
                    MarketOverviewItem("SENSEX", "₹66,150.75", "+425.25", "+0.65%", true)
                    MarketOverviewItem("BANK NIFTY", "₹44,250.50", "-125.75", "-0.28%", false)
                }
            }
        }
        item {
            // Top Gainers
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Top Gainers",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    topGainers.forEach { stock ->
                        StockListItem(
                            stock = stock,
                            onClick = {
                                val intent = Intent(context, AssetDetailActivity::class.java).apply {
                                    putExtra("symbol", stock.symbol)
                                    putExtra("name", stock.name)
                                    putExtra("price", stock.price)
                                    putExtra("change", stock.change)
                                    putExtra("changePercent", stock.changePercent)
                                    putExtra("type", "indian_stock")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
        item {
            // Trending Assets
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Trending",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    trendingAssets.forEach { asset ->
                        AssetListItem(
                            asset = asset,
                            onClick = {
                                val intent = Intent(context, AssetDetailActivity::class.java).apply {
                                    putExtra("symbol", asset.symbol)
                                    putExtra("name", asset.name)
                                    putExtra("price", asset.price)
                                    putExtra("change", asset.change)
                                    putExtra("changePercent", asset.changePercent)
                                    putExtra("type", when (asset.type) {
                                        "stock" -> "indian_stock"
                                        "crypto" -> "crypto"
                                        else -> "indian_stock"
                                    })
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioTab(viewModel: PortfolioViewModel) {
    val portfolio by viewModel.portfolio.collectAsState()
    val context = LocalContext.current
    
    if (portfolio.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = "Empty Portfolio",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Your portfolio is empty",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add assets to start tracking",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Test button for debugging
                Button(
                    onClick = {
                        viewModel.addAsset(
                            AssetType.IndianStock,
                            "RELIANCE",
                            10.0,
                            context
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Test Add RELIANCE", color = Color.White)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            items(portfolio) { item ->
                PortfolioItemCard(
                    item = item,
                    onClick = {
                        val intent = Intent(context, AssetDetailActivity::class.java).apply {
                            putExtra("symbol", item.symbol)
                            putExtra("name", item.name)
                            putExtra("price", item.price)
                            putExtra("change", item.change)
                            putExtra("changePercent", item.changePercent)
                            putExtra("type", when (item.type) {
                                is AssetType.IndianStock -> "indian_stock"
                                is AssetType.Stock -> "us_stock"
                                is AssetType.Crypto -> "crypto"
                            })
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun WatchlistTab(viewModel: PortfolioViewModel) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Watchlist",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    getMockWatchlist().forEach { asset ->
                        AssetListItem(
                            asset = asset,
                            onClick = {
                                val intent = Intent(context, AssetDetailActivity::class.java).apply {
                                    putExtra("symbol", asset.symbol)
                                    putExtra("name", asset.name)
                                    putExtra("price", asset.price)
                                    putExtra("change", asset.change)
                                    putExtra("changePercent", asset.changePercent)
                                    putExtra("type", when (asset.type) {
                                        "stock" -> "us_stock"
                                        "crypto" -> "crypto"
                                        else -> "us_stock"
                                    })
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketOverviewItem(name: String, price: String, change: String, changePercent: String, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Column(horizontalAlignment = Alignment.End) {
            Text(price, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "$change ($changePercent)",
                color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StockListItem(stock: StockData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stock.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stock.name, color = Color(0xFF9CA3AF), fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${stock.price}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${stock.change} (${stock.changePercent}%)",
                    color = if (stock.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AssetListItem(asset: AssetData, onClick: () -> Unit) {
    var price by remember { mutableStateOf(asset.price) }
    var change by remember { mutableStateOf(asset.change) }
    var changePercent by remember { mutableStateOf(asset.changePercent) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(asset.symbol, asset.type) {
        isLoading = true
        when (asset.type) {
            "indian_stock" -> {
                val stockService = IndianStockService()
                coroutineScope.launch {
                    try {
                        val stockData = stockService.getStockPrice(asset.symbol, context)
                        if (stockData != null) {
                            price = stockData.price
                            change = stockData.change
                            changePercent = stockData.changePercent
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
            "us_stock" -> {
                val stockService = USStockService()
                coroutineScope.launch {
                    try {
                        val stockData = stockService.getStockPrice(asset.symbol, context)
                        if (stockData != null) {
                            price = stockData.price
                            change = stockData.change
                            changePercent = stockData.changePercent
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
            "crypto" -> {
                val portfolioViewModel = PortfolioViewModel.getInstance()
                coroutineScope.launch {
                    try {
                        val fetchedPrice = portfolioViewModel.fetchCoinGeckoPrice(asset.symbol)
                        if (fetchedPrice != null) {
                            price = fetchedPrice
                            // Optionally fetch 24h change here
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(asset.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(asset.name, color = Color(0xFF9CA3AF), fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        when (asset.type) {
                            "crypto" -> "$${String.format("%.2f", price)}"
                            else -> "₹${String.format("%.2f", price)}"
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${String.format("%.2f", change)} (${String.format("%.2f", changePercent)}%)",
                        color = if (changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioItemCard(item: PortfolioItem, onClick: () -> Unit) {
    var price by remember { mutableStateOf(item.price) }
    var change by remember { mutableStateOf(item.change) }
    var changePercent by remember { mutableStateOf(item.changePercent) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(item.symbol, item.type) {
        isLoading = true
        when (item.type) {
            is AssetType.IndianStock -> {
                val stockService = IndianStockService()
                coroutineScope.launch {
                    try {
                        val stockData = stockService.getStockPrice(item.symbol, context)
                        if (stockData != null) {
                            price = stockData.price
                            change = stockData.change
                            changePercent = stockData.changePercent
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
            is AssetType.Stock -> {
                val stockService = USStockService()
                coroutineScope.launch {
                    try {
                        val stockData = stockService.getStockPrice(item.symbol, context)
                        if (stockData != null) {
                            price = stockData.price
                            change = stockData.change
                            changePercent = stockData.changePercent
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
            is AssetType.Crypto -> {
                val portfolioViewModel = PortfolioViewModel.getInstance()
                coroutineScope.launch {
                    try {
                        val fetchedPrice = portfolioViewModel.fetchCoinGeckoPrice(item.symbol)
                        if (fetchedPrice != null) {
                            price = fetchedPrice
                            // Optionally fetch 24h change here
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(item.name, color = Color(0xFF9CA3AF), fontSize = 14.sp)
                Text("Qty: ${item.quantity}", color = Color(0xFF6B7280), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        when (item.type) {
                            is AssetType.IndianStock -> "₹${String.format("%.2f", price)}"
                            is AssetType.Stock -> "$${String.format("%.2f", price)}"
                            is AssetType.Crypto -> "$${String.format("%.2f", price)}"
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${String.format("%.2f", changePercent)}%",
                        color = if (changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                    Text(
                        "Value: ${when (item.type) {
                            is AssetType.IndianStock -> "₹${String.format("%.2f", item.quantity * price)}"
                            is AssetType.Stock -> "$${String.format("%.2f", item.quantity * price)}"
                            is AssetType.Crypto -> "$${String.format("%.2f", item.quantity * price)}"
                        }}",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

data class AssetData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val type: String
)

fun getMockTopGainers(): List<StockData> {
    return listOf(
        StockData("RELIANCE", "Reliance Industries", 2450.75, 45.25, 1.88, 12500000L, 1650000.0, 2480.0, 2400.0, 2405.5, 2405.5),
        StockData("TCS", "Tata Consultancy", 3850.50, 75.30, 2.00, 8500000L, 1450000.0, 3900.0, 3775.2, 3775.2, 3775.2),
        StockData("HDFC", "HDFC Bank", 1650.25, 25.75, 1.59, 12000000L, 950000.0, 1675.0, 1624.5, 1624.5, 1624.5),
        StockData("INFY", "Infosys", 1450.80, 30.20, 2.13, 9800000L, 600000.0, 1475.0, 1420.6, 1420.6, 1420.6),
        StockData("ICICIBANK", "ICICI Bank", 950.45, 15.55, 1.66, 15000000L, 650000.0, 965.0, 934.9, 934.9, 934.9)
    )
}

fun getMockTrendingAssets(): List<AssetData> {
    return listOf(
        AssetData("BTC", "Bitcoin", 45000.0, 500.0, 1.12, "crypto"),
        AssetData("ETH", "Ethereum", 3000.0, -50.0, -1.64, "crypto"),
        AssetData("AAPL", "Apple Inc.", 180.0, 2.5, 1.41, "stock"),
        AssetData("GOOGL", "Alphabet Inc.", 140.0, -1.2, -0.85, "stock"),
        AssetData("RELIANCE", "Reliance Industries", 2450.75, 45.25, 1.88, "stock")
    )
}

fun getMockWatchlist(): List<AssetData> {
    return listOf(
        AssetData("TSLA", "Tesla Inc.", 250.0, -4.0, -1.57, "stock"),
        AssetData("MSFT", "Microsoft Corp.", 350.0, 5.0, 1.45, "stock"),
        AssetData("AMZN", "Amazon.com Inc.", 150.0, 3.2, 2.18, "stock"),
        AssetData("BNB", "Binance Coin", 400.0, 10.0, 2.56, "crypto"),
        AssetData("ADA", "Cardano", 0.5, 0.01, 2.04, "crypto")
    )
}

data class SearchResult(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val type: SearchResultType
)

enum class SearchResultType {
    INDIAN_STOCK,
    US_STOCK,
    CRYPTO
}

fun performGlobalSearch(query: String): List<SearchResult> {
    val results = mutableListOf<SearchResult>()
    val lowerQuery = query.lowercase()
    
    // Search Indian stocks
    IndianStockService.INDIAN_STOCKS.forEach { (symbol, name) ->
        if (symbol.lowercase().contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
            results.add(
                SearchResult(
                    symbol = symbol,
                    name = name,
                    price = 2450.75, // Mock price - in real app, fetch from API
                    change = 45.25,
                    changePercent = 1.88,
                    type = SearchResultType.INDIAN_STOCK
                )
            )
        }
    }
    
    // Additional important Indian stocks not in the main list
    val additionalIndianStocks = mapOf(
        "LIC" to "Life Insurance Corporation of India",
        "ADANIPORTS" to "Adani Ports & SEZ",
        "ADANIPOWER" to "Adani Power",
        "ADANITRANS" to "Adani Transmission",
        "ADANIGREEN" to "Adani Green Energy",
        "ADANITOTAL" to "Adani Total Gas",
        "ADANIWILMAR" to "Adani Wilmar",
        "HDFC" to "Housing Development Finance Corporation",
        "HDFCAMC" to "HDFC Asset Management Company",
        "HINDCOPPER" to "Hindustan Copper",
        "HINDPETRO" to "Hindustan Petroleum",
        "HINDZINC" to "Hindustan Zinc",
        "IDEA" to "Vodafone Idea",
        "INDIGO" to "InterGlobe Aviation",
        "IRCTC" to "Indian Railway Catering & Tourism",
        "JINDALSTEL" to "Jindal Steel & Power",
        "JSWENERGY" to "JSW Energy",
        "LUPIN" to "Lupin",
        "MARICO" to "Marico",
        "MCDOWELL-N" to "United Spirits",
        "MUTHOOTFIN" to "Muthoot Finance",
        "NMDC" to "NMDC",
        "PEL" to "Piramal Enterprises",
        "PERSISTENT" to "Persistent Systems",
        "PIDILITIND" to "Pidilite Industries",
        "PNB" to "Punjab National Bank",
        "PVR" to "PVR Cinemas",
        "RBLBANK" to "RBL Bank",
        "SAIL" to "Steel Authority of India",
        "SIEMENS" to "Siemens",
        "TATACOMM" to "Tata Communications",
        "TATAPOWER" to "Tata Power",
        "TORNTPHARM" to "Torrent Pharmaceuticals",
        "VOLTAS" to "Voltas",
        "ZEEL" to "Zee Entertainment Enterprises"
    )
    
    additionalIndianStocks.forEach { (symbol, name) ->
        if (symbol.lowercase().contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
            // Check if not already added from main list
            if (!results.any { it.symbol == symbol }) {
                results.add(
                    SearchResult(
                        symbol = symbol,
                        name = name,
                        price = 2450.75, // Mock price
                        change = 45.25,
                        changePercent = 1.88,
                        type = SearchResultType.INDIAN_STOCK
                    )
                )
            }
        }
    }
    
    // Search US stocks
    val usStocks = mapOf(
        "AAPL" to "Apple Inc.",
        "GOOGL" to "Alphabet Inc.",
        "MSFT" to "Microsoft Corp.",
        "AMZN" to "Amazon.com Inc.",
        "TSLA" to "Tesla Inc.",
        "META" to "Meta Platforms Inc.",
        "NVDA" to "NVIDIA Corp.",
        "NFLX" to "Netflix Inc.",
        "ADBE" to "Adobe Inc.",
        "CRM" to "Salesforce Inc.",
        "PYPL" to "PayPal Holdings Inc.",
        "INTC" to "Intel Corp.",
        "AMD" to "Advanced Micro Devices Inc.",
        "ORCL" to "Oracle Corp.",
        "IBM" to "International Business Machines Corp.",
        "CSCO" to "Cisco Systems Inc.",
        "QCOM" to "Qualcomm Inc.",
        "TXN" to "Texas Instruments Inc.",
        "AVGO" to "Broadcom Inc.",
        "MU" to "Micron Technology Inc."
    )
    
    usStocks.forEach { (symbol, name) ->
        if (symbol.lowercase().contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
            results.add(
                SearchResult(
                    symbol = symbol,
                    name = name,
                    price = 180.0, // Mock price
                    change = 2.5,
                    changePercent = 1.41,
                    type = SearchResultType.US_STOCK
                )
            )
        }
    }
    
    // Search Crypto
    val cryptos = mapOf(
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
    
    cryptos.forEach { (symbol, name) ->
        if (symbol.lowercase().contains(lowerQuery) || name.lowercase().contains(lowerQuery)) {
            results.add(
                SearchResult(
                    symbol = symbol,
                    name = name,
                    price = 45000.0, // Mock price
                    change = 500.0,
                    changePercent = 1.12,
                    type = SearchResultType.CRYPTO
                )
            )
        }
    }
    
    // Return top 10 results
    return results.take(10)
}

@Composable
fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.symbol,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Asset type indicator
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result.type) {
                            SearchResultType.INDIAN_STOCK -> Color(0xFF10B981)
                            SearchResultType.US_STOCK -> Color(0xFF3B82F6)
                            SearchResultType.CRYPTO -> Color(0xFFF59E0B)
                        }
                    )
                ) {
                    Text(
                        when (result.type) {
                            SearchResultType.INDIAN_STOCK -> "IN"
                            SearchResultType.US_STOCK -> "US"
                            SearchResultType.CRYPTO -> "CR"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                result.name,
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when (result.type) {
                    SearchResultType.INDIAN_STOCK -> "₹${result.price}"
                    SearchResultType.US_STOCK -> "$${result.price}"
                    SearchResultType.CRYPTO -> "$${result.price}"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${result.change} (${result.changePercent}%)",
                color = if (result.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                fontSize = 14.sp
            )
        }
    }
} 