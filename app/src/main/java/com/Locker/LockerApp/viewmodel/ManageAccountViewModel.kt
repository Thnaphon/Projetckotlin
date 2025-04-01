package com.Locker.LockerApp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Locker.LockerApp.model.LockerDatabase
import com.Locker.LockerApp.model.ManageAccount
import com.Locker.LockerApp.model.ManageAccountDao
import kotlinx.coroutines.launch


class ManageAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val manageAccountDao: ManageAccountDao =
        LockerDatabase.getDatabase(application).ManageAccountDao()



    private val _manageAccounts = MutableLiveData<List<ManageAccount>>()
    val manageAccounts: LiveData<List<ManageAccount>> = _manageAccounts
    init {
        getAllManageAccounts()

    }

    fun insertManageAccount(manageAccount: ManageAccount) {
        viewModelScope.launch {
            manageAccountDao.insertManageAccount(manageAccount)
            getAllManageAccounts()
            Log.d("Insert","Success")
        }
    }

    fun updateManageAccount(manageAccount: ManageAccount) {
        viewModelScope.launch {
            manageAccountDao.updateManageAccount(manageAccount)
            getAllManageAccounts()
        }
    }

    fun deleteManageAccount(manageAccount: ManageAccount) {
        viewModelScope.launch {
            manageAccountDao.deleteManageAccount(manageAccount)
            getAllManageAccounts()
        }
    }



    fun getAllManageAccounts() {
        viewModelScope.launch {
            manageAccountDao.getAllAccounts().collect { accounts ->
                _manageAccounts.value = accounts
            }
        }
    }
}