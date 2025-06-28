package com.example.assetarc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assetarc.ui.theme.AssetArcTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState

class IndianStocksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssetArcTheme {
                IndianStocksScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndianStocksScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var marketOverview by remember { mutableStateOf<MarketOverview?>(null) }
    var topGainers by remember { mutableStateOf<List<TrendingStock>>(emptyList()) }
    var topLosers by remember { mutableStateOf<List<TrendingStock>>(emptyList()) }
    var trendingStocks by remember { mutableStateOf<List<TrendingStock>>(emptyList()) }
    var dataLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stockService = remember { IndianStockService() }
    val viewModel = remember { PortfolioViewModel.getInstance() }
    val portfolio by viewModel.portfolio.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Start real-time price updates
    LaunchedEffect(Unit) {
        viewModel.loadPortfolio(context)
        viewModel.startRealTimeUpdates(context)
    }
    
    // Stop real-time updates when the screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRealTimeUpdates()
        }
    }

    // Load data on first launch
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                marketOverview = stockService.getMarketOverview()
                topGainers = stockService.getTopGainers()
                topLosers = stockService.getTopLosers()
                trendingStocks = stockService.getTrendingStocks()
                dataLoading = false
            } catch (e: Exception) {
                Log.e("IndianStocksActivity", "Error loading data", e)
                dataLoading = false
            }
        }
    }

    // Search functionality
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            coroutineScope.launch {
                searchResults = stockService.searchStocks(searchQuery)
                showSearchResults = searchResults.isNotEmpty()
            }
        } else {
            searchResults = emptyList()
            showSearchResults = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF059669),
                                Color(0xFF10B981),
                                Color(0xFF34D399)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Indian Markets",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Track NSE & BSE stocks",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        
                        IconButton(
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Portfolio Preview
                    val indianStocksInPortfolio = portfolio.filter { it.type is AssetType.IndianStock }
                    if (indianStocksInPortfolio.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Your Indian Stocks",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                indianStocksInPortfolio.take(3).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            item.symbol,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "₹${String.format("%.2f", item.price)}",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    if (item != indianStocksInPortfolio.take(3).last()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                
                                if (indianStocksInPortfolio.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "+${indianStocksInPortfolio.size - 3} more",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Market overview card
                    marketOverview?.let { overview ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    "Market Overview",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MarketIndexCard(
                                        "SENSEX",
                                        overview.sensex,
                                        overview.sensexChange,
                                        overview.sensexChangePercent
                                    )
                                    MarketIndexCard(
                                        "NIFTY 50",
                                        overview.nifty,
                                        overview.niftyChange,
                                        overview.niftyChangePercent
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Search bar
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Indian stocks...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF10B981),
                        focusedLabelColor = Color(0xFF10B981),
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
                
                // Enhanced Search results dropdown
                if (showSearchResults && searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(searchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = result
                                            showSearchResults = false
                                            // Navigate to stock detail
                                            val intent = Intent(context, AssetDetailActivity::class.java).apply {
                                                putExtra("symbol", result)
                                                putExtra("name", IndianStockService.INDIAN_STOCKS[result] ?: result)
                                                putExtra("price", 2450.75) // Mock price
                                                putExtra("change", 45.25)
                                                putExtra("changePercent", 1.88)
                                                putExtra("type", "indian_stock")
                                            }
                                            context.startActivity(intent)
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            result,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            IndianStockService.INDIAN_STOCKS[result] ?: "",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Card(
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text(
                                            "IN",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (result != searchResults.last()) {
                                    Divider(color = Color(0xFF374151), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
            
            // Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Trending", "Top Gainers", "Top Losers", "Market").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Card(
                        modifier = Modifier
                            .clickable { selectedTab = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF10B981) else Color(0xFF1F2937)
                        )
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(300)
                )
            ) {
                TrendingStocksContent(trendingStocks)
            }
            
            AnimatedVisibility(
                visible = selectedTab == 1,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(300)
                )
            ) {
                TopGainersContent(topGainers)
            }
            
            AnimatedVisibility(
                visible = selectedTab == 2,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(300)
                )
            ) {
                TopLosersContent(topLosers)
            }
            
            AnimatedVisibility(
                visible = selectedTab == 3,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(300)
                )
            ) {
                MarketContent(marketOverview)
            }
        }
        
        if (dataLoading) {
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
                        CircularProgressIndicator(color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading market data...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketIndexCard(name: String, value: Double, change: Double, changePercent: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            name,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            String.format("%.2f", value),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = if (change >= 0) "Up" else "Down",
                tint = if (change >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (change >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                        append(String.format("%.2f", change))
                    }
                    withStyle(SpanStyle(color = if (change >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                        append(" (${String.format("%.2f", changePercent)}%)")
                    }
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TrendingStocksContent(stocks: List<TrendingStock>) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "Trending Stocks",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        items(stocks) { stock ->
            StockListItem(
                stock = stock,
                onClick = {
                    // Navigate to AssetDetailActivity
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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TopGainersContent(stocks: List<TrendingStock>) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "Top Gainers",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        items(stocks) { stock ->
            StockListItem(
                stock = stock,
                onClick = {
                    // Navigate to AssetDetailActivity
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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TopLosersContent(stocks: List<TrendingStock>) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "Top Losers",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        items(stocks) { stock ->
            StockListItem(
                stock = stock,
                onClick = {
                    // Navigate to AssetDetailActivity
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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MarketContent(overview: MarketOverview?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "Market Analysis",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Market Sentiment",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    overview?.let { market ->
                        MarketSentimentItem("SENSEX", market.sensex, market.sensexChangePercent, "Large Cap")
                        MarketSentimentItem("NIFTY 50", market.nifty, market.niftyChangePercent, "Broad Market")
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Trading Statistics",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TradingStatItem("Advancing Stocks", "1,245", Color(0xFF10B981))
                    TradingStatItem("Declining Stocks", "856", Color(0xFFEF4444))
                    TradingStatItem("Unchanged", "234", Color(0xFF9CA3AF))
                }
            }
        }
    }
}

@Composable
fun StockListItem(stock: TrendingStock, onClick: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel = viewModel<PortfolioViewModel>()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add to Portfolio",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    
    if (showAddDialog) {
        AddIndianStockDialog(
            stock = stock,
            onAdd = { quantity ->
                viewModel.addAsset(AssetType.IndianStock, stock.symbol, quantity, context)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AddIndianStockDialog(stock: TrendingStock, onAdd: (Double) -> Unit, onDismiss: () -> Unit) {
    var quantity by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${stock.symbol} to Portfolio", color = Color.White) },
        text = {
            Column {
                Text(
                    "Current Price: ₹${String.format("%.2f", stock.price)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF10B981),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 1.0
                    if (qty > 0) onAdd(qty)
                }
            ) { Text("Add", color = Color(0xFF10B981)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF9CA3AF)) }
        },
        containerColor = Color(0xFF1F2937),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun MarketSentimentItem(name: String, value: Double, changePercent: Double, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                String.format("%.2f", value),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                        append(String.format("%.2f", changePercent))
                        append("%")
                    }
                },
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TradingStatItem(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
} 