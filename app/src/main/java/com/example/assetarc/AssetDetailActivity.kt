package com.example.assetarc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart as MPLineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.WindowManager

class AssetDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContent {
            AssetDetailScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val viewModel = remember { PortfolioViewModel.getInstance() }
    val portfolio by viewModel.portfolio.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Get asset data from intent extras
    val asset = remember {
        val intent = context.findActivity()?.intent
        val symbol = intent?.getStringExtra("symbol") ?: "RELIANCE"
        val name = intent?.getStringExtra("name") ?: "Reliance Industries Ltd"
        val price = intent?.getDoubleExtra("price", 2450.75) ?: 2450.75
        val change = intent?.getDoubleExtra("change", 45.25) ?: 45.25
        val changePercent = intent?.getDoubleExtra("changePercent", 1.88) ?: 1.88
        val typeString = intent?.getStringExtra("type") ?: "indian_stock"
        
        val type = when (typeString) {
            "indian_stock" -> AssetType.IndianStock
            "us_stock" -> AssetType.Stock
            "crypto" -> AssetType.Crypto
            else -> AssetType.IndianStock
        }
        
        PortfolioItem(
            type = type,
            symbol = symbol,
            name = name,
            quantity = 0.0,
            price = price,
            change = change,
            changePercent = changePercent
        )
    }
    
    val isInPortfolio = portfolio.any { it.symbol == asset.symbol && it.type == asset.type }
    val portfolioItem = portfolio.find { it.symbol == asset.symbol && it.type == asset.type }

    val stockService = remember { IndianStockService() }
    var days by remember { mutableStateOf(7) }
    var chartData by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var chartLoading by remember { mutableStateOf(false) }
    var chartError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch chart data when asset.symbol or days changes
    LaunchedEffect(asset.symbol, days) {
        chartLoading = true
        chartError = null
        chartData = emptyList()
        try {
            chartData = stockService.getHistoricalPrices(asset.symbol, days)
            if (chartData.isEmpty()) {
                chartError = "No chart data available."
            }
        } catch (e: Exception) {
            chartError = "Failed to load chart data. Please try again."
        }
        chartLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E21))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Header with gradient background
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
                            IconButton(
                                onClick = { 
                                    context.findActivity()?.finish() 
                                },
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
                            Row {
                                IconButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add to Portfolio",
                                        tint = Color.White
                                    )
                                }
                                if (isInPortfolio) {
                                    IconButton(
                                        onClick = { showRemoveDialog = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove from Portfolio",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            asset.symbol ?: "-",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            asset.name ?: "-",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    when (asset.type) {
                                        is AssetType.IndianStock -> "₹${String.format("%.2f", asset.price ?: 0.0)}"
                                        is AssetType.Stock -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                                        is AssetType.Crypto -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                                        else -> "-"
                                    },
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if ((asset.change ?: 0.0) >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = "Trend",
                                        tint = if ((asset.change ?: 0.0) >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(color = if ((asset.change ?: 0.0) >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                                                append(String.format("%.2f", asset.change ?: 0.0))
                                            }
                                            withStyle(SpanStyle(color = if ((asset.change ?: 0.0) >= 0) Color(0xFF10B981) else Color(0xFFEF4444))) {
                                                append(" (${String.format("%.2f", asset.changePercent ?: 0.0)}%)")
                                            }
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (isInPortfolio && portfolioItem != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "In Portfolio",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Qty: ${portfolioItem.quantity ?: 0.0}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Value: ${when (asset.type) {
                                            is AssetType.IndianStock -> "₹${String.format("%.2f", (portfolioItem.quantity ?: 0.0) * (asset.price ?: 0.0))}"
                                            is AssetType.Stock -> "$${String.format("%.2f", (portfolioItem.quantity ?: 0.0) * (asset.price ?: 0.0))}"
                                            is AssetType.Crypto -> "$${String.format("%.2f", (portfolioItem.quantity ?: 0.0) * (asset.price ?: 0.0))}"
                                            else -> "-"
                                        }}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Tab Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Overview", "Chart", "News", "Analysis").forEachIndexed { index, title ->
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
            }

            item {
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
                    OverviewContent(asset)
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
                    ChartContent(asset)
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
                    NewsContent(asset)
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
                    AnalysisContent(asset)
                }
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
        
        if (showAddDialog) {
            AddAssetDialog(
                asset = asset,
                onAdd = { quantity ->
                    viewModel.addAsset(asset.type, asset.symbol, quantity, context)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
        
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text("Remove from Portfolio", color = Color.White) },
                text = { Text("Are you sure you want to remove ${asset.symbol} from your portfolio?", color = Color.White) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeAsset(asset.symbol, asset.type, context)
                            showRemoveDialog = false
                        }
                    ) { Text("Remove", color = Color(0xFFEF4444)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel", color = Color(0xFF9CA3AF)) }
                },
                containerColor = Color(0xFF1F2937),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
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
fun OverviewContent(asset: PortfolioItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Key Statistics",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatRow("Market Cap", "₹1,650,000 Cr")
                StatRow("Volume", "12.5M")
                StatRow("52W High", "₹2,480.00")
                StatRow("52W Low", "₹2,000.00")
                StatRow("P/E Ratio", "18.5")
                StatRow("Dividend Yield", "0.8%")
            }
        }
        ChartContent(asset)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Company Info",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Reliance Industries Limited is India's largest private sector company by market capitalization and revenue. The company operates in multiple sectors including energy, petrochemicals, natural gas, retail, telecommunications, mass media, and textiles.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ChartContent(asset: PortfolioItem) {
    val context = LocalContext.current
    var chartData by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedRange by remember { mutableStateOf("1M") }
    val coroutineScope = rememberCoroutineScope()
    val stockService = remember { IndianStockService() }

    fun getRangeParams(range: String): Pair<String, Int> = when (range) {
        "1D" -> "1d" to 2
        "1W" -> "5d" to 7
        "1M" -> "1mo" to 30
        "1Y" -> "1y" to 365
        else -> "1mo" to 30
    }

    fun fetchYahooChart(symbol: String, isIndian: Boolean, days: Int) {
        coroutineScope.launch {
            try {
                val result = stockService.getHistoricalPrices(symbol, days)
                if (result.isNotEmpty()) {
                    chartData = result
                    error = null
                } else {
                    error = "No chart data available from Yahoo Finance."
                }
            } catch (e: Exception) {
                error = "Failed to load chart data from Yahoo Finance."
            }
            loading = false
        }
    }

    fun fetchChartData(symbol: String, isIndian: Boolean, range: String) {
        coroutineScope.launch {
            loading = true
            error = null
            chartData = emptyList()
            val (interval, days) = getRangeParams(range)
            val apiKey = "YS4SA0VVGZ8ZQW6MR" // TODO: Replace with your real API key
            val fullSymbol = if (isIndian) "$symbol.NSE" else symbol
            val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=$fullSymbol&apikey=$apiKey"
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    if (json.has("Time Series (Daily)")) {
                        val timeSeries = json.getJSONObject("Time Series (Daily)")
                        val points = mutableListOf<Pair<Long, Double>>()
                        val dates = timeSeries.keys().asSequence().toList().sorted().takeLast(days)
                        for (date in dates) {
                            val dayData = timeSeries.getJSONObject(date)
                            val close = dayData.getString("4. close").toDoubleOrNull()
                            if (close != null) {
                                points.add(System.currentTimeMillis() to close)
                            }
                        }
                        if (points.isNotEmpty()) {
                            chartData = points
                        } else {
                            error = "No chart data available."
                        }
                    } else if (json.has("Note")) {
                        error = "API rate limit reached. Trying Yahoo Finance..."
                        fetchYahooChart(symbol, isIndian, days)
                        return@launch
                    } else {
                        error = "No chart data available."
                        fetchYahooChart(symbol, isIndian, days)
                        return@launch
                    }
                } else {
                    error = "Failed to load chart data. Trying Yahoo Finance..."
                    fetchYahooChart(symbol, isIndian, days)
                    return@launch
                }
            } catch (e: Exception) {
                error = "Failed to load chart data. Trying Yahoo Finance..."
                fetchYahooChart(symbol, isIndian, days)
                return@launch
            }
            loading = false
        }
    }

    LaunchedEffect(asset.symbol, selectedRange) {
        val isIndian = asset.type is AssetType.IndianStock
        fetchChartData(asset.symbol, isIndian, selectedRange)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Time range selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            listOf("1D", "1W", "1M", "1Y").forEach { range ->
                val selected = selectedRange == range
                Button(
                    onClick = { selectedRange = range },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF3B82F6) else Color(0xFF1F2937),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(range)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Price Chart (${selectedRange})",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (chartData.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val minPrice = chartData.minOf { it.second }
                        val maxPrice = chartData.maxOf { it.second }
                        val priceRange = if ((maxPrice - minPrice) == 0.0) 1.0 else (maxPrice - minPrice)
                        val gridColor = Color(0xFF374151)
                        for (i in 0..4) {
                            val y = height * i / 4
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }
                        for (i in 0..6) {
                            val x = width * i / 6
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f
                            )
                        }
                        val lineColor = if (asset.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        val path = Path()
                        var isFirst = true
                        chartData.forEachIndexed { index, (_, price) ->
                            val x = width * index.toFloat() / chartData.size.toFloat()
                            val y = height - (height * ((price - minPrice) / priceRange).toFloat())
                            if (isFirst) {
                                path.moveTo(x, y)
                                isFirst = false
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3f)
                        )
                        val areaColor = if (asset.changePercent >= 0)
                            Color(0xFF10B981).copy(alpha = 0.2f)
                        else
                            Color(0xFFEF4444).copy(alpha = 0.2f)
                        val areaPath = Path()
                        areaPath.addPath(path)
                        areaPath.lineTo(width, height)
                        areaPath.lineTo(0f, height)
                        areaPath.close()
                        drawPath(path = areaPath, color = areaColor)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Chart statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Current Price",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                            Text(
                                when (asset.type) {
                                    is AssetType.IndianStock -> "₹${String.format("%.2f", asset.price ?: 0.0)}"
                                    is AssetType.Stock -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                                    is AssetType.Crypto -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Change",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                            Text(
                                "${String.format("%.2f", asset.changePercent ?: 0.0)}%",
                                color = if (asset.changePercent >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Text(error ?: "No chart data available.", color = Color.White, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun NewsContent(asset: PortfolioItem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        items(getMockNews()) { news ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        news.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        news.summary,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        news.date,
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisContent(asset: PortfolioItem) {
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
                        "Technical Analysis",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AnalysisRow("RSI", "65.2", "Neutral")
                    AnalysisRow("MACD", "Bullish", "Positive")
                    AnalysisRow("Moving Average", "Above 50-day", "Bullish")
                    AnalysisRow("Support", "₹2,400", "Strong")
                    AnalysisRow("Resistance", "₹2,500", "Moderate")
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Analyst Recommendations",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    RecommendationRow("Buy", 15, Color(0xFF10B981))
                    RecommendationRow("Hold", 8, Color(0xFFF59E0B))
                    RecommendationRow("Sell", 2, Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AnalysisRow(label: String, value: String, signal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 14.sp)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(signal, color = Color(0xFF10B981), fontSize = 12.sp)
        }
    }
}

@Composable
fun RecommendationRow(recommendation: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(recommendation, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("$count analysts", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun AddAssetDialog(asset: PortfolioItem, onAdd: (Double) -> Unit, onDismiss: () -> Unit) {
    var quantity by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${asset.symbol} to Portfolio", color = Color.White) },
        text = {
            Column {
                Text(
                    "Current Price: ${when (asset.type) {
                        is AssetType.IndianStock -> "₹${String.format("%.2f", asset.price ?: 0.0)}"
                        is AssetType.Stock -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                        is AssetType.Crypto -> "$${String.format("%.2f", asset.price ?: 0.0)}"
                    }}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
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
                    if (qty > 0) onAdd(qty)
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

data class NewsItem(val title: String, val summary: String, val date: String)

fun getMockNews(): List<NewsItem> {
    return listOf(
        NewsItem(
            "Reliance Industries Q3 Results Beat Estimates",
            "Reliance Industries reported better-than-expected quarterly results with strong performance across all segments.",
            "2 hours ago"
        ),
        NewsItem(
            "Reliance Jio Announces New 5G Plans",
            "Reliance Jio has announced new 5G data plans starting from ₹199, expanding its network coverage.",
            "1 day ago"
        ),
        NewsItem(
            "Reliance Retail Expands E-commerce Presence",
            "Reliance Retail is expanding its e-commerce operations with new partnerships and technology investments.",
            "3 days ago"
        ),
        NewsItem(
            "Oil & Gas Segment Shows Strong Recovery",
            "Reliance's oil and gas segment has shown strong recovery with improved refining margins.",
            "1 week ago"
        )
    )
}

// Extension function to get activity from context
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
} 
