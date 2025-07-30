package com.example.eventhou.data.events

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.eventhou.data.model.Event
import com.example.eventhou.R

/**
 * Save and get already swiped events.
 * @param contextWrapper the context to access app preferences.
 */
class EventHistory(contextWrapper: ContextWrapper) {
    private val sharedPref: SharedPreferences = contextWrapper.getSharedPreferences(
        contextWrapper.getString(R.string.preference_event_history),
        Context.MODE_PRIVATE
    )

    /**
     * Add an event to EventHistory.
     */
    fun add(event: Event) {
        val historyDate = event.utcDateISO
        val eventHistory = get(historyDate).toMutableSet()
        eventHistory.add(event.eventId)
        with(sharedPref.edit()) {
            putStringSet(historyDate, eventHistory)
            apply()
        }
    }

    /**
     * Remove all events from EventHistory.
     * This implicates that already swiped events can be swiped again.
     *
     */
    fun removeAll() {
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }

    /**
     * Remove events from EventHistory of a specified date.
     * @param historyDate the date of events, which should be removed from history
     */
    fun removeDate(historyDate: String) {
        with(sharedPref.edit()) {
            remove(historyDate)
            apply()
        }
    }

    /**
     * Get the evendIds of a specified date.
     * @param historyDate the date of events, which are requested from event history
     */
    fun get(historyDate: String): MutableSet<String> {
        return sharedPref.getStringSet(historyDate, hashSetOf<String>())!!
    }
}