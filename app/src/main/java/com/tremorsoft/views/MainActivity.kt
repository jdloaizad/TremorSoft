/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.tremorsoft.R
import com.tremorsoft.databinding.ActivityMainBinding
import com.tremorsoft.interfaces.DrawerController

class MainActivity : AppCompatActivity(), DrawerController {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var drawerLayout: DrawerLayout? = null
    var navView: NavigationView? = null
    val REQUEST_ENABLE_BT = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_login, R.id.nav_signUp, R.id.nav_home,
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView?.setupWithNavController(navController)
        setDrawerLocked()

        supportActionBar?.hide()
        window.statusBarColor = Color.WHITE

        registerReceiver(
            mBluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerForResult.launch(enableBtIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBluetoothStateReceiver)
    }

    override fun onBackPressed() {
        val manager = supportFragmentManager
        if (manager.backStackEntryCount > 0) manager.popBackStack() else super.onBackPressed()
    }

    private val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
        }
    }

    private val mBluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun setDrawerLocked() {
        drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun setDrawerUnlocked() {
        drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }
}