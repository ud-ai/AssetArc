package com.example.assetarc

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.UserProfileChangeRequest

class AccountActivity : AppCompatActivity() {
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnLoginLogout: Button
    private val auth = FirebaseAuth.getInstance()
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Show selected image immediately
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_launcher_foreground)
                .apply(RequestOptions.circleCropTransform())
                .into(ivProfilePic)
            // Upload to Firebase Storage and update profile
            uploadProfileImageToFirebase(it)
        }
    }

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
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfilePic)
            } else {
                val placeholder = getInitialsPlaceholder(user.displayName ?: user.email)
                Glide.with(this)
                    .load(R.drawable.ic_launcher_foreground)
                    .placeholder(placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfilePic)
            }
            btnLoginLogout.text = "Logout"
            btnLoginLogout.setOnClickListener {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            tvUserEmail.text = "Not logged in"
            val placeholder = getInitialsPlaceholder(null)
            Glide.with(this)
                .load(R.drawable.ic_launcher_foreground)
                .placeholder(placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(ivProfilePic)
            btnLoginLogout.text = "Login"
            btnLoginLogout.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        ivProfilePic.setOnClickListener {
            selectImageLauncher.launch("image/*")
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

    private fun getInitialsPlaceholder(nameOrEmail: String?): BitmapDrawable {
        val initials = nameOrEmail?.split("@")?.get(0)?.split(" ", ".", "_")?.filter { it.isNotEmpty() }?.map { it[0].uppercaseChar() }?.joinToString("")?.take(2) ?: "U"
        val size = resources.getDimensionPixelSize(R.dimen.profile_avatar_size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.parseColor("#B0B0B0")
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = Color.WHITE
        paint.textSize = size * 0.4f
        paint.textAlign = Paint.Align.CENTER
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(initials, size / 2f, y, paint)
        return BitmapDrawable(resources, bitmap)
    }

    private fun uploadProfileImageToFirebase(uri: Uri) {
        val user = auth.currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/${user.uid}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()
                    user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Glide.with(this)
                                .load(downloadUri)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .apply(RequestOptions.circleCropTransform())
                                .into(ivProfilePic)
                        }
                    }
                }
            }
    }
} 