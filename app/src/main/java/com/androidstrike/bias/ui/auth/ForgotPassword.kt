package com.androidstrike.bias.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.androidstrike.bias.R
import com.androidstrike.bias.databinding.FragmentForgotPasswordBinding
import com.androidstrike.bias.databinding.FragmentSplashBinding
import com.androidstrike.bias.utils.toast
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonResetPassword.setOnClickListener {
            val email = binding.textRecoverEmail.text.toString().trim()

            //run validation on input
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textRecoverEmail.error = "Valid Email Required"
                binding.textRecoverEmail.requestFocus()
                return@setOnClickListener
            }

            //perform password reset using the firebase auth method
            //binding.pbForgotPassword.visibility = View.VISIBLE
            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener { task->
                    //binding.pbForgotPassword.visible() = View.GONE
                    if (task.isSuccessful){
                        activity?.toast("Check your email")
                    }else{
                        activity?.toast(task.exception?.message!!)
                    }
                }
            findNavController().navigate(R.id.action_forgotPassword_to_signIn)
        }
    }


    //common function to handle progress bar visibility
    fun View.visible(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

}