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
import kotlinx.coroutines.DisposableHandle
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Mock data classes

// USTrendingStock is now defined in USStockService.kt

class USStocksActivity : ComponentActivity() {
    private val portfolioViewModel: PortfolioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssetArcTheme {
                USStocksScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun USStocksScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var trending by remember { mutableStateOf<List<USTrendingStock>>(emptyList()) }
    var topGainers by remember { mutableStateOf<List<USTrendingStock>>(emptyList()) }
    var topLosers by remember { mutableStateOf<List<USTrendingStock>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val portfolioViewModel = remember { PortfolioViewModel.getInstance() }
    val portfolio by portfolioViewModel.portfolio.collectAsState()
    val loadingPortfolio by portfolioViewModel.loading.collectAsState()
    val error by portfolioViewModel.error.collectAsState()
    
    val stockService = remember { USStockService() }

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

    // Load real-time data
    LaunchedEffect(refreshTrigger) {
        try {
            trending = stockService.getTrendingStocks()
            topGainers = stockService.getTopGainers()
            topLosers = stockService.getTopLosers()
            loading = false
        } catch (e: Exception) {
            Log.e("USStocksActivity", "Error loading data", e)
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A),
                                Color(0xFF3B82F6),
                                Color(0xFF60A5FA)
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
                                "US Stocks",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Track US equities",
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
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Portfolio Preview
                    val usStocksInPortfolio = portfolio.filter { it.type is AssetType.Stock }
                    if (usStocksInPortfolio.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Your US Stocks",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                usStocksInPortfolio.take(3).forEach { item ->
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
                                            "$${String.format("%.2f", item.price)}",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    if (item != usStocksInPortfolio.take(3).last()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                
                                if (usStocksInPortfolio.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "+${usStocksInPortfolio.size - 3} more",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Market overview card
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
                            containerColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1F2937)
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
                    0 -> TrendingStocksList(trending)
                    1 -> TrendingStocksList(topGainers)
                    2 -> TrendingStocksList(topLosers)
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add US Stock")
                }
            }
        }
        if (showAddDialog) {
            AddUSStockDialog(trending, onAdd = { symbol, qty ->
                val stock = trending.find { it.symbol.equals(symbol, ignoreCase = true) }
                if (stock != null) {
                    portfolioViewModel.addAsset(AssetType.Stock, stock.symbol, qty, context)
                }
                showAddDialog = false
            }, onDismiss = { showAddDialog = false })
        }
    }
}

@Composable
fun TrendingStocksList(stocks: List<USTrendingStock>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        items(stocks) { stock ->
            var price by remember { mutableStateOf(stock.price) }
            var change by remember { mutableStateOf(stock.change) }
            var changePercent by remember { mutableStateOf(stock.changePercent) }
            var isLoading by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(stock.symbol) {
                isLoading = true
                val stockService = USStockService()
                coroutineScope.launch {
                    try {
                        val stockData = stockService.getStockPrice(stock.symbol, context)
                        if (stockData != null) {
                            price = stockData.price
                            change = stockData.change
                            changePercent = stockData.changePercent
                        }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            }

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
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Stock",
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
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("$${String.format("%.2f", price)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun AddUSStockDialog(trending: List<USTrendingStock>, onAdd: (String, Double) -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSymbol by remember { mutableStateOf(trending.firstOrNull()?.symbol ?: "") }
    var quantity by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add US Stock", color = Color.White) },
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
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFF3B82F6),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF9CA3AF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        trending.forEach { stock ->
                            DropdownMenuItem(
                                text = { Text(stock.symbol) },
                                onClick = {
                                    selectedSymbol = stock.symbol
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
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF3B82F6),
                        focusedLabelColor = Color(0xFF3B82F6),
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
            ) { Text("Add", color = Color(0xFF3B82F6)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF9CA3AF)) }
        },
        containerColor = Color(0xFF1F2937),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
} 