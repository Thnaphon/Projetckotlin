package com.example.LockerApp.view

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.util.Pair
import com.example.LockerApp.model.Account
import com.example.LockerApp.model.AccountDao
import com.example.LockerApp.model.LockerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

interface FaceClassifier {
    fun register(name: String?, phone: String? , role: String?, recognition: Recognition?)

    fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): Recognition?
    class Recognition {
        val id: String?

        /** Display name for the recognition.  */
        val title: String?

        // A sortable score for how good the recognition is relative to others. Lower should be better.
        val distance: Float?
        var embeeding: Any?

        /** Optional location within the source image for the location of the recognized face.  */
        private var location: RectF?
        var crop: Bitmap?

        constructor(
            id: String?, title: String?, distance: Float?, location: RectF?
        ) {
            this.id = id
            this.title = title
            this.distance = distance
            this.location = location
            embeeding = null
            crop = null
        }

        constructor(
            title: String?, embedding: Any?
        ) {
            id = null
            this.title = title
            distance = null
            location = null
            embeeding = embedding
            crop = null
        }
        fun getEmbeddingAsFloatArray(): FloatArray {
            return when (embeeding) {
                is Array<*> -> (embeeding as Array<FloatArray>)[0]
                is FloatArray -> embeeding as FloatArray
                else -> throw IllegalStateException("Invalid embedding format")
            }
        }
        fun getLocation(): RectF {
            return RectF(location)
        }

        fun setLocation(location: RectF?) {
            this.location = location
        }

        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f)
            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            return resultString.trim { it <= ' ' }
        }
    }
}


class TFLiteFaceRecognition private constructor(ctx: Context) : FaceClassifier {
    private var isModelQuantized = false
    private val THRESHOLD = 0.7f

    // Config values.
    private var inputSize = 0
    private lateinit var intValues: IntArray
    private lateinit var embeedings: Array<FloatArray>
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null
    private val accountDao: AccountDao = LockerDatabase.getDatabase(ctx).accountDao()
    var registered = mutableMapOf<String?, FaceClassifier.Recognition>()

    override fun register(name: String?, role: String?, phone: String?, rec: FaceClassifier.Recognition?) {
        val embeddingString = rec?.embeeding?.let {
            val array = (it as Array<FloatArray>)[0]
            Log.d("FaceReg", "Registering embedding for $name: ${array.contentToString()}")
            array.joinToString(",")
        } ?: ""


        // Store in database
        val account = Account(
            Name = name ?: "Unknown",
            Role = role ?: "Unassign",
            Phone = phone ?: "Unassign",
            embedding = embeddingString,
            CreatedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                accountDao.insertAccount(account)
                // Update local cache immediately
                if (rec != null) {
                    registered[name] = rec
                    Log.d("FaceRec", "Registered new face for $name")
                }
            } catch (e: Exception) {
                Log.e("FaceRec", "Error registering face: ${e.message}")
            }
        }
    }
    private suspend fun loadRegisteredFaces() {
        try {
            val accounts = accountDao.getAllAccountsSync()
            accounts.forEach { account ->
                if (account.embedding.isNotEmpty()) {
                    val embeddingArray = account.embedding
                        .split(",")
                        .filter { it.isNotEmpty() }
                        .map { it.toFloat() }
                        .toFloatArray()

                    // Create wrapper array
                    val embeddingWrapper = Array(1) { embeddingArray }

                    registered[account.Name] = FaceClassifier.Recognition(
                        account.Name,
                        embeddingWrapper
                    )
                    Log.d("FaceRec", "Loaded face for ${account.Name}")
                }
            }
            Log.d("FaceRec", "Loaded ${registered.size} faces from database")
        } catch (e: Exception) {
            Log.e("FaceRec", "Error loading faces: ${e.message}")
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadRegisteredFaces()
            val accounts = accountDao.getAllAccounts().value ?: emptyList()
            accounts.forEach { account ->
                val embeddingArray = account.embedding
                    .split(",")
                    .filter { it.isNotEmpty() }
                    .map { it.toFloat() }
                    .toFloatArray()

                // Create as Array<FloatArray>
                val embeddingArrayWrapper = Array(1) { embeddingArray }

                registered[account.Name] = FaceClassifier.Recognition(
                    account.Name,
                    embeddingArrayWrapper
                )
            }
        }
    }

    fun getEmbeddingString(embedding: Array<FloatArray>): String {
        return embedding[0].joinToString(",")
    }

    fun parseEmbeddingString(embeddingString: String): FloatArray {
        return embeddingString.split(",")
            .filter { it.isNotEmpty() }
            .map { it.toFloat() }
            .toFloatArray()
    }

    // ค้นหาค่าที่ใกล้ที่สุดจากฐานข้อมูล
    private fun findNearest(emb: FloatArray): Pair<String?, Float>? {
        var ret: Pair<String?, Float>? = null
        for ((name, value) in registered) {
            val knownEmb = value.getEmbeddingAsFloatArray()
            Log.d("tryResiN", "Comparing $name embedding: ${knownEmb.contentToString()}")

            var distance = 0f
            for (i in emb.indices) {
                val diff = emb[i] - knownEmb[i]
                distance += diff * diff
            }
            distance = Math.sqrt(distance.toDouble()).toFloat()

            Log.d("tryResiN", "Distance for $name: $distance")
            if (ret == null || distance < ret.second) {
                ret = Pair(name, distance)
            }
        }
        return ret
    }

    // รับรูปภาพและกลับค่าผลการรู้จำ
    override fun recognizeImage(bitmap: Bitmap?, storeExtra: Boolean): FaceClassifier.Recognition? {
        val byteBuffer = ByteBuffer.allocateDirect(4 * bitmap!!.width * bitmap!!.height * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(bitmap.width * bitmap.height)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val input = intValues[pixel++]
                byteBuffer.putFloat((((input.shr(16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
            }
        }

        val inputArray = arrayOf<Any>(byteBuffer)
        val outputMap: MutableMap<Int, Any> = HashMap()
        embeedings = Array(1) { FloatArray(OUTPUT_SIZE) }
        outputMap[0] = embeedings
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        Log.d("tryResr",embeedings[0].contentToString())
        var distance = Float.MAX_VALUE
        var label: String? = "Unknown"
        Log.d("FaceRec", "Registered faces count: ${registered.size}")

        if (registered.isNotEmpty()) {
            val nearest = findNearest(embeedings[0])
            if (nearest != null) {
                val name = nearest.first
                val dist = nearest.second
                Log.d("FaceRec", "Found match: ${nearest.first} with distance: ${nearest.second}")
                if (dist < THRESHOLD) {
                    label = name
                    distance = dist
                } else {
                    label = "Unknown"
                    distance = dist
                }
            }
        }

        val rec = FaceClassifier.Recognition(
            "0", // ID, สามารถเปลี่ยนให้เหมาะสม
            label,
            distance,
            RectF()
        )
        if (storeExtra) {
            rec.embeeding = embeedings
        }
        return rec
    }

    companion object {
        private const val OUTPUT_SIZE = 512
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        // ฟังก์ชันในการโหลดโมเดล
        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
            val fileDescriptor = assets.openFd(modelFilename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            inputSize: Int,
            isQuantized: Boolean,
            ctx: Context
        ): FaceClassifier {
            val d = TFLiteFaceRecognition(ctx)
            d.inputSize = inputSize
            try {
                d.tfLite = Interpreter(loadModelFile(assetManager, modelFilename))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            d.isModelQuantized = isQuantized
            val numBytesPerChannel: Int = if (isQuantized) 1 else 4
            d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel)
            d.intValues = IntArray(d.inputSize * d.inputSize)
            return d
        }
    }
}
