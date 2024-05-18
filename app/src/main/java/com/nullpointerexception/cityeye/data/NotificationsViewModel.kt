package com.nullpointerexception.cityeye.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.entities.UserNotification
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _user = MutableLiveData<User>()
    var user: LiveData<User> = _user

    fun setUser(user: User) {
        _user.value = user
    }

    fun getUser(): MutableLiveData<User> {
        return _user
    }

    private val _notifications = MutableLiveData<List<UserNotification>>()
    var notifications: LiveData<List<UserNotification>> = _notifications

    fun setNotifications(user: List<UserNotification>) {
        _notifications.value = user
    }

    fun getNotifications(): MutableLiveData<List<UserNotification>> {
        return _notifications
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