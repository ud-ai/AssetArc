package com.example.assetarc

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assetarc.ui.theme.AssetArcTheme
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.hypot
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            AssetArcTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AuthScreen(
                        onGoogleAuth = { signInWithGoogle() },
                        onPhoneAuth = { isSignUp, phone ->
                            // Use Firebase phone auth flow
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    signInWithPhoneCredential(credential)
                                }
                                override fun onVerificationFailed(e: FirebaseException) {
                                    Toast.makeText(this@LoginActivity, "Phone auth failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    // Show dialog to enter OTP
                                    val otpInput = EditText(this@LoginActivity)
                                    otpInput.hint = "Enter OTP"
                                    AlertDialog.Builder(this@LoginActivity)
                                        .setTitle("Enter OTP")
                                        .setView(otpInput)
                                        .setPositiveButton("Verify") { _, _ ->
                                            val otp = otpInput.text.toString().trim()
                                            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                                            signInWithPhoneCredential(credential)
                                        }
                                        .setNegativeButton("Cancel", null)
                                        .show()
                                }
                            }
                            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                phone, 60, java.util.concurrent.TimeUnit.SECONDS, this@LoginActivity, callbacks)
                        },
                        onEmailAuth = { isSignUp, email, password ->
                            if (isSignUp) signUpWithEmail(email, password) else signInWithEmail(email, password)
                        },
                        darkMode = darkMode,
                        onToggleDarkMode = { darkMode = !darkMode }
                    )
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as: ${user?.displayName}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Firebase auth failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Add these functions to handle email and phone auth, launching DashboardActivity on success
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in as: ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Email sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created: ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Email sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Phone sign in success: ${auth.currentUser?.phoneNumber}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Phone sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

@Composable
fun AuthScreen(
    onGoogleAuth: () -> Unit,
    onPhoneAuth: (Boolean, String) -> Unit,
    onEmailAuth: (Boolean, String, String) -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // Telegram-inspired colors
    val telegramDarkBg = Color(0xFF17212B)
    val telegramDarkCard = Color(0xFF232E3C)
    val telegramAccent = Color(0xFF3390EC)
    val telegramTextPrimary = Color.White
    val telegramTextSecondary = Color(0xFFAEBACB)
    val telegramDivider = Color(0xFF232E3C)
    val telegramLightBg = Color(0xFFe0eafc)
    val telegramLightCard = Color.White
    val telegramLightText = Color(0xFF232526)

    // For radial reveal animation
    var togglePos by remember { mutableStateOf(Offset.Zero) }
    var reveal by remember { mutableStateOf(false) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    var revealRadius by remember { mutableStateOf(0f) }
    var boxSize by remember { mutableStateOf(Size.Zero) }
    val animProgress by animateFloatAsState(
        targetValue = if (reveal) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
    )
    LaunchedEffect(reveal, darkMode) {
        if (reveal) {
            kotlinx.coroutines.delay(900)
            reveal = false
        }
    }
    val backgroundBrush = if (animProgress > 0f && animProgress < 1f) {
        val fromColor = if (!darkMode) telegramDarkBg else telegramLightBg
        val toColor = if (darkMode) telegramDarkBg else telegramLightBg
        Brush.radialGradient(
            colors = listOf(lerp(fromColor, toColor, animProgress), Color.Transparent),
            center = revealCenter,
            radius = revealRadius * animProgress
        )
    } else {
        Brush.verticalGradient(
            colors = if (darkMode) listOf(telegramDarkBg, telegramDarkCard)
            else listOf(telegramLightBg, telegramLightCard)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .onGloballyPositioned { coordinates ->
                boxSize = Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat()
                )
            }
    ) {
        IconButton(
            onClick = {
                revealCenter = togglePos
                // Calculate the farthest distance from the toggle to a corner
                val distances = listOf(
                    revealCenter.getDistance(Offset(0f, 0f)),
                    revealCenter.getDistance(Offset(boxSize.width, 0f)),
                    revealCenter.getDistance(Offset(0f, boxSize.height)),
                    revealCenter.getDistance(Offset(boxSize.width, boxSize.height))
                )
                revealRadius = distances.maxOrNull() ?: 0f
                reveal = true
                onToggleDarkMode()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .onGloballyPositioned { coordinates ->
                    togglePos = coordinates.positionInRoot() + Offset(
                        x = coordinates.size.width / 2f,
                        y = coordinates.size.height / 2f
                    )
                }
        ) {
            Icon(
                imageVector = if (darkMode) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                contentDescription = "Toggle dark mode",
                tint = telegramAccent
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SVG logo removed, native UI only
            Crossfade(targetState = loading, label = "auth_loading") { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(color = telegramAccent)
                } else {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .fillMaxWidth(0.95f)
                            .wrapContentHeight()
                            .verticalScroll(scrollState),
                        elevation = CardDefaults.cardElevation(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (darkMode) telegramDarkCard else telegramLightCard
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Welcome Back!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) telegramTextPrimary else telegramLightText
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            GoogleAuthButton(onClick = onGoogleAuth, accentColor = telegramAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showEmailDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = telegramAccent.copy(red = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isSignUp) "Sign up with Email" else "Sign in with Email",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            PhoneAuthButton(isSignUp = isSignUp, onClick = { showPhoneDialog = true }, accentColor = telegramAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            AppleAuthButton(accentColor = telegramAccent)
                            Spacer(modifier = Modifier.height(32.dp))
                            Divider(color = telegramDivider.copy(alpha = 0.2f))
                            TextButton(onClick = { isSignUp = !isSignUp }) {
                                Text(
                                    if (isSignUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                                    color = telegramAccent
                                )
                            }
                        }
                    }
                }
            }
        }
        // Modern Compose-based phone input dialog
        if (showPhoneDialog) {
            Dialog(onDismissRequest = { showPhoneDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) telegramDarkCard else telegramLightCard
                    ),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.95f)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { showPhoneDialog = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Brightness7, contentDescription = "Close", tint = telegramAccent)
                        }
                    }
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUp) "Sign up with Phone" else "Sign in with Phone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (darkMode) telegramTextPrimary else telegramLightText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1234567890") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = telegramAccent,
                                unfocusedBorderColor = telegramDivider,
                                cursorColor = telegramAccent,
                                focusedLabelColor = telegramAccent,
                                unfocusedLabelColor = telegramTextSecondary,
                                focusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText,
                                unfocusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (phoneInput.isNotEmpty()) {
                                    showPhoneDialog = false
                                    onPhoneAuth(isSignUp, phoneInput)
                                } else {
                                    Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = telegramAccent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Send OTP", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // Modern Compose-based email input dialog
        if (showEmailDialog) {
            Dialog(onDismissRequest = { showEmailDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) telegramDarkCard else telegramLightCard
                    ),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.95f)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { showEmailDialog = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Brightness7, contentDescription = "Close", tint = telegramAccent)
                        }
                    }
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUp) "Sign up with Email" else "Sign in with Email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (darkMode) telegramTextPrimary else telegramLightText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email") },
                            placeholder = { Text("example@email.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = telegramAccent,
                                unfocusedBorderColor = telegramDivider,
                                cursorColor = telegramAccent,
                                focusedLabelColor = telegramAccent,
                                unfocusedLabelColor = telegramTextSecondary,
                                focusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText,
                                unfocusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            placeholder = { Text("Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = telegramAccent,
                                unfocusedBorderColor = telegramDivider,
                                cursorColor = telegramAccent,
                                focusedLabelColor = telegramAccent,
                                unfocusedLabelColor = telegramTextSecondary,
                                focusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText,
                                unfocusedTextColor = if (darkMode) telegramTextPrimary else telegramLightText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (emailInput.isNotEmpty() && passwordInput.isNotEmpty()) {
                                    showEmailDialog = false
                                    onEmailAuth(isSignUp, emailInput, passwordInput)
                                } else {
                                    Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = telegramAccent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isSignUp) "Sign Up" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun Offset.getDistance(other: Offset): Float {
    return kotlin.math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
}

@Composable
fun GoogleAuthButton(onClick: () -> Unit, accentColor: Color) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Text("Sign in with Google", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppleAuthButton(accentColor: Color) {
    Button(
        onClick = {},
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp)),
        enabled = false // Apple Sign-In is not available on Android
    ) {
        Text("Continue with Apple (iOS only)", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhoneAuthButton(isSignUp: Boolean, onClick: () -> Unit, accentColor: Color) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Text(
            if (isSignUp) "Sign up with Phone" else "Sign in with Phone",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
} 