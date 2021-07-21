package com.androidstrike.bias.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

//toast function
fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun View.setOnSingleClickListener(l: View.OnClickListener) {
    setOnClickListener(OnSingleClickListener(l))
}

//enables a function only if clicked once, to avoid memory leaks
fun View.setOnSingleClickListener(l: (View) -> Unit) {
    setOnClickListener(OnSingleClickListener(l))
}

//function to change milliseconds to date format
fun getDate(milliSeconds: Long?, dateFormat: String?): String? {
    // Create a DateFormatter object for displaying date in specified format.
    val formatter = SimpleDateFormat(dateFormat)

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    val calendar: Calendar? = Calendar.getInstance()
    calendar?.setTimeInMillis(milliSeconds!!)
    return formatter.format(calendar?.getTime())
}
