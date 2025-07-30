package com.example.eventhou

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp

/**
 * Maintaining global application state.
 */
class EventhouApplication : Application(), DefaultLifecycleObserver {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super<Application>.onCreate()

        FirebaseApp.initializeApp(this)
        appContainer = AppContainer()

        // Get app life cycle to inform, when app is closed.
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        appContainer.eventHandler.onStop()
    }
}