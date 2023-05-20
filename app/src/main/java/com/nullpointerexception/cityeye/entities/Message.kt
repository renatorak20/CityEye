package com.nullpointerexception.cityeye.entities

import java.io.Serializable

data class Message(
    val text: String? = null,
    val name: String? = null,
    val photoUrl: String? = null,
    val userID: String? = null,
    val time: Int? = null
) : Serializable
