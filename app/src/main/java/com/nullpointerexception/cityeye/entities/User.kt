package com.nullpointerexception.cityeye.entities

data class User(
    val uid: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val provider: String? = null,
    val problems: List<String>? = listOf()
)