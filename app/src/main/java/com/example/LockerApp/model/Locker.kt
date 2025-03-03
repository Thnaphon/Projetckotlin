package com.example.LockerApp.model

import android.content.Context
import androidx.camera.core.processing.SurfaceProcessorNode.In
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update


// Entity สำหรับ Locker
@Entity(tableName = "locker")
data class Locker(
    @PrimaryKey(autoGenerate = true) val LockerID: Int = 0,
    val Lockername:String,
    val detail: String,
    val status: String,
    val TokenTopic: String,
    val availableCompartment:String,
    val usedCompartment: String = "[]"

)

// Entity สำหรับ Compartment
@Entity(tableName = "compartment",
    foreignKeys = [ForeignKey(entity = Locker::class,
        parentColumns = ["LockerID"],
        childColumns = ["LockerID"],
        onDelete = ForeignKey.CASCADE)]
)

data class Compartment(
    @PrimaryKey(autoGenerate = true) val CompartmentID: Int=0,
    val number_compartment: Int,
    val Status: String,
    val LockerID: Int,
    val Name_Item: String,
    val pic_item: String
)


// Entity สำหรับ Account
@Entity(tableName = "account")
data class Account(
    @PrimaryKey(autoGenerate = true) val AccountID: Int = 0,
    val Name: String,
    val Phone: String,
    val Role: String,
    val embedding: String,
    val CreatedDate: String // ควรใช้ date หรือ datetime format
)






// Entity สำหรับ UsageLocker
@Entity(tableName = "usage_locker",
    foreignKeys = [
        ForeignKey(
            entity = Locker::class,
            parentColumns = ["LockerID"],
            childColumns = ["LockerID"],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["AccountID"],
            childColumns = ["AccountID"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Compartment::class,
            parentColumns = ["CompartmentID"],
            childColumns = ["CompartmentID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UsageLocker(
    @PrimaryKey(autoGenerate = true) val UsageLockerID: Int = 0,
    val LockerID: Int,
    val CompartmentID: Int ,
    val AccountID: Int,
    val UsageTime: String,
    val Usage : String,
    val Status: String

)

// Entity สำหรับ UsageLocker
@Entity(tableName = "manage_locker",
    foreignKeys = [
        ForeignKey(
            entity = Locker::class,
            parentColumns = ["LockerID"],
            childColumns = ["LockerID"],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["AccountID"],
            childColumns = ["AccountID"],
            onDelete = ForeignKey.CASCADE
        )

    ]
)
data class ManageLocker(
    @PrimaryKey(autoGenerate = true) val ManageLockerID: Int = 0,
    val LockerID: Int,
    val AccountID: Int,
    val UsageTime: String,
    val Usage : String,
    val Status: String

)

// Entity สำหรับ UsageLocker
@Entity(tableName = "manage_account",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["AccountID"],
            childColumns = ["AccountID"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["AccountID"],
            childColumns = ["ByAccountID"],
            onDelete = ForeignKey.CASCADE
        )

    ]
)
data class ManageAccount(
    @PrimaryKey(autoGenerate = true) val ManageAccount: Int = 0,
    val AccountID : Int,
    val ByAccountID : Int,
    val UsageTime: String,
    val Usage : String

)



// Entity สำหรับ Backup
@Entity(tableName = "backup_settings")
data class BackupSettings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val frequency: String, // เช่น "Daily", "Weekly"
    val backupTime: String, // เวลา backup เช่น "02:00 AM"
    val lastBackupDate: String? = null // เก็บวันที่ backup ล่าสุด
)


// DAO สำหรับ Backup
@Dao
interface BackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBackupSettings(settings: BackupSettings)

    @Query("SELECT * FROM backup_settings LIMIT 1")
    suspend fun getBackupSettings(): BackupSettings?

    @Query("UPDATE backup_settings SET lastBackupDate = :date WHERE id = :id")
    suspend fun updateLastBackupDate(id: Int, date: String)
}

// DAO สำหรับ Locker
@Dao
interface LockerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocker(locker: Locker)

    @Query("SELECT * FROM locker")
    suspend fun getAllLockers(): List<Locker>

    @Query("SELECT COUNT(*) FROM locker")
    suspend fun getLockerCount(): Int

    @Query("SELECT TokenTopic FROM locker WHERE LockerID = :lockerId")
    suspend fun getMqttTopicByLockerId(lockerId: Int): String?

    @Query("SELECT LockerID FROM locker WHERE TokenTopic = :TokenTopic")
    suspend fun getLockerIdcByTopic(TokenTopic: String): Int?

    @Query("SELECT availableCompartment FROM locker WHERE LockerID = :lockerId")
    suspend fun getavailableCompartmentByLockerId(lockerId: Int): String?

    @Query("SELECT * FROM locker WHERE TokenTopic = :topic")
    suspend fun getLockerByTopic(topic: String): Locker?

    @Query("UPDATE locker SET status = :newStatus WHERE LockerID = :lockerID")
    suspend fun updateLockerStatus(lockerID: Int, newStatus: String)

    @Query("SELECT * FROM locker ORDER BY LockerID DESC LIMIT 1")
    suspend fun getLastInsertedLocker(): Locker?
}

// DAO สำหรับ Compartment
@Dao
interface CompartmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompartment(compartment: Compartment): Long

    @Query("SELECT * FROM compartment WHERE LockerID = :lockerId")
    suspend fun getCompartmentsByLocker(lockerId: Int): List<Compartment>

    @Query("SELECT LockerID FROM compartment WHERE CompartmentID = :compartmentId LIMIT 1")
    suspend fun getLockerIdByCompartmentId(compartmentId: Int): Int?

    @Query("UPDATE compartment SET Name_Item = :newName, pic_item = :newPic WHERE CompartmentID = :compartmentID AND LockerID = :lockerID")
    suspend fun updateCompartment(
        compartmentID: Int,
        newName: String,
        newPic: String, // ชื่อไฟล์ของภาพใหม่
        lockerID: Int
    )

    @Query("UPDATE compartment SET Status = :newStatus WHERE CompartmentID = :compartmentID AND LockerID = :lockerID")
    suspend fun updateCompartmentStatus(compartmentID: Int, newStatus: String, lockerID: Int)

    @Query("SELECT COUNT(*) FROM locker WHERE LockerID = :lockerID")
    suspend fun checkLockerExists(lockerID: Int): Boolean


    @Query("SELECT CompartmentID FROM compartment")
    suspend fun getAllCompartmentIds(): List<Int>

    @Query("DELETE FROM compartment WHERE LockerID = :lockerId AND CompartmentID = :compartmentId")
    suspend fun deleteCompartment(lockerId: Int, compartmentId: Int)

    @Query("SELECT CompartmentID FROM compartment ORDER BY CompartmentID DESC LIMIT 1")
    fun getLastCompartmentId(): LiveData<Int?>

    @Query("SELECT number_compartment FROM compartment WHERE LockerID = :lockerId")
    suspend fun getAllCompartmentsNum(lockerId: Int): List<Int>

    @Query("SELECT CompartmentID FROM compartment WHERE LockerID = :lockerId AND number_compartment = :number_compartment")
    suspend fun getCompartmentId(lockerId: Int, number_compartment: Int): Int
}



// DAO สำหรับ Account
@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Query("SELECT * FROM account")
    fun getAllAccounts(): LiveData<List<Account>>

    @Query("SELECT * FROM account")
    suspend fun getAllAccountsSync(): List<Account>

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM account WHERE Name = :name LIMIT 1")
    suspend fun getUserByName(name: String): Account?

    @Update
    suspend fun updateAccount(account: Account)

    @Query("SELECT * FROM account WHERE AccountID = :accountID LIMIT 1")
    suspend fun getUserAccountID(accountID: Int): Account?

    @Query("SELECT Name FROM Account WHERE AccountID = :accountId LIMIT 1")
    fun getAccountNameById(accountId: Int): LiveData<String>

    @Query("UPDATE account SET Name = :name, Phone = :phone, Role = :role WHERE AccountID = :accountId")
    suspend fun updateAccountFields(accountId: Int, name: String, phone: String, role: String)
}






// DAO สำหรับ UsageLocker
@Dao
interface UsageLockerDao {
    // ดึงข้อมูลทั้งหมดจาก UsageLocker
    @Query("SELECT * FROM usage_locker ORDER BY UsageTime DESC")
    fun getAllUsageLockers(): LiveData<List<UsageLocker>>

    // เพิ่มข้อมูลใหม่
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usageLocker: UsageLocker)

    // อัพเดตข้อมูล
    @Update
    suspend fun update(usageLocker: UsageLocker)

    // ลบข้อมูล
    @Delete
    suspend fun delete(usageLocker: UsageLocker)
}

@Dao
interface ManageLockerDao {

    // เพิ่มข้อมูลเข้าในตาราง ManageLocker
    @Insert
    suspend fun insert(manageLocker: ManageLocker)

    // อัพเดตข้อมูลในตาราง ManageLocker
    @Update
    suspend fun update(manageLocker: ManageLocker)

    // ลบข้อมูลในตาราง ManageLocker
    @Delete
    suspend fun delete(manageLocker: ManageLocker)

    // ค้นหาข้อมูลทั้งหมดในตาราง ManageLocker
    @Query("SELECT * FROM manage_locker")
    fun getAllManageLockers(): LiveData<List<ManageLocker>>


    // ค้นหาข้อมูลโดย AccountID
    @Query("SELECT * FROM manage_locker WHERE AccountID = :accountId")
    suspend fun getManageLockerByAccountId(accountId: Int): List<ManageLocker>

    // ค้นหาข้อมูลที่มีการใช้งานโดย Status
    @Query("SELECT * FROM manage_locker WHERE Status = :status")
    suspend fun getManageLockerByStatus(status: String): List<ManageLocker>

    // ค้นหาข้อมูลที่มีการใช้งานโดยช่วงเวลาการใช้งาน
    @Query("SELECT * FROM manage_locker WHERE UsageTime BETWEEN :startTime AND :endTime")
    suspend fun getManageLockerByUsageTime(startTime: String, endTime: String): List<ManageLocker>
}

@Dao
interface ManageAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManageAccount(manageAccount: ManageAccount): Long

    @Update
    suspend fun updateManageAccount(manageAccount: ManageAccount)

    @Delete
    suspend fun deleteManageAccount(manageAccount: ManageAccount)

    @Query("SELECT * FROM manage_account WHERE AccountID = :accountId")
    suspend fun getManageAccountByAccountId(accountId: Int): List<ManageAccount>

    @Query("SELECT * FROM manage_account WHERE ByAccountID = :byAccountId")
    suspend fun getManageAccountByByAccountId(byAccountId: Int): List<ManageAccount>

    @Query("SELECT * FROM manage_account ORDER BY UsageTime DESC")
    suspend fun getAllManageAccounts(): List<ManageAccount>

}


// Room Database สำหรับการรวม Entity และ DAO ทั้งหมด
@Database(entities = [Locker::class, Compartment::class, Account::class,  UsageLocker::class,ManageLocker::class,ManageAccount::class,BackupSettings::class ], version = 1)
abstract class LockerDatabase : RoomDatabase() {
    abstract fun lockerDao(): LockerDao
    abstract fun compartmentDao(): CompartmentDao
    abstract fun accountDao(): AccountDao
    abstract fun usageLockerDao(): UsageLockerDao
    abstract fun backupDao(): BackupDao
    abstract fun ManageLockerDao(): ManageLockerDao
    abstract fun ManageAccountDao(): ManageAccountDao


    companion object {
        @Volatile
        private var INSTANCE: LockerDatabase? = null

        fun getDatabase(context: Context): LockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockerDatabase::class.java,
                    "locker_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
