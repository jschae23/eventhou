package com.example.eventhou.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * The event city / location.
 */
class City {
    var dailyLimit: Int = 0
    var enabled: Boolean = false
    var geopoint: GeoPoint? = null
    lateinit var name: String
    var onlineEvents: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as City
        if (name == other.name) return true
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}