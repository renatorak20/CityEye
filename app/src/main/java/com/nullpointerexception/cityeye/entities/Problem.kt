package com.nullpointerexception.cityeye.entities


data class Problem(
    var problemID: String? = null,
    val title: String? = null,
    val description: String? = null,
    var uid: String? = null,
    val address: String? = null,
    val imageName: String? = null,
    val epoch: Long? = null,
    val solved: Boolean? = null,
    val location_lat: String? = null,
    val location_lon: String? = null,
    val eventId: String? = null,
    val markerId: String? = null,
)

