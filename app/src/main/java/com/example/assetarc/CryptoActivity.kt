@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.assetarc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import android.widget.Toast
import kotlinx.coroutines.launch
import android.util.Log

// Mock data classes

data class CryptoTrending(val symbol: String, val name: String, val price: Double, val change: Double, val changePercent: Double)

class CryptoActivity : ComponentActivity() {
    private val portfolioViewModel: PortfolioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssetArcTheme {
                CryptoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var trending by remember { mutableStateOf<List<CryptoTrending>>(emptyList()) }
    var topGainers by remember { mutableStateOf<List<CryptoTrending>>(emptyList()) }
    var topLosers by remember { mutableStateOf<List<CryptoTrending>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val portfolioViewModel = remember { PortfolioViewModel.getInstance() }
    val portfolio by portfolioViewModel.portfolio.collectAsState()
    val loadingPortfolio by portfolioViewModel.loading.collectAsState()
    val error by portfolioViewModel.error.collectAsState()

    val trendingCryptos = remember { listOf(
        CryptoTrending("BTC", "Bitcoin", 45000.0, 500.0, 1.1),
        CryptoTrending("ETH", "Ethereum", 2800.0, -50.0, -1.75),
        CryptoTrending("BNB", "Binance Coin", 320.0, 15.0, 4.92),
        CryptoTrending("ADA", "Cardano", 0.45, 0.02, 4.65),
        CryptoTrending("SOL", "Solana", 95.0, 8.0, 9.20),
        CryptoTrending("XRP", "Ripple", 0.55, -0.02, -3.51),
        CryptoTrending("DOT", "Polkadot", 7.2, 0.3, 4.35),
        CryptoTrending("DOGE", "Dogecoin", 0.08, 0.005, 6.67),
        CryptoTrending("AVAX", "Avalanche", 35.0, 2.5, 7.69),
        CryptoTrending("MATIC", "Polygon", 0.85, 0.05, 6.25)
    ) }

    val topGainersList = remember { listOf(
        CryptoTrending("SOL", "Solana", 95.0, 8.0, 9.20),
        CryptoTrending("DOGE", "Dogecoin", 0.08, 0.005, 6.67),
        CryptoTrending("AVAX", "Avalanche", 35.0, 2.5, 7.69),
        CryptoTrending("MATIC", "Polygon", 0.85, 0.05, 6.25),
        CryptoTrending("ADA", "Cardano", 0.45, 0.02, 4.65)
    ) }

    val topLosersList = remember { listOf(
        CryptoTrending("XRP", "Ripple", 0.55, -0.02, -3.51),
        CryptoTrending("ETH", "Ethereum", 2800.0, -50.0, -1.75),
        CryptoTrending("BTC", "Bitcoin", 45000.0, 500.0, 1.1),
        CryptoTrending("BNB", "Binance Coin", 320.0, 15.0, 4.92),
        CryptoTrending("DOT", "Polkadot", 7.2, 0.3, 4.35)
    ) }

    // Start real-time price updates
    LaunchedEffect(Unit) {
        portfolioViewModel.loadPortfolio(context)
        portfolioViewModel.startRealTimeUpdates(context)
    }
    
    // Stop real-time updates when the screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            portfolioViewModel.stopRealTimeUpdates()
        }
    }

    // Load mock data
    LaunchedEffect(refreshTrigger) {
        trending = getCryptoTrending()
        topGainers = getCryptoTopGainers()
        topLosers = getCryptoTopLosers()
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Portfolio preview
            if (portfolio.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Your Crypto Portfolio", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        portfolio.forEach { crypto ->
                            Text("${crypto.symbol} - Qty: 1 @ $${crypto.price}", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF59E0B),
                                Color(0xFFFBBF24),
                                Color(0xFFFDE68A)
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
                                "Crypto",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Track cryptocurrencies",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Refresh button
                            IconButton(
                                onClick = {
                                    refreshTrigger++ // Trigger refresh
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                            
                            // Back button
                            IconButton(
                                onClick = { (context as? ComponentActivity)?.finish() },
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
                    }
                }
            }
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Trending", "Top Gainers", "Top Losers").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1F2937)
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
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> CryptoTrendingList(trending)
                    1 -> CryptoTrendingList(topGainers)
                    2 -> CryptoTrendingList(topLosers)
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Crypto")
                }
            }
        }
        if (showAddDialog) {
            AddCryptoDialog(trending, onAdd = { symbol, qty ->
                val crypto = trending.find { it.symbol.equals(symbol, ignoreCase = true) }
                if (crypto != null) {
                    portfolioViewModel.addAsset(AssetType.Crypto, crypto.symbol, qty.toDouble(), context)
                } else {
                    Toast.makeText(context, "Crypto not found", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }, onDismiss = { showAddDialog = false })
        }
    }
}

@Composable
fun CryptoTrendingList(stocks: List<CryptoTrending>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        items(stocks) { stock ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF374151))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (stock.change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Crypto",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stock.symbol,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stock.name,
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$${String.format("%.2f", stock.price)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (stock.change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = if (stock.change >= 0) "Up" else "Down",
                                tint = if (stock.change >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = if (stock.change >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                                        append(String.format("%.2f", stock.change))
                                    }
                                    withStyle(SpanStyle(color = if (stock.change >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                                        append(" (${String.format("%.2f", stock.changePercent)}%)")
                                    }
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCryptoDialog(trending: List<CryptoTrending>, onAdd: (String, Double) -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSymbol by remember { mutableStateOf(trending.firstOrNull()?.symbol ?: "") }
    var quantity by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Crypto", color = Color.White) },
        text = {
            Column {
                  ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedSymbol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Symbol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFFF59E0B),
                            focusedLabelColor = Color(0xFFF59E0B),
                            unfocusedLabelColor = Color(0xFF9CA3AF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        trending.forEach { crypto ->
                            DropdownMenuItem(
                                text = { Text(crypto.symbol) },
                                onClick = {
                                    selectedSymbol = crypto.symbol
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFFF59E0B),
                        focusedLabelColor = Color(0xFFF59E0B),
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
                    if (selectedSymbol.isNotBlank()) onAdd(selectedSymbol, qty)
                    else {
                        // Show error
                        Toast.makeText(context, "Please select a symbol", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Add", color = Color(0xFFF59E0B)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF9CA3AF)) }
        },
        containerColor = Color(0xFF1F2937),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Crypto data functions
fun getCryptoTrending(): List<CryptoTrending> {
    return listOf(
        CryptoTrending("BTC", "Bitcoin", 45000.0, 500.0, 1.12),
        CryptoTrending("ETH", "Ethereum", 3000.0, -50.0, -1.64),
        CryptoTrending("BNB", "Binance Coin", 400.0, 10.0, 2.56),
        CryptoTrending("ADA", "Cardano", 0.5, 0.01, 2.04),
        CryptoTrending("SOL", "Solana", 100.0, 5.0, 5.26),
        CryptoTrending("XRP", "Ripple", 0.8, 0.02, 2.56),
        CryptoTrending("DOT", "Polkadot", 7.2, 0.3, 4.35)
    )
}

fun getCryptoTopGainers(): List<CryptoTrending> {
    return listOf(
        CryptoTrending("SOL", "Solana", 100.0, 5.0, 5.26),
        CryptoTrending("DOT", "Polkadot", 7.2, 0.3, 4.35),
        CryptoTrending("BNB", "Binance Coin", 400.0, 10.0, 2.56),
        CryptoTrending("ADA", "Cardano", 0.5, 0.01, 2.04),
        CryptoTrending("BTC", "Bitcoin", 45000.0, 500.0, 1.12)
    )
}

fun getCryptoTopLosers(): List<CryptoTrending> {
    return listOf(
        CryptoTrending("ETH", "Ethereum", 3000.0, -50.0, -1.64),
        CryptoTrending("XRP", "Ripple", 0.8, 0.02, 2.56),
        CryptoTrending("BTC", "Bitcoin", 45000.0, 500.0, 1.12),
        CryptoTrending("BNB", "Binance Coin", 400.0, 10.0, 2.56),
        CryptoTrending("ADA", "Cardano", 0.5, 0.01, 2.04)
    )
} 