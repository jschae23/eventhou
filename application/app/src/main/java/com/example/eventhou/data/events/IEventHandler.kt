package com.example.eventhou.data.events

import com.example.eventhou.data.model.Event

/**
 * Interface to inform the listeners about EventHandler changes.
 */
interface IEventHandler {
    fun onEventsLoading()
    fun onEventsLoaded(updatedEvents: List<Event>)
    fun onEventsUpdated(updatedEvents: List<Event>)
}