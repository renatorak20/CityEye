package com.nullpointerexception.cityeye.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraUtil {

    private var currentPhotoPath: String? = null

    fun retrieveImage(): File {
        return compressImageFile(File(currentPhotoPath), 80)
    }

    fun createImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(
            Date()
        )
        val imageFileName = "JPEG_" + timeStamp + "_" + Firebase.auth.currentUser!!.uid
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


    private fun compressImageFile(file: File, quality: Int): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val matrix = Matrix()
        matrix.postRotate(90f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val compressedFile = File(file.absolutePath)
        FileOutputStream(compressedFile).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return compressedFile
    }

}