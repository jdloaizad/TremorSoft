/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentHomeBinding


class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var auth: FirebaseAuth
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ):
            View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).supportActionBar?.show()
        (requireActivity() as MainActivity).window.statusBarColor =
            context?.getColor(R.color.colorPrimaryDark)!!
        val actionBarColor = ColorDrawable(Color.parseColor("#C62828"))
        (requireActivity() as MainActivity).actionBar?.setBackgroundDrawable(actionBarColor)
        (requireActivity() as MainActivity).setDrawerUnlocked()

        auth = Firebase.auth
        val user = auth.currentUser

        val navView = (requireActivity() as MainActivity).navView
        val headerView = navView?.getHeaderView(0)
        val emailView = headerView?.findViewById<TextView>(R.id.currentUserEmail)
        val nameView = headerView?.findViewById<TextView>(R.id.currentUserName)
        val profileImage = headerView?.findViewById<ImageView>(R.id.logo)

        emailView?.text = user?.email.toString()
        nameView?.text = user?.displayName.toString()

        with(binding) {
            addPatient.setOnClickListener {
                findNavController().navigate(R.id.action_nav_home_to_nav_patient)
            }
        }

        navView?.menu?.findItem(R.id.nav_login)?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.nav_login -> {
                    signOut()
                }
            }
            true
        }

        if (profileImage != null) {
            Glide.with(this)
                .load(user?.photoUrl)
                .into(profileImage)
        }
    }

    private fun signOut() {
        auth.signOut()
        findNavController().navigate(R.id.action_nav_home_to_nav_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
