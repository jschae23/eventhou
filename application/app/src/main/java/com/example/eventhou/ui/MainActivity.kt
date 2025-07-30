package com.example.eventhou.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.eventhou.EventhouApplication
import com.example.eventhou.R
import com.example.eventhou.data.model.AppUser
import com.example.eventhou.data.events.EventHandler
import com.example.eventhou.ui.favourites.FavouritesFragment
import com.example.eventhou.ui.home.HomeFragment
import com.example.eventhou.ui.profile.ProfileFragment
import com.example.eventhou.util.getCity
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var toolbar: ActionBar
    lateinit var eventHandler: EventHandler
    private lateinit var user: AppUser

    lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fresco.initialize(this)
        setContentView(R.layout.activity_main)
        toolbar = supportActionBar!!
        toolbar.hide()

        //setSupportActionBar(toolbar)
        bottomNavigation = findViewById(R.id.navigationView)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val appContainer = (this.application as EventhouApplication).appContainer
        user = appContainer.userRepository.getLoggedInUser()!!

        // TODO adapt to current location
        val sharedPref = this.getSharedPreferences(
            getString(R.string.preference_filter_city),
            Context.MODE_PRIVATE
        ) ?: return
        eventHandler = EventHandler(user)
        appContainer.eventHandler = eventHandler

        // Initially enable jobRunning to show loading fragment at app start.
        appContainer.eventHandler.isJobRunning = true

        // Cannot use savedInstanceState, cause listener is attached, when starting fragment.
        newHomeFragment()

        val categoryFilterPrefs = EventHandler.getCategoryFilterPrefs(this)
        lifecycleScope.launch(Dispatchers.IO) {
            // Use current location if possible, else use from preference
            val userCity = getCity(
                this@MainActivity,
                this@MainActivity
            )

            with(sharedPref.edit()) {
                putString(EventHandler.PREFERENCE_LOCATION, userCity)
                apply()
            }
            eventHandler.location = userCity ?: EventHandler.FALLBACK_LOCATION
            eventHandler.categoryFilterPrefs = categoryFilterPrefs
            eventHandler.processEvents(this@MainActivity)
        }
    }

    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    newHomeFragment()

                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_favourites -> {
                    val fragmentFavourites = FavouritesFragment.newInstance()
                    supportFragmentManager.beginTransaction().replace(
                        R.id.container,
                        fragmentFavourites,
                        fragmentFavourites.javaClass.simpleName
                    ).commit()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    val fragmentProfile = ProfileFragment.newInstance()
                    supportFragmentManager.beginTransaction().replace(
                        R.id.container,
                        fragmentProfile,
                        fragmentProfile.javaClass.simpleName
                    ).commit()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onBackPressed() {
        if ((bottomNavigation.selectedItemId == R.id.navigation_favourites) or (bottomNavigation.selectedItemId == R.id.navigation_profile)) {
            bottomNavigation.selectedItemId =
                R.id.navigation_home
            newHomeFragment()
        } else {
            super.onBackPressed()
        }
    }


    private fun newHomeFragment() {
        val fragmentHome = HomeFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(
            R.id.container,
            fragmentHome,
            fragmentHome.javaClass.simpleName
        ).commit()
    }
}
