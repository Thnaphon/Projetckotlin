package com.Locker.LockerApp.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.Locker.LockerApp.model.Compartment
import com.Locker.LockerApp.model.CompartmentDao
import com.Locker.LockerApp.model.Locker
import com.Locker.LockerApp.model.LockerDao
import com.Locker.LockerApp.service.MqttService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.eclipse.paho.client.mqttv3.MqttClient


class LockerViewModel(private val lockerDao: LockerDao,private val compartmentDao: CompartmentDao) : ViewModel() {

    private val mqttService = MqttService()

    private val _topicMqttList = MutableStateFlow<List<String>>(emptyList())
    val topicMqttList: StateFlow<List<String>> get() = _topicMqttList

    // ตัวแปร mqttClient ที่จะใช้งาน
    val mqttClient: MqttClient? = mqttService.getClient()

    // สถานะการเชื่อมต่อ
    val connectionStatus: StateFlow<String> = mqttService.connectionStatus

    // ข้อความที่ได้รับ
//    val receivedMessage: StateFlow<String> = mqttService.receivedMessage

    private val _compartments = MutableStateFlow<List<Compartment>>(emptyList())
    val compartments: StateFlow<List<Compartment>> = _compartments


    private val _lockers = MutableStateFlow<List<Locker>>(emptyList()) // กำหนดค่าเริ่มต้น
    val lockers: StateFlow<List<Locker>> = _lockers

    private val _lockerCount = MutableStateFlow(0) // กำหนดค่าเริ่มต้น
    val lockerCount: StateFlow<Int> = _lockerCount

    private val _lastCompartmentId = MutableLiveData<Int?>()
    val lastCompartmentId: LiveData<Int?> = _lastCompartmentId

    private val _compartmentsNumber = MutableLiveData<List<Int>>()
    val compartmentsNumber: LiveData<List<Int>> get() = _compartmentsNumber


    init {
        loadLockers()

    }

    private val _uploadResult = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = _uploadResult

    private fun loadLockers() {
        viewModelScope.launch {
            try {
                val lockersFromDb = lockerDao.getAllLockers() // ดึงข้อมูลจากฐานข้อมูล
                _lockers.value = lockersFromDb // อัพเดตค่าใน _lockers
                _lockerCount.value = lockerDao.getLockerCount() // อัพเดตจำนวนล็อคเกอร์
            } catch (e: Exception) {
                Log.e("LockerViewModel", "Error loading lockers", e)
            }
        }
    }

    fun loadLockersUi() {
        viewModelScope.launch {
            try {
                val lockersFromDb = lockerDao.getAllLockers() // ดึงข้อมูลจากฐานข้อมูล
                _lockers.value = lockersFromDb // อัพเดตค่าใน _lockers
                _lockerCount.value = lockerDao.getLockerCount() // อัพเดตจำนวนล็อคเกอร์
            } catch (e: Exception) {
                Log.e("LockerViewModel", "Error loading lockers", e)
            }
        }
    }


    private fun loadTopics() {
        viewModelScope.launch {
            val topics = lockerDao.getAllLockers().map { it.TokenTopic }
            _topicMqttList.value = topics.filterNotNull() // เก็บเฉพาะ topic ที่ไม่เป็น null
        }
    }


    // ฟังก์ชันเพิ่มล็อคเกอร์ใหม่
// ฟังก์ชันเพิ่มล็อคเกอร์ใหม่
    fun addLocker(Lockername: String,detail: String, receivedTopic: String, availableCompartment:String,onComplete: () -> Unit) {

        val status = "available"
        val topicMqtt = receivedTopic    // สร้าง topic_mqtt ใหม่
        val availableCompartment = availableCompartment

        val newLocker = Locker(Lockername=Lockername,detail = detail, status = status, TokenTopic = topicMqtt,availableCompartment=availableCompartment )
        viewModelScope.launch {
            lockerDao.insertLocker(newLocker)
            // อัปเดตจำนวนล็อคเกอร์หลังจากเพิ่ม
            _lockerCount.value = _lockerCount.value + 1
            // รีเฟรชข้อมูลล็อคเกอร์ใหม่
            loadLockers()
            onComplete()

        }
    }

    fun getCompartmentsByLocker(lockerId: Int): Flow<List<Compartment>> = flow {
        val compartments = if (lockerId == 0) {
            compartmentDao.getAllcompartments() // ดึงทุก compartment
        } else {
            compartmentDao.getCompartmentsByLocker(lockerId) // ดึงเฉพาะ compartment ของ lockerId ที่เลือก
        }
        emit(compartments) // emit ค่าออกไป
    }


    // ฟังก์ชันสำหรับเพิ่ม Compartment
    fun addCompartment(compartment: Compartment,lockerId: Int) {
        viewModelScope.launch {
            compartmentDao.insertCompartment(compartment)
            loadCompartments(lockerId)
            loadCompartmentsByLockerNumber(lockerId)
        }
    }
    // ฟังก์ชันใน ViewModel ที่จะเรียกใน Coroutine



    fun updateCompartmentStatus(compartmentID: Int, newStatus: String, lockerID: Int,Usage_By: String) {
        viewModelScope.launch {
            try {
                // ตรวจสอบว่า LockerID มีอยู่ในตาราง locker หรือไม่
                val lockerExists = compartmentDao.checkLockerExists(lockerID)
                if (lockerExists) {
                    // ถ้ามี LockerID ในตาราง Locker ให้ทำการอัปเดตสถานะ
                    compartmentDao.updateCompartmentStatus(compartmentID, newStatus, lockerID,Usage_By)
                    loadCompartments(lockerID)
                } else {
                    Log.e("LockerViewModel", "LockerID does not exist")
                }
            } catch (e: Exception) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                Log.e("LockerViewModel", "Error updating compartment status", e)
            }
        }


    }



    fun getMqttTopicFromDatabase(lockerId: Int): Flow<String?> = flow {
        val mqttTopic = lockerDao.getMqttTopicByLockerId(lockerId)
        emit(mqttTopic)

    }

    fun getavailableCompartmentByLockerId(lockerId: Int): Flow<String?> = flow {
        val availableCompartment = lockerDao.getavailableCompartmentByLockerId(lockerId)
        emit(availableCompartment)
    }
    fun parseAvailableCompartment(availableCompartment: String?): List<Int> {
        return try {
            // ถ้า availableCompartment เป็น null ให้คืนค่าเป็น List ว่าง
            availableCompartment?.let {
                val gson = Gson()
                gson.fromJson(it, Array<Int>::class.java).toList()
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList() // ถ้ามีข้อผิดพลาดก็จะคืนค่าเป็น List ว่าง
        }
    }
    fun getMqttTopicForCompartment(lockerId: Int): Flow<String?> {
        return flow {
            // ดึง LockerID จาก Compartment


            // ถ้า lockerId ไม่เป็น null, ให้ดึง MQTT topic
            if (lockerId != null) {
                val mqttTopic = lockerDao.getMqttTopicByLockerId(lockerId)
                emit(mqttTopic)
            } else {
                emit(null) // คืนค่า null ถ้าไม่พบ LockerID
            }
        }.flowOn(Dispatchers.IO)
    }

    fun updateLockerStatus(lockerID: Int, newStatus: String) {
        viewModelScope.launch {
            lockerDao.updateLockerStatus(lockerID, newStatus)
        }
    }

    // ใน ViewModel


    fun getAllLockers() {
        viewModelScope.launch {
            // ดึงข้อมูลจาก Dao หรือ Repository
            val lockersFromDb = lockerDao.getAllLockers() // สมมติว่า getAllLockers() คืนค่า List<Locker>
            _lockers.value = lockersFromDb
        }
    }





    fun loadCompartments(lockerId: Int) {
        viewModelScope.launch {
            try {
                val compartmentsFromDb = if (lockerId == 0) {

                    compartmentDao.getAllcompartments()

                } else {
                    // ถ้า lockerId ไม่ใช่ 0 ให้ดึงข้อมูลตาม lockerId
                    compartmentDao.getCompartmentsByLocker(lockerId)
                }

                // อัพเดตข้อมูลของ compartments ใน StateFlow
                _compartments.value = compartmentsFromDb
            } catch (e: Exception) {
                Log.e("LockerViewModel", "Error loading compartments", e)
            }
        }
    }


    // เพิ่มฟังก์ชันนี้ใน LockerViewModel
    private val _compartmentIds = mutableStateOf<List<Compartment>>(emptyList())
    val compartmentIds: State<List<Compartment>> = _compartmentIds

    fun getAllCompartmentIds(): LiveData<List<Int>> {
        return liveData {
            val compartmentIds = compartmentDao.getAllCompartmentIds()
            emit(compartmentIds)
        }
    }
    fun getAllCompartmentNumber(lockerID:Int): LiveData<List<Int>> {
        return liveData {
            val compartmentNumber = if (lockerID == 0) {
                compartmentDao.getAllCompartmentsNum() // ดึงหมายเลขของทุก compartment
            } else {
                compartmentDao.getAllCompartmentsNumbyLockerId(lockerID)
            }
            emit(compartmentNumber)
        }
    }

    fun getLatestLocker(onResult: (Locker?) -> Unit) {
        viewModelScope.launch {
            val latestLocker = lockerDao.getLastInsertedLocker()
            loadLockers()
            onResult(latestLocker)

        }
    }
    fun getLatestCompartment(onResult: (Compartment?) -> Unit) {
        viewModelScope.launch {
            val latestCompartment = compartmentDao.getLastInsertedCompartment()
            onResult(latestCompartment)

        }
    }
    fun delteCompartment(lockerID:Int,compartmentID:Int){
        viewModelScope.launch{
            compartmentDao.deleteCompartment(lockerID, compartmentID)
            loadCompartments(lockerID)
            loadCompartmentsByLockerNumber(lockerID)
        }
    }

    fun updateCompartment(compartmentID: Int, newName: String, newPic: String, lockerID: Int,numcompartment:Int,newStatus:String) {
        // เรียกใช้ภายใน viewModelScope เพื่อทำงานใน Coroutine
        viewModelScope.launch {
            Log.d("Updatecomparatment","$compartmentID/ $newName/$newPic,$lockerID")
            compartmentDao.updateCompartment(compartmentID, newName, newPic, lockerID,newStatus,numcompartment)
            loadCompartments(lockerID)
            loadCompartmentsByLockerNumber(lockerID)


        }
    }

    fun loadCompartmentsByLockerNumber(lockerId: Int) {
        viewModelScope.launch {
            // เรียกใช้ฟังก์ชันใน DAO เพื่อดึงข้อมูล number_compartment
            _compartmentsNumber.postValue(compartmentDao.getAllCompartmentsNumbyLockerId(lockerId))
        }
    }



    fun getCompartmentId(lockerId: Int,number_compartment:Int): Flow<Int> = flow {
        val compartments = compartmentDao.getCompartmentId(lockerId, number_compartment)
        emit(compartments)
    }

    fun deleteLocker(lockerId: Int) {
        viewModelScope.launch {
            lockerDao.deleteLocker(lockerId)
            loadLockers()
            loadCompartmentsByLockerNumber(lockerId)
        }
    }
    fun updateLocker(lockerID: Int, newStatus: String, newNamelocker: String, newDetail: String){
        viewModelScope.launch {
            lockerDao.updateLocker(lockerID, newStatus, newNamelocker, newDetail)
            loadLockers()
        }
    }

    val allLockerToken: LiveData<List<String>> = liveData {
        emit(lockerDao.getAllLockerToekns())
    }

    fun getCompartmentBycompartmentId(compartmentID: Int): Flow<List<Compartment>> = flow{

        val CompartmentById = compartmentDao.getCompartmentsBycompartmentId(compartmentID)
        emit(CompartmentById)
    }

    fun getLockername(lockerId: Int): Flow<String?> = flow {
        val mqttTopic = lockerDao.getNamelocker(lockerId)
        emit(mqttTopic)

    }


}