package com.nullpointerexception.cityeye.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.nullpointerexception.cityeye.entities.Event
import com.nullpointerexception.cityeye.entities.MapItem
import com.nullpointerexception.cityeye.entities.ProblemType
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import com.nullpointerexception.cityeye.util.LocationUtil
import kotlinx.coroutines.launch
import java.io.File

class ProblemPreviewViewModel : ViewModel() {

    private val firebaseRepository: FirebaseRepository = FirebaseRepository()

    private val _coordinates = MutableLiveData<LatLng?>()
    val coordinates: MutableLiveData<LatLng?>
        get() = _coordinates

    private val _image = MutableLiveData<File>()
    val image: LiveData<File>
        get() = _image

    private val _address = MutableLiveData<String?>()
    val address: MutableLiveData<String?>
        get() = _address

    fun setCoordinates(latLng: LatLng?) {
        _coordinates.value = latLng
    }

    fun setImage(image: File) {
        _image.value = image
    }

    fun setAddress(address: String?) {
        _address.value = address
    }

    private val _response = MutableLiveData<Boolean>()
    val response: MutableLiveData<Boolean>
        get() = _response

    fun setResponse(response: Boolean) {
        _response.value = response
    }

    fun addProblem(
        context: Context,
        title: String,
        description: String,
        savedImageFile: File,
        location: LatLng,
        address: String,
        category: String,
        eventTitle: String?,
        markerAddress: String?
    ) {
        viewModelScope.launch {
            val response = firebaseRepository.addNormalProblem(
                context,
                title,
                description,
                savedImageFile,
                location,
                address,
                eventTitle,
                markerAddress
            )
            setResponse(response)
        }
    }

    fun getAddressFromLocation(context: Context, latLng: LatLng) {
        viewModelScope.launch {
            val address = LocationUtil.getAddressFromCo(context, latLng)
            setAddress(address)
        }
    }

    fun getProblemTypes() {
        viewModelScope.launch {
            setProblemType(firebaseRepository.getProblemTypes())
        }
    }

    private val _problemType = MutableLiveData<List<ProblemType>>()
    val problemType: MutableLiveData<List<ProblemType>>
        get() = _problemType

    fun setProblemType(response: List<ProblemType>) {
        _problemType.value = response
    }


    private val _events = MutableLiveData<List<Event>>()
    val events: MutableLiveData<List<Event>>
        get() = _events

    fun setEvents(response: List<Event>) {
        _events.value = response
    }

    fun fetchEvents() {
        viewModelScope.launch {
            setEvents(firebaseRepository.getEvents())
        }
    }

    private val _markers = MutableLiveData<List<MapItem>>()
    val markers: MutableLiveData<List<MapItem>>
        get() = _markers

    fun setMarkers(response: List<MapItem>) {
        _markers.value = response
    }

    fun fetchMarkers() {
        viewModelScope.launch {
            setMarkers(firebaseRepository.getMapItems())
        }
    }


}