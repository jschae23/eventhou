package com.example.eventhou

import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.data.user.UserDataSource
import com.example.eventhou.data.user.UserRepository

/**
 * Container of objects shared across the whole app
 */
class AppContainer {
    val userRepository = UserRepository(UserDataSource())
    lateinit var eventHandler: EventHandler
}
