package com.example.assetarc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class AccountActivity : AppCompatActivity() {
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnLoginLogout: Button
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        ivProfilePic = findViewById(R.id.ivProfilePic)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        btnLoginLogout = findViewById(R.id.btnLoginLogout)

        val user = auth.currentUser
        if (user != null) {
            tvUserEmail.text = user.email ?: "No email"
            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into(ivProfilePic)
            } else {
                ivProfilePic.setImageResource(R.drawable.ic_launcher_foreground)
            }
            btnLoginLogout.text = "Logout"
            btnLoginLogout.setOnClickListener {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            tvUserEmail.text = "Not logged in"
            ivProfilePic.setImageResource(R.drawable.ic_launcher_foreground)
            btnLoginLogout.text = "Login"
            btnLoginLogout.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_account
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    true
                }
                R.id.nav_chat -> {
                    startActivity(Intent(this, GeminiAssistantActivity::class.java))
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    true
                }
                R.id.nav_account -> true // Already here
                else -> false
            }
        }
    }
} 