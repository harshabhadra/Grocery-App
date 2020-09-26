package com.a99Spicy.a99spicy.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.a99Spicy.a99spicy.network.Address
import com.a99Spicy.a99spicy.network.Api
import com.a99Spicy.a99spicy.network.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

enum class Loading {
    SUCCESS, FAILED, PENDING
}

class ProfileViewModel : ViewModel() {

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val apiService = Api.retrofitService

    private var _profileMutableLiveData = MutableLiveData<Profile>()
    val profileLiveData: LiveData<Profile>
        get() = _profileMutableLiveData

    private var _loadingMutableLiveData = MutableLiveData<Loading>()
    val loadingLiveData:LiveData<Loading>
    get() = _loadingMutableLiveData

    init {
        _profileMutableLiveData.value = null
        _loadingMutableLiveData.value = null
    }

    //Get user profile
    fun getProfile(userId: String) {
        uiScope.launch {
            val responseDeferred = apiService.getSingleCustomerAsync(userId)
            try {
                _loadingMutableLiveData.value = Loading.PENDING
                val response = responseDeferred.await()
                _profileMutableLiveData.value = response
                _loadingMutableLiveData.value = Loading.SUCCESS
                Timber.e("Successfully received profile")
            } catch (e: Exception) {
                Timber.e("Failed to get user profile: ${e.message}")
                _profileMutableLiveData.value = null
                _loadingMutableLiveData.value = Loading.FAILED
            }
        }
    }

    fun resetLoadingData(){
        _loadingMutableLiveData.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}