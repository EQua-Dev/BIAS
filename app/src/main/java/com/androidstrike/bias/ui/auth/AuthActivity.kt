package com.androidstrike.bias.ui.auth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.androidstrike.bias.R

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        supportActionBar?.hide()
    }
}