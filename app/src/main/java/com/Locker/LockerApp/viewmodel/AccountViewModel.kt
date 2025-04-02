package com.Locker.LockerApp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.Locker.LockerApp.model.Account
import com.Locker.LockerApp.model.AccountDao
import com.Locker.LockerApp.model.LockerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val accountDao: AccountDao = LockerDatabase.getDatabase(application).accountDao()

    private val _userDetails = MutableLiveData<List<Account>>()
    val userDetails: LiveData<List<Account>> = accountDao.getAllAccounts().asLiveData()


    var existingServiceAccount by mutableStateOf<Account?>(null)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            existingServiceAccount = accountDao.getUserByName("service")

            if (existingServiceAccount == null) {
                val currentDate =
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val serviceAccount = Account(
                    Name = "service",
                    Phone = "0813028111",
                    Role = "service",
                    embedding = "",
                    CreatedDate = currentDate,
                    consent_pdda = true
                )

                accountDao.insertAccount(serviceAccount) // บันทึกและรับค่า ID กลับมา
                refreshUserDetails()
                accountDao.getAllAccounts()

            }
        }
    }



    fun insertAccount(account: Account) {
        viewModelScope.launch {
            accountDao.insertAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            accountDao.deleteAccount(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountDao.updateAccount(account)
        }
    }

    fun getAccountNameById(name: String): LiveData<Int> {
        return accountDao.getAccountNameById(name)
    }

    fun updateAccountFields(accountId: Int, name: String, phone: String, role: String) {
        viewModelScope.launch {
            accountDao.updateAccountFields(accountId, name, phone, role)
        }
    }

    fun refreshUserDetails() {
        viewModelScope.launch {
            _userDetails.value = accountDao.getAllAccounts().first() // ดึงค่าล่าสุดจาก Flow
        }
    }



}