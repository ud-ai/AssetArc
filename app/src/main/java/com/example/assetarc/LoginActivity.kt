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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

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
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            AssetArcTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AuthScreen(
                        onGoogleAuth = { signInWithGoogle() },
                        onPhoneAuth = { isSignUp, phone ->
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    signInWithPhoneCredential(credential)
                                }
                                override fun onVerificationFailed(e: FirebaseException) {
                                    loading = false
                                    errorMessage = "Phone auth failed: ${e.localizedMessage ?: e.message}"
                                }
                                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    loading = false
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
                        onToggleDarkMode = { darkMode = !darkMode },
                        loading = loading,
                        setLoading = { loading = it },
                        errorMessage = errorMessage,
                        setErrorMessage = { errorMessage = it }
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
        // Show loading indicator (handled in composable)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in as: ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    val message = when (val ex = task.exception) {
                        is FirebaseAuthInvalidUserException -> "No user found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                        else -> ex?.localizedMessage ?: "Sign in failed."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
                    val message = when (val ex = task.exception) {
                        is FirebaseAuthUserCollisionException -> "Email already in use."
                        is FirebaseAuthWeakPasswordException -> "Password is too weak."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                        else -> ex?.localizedMessage ?: "Sign up failed."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
                    val message = task.exception?.localizedMessage ?: "Phone sign in failed."
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
    onToggleDarkMode: () -> Unit,
    loading: Boolean,
    setLoading: (Boolean) -> Unit,
    errorMessage: String?,
    setErrorMessage: (String?) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // Minimal color palette
    val accentColor = Color(0xFF3B82F6) // Soft blue
    val backgroundColor = Color(0xFFF6F8FA) // Light gray
    val cardColor = Color.White
    val textPrimary = Color(0xFF1A1A1A)
    val textSecondary = Color(0xFF6B7280)
    val dividerColor = Color(0xFFE5E7EB)
    val buttonColor = accentColor
    val buttonTextColor = Color.White
    val fieldBg = Color(0xFFF3F4F6)
    val fieldBorder = Color(0xFFD1D5DB)

    // Animated card
    val cardAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        cardAnim.animateTo(1f, animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing))
    }
    // Button scale animation
    val coroutineScope = rememberCoroutineScope()
    val googleBtnScale = remember { Animatable(1f) }
    val emailBtnScale = remember { Animatable(1f) }
    val phoneBtnScale = remember { Animatable(1f) }
    // For radial reveal animation (dark mode)
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
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(backgroundColor, Color.White)
    )
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
        // App name at the top
        Text(
            text = "AssetArc",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )
        // Dark mode toggle (kept for UI, but always minimal)
        IconButton(
            onClick = {
                revealCenter = togglePos
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
                tint = accentColor
            )
        }
        // Animated card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp, bottom = 32.dp)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(
                        alpha = cardAnim.value,
                        translationY = (1f - cardAnim.value) * 60f
                    )
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .fillMaxWidth(0.97f)
                    .wrapContentHeight()
                    .verticalScroll(scrollState),
                elevation = CardDefaults.cardElevation(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(36.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back!",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    if (loading) {
                        CircularProgressIndicator(color = accentColor)
                    } else {
                        errorMessage?.let {
                            Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        // Google Auth Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    googleBtnScale.animateTo(0.92f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                                    googleBtnScale.animateTo(1f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                                }
                                onGoogleAuth()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .graphicsLayer(scaleX = googleBtnScale.value, scaleY = googleBtnScale.value)
                        ) {
                            Text("Sign in with Google", color = buttonTextColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Email Auth Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    emailBtnScale.animateTo(0.92f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                                    emailBtnScale.animateTo(1f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                                }
                                showEmailDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .graphicsLayer(scaleX = emailBtnScale.value, scaleY = emailBtnScale.value)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = buttonTextColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSignUp) "Sign up with Email" else "Sign in with Email",
                                color = buttonTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Phone Auth Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    phoneBtnScale.animateTo(0.92f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                                    phoneBtnScale.animateTo(1f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                                }
                                showPhoneDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .graphicsLayer(scaleX = phoneBtnScale.value, scaleY = phoneBtnScale.value)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = buttonTextColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSignUp) "Sign up with Phone" else "Sign in with Phone",
                                color = buttonTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Divider(color = dividerColor.copy(alpha = 0.7f))
                        TextButton(onClick = { isSignUp = !isSignUp }) {
                            Text(
                                if (isSignUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        // Phone and Email dialogs remain unchanged, but use minimal theme
        if (showPhoneDialog) {
            Dialog(onDismissRequest = { showPhoneDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUp) "Sign up with Phone" else "Sign in with Phone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number", color = textSecondary) },
                            placeholder = { Text("Enter phone number", color = textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = fieldBorder,
                                cursorColor = accentColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textSecondary,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().background(fieldBg)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val digits = phoneInput.filter { it.isDigit() }
                                if (digits.length == 10) {
                                    setLoading(true)
                                    setErrorMessage(null)
                                    showPhoneDialog = false
                                    val formattedPhone = if (digits.startsWith("+")) digits else "+91$digits"
                                    onPhoneAuth(isSignUp, formattedPhone)
                                } else {
                                    setErrorMessage("Enter a valid 10-digit phone number.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Send OTP", color = buttonTextColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (showEmailDialog) {
            Dialog(onDismissRequest = { showEmailDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUp) "Sign up with Email" else "Sign in with Email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email", color = textSecondary) },
                            placeholder = { Text("example@email.com", color = textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = fieldBorder,
                                cursorColor = accentColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textSecondary,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().background(fieldBg)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = textSecondary) },
                            placeholder = { Text("Password", color = textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = fieldBorder,
                                cursorColor = accentColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textSecondary,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().background(fieldBg)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (emailInput.isNotEmpty() && passwordInput.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                    setLoading(true)
                                    setErrorMessage(null)
                                    showEmailDialog = false
                                    onEmailAuth(isSignUp, emailInput, passwordInput)
                                } else {
                                    setErrorMessage("Invalid email or password.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isSignUp) "Sign Up" else "Sign In", color = buttonTextColor, fontWeight = FontWeight.Bold)
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