package com.example.LockerApp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.model.LockerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val accountDao: AccountDao = LockerDatabase.getDatabase(application).accountDao()

    var userDetails: LiveData<List<Account>> = accountDao.getAllAccounts()
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
                    CreatedDate = currentDate
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

    fun getAccountNameById(accountId: Int): LiveData<String> {
        return accountDao.getAccountNameById(accountId)
    }

    fun updateAccountFields(accountId: Int, name: String, phone: String, role: String) {
        viewModelScope.launch {
            accountDao.updateAccountFields(accountId, name, phone, role)
        }
    }
    fun refreshUserDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            // ลบข้อมูลจาก cache (หากจำเป็น) และดึงข้อมูลใหม่
            val newDetails = accountDao.getAllAccounts()
            userDetails = newDetails
        }
    }

}
