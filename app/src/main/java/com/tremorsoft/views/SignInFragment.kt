/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentLoginBinding

class SignInFragment : BaseFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setProgressBar(binding.progressBar)
        (requireActivity() as MainActivity).supportActionBar?.hide()
        (requireActivity() as MainActivity).window.statusBarColor = Color.WHITE
        (requireActivity() as MainActivity).setDrawerLocked()
        // Buttons
        with(binding) {
            emailSignInButton.setOnClickListener {
                val email = binding.siEmail.text.toString()
                val password = binding.siPassword.text.toString()
                signInWithEmailAndPassword(email, password)
            }

            toSignUpButton.setOnClickListener { signUp() }
            forgotPasswordButton.setOnClickListener {
                val email = binding.siEmail.text.toString()
                forgotPassword(email)
            }
            googleSignIn.setOnClickListener { signInWithGoogle() }
        }

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            hideProgressBar()
            auth.signOut()
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm("signIn")) {
            return
        }

        showProgressBar()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI()

                    if (user != null) {
                        if (user.isEmailVerified) {
                            findNavController().navigate(R.id.action_nav_login_to_nav_home)
                        } else {
                            auth.signOut()
                            Toast.makeText(context, "Please make sure you have verified your account.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        context, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI()
                }
                hideProgressBar()
            }
    }

    private fun forgotPassword(email: String) {

        if (!validateForm("passReset")) {
            return
        }

        showProgressBar()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "sendPasswordResetEmail:success")
                    Toast.makeText(
                        context,
                        "Password reset email sent to ${binding.siEmail.text.toString()} ",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(
                        context,
                        "Failed to send password reset email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                hideProgressBar()
            }
    }

    private val googleSignInGetResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e)
                }
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgressBar()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    auth.currentUser
                    updateUI()
                    findNavController().navigate(R.id.action_nav_login_to_nav_home)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    val view = binding.mainLayout
                    Snackbar.make(view, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    updateUI()
                }

                hideProgressBar()
            }
    }

    private fun signInWithGoogle() {
        val googleSignInIntent = googleSignInClient.signInIntent
        googleSignInGetResult.launch(googleSignInIntent)
    }

    private fun signUp() {
        findNavController().navigate(R.id.action_nav_login_to_nav_signUp)
    }

    private fun validateForm(button: String): Boolean {
        var valid = true

        val email = binding.siEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.siEmail.error = "Required."
            valid = false
        } else {
            binding.siEmail.error = null
        }

        if (TextUtils.equals(button, "signIn")) {
            val password = binding.siPassword.text.toString()
            if (TextUtils.isEmpty(password)) {
                binding.siPassword.error = "Required."
                valid = false
            } else {
                binding.siPassword.error = null
            }
        }

        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isEmailVerified) {
            Toast.makeText(context, "Please make sure you have verified the email address.",
                Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }

    private fun updateUI() {
        hideProgressBar()
    }

    override fun onStop() {
        super.onStop()
        hideProgressBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}