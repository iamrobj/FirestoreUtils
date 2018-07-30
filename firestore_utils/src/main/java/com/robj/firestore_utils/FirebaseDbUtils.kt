package com.robj.firestore_utils

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.robj.radicallyreusable.base.components.Optional
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers

/**
 * Created by Rob J on 17/01/18.
 */
class FirebaseDbUtils {

    companion object {

        private val TAG = FirebaseDbUtils::class.java.simpleName

        @JvmStatic
        fun <T> readDocFromDb(ref: DocumentReference, cls: Class<T>): Observable<Optional<T>> {
            return Observable.create { e -> setReadListeners(ref.get(), e, cls) }
        }

        private fun <T> setReadListeners(documentSnapshotTask: Task<DocumentSnapshot>, e: ObservableEmitter<Optional<T>>?, cls: Class<T>) {
            documentSnapshotTask
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val t = documentSnapshot.toObject(cls)
                            if (e != null && !e.isDisposed) {
                                e.onNext(Optional(t))
                                e.onComplete()
                            }
                        } else {
                            e!!.onNext(Optional(null))
                            e.onComplete()
                        }
                    }.addOnFailureListener(FailureListener(e))
        }

        @JvmStatic
        fun <T : DatabaseValueProvider> readCollectionFromDb(ref: CollectionReference, cls: Class<T>): Observable<List<T>> {
            return Observable.create<List<T>> { e ->
                ref.get()
                        .addOnSuccessListener { querySnapshot ->
                            if (e != null && !e.isDisposed) {
                                val results = ArrayList<T>()
                                for (snapshot1 in querySnapshot.documents) {
                                    val result = snapshot1.toObject(cls)
                                    if(result != null)
                                        results.add(result)
                                }
                                e.onNext(results)
                                e.onComplete()
                            }
                        }.addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T> parseQueryResultsFromDb(query: Query, cls: Class<T>): Observable<List<T>> {
            return Observable.create<List<T>> { e ->
                query.get()
                        .addOnSuccessListener { querySnapshot ->
                            if (e != null && !e.isDisposed) {
                                val results = ArrayList<T>()
                                for (snapshot1 in querySnapshot.documents) {
                                    val result = snapshot1.toObject(cls)
                                    if(result != null)
                                        results.add(result)
                                }
                                e.onNext(results)
                                e.onComplete()
                            }
                        }
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun readQueryFromDb(query: Query): Observable<QuerySnapshot> {
            return Observable.create<QuerySnapshot> { e ->
                query.get()
                        .addOnSuccessListener { querySnapshot ->
                            if (e != null && !e.isDisposed) {
                                e.onNext(querySnapshot)
                                e.onComplete()
                            }
                        }
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T : DatabaseValueProvider> writeDocsToCollection(db: FirebaseFirestore, ref: CollectionReference, items: List<T>): Observable<List<T>> {
            return Observable.create<List<T>> { e ->
                val batch = db.batch()
                for (t in items)
                    batch.set(ref.document(t.getId()), t)
                batch.commit()
                        .addOnSuccessListener(WriteSuccessListener(e, items))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun deleteDoc(ref: DocumentReference): Observable<Boolean> {
            return Observable.create<Boolean> { e ->
                ref.delete()
                        .addOnSuccessListener(WriteSuccessListener(e, true))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T : Any> writeDocToCollection(ref: CollectionReference, id: String, t: T): Observable<T> {
            return Observable.create<T> { e ->
                ref.document(id).set(t)
                        .addOnSuccessListener(WriteSuccessListener(e, t))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T : Any> writeDocToCollection(ref: CollectionReference, t: T): Observable<T> {
            return Observable.create<T> { e ->
                ref.add(t)
                        .addOnSuccessListener(WriteSuccessListener(e, t))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T : Any> writeDoc(ref: DocumentReference, t: T): Observable<T> {
            return Observable.create<T> { e ->
                ref.set(t)
                        .addOnSuccessListener(WriteSuccessListener(e, t))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun removeDocFromCollection(ref: CollectionReference, id: String): Observable<Boolean> {
            return Observable.create<Boolean> { e ->
                ref.document(id).delete()
                        .addOnSuccessListener(WriteSuccessListener(e, true))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T : Any> updateDocToDb(ref: DocumentReference, t: T): Observable<T> {
            return Observable.create<T> { e ->
                ref.set(t)
                        .addOnSuccessListener(WriteSuccessListener(e, t))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun mergeMapInDoc(ref: DocumentReference, value: Map<String, Any>): Observable<Boolean> {
            return Observable.create<Boolean> { e ->
                ref.set(value, SetOptions.merge())
                        .addOnSuccessListener(WriteSuccessListener(e, true))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun <T> deleteFieldInDoc(ref: DocumentReference, field: String): Observable<Boolean> {
            val map = HashMap<String, Any>()
            map[field] = FieldValue.delete()
            return Observable.create<Boolean> { e ->
                ref.update(map)
                        .addOnSuccessListener(WriteSuccessListener(e, true))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        @JvmStatic
        fun runTask(task: Task<Void>): Observable<Boolean> {
            return Observable.create<Boolean> { e ->
                task
                        .addOnSuccessListener(WriteSuccessListener(e, true))
                        .addOnFailureListener(FailureListener(e))
            }
                    .compose(applyObservableRules())
        }

        private class FailureListener(private val e: ObservableEmitter<*>?) : OnFailureListener {
            override fun onFailure(ex: Exception) {
                if (e != null && !e.isDisposed) {
                    ex.printStackTrace()
                    e.onError(ex)
                    e.onComplete()
                }
            }
        }

        private class WriteSuccessListener<T, R>(e: ObservableEmitter<R>, private val result: R) : com.google.android.gms.tasks.OnSuccessListener<T> {
            private val e: ObservableEmitter<R>?

            init {
                this.e = e
            }

            override fun onSuccess(t: T) {
                if (e != null && !e.isDisposed) {
                    e.onNext(result)
                    e.onComplete()
                }
            }
        }

        private fun <T> applyObservableRules(): ObservableTransformer<T, T> {
            return ObservableTransformer<T, T> {
                it.take(1).subscribeOn(Schedulers.computation())
            };
        }

    }

    class AuthException(errorMsg: String) : RuntimeException(errorMsg)

    interface DatabaseValueProvider {
        fun getId(): String
    }

}
