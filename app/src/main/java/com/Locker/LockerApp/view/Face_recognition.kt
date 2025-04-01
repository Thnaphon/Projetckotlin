package com.Locker.LockerApp.view

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.Locker.LockerApp.model.Account
import com.Locker.LockerApp.model.AccountDao
import com.Locker.LockerApp.model.LockerDatabase
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
    fun register(name: String?, role: String?, phone: String?, recognition: Recognition?):Int
    fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): Recognition?

    fun close()

    class Recognition {
        val id: String?
        val title: String?
        val distance: Float?
        var embeeding: Any?
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

        //clean up bitmap resources
        fun recycle() {
            try {
                crop?.recycle()
                crop = null
            } catch (e: Exception) {
                Log.e("Recognition", "Error recycling bitmap", e)
            }
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

class TFLiteFaceRecognition private constructor(context: Context) : FaceClassifier {
    // Use application context to prevent activity/fragment context leaks
    private val appContext = context.applicationContext

    private var isModelQuantized = false
    private val THRESHOLD = 1.12f
    private val interpreterLock = Object()
    // Config values
    private var inputSize = 0
    private lateinit var intValues: IntArray
    private lateinit var embeedings: Array<FloatArray>
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null

    // Database access
    private val accountDao: AccountDao = LockerDatabase.getDatabase(appContext).accountDao()
    var registered = mutableMapOf<String?, FaceClassifier.Recognition>()

    // Close and release resources
    override fun close() {
        try {
            tfLite?.close()
            tfLite = null
            imgData = null

            // Clean up Recognition objects
            registered.values.forEach { it.recycle() }
            registered.clear()
        } catch (e: Exception) {
            Log.e("TFLiteFaceRecognition", "Error closing resources", e)
        }
    }

    override fun register(
        name: String?,
        role: String?,
        phone: String?,
        rec: FaceClassifier.Recognition?
    ): Int {
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

        var newAccountId = 0

        kotlinx.coroutines.runBlocking {
            try {
                newAccountId = accountDao.insertAccountAndGetId(account).toInt()
                if (rec != null) {
                    registered[name] = rec
                    Log.d("FaceRec", "Registered new face for $name with ID: $newAccountId")
                } else {
                    // nothing
                }
            } catch (e: Exception) {
                Log.e("FaceRec", "Error registering face: ${e.message}")
            }
        }
        return newAccountId
    }

    private suspend fun loadRegisteredFaces() {
        try {
            // Clear existing cache
            registered.values.forEach { it.recycle() }
            registered.clear()

            val accounts = accountDao.getAllAccountsSync()
            accounts.forEach { account ->
                if (account.embedding.isNotEmpty()) {
                    val embeddingArray = account.embedding
                        .split(",")
                        .filter { it.isNotEmpty() }
                        .map { it.toFloat() }
                        .toFloatArray()

                    // wrapper array
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
        }
    }

    // Handle bitmap recognition with proper memory management
    override fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): FaceClassifier.Recognition? {
        if (bitmap == null || bitmap.isRecycled) {
            Log.e("FaceRec", "Invalid bitmap provided for recognition")
            return null
        }

        try {

            synchronized(interpreterLock) {
                // Use cached buffer when possible, or create a new one
                val bufferSize = 4 * bitmap.width * bitmap.height * 3
                if (imgData == null || imgData?.capacity() != bufferSize) {
                    imgData = ByteBuffer.allocateDirect(bufferSize)
                    imgData?.order(ByteOrder.nativeOrder())
                }

                imgData?.clear() // Reset position

                // Valid intValues array is properly sized
                if (!::intValues.isInitialized || intValues.size != bitmap.width * bitmap.height) {
                    intValues = IntArray(bitmap.width * bitmap.height)
                }

                bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                var pixel = 0
                for (i in 0 until bitmap.width) {
                    for (j in 0 until bitmap.height) {
                        val input = intValues[pixel++]
                        imgData?.putFloat((((input.shr(16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                        imgData?.putFloat((((input.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                        imgData?.putFloat((((input and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                    }
                }
                if (tfLite == null) {
                    Log.e("FaceRec", "TFLite interpreter is null")
                    return null
                }

                val inputArray = arrayOf<Any>(imgData!!)
                val outputMap: MutableMap<Int, Any> = HashMap()
                embeedings = Array(1) { FloatArray(OUTPUT_SIZE) }
                outputMap[0] = embeedings
                tfLite?.runForMultipleInputsOutputs(inputArray, outputMap)

                var distance = Float.MAX_VALUE
                var label: String? = "Unknown"

                if (registered.isNotEmpty()) {
                    val nearest = findNearest(embeedings[0])
                    if (nearest != null) {
                        val name = nearest.first
                        val dist = nearest.second
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
                    "0",
                    label,
                    distance,
                    RectF()
                )

                if (getExtra) {
                    // Create a copy to prevent modifying the original
                    val embeddingsCopy = Array(1) { embeedings[0].clone() }
                    rec.embeeding = embeddingsCopy
                }

                // set crop if requested and create a copy to manage independently
                if (getExtra && bitmap != null) {
                    rec.crop = bitmap.copy(bitmap.config, true)
                }

                return rec
            }
        } catch (e: Exception) {
            Log.e("TFLiteFaceRecognition", "Error during recognition", e)
            return null
        }
    }

    // Find nearest face in database
    private fun findNearest(emb: FloatArray): Pair<String?, Float>? {
        var ret: Pair<String?, Float>? = null
        for ((name, value) in registered) {
            try {
                val knownEmb = value.getEmbeddingAsFloatArray()

                var distance = 0f
                for (i in emb.indices) {
                    val diff = emb[i] - knownEmb[i]
                    distance += diff * diff
                }
                distance = Math.sqrt(distance.toDouble()).toFloat()

                if (ret == null || distance < ret.second) {
                    ret = Pair(name, distance)
                }
            } catch (e: Exception) {
                Log.e("TFLiteFaceRecognition", "Error comparing with $name", e)
            }
        }
        return ret
    }

    companion object {
        private const val OUTPUT_SIZE = 512
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        // Prevent leaking context through companion object
        private var instanceCount = 0

        @Synchronized
        fun incrementInstanceCount() {
            instanceCount++
            Log.d("TFLiteFaceRecognition", "Created new instance, total: $instanceCount")
        }

        @Synchronized
        fun decrementInstanceCount() {
            instanceCount--
            Log.d("TFLiteFaceRecognition", "Destroyed instance, remaining: $instanceCount")
        }

        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
            val fileDescriptor = assets.openFd(modelFilename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val mappedByteBuffer =
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            // Clean up resources
            fileChannel.close()
            inputStream.close()
            fileDescriptor.close()

            return mappedByteBuffer
        }

        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            inputSize: Int,
            isQuantized: Boolean,
            context: Context
        ): FaceClassifier {
            val d = TFLiteFaceRecognition(context.applicationContext)
            d.inputSize = inputSize

            try {
                // Load TFLite model
                val modelBuffer = loadModelFile(assetManager, modelFilename)
                d.tfLite = Interpreter(modelBuffer)

                // Track instances for debugging
                incrementInstanceCount()
            } catch (e: Exception) {
                Log.e("TFLiteFaceRecognition", "Error creating face classifier", e)
                throw RuntimeException(e)
            }

            d.isModelQuantized = isQuantized
            val numBytesPerChannel: Int = if (isQuantized) 1 else 4
            d.imgData =
                ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel)
            d.imgData?.order(ByteOrder.nativeOrder())
            d.intValues = IntArray(d.inputSize * d.inputSize)

            return d
        }
    }
}