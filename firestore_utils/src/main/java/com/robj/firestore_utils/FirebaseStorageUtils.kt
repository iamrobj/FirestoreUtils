package com.robj.involvd.utils

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.reactivex.Observable
import java.io.ByteArrayOutputStream

/**
 * Created by Rob J on 21/07/18.
 */
class FirebaseStorageUtils {

    companion object {

        private var TAG: String = FirebaseStorageUtils::class.java.simpleName
        private const val MAX_AVATAR_SIZE = 400

        @JvmStatic
        fun uploadImage(bitmap: Bitmap, filename: String): Observable<String> {
            val storage = FirebaseStorage.getInstance()
            val imageRef = storage.reference.child("$filename.png")

            return Observable.create<String> {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                val uploadTask = imageRef.putBytes(data)
                uploadTask.addOnProgressListener {
                    Log.d(TAG, ((it.bytesTransferred/it.totalByteCount) * 100).toString() + "% uploaded..")
                }.addOnFailureListener(OnFailureListener { e ->
                    it.onError(e)
                    it.onComplete()
                }).addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot> {snapshot ->
                    Log.d(TAG, "$filename upload success..")
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        it.onNext(uri.toString())
                        it.onComplete()
                    }.addOnFailureListener { e ->
                        it.onError(e)
                        it.onComplete()
                    };
                })
            }
        }

    }

}