package com.androidstrike.bias.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.androidstrike.bias.R
import com.androidstrike.bias.model.User
import com.androidstrike.bias.ui.Landing
import com.androidstrike.bias.utils.Common
import com.androidstrike.bias.utils.toast
import com.androidstrike.bias.utils.visible
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.toObject
import com.rengwuxian.materialedittext.MaterialEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class SignIn : Fragment() {

    lateinit var password: String
    lateinit var email: String
    lateinit var txtWelcomeNote: TextView
    lateinit var txtSignUp: TextView
    lateinit var txtForgotPassword: TextView
    lateinit var btnRegister: Button
    lateinit var etEmail: MaterialEditText
    lateinit var etPassword: MaterialEditText
    lateinit var pbLoading: ProgressBar




    var userName: String = "User"
    private var firebaseUser: FirebaseUser? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)

        txtWelcomeNote = view.findViewById(R.id.log_in_welcome_message)
        btnRegister = view.findViewById(R.id.log_in_btn_register)
        etEmail = view.findViewById(R.id.log_in_email)
        etPassword = view.findViewById(R.id.log_in_password)
        txtSignUp = view.findViewById(R.id.login_tv_signup)
        txtForgotPassword = view.findViewById(R.id.login_forgot_password)
        pbLoading = view.findViewById(R.id.pb_sign_in)

        //display user name if user has previously used app on device
        if (isFirstTime())
            txtWelcomeNote.text = "Welcome"
        else
            txtWelcomeNote.text = "Welcome Back $userName"

        btnRegister.setOnClickListener {
            //first run validation on input
            email = etEmail.text.toString()
            password = etPassword.text.toString()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid Email Required"
                etEmail.requestFocus()
            }
            if (password.isEmpty()) {
                etPassword.error = "Password Required"
                etPassword.requestFocus()
            } else //perform sign in
                signIn(email, password)
        }

        txtForgotPassword.setOnClickListener {
            //navigate to password reset screen
            findNavController().navigate(R.id.action_signIn_to_forgotPassword)
        }

        txtSignUp.setOnClickListener {
            //navigate to sign up screen
            findNavController().navigate(R.id.action_signIn_to_signUp)
        }

        return view
    }

    private fun signIn(email: String, password: String) {
        //implement sign in method
        pbLoading.visible(true)
        email.let { Common.mAuth.signInWithEmailAndPassword(it, password) }
            .addOnCompleteListener { it ->
                if (it.isSuccessful) {
                    //login success
                    Log.d("Equa", "signIn: ${Common.userId}")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val querySnapshot =Common.userCollectionRef.document(Common.userId.toString()).get().await()//.whereEqualTo("uid", Common.userId!!).get().await()

                            val sb = StringBuilder()
                                Common.userDepartment = querySnapshot["department"].toString()
//                            }
                            Log.d("Equa", "signIn: ${Common.userDepartment}")
                            withContext(Dispatchers.Main){
                                pbLoading.visible(false)

                                val i = Intent(requireContext(),Landing::class.java)
                                startActivity(i)
//                                findNavController().navigate(R.id.action_signIn_to_landing)
                            }


                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                pbLoading.visible(false)
                                activity?.toast(e.message.toString())
                                Log.d("Equa", "signIn: ${e.message.toString()}")
                            }
                        }
                    }

//                    Common.currentUser = firebaseUser?.uid!!
                } else {
                    pbLoading.visible(false)
                    activity?.toast(it.exception?.message.toString())
                }
            }
    }


    //boolean shared pref to store whether user is using the app for the 1st time
    private fun isFirstTime(): Boolean {
        //get the from the shared preference: user name and return true if user has previously launched app on device
        val sharedPref =
            requireActivity().getSharedPreferences(Common.sharedPrefName, Context.MODE_PRIVATE)
        userName = sharedPref.getString(Common.userNamekey, "User").toString()
        return sharedPref.getBoolean(Common.firstTimeKey, true)

    }


}