package com.nullpointerexception.cityeye.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nullpointerexception.cityeye.entities.Problem
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.firebase.FirebaseRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val firebaseRepository = FirebaseRepository()

    private val _user = MutableLiveData<User>()
    var user: LiveData<User> = _user

    private val _problems = MutableLiveData<List<Problem>>()

    private fun setProblems(problems: List<Problem>) {
        _problems.value = problems
    }

    fun getProblems(): MutableLiveData<List<Problem>> {
        return _problems
    }

    private fun setUser(user: User) {
        _user.value = user
    }

    fun getUser(): MutableLiveData<User> {
        return _user
    }

    fun getCurrentUser(uid: String) {
        viewModelScope.launch {
            val user = firebaseRepository.getUser(uid)
            if (user != null) {
                setUser(user)
            }
        }
    }

    fun getUserProblems() {
        viewModelScope.launch {
            val problems = firebaseRepository.getAllProblems().filter { it.uid == _user.value?.uid }
            setProblems(problems)
        }
    }
}