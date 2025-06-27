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
                        onPhoneAuth = { isSignUp -> showPhoneDialog(isSignUp) },
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
                } else {
                    Toast.makeText(this, "Firebase auth failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showPhoneDialog(isSignUp: Boolean) {
        val context = this
        val phoneInput = EditText(context)
        phoneInput.hint = "+1234567890"
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(phoneInput)
        AlertDialog.Builder(context)
            .setTitle(if (isSignUp) "Sign up with Phone" else "Sign in with Phone")
            .setView(layout)
            .setPositiveButton("Send OTP") { _, _ ->
                val phone = phoneInput.text.toString().trim()
                if (phone.isNotEmpty()) {
                    sendOtp(phone, isSignUp)
                } else {
                    Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendOtp(phone: String, isSignUp: Boolean) {
        val context = this
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(context, "Phone auth failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                showOtpDialog(verificationId)
            }
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phone, 60, TimeUnit.SECONDS, this, callbacks)
    }

    private fun showOtpDialog(verificationId: String) {
        val context = this
        val otpInput = EditText(context)
        otpInput.hint = "Enter OTP"
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(otpInput)
        AlertDialog.Builder(context)
            .setTitle("Enter OTP")
            .setView(layout)
            .setPositiveButton("Verify") { _, _ ->
                val otp = otpInput.text.toString().trim()
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                signInWithPhoneAuthCredential(credential)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in as: ${auth.currentUser?.phoneNumber}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Phone sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

@Composable
fun AuthScreen(
    onGoogleAuth: () -> Unit,
    onPhoneAuth: (Boolean) -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // For radial reveal animation
    var togglePos by remember { mutableStateOf(Offset.Zero) }
    var reveal by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxRadius = hypot(screenWidthPx, screenHeightPx)
    val animProgress by animateFloatAsState(
        targetValue = if (reveal) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    LaunchedEffect(reveal, darkMode) {
        if (reveal) {
            kotlinx.coroutines.delay(1000)
            reveal = false
        }
    }
    val backgroundBrush = if (animProgress > 0f && animProgress < 1f) {
        Brush.radialGradient(
            colors = if (darkMode) listOf(Color(0xFF232526), Color.Transparent) else listOf(Color(0xFFe0eafc), Color.Transparent),
            center = togglePos,
            radius = (maxRadius * animProgress) + 200f
        )
    } else {
        Brush.verticalGradient(
            colors = if (darkMode) listOf(Color(0xFF232526), Color(0xFF414345))
            else listOf(Color(0xFFe0eafc), Color(0xFFcfdef3))
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        IconButton(
            onClick = {
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
                tint = Color.Red
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = loading, label = "auth_loading") { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator()
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
                            containerColor = if (darkMode) Color(0xFF232526) else Color.White
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Create Account" else "Welcome Back!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            GoogleAuthButton(onClick = onGoogleAuth)
                            Spacer(modifier = Modifier.height(16.dp))
                            PhoneAuthButton(isSignUp = isSignUp, onClick = { onPhoneAuth(isSignUp) })
                            Spacer(modifier = Modifier.height(16.dp))
                            AppleAuthButton()
                            Spacer(modifier = Modifier.height(32.dp))
                            TextButton(onClick = { isSignUp = !isSignUp }) {
                                Text(
                                    if (isSignUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleAuthButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Text("Sign in with Google", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhoneAuthButton(isSignUp: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isSignUp) "Sign up with Phone" else "Sign in with Phone",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppleAuthButton() {
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