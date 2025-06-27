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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.assetarc.ui.theme.AssetArcTheme
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.concurrent.TimeUnit

class SignUpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            AssetArcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignUpScreen(
                        onEmailSignUp = { showEmailDialog() },
                        onPhoneSignUp = { showPhoneDialog() }
                    )
                }
            }
        }
    }

    private fun showEmailDialog() {
        val context = this
        val emailInput = EditText(context)
        emailInput.hint = "Email"
        val passwordInput = EditText(context)
        passwordInput.hint = "Password"
        passwordInput.inputType = 129 // TYPE_TEXT_VARIATION_PASSWORD
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(emailInput)
        layout.addView(passwordInput)
        AlertDialog.Builder(context)
            .setTitle("Sign up with Email")
            .setView(layout)
            .setPositiveButton("Sign Up") { _, _ ->
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches() && password.isNotEmpty()) {
                    signUpWithEmail(email, password)
                } else {
                    Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created: ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Email sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showPhoneDialog() {
        val context = this
        val phoneInput = EditText(context)
        phoneInput.hint = "+1234567890"
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        layout.addView(phoneInput)
        AlertDialog.Builder(context)
            .setTitle("Sign up with Phone")
            .setView(layout)
            .setPositiveButton("Send OTP") { _, _ ->
                val phone = phoneInput.text.toString().trim()
                if (phone.isNotEmpty()) {
                    sendOtp(phone)
                } else {
                    Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendOtp(phone: String) {
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
                    Toast.makeText(this, "Phone sign up success: ${auth.currentUser?.phoneNumber}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Phone sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

@Composable
fun SignUpScreen(
    onEmailSignUp: () -> Unit,
    onPhoneSignUp: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = loading, label = "signup_loading") { isLoading ->
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    EmailSignUpButton(onClick = onEmailSignUp)
                    Spacer(modifier = Modifier.height(16.dp))
                    PhoneSignUpButton(onClick = onPhoneSignUp)
                    Spacer(modifier = Modifier.height(16.dp))
                    AppleSignUpButton { /* UI only */ }
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }) {
                        Text("Already have an account? Log In", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun EmailSignUpButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign up with Email", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PhoneSignUpButton(onClick: () -> Unit) {
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
        Text("Sign up with Phone", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppleSignUpButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp)),
        enabled = false // Apple Sign-In is not available on Android
    ) {
        Text("Sign up with Apple (iOS only)", color = Color.White, fontWeight = FontWeight.Bold)
    }
} 