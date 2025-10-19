package com.nullpointerexception.cityeye.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.nullpointerexception.cityeye.R
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocationUtil {

    suspend fun getAddressFromCo(context: Context, latLng: LatLng): String? =
        suspendCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())

            try {
                val addressList: MutableList<Address>? =
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    continuation.resume(address.getAddressLine(0))
                } else {
                    continuation.resume(null)
                }
            } catch (e: IOException) {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.geocoderError),
                    Toast.LENGTH_LONG
                ).show()
                continuation.resume(null)
            }
        }
}