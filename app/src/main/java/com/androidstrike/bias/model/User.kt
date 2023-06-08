package com.androidstrike.bias.model

import com.google.firebase.firestore.DocumentId

data class User(
    var name: String = "",
    var email: String = "",
    var department: String = "",
    var uid: String = "",
    val regNo: String = ""
//    var classes: HashMap<String,Map<String,Boolean>>? = null
)
