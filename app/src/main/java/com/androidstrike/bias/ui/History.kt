package com.androidstrike.bias.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.androidstrike.bias.R

class History : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view_history) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration =
            AppBarConfiguration(setOf(R.id.attendance_overview, R.id.attendance_record))

        setupActionBarWithNavController(navController, appBarConfiguration)


    }
}