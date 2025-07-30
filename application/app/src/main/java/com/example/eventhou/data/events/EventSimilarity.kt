package com.example.eventhou.data.events

import com.example.eventhou.data.model.Event
import com.example.eventhou.data.model.AppUser
import com.example.eventhou.util.FirebaseUtils
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.Instant
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

/**
 * Calculates the score of the specified event.
 * @param event the event to be scored
 * @param user the user whose events are scored
 */
class EventSimilarity(val event: Event, val user: AppUser) {

    /**
     * The firestore object.
     */
    val firestore: FirebaseFirestore = Firebase.firestore

    /**
     * Get the overall similarity score (preferably in range [0,1]).
     *
     * @return {number}
     */
    suspend fun getScore(): Double {
        var score = 0.0
        // If EventScore + EventCategoryScore < EventCategoryScoreOfUser => more personalized than mainstream
        score += this.getDateTimeScore() * 0.25
        score += this.getEventScore() * 0.2
        score += this.getEventCategoryScore() * 0.15
        val dbUser = firestore.collection(EventHandler.USERS_KEY).document(user.userId)
        val docSnap = FirebaseUtils.getDocumentSnapshot(dbUser)
        if (docSnap.exists()) {
            // Only add user score if user properties exist
            score += this.getEventCategoryScoreOfUser(dbUser) * 0.4
        }
        // Add more scores to recommend more precisely.
        return score
    }

    /**
     * Get the popularity score of the event itself compared to all events on this day.
     * @return the score in [0-1]
     */
    private fun getEventScore(): Double {
        return event.popularityNormalized
    }

    /**
     * Get the popularity score of the event categories compared to all categories.
     * @return the score in [0-1]
     */
    private suspend fun getEventCategoryScore(): Double {
        if (event.categories != null) {
            // Calculates arithmetic mean, there's maybe a more suitable approach
            val fbCategories = firestore.collection(EventHandler.CATEGORIES_KEY)
            return getEventCategoryScoreOfCollectionRef(fbCategories)
        }
        return 0.0
    }

    /**
     * Get the popularity score of the event categories compared to the users categories.
     *
     * @param dbUser the firestore document reference to current user.
     * @return the score in [0-1]
     */
    private suspend fun getEventCategoryScoreOfUser(dbUser: DocumentReference): Double {
        if (event.categories != null) {
            // Calculates arithmetic mean, there's maybe a more suitable approach
            val fbCategories = dbUser.collection(EventHandler.CATEGORIES_KEY)
            return getEventCategoryScoreOfCollectionRef(fbCategories)
        }
        return 0.0
    }

    /**
     * Generic method to calculate category score of a firestore collection.
     *
     * @param fbCategories the firestore categories collection reference
     * @return the score in [0-1]
     */
    private suspend fun getEventCategoryScoreOfCollectionRef(fbCategories: CollectionReference): Double {
        var score = 0.0
        var count = 0
        for (category in event.categories!!) {
            val fbCategory = fbCategories.document(category.categoryName!!)
            val docSnap = FirebaseUtils.getDocumentSnapshot(fbCategory)
            if (docSnap.exists()) {
                var catScore = docSnap.getDouble(EventHandler.POPULARITY_NORMALIZED_KEY) ?: 0.0
                if (category.subCategories != null) {
                    catScore *= getEventSubCategoryScoreOfCollectionRef(
                        fbCategory.collection(EventHandler.SUBCATEGORIES_KEY),
                        category.subCategories!!
                    )
                }
                count += 1
                score += catScore
            }
        }
        return if (count > 0) (score / count) else 0.0
    }


    /**
     * Generic method to calculate subcategory score of a firestore collection.
     *
     * @return the score in [0-1]
     */
    private suspend fun getEventSubCategoryScoreOfCollectionRef(
        fbSubCategories: CollectionReference,
        subcategories: List<String>
    ): Double {
        var score = 0.0
        var count = 0
        for (subCategoryName in subcategories) {
            if (subCategoryName.isNotEmpty()) {
                val fbSubCategory = fbSubCategories.document(subCategoryName)
                val docSnap = FirebaseUtils.getDocumentSnapshot(fbSubCategory)
                if (docSnap.exists()) {
                    count += 1
                    score += docSnap.getDouble(EventHandler.POPULARITY_NORMALIZED_KEY) ?: 0.0
                }
            }
        }
        return if (count > 0) (score / count) else 0.0
    }

    /**
     * Determines how similar the day time is.
     *
     * @return the day time score in [0-1]
     */
    private fun getDateTimeScore(): Double {
        return scoreFromDistanceInInfiniteRange(
            (Instant.parse(event.utcDateTimeISO).epochSecond - Instant.now().epochSecond).toDouble(),
            DAY_IN_SECONDS,
            1.0
        )
    }

    companion object {

        val DAY_IN_MINUTES = 24 * 60.0
        val DAY_IN_SECONDS = 24 * 60 * 60.0

        /**
         * Calculates the score in an infinite range by defining a mean range.
         *
         * @param distance the distance to your optimal value
         * @param meanRange the mean range distance where the score results in 0.5.
         * @return the score in [0-1]
         */
        fun scoreFromDistanceInInfiniteRange(
            distance: Double,
            meanRange: Double,
            polynomial: Double = 1.0
        ): Double {
            return (1 - this.normalizeDistanceInInfiniteRange(distance, meanRange)).pow(polynomial)
        }


        /**
         * Get the score from specified distance.
         *
         * @param distance the distance to your optimal value (may be outside of coset)
         * @param coset the coset (Restklasse)
         * @param polynomial the ratio of importance to distance
         * @return the score in [0-1]
         */
        fun scoreFromDistanceInCoset(
            distance: Double,
            coset: Double,
            polynomial: Double = 1.0
        ): Double {
            return (1 - this.normalizeDistanceInCoset(distance, coset)).pow(polynomial)
        }

        /**
         * Get the distance in range [0,1], if distance is infinite.
         * @param distance the distance to your optimal value
         * @param meanRange the mean range distance where half of the values are expected to be included.
         */
        fun normalizeDistanceInInfiniteRange(distance: Double, meanRange: Double): Double {
            return (2 / (1 + exp(-abs(distance) / meanRange))) - 1
        }

        /**
         * Get the distance in range [0,1].
         *
         * @param distance the distance to your optimal value
         * @param coset the coset (Restklasse)
         */
        fun normalizeDistanceInCoset(distance: Double, coset: Double): Double {
            val half = coset / 2.0
            val distCoset = ((distance + coset) % coset)
            val absDistance = if (distCoset > half) (coset - distCoset) else distCoset
            return absDistance / half
        }
    }

}