package com.nullpointerexception.cityeye.firebase

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.entities.Answer
import com.nullpointerexception.cityeye.entities.Event
import com.nullpointerexception.cityeye.entities.MapItem
import com.nullpointerexception.cityeye.entities.Message
import com.nullpointerexception.cityeye.entities.Problem
import com.nullpointerexception.cityeye.entities.ProblemType
import com.nullpointerexception.cityeye.entities.SupportedCity
import com.nullpointerexception.cityeye.entities.User
import com.nullpointerexception.cityeye.entities.UserNotification
import com.nullpointerexception.cityeye.entities.WebUser
import com.nullpointerexception.cityeye.util.NetworkUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseRepository {

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val auth = Firebase.auth

    /*
    suspend fun addNormalProblem(
        context: Context,
        title: String,
        description: String,
        savedImageFile: File,
        location: LatLng,
        address: String,
        eventName: String?,
        markerAddress: String?
    ): Boolean {

        val problemID = firestore.collection("problems").document()
        val eventId = getEvents().find { it.title == eventName }?.id
        val markerId =
            getMapItems().find { markerAddress?.let { it1 -> it.address?.contains(it1) } == true }?.id

        val problem = Problem(
            problemID.id,
            title.trim(),
            description.trim(),
            auth.uid!!,
            address,
            savedImageFile.name,
            (System.currentTimeMillis() / 1000),
            false,
            location.latitude.toString(),
            location.longitude.toString(),
            eventId,
            markerId
        )


        if (NetworkUtil.isNetworkAvailable(context) && !isDuplicateProblem(
                context,
                savedImageFile.name
            )
        ) {
            val imagesRef = storage.reference.child("images/${savedImageFile.name}")
            val userRef = firestore.collection("users").document(auth.currentUser!!.uid)

            val uploadTask = suspendCoroutine { continuation ->
                imagesRef.putFile(Uri.fromFile(savedImageFile))
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener {
                        continuation.resume(false)
                    }
            }

            if (uploadTask) {
                val batchResult = suspendCoroutine { continuation ->
                    firestore.runBatch { batch ->
                        batch.update(userRef, "problems", FieldValue.arrayUnion(problemID))
                        batch.set(problemID, problem)
                    }.addOnSuccessListener {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.problemUploaded),
                            Toast.LENGTH_SHORT
                        ).show()

                        FirebaseDatabase.getInstance().reference.child("messages")
                            .setValue(problemID)
                            .addOnSuccessListener {
                                continuation.resume(true)
                            }
                            .addOnFailureListener {
                                continuation.resume(false)
                            }
                    }.addOnFailureListener {
                        continuation.resume(false)
                    }
                }

                return if (batchResult) {
                    true
                } else {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.errorUploading),
                        Toast.LENGTH_SHORT
                    ).show()
                    removeImage(savedImageFile.name)
                    false
                }
            } else {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.errorUploading),
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } else {
            return false
        }
    }
*/

    suspend fun addNormalProblem(
        context: Context,
        title: String,
        description: String,
        savedImageFile: File,
        location: LatLng,
        address: String,
        eventName: String?,
        markerAddress: String?
    ): Boolean {
        val problemID = firestore.collection("problems").document()
        val eventId = getEvents().find { it.title == eventName }?.id
        val markerId =
            getMapItems().find { markerAddress?.let { it1 -> it.address?.contains(it1) } == true }?.id

        val problem = Problem(
            problemID.id,
            title.trim(),
            description.trim(),
            auth.uid!!,
            address,
            savedImageFile.name,
            (System.currentTimeMillis() / 1000),
            false,
            location.latitude.toString(),
            location.longitude.toString(),
            eventId,
            markerId
        )

        if (NetworkUtil.isNetworkAvailable(context) && !isDuplicateProblem(
                context,
                savedImageFile.name
            )
        ) {
            val imagesRef = storage.reference.child("images/${savedImageFile.name}")
            val userRef = firestore.collection("users").document(auth.currentUser!!.uid)

            val uploadSuccess = retryWithExponentialBackoff {
                imagesRef.putFile(Uri.fromFile(savedImageFile)).await()
                true
            }

            if (uploadSuccess == true) {
                val transactionResult = retryWithExponentialBackoff {
                    firestore.runTransaction { transaction ->
                        transaction.update(userRef, "problems", FieldValue.arrayUnion(problemID))
                        transaction.set(problemID, problem)
                    }.await()

                    FirebaseDatabase.getInstance().reference.child("messages")
                        .setValue(problemID.id).await()
                    true
                }

                if (transactionResult == true) {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.problemUploaded),
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                } else {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.errorUploading),
                        Toast.LENGTH_SHORT
                    ).show()
                    removeImage(savedImageFile.name)
                    return false
                }
            } else {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.errorUploading),
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } else {
            return false
        }
    }

    suspend fun <T> retryWithExponentialBackoff(
        retries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(retries - 1) {
            try {
                return block()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isDuplicateProblem(context: Context, imageName: String): Boolean {
        val database = Firebase.firestore
        val colRef = database.collection("problems")

        var isDuplicate = false

        colRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val problem = document.toObject(Problem::class.java)

                    if (problem.imageName == imageName) {
                        isDuplicate = true
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.problemExists),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }
        return isDuplicate
    }

    suspend fun isDuplicateUser(uid: String): Boolean = suspendCoroutine { continuation ->
        val database = Firebase.firestore
        val colRef = database.collection("users").document(uid)

        colRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    fun addNewUser(context: Context): Boolean {
        val loggedUser = auth.currentUser
        val newUser = User(
            loggedUser?.uid,
            loggedUser?.displayName,
            loggedUser?.email
        )

        if (NetworkUtil.isNetworkAvailable(context)) {
            loggedUser?.let {
                firestore.collection("users").document(it.uid)
                    .set(newUser).addOnSuccessListener {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.welcome),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.errorLogging),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addOnFailureListener
                    }
            }
        } else {
            Toast.makeText(
                context,
                context.resources.getString(R.string.errorLogging),
                Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    fun updateFCMToken(context: Context) {

        val database = Firebase.firestore
        val loggedUser = Firebase.auth.currentUser

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            val token = task.result

            if (NetworkUtil.isNetworkAvailable(context)) {
                loggedUser?.let {
                    database.collection("users").document(it.uid)
                        .update("fcmToken", token).addOnSuccessListener {
                        }
                        .addOnFailureListener {
                        }
                }
            }
        })

    }

    fun removeImage(imageName: String): Boolean {
        val storageRef = Firebase.storage.reference

        val desertRef = storageRef.child("images/${imageName}")

        var isDeleted = false

        desertRef.delete().addOnSuccessListener {
            isDeleted = true
        }.addOnFailureListener {

        }

        return isDeleted
    }

    suspend fun getUser(userID: String): User? = suspendCoroutine { continuation ->
        val docRef = Firebase.firestore.collection("users").document(userID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    continuation.resume(document.toObject(User::class.java))
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    suspend fun getProblemById(id: String): Problem? =
        suspendCoroutine { continuation ->
            val docRef = Firebase.firestore.collection("problems").whereEqualTo("problemID", id)
            docRef.get()
                .addOnSuccessListener { documentResponse ->
                    val documents = documentResponse.documents
                    for (document in documents) {
                        val problem = document.toObject(Problem::class.java)
                        if (problem != null) {
                            continuation.resume(problem)
                        }
                    }

                }
                .addOnFailureListener { exception ->
                    continuation.resume(null)
                }
        }

    suspend fun getUserNotifications(notifications: List<String>): List<UserNotification> =
        suspendCoroutine { continuation ->
            if (notifications.isNotEmpty()) {
                val colRef = Firebase.firestore.collection("userNotifications")
                colRef.get()
                    .addOnSuccessListener { querySnapshot ->
                        val documents = querySnapshot.documents
                        val notificationsList = mutableListOf<UserNotification>()
                        for (document in documents) {
                            if (notifications.contains(document.id)) {
                                val notification = document.toObject(UserNotification::class.java)
                                if (notification != null) {
                                    notificationsList.add(notification)
                                }
                            }
                        }
                        continuation.resume(notificationsList)
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "Error getting documents: ", exception)
                        continuation.resumeWithException(exception)
                    }
            } else {
                continuation.resume(emptyList())
            }
        }

    suspend fun getAllProblems(): List<Problem> =
        suspendCoroutine { continuation ->
            val colRef = Firebase.firestore.collection("problems")
            colRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val documents = querySnapshot.documents
                    val problemList = mutableListOf<Problem>()
                    for (document in documents) {
                        val problem = document.toObject(Problem::class.java)
                        if (problem != null) {
                            problemList.add(problem)
                        }
                    }
                    continuation.resume(problemList)
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }

        }

    fun markNotificationAsRead(notificationID: String) {
        val database = Firebase.firestore
        database.collection("userNotifications").document(notificationID)
            .update("isRead", true).addOnSuccessListener {
            }
            .addOnFailureListener {
                Log.i("FAILURE", it.toString())
            }
    }

    suspend fun getSupportedCities() = suspendCoroutine {
        val database = Firebase.firestore
        val colRef = database.collection("supportedCities")
        colRef.get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val listOfSupportedCities = mutableListOf<SupportedCity>()
                for (document in documents) {
                    val event = SupportedCity(document.id, document.data?.get("cityName") as String)
                    listOfSupportedCities.add(event)
                }
                it.resume(listOfSupportedCities)
            }
            .addOnFailureListener { e ->
                it.resumeWithException(e)
            }
    }

    suspend fun getProblemTypes() = suspendCoroutine {
        val database = Firebase.firestore
        val colRef = database.collection("problemType")
        colRef.get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val listOfProblemTypes = mutableListOf<ProblemType>()
                for (document in documents) {
                    val event = ProblemType(document.id, document.data?.get("type") as String)
                    listOfProblemTypes.add(event)
                }
                it.resume(listOfProblemTypes)
            }
            .addOnFailureListener { e ->
                it.resumeWithException(e)
            }
    }

    suspend fun getEvents(): List<Event> =
        suspendCoroutine { continuation ->
            val colRef = Firebase.firestore.collection("events")
            colRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val documents = querySnapshot.documents
                    val eventsList = mutableListOf<Event>()
                    for (document in documents) {
                        val event = document.toObject(Event::class.java)
                        if (event != null) {
                            event.id = document.id
                            /*if ((event.epochEnd!! * 1000) > System.currentTimeMillis()) {
                                eventsList.add(event)
                            }*/
                            eventsList.add(event)
                        }
                    }
                    continuation.resume(eventsList)
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }

        }


    suspend fun getMapItems(): List<MapItem> =
        suspendCoroutine { continuation ->
            val colRef = Firebase.firestore.collection("markers")
            colRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val documents = querySnapshot.documents
                    val itemsList = mutableListOf<MapItem>()
                    for (document in documents) {
                        if (document != null) {
                            val marker = MapItem(
                                document.id,
                                document.data?.get("type") as String,
                                document.data?.get("lat") as Double,
                                document.data?.get("lng") as Double,
                                document.data?.get("address") as String
                            )
                            itemsList.add(marker)
                        }
                    }
                    continuation.resume(itemsList)
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }

        }

    suspend fun getAnswerByID(problemID: String): Answer? = suspendCoroutine { continuation ->
        val docRef = Firebase.firestore.collection("answers").whereEqualTo("problemID", problemID)
        docRef.get()
            .addOnSuccessListener { document ->
                val documents = document.documents
                if (documents.size == 0) {
                    continuation.resume(null)
                } else {
                    var problem: Answer? = null
                    for (document in documents) {
                        problem = document.toObject(Answer::class.java)
                    }
                    if (problem != null) {
                        continuation.resume(problem)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    fun assignUsername(userID: String) {
        val docRef = Firebase.firestore.collection("users").document(userID)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        val formattedDate = dateFormat.format(currentDate)

        val numberDigits = formattedDate.toString().toMutableList()
        numberDigits.shuffle()

        val shuffledNumber = numberDigits.joinToString("").toInt()

        docRef.update("displayName", "User$shuffledNumber")
    }

    fun sendMessage(problemID: String, text: String, user: FirebaseUser) {
        val ref = Firebase.firestore.collection("messages").document(problemID)
            .collection("problemMessages")

        ref.add(
            Message(
                text,
                user.displayName,
                user.photoUrl.toString(),
                user.uid,
                Instant.now().epochSecond.toInt()
            )
        )
    }

    suspend fun getAllUsers(): ArrayList<User>? = suspendCoroutine { continuation ->
        val docRef = Firebase.firestore.collection("users")
        docRef.get()
            .addOnSuccessListener { document ->
                val documents = document.documents
                if (documents.size == 0) {
                    continuation.resume(null)
                } else {
                    var array = arrayListOf<User>()
                    for (document in documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            array.add(user)
                        }
                    }
                    continuation.resume(array)

                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    suspend fun getAllWebUsers(): ArrayList<WebUser>? = suspendCoroutine { continuation ->
        val docRef = Firebase.firestore.collection("webUsers")
        docRef.get()
            .addOnSuccessListener { document ->
                val documents = document.documents
                if (documents.size == 0) {
                    continuation.resume(null)
                } else {
                    var array = arrayListOf<WebUser>()
                    for (documentA in documents) {
                        if (documentA != null) {
                            val user = WebUser(
                                documentA.id,
                                documentA.get("name") as String,
                                documentA.get("city") as String,
                                documentA.get("email") as String,
                                documentA.get("lastActive") as Timestamp,
                                documentA.get("role") as String
                            )
                            array.add(user)
                        }
                    }
                    continuation.resume(array)

                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }
}