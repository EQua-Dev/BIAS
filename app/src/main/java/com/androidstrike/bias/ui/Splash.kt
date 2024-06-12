package com.androidstrike.bias.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.androidstrike.bias.R
import com.androidstrike.bias.databinding.FragmentSplashBinding
import com.androidstrike.bias.utils.Common

class Splash : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Handler().postDelayed({
//
//        },3000)
        // Inflate the layout for this fragment
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.logoImage.alpha = 0f
        binding.logoImage.animate().setDuration(2000).alpha(1f).withEndAction {
            if (!isFirstTime()) { //checks if it is first time launching app
                findNavController().navigate(R.id.action_splash_to_signIn)
            } else
                findNavController().navigate(R.id.action_splash_to_signUp)

        }

    }

    //boolean shared pref to store whether user is using the app for the 1st time
    private fun isFirstTime(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences(Common.sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(Common.firstTimeKey, true)
    }

}