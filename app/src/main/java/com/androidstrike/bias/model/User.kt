package com.androidstrike.bias.model

import com.google.firebase.firestore.DocumentId

data class User(
    var name: String? = null,
    var email: String? = null,
    var department: String? = null,
    var uid: String? = null,
//    var classes: HashMap<String,Map<String,Boolean>>? = null
)
