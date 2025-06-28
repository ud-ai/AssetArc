package com.example.assetarc

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assetarc.ui.theme.AssetArcTheme

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
    val viewModel: PortfolioViewModel = viewModel()
    val portfolio by viewModel.portfolio.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    
    // Update prices periodically
    LaunchedEffect(Unit) {
        viewModel.updatePrices(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    Text(
                        "Your Investment Dashboard",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                    
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
                            .clickable { selectedTab = index },
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
                0 -> OverviewTab(viewModel)
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
    }
}

@Composable
fun OverviewTab(viewModel: PortfolioViewModel) {
    val context = LocalContext.current
    
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
                    
                    getMockTopGainers().forEach { stock ->
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
                    
                    getMockTrendingAssets().forEach { asset ->
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
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
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
            .clickable { onClick() },
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
            Column {
                Text(asset.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(asset.name, color = Color(0xFF9CA3AF), fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when (asset.type) {
                        "stock" -> "₹${asset.price}"
                        "crypto" -> "$${asset.price}"
                        else -> "₹${asset.price}"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${asset.change} (${asset.changePercent}%)",
                    color = if (asset.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PortfolioItemCard(item: PortfolioItem, onClick: () -> Unit) {
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
            Column {
                Text(item.symbol, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(item.name, color = Color(0xFF9CA3AF), fontSize = 14.sp)
                Text("Qty: ${item.quantity}", color = Color(0xFF6B7280), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when (item.type) {
                        is AssetType.IndianStock -> "₹${String.format("%.2f", item.price)}"
                        is AssetType.Stock -> "$${String.format("%.2f", item.price)}"
                        is AssetType.Crypto -> "$${String.format("%.2f", item.price)}"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.2f", item.changePercent)}%",
                    color = if (item.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 14.sp
                )
                Text(
                    "Value: ${when (item.type) {
                        is AssetType.IndianStock -> "₹${String.format("%.2f", item.quantity * item.price)}"
                        is AssetType.Stock -> "$${String.format("%.2f", item.quantity * item.price)}"
                        is AssetType.Crypto -> "$${String.format("%.2f", item.quantity * item.price)}"
                    }}",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
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