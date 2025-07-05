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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Help
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.example.assetarc.GeminiContentRequest
import com.example.assetarc.Content
import com.example.assetarc.Part
import com.example.assetarc.GeminiContentResponse
import androidx.compose.runtime.DisposableEffect

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
                // Use FirebaseAuth to determine if the user is logged in
                val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                var forceUpdate by remember { mutableStateOf(0) }

                // Listen for auth state changes (force recomposition)
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener {
                        forceUpdate++ // Triggers recomposition
                    }
                    FirebaseAuth.getInstance().addAuthStateListener(listener)
                    onDispose {
                        FirebaseAuth.getInstance().removeAuthStateListener(listener)
                    }
                }

                if (!isLoggedIn) {
                    StartScreen(onGetStarted = {
                        // Navigate to LoginActivity
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                    })
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
            ) {
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
                        icon = {
                            // Use standard icon size and alignment for best results
                            Icon(
                                modifier = Modifier.size(26.dp),
                                contentDescription = screen.label,
                                imageVector = when (screen) {
                                    is Screen.Dashboard -> Icons.Filled.AccountBalanceWallet
                                    is Screen.Chat -> Icons.Outlined.Chat
                                    is Screen.Account -> Icons.Filled.Person
                                    is Screen.IndianStocks -> Icons.Filled.ShowChart
                                    is Screen.USStocks -> Icons.Filled.AttachMoney
                                    is Screen.Crypto -> Icons.Filled.CurrencyBitcoin
                                },
                                tint = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        label = { 
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val geminiApi = GeminiApiService.create()
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Load chat history
    LaunchedEffect(Unit) {
        userId?.let { uid ->
            firestore.collection("users").document(uid).collection("chatHistory")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener { result ->
                    val chatMessages = mutableListOf<ChatMessage>()
                    for (doc in result) {
                        val message = doc.getString("message") ?: ""
                        val isUser = message.startsWith("You:")
                        val cleanMsg = message.removePrefix("You: ").removePrefix("Gemini: ")
                        chatMessages.add(ChatMessage(cleanMsg, isUser, doc.getLong("timestamp") ?: System.currentTimeMillis()))
                    }
                    messages = chatMessages
                }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Chat,
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Gemini Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }
            if (isLoading) {
                item {
                    ChatMessageItem(
                        message = ChatMessage("Typing...", false, System.currentTimeMillis()),
                        isLoading = true
                    )
                }
            }
        }
        
        // Input section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type your message...") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val userMessage = inputText.trim()
                                inputText = ""
                                isLoading = true
                                
                                // Add user message
                                val newUserMessage = ChatMessage(userMessage, true, System.currentTimeMillis())
                                messages = messages + newUserMessage
                                
                                // Save to Firestore
                                userId?.let { uid ->
                                    val chatRef = firestore.collection("users").document(uid).collection("chatHistory")
                                    val data = hashMapOf(
                                        "message" to "You: $userMessage",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    chatRef.add(data)
                                }
                                
                                // Send to Gemini
                                val request = GeminiContentRequest(
                                    contents = listOf(Content(parts = listOf(Part(text = userMessage))))
                                )
                                geminiApi.generateContent(request).enqueue(object : Callback<GeminiContentResponse> {
                                    override fun onResponse(call: Call<GeminiContentResponse>, response: Response<GeminiContentResponse>) {
                                        isLoading = false
                                        val geminiText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                            ?: "Sorry, I couldn't generate a response."
                                        
                                        val newGeminiMessage = ChatMessage(geminiText, false, System.currentTimeMillis())
                                        messages = messages + newGeminiMessage
                                        
                                        // Save Gemini response to Firestore
                                        userId?.let { uid ->
                                            val chatRef = firestore.collection("users").document(uid).collection("chatHistory")
                                            val data = hashMapOf(
                                                "message" to "Gemini: $geminiText",
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            chatRef.add(data)
                                        }
                                    }
                                    override fun onFailure(call: Call<GeminiContentResponse>, t: Throwable) {
                                        isLoading = false
                                        val errorMessage = ChatMessage("Error: ${t.localizedMessage}", false, System.currentTimeMillis())
                                        messages = messages + errorMessage
                                    }
                                })
                                
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
                    ),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMessage = inputText.trim()
                            inputText = ""
                            isLoading = true
                            
                            // Add user message
                            val newUserMessage = ChatMessage(userMessage, true, System.currentTimeMillis())
                            messages = messages + newUserMessage
                            
                            // Save to Firestore
                            userId?.let { uid ->
                                val chatRef = firestore.collection("users").document(uid).collection("chatHistory")
                                val data = hashMapOf(
                                    "message" to "You: $userMessage",
                                    "timestamp" to System.currentTimeMillis()
                                )
                                chatRef.add(data)
                            }
                            
                            // Send to Gemini
                            val request = GeminiContentRequest(
                                contents = listOf(Content(parts = listOf(Part(text = userMessage))))
                            )
                            geminiApi.generateContent(request).enqueue(object : Callback<GeminiContentResponse> {
                                override fun onResponse(call: Call<GeminiContentResponse>, response: Response<GeminiContentResponse>) {
                                    isLoading = false
                                    val geminiText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                        ?: "Sorry, I couldn't generate a response."
                                    
                                    val newGeminiMessage = ChatMessage(geminiText, false, System.currentTimeMillis())
                                    messages = messages + newGeminiMessage
                                    
                                    // Save Gemini response to Firestore
                                    userId?.let { uid ->
                                        val chatRef = firestore.collection("users").document(uid).collection("chatHistory")
                                        val data = hashMapOf(
                                            "message" to "Gemini: $geminiText",
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        chatRef.add(data)
                                    }
                                }
                                override fun onFailure(call: Call<GeminiContentResponse>, t: Throwable) {
                                    isLoading = false
                                    val errorMessage = ChatMessage("Error: ${t.localizedMessage}", false, System.currentTimeMillis())
                                    messages = messages + errorMessage
                                }
                            })
                            
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(24.dp)
                        ),
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage, isLoading: Boolean = false) {
    val isUser = message.isUser
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Gemini avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Chat,
                    contentDescription = "Gemini",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, false)
        ) {
            Surface(
                color = if (isUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "You",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Account",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Profile Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(40.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // User Info
                        if (user != null) {
                            Text(
                                text = user.email ?: "No email",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "Not signed in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sign in to access your account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            item {
                // Account Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Account Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Login/Logout Button
                        Button(
                            onClick = {
                                if (user != null) {
                                    auth.signOut()
                                    // Navigate to login
                                    context.startActivity(Intent(context, LoginActivity::class.java))
                                } else {
                                    // Navigate to login
                                    context.startActivity(Intent(context, LoginActivity::class.java))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user != null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (user != null) Icons.Filled.Logout else Icons.Filled.Login,
                                contentDescription = if (user != null) "Logout" else "Login",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (user != null) "Logout" else "Login",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            item {
                // Settings Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Settings Items
                        SettingsItem(
                            icon = Icons.Filled.Settings,
                            title = "Preferences",
                            subtitle = "Customize your experience"
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = Icons.Filled.Help,
                            title = "Help & Support",
                            subtitle = "Get help and contact support"
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = Icons.Filled.Email,
                            title = "Contact Us",
                            subtitle = "Send us feedback"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 8.dp),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            Icons.Filled.Send,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

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