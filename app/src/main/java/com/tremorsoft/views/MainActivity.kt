/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

//  Copyright (c) 2003-2020 Xsens Technologies B.V. or subsidiaries worldwide.
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification,
//  are permitted provided that the following conditions are met:
//
//  1.      Redistributions of source code must retain the above copyright notice,
//           this list of conditions, and the following disclaimer.
//
//  2.      Redistributions in binary form must reproduce the above copyright notice,
//           this list of conditions, and the following disclaimer in the documentation
//           and/or other materials provided with the distribution.
//
//  3.      Neither the names of the copyright holders nor the names of their contributors
//           may be used to endorse or promote products derived from this software without
//           specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
//  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
//  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
//  THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
//  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
//  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY OR
//  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.THE LAWS OF THE NETHERLANDS
//  SHALL BE EXCLUSIVELY APPLICABLE AND ANY DISPUTES SHALL BE FINALLY SETTLED UNDER THE RULES
//  OF ARBITRATION OF THE INTERNATIONAL CHAMBER OF COMMERCE IN THE HAGUE BY ONE OR MORE
//  ARBITRATORS APPOINTED IN ACCORDANCE WITH SAID RULES.
//

package com.tremorsoft.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.tremorsoft.utils.Utils
import com.tremorsoft.viewmodels.BluetoothViewModel


class MainActivity : AppCompatActivity(), DrawerController {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    var drawerLayout: DrawerLayout? = null
    var navView: NavigationView? = null

    private val TAG = MainActivity::class.java.simpleName

    private val REQUEST_ENABLE_BLUETOOTH = 1001
    private val REQUEST_PERMISSION_LOCATION = 1002

    // The Bluetooth view model instance
    private var mBluetoothViewModel: BluetoothViewModel? = null

    // A variable for scanning flag
    private var mIsScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
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

        bindViewModel()
        //checkBluetoothAndPermission()

        // Register this action to monitor Bluetooth status.
        registerReceiver(
            mBluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun onPostResume() {
        super.onPostResume()
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBluetoothStateReceiver)
    }

    override fun onBackPressed() {
        val manager = supportFragmentManager

        // If the fragment count > 0 in the stack, try to resume the previous page.
        if (manager.backStackEntryCount > 0) manager.popBackStack() else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult() - requestCode = $requestCode, resultCode = $resultCode"
        )
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) checkBluetoothAndPermission() else Toast.makeText(
                this,
                getString(R.string.hint_turn_on_bluetooth),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult() - requestCode = $requestCode")

        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            for (i in grantResults.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) checkBluetoothAndPermission() else Toast.makeText(
                        this,
                        getString(R.string.hint_allow_location),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun checkBluetoothAndPermission(): Boolean {
        val isBluetoothEnabled = Utils.isBluetoothAdapterEnabled(this)
        val isPermissionGranted = Utils.isLocationPermissionGranted(this)

        if (isBluetoothEnabled) {
            if (!isPermissionGranted) {
                Utils.requestLocationPermission(
                    this,
                    REQUEST_PERMISSION_LOCATION
                )
            }
        } else {
            Utils.requestEnableBluetooth(this, REQUEST_ENABLE_BLUETOOTH)
        }
        val status = isBluetoothEnabled && isPermissionGranted
        Log.i(TAG, "checkBluetoothAndPermission() - $status")
        mBluetoothViewModel!!.updateBluetoothEnableState(status)
        return status
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        mBluetoothViewModel = BluetoothViewModel.getInstance(this)
        mBluetoothViewModel?.isScanning?.observe(this) { scanning: Boolean ->
            // If the status of scanning is changed, try to refresh the menu.
            mIsScanning = scanning

        }
    }

    /**
     * A receiver for Bluetooth status.
     */
    private val mBluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> mBluetoothViewModel!!.updateBluetoothEnableState(
                            false
                        )
                        BluetoothAdapter.STATE_ON -> mBluetoothViewModel!!.updateBluetoothEnableState(
                            true
                        )
                    }
                }
            }
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