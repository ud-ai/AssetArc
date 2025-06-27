package com.example.assetarc

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assetarc.ui.theme.AssetArcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.example.assetarc.BuildConfig
import androidx.compose.runtime.rememberCoroutineScope

sealed class AssetType { object Stock : AssetType(); object Crypto : AssetType() }
data class PortfolioItem(val type: AssetType, val symbol: String, val quantity: Double, val price: Double)

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

@Composable
fun DashboardScreen() {
    var portfolio by remember { mutableStateOf(listOf<PortfolioItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var totalBalance by remember { mutableStateOf(0.0) }
    var loading by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Prompt for API key if not set
    LaunchedEffect(Unit) {
        if (apiKey.isBlank()) {
            // In production, use secure storage. For demo, prompt user.
            apiKey = promptForApiKey(context)
        }
    }

    // Update total balance whenever portfolio changes
    LaunchedEffect(portfolio) {
        totalBalance = portfolio.sumOf { it.quantity * it.price }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF17212B))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Move search bar to the top
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search portfolio...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3390EC),
                    unfocusedBorderColor = Color(0xFF232E3C),
                    cursorColor = Color(0xFF3390EC),
                    focusedLabelColor = Color(0xFF3390EC),
                    unfocusedLabelColor = Color(0xFFAEBACB),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Divider(color = Color(0xFF232E3C), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232E3C))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Balance", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$" + String.format("%.2f", totalBalance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Portfolio",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
            )
            val filteredPortfolio = portfolio.filter {
                searchQuery.isBlank() ||
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                (it.type is AssetType.Stock && "stock".contains(searchQuery, ignoreCase = true)) ||
                (it.type is AssetType.Crypto && "crypto".contains(searchQuery, ignoreCase = true))
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (filteredPortfolio.isEmpty()) {
                    item {
                        Text("No assets yet. Add stocks or crypto!", color = Color(0xFFAEBACB), modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(filteredPortfolio) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF232E3C))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.type is AssetType.Stock) "Stock" else "Crypto",
                                    color = if (item.type is AssetType.Stock) Color(0xFF3390EC) else Color(0xFFF7931A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(60.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.symbol, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Qty: ${item.quantity}", color = Color.White, modifier = Modifier.width(80.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$${String.format("%.2f", item.price)}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3390EC))
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFF3390EC),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Asset")
        }
        if (showAddDialog) {
            AddAssetDialog(
                onAdd = { type, symbol, qty ->
                    showAddDialog = false
                    loading = true
                    coroutineScope.launch {
                        fetchFinnhubPrice(symbol, type, apiKey, context) { price ->
                            loading = false
                            if (price != null) {
                                portfolio = portfolio + PortfolioItem(type, symbol.uppercase(), qty, price)
                            } else {
                                Toast.makeText(context, "Failed to fetch price for $symbol", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

fun promptForApiKey(context: Context): String {
    return BuildConfig.FINNHUB_API_KEY
}

suspend fun fetchFinnhubPrice(symbol: String, type: AssetType, apiKey: String, context: Context, onResult: (Double?) -> Unit) {
    val querySymbol = if (type is AssetType.Crypto) "BINANCE:${symbol.uppercase()}USDT" else symbol.uppercase()
    val url = "https://finnhub.io/api/v1/quote?symbol=$querySymbol&token=$apiKey"
    val price = withContext(Dispatchers.IO) {
        try {
            val response = URL(url).readText()
            val json = JSONObject(response)
            json.optDouble("c", Double.NaN).takeIf { !it.isNaN() }
        } catch (e: Exception) {
            null
        }
    }
    onResult(price)
}

@Composable
fun AddAssetDialog(onAdd: (AssetType, String, Double) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf<AssetType>(AssetType.Stock) }
    var symbol by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Asset") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type is AssetType.Stock, onClick = { type = AssetType.Stock })
                    Text("Stock", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = type is AssetType.Crypto, onClick = { type = AssetType.Crypto })
                    Text("Crypto")
                }
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbol (e.g. AAPL, BTC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantity = qty.toDoubleOrNull() ?: 0.0
                    if (symbol.isNotBlank() && quantity > 0) {
                        onAdd(type, symbol, quantity)
                    }
                },
                enabled = symbol.isNotBlank() && qty.isNotBlank() && qty.toDoubleOrNull() != null && qty.toDouble() > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 