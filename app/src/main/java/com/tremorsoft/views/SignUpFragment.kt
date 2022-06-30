/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.insertImage
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentSignUpBinding
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class SignUpFragment : BaseFragment() {

    private var _binding: FragmentSignUpBinding? = null
    private lateinit var auth: FirebaseAuth
    private val binding get() = _binding!!
    private var imageView: ImageView? = null
    private var imageBitmap: Bitmap? = null
    private var imageUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ):
            View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setProgressBar(binding.progressBar)
        (requireActivity() as MainActivity).supportActionBar?.hide()
        (requireActivity() as MainActivity).window.statusBarColor = Color.WHITE
        (requireActivity() as MainActivity).setDrawerLocked()

        imageView = binding.imageView

        with(binding) {
            imageCardView.setOnClickListener {
                uploadProfileImage()
            }
            emailCreateAccountButton.setOnClickListener {
                val email = binding.suEmail.text.toString()
                val password = binding.suPassword.text.toString()
                createAccount(email, password)
            }
            toSignInButton.setOnClickListener { toSignInFragment() }
        }

        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun toSignInFragment() {
        hideProgressBar()
        findNavController().navigate(R.id.action_nav_signUp_to_nav_login)
    }

    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        if (!validateForm()) {
            return
        }

        showProgressBar()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "createUserWithEmail:success")
                updateUI(auth.currentUser)
                sendEmailVerification()
            } else {
                // If sign in fails, display a message to the user.
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email sent to ${user.email} ", Toast.LENGTH_SHORT).show()
                    updateUI(auth.currentUser)
                    toSignInFragment()
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    @SuppressLint("RestrictedApi")
    private fun uploadProfileImage() {
        val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        uploadGetResult.launch(photoIntent)
    }

    private val uploadGetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            try {
                imageBitmap = it.data?.extras?.get("data") as Bitmap
                val bytes = ByteArrayOutputStream()
                imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

                val path: String = insertImage(context?.contentResolver, imageBitmap, "Title", null)
                imageUri = Uri.parse(path)
                imageView?.setImageURI(imageUri)

                Log.e("Activity", "Pick from Camera::>>> ")

                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val destination = File(
                    Environment.getExternalStorageDirectory().toString() + "/" +
                            getString(R.string.app_name), "IMG_$timeStamp.jpg"
                )

                val fo: FileOutputStream
                try {
                    destination.run {
                        createNewFile()
                        fo = FileOutputStream(this)
                    }
                    fo.run {
                        write(bytes.toByteArray())
                        close()
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun reload() {
        auth.currentUser!!.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUI(auth.currentUser)
            } else {
                Log.e(TAG, "reload", task.exception)
                Toast.makeText(context, "Failed to reload user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val name = binding.suName.text.toString()
        if (TextUtils.isEmpty(name)) {
            binding.suName.error = "Required."
            valid = false
        } else {
            binding.suName.error = null
        }

        val email = binding.suEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.suEmail.error = "Required."
            valid = false
        } else {
            binding.suEmail.error = null
        }

        val password = binding.suPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.suPassword.error = "Required."
            valid = false
        } else {
            binding.suPassword.error = null
        }

        val confirmPassword = binding.suConfirmPassword.text.toString()
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.suConfirmPassword.error = "Required."
            valid = false
        } else {
            binding.suConfirmPassword.error = null
        }

        if (!TextUtils.equals(password, confirmPassword)) {
            Toast.makeText(context, "Please make sure your passwords match.", Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val profileUpdates = userProfileChangeRequest {
                displayName = binding.suName.text.toString()
                photoUri = imageUri
            }
            user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated.")
                }
            }
        }
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
        private const val TAG = "SignUp"
    }
}