package com.androidstrike.bias.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.androidstrike.bias.R
import com.androidstrike.bias.utils.Common
import kotlinx.android.synthetic.main.fragment_splash.*

class Splash : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Handler().postDelayed({
//
//        },3000)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        logo_image.alpha = 0f
        logo_image.animate().setDuration(2000).alpha(1f).withEndAction {
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