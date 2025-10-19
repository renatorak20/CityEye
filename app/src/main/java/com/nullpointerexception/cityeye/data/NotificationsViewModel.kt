package com.nullpointerexception.cityeye.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _user = MutableLiveData<User>()
    var user: LiveData<User> = _user

    private fun setUser(user: User) {
        _user.value = user
    }

    fun getUserFromDatabase(uid: String) {
        viewModelScope.launch {
            val user = firebaseRepository.getUser(uid)
            if (user != null) {
                setUser(user)
            }
        }
    }
}