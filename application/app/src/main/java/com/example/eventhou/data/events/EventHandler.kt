package com.example.eventhou.data.events

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.eventhou.R
import com.example.eventhou.data.model.AppUser
import com.example.eventhou.data.model.Category
import com.example.eventhou.data.model.City
import com.example.eventhou.data.model.Event
import com.example.eventhou.util.FirebaseUtils
import com.example.eventhou.util.dateToISO
import com.example.eventhou.util.datesUntil
import com.example.eventhou.util.logUserLocation
import com.google.firebase.firestore.*
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.time.LocalDate

/**
 * Handle all operations regarding the events.
 * @param user the user who interacts with the events
 */
class EventHandler(val user: AppUser) {

    companion object {
        const val USERS_KEY = "users"
        const val EVENTS_KEY = "events"
        const val LOCATIONS_KEY = "locations"
        const val CATEGORIES_KEY = "categories"
        const val SUBCATEGORIES_KEY = "subCategories"
        const val POPULARITY_KEY = "popularity"
        const val POPULARITY_NORMALIZED_KEY = "popularityNormalized"
        const val USER_FAVORITE_EVENTS = "favoriteEvents"

        const val PREFERENCE_LOCATION = "location"
        const val FALLBACK_LOCATION = "Munich"

        /**
         * Get the category preferences from shared app preferences.
         */
        fun getCategoryFilterPrefs(activityContext: Context): Collection<Category> {
            val filterCats = mutableMapOf<String, Category>()
            val sharedPref = activityContext.getSharedPreferences(
                activityContext.getString(R.string.preference_filter_categories),
                Context.MODE_PRIVATE
            ) ?: return filterCats.values

            val categoryLabels = activityContext.resources.getStringArray(R.array.category_labels)
            val categorySlugs = activityContext.resources.getStringArray(R.array.category_slugs)
            val subCategorySlugs =
                activityContext.resources.obtainTypedArray(R.array.subcategory_slugs)
            val subCategoryLabels =
                activityContext.resources.obtainTypedArray(R.array.subcategory_labels)

            // Iterate through categories from preferences and convert them to a
            // Category collection with subcategories.
            sharedPref.all?.forEach {
                if (it.value == true) {
                    // preference is stored as "category.subcategory"
                    val slugs = it.key.split('.')
                    if (slugs.size > 1) {
                        val catIndex = categorySlugs.indexOf(slugs[0])
                        if (catIndex >= 0) {
                            val subSlugsId: Int = subCategorySlugs.getResourceId(catIndex, 0)
                            val subLabelsId: Int = subCategoryLabels.getResourceId(catIndex, 0)
                            if (subLabelsId > 0 && subLabelsId > 0) {
                                val subcategorySlugs =
                                    activityContext.resources.getStringArray(subSlugsId)
                                val subcategoryLabels =
                                    activityContext.resources.getStringArray(subLabelsId)
                                val filterCat: Category
                                if (filterCats.containsKey(slugs[0])) {
                                    filterCat = filterCats[slugs[0]]!!
                                } else {
                                    // Add filter category if not existent
                                    filterCat =
                                        Category()
                                    filterCat.categoryName = categoryLabels[catIndex]
                                    filterCat.subCategories = mutableListOf()
                                    filterCats[slugs[0]] = filterCat
                                }
                                val subCatsList = filterCat.subCategories as MutableList
                                val index = subcategorySlugs.indexOf(slugs[1])
                                subCatsList.add(subcategoryLabels[index])
                            }
                        }
                    }
                }
            }
            subCategorySlugs.recycle()
            subCategoryLabels.recycle()

            return filterCats.values
        }
    }

    /**
     * The firestore object.
     */
    private val firestore: FirebaseFirestore = Firebase.firestore

    /**
     * Document for accessing firestore user specific data.
     */
    private val dbUser = firestore.collection(USERS_KEY).document(user.userId)

    /**
     * Determines if needs updating.
     */
    private var update = false

    /**
     * The current stack of upcoming events (to be swiped).
     */
    private val allEvents = mutableSetOf<Event>()


    /**
     * The current stack of available cities.
     */
    val allCities = mutableSetOf<City>()


    /**
     * The filtered list of allEvents.
     */
    var filteredEvents = mutableListOf<Event>()
        set(value) {
            field.clear()
            field.addAll(value)
            notifyEventsUpdated()
        }

    val eventHandles = mutableListOf<IEventHandler>()

    /**
     * The async job which filters events.
     */
    private var filterJob: Job? = null

    /**
     * The async job which queries events.
     */
    private var queryJob: Job? = null

    /**
     * Determines if an async job is currently running
     */
    var isJobRunning = false

    /**
     * The UTC start date (inclusive)
     */
    var startDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            dateRange = datesUntil(startDate, endDate)
        }

    /**
     * The UTC end date (exclusive)
     * Means that all events up to exactly this date time are taken into account (but not the date itself).
     */
    var endDate: LocalDate = LocalDate.now().plusDays(3) // Two days in advance
        set(value) {
            field = value
            dateRange = datesUntil(startDate, endDate)
        }

    /**
     * The user location or the fallback location (not necessarily the event location)
     */
    var location = FALLBACK_LOCATION

    /**
     * The date range from startDate to endDate, is updated automatically.
     */
    private var dateRange = datesUntil(startDate, endDate)

    /**
     * The current category filter preferences.
     */
    var categoryFilterPrefs: Collection<Category> = mutableSetOf()

    /**
     * Process all the event tasks: query cities, query events, apply filter & recommender system.
     * @param contextWrapper the context to access history preferences.
     */
    fun processEvents(contextWrapper: ContextWrapper) {
        queryJob?.cancel()
        queryJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                if (allCities.isEmpty()) {
                    queryCities()
                }
                queryEvents()
                applyFilter(contextWrapper)
            } catch (ffex: FirebaseFirestoreException) {
                Log.e("Firebase Async", ffex.message)
            }
        }
    }

    /**
     * Query the events from the firestore database.
     */
    suspend fun queryEvents() {
        notifyEventsLoading()

        for (date in dateRange) {
            val dateStr = dateToISO(date)

            var eventCollection: QuerySnapshot?

            // Query events from current location
            eventCollection = FirebaseUtils.getQuerySnapshot(
                firestore.collection("events").document(dateStr).collection(location)
            )

            if (eventCollection.isEmpty) {
                // Query events from fallback location
                location = FALLBACK_LOCATION
                eventCollection = FirebaseUtils.getQuerySnapshot(
                    firestore.collection("events").document(dateStr).collection(location)
                )

                if (BuildConfig.DEBUG) {
                    logUserLocation(
                        location,
                        firestore,
                        dateStr,
                        user
                    )
                }
            }

            for (document in eventCollection) {
                val event = document.toObject(Event::class.java)
                if (!allEvents.contains(event)) {
                    allEvents.add(preProcessEvent(event))
                }
            }
        }
        filteredEvents.addAll(allEvents)

        // Remove notify EventsLoaded, because currently, they are always filtered afterwards.
        // If use queryEvents in other cases, enable the following line again:
        //notifyEventsLoaded()
    }

    /**
     * Preprocess event from database.
     * @param event the event to be preprocessed
     */
    private fun preProcessEvent(event: Event): Event {
        if (event.artistImageSrc.isNullOrEmpty()) event.artistImageSrc =
            "https://assets.bandsintown.com/images/fallbackImage.png"
        event.utcDateISO = event.utcDateTimeISO.substring(0, 10)
        event.location = location
        return event
    }

    /**
     * Query the cities from database.
     */
    suspend fun queryCities() {
        val cityCollection = FirebaseUtils.getQuerySnapshot(
            firestore.collection(LOCATIONS_KEY)
        )

        for (document in cityCollection) {
            val city: City = document.toObject(
                City::class.java
            )
            if (city.enabled && !allCities.contains(city)) {
                allCities.add(city)
            }
        }
    }

    /**
     * Set all the events available in this session.
     */
    private fun setAllEvents(updatedEvents: List<Event>) {
        allEvents.clear()
        allEvents.addAll(updatedEvents)
    }

    /**
     * Apply user filters.
     */
    fun applyFilter(contextWrapper: ContextWrapper) {
        filterJob?.cancel()
        filterJob = GlobalScope.launch(Dispatchers.IO) {
            notifyEventsLoading()
            val eventHistory = mutableSetOf<String>()
            try {
                for (date in dateRange) {
                    eventHistory.addAll(EventHistory(contextWrapper).get(dateToISO(date)))
                }
                filteredEvents.clear()
                filteredEvents.addAll(allEvents)

                applyDateFilter(dateRange)
                applyHistoryFilter(eventHistory)
                applyCategoryFilter(categoryFilterPrefs)
                applyRecommendations()
            } catch (ffex: FirebaseFirestoreException) {
                Log.e("Firebase Async", ffex.message)
            }
            filterJob = null
            notifyEventsLoaded()
        }
    }

    /**
     * Notify that the events are currently loading or processed.
     */
    private fun notifyEventsLoading() {
        isJobRunning = true
        for (handler in eventHandles) {
            handler.onEventsLoading()
        }
    }

    /**
     * Notify that the events are all loaded and processed.
     */
    private fun notifyEventsLoaded() {
        isJobRunning = false
        for (handler in eventHandles) {
            handler.onEventsLoaded(filteredEvents)
        }
    }

    /**
     * Notify that events were currently updated.
     */
    private fun notifyEventsUpdated() {
        for (handler in eventHandles) {
            handler.onEventsUpdated(filteredEvents)
        }
    }

    /**
     * Apply the date range filter.
     * @param dates the date range
     */
    private fun applyDateFilter(dates: List<LocalDate>) {
        val isoDates = dates.map { dateToISO(it) }
        val tmp = filteredEvents.filter {
            isoDates.contains(it.utcDateISO)
        }
        filteredEvents.clear()
        filteredEvents.addAll(tmp)
    }

    /**
     * Filter out already swiped events.
     * @param eventHistory set of events, which already were swiped
     */
    private fun applyHistoryFilter(eventHistory: MutableSet<String>) {
        val tmp = filteredEvents.filter {
            !eventHistory.contains(it.eventId)
        }
        filteredEvents.clear()
        filteredEvents.addAll(tmp)
    }

    /**
     * Filter the events by categories.
     * @param categoriesFilter the collection of categories
     */
    private fun applyCategoryFilter(categoriesFilter: Collection<Category>) {
        if (!categoriesFilter.isEmpty()) {
            val tmp = mutableListOf<Event>()
            filteredEvents.forEach { event ->
                event.categories?.let {
                    categoriesFilter.forEach { filterCat ->
                        val category = it.findLast { it == filterCat }
                        category?.let { cat ->
                            cat.subCategories?.let { subCats ->
                                filterCat.subCategories?.let { filterSubCats ->
                                    if (subCats.intersect(filterSubCats).isNotEmpty()) {
                                        tmp.add(event)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            filteredEvents.clear()
            filteredEvents.addAll(tmp)
        }
    }

    /**
     * Score the events by its category & popularity and sort them afterwards.
     */
    private suspend fun applyRecommendations() = withContext(Dispatchers.IO) {
        // Safe deferred events in own list to allow parallel score calculation. Not thread safe!
        val deferredEvents = mutableListOf<Deferred<Event>>()
        for (event in filteredEvents) {
            if (!isActive) return@withContext
            if (event.score == null) {
                val deferredEvent = GlobalScope.async {
                    val eventSimilarity = EventSimilarity(event, user)
                    event.score = eventSimilarity.getScore()
                    return@async event
                }
                deferredEvents.add(deferredEvent)
            }
        }
        // Wait for all events to finish score calculation.
        for (deferredEvent in deferredEvents) {
            if (!isActive) return@withContext
            deferredEvent.await()
        }
        filteredEvents.sortByDescending { it.score }
    }

    /**
     * Boost an event.
     * @param event the event to be boosted.
     * @param contextWrapper the context to access history preferences.
     */
    fun boostEvent(event: Event, contextWrapper: ContextWrapper) {
        EventHistory(contextWrapper).add(event)

        addFavoriteEventToUser(event)
        incrementEventPopularity(event)
        incrementEventCategoriesPopularity(event)
        incrementEventCategoriesPopularityOfUser(event)
        update = true
    }

    /**
     * Mark event as favorite of a user.
     * @param event the event to be favored.
     */
    fun addFavoriteEventToUser(event: Event) {

        val favoriteEventDoc = dbUser.collection(USER_FAVORITE_EVENTS)
            .document(event.utcDateISO)
            .collection(event.location)
            .document(event.eventId)
        FirebaseUtils.mergeDocumentRef(favoriteEventDoc, "eventId", event.eventId)
    }

    /**
     * Get the users favorite events.
     */
    suspend fun getFavoriteEventsFromUser(): List<Event> {
        var dateCounter = LocalDate.now()

        // Safe deferred events in own list to allow parallel fetches. Not thread safe!
        val deferredEvents = mutableListOf<Deferred<Event?>>()

        // TODO Unfortunately cannot list collections, which are empty -> needs database restructuring
        // Iterate through all date documents and search for favorite events.
        for (i in 0 until 10) {
            val dateStr = dateCounter.toString()
            val favoriteEventCollection = FirebaseUtils.getQuerySnapshot(
                dbUser.collection(USER_FAVORITE_EVENTS).document(dateStr).collection(location)
            )
            if (!favoriteEventCollection.isEmpty) {
                for (document in favoriteEventCollection) {
                    // TODO use with reference instead of id
                    val deferredEvent = GlobalScope.async {
                        val favEventDocSnap = FirebaseUtils.getDocumentSnapshot(
                            firestore.collection(EVENTS_KEY).document(dateStr)
                                .collection(location)
                                .document(document.id)
                        )
                        if (favEventDocSnap.exists()) {
                            val favEvent = favEventDocSnap.toObject(Event::class.java)!!
                            return@async preProcessEvent(favEvent)
                        } else
                            return@async null
                    }
                    deferredEvents.add(deferredEvent)
                }
            }
            dateCounter = dateCounter.plusDays(1)
        }
        val favouriteEvents = mutableListOf<Event>()
        for (deferredEvent in deferredEvents) {
            val event = deferredEvent.await()
            event?.let {
                favouriteEvents.add(it)
            }
        }
        return favouriteEvents
    }

    /**
     * Unmark event as favorite of a user.
     * @param event the event to be unmarked
     */
    fun removeFavoriteEventFromUser(event: Event) {
        val favoriteEventDoc = dbUser.collection(USER_FAVORITE_EVENTS)
            .document(event.utcDateISO)
            .collection(event.location)
            .document(event.eventId)
        FirebaseUtils.deleteDocumentRef(favoriteEventDoc)
    }

    /**
     * Increment the event popularity by one.
     * @param event the event
     */
    private fun incrementEventPopularity(event: Event) {
        val eventDoc = firestore.collection(EVENTS_KEY)
            .document(event.utcDateISO)
            .collection(event.location)
            .document(event.eventId)
        FirebaseUtils.mergeOrUpdateDocumentRef(eventDoc, POPULARITY_KEY, FieldValue.increment(1), 1)
    }

    /**
     * Increment the categories / subcategories popularity of an event by one.
     * @param event the event
     */
    private fun incrementEventCategoriesPopularity(event: Event) {
        event.categories?.let {
            for (category in it) {
                val cat = firestore.collection(CATEGORIES_KEY).document(category.categoryName!!)
                incrementEventCategoryPopularityOfDocumentRef(category, cat)
            }
        }
    }

    /**
     * Increment the categories / subcategories popularity of an event for the current user by one.
     * @param event the event
     */
    private fun incrementEventCategoriesPopularityOfUser(event: Event) {
        event.categories?.let {
            for (category in it) {
                val cat = dbUser.collection(CATEGORIES_KEY).document(category.categoryName!!)
                incrementEventCategoryPopularityOfDocumentRef(category, cat)
            }
        }
    }

    /**
     * Increment the category popularity of specified firestore document.
     * @param category the category
     * @param cat the document reference to be altered
     */
    private fun incrementEventCategoryPopularityOfDocumentRef(
        category: Category,
        cat: DocumentReference
    ) {
        FirebaseUtils.mergeOrUpdateDocumentRef(cat, POPULARITY_KEY, FieldValue.increment(1), 1)

        for (subcategory in category.subCategories!!) {
            val subcat = cat.collection(SUBCATEGORIES_KEY).document(subcategory)
            FirebaseUtils.mergeOrUpdateDocumentRef(
                subcat,
                POPULARITY_KEY,
                FieldValue.increment(1),
                1
            )
        }
    }

    /**
     * Normalize the popularity of all events on the specified date.
     * (Can be handled on server)
     */
    fun normalizeEventPopularity(eventDate: String) {
        val fbEvents = firestore.collection(EVENTS_KEY)
            .document(eventDate)
            .collection(location)
        normalizePopularity(fbEvents)
    }

    /**
     * Normalize the popularity of a category.
     * (Can be handled on server)
     */
    fun normalizeEventCategoryPopularity() {
        val fbCategories = firestore.collection(CATEGORIES_KEY)
        fbCategories.get().addOnSuccessListener { categories ->
            for (category in categories) {
                normalizePopularity(
                    fbCategories.document(category.id).collection(SUBCATEGORIES_KEY)
                )
            }
        }
        normalizePopularity(fbCategories)
    }

    /**
     * Normalize the popularity of a category for the current user.
     */
    fun normalizeEventCategoryPopularityOfUser() {
        val fbCategories = dbUser.collection(CATEGORIES_KEY)
        fbCategories.get().addOnSuccessListener { categories ->
            for (category in categories) {
                normalizePopularity(
                    fbCategories.document(category.id).collection(SUBCATEGORIES_KEY)
                )
            }
        }
        normalizePopularity(fbCategories)
    }

    /**
     * Generic method to normalize the popularities in a firestore collection.
     */
    private fun normalizePopularity(collection: CollectionReference) {
        collection.get().addOnSuccessListener { docs ->
            // Get maximum of all popularities
            var maxPopularity = 0
            for (doc in docs) {
                val pop: Int? = (doc.get(POPULARITY_KEY) as Long?)?.toInt()
                if (pop != null && pop > maxPopularity) {
                    maxPopularity = pop
                }
            }
            // Normalized popularity
            for (doc in docs) {
                val pop: Int = (doc.get(POPULARITY_KEY) as Long?)?.toInt() ?: 0
                val popNormalized = pop.toDouble() / maxPopularity
                FirebaseUtils.mergeOrUpdateDocumentRef(
                    collection.document(doc.id),
                    POPULARITY_NORMALIZED_KEY,
                    popNormalized,
                    popNormalized
                )
            }
        }
    }

    /**
     * Degrade an event.
     * @param event the event to be degraded.
     * @param contextWrapper the context to access history preferences.
     */
    fun degradeEvent(event: Event, contextWrapper: ContextWrapper) {
        // TODO may consider negative rating, too
        EventHistory(contextWrapper).add(event)
    }

    /**
     * Stop current running jobs.
     * Normalize popularity values once the user closes the app:
     * This ensures, that the data is already updated on next app start without waiting for recalculation.
     * Do calculations which concern all users on the server!
     */
    fun onStop() {
        //eventHandler.normalizeEventPopularity(date)
        //eventHandler.normalizeEventCategoryPopularity()
        if (update) {
            normalizeEventCategoryPopularityOfUser()
            update = false
        }
        filterJob?.cancel()
        queryJob?.cancel()
    }
}