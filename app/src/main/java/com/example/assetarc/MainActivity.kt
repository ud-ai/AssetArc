package com.example.assetarc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.assetarc.ui.theme.AssetArcTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.filled.Person
import com.example.assetarc.CryptoScreen
import com.example.assetarc.DashboardScreen
import com.example.assetarc.IndianStocksScreen
import com.example.assetarc.USStocksScreen

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Dashboard : Screen("dashboard", "Dashboard", { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Dashboard") })
    object Chat : Screen("chat", "Chat", { Icon(Icons.Outlined.Chat, contentDescription = "Chat") })
    object Account : Screen("account", "Account", { Icon(Icons.Filled.Person, contentDescription = "Account") })
    object IndianStocks : Screen("indian_stocks", "Indian Stocks", { Icon(Icons.Filled.ShowChart, contentDescription = "Indian Stocks") })
    object USStocks : Screen("us_stocks", "US Stocks", { Icon(Icons.Filled.AttachMoney, contentDescription = "US Stocks") })
    object Crypto : Screen("crypto", "Crypto", { Icon(Icons.Filled.CurrencyBitcoin, contentDescription = "Crypto") })
    companion object {
        val mainScreens = listOf(Dashboard, Chat, Account, IndianStocks, USStocks, Crypto)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssetArcTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                if (!isLoggedIn) {
                    StartScreen(onGetStarted = { isLoggedIn = true })
                } else {
                    MainNavScaffold()
                }
            }
        }
    }
}

@Composable
fun MainNavScaffold() {
    val navController = rememberNavController()
    var selectedIndex by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.mainScreens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = screen.icon,
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Account.route) { AccountScreen() }
            composable(Screen.IndianStocks.route) { IndianStocksScreen() }
            composable(Screen.USStocks.route) { USStocksScreen() }
            composable(Screen.Crypto.route) { CryptoScreen() }
        }
    }
}

// Stub composables for screens (replace with your actual implementations)
@Composable fun ChatScreen() { Text("Chat", modifier = Modifier.fillMaxSize()) }
@Composable fun AccountScreen() { Text("Account", modifier = Modifier.fillMaxSize()) }

@Composable
fun StartScreen(onGetStarted: () -> Unit) {
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF3B82F6),
        targetValue = Color(0xFF6366F1),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF60A5FA),
        targetValue = Color(0xFF818CF8),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "color2"
    )
    val color3 by infiniteTransition.animateColor(
        initialValue = Color(0xFF1E3A8A),
        targetValue = Color(0xFF6366F1),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "color3"
    )

    // Animations for content
    val appNameAnim = remember { Animatable(0f) }
    val welcomeAnim = remember { Animatable(0f) }
    val descAnim = remember { Animatable(0f) }
    val buttonAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appNameAnim.animateTo(1f, animationSpec = tween(700, delayMillis = 100))
        welcomeAnim.animateTo(1f, animationSpec = tween(500, delayMillis = 300))
        descAnim.animateTo(1f, animationSpec = tween(500, delayMillis = 500))
        buttonAnim.animateTo(1f, animationSpec = tween(500, delayMillis = 700))
    }

    // Button press animation
    val buttonScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(color1, color2, color3)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated App Name
            Text(
                text = "AssetArc",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = appNameAnim.value,
                        scaleX = 0.8f + 0.2f * appNameAnim.value,
                        scaleY = 0.8f + 0.2f * appNameAnim.value
                    )
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Animated Welcome
            Text(
                text = "Welcome to AssetArc",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer(alpha = welcomeAnim.value)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Animated Description
            Text(
                text = "Track your investments in stocks and crypto with ease. Get started by adding your portfolio.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .graphicsLayer(alpha = descAnim.value)
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Animated Button
            Button(
                onClick = {
                    onGetStarted() // Trigger navigation immediately
                    coroutineScope.launch {
                        buttonScale.animateTo(0.92f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                        buttonScale.animateTo(1f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = buttonScale.value * buttonAnim.value,
                        scaleY = buttonScale.value * buttonAnim.value,
                        alpha = buttonAnim.value
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)) // Bright yellow
            ) {
                Text("Get Started", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}