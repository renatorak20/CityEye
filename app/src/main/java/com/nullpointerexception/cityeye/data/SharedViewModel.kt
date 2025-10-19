package com.nullpointerexception.cityeye.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.maps.GeoApiContext
import com.google.maps.PlacesApi
import com.google.maps.model.PlaceType
import com.google.maps.model.PlacesSearchResponse
import com.google.maps.model.RankBy
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.entities.Event
import com.nullpointerexception.cityeye.entities.MapItem
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.maps.model.LatLng as NewLatLng


class SharedViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _myCoordinates = MutableLiveData<LatLng>()
    var myCoordinates: LiveData<LatLng> = _myCoordinates

    fun setMyCoordinates(newCoordinates: LatLng) {
        _myCoordinates.value = newCoordinates
    }

    fun getMyCoordinates(): MutableLiveData<LatLng> {
        return _myCoordinates
    }

    private val _problemCoordinates = MutableLiveData<List<LatLng>>()

    private fun setCoordinates(newCoordinates: List<LatLng>) {
        _problemCoordinates.value = newCoordinates
    }

    fun getCoordinates(): MutableLiveData<List<LatLng>> {
        return _problemCoordinates
    }

    var hasZoomedIn = false

    fun loadProblemCoordinates() {
        viewModelScope.launch {
            val problems = firebaseRepository.getAllProblems()
            val coordinates: MutableList<LatLng> = mutableListOf()

            for (problem in problems) {
                problem.location_lat?.toDouble()?.let {lat ->
                    problem.location_lon?.toDouble()?.let { long ->
                        LatLng(
                            lat, long
                        )
                    }
                }?.let {
                    coordinates.add(
                        it
                    )
                }
            }

            setCoordinates(
                coordinates
            )
        }
    }

    private val _places = MutableLiveData<PlacesSearchResponse>()

    fun getPlaces(): MutableLiveData<PlacesSearchResponse> {
        return _places
    }

    private fun setPlaces(sc: PlacesSearchResponse) {
        _places.value = sc
    }


    @SuppressLint("MissingPermission")
    fun getNearbyPlaces(activity: Activity, myLocation: LatLng?, met: Int) {

        viewModelScope.launch {
            val request: PlacesSearchResponse?
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
            Places.initialize(
                activity.applicationContext,
                activity.applicationContext.getString(R.string.maps_key)
            )

            val location = myLocation ?: withContext(Dispatchers.IO) {
                fusedLocationClient.lastLocation.await()
            }

            val apiContext: GeoApiContext = GeoApiContext.Builder()
                .apiKey(activity.applicationContext.getString(R.string.maps_key)).build()

            request = withContext(Dispatchers.IO) {
                val nLocation: LatLng = if (location is Location) {
                    LatLng(location.latitude, location.longitude)
                } else {
                    location as LatLng
                }
                PlacesApi.nearbySearchQuery(
                    apiContext, NewLatLng(nLocation.latitude, nLocation.longitude)
                ).radius(met).type(PlaceType.CAFE).type(PlaceType.RESTAURANT)
                    .rankby(RankBy.PROMINENCE).language("en").await()
            }

            setPlaces(request)
        }
    }

    private val _events = MutableLiveData<List<Event>>()
    var event: LiveData<List<Event>> = _events

    fun setEvents(events: List<Event>) {
        _events.value = events
    }

    fun getEvents(): MutableLiveData<List<Event>> {
        return _events
    }

    fun getAllEvents() {
        viewModelScope.launch {
            val events = firebaseRepository.getEvents()
            setEvents(events)
        }
    }


    private val _mapItems = MutableLiveData<List<MapItem>>()

    private fun setMapItems(items: List<MapItem>) {
        _mapItems.value = items
    }

    fun getMapItems(): MutableLiveData<List<MapItem>> {
        return _mapItems
    }

    fun getLatestMapItems() {
        viewModelScope.launch {
            val response = firebaseRepository.getMapItems()
            setMapItems(response)
        }
    }


    fun getMyFusedLocationNow(activity: Activity) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

            val location = withContext(Dispatchers.IO) {
                if (ActivityCompat.checkSelfPermission(
                        activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        activity.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 99
                    )
                } else {
                    fusedLocationClient.lastLocation.await()
                }
            }
            if (location != null && location !is Unit) {
                setMyCoordinates(LatLng((location as Location).latitude, location.longitude))
            }
        }
    }

    var areItemsLoaded = false


    private val _users = MutableLiveData<List<User>>()
    private fun setUsers(items: List<User>) {
        _users.value = items
    }

    fun getUsers(): MutableLiveData<List<User>> {
        return _users
    }

    fun getAllUsers() {
        viewModelScope.launch {
            setUsers(firebaseRepository.getAllUsers()!!)
        }
    }

}