package com.example.eventhou.data.model

import com.example.eventhou.util.dateTimeToLocalizedShort
import java.util.*

/**
 * The event.
 */
class Event {

    var artistImageSrc: String? = null
    var artistName: String? = null
    var artistId: String? = null
    var artistUrl: String? = null
    var callToActionRedirectUrl: String? = null
    var callToActionText: String? = null
    var categories: List<Category>? = null
    lateinit var utcDateTimeISO: String // alias utcDateTime
    lateinit var utcDateISO: String
    lateinit var utcDate: Date
    lateinit var eventId: String
    var eventUrl: String? = null
    var fallbackImageUrl: String? = null
    var isFirstDayOfMonth: Boolean = false
    var localStartTime: String? = null
    var locationText: String? = null
    var pinIconSrc: String? = null
    var streamStart: String? = null
    var streamingEvent: Boolean = true
    var timezone: String? = null
    var title: String? = null
    var venueName: String? = null
    var watchLiveText: String? = null
    var score: Double? = null // Local value only!
    var popularity: Int = 0
    var popularityNormalized: Double = 0.0
    lateinit var location: String

    /**
     * Shows the event dateTime as localized string.
     * Note: Decisive for the localized date is the location of the user, not of the event!
     */
    fun localizedDateStr(): String {
        return dateTimeToLocalizedShort(utcDate)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Event
        if (eventId == other.eventId) return true
        return false
    }

    override fun hashCode(): Int {
        return eventId.hashCode()
    }

}