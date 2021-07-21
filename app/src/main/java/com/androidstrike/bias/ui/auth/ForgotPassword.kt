package com.androidstrike.bias.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.androidstrike.bias.R
import com.androidstrike.bias.utils.toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_forgot_password.*
import kotlinx.android.synthetic.main.fragment_forgot_password.view.*

class ForgotPassword : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_forgot_password, container, false)

        view.button_reset_password.setOnClickListener {
            val email = text_recover_email.text.toString().trim()

            //run validation on input
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                text_recover_email.error = "Valid Email Required"
                text_recover_email.requestFocus()
                return@setOnClickListener
            }

            //perform password reset using the firebase auth method
            pb_forgot_password.visibility = View.VISIBLE
            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener { task->
                    pb_forgot_password.visibility = View.GONE
                    if (task.isSuccessful){
                        activity?.toast("Check your email")
                    }else{
                        activity?.toast(task.exception?.message!!)
                    }
                }
            findNavController().navigate(R.id.action_forgotPassword_to_signIn)
        }
        return view
    }


}