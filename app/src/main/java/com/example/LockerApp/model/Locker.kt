package com.example.LockerApp.model

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase



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
    val detail_item: String,
    val pic_item: String
)

// Entity สำหรับ Faces
@Entity(tableName = "faces")
data class Faces(
    @PrimaryKey(autoGenerate = true) val FaceID: Int = 0,
    val UserID: String, // เชื่อมโยงกับ User
    val FaceData: String // ข้อมูลของ Face
)

// Entity สำหรับ Account
@Entity(tableName = "account")
data class Account(
    @PrimaryKey val AccountID: String,
    val Name: String,
    val Phone: String,
    val Role: String,
    val CreatedDate: String // ควรใช้ date หรือ datetime format
)

// Entity สำหรับ User (เชื่อมโยงกับ Account) - เปลี่ยนความสัมพันธ์เป็น 1:1
@Entity(tableName = "user",
    foreignKeys = [ForeignKey(entity = Account::class,
        parentColumns = ["AccountID"],
        childColumns = ["AccountID"],
        onDelete = ForeignKey.CASCADE)]
)
data class User(
    @PrimaryKey val UserID: String,
    val AccountID: String,
    val id_face: Int // เชื่อมโยงกับ Faces
)

// Entity สำหรับ Service - เปลี่ยนความสัมพันธ์เป็น 1:1
@Entity(tableName = "service",
    foreignKeys = [ForeignKey(entity = Account::class,
        parentColumns = ["AccountID"],
        childColumns = ["AccountID"],
        onDelete = ForeignKey.CASCADE)]
)
data class Service(
    @PrimaryKey(autoGenerate = true) val ServiceID: Int = 0,
    val AccountID: String,
    val Username: String,
    val Password: String,
    val id_face: Int
)

// Entity สำหรับ UsageLocker
@Entity(tableName = "usage_locker",
    foreignKeys = [ForeignKey(entity = Locker::class,
        parentColumns = ["LockerID"],
        childColumns = ["LockerID"],
        onDelete = ForeignKey.CASCADE)]
)
data class UsageLocker(
    @PrimaryKey(autoGenerate = true) val UsageLockerID: Int = 0,
    val LockerID: Int,
    val UserID: String, // เชื่อมโยงกับ User
    val UsageTime: String // เวลาในการใช้งาน
)

// Entity สำหรับ Backup - เปลี่ยนความสัมพันธ์กับ User เป็น 1:N และกับ Service เป็น 1:N
@Entity(tableName = "backup",
    foreignKeys = [ForeignKey(entity = User::class,
        parentColumns = ["UserID"],
        childColumns = ["UserID"],
        onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Service::class,
            parentColumns = ["ServiceID"],
            childColumns = ["ServiceID"],
            onDelete = ForeignKey.CASCADE)]
)
data class Backup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val Time: String, // เวลาในการสำรองข้อมูล
    val Frequency: String,
    val UserID: String, // เปลี่ยน AccountID เป็น UserID
    val ServiceID: Int // เพิ่ม ServiceID เพื่อเชื่อมโยงกับ Service
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

// DAO สำหรับ Faces
@Dao
interface FacesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: Faces)

    @Query("SELECT * FROM faces WHERE UserID = :userId")
    suspend fun getFacesByUser(userId: String): Faces?
}

// DAO สำหรับ Account
@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Query("SELECT * FROM account")
    suspend fun getAllAccounts(): List<Account>
}

// DAO สำหรับ User
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user WHERE AccountID = :accountId")
    suspend fun getUserByAccount(accountId: String): User?
}

// DAO สำหรับ Service
@Dao
interface ServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Query("SELECT * FROM service WHERE AccountID = :accountId")
    suspend fun getServiceByAccount(accountId: String): Service?
}

// DAO สำหรับ UsageLocker
@Dao
interface UsageLockerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLocker(usageLocker: UsageLocker)

    @Query("SELECT * FROM usage_locker WHERE LockerID = :lockerId")
    suspend fun getUsageLockersByLocker(lockerId: Int): List<UsageLocker>
}

// DAO สำหรับ Backup
@Dao
interface BackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: Backup)

    @Query("SELECT * FROM backup WHERE UserID = :userId")
    suspend fun getBackupByUser(userId: String): List<Backup>

    @Query("SELECT * FROM backup WHERE ServiceID = :serviceId")
    suspend fun getBackupByService(serviceId: Int): List<Backup>
}

// Room Database สำหรับการรวม Entity และ DAO ทั้งหมด
@Database(entities = [Locker::class, Compartment::class, Faces::class, Account::class, User::class, Service::class, UsageLocker::class, Backup::class], version = 1)
abstract class LockerDatabase : RoomDatabase() {
    abstract fun lockerDao(): LockerDao
    abstract fun compartmentDao(): CompartmentDao
    abstract fun facesDao(): FacesDao
    abstract fun accountDao(): AccountDao
    abstract fun userDao(): UserDao
    abstract fun serviceDao(): ServiceDao
    abstract fun usageLockerDao(): UsageLockerDao
    abstract fun backupDao(): BackupDao

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
