package com.example.eventhou.util

import android.util.Log
import com.google.firebase.firestore.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseUtils {
    companion object {
        
        /**
         * Suspend while getting the document snapshot of a document reference.
         */
        suspend fun getDocumentSnapshot(docRef: DocumentReference): DocumentSnapshot {
            return suspendCoroutine { continuation ->
                docRef.get().addOnSuccessListener { doc ->
                    continuation.resume(doc)
                }.addOnFailureListener { failure ->
                    Log.w("EventSimilarity", "Failed to fetch document of " + docRef.id)
                    continuation.resumeWithException(failure)
                }
            }
        }

        /**
         * Suspend while getting the query snapshot of a query.
         */
        suspend fun getQuerySnapshot(query: Query): QuerySnapshot {
            return suspendCoroutine { continuation ->
                query.get().addOnSuccessListener { collection ->
                    continuation.resume(collection)
                }.addOnFailureListener { failure ->
                    Log.w("EventSimilarity", "Failed to fetch query of $query")
                    continuation.resumeWithException(failure)
                }
            }
        }

        /**
         * Merge or update a document reference with the specified value.
         */
        fun mergeOrUpdateDocumentRef(
            doc: DocumentReference,
            field: String,
            updateValue: Any,
            setValue: Any
        ) {
            doc.update(field, updateValue).addOnFailureListener { result ->
                mergeDocumentRef(
                    doc,
                    field,
                    setValue
                )
            }
        }

        /**
         * Merge a document reference with the specified value.
         */
        fun mergeDocumentRef(
            doc: DocumentReference,
            field: String,
            setValue: Any,
            logFailure: Boolean = true
        ) {
            doc.set(hashMapOf(field to setValue), SetOptions.merge())
                .addOnFailureListener { result ->
                    if (logFailure) {
                        Log.e("Firebase", result.message)
                    }
                }
        }

        /**
         * Delete a document reference.
         */
        fun deleteDocumentRef(doc: DocumentReference, logFailure: Boolean = true) {
            doc.delete().addOnFailureListener { result ->
                if (logFailure) {
                    Log.e("Firebase delete", result.message)
                }
            }
        }
    }
}