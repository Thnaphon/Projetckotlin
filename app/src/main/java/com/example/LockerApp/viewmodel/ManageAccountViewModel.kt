package com.example.LockerApp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.LockerDatabase
import com.example.LockerApp.model.ManageAccount
import com.example.LockerApp.model.ManageAccountDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


class ManageAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val manageAccountDao: ManageAccountDao =
        LockerDatabase.getDatabase(application).ManageAccountDao()

    private val _manageAccounts = MutableStateFlow<List<ManageAccount>>(emptyList())
    val manageAccounts: StateFlow<List<ManageAccount>> = _manageAccounts.asStateFlow()

    init {
        getAllManageAccounts()
    }

    fun insertManageAccount(manageAccount: ManageAccount) {
        viewModelScope.launch {
            manageAccountDao.insertManageAccount(manageAccount)
            getAllManageAccounts()
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

    fun getManageAccountByAccountId(accountId: Int) {
        viewModelScope.launch {
            _manageAccounts.value = manageAccountDao.getManageAccountByAccountId(accountId)
        }
    }

    fun getManageAccountByByAccountId(byAccountId: Int) {
        viewModelScope.launch {
            _manageAccounts.value = manageAccountDao.getManageAccountByByAccountId(byAccountId)
        }
    }

    private fun getAllManageAccounts() {
        viewModelScope.launch {
            _manageAccounts.value = manageAccountDao.getAllManageAccounts()
        }
    }
}