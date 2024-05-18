package com.nullpointerexception.cityeye.entities

import com.google.firebase.Timestamp

data class WebUser(
    val id: String?,
    val name: String?,
    val email: String?,
    val role: String?,
    val lastActive: Timestamp?,
    val city: String?
)
