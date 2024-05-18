package com.nullpointerexception.cityeye.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _messagesCount = MutableLiveData<Int>()
    var messagesCount: LiveData<Int> = _messagesCount

    fun setMessagesCount(count: Int) {
        _messagesCount.value = count
    }

    fun getMessagesCount(): MutableLiveData<Int> {
        return _messagesCount
    }

    private val _user = MutableLiveData<User>()
    var user: LiveData<User> = _user

    fun setUser(user: User) {
        _user.value = user
    }

    fun getUser(): MutableLiveData<User> {
        return _user
    }

    fun getUserFromDb() {
        viewModelScope.launch {
            val response = firebaseRepository.getUser(Firebase.auth.currentUser!!.uid)
            response?.let { setUser(it) }
        }
    }

    var badge: BadgeDrawable? = null

}