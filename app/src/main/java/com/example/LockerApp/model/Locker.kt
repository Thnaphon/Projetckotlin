package com.example.LockerApp.model

import android.content.Context
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
    val topic_mqtt: String,
    val availableCompartment:String

)

// Entity สำหรับ Compartment
@Entity(tableName = "compartment",
    foreignKeys = [ForeignKey(entity = Locker::class,
        parentColumns = ["LockerID"],
        childColumns = ["LockerID"],
        onDelete = ForeignKey.CASCADE)]
)
data class Compartment(
    @PrimaryKey(autoGenerate = true) val CompartmentID: Int = 0,
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
//        ForeignKey(
//            entity = Account::class,
//            parentColumns = ["AccountID"],
//            childColumns = ["AccountID"],
//            onDelete = ForeignKey.CASCADE
//        ),
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
//    val AccountID: Int,
    val UsageTime: String,
    val Usage : String,
    val Status: String

)



// DAO สำหรับ Locker
@Dao
interface LockerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocker(locker: Locker)

    @Query("SELECT * FROM locker")
    suspend fun getAllLockers(): List<Locker>

    @Query("SELECT COUNT(*) FROM locker")
    suspend fun getLockerCount(): Int

    @Query("SELECT topic_mqtt FROM locker WHERE LockerID = :lockerId")
    suspend fun getMqttTopicByLockerId(lockerId: Int): String?
}

// DAO สำหรับ Compartment
@Dao
interface CompartmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompartment(compartment: Compartment)

    @Query("SELECT * FROM compartment WHERE LockerID = :lockerId")
    suspend fun getCompartmentsByLocker(lockerId: Int): List<Compartment>

    @Query("SELECT LockerID FROM compartment WHERE CompartmentID = :compartmentId LIMIT 1")
    suspend fun getLockerIdByCompartmentId(compartmentId: Int): Int?
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

// DAO สำหรับ Backup


// Room Database สำหรับการรวม Entity และ DAO ทั้งหมด
@Database(entities = [Locker::class, Compartment::class, Account::class,  UsageLocker::class], version = 1)
abstract class LockerDatabase : RoomDatabase() {
    abstract fun lockerDao(): LockerDao
    abstract fun compartmentDao(): CompartmentDao
    abstract fun accountDao(): AccountDao
    abstract fun usageLockerDao(): UsageLockerDao

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
